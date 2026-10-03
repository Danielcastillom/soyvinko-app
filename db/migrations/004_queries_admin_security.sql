CREATE FUNCTION api.professional_profile(p_professional uuid) RETURNS jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE result jsonb;
BEGIN
  PERFORM internal.actor();
  SELECT jsonb_build_object('id',p.user_id,'slug',p.slug,'name',p.business_name,'bio',p.bio,
    'years_experience',p.years_experience,'accepts_requests',p.accepts_requests,
    'verified',EXISTS(SELECT 1 FROM app.kyc_cases k WHERE k.user_id=p.user_id AND k.status='approved' AND k.expires_at>now()),
    'rating',(SELECT round(avg(rating),2) FROM app.reviews WHERE subject_id=p.user_id AND moderation_status='published'),
    'review_count',(SELECT count(*) FROM app.reviews WHERE subject_id=p.user_id AND moderation_status='published'),
    'recommendations',(SELECT count(*) FROM app.reviews WHERE subject_id=p.user_id AND moderation_status='published' AND recommends),
    'offerings',coalesce((SELECT jsonb_agg(jsonb_build_object('id',o.id,'category_id',o.category_id,'title',o.title,
      'description',o.description,'pricing_type',o.pricing_type,'price_min_minor',o.price_min_minor,'price_max_minor',o.price_max_minor,'currency',o.currency))
      FROM app.offerings o WHERE o.professional_id=p.user_id AND o.active),'[]'::jsonb),
    'branches',coalesce((SELECT jsonb_agg(jsonb_build_object('id',b.id,'name',b.name,'city',b.city,
      'address',CASE WHEN b.public_address THEN b.address_line END,
      'latitude',CASE WHEN b.public_address THEN public.ST_Y(b.location::public.geometry) END,
      'longitude',CASE WHEN b.public_address THEN public.ST_X(b.location::public.geometry) END))
      FROM app.branches b WHERE b.professional_id=p.user_id AND b.active),'[]'::jsonb),
    'portfolio',coalesce((SELECT jsonb_agg(jsonb_build_object('id',pi.id,'title',pi.title,'description',pi.description,'file_id',pi.file_id))
      FROM app.portfolio_items pi JOIN app.files f ON f.id=pi.file_id WHERE pi.professional_id=p.user_id
        AND f.moderation_status='approved' AND f.exif_removed AND f.deleted_at IS NULL),'[]'::jsonb))
  INTO result FROM app.professionals p JOIN app.users u ON u.id=p.user_id
  WHERE p.user_id=p_professional AND p.status='active' AND u.status='active';
  RETURN result;
END $$;

CREATE FUNCTION api.search_professionals(p_address uuid,p_category uuid,p_radius_m integer DEFAULT 10000,
  p_sort text DEFAULT 'distance',p_verified_only boolean DEFAULT false,p_currency text DEFAULT NULL,
  p_max_price bigint DEFAULT NULL,p_min_rating numeric DEFAULT 0,p_limit integer DEFAULT 30,p_offset integer DEFAULT 0)
RETURNS TABLE(professional_id uuid,profile jsonb,distance_m bigint,verified boolean,sponsored boolean)
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor(); a app.addresses;
BEGIN
  SELECT * INTO a FROM app.addresses WHERE id=p_address AND user_id=v AND archived_at IS NULL;
  IF NOT FOUND THEN RAISE EXCEPTION 'address_not_owned' USING ERRCODE='42501'; END IF;
  IF p_sort IS NULL OR p_sort NOT IN('distance','recommended') OR p_radius_m IS NULL OR p_radius_m NOT BETWEEN 100 AND 200000
    OR p_limit IS NULL OR p_limit NOT BETWEEN 1 AND 100 OR p_offset IS NULL OR p_offset<0 OR p_min_rating NOT BETWEEN 0 AND 5
    OR (p_max_price IS NOT NULL AND (p_max_price<0 OR p_currency IS NULL)) THEN RAISE EXCEPTION 'invalid_search_filters'; END IF;
  RETURN QUERY
  WITH candidates AS (
    SELECT p.user_id,min(public.ST_Distance(b.location,a.location)) AS meters,
      EXISTS(SELECT 1 FROM app.kyc_cases k WHERE k.user_id=p.user_id AND k.status='approved' AND k.expires_at>now()) AS is_verified,
      coalesce((SELECT max(e.weight) FROM app.entitlements e WHERE e.professional_id=p.user_id AND e.kind='positioning'
        AND e.revoked_at IS NULL AND e.starts_at<=now() AND e.ends_at>now()),0) AS boost,
      coalesce((SELECT avg(r.rating) FROM app.reviews r WHERE r.subject_id=p.user_id AND r.moderation_status='published'),0) AS rating
    FROM app.professionals p JOIN app.users u ON u.id=p.user_id AND u.status='active'
    JOIN app.coverage_areas ca ON ca.professional_id=p.user_id AND ca.category_id=p_category AND ca.active
    JOIN app.categories cat ON cat.id=ca.category_id AND cat.active
    JOIN app.branches b ON b.id=ca.branch_id AND b.active
    WHERE p.status='active' AND p.accepts_requests AND p.user_id<>v
      AND public.ST_DWithin(b.location,a.location,p_radius_m)
      AND public.ST_DWithin(b.location,a.location,ca.radius_m)
      AND (ca.polygon IS NULL OR public.ST_Covers(ca.polygon,a.location))
      AND EXISTS(SELECT 1 FROM app.offerings o WHERE o.professional_id=p.user_id AND o.category_id=p_category AND o.active
        AND (p_currency IS NULL OR o.currency=p_currency) AND (p_max_price IS NULL OR o.price_min_minor<=p_max_price))
    GROUP BY p.user_id
  )
  SELECT c.user_id,api.professional_profile(c.user_id),round(c.meters)::bigint,c.is_verified,c.boost>0
  FROM candidates c WHERE (NOT p_verified_only OR c.is_verified) AND c.rating>=p_min_rating
  ORDER BY CASE WHEN p_sort='distance' THEN c.meters END ASC,
    CASE WHEN p_sort='recommended' THEN c.is_verified::integer END DESC,
    CASE WHEN p_sort='recommended' THEN c.boost END DESC,
    CASE WHEN p_sort='recommended' THEN c.rating END DESC,
    c.meters ASC,c.user_id ASC LIMIT p_limit OFFSET p_offset;
END $$;

CREATE FUNCTION api.available_requests(p_limit integer DEFAULT 30,p_offset integer DEFAULT 0)
RETURNS TABLE(request_id uuid,category_id uuid,title text,description text,mode text,
  budget_min_minor bigint,budget_max_minor bigint,currency char(3),expires_at timestamptz,attachments jsonb)
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor();
BEGIN
  IF p_limit IS NULL OR p_limit NOT BETWEEN 1 AND 100 OR p_offset IS NULL OR p_offset<0 THEN RAISE EXCEPTION 'invalid_pagination'; END IF;
  RETURN QUERY SELECT r.id,r.category_id,r.title,r.description,r.mode,r.budget_min_minor,r.budget_max_minor,r.currency,r.expires_at,
    coalesce((SELECT jsonb_agg(jsonb_build_object('file_id',f.id,'mime_type',f.mime_type)) FROM app.request_attachments ra
      JOIN app.files f ON f.id=ra.file_id WHERE ra.request_id=r.id AND f.moderation_status='approved' AND f.exif_removed AND f.deleted_at IS NULL),'[]'::jsonb)
    FROM app.service_requests r WHERE r.status='open' AND r.moderation_status='approved' AND r.expires_at>now() AND internal.eligible(v,r.id)
    ORDER BY r.created_at DESC,r.id LIMIT p_limit OFFSET p_offset;
  -- Deliberadamente sin customer_id, dirección, coordenadas NI distancia al cliente.
END $$;

CREATE FUNCTION api.my_requests() RETURNS SETOF jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  SELECT to_jsonb(r) FROM app.service_requests r WHERE customer_id=internal.actor() ORDER BY created_at DESC LIMIT 200;
$$;

CREATE FUNCTION api.request_proposals(p_request uuid) RETURNS SETOF jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor();
BEGIN
  IF NOT EXISTS(SELECT 1 FROM app.service_requests WHERE id=p_request AND customer_id=v) THEN
    RAISE EXCEPTION 'request_not_owned' USING ERRCODE='42501';
  END IF;
  RETURN QUERY SELECT jsonb_build_object('id',p.id,'professional',api.professional_profile(p.professional_id),
    'message',p.message,'price_minor',p.price_minor,'currency',p.currency,'status',p.status,'expires_at',p.expires_at)
  FROM app.proposals p WHERE p.request_id=p_request ORDER BY p.created_at,p.id;
END $$;

CREATE FUNCTION api.my_contracts() RETURNS SETOF jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  SELECT to_jsonb(c) FROM app.contracts c WHERE internal.actor() IN(customer_id,professional_id) ORDER BY accepted_at DESC LIMIT 200;
$$;

CREATE FUNCTION api.contract_contacts(p_contract uuid) RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE c app.contracts:=internal.participant(p_contract); v uuid:=internal.actor(); v_other uuid; result jsonb;
BEGIN
  IF c.status='cancelled' THEN RAISE EXCEPTION 'contract_cancelled'; END IF;
  v_other:=CASE WHEN v=c.customer_id THEN c.professional_id ELSE c.customer_id END;
  SELECT jsonb_build_object('user_id',u.id,'name',u.display_name,
    'contacts',coalesce((SELECT jsonb_agg(jsonb_build_object('kind',ct.kind,'label',ct.label,'value',ct.value))
       FROM app.user_contacts ct WHERE ct.user_id=u.id AND ct.share_on_contract),'[]'::jsonb),
    'professional_contacts',CASE WHEN v_other=c.professional_id THEN coalesce((SELECT jsonb_agg(jsonb_build_object('kind',pc.kind,
      'label',pc.label,'value',pc.value,'branch_id',pc.branch_id)) FROM app.professional_contacts pc WHERE pc.professional_id=v_other),'[]'::jsonb) ELSE '[]'::jsonb END)
  INTO result FROM app.users u WHERE u.id=v_other;
  PERFORM internal.audit('contacts.read','contract',c.id);
  RETURN result;
END $$;

CREATE FUNCTION api.chat_messages(p_contract uuid,p_limit integer DEFAULT 50,p_before timestamptz DEFAULT 'infinity',p_before_id uuid DEFAULT 'ffffffff-ffff-ffff-ffff-ffffffffffff')
RETURNS SETOF jsonb LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE c app.contracts:=internal.participant(p_contract);
BEGIN
  IF p_limit IS NULL OR p_limit NOT BETWEEN 1 AND 100 THEN RAISE EXCEPTION 'invalid_pagination'; END IF;
  RETURN QUERY SELECT jsonb_build_object('id',m.id,'sender_id',m.sender_id,'kind',m.kind,
    'body',CASE WHEN m.redacted_at IS NULL THEN m.body END,'file_id',CASE WHEN m.redacted_at IS NULL THEN m.file_id END,'created_at',m.created_at)
  FROM app.messages m JOIN app.conversations conv ON conv.id=m.conversation_id
  WHERE conv.contract_id=c.id AND (m.created_at,m.id)<(p_before,p_before_id) ORDER BY m.created_at DESC,m.id DESC LIMIT p_limit;
END $$;

CREATE FUNCTION api.my_wallet() RETURNS jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  SELECT jsonb_build_object('balance',w.balance,'ledger',coalesce((SELECT jsonb_agg(to_jsonb(x) ORDER BY x.id DESC)
    FROM (SELECT id,delta,balance_after,kind,contract_id,purchase_id,reason,created_at FROM app.credit_ledger
          WHERE professional_id=w.professional_id ORDER BY id DESC LIMIT 100) x),'[]'::jsonb))
  FROM app.credit_wallets w WHERE professional_id=internal.actor();
$$;

CREATE FUNCTION api.active_plans() RETURNS SETOF jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
BEGIN
  PERFORM internal.actor();
  RETURN QUERY SELECT to_jsonb(p) FROM app.plans p WHERE active ORDER BY price_minor,id;
END $$;

CREATE FUNCTION api.store_products(p_store uuid) RETURNS SETOF jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
BEGIN
  PERFORM internal.actor();
  RETURN QUERY SELECT to_jsonb(p) FROM app.products p JOIN app.stores s ON s.id=p.store_id
    JOIN app.professionals pro ON pro.user_id=s.professional_id JOIN app.users u ON u.id=pro.user_id
    WHERE s.id=p_store AND s.status='active' AND pro.status='active' AND u.status='active'
      AND p.status='active' AND internal.has_entitlement(s.professional_id,'marketplace') ORDER BY p.name,p.id LIMIT 200;
END $$;

CREATE FUNCTION api.my_orders() RETURNS SETOF jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  SELECT to_jsonb(o)||jsonb_build_object('items',(SELECT jsonb_agg(to_jsonb(i)) FROM app.order_items i WHERE i.order_id=o.id))
  FROM app.orders o JOIN app.stores s ON s.id=o.store_id
  WHERE internal.actor() IN(o.customer_id,s.professional_id) ORDER BY o.created_at DESC LIMIT 200;
$$;

CREATE FUNCTION api.update_professional(p_name text,p_bio text,p_years integer,p_accepts boolean) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
BEGIN
  UPDATE app.professionals SET business_name=p_name,bio=p_bio,years_experience=p_years,accepts_requests=p_accepts,updated_at=now()
    WHERE user_id=internal.actor() AND status<>'suspended';
  IF NOT FOUND THEN RAISE EXCEPTION 'professional_unavailable'; END IF;
END $$;

CREATE FUNCTION api.withdraw_proposal(p_proposal uuid) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE p app.proposals;
BEGIN
  SELECT * INTO p FROM app.proposals WHERE id=p_proposal AND professional_id=internal.actor();
  IF NOT FOUND THEN RAISE EXCEPTION 'proposal_not_owned' USING ERRCODE='42501'; END IF;
  PERFORM 1 FROM app.service_requests WHERE id=p.request_id FOR UPDATE;
  UPDATE app.proposals SET status='withdrawn' WHERE id=p.id AND status='pending';
  IF NOT FOUND THEN RAISE EXCEPTION 'proposal_not_pending'; END IF;
END $$;

CREATE FUNCTION api.moderate_request(p_request uuid,p_approve boolean,p_reason text) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.require_permission('content.moderate');
BEGIN
  IF p_approve IS NULL OR length(trim(coalesce(p_reason,'')))<5 THEN RAISE EXCEPTION 'reason_required'; END IF;
  UPDATE app.service_requests SET moderation_status=CASE WHEN p_approve THEN 'approved' ELSE 'rejected' END
  WHERE id=p_request AND status IN('draft','open');
  IF NOT FOUND THEN RAISE EXCEPTION 'request_not_moderatable'; END IF;
  INSERT INTO app.moderation_actions(actor_id,target_type,target_id,action,reason)
  VALUES(v,'request',p_request,CASE WHEN p_approve THEN 'approve' ELSE 'reject' END,p_reason);
END $$;

CREATE FUNCTION api.moderate_review(p_review uuid,p_status text,p_reason text) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.require_permission('content.moderate');
BEGIN
  IF p_status IS NULL OR p_status NOT IN('published','hidden') OR length(trim(coalesce(p_reason,'')))<5 THEN RAISE EXCEPTION 'invalid_moderation'; END IF;
  UPDATE app.reviews SET moderation_status=p_status WHERE id=p_review;
  IF NOT FOUND THEN RAISE EXCEPTION 'review_not_found'; END IF;
  INSERT INTO app.moderation_actions(actor_id,target_type,target_id,action,reason) VALUES(v,'review',p_review,p_status,p_reason);
END $$;

CREATE FUNCTION api.submit_kyc(p_provider text,p_external_reference text) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor(); v_id uuid;
BEGIN
  INSERT INTO app.kyc_cases(user_id,provider,external_reference) VALUES(v,p_provider,p_external_reference) RETURNING id INTO v_id;
  PERFORM internal.audit('kyc.submitted','kyc',v_id);
  RETURN v_id;
END $$;

CREATE FUNCTION api.review_kyc(p_case uuid,p_status text,p_reason text,p_expires_at timestamptz DEFAULT NULL) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.require_permission('kyc.review'); k app.kyc_cases;
BEGIN
  SELECT * INTO k FROM app.kyc_cases WHERE id=p_case FOR UPDATE;
  IF NOT FOUND OR k.user_id=v THEN RAISE EXCEPTION 'kyc_review_not_allowed'; END IF;
  IF p_status IS NULL OR p_status NOT IN('approved','rejected','revoked','in_review','expired')
    OR length(trim(coalesce(p_reason,'')))<5 OR (p_status='approved' AND (p_expires_at IS NULL OR p_expires_at<=now())) THEN
    RAISE EXCEPTION 'invalid_kyc_decision'; END IF;
  UPDATE app.kyc_cases SET status=p_status,reviewed_at=now(),reviewed_by=v,reason=p_reason,expires_at=p_expires_at WHERE id=k.id;
  INSERT INTO app.moderation_actions(actor_id,target_type,target_id,action,reason) VALUES(v,'kyc',k.id,p_status,p_reason);
END $$;

CREATE FUNCTION api.set_user_status(p_user uuid,p_status text,p_reason text) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.require_permission('users.manage');
BEGIN
  IF p_user=v OR p_status IS NULL OR p_status NOT IN('active','suspended') OR length(trim(coalesce(p_reason,'')))<10 THEN RAISE EXCEPTION 'invalid_user_action'; END IF;
  UPDATE app.users SET status=p_status,updated_at=now() WHERE id=p_user;
  IF NOT FOUND THEN RAISE EXCEPTION 'user_not_found'; END IF;
  IF p_status='suspended' THEN UPDATE app.sessions SET revoked_at=now() WHERE user_id=p_user AND revoked_at IS NULL; END IF;
  INSERT INTO app.moderation_actions(actor_id,target_type,target_id,action,reason) VALUES(v,'user',p_user,p_status,p_reason);
END $$;

CREATE FUNCTION api.assign_role(p_user uuid,p_role text,p_enable boolean,p_reason text) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.require_permission('roles.manage');
BEGIN
  IF p_user=v OR p_role NOT IN('support','admin','superadmin') OR length(trim(coalesce(p_reason,'')))<10 OR p_enable IS NULL THEN
    RAISE EXCEPTION 'invalid_role_action';
  END IF;
  IF p_enable THEN INSERT INTO app.user_roles(user_id,role_code,granted_by) VALUES(p_user,p_role,v) ON CONFLICT DO NOTHING;
  ELSE DELETE FROM app.user_roles WHERE user_id=p_user AND role_code=p_role; END IF;
  PERFORM internal.audit('role.changed','user',p_user,jsonb_build_object('role',p_role,'enabled',p_enable,'reason',p_reason));
END $$;

CREATE FUNCTION api.support_ticket(p_ticket uuid,p_reason text) RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.require_permission('support.read'); result jsonb;
BEGIN
  IF length(trim(coalesce(p_reason,'')))<10 THEN RAISE EXCEPTION 'access_reason_required'; END IF;
  SELECT to_jsonb(t)||jsonb_build_object('messages',(SELECT jsonb_agg(to_jsonb(m) ORDER BY m.created_at) FROM app.support_messages m WHERE m.ticket_id=t.id))
    INTO result FROM app.support_tickets t WHERE t.id=p_ticket;
  PERFORM internal.audit('support.ticket_read','ticket',p_ticket,jsonb_build_object('reason',p_reason));
  RETURN result;
END $$;

CREATE FUNCTION api.manage_ticket(p_ticket uuid,p_status text,p_assignee uuid,p_note text) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.require_permission('support.manage');
BEGIN
  IF length(trim(coalesce(p_note,'')))<5 THEN RAISE EXCEPTION 'note_required'; END IF;
  IF p_assignee IS NOT NULL AND NOT EXISTS(SELECT 1 FROM app.user_roles ur JOIN app.role_permissions rp ON rp.role_code=ur.role_code
      JOIN app.users u ON u.id=ur.user_id WHERE ur.user_id=p_assignee AND rp.permission_code='support.manage' AND u.status='active') THEN
    RAISE EXCEPTION 'invalid_support_assignee'; END IF;
  UPDATE app.support_tickets SET status=p_status,assigned_to=p_assignee,
    resolved_at=CASE WHEN p_status IN('resolved','closed') THEN now() END WHERE id=p_ticket;
  IF NOT FOUND THEN RAISE EXCEPTION 'ticket_not_found'; END IF;
  INSERT INTO app.support_messages(ticket_id,author_id,body,internal_only) VALUES(p_ticket,v,p_note,true);
  PERFORM internal.audit('support.ticket_updated','ticket',p_ticket,jsonb_build_object('status',p_status));
END $$;

-- El servicio de autenticación valida Argon2id, MFA y límites de intentos.
CREATE FUNCTION api.auth_account(p_email text) RETURNS jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  SELECT jsonb_build_object('user_id',u.id,'status',u.status,'password_hash',c.password_hash,
    'locked_until',c.locked_until,'failed_attempts',c.failed_attempts,'mfa_enabled',c.mfa_enabled_at IS NOT NULL)
  FROM app.users u JOIN app.credentials c ON c.user_id=u.id WHERE u.email=lower(trim(p_email));
$$;

CREATE FUNCTION api.create_session(p_user uuid,p_refresh_hash text,p_device text,p_expires_at timestamptz) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v_id uuid;
BEGIN
  IF p_refresh_hash IS NULL OR p_refresh_hash !~ '^[a-f0-9]{64}$' OR NOT EXISTS(SELECT 1 FROM app.users WHERE id=p_user AND status='active')
    OR p_expires_at>now()+interval '90 days' OR p_expires_at<=now() THEN RAISE EXCEPTION 'invalid_session'; END IF;
  INSERT INTO app.sessions(user_id,refresh_token_hash,device_label,expires_at) VALUES(p_user,p_refresh_hash,p_device,p_expires_at) RETURNING id INTO v_id;
  RETURN v_id;
END $$;

CREATE FUNCTION api.resolve_session(p_refresh_hash text) RETURNS uuid
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  SELECT s.user_id FROM app.sessions s JOIN app.users u ON u.id=s.user_id
    WHERE s.refresh_token_hash=p_refresh_hash AND s.revoked_at IS NULL AND s.expires_at>now() AND u.status='active';
$$;

CREATE FUNCTION api.revoke_session(p_session uuid) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
BEGIN
  UPDATE app.sessions SET revoked_at=now() WHERE id=p_session AND user_id=internal.actor();
END $$;

-- Solo funciones autorizadas. No se conceden tablas, secuencias ni helpers al backend.
REVOKE ALL ON ALL TABLES IN SCHEMA app FROM PUBLIC, chamba_api, chamba_auth, chamba_billing, chamba_admin;
REVOKE ALL ON ALL SEQUENCES IN SCHEMA app FROM PUBLIC, chamba_api, chamba_auth, chamba_billing, chamba_admin;
REVOKE ALL ON ALL FUNCTIONS IN SCHEMA app,api,internal FROM PUBLIC;
GRANT USAGE ON SCHEMA api TO chamba_api,chamba_auth,chamba_billing,chamba_admin;
DO $$ DECLARE f record; target text; BEGIN
  FOR f IN SELECT p.oid::regprocedure AS signature,p.proname FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace WHERE n.nspname='api' LOOP
    target:=CASE
      WHEN f.proname IN('register_user','auth_account','create_session','resolve_session') THEN 'chamba_auth'
      WHEN f.proname IN('confirm_purchase') THEN 'chamba_billing'
      WHEN f.proname IN('refund_contract_credit','moderate_request','moderate_review','review_kyc','set_user_status','assign_role','support_ticket','manage_ticket') THEN 'chamba_admin'
      ELSE 'chamba_api' END;
    EXECUTE format('GRANT EXECUTE ON FUNCTION %s TO %I',f.signature,target);
  END LOOP;
END $$;
