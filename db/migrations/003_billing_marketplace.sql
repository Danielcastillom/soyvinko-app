CREATE FUNCTION api.start_purchase(p_plan uuid,p_key uuid) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor(); p app.plans; old app.credit_purchases; v_id uuid;
BEGIN
  PERFORM 1 FROM app.professionals WHERE user_id=v AND status='active' FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'professional_required'; END IF;
  SELECT * INTO old FROM app.credit_purchases WHERE professional_id=v AND idempotency_key=p_key;
  IF FOUND THEN
    IF old.plan_id<>p_plan THEN RAISE EXCEPTION 'idempotency_conflict'; END IF;
    RETURN old.id;
  END IF;
  SELECT * INTO p FROM app.plans WHERE id=p_plan AND active;
  IF NOT FOUND THEN RAISE EXCEPTION 'plan_unavailable'; END IF;
  INSERT INTO app.credit_purchases(professional_id,plan_id,idempotency_key,credits,amount_minor,currency,
    positioning_weight,positioning_days,marketplace_days)
  VALUES(v,p.id,p_key,p.credits,p.price_minor,p.currency,p.positioning_weight,p.positioning_days,p.marketplace_days) RETURNING id INTO v_id;
  PERFORM internal.audit('purchase.created','purchase',v_id);
  RETURN v_id;
END $$;

-- Solo chamba_billing: el backend verifica firma, estado e importe con la pasarela.
-- Una respuesta del navegador nunca puede acreditar créditos.
CREATE FUNCTION api.confirm_purchase(p_purchase uuid,p_provider text,p_event text,p_payment text,
  p_amount bigint,p_currency text,p_payload_hash text) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE p app.credit_purchases; e app.payment_events;
BEGIN
  SELECT * INTO p FROM app.credit_purchases WHERE id=p_purchase FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'purchase_not_found'; END IF;
  SELECT * INTO e FROM app.payment_events WHERE provider=p_provider AND external_event_id=p_event;
  IF FOUND THEN
    IF (e.purchase_id,e.external_payment_id,e.amount_minor,e.currency,e.payload_hash)
      IS DISTINCT FROM (p_purchase,p_payment,p_amount,p_currency::char(3),p_payload_hash) THEN RAISE EXCEPTION 'payment_event_conflict'; END IF;
    RETURN p.id;
  END IF;
  IF p_amount IS DISTINCT FROM p.amount_minor OR p_currency IS DISTINCT FROM p.currency::text THEN RAISE EXCEPTION 'payment_amount_mismatch'; END IF;
  IF p.status='paid' THEN
    IF NOT EXISTS(SELECT 1 FROM app.payment_events WHERE purchase_id=p.id AND provider=p_provider AND external_payment_id=p_payment AND event_type='captured') THEN
      RAISE EXCEPTION 'purchase_already_paid_by_another_payment';
    END IF;
    INSERT INTO app.payment_events(provider,external_event_id,external_payment_id,purchase_id,event_type,amount_minor,currency,payload_hash)
    VALUES(p_provider,p_event,p_payment,p.id,'capture_duplicate',p_amount,p_currency,p_payload_hash);
    RETURN p.id;
  END IF;
  IF p.status<>'pending' THEN RAISE EXCEPTION 'purchase_not_payable'; END IF;
  INSERT INTO app.payment_events(provider,external_event_id,external_payment_id,purchase_id,event_type,amount_minor,currency,payload_hash)
  VALUES(p_provider,p_event,p_payment,p.id,'captured',p_amount,p_currency,p_payload_hash);
  IF p.credits>0 THEN
    PERFORM internal.post_credit(p.professional_id,p.credits,'purchase','purchase:'||p.id,'Compra de créditos confirmada',NULL,p.id);
  END IF;
  UPDATE app.credit_purchases SET status='paid',paid_at=now() WHERE id=p.id;
  IF p.positioning_days>0 THEN
    INSERT INTO app.entitlements(professional_id,purchase_id,kind,weight,starts_at,ends_at)
    VALUES(p.professional_id,p.id,'positioning',p.positioning_weight,now(),now()+make_interval(days=>p.positioning_days));
  END IF;
  IF p.marketplace_days>0 THEN
    INSERT INTO app.entitlements(professional_id,purchase_id,kind,starts_at,ends_at)
    VALUES(p.professional_id,p.id,'marketplace',now(),now()+make_interval(days=>p.marketplace_days));
  END IF;
  PERFORM internal.audit('purchase.paid','purchase',p.id);
  RETURN p.id;
END $$;

CREATE FUNCTION api.refund_contract_credit(p_contract uuid,p_ticket uuid,p_reason text) RETURNS bigint
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.require_permission('credits.refund'); c app.contracts; l app.credit_ledger;
BEGIN
  SELECT * INTO c FROM app.contracts WHERE id=p_contract FOR UPDATE;
  IF NOT FOUND OR c.status NOT IN('cancelled','disputed') THEN RAISE EXCEPTION 'refund_not_allowed'; END IF;
  IF length(trim(coalesce(p_reason,'')))<10 OR NOT EXISTS(SELECT 1 FROM app.support_tickets WHERE id=p_ticket AND contract_id=c.id) THEN
    RAISE EXCEPTION 'related_ticket_and_reason_required';
  END IF;
  SELECT * INTO l FROM app.credit_ledger WHERE contract_id=c.id AND kind='contract_debit';
  IF NOT FOUND THEN RAISE EXCEPTION 'debit_not_found'; END IF;
  PERFORM internal.audit('credit.refunded','contract',c.id,jsonb_build_object('ticket_id',p_ticket,'reason',p_reason));
  RETURN internal.post_credit(c.professional_id,1,'contract_refund','refund:'||c.id,p_reason,c.id,NULL,l.id);
END $$;

CREATE FUNCTION internal.has_entitlement(p_professional uuid,p_kind text) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  SELECT EXISTS(SELECT 1 FROM app.entitlements WHERE professional_id=p_professional AND kind=p_kind
                AND revoked_at IS NULL AND starts_at<=now() AND ends_at>now());
$$;

CREATE FUNCTION api.activate_store(p_slug text,p_name text,p_description text DEFAULT '') RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor(); v_id uuid;
BEGIN
  PERFORM 1 FROM app.professionals WHERE user_id=v AND status='active' FOR UPDATE;
  IF NOT FOUND OR NOT internal.has_entitlement(v,'marketplace') THEN RAISE EXCEPTION 'marketplace_entitlement_required'; END IF;
  IF EXISTS(SELECT 1 FROM app.stores WHERE professional_id=v AND status='suspended') THEN RAISE EXCEPTION 'store_suspended'; END IF;
  INSERT INTO app.stores(professional_id,slug,name,description,status) VALUES(v,p_slug,p_name,p_description,'active')
  ON CONFLICT(professional_id) DO UPDATE SET status='active',name=EXCLUDED.name,description=EXCLUDED.description RETURNING id INTO v_id;
  PERFORM internal.audit('store.activated','store',v_id);
  RETURN v_id;
END $$;

CREATE FUNCTION api.add_product(p_store uuid,p_sku text,p_name text,p_description text,p_price bigint,p_currency text,p_stock integer) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor(); v_id uuid;
BEGIN
  IF NOT EXISTS(SELECT 1 FROM app.stores WHERE id=p_store AND professional_id=v AND status='active')
    OR NOT internal.has_entitlement(v,'marketplace') THEN RAISE EXCEPTION 'store_access_denied' USING ERRCODE='42501'; END IF;
  INSERT INTO app.products(store_id,sku,name,description,price_minor,currency,stock,status)
  VALUES(p_store,p_sku,p_name,p_description,p_price,p_currency,p_stock,'active') RETURNING id INTO v_id;
  IF p_stock>0 THEN
    INSERT INTO app.inventory_movements(product_id,delta,reason,actor_id) VALUES(v_id,p_stock,'initial_stock',v);
  END IF;
  PERFORM internal.audit('product.created','product',v_id);
  RETURN v_id;
END $$;

CREATE FUNCTION api.create_order(p_store uuid,p_items jsonb,p_key uuid) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor(); s app.stores; pr app.products; old app.orders;
  item record; v_id uuid:=gen_random_uuid(); v_total bigint:=0; v_currency char(3); v_canonical jsonb; v_existing jsonb;
BEGIN
  PERFORM 1 FROM app.users WHERE id=v FOR UPDATE;
  IF jsonb_typeof(p_items) IS DISTINCT FROM 'array' THEN RAISE EXCEPTION 'invalid_order_items'; END IF;
  IF jsonb_array_length(p_items) NOT BETWEEN 1 AND 100 THEN RAISE EXCEPTION 'invalid_order_items'; END IF;
  IF EXISTS(SELECT 1 FROM jsonb_array_elements(p_items) x
    WHERE jsonb_typeof(x->'quantity') IS DISTINCT FROM 'number' OR coalesce(x->>'quantity','') !~ '^[1-9][0-9]{0,5}$'
      OR x->>'product_id' IS NULL) THEN RAISE EXCEPTION 'invalid_quantity'; END IF;
  IF (SELECT count(*) FROM jsonb_to_recordset(p_items) AS x(product_id uuid,quantity integer))<>
    (SELECT count(DISTINCT product_id) FROM jsonb_to_recordset(p_items) AS x(product_id uuid,quantity integer)) THEN
    RAISE EXCEPTION 'duplicate_product';
  END IF;
  SELECT jsonb_agg(jsonb_build_object('product_id',product_id,'quantity',quantity) ORDER BY product_id)
    INTO v_canonical FROM jsonb_to_recordset(p_items) AS x(product_id uuid,quantity integer);
  SELECT * INTO old FROM app.orders WHERE customer_id=v AND idempotency_key=p_key;
  IF FOUND THEN
    SELECT jsonb_agg(jsonb_build_object('product_id',product_id,'quantity',quantity) ORDER BY product_id)
      INTO v_existing FROM app.order_items WHERE order_id=old.id;
    IF old.store_id<>p_store OR v_existing IS DISTINCT FROM v_canonical THEN RAISE EXCEPTION 'idempotency_conflict'; END IF;
    RETURN old.id;
  END IF;
  SELECT * INTO s FROM app.stores WHERE id=p_store AND status='active';
  IF NOT FOUND OR s.professional_id=v OR NOT internal.has_entitlement(s.professional_id,'marketplace')
    OR NOT EXISTS(SELECT 1 FROM app.professionals p JOIN app.users u ON u.id=p.user_id
                  WHERE p.user_id=s.professional_id AND p.status='active' AND u.status='active') THEN RAISE EXCEPTION 'store_unavailable'; END IF;
  -- Orden constante al bloquear productos: evita interbloqueos entre carritos.
  FOR item IN SELECT * FROM jsonb_to_recordset(p_items) AS x(product_id uuid,quantity integer) ORDER BY product_id LOOP
    SELECT * INTO pr FROM app.products WHERE id=item.product_id AND store_id=p_store AND status='active' FOR UPDATE;
    IF NOT FOUND OR pr.stock<item.quantity THEN RAISE EXCEPTION 'insufficient_stock'; END IF;
    IF v_currency IS NOT NULL AND v_currency<>pr.currency THEN RAISE EXCEPTION 'mixed_currencies'; END IF;
    v_currency:=pr.currency;
    v_total:=v_total+pr.price_minor*item.quantity;
  END LOOP;
  INSERT INTO app.orders(id,customer_id,store_id,total_minor,currency,idempotency_key) VALUES(v_id,v,p_store,v_total,v_currency,p_key);
  FOR item IN SELECT * FROM jsonb_to_recordset(p_items) AS x(product_id uuid,quantity integer) ORDER BY product_id LOOP
    SELECT * INTO pr FROM app.products WHERE id=item.product_id;
    INSERT INTO app.order_items(order_id,product_id,sku_snapshot,name_snapshot,quantity,unit_price_minor)
    VALUES(v_id,pr.id,pr.sku,pr.name,item.quantity,pr.price_minor);
    UPDATE app.products SET stock=stock-item.quantity,updated_at=now() WHERE id=pr.id;
    INSERT INTO app.inventory_movements(product_id,order_id,delta,reason,actor_id) VALUES(pr.id,v_id,-item.quantity,'order_reserved',v);
  END LOOP;
  INSERT INTO app.order_events(order_id,actor_id,event_type) VALUES(v_id,v,'created');
  INSERT INTO app.outbox(topic,aggregate_id,recipient_id,deduplication_key) VALUES('order.created',v_id,s.professional_id,'order:'||v_id);
  PERFORM internal.audit('order.created','order',v_id);
  RETURN v_id;
END $$;

CREATE FUNCTION api.advance_order(p_order uuid,p_action text) RETURNS text
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor(); o app.orders; v_seller uuid; v_status text; item record;
BEGIN
  SELECT * INTO o FROM app.orders WHERE id=p_order FOR UPDATE;
  SELECT professional_id INTO v_seller FROM app.stores WHERE id=o.store_id;
  IF o.id IS NULL OR v NOT IN(o.customer_id,v_seller) THEN RAISE EXCEPTION 'order_access_denied' USING ERRCODE='42501'; END IF;
  IF p_action='cancel' THEN
    IF o.status='cancelled' THEN RETURN o.status; END IF;
    IF o.status NOT IN('pending','accepted') THEN RAISE EXCEPTION 'open_dispute_instead'; END IF;
    FOR item IN SELECT * FROM app.order_items WHERE order_id=o.id ORDER BY product_id LOOP
      UPDATE app.products SET stock=stock+item.quantity,updated_at=now() WHERE id=item.product_id;
      INSERT INTO app.inventory_movements(product_id,order_id,delta,reason,actor_id) VALUES(item.product_id,o.id,item.quantity,'order_cancelled',v);
    END LOOP;
    v_status:='cancelled';
  ELSIF p_action='accept' AND o.status='pending' AND v=v_seller THEN v_status:='accepted';
  ELSIF p_action='dispatch' AND o.status='accepted' AND v=v_seller THEN v_status:='dispatched';
  ELSIF p_action='receive' AND o.status IN('accepted','dispatched') AND v=o.customer_id THEN v_status:='delivered';
  ELSE RAISE EXCEPTION 'invalid_order_transition'; END IF;
  UPDATE app.orders SET status=v_status WHERE id=o.id;
  INSERT INTO app.order_events(order_id,actor_id,event_type,metadata) VALUES(o.id,v,p_action,jsonb_build_object('from',o.status,'to',v_status));
  PERFORM internal.audit('order.'||p_action,'order',o.id);
  RETURN v_status;
END $$;

CREATE FUNCTION api.open_ticket(p_category text,p_subject text,p_description text,p_contract uuid DEFAULT NULL,p_order uuid DEFAULT NULL) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor(); v_id uuid;
BEGIN
  IF p_contract IS NOT NULL THEN PERFORM internal.participant(p_contract); END IF;
  IF p_order IS NOT NULL AND NOT EXISTS(SELECT 1 FROM app.orders o JOIN app.stores s ON s.id=o.store_id
    WHERE o.id=p_order AND v IN(o.customer_id,s.professional_id)) THEN RAISE EXCEPTION 'order_access_denied' USING ERRCODE='42501'; END IF;
  INSERT INTO app.support_tickets(opened_by,category,subject,description,contract_id,order_id)
  VALUES(v,p_category,p_subject,p_description,p_contract,p_order) RETURNING id INTO v_id;
  PERFORM internal.audit('ticket.opened','ticket',v_id);
  RETURN v_id;
END $$;

CREATE FUNCTION api.dispute_contract(p_contract uuid,p_reason text) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor(); c app.contracts; v_ticket uuid;
BEGIN
  SELECT * INTO c FROM app.contracts WHERE id=p_contract AND v IN(customer_id,professional_id) FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'contract_access_denied' USING ERRCODE='42501'; END IF;
  IF length(trim(coalesce(p_reason,'')))<10 OR c.status='disputed' THEN RAISE EXCEPTION 'invalid_dispute'; END IF;
  v_ticket:=api.open_ticket('service','Disputa del servicio',p_reason,c.id,NULL);
  UPDATE app.contracts SET status='disputed' WHERE id=c.id;
  UPDATE app.location_shares SET revoked_at=now() WHERE contract_id=c.id AND revoked_at IS NULL;
  DELETE FROM app.professional_positions WHERE contract_id=c.id;
  INSERT INTO app.contract_events(contract_id,actor_id,event_type,from_status,to_status,metadata)
  VALUES(c.id,v,'dispute',c.status,'disputed',jsonb_build_object('ticket_id',v_ticket));
  RETURN v_ticket;
END $$;
