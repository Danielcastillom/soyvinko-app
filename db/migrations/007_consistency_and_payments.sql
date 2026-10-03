ALTER TABLE app.contracts ADD CONSTRAINT contract_customer_identity UNIQUE(id,customer_id);
ALTER TABLE app.location_shares ADD CONSTRAINT location_share_customer_fk
  FOREIGN KEY(contract_id,customer_id) REFERENCES app.contracts(id,customer_id);

-- Todos los cambios de consentimiento/posición toman el mismo bloqueo del contrato.
CREATE OR REPLACE FUNCTION api.revoke_location(p_contract uuid) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE c app.contracts; v uuid:=internal.actor();
BEGIN
  SELECT * INTO c FROM app.contracts WHERE id=p_contract FOR UPDATE;
  IF NOT FOUND OR c.customer_id<>v THEN RAISE EXCEPTION 'only_customer_can_revoke' USING ERRCODE='42501'; END IF;
  UPDATE app.location_shares SET revoked_at=now() WHERE contract_id=c.id AND revoked_at IS NULL;
  DELETE FROM app.professional_positions WHERE contract_id=c.id;
  PERFORM internal.audit('location.revoked','contract',c.id);
END $$;

CREATE OR REPLACE FUNCTION api.report_position(p_contract uuid,p_lon double precision,p_lat double precision,p_accuracy integer) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE c app.contracts; v uuid:=internal.actor();
BEGIN
  SELECT * INTO c FROM app.contracts WHERE id=p_contract AND professional_id=v FOR UPDATE;
  IF NOT FOUND OR c.status NOT IN ('en_route','arrived','in_progress') OR NOT EXISTS(
    SELECT 1 FROM app.location_shares WHERE contract_id=c.id AND revoked_at IS NULL AND expires_at>now()) THEN
    RAISE EXCEPTION 'position_not_allowed' USING ERRCODE='42501';
  END IF;
  INSERT INTO app.professional_positions(contract_id,location,accuracy_m,expires_at)
  VALUES(c.id,internal.point(p_lon,p_lat),p_accuracy,now()+interval '15 minutes')
  ON CONFLICT(contract_id) DO UPDATE SET location=EXCLUDED.location,accuracy_m=EXCLUDED.accuracy_m,recorded_at=now(),expires_at=EXCLUDED.expires_at;
END $$;

CREATE OR REPLACE FUNCTION api.accept_terms(p_terms uuid) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE t app.contract_terms; c app.contracts; v uuid:=internal.actor();
BEGIN
  SELECT * INTO t FROM app.contract_terms WHERE id=p_terms;
  IF NOT FOUND THEN RAISE EXCEPTION 'terms_not_found'; END IF;
  SELECT * INTO c FROM app.contracts WHERE id=t.contract_id AND v IN(customer_id,professional_id) FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'contract_access_denied' USING ERRCODE='42501'; END IF;
  IF c.status NOT IN('accepted','en_route','arrived','in_progress') THEN RAISE EXCEPTION 'terms_not_allowed'; END IF;
  SELECT * INTO t FROM app.contract_terms WHERE id=p_terms;
  IF EXISTS(SELECT 1 FROM app.contract_terms WHERE contract_id=c.id AND version>t.version) THEN RAISE EXCEPTION 'terms_superseded'; END IF;
  IF t.customer_accepted_at IS NOT NULL AND t.professional_accepted_at IS NOT NULL THEN RETURN; END IF;
  UPDATE app.contract_terms SET customer_accepted_at=CASE WHEN v=c.customer_id THEN coalesce(customer_accepted_at,now()) ELSE customer_accepted_at END,
    professional_accepted_at=CASE WHEN v=c.professional_id THEN coalesce(professional_accepted_at,now()) ELSE professional_accepted_at END
    WHERE id=t.id RETURNING * INTO t;
  IF t.customer_accepted_at IS NOT NULL AND t.professional_accepted_at IS NOT NULL THEN
    UPDATE app.contracts SET agreed_price_minor=t.price_minor,payment_method=t.payment_method,payment_terms=t.payment_terms,terms_version=t.version WHERE id=c.id;
    PERFORM internal.audit('contract.terms_accepted','contract',c.id,jsonb_build_object('version',t.version));
  END IF;
END $$;

CREATE OR REPLACE FUNCTION api.review_kyc(p_case uuid,p_status text,p_reason text,p_expires_at timestamptz DEFAULT NULL) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.require_permission('kyc.review'); k app.kyc_cases;
BEGIN
  SELECT * INTO k FROM app.kyc_cases WHERE id=p_case;
  IF NOT FOUND OR k.user_id=v THEN RAISE EXCEPTION 'kyc_review_not_allowed'; END IF;
  PERFORM 1 FROM app.users WHERE id=k.user_id FOR UPDATE;
  IF p_status IS NULL OR p_status NOT IN('approved','rejected','revoked','in_review','expired')
    OR length(trim(coalesce(p_reason,'')))<5 OR (p_status='approved' AND (p_expires_at IS NULL OR p_expires_at<=now())) THEN
    RAISE EXCEPTION 'invalid_kyc_decision'; END IF;
  IF p_status='approved' THEN
    UPDATE app.kyc_cases SET status='revoked',reason='Sustituida por una verificación posterior',reviewed_by=v,reviewed_at=now()
    WHERE user_id=k.user_id AND status='approved' AND id<>k.id;
  END IF;
  UPDATE app.kyc_cases SET status=p_status,reviewed_at=now(),reviewed_by=v,reason=p_reason,expires_at=p_expires_at WHERE id=k.id;
  INSERT INTO app.moderation_actions(actor_id,target_type,target_id,action,reason) VALUES(v,'kyc',k.id,p_status,p_reason);
END $$;
CREATE UNIQUE INDEX kyc_one_approved_idx ON app.kyc_cases(user_id) WHERE status='approved';

CREATE FUNCTION api.report_external_payment(p_contract uuid,p_order uuid,p_amount bigint,p_currency text,p_method text) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor(); c app.contracts; o app.orders; v_id uuid;
BEGIN
  IF num_nonnulls(p_contract,p_order)<>1 THEN RAISE EXCEPTION 'one_transaction_required'; END IF;
  IF p_contract IS NOT NULL THEN
    c:=internal.participant(p_contract);
    IF c.status IN('cancelled','disputed') OR c.currency<>p_currency THEN RAISE EXCEPTION 'invalid_payment_report'; END IF;
  ELSE
    SELECT o1.* INTO o FROM app.orders o1 JOIN app.stores s ON s.id=o1.store_id
    WHERE o1.id=p_order AND v IN(o1.customer_id,s.professional_id);
    IF NOT FOUND OR o.status IN('pending','cancelled','disputed') OR o.currency<>p_currency THEN RAISE EXCEPTION 'invalid_payment_report'; END IF;
  END IF;
  INSERT INTO app.external_payment_reports(contract_id,order_id,reported_by,amount_minor,currency,method)
  VALUES(p_contract,p_order,v,p_amount,p_currency,p_method) RETURNING id INTO v_id;
  PERFORM internal.audit('external_payment.reported','external_payment',v_id);
  RETURN v_id;
END $$;

CREATE FUNCTION api.confirm_external_payment(p_report uuid) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor(); r app.external_payment_reports; c app.contracts;
BEGIN
  SELECT * INTO r FROM app.external_payment_reports WHERE id=p_report FOR UPDATE;
  IF NOT FOUND OR r.reported_by=v THEN RAISE EXCEPTION 'counterparty_confirmation_required'; END IF;
  IF r.contract_id IS NOT NULL THEN c:=internal.participant(r.contract_id);
  ELSIF NOT EXISTS(SELECT 1 FROM app.orders o JOIN app.stores s ON s.id=o.store_id
    WHERE o.id=r.order_id AND v IN(o.customer_id,s.professional_id)) THEN RAISE EXCEPTION 'order_access_denied' USING ERRCODE='42501'; END IF;
  IF r.confirmed_at IS NOT NULL THEN RETURN; END IF;
  UPDATE app.external_payment_reports SET confirmed_by=v,confirmed_at=now() WHERE id=r.id;
  PERFORM internal.audit('external_payment.confirmed','external_payment',r.id);
END $$;

CREATE FUNCTION api.share_order_delivery(p_order uuid,p_name text,p_phone text,p_address_ciphertext bytea,p_key_id text) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor();
BEGIN
  PERFORM 1 FROM app.orders WHERE id=p_order AND customer_id=v AND status='accepted' FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'delivery_share_not_allowed'; END IF;
  INSERT INTO app.order_delivery(order_id,recipient_name,phone,address_ciphertext,encryption_key_id)
  VALUES(p_order,p_name,p_phone,p_address_ciphertext,p_key_id)
  ON CONFLICT(order_id) DO UPDATE SET recipient_name=EXCLUDED.recipient_name,phone=EXCLUDED.phone,
    address_ciphertext=EXCLUDED.address_ciphertext,encryption_key_id=EXCLUDED.encryption_key_id,buyer_shared_at=now();
  PERFORM internal.audit('order.delivery_shared','order',p_order);
END $$;

CREATE FUNCTION api.order_delivery(p_order uuid) RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor(); result jsonb;
BEGIN
  IF NOT EXISTS(SELECT 1 FROM app.orders o JOIN app.stores s ON s.id=o.store_id
    WHERE o.id=p_order AND v IN(o.customer_id,s.professional_id) AND o.status IN('accepted','dispatched','delivered')) THEN
    RAISE EXCEPTION 'order_access_denied' USING ERRCODE='42501'; END IF;
  SELECT to_jsonb(d) INTO result FROM app.order_delivery d WHERE d.order_id=p_order;
  PERFORM internal.audit('order.delivery_read','order',p_order);
  RETURN result;
END $$;

GRANT EXECUTE ON FUNCTION api.report_external_payment(uuid,uuid,bigint,text,text),api.confirm_external_payment(uuid),
  api.share_order_delivery(uuid,text,text,bytea,text),api.order_delivery(uuid) TO chamba_api;
