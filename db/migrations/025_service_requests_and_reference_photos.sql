-- Professional offerings remain reusable. A customer need has exactly one assignee.
ALTER TABLE app.service_requests ADD COLUMN offering_id uuid REFERENCES app.offerings;
ALTER TABLE app.service_requests ADD COLUMN offering_snapshot jsonb;
ALTER TABLE app.service_requests ADD COLUMN requires_quote boolean NOT NULL DEFAULT false;
ALTER TABLE app.service_requests ADD COLUMN creation_payload jsonb;
CREATE INDEX requests_offering_idx ON app.service_requests(offering_id) WHERE offering_id IS NOT NULL;

CREATE FUNCTION internal.create_customer_need(p_offering uuid,p_category uuid,p_delivery_mode text,p_title text,
  p_description text,p_currency text,p_address uuid,p_budget bigint,p_radius_m integer,
  p_expires_at timestamptz,p_key uuid,p_files uuid[],p_protect boolean) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor(); r app.service_requests; o app.offerings; a app.addresses;
  v_id uuid; v_payload jsonb; v_snapshot jsonb; v_target uuid; v_files uuid[];
BEGIN
  IF p_key IS NULL OR p_files IS NULL OR cardinality(p_files)>6
    OR cardinality(p_files)<>(SELECT count(DISTINCT x) FROM unnest(p_files) x) THEN
    RAISE EXCEPTION 'invalid_request_images'; END IF;
  SELECT ARRAY(SELECT x FROM unnest(p_files) x ORDER BY x) INTO v_files;
  v_payload:=jsonb_build_object('offering',p_offering,'category',p_category,'delivery_mode',p_delivery_mode,
    'title',p_title,'description',p_description,'currency',p_currency,'address',p_address,'budget',p_budget,
    'radius_m',p_radius_m,'expires_at',p_expires_at,'files',v_files,'protect',p_protect);
  PERFORM 1 FROM app.users WHERE id=v FOR UPDATE;
  SELECT * INTO r FROM app.service_requests WHERE customer_id=v AND idempotency_key=p_key;
  IF FOUND THEN
    IF r.creation_payload IS DISTINCT FROM v_payload THEN RAISE EXCEPTION 'idempotency_conflict'; END IF;
    RETURN r.id;
  END IF;
  IF p_currency IS NULL OR p_currency NOT IN('VES','USD') THEN RAISE EXCEPTION 'unsupported_currency'; END IF;
  IF length(trim(coalesce(p_description,''))) NOT BETWEEN 10 AND 10000
    OR p_expires_at IS NULL OR p_expires_at<=now() OR p_protect IS NULL THEN
    RAISE EXCEPTION 'invalid_request_details'; END IF;
  IF p_offering IS NOT NULL THEN
    SELECT * INTO o FROM app.offerings WHERE id=p_offering AND active FOR SHARE;
    IF NOT FOUND OR o.professional_id=v OR o.currency NOT IN('VES','USD') OR NOT EXISTS(
      SELECT 1 FROM app.professionals p JOIN app.users u ON u.id=p.user_id
      WHERE p.user_id=o.professional_id AND p.status='active' AND p.accepts_requests AND u.status='active') THEN
      RAISE EXCEPTION 'offering_unavailable'; END IF;
    p_category:=o.category_id; p_delivery_mode:=o.delivery_mode; v_target:=o.professional_id;
    p_title:=left(CASE WHEN length(trim(o.title))<5 THEN 'Servicio: '||o.title ELSE o.title END,160);
    p_budget:=NULL; p_radius_m:=CASE WHEN o.delivery_mode='onsite' THEN 200000 END;
    v_snapshot:=jsonb_build_object('id',o.id,'professional_id',o.professional_id,'title',o.title,
      'category_id',o.category_id,'delivery_mode',o.delivery_mode,'currency',o.currency,'price_min_minor',o.price_min_minor);
  END IF;
  IF p_delivery_mode IS NULL OR p_delivery_mode NOT IN('onsite','remote')
    OR length(trim(coalesce(p_title,''))) NOT BETWEEN 5 AND 160
    OR (p_budget IS NOT NULL AND p_budget<=0) THEN RAISE EXCEPTION 'invalid_request_details'; END IF;
  IF NOT EXISTS(SELECT 1 FROM app.categories WHERE id=p_category AND active) THEN RAISE EXCEPTION 'invalid_category'; END IF;
  IF p_delivery_mode='onsite' THEN
    p_radius_m:=coalesce(p_radius_m,10000);
    IF p_radius_m NOT BETWEEN 100 AND 200000 THEN RAISE EXCEPTION 'invalid_request_details'; END IF;
    SELECT * INTO a FROM app.addresses WHERE id=p_address AND user_id=v AND archived_at IS NULL FOR SHARE;
    IF NOT FOUND THEN RAISE EXCEPTION 'address_not_owned' USING ERRCODE='42501'; END IF;
    IF a.country_code<>'VE' THEN RAISE EXCEPTION 'venezuela_only'; END IF;
  ELSIF p_address IS NOT NULL OR p_radius_m IS NOT NULL THEN
    RAISE EXCEPTION 'remote_location_not_allowed';
  END IF;
  -- Lock selected files through the atomic creation; pending images remain private until moderation.
  PERFORM 1 FROM app.files WHERE id=ANY(v_files) FOR SHARE;
  IF EXISTS(SELECT 1 FROM unnest(v_files) x WHERE NOT EXISTS(SELECT 1 FROM app.files f
    WHERE f.id=x AND f.owner_id=v AND f.purpose='request' AND f.deleted_at IS NULL
      AND f.mime_type IN('image/jpeg','image/png','image/webp') AND f.exif_removed AND f.moderation_status<>'rejected')) THEN
    RAISE EXCEPTION 'invalid_request_images'; END IF;
  INSERT INTO app.service_requests(customer_id,category_id,mode,target_professional_id,title,description,currency,
    budget_min_minor,budget_max_minor,search_radius_m,expires_at,idempotency_key,delivery_mode,
    requires_platform_payment,offering_id,offering_snapshot,requires_quote,creation_payload)
  VALUES(v,p_category,CASE WHEN p_offering IS NULL THEN 'broadcast' ELSE 'direct' END,v_target,p_title,p_description,p_currency,
    NULL,p_budget,p_radius_m,p_expires_at,p_key,p_delivery_mode,p_delivery_mode='remote' OR p_protect,
    p_offering,v_snapshot,true,v_payload) RETURNING id INTO v_id;
  IF p_delivery_mode='onsite' THEN
    INSERT INTO app.request_locations(request_id,customer_id,source_address_id,location,address_line,instructions,country_code,city)
    VALUES(v_id,v,a.id,a.location,a.address_line,a.instructions,a.country_code,a.city);
  END IF;
  IF p_offering IS NOT NULL AND NOT internal.eligible(v_target,v_id) THEN RAISE EXCEPTION 'professional_outside_coverage'; END IF;
  INSERT INTO app.request_attachments(request_id,file_id) SELECT v_id,x FROM unnest(v_files) x;
  PERFORM internal.audit('request.created','request',v_id);
  RETURN v_id;
END $$;

CREATE FUNCTION api.request_offering(p_offering uuid,p_description text,p_currency text,p_address uuid,
  p_expires_at timestamptz,p_key uuid,p_files uuid[] DEFAULT '{}',p_protect boolean DEFAULT false) RETURNS uuid
LANGUAGE sql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  SELECT internal.create_customer_need(p_offering,NULL,NULL,NULL,p_description,p_currency,p_address,NULL,NULL,
    p_expires_at,p_key,p_files,p_protect);
$$;
CREATE FUNCTION api.create_customer_request(p_category uuid,p_delivery_mode text,p_title text,p_description text,
  p_currency text,p_address uuid,p_budget bigint,p_radius_m integer,p_expires_at timestamptz,p_key uuid,
  p_files uuid[] DEFAULT '{}',p_protect boolean DEFAULT false) RETURNS uuid
LANGUAGE sql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  SELECT internal.create_customer_need(NULL,p_category,p_delivery_mode,p_title,p_description,p_currency,p_address,
    p_budget,p_radius_m,p_expires_at,p_key,p_files,p_protect);
$$;
GRANT EXECUTE ON FUNCTION api.request_offering(uuid,text,text,uuid,timestamptz,uuid,uuid[],boolean),
  api.create_customer_request(uuid,text,text,text,text,uuid,bigint,integer,timestamptz,uuid,uuid[],boolean) TO chamba_api;

CREATE OR REPLACE FUNCTION internal.eligible(p_professional uuid,p_request uuid) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  SELECT EXISTS(SELECT 1 FROM app.service_requests r
    JOIN app.categories cat ON cat.id=r.category_id AND cat.active
    JOIN app.professionals p ON p.user_id=p_professional AND p.status='active' AND p.accepts_requests
    JOIN app.users u ON u.id=p.user_id AND u.status='active'
    WHERE r.id=p_request AND r.customer_id<>p_professional AND (r.mode='broadcast' OR r.target_professional_id=p_professional)
      AND EXISTS(SELECT 1 FROM app.offerings o WHERE o.professional_id=p_professional AND o.category_id=r.category_id
        AND o.active AND o.delivery_mode=r.delivery_mode AND (r.offering_id IS NULL OR o.id=r.offering_id))
      AND (r.delivery_mode='remote' OR EXISTS(SELECT 1 FROM app.request_locations l
        JOIN app.coverage_areas ca ON ca.professional_id=p_professional AND ca.category_id=r.category_id AND ca.active
        JOIN app.branches b ON b.id=ca.branch_id AND b.active
        WHERE l.request_id=r.id AND public.ST_DWithin(b.location,l.location,CASE WHEN r.mode='direct' THEN ca.radius_m ELSE least(ca.radius_m,r.search_radius_m) END)
          AND (ca.polygon IS NULL OR public.ST_Covers(ca.polygon,l.location)))));
$$;

CREATE OR REPLACE FUNCTION api.submit_proposal(p_request uuid,p_message text,p_price_minor bigint,p_expires_at timestamptz) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid := internal.actor(); r app.service_requests; v_id uuid; old app.proposals;
BEGIN
  SELECT * INTO r FROM app.service_requests WHERE id=p_request FOR UPDATE;
  IF NOT FOUND OR (r.mode<>'broadcast' AND NOT r.requires_quote) OR r.status<>'open' OR r.expires_at<=now() OR r.moderation_status<>'approved'
     OR NOT internal.eligible(v,p_request) THEN RAISE EXCEPTION 'request_unavailable'; END IF;
  IF r.requires_quote AND (p_price_minor IS NULL OR p_price_minor<=0) THEN RAISE EXCEPTION 'positive_offer_required'; END IF;
  SELECT * INTO old FROM app.proposals WHERE request_id=p_request AND professional_id=v;
  IF FOUND THEN
    IF (old.message,old.price_minor,old.expires_at) IS DISTINCT FROM (p_message,p_price_minor,p_expires_at) THEN
      RAISE EXCEPTION 'proposal_already_exists';
    END IF;
    RETURN old.id;
  END IF;
  IF p_expires_at>r.expires_at OR p_expires_at<=now() THEN RAISE EXCEPTION 'invalid_proposal_expiry'; END IF;
  INSERT INTO app.proposals(request_id,professional_id,message,price_minor,currency,expires_at)
  VALUES(r.id,v,p_message,p_price_minor,r.currency,p_expires_at) RETURNING id INTO v_id;
  INSERT INTO app.outbox(topic,aggregate_id,recipient_id,deduplication_key)
  VALUES('proposal.received',r.id,r.customer_id,'proposal:'||v_id);
  PERFORM internal.audit('proposal.created','proposal',v_id);
  RETURN v_id;
END $$;

CREATE OR REPLACE FUNCTION api.accept_proposal(p_proposal uuid) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid := internal.actor(); p app.proposals; r app.service_requests; c app.contracts;
BEGIN
  SELECT * INTO p FROM app.proposals WHERE id=p_proposal;
  IF NOT FOUND THEN RAISE EXCEPTION 'proposal_unavailable'; END IF;
  SELECT * INTO r FROM app.service_requests WHERE id=p.request_id FOR UPDATE;
  IF r.customer_id<>v OR (r.mode<>'broadcast' AND NOT r.requires_quote) THEN RAISE EXCEPTION 'request_not_owned' USING ERRCODE='42501'; END IF;
  IF r.mode='direct' AND p.professional_id<>r.target_professional_id THEN RAISE EXCEPTION 'proposal_unavailable'; END IF;
  SELECT * INTO c FROM app.contracts WHERE request_id=r.id;
  IF FOUND THEN
    IF c.proposal_id=p_proposal THEN RETURN c.id; END IF;
    RAISE EXCEPTION 'request_already_assigned';
  END IF;
  SELECT * INTO p FROM app.proposals WHERE id=p_proposal FOR UPDATE;
  IF p.status<>'pending' OR p.expires_at<=now() THEN RAISE EXCEPTION 'proposal_unavailable'; END IF;
  RETURN internal.make_contract(r.id,p.professional_id,p.id,p.price_minor);
END $$;

CREATE OR REPLACE FUNCTION api.accept_direct_request(p_request uuid) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid := internal.actor(); r app.service_requests; c app.contracts;
BEGIN
  SELECT * INTO r FROM app.service_requests WHERE id=p_request FOR UPDATE;
  IF NOT FOUND OR r.mode<>'direct' OR r.target_professional_id<>v THEN RAISE EXCEPTION 'not_target_professional' USING ERRCODE='42501'; END IF;
  SELECT * INTO c FROM app.contracts WHERE request_id=p_request;
  IF FOUND THEN RETURN c.id; END IF;
  IF r.requires_quote THEN RAISE EXCEPTION 'customer_quote_confirmation_required'; END IF;
  -- En directo el cliente consiente al publicar. El presupuesto no es un precio pactado.
  RETURN internal.make_contract(r.id,v,NULL,NULL);
END $$;

CREATE OR REPLACE FUNCTION api.service_feed(p_delivery_mode text,p_limit integer DEFAULT 30,p_offset integer DEFAULT 0) RETURNS SETOF jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor();
BEGIN
  IF p_delivery_mode IS NULL OR p_delivery_mode NOT IN('onsite','remote') OR p_limit IS NULL OR p_limit NOT BETWEEN 1 AND 100
    OR p_offset IS NULL OR p_offset<0 THEN RAISE EXCEPTION 'invalid_feed_filters'; END IF;
  RETURN QUERY SELECT jsonb_build_object('request_id',r.id,'category_id',r.category_id,'title',r.title,'description',r.description,
    'delivery_mode',r.delivery_mode,'mode',r.mode,'requires_quote',r.requires_quote,'offering_id',r.offering_id,'currency',r.currency,'offered_price_minor',r.offered_price_minor,
    'budget_min_minor',r.budget_min_minor,'budget_max_minor',r.budget_max_minor,'expires_at',r.expires_at,
    'requires_platform_payment',r.requires_platform_payment OR EXISTS(SELECT 1 FROM app.users u WHERE u.id=r.customer_id AND u.requires_platform_payment),
    'attachments',coalesce((SELECT jsonb_agg(jsonb_build_object('file_id',f.id,'mime_type',f.mime_type)) FROM app.request_attachments ra
      JOIN app.files f ON f.id=ra.file_id WHERE ra.request_id=r.id AND f.moderation_status='approved' AND f.exif_removed AND f.deleted_at IS NULL),'[]'::jsonb))
  FROM app.service_requests r WHERE r.delivery_mode=p_delivery_mode AND r.status='open' AND r.moderation_status='approved'
    AND r.expires_at>now() AND internal.eligible(v,r.id) ORDER BY r.created_at DESC,r.id LIMIT p_limit OFFSET p_offset;
END $$;

CREATE OR REPLACE FUNCTION api.my_requests() RETURNS SETOF jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  SELECT (to_jsonb(r)-'creation_payload')||jsonb_build_object(
    'contract_id',(SELECT c.id FROM app.contracts c WHERE c.request_id=r.id),
    'attachments',coalesce((SELECT jsonb_agg(jsonb_build_object('file_id',f.id,'mime_type',f.mime_type,
      'moderation_status',f.moderation_status) ORDER BY f.created_at,f.id)
      FROM app.request_attachments ra JOIN app.files f ON f.id=ra.file_id
      WHERE ra.request_id=r.id AND f.deleted_at IS NULL),'[]'::jsonb))
  FROM app.service_requests r WHERE customer_id=internal.actor() ORDER BY created_at DESC LIMIT 200;
$$;

CREATE FUNCTION api.remove_request_image(p_request uuid,p_file uuid) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
BEGIN
  PERFORM 1 FROM app.service_requests WHERE id=p_request AND customer_id=internal.actor() AND status='draft' FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'request_not_editable'; END IF;
  DELETE FROM app.request_attachments WHERE request_id=p_request AND file_id=p_file;
  UPDATE app.service_requests SET moderation_status='pending' WHERE id=p_request;
END $$;
GRANT EXECUTE ON FUNCTION api.remove_request_image(uuid,uuid) TO chamba_api;

CREATE OR REPLACE FUNCTION api.attach_request_file(p_request uuid,p_file uuid) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor();
BEGIN
  PERFORM 1 FROM app.service_requests WHERE id=p_request AND customer_id=v AND status='draft' FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'request_not_editable'; END IF;
  IF NOT EXISTS(SELECT 1 FROM app.request_attachments WHERE request_id=p_request AND file_id=p_file)
    AND (SELECT count(*) FROM app.request_attachments WHERE request_id=p_request)>=6 THEN
    RAISE EXCEPTION 'invalid_request_images'; END IF;
  IF NOT EXISTS(SELECT 1 FROM app.files WHERE id=p_file AND owner_id=v AND purpose='request' AND deleted_at IS NULL
    AND mime_type IN('image/jpeg','image/png','image/webp') AND exif_removed AND moderation_status<>'rejected') THEN
    RAISE EXCEPTION 'file_not_owned' USING ERRCODE='42501'; END IF;
  INSERT INTO app.request_attachments(request_id,file_id) VALUES(p_request,p_file) ON CONFLICT DO NOTHING;
  UPDATE app.service_requests SET moderation_status='pending' WHERE id=p_request;
END $$;
