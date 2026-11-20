-- Correlación explícita del archivo solicitado y congelación de nuevos envíos.
ALTER TABLE app.funding_quotes ADD COLUMN terms_fingerprint jsonb NOT NULL DEFAULT '{}';

CREATE FUNCTION internal.funding_terms_snapshot() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
BEGIN
  SELECT jsonb_build_object('price_minor',agreed_price_minor,'currency',currency,'version',terms_version,
    'payment_terms',payment_terms,'requirements',work_requirements,'payment_method',payment_method)
    INTO NEW.terms_fingerprint FROM app.contracts WHERE id=NEW.contract_id;
  RETURN NEW;
END $$;
CREATE TRIGGER funding_terms_snapshot BEFORE INSERT ON app.funding_quotes FOR EACH ROW EXECUTE FUNCTION internal.funding_terms_snapshot();

CREATE FUNCTION internal.check_funding_quote() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE snapshot jsonb; quote_snapshot jsonb;
BEGIN
  IF NEW.status='funded' AND OLD.status='unfunded' THEN
    SELECT jsonb_build_object('price_minor',agreed_price_minor,'currency',currency,'version',terms_version,
      'payment_terms',payment_terms,'requirements',work_requirements,'payment_method',payment_method)
      INTO snapshot FROM app.contracts WHERE id=NEW.contract_id;
    SELECT terms_fingerprint INTO quote_snapshot FROM app.funding_quotes WHERE id=NEW.quote_id;
    IF snapshot IS DISTINCT FROM quote_snapshot THEN RAISE EXCEPTION 'funding_quote_stale'; END IF;
  END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER check_funding_quote BEFORE UPDATE ON app.service_escrows FOR EACH ROW EXECUTE FUNCTION internal.check_funding_quote();
CREATE OR REPLACE FUNCTION api.submit_delivery(p_contract uuid,p_summary text,p_key uuid,p_files uuid[] DEFAULT '{}') RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor(); c app.contracts; d app.service_deliveries; v_version integer; old_files uuid[];
BEGIN
  SELECT * INTO c FROM app.contracts WHERE id=p_contract AND professional_id=v FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'only_professional_can_deliver' USING ERRCODE='42501'; END IF;
  IF p_files IS NULL OR cardinality(p_files)>20 OR cardinality(p_files)<>(SELECT count(DISTINCT x) FROM unnest(p_files) x) THEN
    RAISE EXCEPTION 'invalid_delivery_files'; END IF;
  SELECT * INTO d FROM app.service_deliveries WHERE contract_id=c.id AND client_message_id=p_key;
  IF FOUND THEN
    SELECT coalesce(array_agg(file_id ORDER BY file_id),'{}') INTO old_files FROM app.delivery_files WHERE delivery_id=d.id;
    IF d.summary IS DISTINCT FROM p_summary OR old_files IS DISTINCT FROM ARRAY(SELECT x FROM unnest(p_files) x ORDER BY x) THEN
      RAISE EXCEPTION 'delivery_idempotency_conflict'; END IF;
    RETURN d.id;
  END IF;
  IF c.payment_protection<>'guaranteed' OR c.status<>'in_progress'
    OR NOT EXISTS(SELECT 1 FROM app.service_escrows WHERE contract_id=c.id AND status='funded') THEN RAISE EXCEPTION 'delivery_not_allowed'; END IF;
  IF EXISTS(SELECT 1 FROM unnest(p_files) AS requested(file_id) WHERE NOT EXISTS(SELECT 1 FROM app.files f WHERE f.id=requested.file_id AND f.owner_id=v
    AND f.purpose IN('chat','evidence','portfolio') AND f.moderation_status='approved' AND f.exif_removed AND f.deleted_at IS NULL)) THEN
    RAISE EXCEPTION 'unsafe_delivery_file'; END IF;
  SELECT coalesce(max(version),0)+1 INTO v_version FROM app.service_deliveries WHERE contract_id=c.id;
  INSERT INTO app.service_deliveries(contract_id,professional_id,version,summary,requirements_snapshot,terms_version,client_message_id)
  VALUES(c.id,v,v_version,p_summary,c.work_requirements,c.terms_version,p_key) RETURNING * INTO d;
  INSERT INTO app.delivery_files(delivery_id,file_id) SELECT d.id,x FROM unnest(p_files) x;
  UPDATE app.contracts SET professional_finished_at=now(),status='awaiting_confirmation' WHERE id=c.id;
  INSERT INTO app.contract_events(contract_id,actor_id,event_type,metadata) VALUES(c.id,v,'delivery_submitted',jsonb_build_object('delivery_id',d.id,'version',d.version));
  INSERT INTO app.outbox(topic,aggregate_id,recipient_id,deduplication_key) VALUES('delivery.submitted',c.id,c.customer_id,'delivery:'||d.id);
  RETURN d.id;
END $$;

CREATE OR REPLACE FUNCTION api.prepare_withdrawal(p_withdrawal uuid) RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE wd app.money_withdrawals; w app.money_wallets;
BEGIN
  SELECT * INTO wd FROM app.money_withdrawals WHERE id=p_withdrawal FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'withdrawal_not_found'; END IF;
  SELECT * INTO w FROM app.money_wallets WHERE id=wd.wallet_id FOR UPDATE;
  IF w.status<>'active' THEN RAISE EXCEPTION 'wallet_frozen_requires_reconciliation'; END IF;
  IF wd.status='pending' THEN
    PERFORM internal.withdrawal_identity(w.user_id);
    IF NOT EXISTS(SELECT 1 FROM app.payout_methods WHERE id=wd.payout_method_id AND status='verified') THEN RAISE EXCEPTION 'payout_method_disabled'; END IF;
    UPDATE app.money_withdrawals SET status='processing',dispatched_at=now() WHERE id=wd.id RETURNING * INTO wd;
  ELSIF wd.status<>'processing' THEN RAISE EXCEPTION 'withdrawal_not_dispatchable'; END IF;
  RETURN jsonb_build_object('id',wd.id,'provider',wd.destination_provider,'destination_token',wd.destination_token,
    'currency',w.currency,'net_minor',wd.net_minor,'gross_minor',wd.amount_minor,'fee_minor',wd.fee_minor,
    'provider_idempotency_key','withdrawal:'||wd.id,'status',wd.status);
END $$;
