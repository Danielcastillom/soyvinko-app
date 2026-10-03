-- 038_money_identity_retention.sql
--   1. convert_money_to_vinkos: idempotencia por usuario, reintento con los importes originales y
--      contabilidad correcta (la venta de Vinkos es ingreso de la plataforma, no caja por conciliar).
--   2. update_account: el RIF queda fijado por la identidad aprobada (KYC); antes era editable siempre.
--   3. Retención de datos personales en la limpieza del worker y purga física de archivos borrados.

-- 1. Conversión de dinero a Vinkos --------------------------------------------------------------
CREATE OR REPLACE FUNCTION api.convert_money_to_vinkos(p_currency text, p_vinkos bigint, p_key uuid)
RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE
  v uuid := internal.actor();
  w uuid;
  r app.vinko_exchange_rates;
  v_cost bigint;
  v_avail uuid;
  v_revenue uuid;
  v_current_avail bigint;
  v_new_credits bigint;
  v_tx_key text;
  old_tx app.money_transactions;
BEGIN
  IF p_currency IS NULL OR p_currency NOT IN ('VES', 'USD') THEN RAISE EXCEPTION 'invalid_currency'; END IF;
  IF p_vinkos IS NULL OR p_vinkos <= 0 THEN RAISE EXCEPTION 'invalid_vinkos_amount'; END IF;
  IF p_key IS NULL THEN RAISE EXCEPTION 'idempotency_key_required'; END IF;

  -- La clave incluye al usuario: la de otra persona nunca se interpreta como un reintento propio.
  v_tx_key := 'vinko_convert:' || v::text || ':' || p_key::text;

  w := internal.money_wallet(v, p_currency);
  PERFORM 1 FROM app.money_wallets WHERE id = w AND status = 'active' FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'wallet_frozen'; END IF;

  -- Reintento: devuelve lo que realmente se cobró (también para claves del formato anterior a 038).
  SELECT * INTO old_tx FROM app.money_transactions
  WHERE idempotency_key = v_tx_key OR (idempotency_key = 'vinko_convert:' || p_key::text AND actor_id = v);
  IF FOUND THEN
    SELECT balance INTO v_new_credits FROM app.credit_wallets WHERE professional_id = v;
    RETURN jsonb_build_object(
      'credits', v_new_credits,
      'debited_minor', (SELECT -sum(delta_minor) FROM app.money_entries WHERE transaction_id = old_tx.id AND delta_minor < 0),
      'currency', old_tx.currency,
      'replayed', true);
  END IF;

  SELECT * INTO r FROM app.vinko_exchange_rates WHERE currency = p_currency AND active;
  IF NOT FOUND THEN RAISE EXCEPTION 'exchange_rate_not_configured'; END IF;
  v_cost := p_vinkos * r.rate_minor;

  v_avail := internal.money_account('available', p_currency, w);
  SELECT balance_minor INTO v_current_avail FROM app.money_accounts WHERE id = v_avail FOR UPDATE;
  IF coalesce(v_current_avail, 0) < v_cost THEN
    RAISE EXCEPTION 'insufficient_funds';
  END IF;

  v_revenue := internal.money_account('platform_fees', p_currency);
  PERFORM internal.post_money('vinko_purchase', p_currency, v_tx_key, p_key,
    jsonb_build_array(
      jsonb_build_object('account_id', v_avail, 'delta', -v_cost),
      jsonb_build_object('account_id', v_revenue, 'delta', v_cost)
    ));

  INSERT INTO app.credit_wallets(professional_id, balance)
  VALUES(v, 0)
  ON CONFLICT (professional_id) DO NOTHING;

  UPDATE app.credit_wallets
  SET balance = balance + p_vinkos, updated_at = now()
  WHERE professional_id = v
  RETURNING balance INTO v_new_credits;

  INSERT INTO app.credit_ledger(professional_id, delta, balance_after, kind, idempotency_key, reason, actor_id)
  VALUES(v, p_vinkos, v_new_credits, 'vinko_conversion', p_key,
         'Conversión a Vinkos: ' || p_vinkos::text || ' Vinkos (' || (v_cost / 100)::text || ' ' || p_currency || ')', v);

  PERFORM internal.audit('vinko.converted', 'credit_wallet', v,
    jsonb_build_object('vinkos', p_vinkos, 'cost_minor', v_cost, 'currency', p_currency));

  RETURN jsonb_build_object('credits', v_new_credits, 'debited_minor', v_cost, 'currency', p_currency);
END $$;

-- 2. RIF fijado por la identidad aprobada -------------------------------------------------------
CREATE OR REPLACE FUNCTION api.update_account(
  p_name text,
  p_locale text DEFAULT 'es'::text,
  p_timezone text DEFAULT 'UTC'::text,
  p_business_name text DEFAULT NULL,
  p_rif text DEFAULT NULL,
  p_contact_name text DEFAULT NULL
) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE
  v_user app.users;
  v_display text;
  v_new_rif text := nullif(upper(trim(p_rif)), '');
BEGIN
  SELECT * INTO v_user FROM app.users WHERE id=internal.actor() FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'user_not_found'; END IF;

  -- Con identidad aprobada y vigente, el RIF solo cambia mediante una nueva verificación revisada.
  IF p_rif IS NOT NULL AND v_new_rif IS DISTINCT FROM v_user.rif AND EXISTS (
    SELECT 1 FROM app.kyc_cases k
    WHERE k.user_id = v_user.id AND k.status = 'approved' AND k.expires_at > now()
  ) THEN
    RAISE EXCEPTION 'rif_locked_by_kyc';
  END IF;

  IF v_user.account_type = 'juridico' THEN
    v_display := coalesce(nullif(trim(p_business_name), ''), nullif(trim(p_name), ''), v_user.display_name);
  ELSE
    v_display := coalesce(nullif(trim(p_name), ''), v_user.display_name);
  END IF;

  UPDATE app.users SET
    display_name = v_display,
    business_name = CASE WHEN v_user.account_type = 'juridico' AND p_business_name IS NOT NULL THEN nullif(trim(p_business_name), '') ELSE business_name END,
    rif = CASE WHEN p_rif IS NOT NULL THEN v_new_rif ELSE rif END,
    contact_name = CASE WHEN v_user.account_type = 'juridico' AND p_contact_name IS NOT NULL THEN nullif(trim(p_contact_name), '') ELSE contact_name END,
    locale = coalesce(p_locale, locale),
    timezone = coalesce(p_timezone, timezone),
    updated_at = now()
  WHERE id = v_user.id;
END $$;

-- 3. Retención de datos personales --------------------------------------------------------------
ALTER TABLE app.files ADD COLUMN IF NOT EXISTS purged_at timestamptz;

CREATE OR REPLACE FUNCTION api.worker_housekeeping()
RETURNS void
LANGUAGE sql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  DELETE FROM app.http_rate_limits WHERE expires_at<now()-interval '1 day';
  DELETE FROM app.auth_challenges WHERE expires_at<now()-interval '30 days';

  -- KYC cerrado sin aprobación: 90 días después se eliminan documento cifrado, huella y metadatos
  -- (cédula, nombre, nacimiento, teléfono, dirección). Queda el registro del caso y su decisión.
  UPDATE app.kyc_cases
  SET document_ciphertext = NULL, identity_fingerprint = NULL, external_reference = NULL
  WHERE status IN ('rejected', 'expired', 'revoked')
    AND coalesce(reviewed_at, submitted_at) < now() - interval '90 days'
    AND (document_ciphertext IS NOT NULL OR identity_fingerprint IS NOT NULL OR external_reference IS NOT NULL);

  UPDATE app.files f SET deleted_at = now()
  WHERE f.purpose = 'kyc' AND f.deleted_at IS NULL
    AND EXISTS (SELECT 1 FROM app.kyc_documents d WHERE d.file_id = f.id)
    AND NOT EXISTS (
      SELECT 1 FROM app.kyc_documents d JOIN app.kyc_cases k ON k.id = d.case_id
      WHERE d.file_id = f.id
        AND (k.status IN ('pending', 'in_review', 'approved')
             OR coalesce(k.reviewed_at, k.submitted_at) >= now() - interval '90 days'));

  -- Ubicación exacta del cliente: 180 días después de cerrar la solicitud se conserva solo la zona
  -- aproximada (~1 km) y se borran dirección e indicaciones.
  UPDATE app.request_locations l
  SET address_line = '', instructions = NULL, source_address_id = NULL,
      location = ST_SnapToGrid(l.location::geometry, 0.01)::geography
  FROM app.service_requests r
  WHERE r.id = l.request_id
    AND r.status IN ('completed', 'cancelled', 'expired')
    AND coalesce(r.expires_at, r.created_at) < now() - interval '180 days'
    AND l.address_line <> '';
$$;

-- Purga física: el worker borra del disco los archivos marcados como eliminados hace más de 1 día.
CREATE OR REPLACE FUNCTION api.worker_files_to_purge(p_limit integer DEFAULT 100)
RETURNS SETOF jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  SELECT jsonb_build_object('id', id, 'object_key', object_key)
  FROM app.files
  WHERE deleted_at < now() - interval '1 day' AND purged_at IS NULL
  ORDER BY deleted_at
  LIMIT least(greatest(coalesce(p_limit, 100), 1), 500);
$$;

CREATE OR REPLACE FUNCTION api.worker_mark_file_purged(p_file uuid)
RETURNS void
LANGUAGE sql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  UPDATE app.files SET purged_at = now() WHERE id = p_file AND deleted_at IS NOT NULL AND purged_at IS NULL;
$$;

REVOKE ALL ON FUNCTION api.worker_files_to_purge(integer), api.worker_mark_file_purged(uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION api.worker_files_to_purge(integer), api.worker_mark_file_purged(uuid) TO chamba_worker;
