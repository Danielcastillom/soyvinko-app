DO $$ BEGIN
  IF NOT EXISTS(SELECT 1 FROM pg_roles WHERE rolname='chamba_worker') THEN CREATE ROLE chamba_worker NOLOGIN; END IF;
END $$;
GRANT USAGE ON SCHEMA api TO chamba_worker;

CREATE FUNCTION api.my_account() RETURNS jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  SELECT jsonb_build_object('id',u.id,'email',u.email,'name',u.display_name,'status',u.status,
    'roles',(SELECT jsonb_agg(role_code) FROM app.user_roles WHERE user_id=u.id),
    'contacts',(SELECT jsonb_agg(to_jsonb(c)) FROM app.user_contacts c WHERE c.user_id=u.id),
    'addresses',(SELECT jsonb_agg((to_jsonb(a)-'location')||jsonb_build_object('longitude',public.ST_X(a.location::public.geometry),
      'latitude',public.ST_Y(a.location::public.geometry))) FROM app.addresses a WHERE a.user_id=u.id AND a.archived_at IS NULL))
  FROM app.users u WHERE u.id=internal.actor();
$$;

CREATE FUNCTION api.my_professional() RETURNS jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  SELECT to_jsonb(p)||jsonb_build_object('branches',(SELECT jsonb_agg((to_jsonb(b)-'location')||jsonb_build_object(
    'longitude',public.ST_X(b.location::public.geometry),'latitude',public.ST_Y(b.location::public.geometry))) FROM app.branches b WHERE b.professional_id=p.user_id),
    'contacts',(SELECT jsonb_agg(to_jsonb(c)) FROM app.professional_contacts c WHERE c.professional_id=p.user_id),
    'coverage',(SELECT jsonb_agg(to_jsonb(c)-'polygon') FROM app.coverage_areas c WHERE c.professional_id=p.user_id),
    'offerings',(SELECT jsonb_agg(to_jsonb(o)) FROM app.offerings o WHERE o.professional_id=p.user_id),
    'store',(SELECT to_jsonb(s) FROM app.stores s WHERE s.professional_id=p.user_id))
  FROM app.professionals p WHERE p.user_id=internal.actor();
$$;

CREATE FUNCTION api.add_professional_contact(p_branch uuid,p_kind text,p_label text,p_value text) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v_id uuid;
BEGIN
  INSERT INTO app.professional_contacts(professional_id,branch_id,kind,label,value)
  VALUES(internal.actor(),p_branch,p_kind,p_label,p_value) RETURNING id INTO v_id;
  RETURN v_id;
END $$;

CREATE FUNCTION api.professional_reviews(p_professional uuid,p_limit integer DEFAULT 30,p_offset integer DEFAULT 0) RETURNS SETOF jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
BEGIN
  PERFORM internal.actor();
  IF p_limit IS NULL OR p_limit NOT BETWEEN 1 AND 100 OR p_offset IS NULL OR p_offset<0 THEN RAISE EXCEPTION 'invalid_pagination'; END IF;
  RETURN QUERY SELECT jsonb_build_object('id',id,'rating',rating,'recommends',recommends,'comment',comment,'response',response,'created_at',created_at)
  FROM app.reviews WHERE subject_id=p_professional AND moderation_status='published' ORDER BY created_at DESC,id LIMIT p_limit OFFSET p_offset;
END $$;

CREATE FUNCTION api.register_upload(p_owner uuid,p_object_key text,p_mime text,p_bytes bigint,p_sha256 text,p_purpose text,p_exif_removed boolean) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v_id uuid;
BEGIN
  INSERT INTO app.files(owner_id,object_key,mime_type,bytes,sha256,purpose,exif_removed)
  VALUES(p_owner,p_object_key,p_mime,p_bytes,p_sha256,p_purpose,p_exif_removed) RETURNING id INTO v_id;
  RETURN v_id;
END $$;

CREATE FUNCTION api.moderate_file(p_file uuid,p_approve boolean,p_reason text) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.require_permission('content.moderate');
BEGIN
  IF p_approve IS NULL OR length(trim(coalesce(p_reason,'')))<5 THEN RAISE EXCEPTION 'reason_required'; END IF;
  UPDATE app.files SET moderation_status=CASE WHEN p_approve THEN 'approved' ELSE 'rejected' END
    WHERE id=p_file AND deleted_at IS NULL AND (NOT p_approve OR exif_removed OR purpose='kyc');
  IF NOT FOUND THEN RAISE EXCEPTION 'file_not_moderatable'; END IF;
  INSERT INTO app.moderation_actions(actor_id,target_type,target_id,action,reason)
  VALUES(v,'file',p_file,CASE WHEN p_approve THEN 'approve' ELSE 'reject' END,p_reason);
END $$;

CREATE FUNCTION api.attach_request_file(p_request uuid,p_file uuid) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor();
BEGIN
  PERFORM 1 FROM app.service_requests WHERE id=p_request AND customer_id=v AND status='draft' FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'request_not_editable'; END IF;
  IF NOT EXISTS(SELECT 1 FROM app.files WHERE id=p_file AND owner_id=v AND purpose='request' AND deleted_at IS NULL) THEN
    RAISE EXCEPTION 'file_not_owned' USING ERRCODE='42501'; END IF;
  INSERT INTO app.request_attachments(request_id,file_id) VALUES(p_request,p_file) ON CONFLICT DO NOTHING;
  UPDATE app.service_requests SET moderation_status='pending' WHERE id=p_request;
END $$;

CREATE FUNCTION api.attach_kyc_file(p_case uuid,p_file uuid,p_kind text) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor();
BEGIN
  PERFORM 1 FROM app.kyc_cases WHERE id=p_case AND user_id=v AND status='pending' FOR UPDATE;
  IF NOT FOUND OR NOT EXISTS(SELECT 1 FROM app.files WHERE id=p_file AND owner_id=v AND purpose='kyc' AND deleted_at IS NULL) THEN
    RAISE EXCEPTION 'kyc_file_access_denied' USING ERRCODE='42501'; END IF;
  INSERT INTO app.kyc_documents(case_id,file_id,kind) VALUES(p_case,p_file,p_kind) ON CONFLICT DO NOTHING;
END $$;

CREATE FUNCTION api.file_object_key(p_file uuid) RETURNS text
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor(); f app.files; allowed boolean:=false;
BEGIN
  SELECT * INTO f FROM app.files WHERE id=p_file AND deleted_at IS NULL;
  IF NOT FOUND THEN RAISE EXCEPTION 'file_access_denied' USING ERRCODE='42501'; END IF;
  IF f.owner_id=v THEN allowed:=true;
  ELSIF f.purpose<>'kyc' AND f.moderation_status='approved' AND f.exif_removed THEN
    allowed:=EXISTS(SELECT 1 FROM app.request_attachments ra JOIN app.service_requests r ON r.id=ra.request_id
      WHERE ra.file_id=f.id AND ((r.status='open' AND r.moderation_status='approved' AND r.expires_at>now() AND internal.eligible(v,r.id))
       OR EXISTS(SELECT 1 FROM app.contracts c WHERE c.request_id=r.id AND v IN(c.customer_id,c.professional_id))))
      OR EXISTS(SELECT 1 FROM app.portfolio_items pi JOIN app.professionals p ON p.user_id=pi.professional_id
        JOIN app.users u ON u.id=p.user_id WHERE pi.file_id=f.id AND p.status='active' AND u.status='active')
      OR EXISTS(SELECT 1 FROM app.product_images im JOIN app.products p ON p.id=im.product_id JOIN app.stores s ON s.id=p.store_id
        WHERE im.file_id=f.id AND p.status='active' AND s.status='active' AND internal.has_entitlement(s.professional_id,'marketplace'))
      OR EXISTS(SELECT 1 FROM app.messages m JOIN app.conversations cv ON cv.id=m.conversation_id JOIN app.contracts c ON c.id=cv.contract_id
        WHERE m.file_id=f.id AND m.redacted_at IS NULL AND v IN(c.customer_id,c.professional_id));
  END IF;
  IF NOT allowed THEN RAISE EXCEPTION 'file_access_denied' USING ERRCODE='42501'; END IF;
  PERFORM internal.audit('file.read','file',f.id);
  RETURN f.object_key;
END $$;

CREATE FUNCTION api.kyc_case(p_case uuid,p_reason text) RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.require_permission('kyc.review'); result jsonb;
BEGIN
  IF length(trim(coalesce(p_reason,'')))<10 THEN RAISE EXCEPTION 'access_reason_required'; END IF;
  SELECT (to_jsonb(k)-'document_ciphertext')||jsonb_build_object('documents',(
    SELECT jsonb_agg(jsonb_build_object('kind',d.kind,'file_id',f.id,'object_key',f.object_key)) FROM app.kyc_documents d
    JOIN app.files f ON f.id=d.file_id WHERE d.case_id=k.id AND f.deleted_at IS NULL)) INTO result FROM app.kyc_cases k WHERE k.id=p_case;
  PERFORM internal.audit('kyc.read','kyc',p_case,jsonb_build_object('reason',p_reason));
  RETURN result;
END $$;

CREATE FUNCTION api.claim_notifications(p_limit integer DEFAULT 50) RETURNS SETOF jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
BEGIN
  IF p_limit IS NULL OR p_limit NOT BETWEEN 1 AND 100 THEN RAISE EXCEPTION 'invalid_batch'; END IF;
  RETURN QUERY WITH picked AS (
    SELECT id FROM app.outbox WHERE available_at<=now() AND (status='pending' OR (status='processing' AND locked_until<now()))
    ORDER BY available_at,id LIMIT p_limit FOR UPDATE SKIP LOCKED
  ), claimed AS (
    UPDATE app.outbox o SET status='processing',attempts=attempts+1,locked_until=now()+interval '2 minutes'
    FROM picked WHERE o.id=picked.id RETURNING o.*
  ) SELECT to_jsonb(claimed) FROM claimed;
END $$;

CREATE FUNCTION api.finish_notification(p_id uuid,p_attempt integer,p_success boolean,p_channel text,p_provider_id text,p_error text DEFAULT NULL) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE o app.outbox;
BEGIN
  SELECT * INTO o FROM app.outbox WHERE id=p_id FOR UPDATE;
  IF NOT FOUND OR o.status<>'processing' OR o.attempts<>p_attempt THEN RAISE EXCEPTION 'notification_lease_lost'; END IF;
  IF p_success IS NULL THEN RAISE EXCEPTION 'result_required'; END IF;
  INSERT INTO app.notification_deliveries(outbox_id,channel,provider_message_id,attempt,status)
  VALUES(o.id,p_channel,p_provider_id,p_attempt,CASE WHEN p_success THEN 'delivered' ELSE 'failed' END);
  UPDATE app.outbox SET status=CASE WHEN p_success THEN 'delivered' WHEN attempts>=8 THEN 'failed' ELSE 'pending' END,
    locked_until=NULL,delivered_at=CASE WHEN p_success THEN now() END,
    last_error=left(p_error,500),available_at=now()+make_interval(secs=>least(3600,30*power(2,least(attempts,7))::integer)) WHERE id=o.id;
END $$;

CREATE FUNCTION api.expire_requests() RETURNS integer
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v_count integer;
BEGIN
  WITH expired AS(UPDATE app.service_requests SET status='expired' WHERE status IN('draft','open') AND expires_at<=now() RETURNING id)
  UPDATE app.proposals SET status='rejected' WHERE request_id IN(SELECT id FROM expired) AND status='pending';
  GET DIAGNOSTICS v_count=ROW_COUNT;
  DELETE FROM app.professional_positions WHERE expires_at<=now();
  UPDATE app.location_shares SET revoked_at=now() WHERE revoked_at IS NULL AND expires_at<=now();
  RETURN v_count;
END $$;

-- Planes sin precios predeterminados; importes siempre en unidades menores.
CREATE FUNCTION api.configure_plan(p_code text,p_name text,p_credits integer,p_price bigint,p_currency text,
  p_weight integer,p_positioning_days integer,p_marketplace_days integer,p_active boolean) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.require_permission('plans.manage'); v_id uuid;
BEGIN
  INSERT INTO app.plans(code,name,credits,price_minor,currency,positioning_weight,positioning_days,marketplace_days,active)
  VALUES(p_code,p_name,p_credits,p_price,p_currency,p_weight,p_positioning_days,p_marketplace_days,p_active)
  ON CONFLICT(code) DO UPDATE SET name=EXCLUDED.name,credits=EXCLUDED.credits,price_minor=EXCLUDED.price_minor,currency=EXCLUDED.currency,
    positioning_weight=EXCLUDED.positioning_weight,positioning_days=EXCLUDED.positioning_days,marketplace_days=EXCLUDED.marketplace_days,active=EXCLUDED.active
  RETURNING id INTO v_id;
  PERFORM internal.audit('plan.configured','plan',v_id);
  RETURN v_id;
END $$;

GRANT EXECUTE ON FUNCTION api.my_account(),api.my_professional(),api.add_professional_contact(uuid,text,text,text),
  api.professional_reviews(uuid,integer,integer),api.attach_request_file(uuid,uuid),api.attach_kyc_file(uuid,uuid,text),api.file_object_key(uuid) TO chamba_api;
GRANT EXECUTE ON FUNCTION api.moderate_file(uuid,boolean,text),api.kyc_case(uuid,text),
  api.configure_plan(text,text,integer,bigint,text,integer,integer,integer,boolean) TO chamba_admin;
GRANT EXECUTE ON FUNCTION api.register_upload(uuid,text,text,bigint,text,text,boolean),api.claim_notifications(integer),
  api.finish_notification(uuid,integer,boolean,text,text,text),api.expire_requests() TO chamba_worker;

-- Auditoría de cambios sin copiar contraseñas, teléfonos, coordenadas ni documentos.
CREATE FUNCTION internal.audit_row_change() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE data jsonb; v_id uuid;
BEGIN
  data:=CASE WHEN TG_OP='DELETE' THEN to_jsonb(OLD) ELSE to_jsonb(NEW) END;
  v_id:=coalesce(data->>'id',data->>'user_id',data->>'professional_id',data->>'contract_id',data->>'order_id')::uuid;
  PERFORM internal.audit(lower(TG_OP),TG_TABLE_NAME,v_id,jsonb_build_object('table',TG_TABLE_NAME));
  RETURN NULL;
END $$;
DO $$ DECLARE t text; BEGIN
  FOREACH t IN ARRAY ARRAY['users','credentials','auth_identities','sessions','user_roles','user_contacts','addresses','user_consents',
    'professionals','branches','professional_contacts','branch_hours','offerings','coverage_areas','portfolio_items','professional_credentials',
    'kyc_cases','files','plans','credit_purchases','entitlements','service_requests','request_locations','proposals','contracts','contract_terms',
    'location_shares','reviews','stores','products','orders','order_delivery','external_payment_reports','support_tickets'] LOOP
    EXECUTE format('CREATE TRIGGER audit_change AFTER INSERT OR UPDATE OR DELETE ON app.%I FOR EACH ROW EXECUTE FUNCTION internal.audit_row_change()',t);
  END LOOP;
END $$;
