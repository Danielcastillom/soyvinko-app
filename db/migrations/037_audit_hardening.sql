-- 037_audit_hardening.sql
-- Endurecimiento tras la auditoría de octubre 2026:
--   1. api.update_account: se había reemplazado fuera de las migraciones sin SET search_path.
--      Se versiona el comportamiento vigente (RIF editable también para personas naturales) con search_path fijo.
--   2. api.admin_advance_order: máquina de estados explícita. Antes un pedido cancelado podía pasar a
--      entregado y volver a cancelarse, reintegrando el inventario dos veces.
--   3. Permisos: las funciones de contratos de 035/036 solo las usa chamba_api.
--   4. Índices en columnas consultadas en caminos calientes y claves foráneas sin índice.

-- 1. update_account con search_path fijo -----------------------------------------------------------
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
BEGIN
  SELECT * INTO v_user FROM app.users WHERE id=internal.actor() FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'user_not_found'; END IF;

  IF v_user.account_type = 'juridico' THEN
    v_display := coalesce(nullif(trim(p_business_name), ''), nullif(trim(p_name), ''), v_user.display_name);
  ELSE
    v_display := coalesce(nullif(trim(p_name), ''), v_user.display_name);
  END IF;

  UPDATE app.users SET
    display_name = v_display,
    business_name = CASE WHEN v_user.account_type = 'juridico' AND p_business_name IS NOT NULL THEN nullif(trim(p_business_name), '') ELSE business_name END,
    rif = CASE WHEN p_rif IS NOT NULL THEN nullif(upper(trim(p_rif)), '') ELSE rif END,
    contact_name = CASE WHEN v_user.account_type = 'juridico' AND p_contact_name IS NOT NULL THEN nullif(trim(p_contact_name), '') ELSE contact_name END,
    locale = coalesce(p_locale, locale),
    timezone = coalesce(p_timezone, timezone),
    updated_at = now()
  WHERE id = v_user.id;
END $$;

-- 2. admin_advance_order con transiciones explícitas -------------------------------------------------
CREATE OR REPLACE FUNCTION api.admin_advance_order(p_order uuid, p_action text, p_reason text)
RETURNS text
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid := internal.require_permission('support.manage');
  o app.orders; v_status text; v_allowed text[]; item record;
BEGIN
  SELECT * INTO o FROM app.orders WHERE id = p_order FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'order_not_found'; END IF;
  IF length(trim(coalesce(p_reason, ''))) < 5 THEN RAISE EXCEPTION 'reason_required'; END IF;

  -- Estados de origen permitidos por acción. 'delivered' y 'cancelled' son finales.
  CASE p_action
    WHEN 'accept'   THEN v_status := 'accepted';   v_allowed := ARRAY['pending','disputed'];
    WHEN 'dispatch' THEN v_status := 'dispatched'; v_allowed := ARRAY['pending','accepted','disputed'];
    WHEN 'receive'  THEN v_status := 'delivered';  v_allowed := ARRAY['accepted','dispatched','disputed'];
    WHEN 'cancel'   THEN v_status := 'cancelled';  v_allowed := ARRAY['pending','accepted','dispatched','disputed'];
    ELSE RAISE EXCEPTION 'invalid_order_transition';
  END CASE;

  -- Reintento de la misma acción: idempotente, sin efectos.
  IF o.status = v_status THEN RETURN o.status; END IF;
  IF NOT o.status = ANY(v_allowed) THEN RAISE EXCEPTION 'invalid_order_transition'; END IF;

  IF p_action = 'cancel' THEN
    FOR item IN SELECT * FROM app.order_items WHERE order_id = o.id ORDER BY product_id LOOP
      UPDATE app.products SET stock = stock + item.quantity, updated_at = now() WHERE id = item.product_id;
      INSERT INTO app.inventory_movements(product_id, order_id, delta, reason, actor_id)
      VALUES(item.product_id, o.id, item.quantity, 'admin_order_cancelled', v);
    END LOOP;
  END IF;

  UPDATE app.orders SET status = v_status WHERE id = o.id;
  INSERT INTO app.order_events(order_id, actor_id, event_type, metadata)
  VALUES(o.id, v, 'admin_' || p_action, jsonb_build_object('from', o.status, 'to', v_status, 'reason', p_reason));
  PERFORM internal.audit('order.admin_' || p_action, 'order', o.id, jsonb_build_object('reason', p_reason));
  RETURN v_status;
END $$;

-- 3. Permisos mínimos -----------------------------------------------------------------------------
REVOKE EXECUTE ON FUNCTION
  api.rate_contract(uuid, smallint, boolean, text),
  api.contract_contacts(uuid),
  api.contract_detail(uuid),
  api.accept_direct_request(uuid),
  api.my_contracts()
FROM chamba_auth, chamba_admin;

-- 4. Índices --------------------------------------------------------------------------------------
-- db.py aplica cada migración dentro de una transacción: no se puede usar CONCURRENTLY.
CREATE INDEX IF NOT EXISTS auth_challenges_user_purpose_open_idx
  ON app.auth_challenges(user_id, purpose) WHERE consumed_at IS NULL;
CREATE INDEX IF NOT EXISTS service_requests_target_professional_idx
  ON app.service_requests(target_professional_id, status) WHERE target_professional_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS kyc_cases_status_submitted_idx ON app.kyc_cases(status, submitted_at);
CREATE INDEX IF NOT EXISTS contract_events_contract_created_idx ON app.contract_events(contract_id, created_at);
CREATE INDEX IF NOT EXISTS support_messages_ticket_idx ON app.support_messages(ticket_id);
CREATE INDEX IF NOT EXISTS inventory_movements_product_idx ON app.inventory_movements(product_id);
CREATE INDEX IF NOT EXISTS inventory_movements_order_idx ON app.inventory_movements(order_id);
CREATE INDEX IF NOT EXISTS payout_methods_user_idx ON app.payout_methods(user_id);
CREATE INDEX IF NOT EXISTS offerings_professional_idx ON app.offerings(professional_id);
CREATE INDEX IF NOT EXISTS coverage_areas_professional_idx ON app.coverage_areas(professional_id);
CREATE INDEX IF NOT EXISTS files_owner_idx ON app.files(owner_id);
CREATE INDEX IF NOT EXISTS external_payment_reports_contract_idx ON app.external_payment_reports(contract_id);
CREATE INDEX IF NOT EXISTS external_payment_reports_order_idx ON app.external_payment_reports(order_id);
CREATE INDEX IF NOT EXISTS messages_sender_idx ON app.messages(sender_id);
CREATE INDEX IF NOT EXISTS contracts_proposal_idx ON app.contracts(proposal_id);
