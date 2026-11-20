-- 034_vinko_credits_and_exchange_rates.sql: Soporte para créditos Vinkos, tasa de cambio personalizable y conversión en wallet

-- 1. Tabla de políticas de tasa de cambio para Vinkos
CREATE TABLE IF NOT EXISTS app.vinko_exchange_rates (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  currency char(3) NOT NULL CHECK(currency IN ('VES', 'USD')),
  rate_minor bigint NOT NULL CHECK(rate_minor > 0),
  active boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid REFERENCES app.users,
  reason text NOT NULL CHECK(length(reason) >= 3)
);
CREATE UNIQUE INDEX IF NOT EXISTS vinko_rate_active_idx ON app.vinko_exchange_rates(currency) WHERE active;

-- 2. Tasas iniciales por defecto (1 Vinko = 50,00 Bs. / 1 Vinko = 1,00 USD)
INSERT INTO app.vinko_exchange_rates(currency, rate_minor, active, reason)
VALUES
  ('VES', 5000, true, 'Tasa oficial inicial de Vinkos (1 Vinko = 50,00 Bs)'),
  ('USD', 100, true, 'Tasa oficial inicial de Vinkos (1 Vinko = 1,00 USD)')
ON CONFLICT DO NOTHING;

-- 3. Habilitar credit_wallets para cualquier usuario (clientes y profesionales)
ALTER TABLE app.credit_wallets DROP CONSTRAINT IF EXISTS credit_wallets_professional_id_fkey;
ALTER TABLE app.credit_wallets ADD CONSTRAINT credit_wallets_professional_id_fkey
  FOREIGN KEY (professional_id) REFERENCES app.users(id) ON DELETE CASCADE;

-- 4. Ampliar tipos de ledger en credit_ledger
ALTER TABLE app.credit_ledger DROP CONSTRAINT IF EXISTS credit_ledger_kind_check;
ALTER TABLE app.credit_ledger ADD CONSTRAINT credit_ledger_kind_check
  CHECK (kind = ANY (ARRAY['welcome'::text, 'purchase'::text, 'contract_debit'::text, 'contract_refund'::text, 'adjustment'::text, 'purchase_reversal'::text, 'vinko_purchase'::text, 'vinko_conversion'::text]));

-- 5. Función administrativa para configurar la tasa de cambio de Vinkos
CREATE OR REPLACE FUNCTION api.configure_vinko_exchange_rate(p_currency text, p_rate_minor bigint, p_reason text) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid := internal.require_permission('money.configure'); v_id uuid;
BEGIN
  IF p_currency IS NULL OR p_currency NOT IN ('VES', 'USD') THEN RAISE EXCEPTION 'invalid_currency'; END IF;
  IF p_rate_minor IS NULL OR p_rate_minor <= 0 THEN RAISE EXCEPTION 'invalid_rate'; END IF;
  IF length(trim(coalesce(p_reason, ''))) < 3 THEN RAISE EXCEPTION 'reason_required'; END IF;

  PERFORM pg_advisory_xact_lock(hashtextextended('vinko-rate:'||p_currency, 0));
  UPDATE app.vinko_exchange_rates SET active = false WHERE currency = p_currency AND active;
  INSERT INTO app.vinko_exchange_rates(currency, rate_minor, created_by, reason)
  VALUES(p_currency, p_rate_minor, v, p_reason) RETURNING id INTO v_id;

  PERFORM internal.audit('vinko.rate_configured', 'vinko_exchange_rate', v_id,
    jsonb_build_object('currency', p_currency, 'rate_minor', p_rate_minor, 'reason', p_reason));
  RETURN v_id;
END $$;

-- 6. Función pública/autenticada para consultar las tasas activas
CREATE OR REPLACE FUNCTION api.get_vinko_exchange_rates() RETURNS jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
BEGIN
  RETURN coalesce(
    (SELECT jsonb_object_agg(r.currency, jsonb_build_object(
       'currency', r.currency,
       'rate_minor', r.rate_minor,
       'created_at', r.created_at,
       'reason', r.reason
     )) FROM app.vinko_exchange_rates r WHERE r.active),
    '{}'::jsonb
  );
END $$;

-- 7. Función para convertir saldo disponible a créditos Vinkos
CREATE OR REPLACE FUNCTION api.convert_money_to_vinkos(p_currency text, p_vinkos bigint, p_key uuid) RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE
  v uuid := internal.actor();
  w uuid;
  r app.vinko_exchange_rates;
  v_cost bigint;
  v_avail uuid;
  v_clearing uuid;
  v_current_avail bigint;
  v_new_credits bigint;
  old_tx uuid;
BEGIN
  IF p_currency IS NULL OR p_currency NOT IN ('VES', 'USD') THEN RAISE EXCEPTION 'invalid_currency'; END IF;
  IF p_vinkos IS NULL OR p_vinkos <= 0 THEN RAISE EXCEPTION 'invalid_vinkos_amount'; END IF;
  IF p_key IS NULL THEN RAISE EXCEPTION 'idempotency_key_required'; END IF;

  SELECT * INTO r FROM app.vinko_exchange_rates WHERE currency = p_currency AND active;
  IF NOT FOUND THEN RAISE EXCEPTION 'exchange_rate_not_configured'; END IF;

  v_cost := p_vinkos * r.rate_minor;

  w := internal.money_wallet(v, p_currency);
  PERFORM 1 FROM app.money_wallets WHERE id = w AND status = 'active' FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'wallet_frozen'; END IF;

  SELECT id INTO old_tx FROM app.money_transactions WHERE idempotency_key = 'vinko_convert:' || p_key::text;
  IF FOUND THEN
    SELECT balance INTO v_new_credits FROM app.credit_wallets WHERE professional_id = v;
    RETURN jsonb_build_object('credits', v_new_credits, 'debited_minor', v_cost, 'currency', p_currency);
  END IF;

  v_avail := internal.money_account('available', p_currency, w);
  SELECT balance_minor INTO v_current_avail FROM app.money_accounts WHERE id = v_avail FOR UPDATE;
  IF coalesce(v_current_avail, 0) < v_cost THEN
    RAISE EXCEPTION 'insufficient_funds';
  END IF;

  v_clearing := internal.money_account('cash_clearing', p_currency);
  PERFORM internal.post_money('topup', p_currency, 'vinko_convert:' || p_key::text, p_key,
    jsonb_build_array(
      jsonb_build_object('account_id', v_avail, 'delta', -v_cost),
      jsonb_build_object('account_id', v_clearing, 'delta', v_cost)
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

-- 8. Actualizar api.my_wallet() para incluir balance de Vinkos y tasas activas
CREATE OR REPLACE FUNCTION api.my_wallet() RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE
  v uuid := internal.actor();
  v_bal bigint := 0;
  v_rates jsonb;
  v_ledger jsonb;
BEGIN
  INSERT INTO app.credit_wallets(professional_id, balance)
  VALUES(v, 0)
  ON CONFLICT (professional_id) DO NOTHING;

  SELECT balance INTO v_bal FROM app.credit_wallets WHERE professional_id = v;
  v_bal := coalesce(v_bal, 0);

  SELECT coalesce(jsonb_agg(to_jsonb(x) ORDER BY x.id DESC), '[]'::jsonb)
  INTO v_ledger
  FROM (
    SELECT id, delta, balance_after, kind, contract_id, purchase_id, reason, created_at
    FROM app.credit_ledger
    WHERE professional_id = v
    ORDER BY id DESC LIMIT 100
  ) x;

  SELECT api.get_vinko_exchange_rates() INTO v_rates;

  RETURN jsonb_build_object(
    'balance', v_bal,
    'credits', v_bal,
    'ledger', v_ledger,
    'rates', v_rates
  );
END $$;

-- 9. Actualizar api.admin_list() para soportar el recurso 'vinko_rates'
CREATE OR REPLACE FUNCTION api.admin_list(p_resource text, p_limit integer DEFAULT 50, p_offset integer DEFAULT 0)
RETURNS SETOF jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE tbl text; perm text;
BEGIN
  IF p_limit NOT BETWEEN 1 AND 100 OR p_offset < 0 THEN RAISE EXCEPTION 'invalid_pagination'; END IF;

  SELECT t, p INTO tbl, perm FROM (VALUES
    ('users', 'users', 'users.manage'),
    ('requests', 'service_requests', 'content.moderate'),
    ('reviews', 'reviews', 'content.moderate'),
    ('files', 'files', 'content.moderate'),
    ('kyc', 'kyc_cases', 'kyc.review'),
    ('plans', 'plans', 'plans.manage'),
    ('fees', 'withdrawal_fee_policies', 'money.configure'),
    ('tickets', 'support_tickets', 'support.read'),
    ('withdrawals', 'money_withdrawals', 'money.configure'),
    ('audit', 'audit_events', 'platform.read'),
    ('contracts', 'contracts', 'platform.read'),
    ('stores', 'stores', 'platform.read'),
    ('orders', 'orders', 'platform.read'),
    ('products', 'products', 'platform.read'),
    ('vinko_rates', 'vinko_exchange_rates', 'money.configure')
  ) x(k, t, p) WHERE k = p_resource;

  IF tbl IS NULL THEN RAISE EXCEPTION 'unknown_resource'; END IF;
  PERFORM internal.require_permission(perm);

  IF p_resource = 'vinko_rates' THEN
    RETURN QUERY
    SELECT jsonb_build_object(
      'id', r.id,
      'currency', r.currency,
      'rate_minor', r.rate_minor,
      'active', r.active,
      'status', CASE WHEN r.active THEN 'active' ELSE 'archived' END,
      'reason', r.reason,
      'created_at', r.created_at,
      'creator_name', coalesce(u.display_name, 'Administración')
    )
    FROM app.vinko_exchange_rates r
    LEFT JOIN app.users u ON u.id = r.created_by
    ORDER BY r.created_at DESC, r.id
    LIMIT p_limit OFFSET p_offset;
    RETURN;
  END IF;

  IF p_resource = 'withdrawals' THEN
    RETURN QUERY
    SELECT (to_jsonb(wd)-'destination_token') || jsonb_build_object('currency', w.currency, 'owner_name', u.display_name)
    FROM app.money_withdrawals wd
    JOIN app.money_wallets w ON w.id = wd.wallet_id
    JOIN app.users u ON u.id = w.user_id
    ORDER BY wd.created_at DESC, wd.id
    LIMIT p_limit OFFSET p_offset;
    RETURN;
  END IF;

  IF p_resource = 'contracts' THEN
    RETURN QUERY
    SELECT jsonb_build_object(
      'id', c.id,
      'request_id', c.request_id,
      'request_title', coalesce(r.title, 'Servicio acordado'),
      'category_name', coalesce(cat.name, 'Servicios Generales'),
      'customer_id', c.customer_id,
      'customer_name', cu.display_name,
      'customer_email', cu.email,
      'professional_id', c.professional_id,
      'professional_name', pu.display_name,
      'professional_email', pu.email,
      'status', c.status,
      'delivery_mode', c.delivery_mode,
      'agreed_price_minor', c.agreed_price_minor,
      'currency', c.currency,
      'platform_fee_minor', c.platform_fee_minor,
      'payment_protection', c.payment_protection,
      'plan_code', coalesce(p.plan_code, 'PRO-PROMO'),
      'accepted_at', c.accepted_at,
      'completed_at', c.completed_at,
      'cancelled_at', c.cancelled_at,
      'cancellation_reason', c.cancellation_reason
    )
    FROM app.contracts c
    JOIN app.users cu ON cu.id = c.customer_id
    JOIN app.users pu ON pu.id = c.professional_id
    LEFT JOIN app.professionals p ON p.user_id = c.professional_id
    LEFT JOIN app.service_requests r ON r.id = c.request_id
    LEFT JOIN app.categories cat ON cat.id = r.category_id
    ORDER BY coalesce(c.accepted_at, c.customer_accepted_at, now()) DESC, c.id
    LIMIT p_limit OFFSET p_offset;
    RETURN;
  END IF;

  IF p_resource = 'stores' THEN
    RETURN QUERY
    SELECT jsonb_build_object(
      'id', s.id,
      'professional_id', s.professional_id,
      'owner_name', u.display_name,
      'owner_email', u.email,
      'slug', s.slug,
      'name', s.name,
      'description', s.description,
      'status', s.status,
      'plan_code', s.plan_code,
      'created_at', s.created_at,
      'products_count', (SELECT count(*) FROM app.products p WHERE p.store_id = s.id),
      'orders_count', (SELECT count(*) FROM app.orders o WHERE o.store_id = s.id)
    )
    FROM app.stores s
    JOIN app.users u ON u.id = s.professional_id
    ORDER BY s.created_at DESC
    LIMIT p_limit OFFSET p_offset;
    RETURN;
  END IF;

  IF p_resource = 'orders' THEN
    RETURN QUERY
    SELECT jsonb_build_object(
      'id', o.id,
      'customer_id', o.customer_id,
      'customer_name', u.display_name,
      'customer_email', u.email,
      'store_id', o.store_id,
      'store_name', s.name,
      'seller_name', su.display_name,
      'seller_email', su.email,
      'status', o.status,
      'total_minor', o.total_minor,
      'currency', o.currency,
      'payment_method', o.payment_method,
      'payment_status', o.payment_status,
      'platform_fee_minor', o.platform_fee_minor,
      'created_at', o.created_at,
      'items_count', (SELECT count(*) FROM app.order_items oi WHERE oi.order_id = o.id),
      'delivery_contact', (SELECT recipient_name || ' (' || phone || ')' FROM app.order_delivery od WHERE od.order_id = o.id)
    )
    FROM app.orders o
    JOIN app.users u ON u.id = o.customer_id
    JOIN app.stores s ON s.id = o.store_id
    JOIN app.users su ON su.id = s.professional_id
    ORDER BY o.created_at DESC
    LIMIT p_limit OFFSET p_offset;
    RETURN;
  END IF;

  IF p_resource = 'products' THEN
    RETURN QUERY
    SELECT jsonb_build_object(
      'id', p.id,
      'store_id', p.store_id,
      'store_name', s.name,
      'seller_name', su.display_name,
      'name', p.name,
      'sku', p.sku,
      'description', p.description,
      'price_minor', p.price_minor,
      'currency', p.currency,
      'stock', p.stock,
      'status', p.status,
      'created_at', p.created_at
    )
    FROM app.products p
    JOIN app.stores s ON s.id = p.store_id
    JOIN app.users su ON su.id = s.professional_id
    ORDER BY p.created_at DESC
    LIMIT p_limit OFFSET p_offset;
    RETURN;
  END IF;

  IF p_resource = 'plans' THEN
    RETURN QUERY
    SELECT jsonb_build_object(
      'id', pl.id,
      'code', pl.code,
      'name', pl.name,
      'affiliate_type', pl.affiliate_type,
      'price_minor', pl.price_minor,
      'currency', pl.currency,
      'credits', pl.credits,
      'fee_summary', pl.fee_summary,
      'benefits_summary', pl.benefits_summary,
      'max_products', pl.max_products,
      'presential_fee_percent', pl.presential_fee_percent,
      'remote_fee_percent', pl.remote_fee_percent,
      'positioning_weight', pl.positioning_weight,
      'active', pl.active,
      'created_at', pl.created_at
    )
    FROM app.plans pl
    ORDER BY pl.affiliate_type, pl.price_minor ASC, pl.code;
    RETURN;
  END IF;

  RETURN QUERY EXECUTE format('SELECT to_jsonb(t)-ARRAY[''document_ciphertext'',''identity_fingerprint'',''encryption_key_id'',''object_key'',''destination_token''] FROM app.%I t ORDER BY 1 LIMIT $1 OFFSET $2', tbl) USING p_limit, p_offset;
END $$;

-- 10. Permisos de ejecución para las nuevas funciones
GRANT EXECUTE ON FUNCTION api.configure_vinko_exchange_rate(text, bigint, text) TO chamba_admin;
GRANT EXECUTE ON FUNCTION api.get_vinko_exchange_rates() TO chamba_api, chamba_admin;
GRANT EXECUTE ON FUNCTION api.convert_money_to_vinkos(text, bigint, uuid) TO chamba_api;
GRANT EXECUTE ON FUNCTION api.my_wallet() TO chamba_api;
