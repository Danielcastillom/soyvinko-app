CREATE FUNCTION internal.actor() RETURNS uuid
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid;
BEGIN
  v := nullif(current_setting('app.user_id',true),'')::uuid;
  IF v IS NULL OR NOT EXISTS(SELECT 1 FROM app.users WHERE id=v AND status='active') THEN
    RAISE EXCEPTION 'authentication_required' USING ERRCODE='42501';
  END IF;
  RETURN v;
END $$;

CREATE FUNCTION internal.require_permission(p_permission text) RETURNS uuid
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid := internal.actor();
BEGIN
  IF NOT EXISTS(SELECT 1 FROM app.user_roles ur JOIN app.role_permissions rp ON rp.role_code=ur.role_code
                WHERE ur.user_id=v AND rp.permission_code=p_permission) THEN
    RAISE EXCEPTION 'permission_denied: %',p_permission USING ERRCODE='42501';
  END IF;
  RETURN v;
END $$;

CREATE FUNCTION internal.audit(p_action text,p_type text,p_id uuid,p_metadata jsonb DEFAULT '{}') RETURNS void
LANGUAGE sql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  INSERT INTO app.audit_events(actor_id,action,entity_type,entity_id,correlation_id,metadata)
  VALUES(nullif(current_setting('app.user_id',true),'')::uuid,p_action,p_type,p_id,
         nullif(current_setting('app.correlation_id',true),''),p_metadata);
$$;

CREATE FUNCTION internal.post_credit(p_professional uuid,p_delta bigint,p_kind text,p_key text,p_reason text,
  p_contract uuid DEFAULT NULL,p_purchase uuid DEFAULT NULL,p_reverses bigint DEFAULT NULL) RETURNS bigint
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v_balance bigint; v_old app.credit_ledger;
BEGIN
  SELECT balance INTO v_balance FROM app.credit_wallets WHERE professional_id=p_professional FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'wallet_not_found'; END IF;
  SELECT * INTO v_old FROM app.credit_ledger WHERE idempotency_key=p_key;
  IF FOUND THEN
    IF (v_old.professional_id,v_old.delta,v_old.kind,v_old.contract_id,v_old.purchase_id,v_old.reverses_id)
      IS DISTINCT FROM (p_professional,p_delta,p_kind,p_contract,p_purchase,p_reverses) THEN
      RAISE EXCEPTION 'idempotency_conflict';
    END IF;
    RETURN v_old.balance_after;
  END IF;
  IF p_delta=0 OR v_balance+p_delta<0 THEN RAISE EXCEPTION 'insufficient_credits'; END IF;
  UPDATE app.credit_wallets SET balance=balance+p_delta,updated_at=now() WHERE professional_id=p_professional
    RETURNING balance INTO v_balance;
  INSERT INTO app.credit_ledger(professional_id,delta,balance_after,kind,contract_id,purchase_id,reverses_id,idempotency_key,actor_id,reason)
  VALUES(p_professional,p_delta,v_balance,p_kind,p_contract,p_purchase,p_reverses,p_key,
         nullif(current_setting('app.user_id',true),'')::uuid,p_reason);
  RETURN v_balance;
END $$;

CREATE FUNCTION api.register_user(p_email text,p_name text,p_argon2id_hash text) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid;
BEGIN
  INSERT INTO app.users(email,display_name) VALUES(lower(trim(p_email)),trim(p_name)) RETURNING id INTO v;
  INSERT INTO app.credentials(user_id,password_hash) VALUES(v,p_argon2id_hash);
  INSERT INTO app.user_roles(user_id,role_code) VALUES(v,'customer');
  PERFORM internal.audit('user.registered','user',v);
  RETURN v;
END $$;

CREATE FUNCTION api.activate_professional(p_slug text,p_business_name text,p_bio text DEFAULT '') RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid := internal.actor();
BEGIN
  -- El bloqueo del usuario serializa dos activaciones simultáneas.
  PERFORM 1 FROM app.users WHERE id=v FOR UPDATE;
  IF EXISTS(SELECT 1 FROM app.professionals WHERE user_id=v) THEN RETURN v; END IF;
  INSERT INTO app.professionals(user_id,slug,business_name,bio) VALUES(v,p_slug,p_business_name,p_bio);
  INSERT INTO app.user_roles(user_id,role_code) VALUES(v,'professional');
  INSERT INTO app.credit_wallets(professional_id) VALUES(v);
  PERFORM internal.post_credit(v,3,'welcome','welcome:'||v,'Activación inicial de perfil profesional');
  PERFORM internal.audit('professional.activated','professional',v);
  RETURN v;
END $$;

CREATE FUNCTION api.save_contact(p_kind text,p_label text,p_value text,p_share boolean DEFAULT true) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid := internal.actor(); v_id uuid;
BEGIN
  INSERT INTO app.user_contacts(user_id,kind,label,value,share_on_contract) VALUES(v,p_kind,p_label,p_value,p_share)
  ON CONFLICT(user_id,kind,value) DO UPDATE SET label=EXCLUDED.label,share_on_contract=EXCLUDED.share_on_contract
  RETURNING id INTO v_id;
  RETURN v_id;
END $$;

CREATE FUNCTION internal.point(p_lon double precision,p_lat double precision) RETURNS public.geography
LANGUAGE plpgsql IMMUTABLE SET search_path=pg_catalog,public,pg_temp AS $$
BEGIN
  IF p_lon IS NULL OR p_lat IS NULL OR NOT(p_lon BETWEEN -180 AND 180 AND p_lat BETWEEN -90 AND 90) THEN
    RAISE EXCEPTION 'invalid_coordinates';
  END IF;
  RETURN public.ST_SetSRID(public.ST_MakePoint(p_lon,p_lat),4326)::public.geography;
END $$;

CREATE FUNCTION api.add_address(p_label text,p_country text,p_city text,p_line text,p_lon double precision,
  p_lat double precision,p_instructions text DEFAULT NULL) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v_id uuid;
BEGIN
  INSERT INTO app.addresses(user_id,label,country_code,city,address_line,location,instructions)
  VALUES(internal.actor(),p_label,p_country,p_city,p_line,internal.point(p_lon,p_lat),p_instructions) RETURNING id INTO v_id;
  RETURN v_id;
END $$;

CREATE FUNCTION api.add_branch(p_name text,p_country text,p_city text,p_line text,p_lon double precision,
  p_lat double precision,p_public_address boolean DEFAULT false) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v_id uuid;
BEGIN
  INSERT INTO app.branches(professional_id,name,country_code,city,address_line,location,public_address)
  VALUES(internal.actor(),p_name,p_country,p_city,p_line,internal.point(p_lon,p_lat),p_public_address) RETURNING id INTO v_id;
  RETURN v_id;
END $$;

CREATE FUNCTION api.add_offering(p_category uuid,p_title text,p_currency text,p_price_minor bigint DEFAULT NULL,
  p_description text DEFAULT '') RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v_id uuid;
BEGIN
  IF NOT EXISTS(SELECT 1 FROM app.categories WHERE id=p_category AND active) THEN RAISE EXCEPTION 'invalid_category'; END IF;
  INSERT INTO app.offerings(professional_id,category_id,title,currency,price_min_minor,pricing_type,description)
  VALUES(internal.actor(),p_category,p_title,p_currency,p_price_minor,
         CASE WHEN p_price_minor IS NULL THEN 'quote' ELSE 'from' END,p_description) RETURNING id INTO v_id;
  RETURN v_id;
END $$;

CREATE FUNCTION api.set_coverage(p_branch uuid,p_category uuid,p_radius_m integer) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid := internal.actor(); v_id uuid;
BEGIN
  IF NOT EXISTS(SELECT 1 FROM app.branches WHERE id=p_branch AND professional_id=v AND active) THEN
    RAISE EXCEPTION 'branch_not_owned' USING ERRCODE='42501';
  END IF;
  INSERT INTO app.coverage_areas(professional_id,branch_id,category_id,radius_m) VALUES(v,p_branch,p_category,p_radius_m)
  ON CONFLICT(branch_id,category_id) DO UPDATE SET radius_m=EXCLUDED.radius_m,active=true RETURNING id INTO v_id;
  RETURN v_id;
END $$;

CREATE FUNCTION internal.eligible(p_professional uuid,p_request uuid) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  SELECT EXISTS (
    SELECT 1 FROM app.service_requests r JOIN app.request_locations l ON l.request_id=r.id
    JOIN app.coverage_areas c ON c.category_id=r.category_id AND c.professional_id=p_professional AND c.active
    JOIN app.branches b ON b.id=c.branch_id AND b.active
    JOIN app.professionals p ON p.user_id=c.professional_id AND p.status='active' AND p.accepts_requests
    JOIN app.users u ON u.id=p.user_id AND u.status='active'
    JOIN app.categories cat ON cat.id=r.category_id AND cat.active
    WHERE r.id=p_request AND r.customer_id<>p_professional
      AND (r.mode='broadcast' OR r.target_professional_id=p_professional)
      AND public.ST_DWithin(b.location,l.location,least(c.radius_m,r.search_radius_m))
      AND (c.polygon IS NULL OR public.ST_Covers(c.polygon,l.location))
      AND EXISTS(SELECT 1 FROM app.offerings o WHERE o.professional_id=p_professional AND o.category_id=r.category_id AND o.active)
  );
$$;

CREATE FUNCTION api.create_request(p_category uuid,p_mode text,p_target uuid,p_title text,p_description text,
  p_address uuid,p_radius_m integer,p_currency text,p_budget_min bigint,p_budget_max bigint,
  p_expires_at timestamptz,p_key uuid) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid := internal.actor(); v_id uuid; a app.addresses; r app.service_requests;
BEGIN
  PERFORM 1 FROM app.users WHERE id=v FOR UPDATE;
  SELECT * INTO r FROM app.service_requests WHERE customer_id=v AND idempotency_key=p_key;
  IF FOUND THEN
    IF (r.category_id,r.mode,r.target_professional_id,r.title,r.description,r.search_radius_m,r.currency,r.budget_min_minor,r.budget_max_minor,r.expires_at)
      IS DISTINCT FROM (p_category,p_mode,p_target,p_title,p_description,p_radius_m,p_currency::char(3),p_budget_min,p_budget_max,p_expires_at)
      OR NOT EXISTS(SELECT 1 FROM app.request_locations WHERE request_id=r.id AND source_address_id=p_address) THEN
      RAISE EXCEPTION 'idempotency_conflict';
    END IF;
    RETURN r.id;
  END IF;
  SELECT * INTO a FROM app.addresses WHERE id=p_address AND user_id=v AND archived_at IS NULL;
  IF NOT FOUND THEN RAISE EXCEPTION 'address_not_owned' USING ERRCODE='42501'; END IF;
  IF NOT EXISTS(SELECT 1 FROM app.categories WHERE id=p_category AND active) THEN RAISE EXCEPTION 'invalid_category'; END IF;
  INSERT INTO app.service_requests(customer_id,category_id,mode,target_professional_id,title,description,currency,
    budget_min_minor,budget_max_minor,search_radius_m,expires_at,idempotency_key)
  VALUES(v,p_category,p_mode,p_target,p_title,p_description,p_currency,p_budget_min,p_budget_max,p_radius_m,p_expires_at,p_key)
  RETURNING id INTO v_id;
  INSERT INTO app.request_locations(request_id,customer_id,source_address_id,location,address_line,instructions,country_code,city)
  VALUES(v_id,v,a.id,a.location,a.address_line,a.instructions,a.country_code,a.city);
  PERFORM internal.audit('request.created','request',v_id);
  RETURN v_id;
END $$;

CREATE FUNCTION api.publish_request(p_request uuid) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid := internal.actor(); r app.service_requests;
BEGIN
  SELECT * INTO r FROM app.service_requests WHERE id=p_request AND customer_id=v FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'request_not_owned' USING ERRCODE='42501'; END IF;
  IF r.status='open' THEN RETURN; END IF;
  IF r.status<>'draft' OR r.moderation_status<>'approved' OR r.expires_at<=now() THEN RAISE EXCEPTION 'request_not_publishable'; END IF;
  IF EXISTS(SELECT 1 FROM app.request_attachments ra JOIN app.files f ON f.id=ra.file_id
            WHERE ra.request_id=r.id AND (f.owner_id<>v OR f.purpose<>'request' OR f.moderation_status<>'approved' OR NOT f.exif_removed OR f.deleted_at IS NOT NULL)) THEN
    RAISE EXCEPTION 'unsafe_attachment';
  END IF;
  IF r.mode='direct' AND NOT internal.eligible(r.target_professional_id,r.id) THEN RAISE EXCEPTION 'professional_outside_coverage'; END IF;
  UPDATE app.service_requests SET status='open' WHERE id=r.id;
  -- Solo identificador opaco en la notificación: el feed vuelve a autorizar.
  INSERT INTO app.outbox(topic,aggregate_id,recipient_id,payload,deduplication_key)
  SELECT 'request.available',r.id,p.user_id,jsonb_build_object('request_id',r.id),'request:'||r.id||':'||p.user_id
  FROM app.professionals p WHERE internal.eligible(p.user_id,r.id)
  ON CONFLICT(deduplication_key) DO NOTHING;
  PERFORM internal.audit('request.published','request',r.id);
END $$;

CREATE FUNCTION api.submit_proposal(p_request uuid,p_message text,p_price_minor bigint,p_expires_at timestamptz) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid := internal.actor(); r app.service_requests; v_id uuid; old app.proposals;
BEGIN
  SELECT * INTO r FROM app.service_requests WHERE id=p_request FOR UPDATE;
  IF NOT FOUND OR r.mode<>'broadcast' OR r.status<>'open' OR r.expires_at<=now() OR r.moderation_status<>'approved'
     OR NOT internal.eligible(v,p_request) THEN RAISE EXCEPTION 'request_unavailable'; END IF;
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

CREATE FUNCTION internal.make_contract(p_request uuid,p_professional uuid,p_proposal uuid,p_price bigint) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE r app.service_requests; v_id uuid;
BEGIN
  -- Llamada solo desde aceptación autorizada. El bloqueo evita doble adjudicación.
  SELECT * INTO r FROM app.service_requests WHERE id=p_request FOR UPDATE;
  IF r.status<>'open' OR r.expires_at<=now() OR r.moderation_status<>'approved' THEN RAISE EXCEPTION 'request_unavailable'; END IF;
  IF NOT internal.eligible(p_professional,p_request) THEN RAISE EXCEPTION 'professional_unavailable'; END IF;
  IF NOT EXISTS(SELECT 1 FROM app.users WHERE id=r.customer_id AND status='active') THEN RAISE EXCEPTION 'customer_unavailable'; END IF;
  INSERT INTO app.contracts(request_id,customer_id,professional_id,proposal_id,agreed_price_minor,currency,customer_accepted_at,professional_accepted_at)
  VALUES(r.id,r.customer_id,p_professional,p_proposal,p_price,r.currency,now(),now()) RETURNING id INTO v_id;
  PERFORM internal.post_credit(p_professional,-1,'contract_debit','contract:'||v_id,'Contratación aceptada por ambas partes',v_id);
  UPDATE app.service_requests SET status='matched' WHERE id=r.id;
  UPDATE app.proposals SET status=CASE WHEN id=p_proposal THEN 'accepted' ELSE 'rejected' END,
    customer_accepted_at=CASE WHEN id=p_proposal THEN now() END WHERE request_id=r.id AND status='pending';
  INSERT INTO app.conversations(contract_id) VALUES(v_id);
  INSERT INTO app.contract_events(contract_id,actor_id,event_type,to_status) VALUES(v_id,internal.actor(),'mutual_acceptance','accepted');
  INSERT INTO app.outbox(topic,aggregate_id,recipient_id,deduplication_key)
  SELECT 'contract.accepted',v_id,x,'contract:'||v_id||':'||x FROM unnest(ARRAY[r.customer_id,p_professional]) AS x;
  PERFORM internal.audit('contract.accepted','contract',v_id);
  RETURN v_id;
END $$;

CREATE FUNCTION api.accept_direct_request(p_request uuid) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid := internal.actor(); r app.service_requests; c app.contracts;
BEGIN
  SELECT * INTO r FROM app.service_requests WHERE id=p_request FOR UPDATE;
  IF NOT FOUND OR r.mode<>'direct' OR r.target_professional_id<>v THEN RAISE EXCEPTION 'not_target_professional' USING ERRCODE='42501'; END IF;
  SELECT * INTO c FROM app.contracts WHERE request_id=p_request;
  IF FOUND THEN RETURN c.id; END IF;
  -- En directo el cliente consiente al publicar. El presupuesto no es un precio pactado.
  RETURN internal.make_contract(r.id,v,NULL,NULL);
END $$;

CREATE FUNCTION api.accept_proposal(p_proposal uuid) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid := internal.actor(); p app.proposals; r app.service_requests; c app.contracts;
BEGIN
  SELECT * INTO p FROM app.proposals WHERE id=p_proposal;
  IF NOT FOUND THEN RAISE EXCEPTION 'proposal_unavailable'; END IF;
  SELECT * INTO r FROM app.service_requests WHERE id=p.request_id FOR UPDATE;
  IF r.customer_id<>v OR r.mode<>'broadcast' THEN RAISE EXCEPTION 'request_not_owned' USING ERRCODE='42501'; END IF;
  SELECT * INTO c FROM app.contracts WHERE request_id=r.id;
  IF FOUND THEN
    IF c.proposal_id=p_proposal THEN RETURN c.id; END IF;
    RAISE EXCEPTION 'request_already_assigned';
  END IF;
  SELECT * INTO p FROM app.proposals WHERE id=p_proposal FOR UPDATE;
  IF p.status<>'pending' OR p.expires_at<=now() THEN RAISE EXCEPTION 'proposal_unavailable'; END IF;
  RETURN internal.make_contract(r.id,p.professional_id,p.id,p.price_minor);
END $$;

CREATE FUNCTION internal.participant(p_contract uuid) RETURNS app.contracts
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid := internal.actor(); c app.contracts;
BEGIN
  SELECT * INTO c FROM app.contracts WHERE id=p_contract AND v IN (customer_id,professional_id);
  IF NOT FOUND THEN RAISE EXCEPTION 'contract_access_denied' USING ERRCODE='42501'; END IF;
  RETURN c;
END $$;

CREATE FUNCTION api.share_location(p_contract uuid,p_hours integer DEFAULT 8) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid := internal.actor(); c app.contracts; v_id uuid; v_chat uuid;
BEGIN
  SELECT * INTO c FROM app.contracts WHERE id=p_contract FOR UPDATE;
  IF NOT FOUND OR c.customer_id<>v THEN RAISE EXCEPTION 'only_customer_can_share' USING ERRCODE='42501'; END IF;
  IF c.status NOT IN ('accepted','en_route','arrived','in_progress','awaiting_confirmation') OR p_hours IS NULL OR p_hours NOT BETWEEN 1 AND 24 THEN
    RAISE EXCEPTION 'location_share_not_allowed';
  END IF;
  UPDATE app.location_shares SET revoked_at=now() WHERE contract_id=c.id AND revoked_at IS NULL;
  INSERT INTO app.location_shares(contract_id,customer_id,expires_at) VALUES(c.id,v,now()+make_interval(hours=>p_hours)) RETURNING id INTO v_id;
  SELECT id INTO v_chat FROM app.conversations WHERE contract_id=c.id;
  INSERT INTO app.messages(conversation_id,sender_id,client_message_id,kind,body)
  VALUES(v_chat,v,v_id,'location_shared','El cliente ha compartido la ubicación del servicio.');
  PERFORM internal.audit('location.shared','contract',c.id,jsonb_build_object('share_id',v_id));
  RETURN v_id;
END $$;

CREATE FUNCTION api.revoke_location(p_contract uuid) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE c app.contracts := internal.participant(p_contract);
BEGIN
  IF c.customer_id<>internal.actor() THEN RAISE EXCEPTION 'only_customer_can_revoke' USING ERRCODE='42501'; END IF;
  UPDATE app.location_shares SET revoked_at=now() WHERE contract_id=c.id AND revoked_at IS NULL;
  DELETE FROM app.professional_positions WHERE contract_id=c.id;
  PERFORM internal.audit('location.revoked','contract',c.id);
END $$;

CREATE FUNCTION api.navigate(p_contract uuid) RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE c app.contracts := internal.participant(p_contract); v_result jsonb;
BEGIN
  IF c.status NOT IN ('accepted','en_route','arrived','in_progress','awaiting_confirmation') OR NOT EXISTS(
    SELECT 1 FROM app.location_shares WHERE contract_id=c.id AND revoked_at IS NULL AND expires_at>now()) THEN
    RAISE EXCEPTION 'location_not_shared' USING ERRCODE='42501';
  END IF;
  SELECT jsonb_build_object('destination',jsonb_build_object('latitude',public.ST_Y(l.location::public.geometry),
    'longitude',public.ST_X(l.location::public.geometry),'address',l.address_line,'instructions',l.instructions),
    'professional_position',(SELECT jsonb_build_object('latitude',public.ST_Y(p.location::public.geometry),
      'longitude',public.ST_X(p.location::public.geometry),'recorded_at',p.recorded_at,'accuracy_m',p.accuracy_m)
      FROM app.professional_positions p WHERE p.contract_id=c.id AND p.expires_at>now()))
  INTO v_result FROM app.request_locations l WHERE l.request_id=c.request_id;
  PERFORM internal.audit('location.read','contract',c.id);
  RETURN v_result;
END $$;

CREATE FUNCTION api.report_position(p_contract uuid,p_lon double precision,p_lat double precision,p_accuracy integer) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE c app.contracts := internal.participant(p_contract);
BEGIN
  IF c.professional_id<>internal.actor() OR c.status NOT IN ('en_route','arrived','in_progress') OR NOT EXISTS(
    SELECT 1 FROM app.location_shares WHERE contract_id=c.id AND revoked_at IS NULL AND expires_at>now()) THEN
    RAISE EXCEPTION 'position_not_allowed' USING ERRCODE='42501';
  END IF;
  INSERT INTO app.professional_positions(contract_id,location,accuracy_m,expires_at)
  VALUES(c.id,internal.point(p_lon,p_lat),p_accuracy,now()+interval '15 minutes')
  ON CONFLICT(contract_id) DO UPDATE SET location=EXCLUDED.location,accuracy_m=EXCLUDED.accuracy_m,recorded_at=now(),expires_at=EXCLUDED.expires_at;
END $$;

CREATE FUNCTION api.advance_contract(p_contract uuid,p_action text) RETURNS text
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid := internal.actor(); c app.contracts; v_status text;
BEGIN
  SELECT * INTO c FROM app.contracts WHERE id=p_contract AND v IN (customer_id,professional_id) FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'contract_access_denied' USING ERRCODE='42501'; END IF;
  IF p_action='finish' AND c.status='completed' THEN RETURN 'completed'; END IF;
  IF p_action='finish' THEN
    IF c.status NOT IN ('in_progress','awaiting_confirmation') THEN RAISE EXCEPTION 'invalid_transition'; END IF;
    IF v=c.professional_id THEN
      UPDATE app.contracts SET professional_finished_at=coalesce(professional_finished_at,now()) WHERE id=c.id;
    ELSE
      UPDATE app.contracts SET customer_finished_at=coalesce(customer_finished_at,now()) WHERE id=c.id;
    END IF;
    UPDATE app.contracts SET status=CASE WHEN professional_finished_at IS NOT NULL AND customer_finished_at IS NOT NULL
      THEN 'completed' ELSE 'awaiting_confirmation' END,
      completed_at=CASE WHEN professional_finished_at IS NOT NULL AND customer_finished_at IS NOT NULL THEN now() END
    WHERE id=c.id RETURNING status INTO v_status;
    IF v_status='completed' THEN UPDATE app.service_requests SET status='completed' WHERE id=c.request_id; END IF;
  ELSE
    IF v<>c.professional_id THEN RAISE EXCEPTION 'only_professional_can_advance' USING ERRCODE='42501'; END IF;
    IF p_action='depart' AND c.status='accepted' THEN
      IF NOT EXISTS(SELECT 1 FROM app.location_shares WHERE contract_id=c.id AND revoked_at IS NULL AND expires_at>now()) THEN
        RAISE EXCEPTION 'location_not_shared';
      END IF;
      v_status:='en_route'; UPDATE app.contracts SET en_route_at=now() WHERE id=c.id;
    ELSIF p_action='arrive' AND c.status='en_route' THEN
      v_status:='arrived'; UPDATE app.contracts SET arrived_at=now() WHERE id=c.id;
    ELSIF p_action='start' AND c.status='arrived' THEN
      v_status:='in_progress'; UPDATE app.contracts SET started_at=now() WHERE id=c.id;
    ELSIF (p_action,c.status) IN (('depart','en_route'),('arrive','arrived'),('start','in_progress')) THEN RETURN c.status;
    ELSE RAISE EXCEPTION 'invalid_transition'; END IF;
    UPDATE app.contracts SET status=v_status WHERE id=c.id;
  END IF;
  INSERT INTO app.contract_events(contract_id,actor_id,event_type,from_status,to_status) VALUES(c.id,v,p_action,c.status,v_status);
  IF v_status='completed' THEN
    UPDATE app.location_shares SET revoked_at=now() WHERE contract_id=c.id AND revoked_at IS NULL;
    DELETE FROM app.professional_positions WHERE contract_id=c.id;
    INSERT INTO app.outbox(topic,aggregate_id,recipient_id,deduplication_key)
    SELECT 'review.requested',c.id,x,'review:'||c.id||':'||x FROM unnest(ARRAY[c.customer_id,c.professional_id]) AS x;
  END IF;
  PERFORM internal.audit('contract.'||p_action,'contract',c.id);
  RETURN v_status;
END $$;

CREATE FUNCTION api.cancel_request(p_request uuid,p_reason text) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid := internal.actor(); r app.service_requests;
BEGIN
  SELECT * INTO r FROM app.service_requests WHERE id=p_request AND customer_id=v FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'request_not_owned' USING ERRCODE='42501'; END IF;
  IF r.status='cancelled' THEN RETURN; END IF;
  IF r.status NOT IN ('draft','open') THEN RAISE EXCEPTION 'cancel_contract_instead'; END IF;
  IF length(trim(coalesce(p_reason,'')))<5 THEN RAISE EXCEPTION 'reason_required'; END IF;
  UPDATE app.service_requests SET status='cancelled' WHERE id=r.id;
  UPDATE app.proposals SET status='rejected' WHERE request_id=r.id AND status='pending';
  PERFORM internal.audit('request.cancelled','request',r.id,jsonb_build_object('reason',p_reason));
END $$;

CREATE FUNCTION api.cancel_contract(p_contract uuid,p_reason text) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid := internal.actor(); c app.contracts;
BEGIN
  SELECT * INTO c FROM app.contracts WHERE id=p_contract AND v IN (customer_id,professional_id) FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'contract_access_denied' USING ERRCODE='42501'; END IF;
  IF c.status='cancelled' THEN RETURN; END IF;
  IF c.status NOT IN ('accepted','en_route','arrived') THEN RAISE EXCEPTION 'open_dispute_instead'; END IF;
  IF length(trim(coalesce(p_reason,'')))<5 THEN RAISE EXCEPTION 'reason_required'; END IF;
  UPDATE app.contracts SET status='cancelled',cancelled_at=now(),cancellation_reason=p_reason WHERE id=c.id;
  UPDATE app.service_requests SET status='cancelled' WHERE id=c.request_id;
  UPDATE app.location_shares SET revoked_at=now() WHERE contract_id=c.id AND revoked_at IS NULL;
  DELETE FROM app.professional_positions WHERE contract_id=c.id;
  INSERT INTO app.contract_events(contract_id,actor_id,event_type,from_status,to_status,metadata)
  VALUES(c.id,v,'cancel',c.status,'cancelled',jsonb_build_object('reason',p_reason));
  PERFORM internal.audit('contract.cancelled','contract',c.id);
  -- No devolución automática: la política comercial aún no fue definida.
END $$;

CREATE FUNCTION api.send_message(p_contract uuid,p_body text,p_key uuid) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE c app.contracts := internal.participant(p_contract); v uuid := internal.actor(); v_chat uuid; m app.messages;
BEGIN
  IF c.status='cancelled' THEN RAISE EXCEPTION 'chat_closed'; END IF;
  SELECT id INTO v_chat FROM app.conversations WHERE contract_id=c.id AND closed_at IS NULL FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'chat_unavailable'; END IF;
  SELECT * INTO m FROM app.messages WHERE conversation_id=v_chat AND sender_id=v AND client_message_id=p_key;
  IF FOUND THEN
    IF m.body IS DISTINCT FROM p_body THEN RAISE EXCEPTION 'idempotency_conflict'; END IF;
    RETURN m.id;
  END IF;
  INSERT INTO app.messages(conversation_id,sender_id,client_message_id,body) VALUES(v_chat,v,p_key,p_body) RETURNING * INTO m;
  INSERT INTO app.outbox(topic,aggregate_id,recipient_id,deduplication_key)
  VALUES('chat.message',m.id,CASE WHEN v=c.customer_id THEN c.professional_id ELSE c.customer_id END,'message:'||m.id);
  RETURN m.id;
END $$;

CREATE FUNCTION api.rate_contract(p_contract uuid,p_rating smallint,p_recommends boolean,p_comment text) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE c app.contracts := internal.participant(p_contract); v uuid := internal.actor(); v_id uuid;
BEGIN
  IF c.status<>'completed' THEN RAISE EXCEPTION 'contract_not_completed'; END IF;
  INSERT INTO app.reviews(contract_id,author_id,subject_id,rating,recommends,comment)
  VALUES(c.id,v,CASE WHEN v=c.customer_id THEN c.professional_id ELSE c.customer_id END,p_rating,p_recommends,p_comment)
  RETURNING id INTO v_id;
  PERFORM internal.audit('review.created','review',v_id);
  RETURN v_id;
END $$;

CREATE FUNCTION api.propose_terms(p_contract uuid,p_price bigint,p_method text,p_terms text) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE c app.contracts; v uuid:=internal.actor(); v_id uuid; v_version integer;
BEGIN
  SELECT * INTO c FROM app.contracts WHERE id=p_contract AND v IN(customer_id,professional_id) FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'contract_access_denied' USING ERRCODE='42501'; END IF;
  IF c.status NOT IN('accepted','en_route','arrived','in_progress') THEN RAISE EXCEPTION 'terms_not_allowed'; END IF;
  SELECT coalesce(max(version),0)+1 INTO v_version FROM app.contract_terms WHERE contract_id=c.id;
  INSERT INTO app.contract_terms(contract_id,version,proposed_by,price_minor,currency,payment_method,payment_terms,customer_accepted_at,professional_accepted_at)
  VALUES(c.id,v_version,v,p_price,c.currency,p_method,p_terms,CASE WHEN v=c.customer_id THEN now() END,
    CASE WHEN v=c.professional_id THEN now() END) RETURNING id INTO v_id;
  RETURN v_id;
END $$;

CREATE FUNCTION api.accept_terms(p_terms uuid) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE t app.contract_terms; c app.contracts; v uuid:=internal.actor();
BEGIN
  SELECT * INTO t FROM app.contract_terms WHERE id=p_terms;
  c:=internal.participant(t.contract_id);
  PERFORM 1 FROM app.contracts WHERE id=c.id FOR UPDATE;
  IF c.status NOT IN('accepted','en_route','arrived','in_progress') THEN RAISE EXCEPTION 'terms_not_allowed'; END IF;
  IF EXISTS(SELECT 1 FROM app.contract_terms WHERE contract_id=c.id AND version>t.version) THEN RAISE EXCEPTION 'terms_superseded'; END IF;
  UPDATE app.contract_terms SET customer_accepted_at=CASE WHEN v=c.customer_id THEN coalesce(customer_accepted_at,now()) ELSE customer_accepted_at END,
    professional_accepted_at=CASE WHEN v=c.professional_id THEN coalesce(professional_accepted_at,now()) ELSE professional_accepted_at END
    WHERE id=t.id RETURNING * INTO t;
  IF t.customer_accepted_at IS NOT NULL AND t.professional_accepted_at IS NOT NULL THEN
    UPDATE app.contracts SET agreed_price_minor=t.price_minor,payment_method=t.payment_method,payment_terms=t.payment_terms,terms_version=t.version WHERE id=c.id;
    PERFORM internal.audit('contract.terms_accepted','contract',c.id,jsonb_build_object('version',t.version));
  END IF;
END $$;
