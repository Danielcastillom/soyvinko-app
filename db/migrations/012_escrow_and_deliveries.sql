CREATE FUNCTION api.quote_contract_funding(p_contract uuid) RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor(); c app.contracts; q app.funding_quotes;
BEGIN
  SELECT * INTO c FROM app.contracts WHERE id=p_contract AND customer_id=v FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'only_customer_can_fund' USING ERRCODE='42501'; END IF;
  IF c.payment_protection<>'guaranteed' OR c.status<>'accepted' OR c.agreed_price_minor IS NULL OR c.agreed_price_minor<=0 THEN
    RAISE EXCEPTION 'agreed_positive_price_required'; END IF;
  IF NOT EXISTS(SELECT 1 FROM app.service_escrows WHERE contract_id=c.id AND status='unfunded') THEN RAISE EXCEPTION 'escrow_already_funded'; END IF;
  IF EXISTS(SELECT 1 FROM app.contract_terms t WHERE t.contract_id=c.id AND t.version=(SELECT max(version) FROM app.contract_terms WHERE contract_id=c.id)
    AND (t.customer_accepted_at IS NULL OR t.professional_accepted_at IS NULL)) THEN RAISE EXCEPTION 'pending_contract_terms'; END IF;
  INSERT INTO app.funding_quotes(contract_id,customer_id,terms_version,price_minor,currency,expires_at)
  VALUES(c.id,v,c.terms_version,c.agreed_price_minor,c.currency,now()+interval '15 minutes') RETURNING * INTO q;
  RETURN to_jsonb(q)||jsonb_build_object('requirements',c.work_requirements,'professional_receives_minor',q.price_minor);
END $$;

CREATE FUNCTION api.fund_contract(p_quote uuid,p_key uuid) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor(); c app.contracts; q app.funding_quotes; e app.service_escrows; w uuid; a uuid; held uuid; tx uuid;
BEGIN
  IF p_key IS NULL THEN RAISE EXCEPTION 'idempotency_key_required'; END IF;
  SELECT * INTO q FROM app.funding_quotes WHERE id=p_quote AND customer_id=v;
  IF NOT FOUND THEN RAISE EXCEPTION 'funding_quote_not_owned' USING ERRCODE='42501'; END IF;
  SELECT * INTO c FROM app.contracts WHERE id=q.contract_id AND customer_id=v FOR UPDATE;
  SELECT * INTO e FROM app.service_escrows WHERE contract_id=c.id FOR UPDATE;
  IF e.status<>'unfunded' THEN
    IF e.quote_id=q.id THEN RETURN c.id; END IF;
    RAISE EXCEPTION 'escrow_already_funded';
  END IF;
  IF c.payment_protection<>'guaranteed' OR c.status<>'accepted' OR q.expires_at<=now()
    OR (q.price_minor,q.currency,q.terms_version) IS DISTINCT FROM(c.agreed_price_minor,c.currency,c.terms_version) THEN
    RAISE EXCEPTION 'funding_quote_stale'; END IF;
  IF EXISTS(SELECT 1 FROM app.contract_terms t WHERE t.contract_id=c.id AND t.version=(SELECT max(version) FROM app.contract_terms WHERE contract_id=c.id)
    AND (t.customer_accepted_at IS NULL OR t.professional_accepted_at IS NULL)) THEN RAISE EXCEPTION 'pending_contract_terms'; END IF;
  IF NOT EXISTS(SELECT 1 FROM app.users WHERE id=c.professional_id AND status='active') THEN RAISE EXCEPTION 'professional_unavailable'; END IF;
  w:=internal.money_wallet(v,c.currency);
  PERFORM 1 FROM app.money_wallets WHERE id=w AND status='active' FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'wallet_frozen'; END IF;
  a:=internal.money_account('available',c.currency,w); held:=internal.money_account('contract_hold',c.currency,w,c.id);
  tx:=internal.post_money('contract_funded',c.currency,'contract:fund:'||c.id,c.id,
    jsonb_build_array(jsonb_build_object('account_id',a,'delta',-q.price_minor),jsonb_build_object('account_id',held,'delta',q.price_minor)),
    jsonb_build_object('client_key',p_key,'quote_id',q.id));
  UPDATE app.service_escrows SET status='funded',quote_id=q.id,funded_at=now() WHERE contract_id=c.id;
  INSERT INTO app.contract_events(contract_id,actor_id,event_type,metadata)
  VALUES(c.id,v,'funds_reserved',jsonb_build_object('amount_minor',q.price_minor,'currency',q.currency,'transaction_id',tx));
  INSERT INTO app.outbox(topic,aggregate_id,recipient_id,deduplication_key) VALUES('contract.funded',c.id,c.professional_id,'funded:'||c.id);
  RETURN c.id;
END $$;

CREATE FUNCTION api.escrow_details(p_contract uuid) RETURNS jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE c app.contracts:=internal.participant(p_contract); result jsonb;
BEGIN
  SELECT to_jsonb(e)||jsonb_build_object('price_minor',q.price_minor,'currency',coalesce(q.currency,c.currency),
    'service_fee_minor',0,'professional_receives_minor',q.price_minor,'requirements',c.work_requirements,
    'deliveries',(SELECT jsonb_agg(to_jsonb(d)||jsonb_build_object('files',(SELECT jsonb_agg(file_id) FROM app.delivery_files WHERE delivery_id=d.id)) ORDER BY d.version)
      FROM app.service_deliveries d WHERE d.contract_id=c.id)) INTO result
  FROM app.service_escrows e LEFT JOIN app.funding_quotes q ON q.id=e.quote_id WHERE e.contract_id=c.id;
  RETURN result;
END $$;

CREATE FUNCTION api.submit_delivery(p_contract uuid,p_summary text,p_key uuid,p_files uuid[] DEFAULT '{}') RETURNS uuid
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
  IF EXISTS(SELECT 1 FROM unnest(p_files) id WHERE NOT EXISTS(SELECT 1 FROM app.files f WHERE f.id=id AND f.owner_id=v
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

CREATE FUNCTION api.request_delivery_changes(p_delivery uuid,p_reason text) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor(); c app.contracts; d app.service_deliveries;
BEGIN
  SELECT * INTO d FROM app.service_deliveries WHERE id=p_delivery;
  SELECT * INTO c FROM app.contracts WHERE id=d.contract_id AND customer_id=v FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'only_customer_can_review' USING ERRCODE='42501'; END IF;
  SELECT * INTO d FROM app.service_deliveries WHERE id=p_delivery;
  IF d.status<>'submitted' OR c.status<>'awaiting_confirmation' OR length(trim(coalesce(p_reason,'')))<10 THEN RAISE EXCEPTION 'delivery_not_reviewable'; END IF;
  IF EXISTS(SELECT 1 FROM app.service_deliveries WHERE contract_id=c.id AND version>d.version) THEN RAISE EXCEPTION 'delivery_superseded'; END IF;
  UPDATE app.service_deliveries SET status='changes_requested',reviewed_at=now(),review_note=p_reason WHERE id=d.id;
  UPDATE app.contracts SET status='in_progress',professional_finished_at=NULL WHERE id=c.id;
  INSERT INTO app.contract_events(contract_id,actor_id,event_type,metadata) VALUES(c.id,v,'delivery_changes_requested',jsonb_build_object('delivery_id',d.id,'reason',p_reason));
END $$;

CREATE FUNCTION api.release_contract_funds(p_contract uuid,p_delivery uuid,p_key uuid) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor(); c app.contracts; e app.service_escrows; q app.funding_quotes; d app.service_deliveries;
  source_wallet uuid; target_wallet uuid; held uuid; available uuid; tx uuid;
BEGIN
  IF p_key IS NULL THEN RAISE EXCEPTION 'idempotency_key_required'; END IF;
  SELECT * INTO c FROM app.contracts WHERE id=p_contract AND customer_id=v FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'only_customer_can_release' USING ERRCODE='42501'; END IF;
  SELECT * INTO d FROM app.service_deliveries WHERE id=p_delivery AND contract_id=c.id;
  IF NOT FOUND THEN RAISE EXCEPTION 'delivery_not_found'; END IF;
  SELECT * INTO e FROM app.service_escrows WHERE contract_id=c.id FOR UPDATE;
  IF e.status='released' AND d.status='accepted' THEN RETURN c.id; END IF;
  IF e.status IS DISTINCT FROM 'funded' OR c.status<>'awaiting_confirmation' OR c.professional_finished_at IS NULL
    OR d.status<>'submitted' OR d.terms_version<>c.terms_version OR EXISTS(SELECT 1 FROM app.service_deliveries WHERE contract_id=c.id AND version>d.version) THEN
    RAISE EXCEPTION 'funds_not_releasable'; END IF;
  SELECT * INTO q FROM app.funding_quotes WHERE id=e.quote_id;
  source_wallet:=internal.money_wallet(c.customer_id,c.currency); target_wallet:=internal.money_wallet(c.professional_id,c.currency);
  PERFORM 1 FROM app.money_wallets WHERE id IN(source_wallet,target_wallet) ORDER BY id FOR UPDATE;
  IF EXISTS(SELECT 1 FROM app.money_wallets WHERE id=source_wallet AND status<>'active') THEN RAISE EXCEPTION 'wallet_frozen'; END IF;
  held:=internal.money_account('contract_hold',c.currency,source_wallet,c.id); available:=internal.money_account('available',c.currency,target_wallet);
  tx:=internal.post_money('contract_released',c.currency,'contract:release:'||c.id,c.id,
    jsonb_build_array(jsonb_build_object('account_id',held,'delta',-q.price_minor),jsonb_build_object('account_id',available,'delta',q.price_minor)),
    jsonb_build_object('delivery_id',d.id,'client_key',p_key));
  UPDATE app.service_escrows SET status='released',released_at=now(),release_key=p_key WHERE contract_id=c.id;
  UPDATE app.service_deliveries SET status='accepted',reviewed_at=now(),review_note='Cliente validó los requisitos y liberó el pago' WHERE id=d.id;
  UPDATE app.contracts SET status='completed',customer_finished_at=now(),completed_at=now() WHERE id=c.id;
  UPDATE app.service_requests SET status='completed' WHERE id=c.request_id;
  UPDATE app.location_shares SET revoked_at=now() WHERE contract_id=c.id AND revoked_at IS NULL;
  DELETE FROM app.professional_positions WHERE contract_id=c.id;
  INSERT INTO app.contract_events(contract_id,actor_id,event_type,from_status,to_status,metadata)
  VALUES(c.id,v,'customer_released_payment',c.status,'completed',jsonb_build_object('delivery_id',d.id,'transaction_id',tx,'amount_minor',q.price_minor));
  INSERT INTO app.outbox(topic,aggregate_id,recipient_id,deduplication_key)
  SELECT 'review.requested',c.id,x,'review:'||c.id||':'||x FROM unnest(ARRAY[c.customer_id,c.professional_id]) x;
  RETURN c.id;
END $$;

CREATE FUNCTION api.request_escrow_refund(p_contract uuid,p_reason text) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor(); c app.contracts; ticket uuid;
BEGIN
  SELECT * INTO c FROM app.contracts WHERE id=p_contract AND customer_id=v FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'only_customer_can_request_refund' USING ERRCODE='42501'; END IF;
  IF length(trim(coalesce(p_reason,'')))<10 OR NOT EXISTS(SELECT 1 FROM app.service_escrows WHERE contract_id=c.id AND status='funded') THEN
    RAISE EXCEPTION 'escrow_refund_not_available'; END IF;
  UPDATE app.service_escrows SET refund_requested_at=now(),refund_reason=p_reason WHERE contract_id=c.id;
  ticket:=api.dispute_contract(c.id,p_reason);
  RETURN ticket;
END $$;

CREATE FUNCTION internal.refund_escrow(p_contract uuid,p_reason text) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE c app.contracts; e app.service_escrows; q app.funding_quotes; w uuid; held uuid; a uuid;
BEGIN
  SELECT * INTO c FROM app.contracts WHERE id=p_contract FOR UPDATE;
  SELECT * INTO e FROM app.service_escrows WHERE contract_id=c.id FOR UPDATE;
  IF e.status='refunded' THEN RETURN; END IF;
  IF e.status IS NULL OR e.status NOT IN('funded','disputed') THEN RAISE EXCEPTION 'escrow_refund_not_available'; END IF;
  SELECT * INTO q FROM app.funding_quotes WHERE id=e.quote_id;
  w:=internal.money_wallet(c.customer_id,c.currency);
  PERFORM 1 FROM app.money_wallets WHERE id=w FOR UPDATE;
  held:=internal.money_account('contract_hold',c.currency,w,c.id); a:=internal.money_account('available',c.currency,w);
  PERFORM internal.post_money('contract_refunded',c.currency,'contract:refund:'||c.id,c.id,
    jsonb_build_array(jsonb_build_object('account_id',held,'delta',-q.price_minor),jsonb_build_object('account_id',a,'delta',q.price_minor)));
  UPDATE app.service_escrows SET status='refunded',refunded_at=now() WHERE contract_id=c.id;
  UPDATE app.contracts SET status='cancelled',cancelled_at=now(),cancellation_reason=p_reason WHERE id=c.id;
  UPDATE app.service_requests SET status='cancelled' WHERE id=c.request_id;
  UPDATE app.location_shares SET revoked_at=now() WHERE contract_id=c.id AND revoked_at IS NULL;
  DELETE FROM app.professional_positions WHERE contract_id=c.id;
  INSERT INTO app.contract_events(contract_id,actor_id,event_type,to_status,metadata)
  VALUES(c.id,internal.actor(),'escrow_refunded','cancelled',jsonb_build_object('amount_minor',q.price_minor,'reason',p_reason));
END $$;

CREATE FUNCTION api.approve_escrow_refund(p_contract uuid) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor(); c app.contracts; e app.service_escrows;
BEGIN
  SELECT * INTO c FROM app.contracts WHERE id=p_contract AND professional_id=v FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'only_professional_can_approve_refund' USING ERRCODE='42501'; END IF;
  SELECT * INTO e FROM app.service_escrows WHERE contract_id=c.id;
  IF e.refund_requested_at IS NULL THEN RAISE EXCEPTION 'customer_refund_request_required'; END IF;
  PERFORM internal.refund_escrow(c.id,e.refund_reason);
END $$;

CREATE FUNCTION api.resolve_escrow_refund(p_contract uuid,p_ticket uuid,p_reason text) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.require_permission('money.refund');
BEGIN
  IF length(trim(coalesce(p_reason,'')))<10 OR NOT EXISTS(SELECT 1 FROM app.support_tickets WHERE id=p_ticket AND contract_id=p_contract)
    OR NOT EXISTS(SELECT 1 FROM app.contracts WHERE id=p_contract AND status IN('disputed','cancelled')) THEN
    RAISE EXCEPTION 'dispute_ticket_and_reason_required'; END IF;
  PERFORM internal.refund_escrow(p_contract,p_reason);
  PERFORM internal.audit('escrow.refund_resolved','contract',p_contract,jsonb_build_object('ticket_id',p_ticket,'reason',p_reason));
END $$;

-- Incidencia del proveedor: congelar y reconciliar; nunca inventar saldo ni
-- devolver automáticamente fondos de un retiro cuyo resultado es incierto.
CREATE FUNCTION api.flag_money_incident(p_wallet uuid,p_provider text,p_event text,p_reason text) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v_id uuid; old app.money_incidents;
BEGIN
  IF length(trim(coalesce(p_reason,'')))<10 THEN RAISE EXCEPTION 'reason_required'; END IF;
  PERFORM pg_advisory_xact_lock(hashtextextended('incident:'||p_provider||':'||p_event,0));
  SELECT * INTO old FROM app.money_incidents WHERE provider=p_provider AND event_id=p_event;
  IF FOUND THEN
    IF (old.wallet_id,old.reason) IS DISTINCT FROM(p_wallet,p_reason) THEN RAISE EXCEPTION 'provider_event_conflict'; END IF;
    RETURN old.id;
  END IF;
  UPDATE app.money_wallets SET status='frozen' WHERE id=p_wallet;
  IF NOT FOUND THEN RAISE EXCEPTION 'wallet_not_found'; END IF;
  INSERT INTO app.money_incidents(wallet_id,provider,event_id,reason) VALUES(p_wallet,p_provider,p_event,p_reason) RETURNING id INTO v_id;
  RETURN v_id;
END $$;

ALTER FUNCTION api.file_object_key(uuid) SET SCHEMA internal;
ALTER FUNCTION internal.file_object_key(uuid) RENAME TO base_file_object_key;
REVOKE ALL ON FUNCTION internal.base_file_object_key(uuid) FROM chamba_api;
CREATE FUNCTION api.file_object_key(p_file uuid) RETURNS text
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor(); result text;
BEGIN
  SELECT f.object_key INTO result FROM app.files f WHERE f.id=p_file AND f.moderation_status='approved' AND f.exif_removed AND f.deleted_at IS NULL
    AND EXISTS(SELECT 1 FROM app.delivery_files df JOIN app.service_deliveries d ON d.id=df.delivery_id JOIN app.contracts c ON c.id=d.contract_id
      WHERE df.file_id=f.id AND v IN(c.customer_id,c.professional_id));
  IF FOUND THEN PERFORM internal.audit('delivery_file.read','file',p_file); RETURN result; END IF;
  RETURN internal.base_file_object_key(p_file);
END $$;

CREATE FUNCTION api.payment_preferences() RETURNS jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  SELECT jsonb_build_object('requires_platform_payment',requires_platform_payment) FROM app.users WHERE id=internal.actor();
$$;

GRANT EXECUTE ON FUNCTION api.quote_contract_funding(uuid),api.fund_contract(uuid,uuid),api.escrow_details(uuid),
  api.submit_delivery(uuid,text,uuid,uuid[]),api.request_delivery_changes(uuid,text),api.release_contract_funds(uuid,uuid,uuid),
  api.request_escrow_refund(uuid,text),api.approve_escrow_refund(uuid),api.file_object_key(uuid),api.payment_preferences() TO chamba_api;
GRANT EXECUTE ON FUNCTION api.resolve_escrow_refund(uuid,uuid,text) TO chamba_admin;
GRANT EXECUTE ON FUNCTION api.flag_money_incident(uuid,text,text,text) TO chamba_billing;
