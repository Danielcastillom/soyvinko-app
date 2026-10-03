-- Customer submission publishes immediately; administrators can revoke it afterwards.
CREATE OR REPLACE FUNCTION api.publish_request(p_request uuid) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor(); r app.service_requests; file_id uuid;
BEGIN
  SELECT * INTO r FROM app.service_requests WHERE id=p_request AND customer_id=v FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'request_not_owned' USING ERRCODE='42501'; END IF;
  IF r.status='open' AND r.moderation_status='approved' THEN RETURN; END IF;
  IF r.status<>'draft' OR r.moderation_status='rejected' OR r.expires_at<=now()
    OR (r.moderation_status='pending' AND (SELECT action FROM app.moderation_actions WHERE target_type='request' AND target_id=r.id
        ORDER BY created_at DESC,id DESC LIMIT 1)='reject') THEN
    RAISE EXCEPTION 'request_not_publishable'; END IF;
  PERFORM 1 FROM app.files f JOIN app.request_attachments ra ON ra.file_id=f.id
    WHERE ra.request_id=r.id ORDER BY f.id FOR UPDATE OF f;
  IF EXISTS(SELECT 1 FROM app.request_attachments ra JOIN app.files f ON f.id=ra.file_id
    WHERE ra.request_id=r.id AND (f.owner_id<>v OR f.purpose<>'request' OR f.moderation_status='rejected'
      OR NOT f.exif_removed OR f.deleted_at IS NOT NULL OR f.mime_type NOT IN('image/jpeg','image/png','image/webp')
      OR f.bytes NOT BETWEEN 1 AND 10485760)) THEN RAISE EXCEPTION 'unsafe_attachment'; END IF;
  IF r.mode='direct' AND NOT internal.eligible(r.target_professional_id,r.id) THEN
    RAISE EXCEPTION 'professional_outside_coverage'; END IF;
  FOR file_id IN
    UPDATE app.files f SET moderation_status='approved'
    FROM app.request_attachments ra WHERE ra.file_id=f.id AND ra.request_id=r.id AND f.moderation_status='pending'
    RETURNING f.id
  LOOP
    PERFORM internal.audit('file.auto_approved','file',file_id,jsonb_build_object('policy','request_publication','request_id',r.id));
  END LOOP;
  UPDATE app.service_requests SET status='open',moderation_status='approved' WHERE id=r.id;
  INSERT INTO app.outbox(topic,aggregate_id,recipient_id,payload,deduplication_key)
    SELECT 'request.available',r.id,p.user_id,jsonb_build_object('request_id',r.id),'request:'||r.id||':'||p.user_id
    FROM app.professionals p WHERE internal.eligible(p.user_id,r.id)
    ON CONFLICT(deduplication_key) DO NOTHING;
  PERFORM internal.audit('request.published','request',r.id,jsonb_build_object('moderation','automatic'));
END $$;

CREATE OR REPLACE FUNCTION api.request_offering(p_offering uuid,p_description text,p_currency text,p_address uuid,
  p_expires_at timestamptz,p_key uuid,p_files uuid[] DEFAULT '{}',p_protect boolean DEFAULT false) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE ident uuid;
BEGIN
  ident:=internal.create_customer_need(p_offering,NULL,NULL,NULL,p_description,p_currency,p_address,NULL,NULL,
    p_expires_at,p_key,p_files,p_protect);
  -- A repeated creation must not reopen cancelled, matched or administrator-rejected requests.
  IF EXISTS(SELECT 1 FROM app.service_requests WHERE id=ident AND status='draft' AND moderation_status<>'rejected') THEN
    PERFORM api.publish_request(ident);
  END IF;
  RETURN ident;
END $$;

CREATE OR REPLACE FUNCTION api.create_customer_request(p_category uuid,p_delivery_mode text,p_title text,p_description text,
  p_currency text,p_address uuid,p_budget bigint,p_radius_m integer,p_expires_at timestamptz,p_key uuid,
  p_files uuid[] DEFAULT '{}',p_protect boolean DEFAULT false) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE ident uuid;
BEGIN
  ident:=internal.create_customer_need(NULL,p_category,p_delivery_mode,p_title,p_description,p_currency,p_address,
    p_budget,p_radius_m,p_expires_at,p_key,p_files,p_protect);
  IF EXISTS(SELECT 1 FROM app.service_requests WHERE id=ident AND status='draft' AND moderation_status<>'rejected') THEN
    PERFORM api.publish_request(ident);
  END IF;
  RETURN ident;
END $$;

CREATE OR REPLACE FUNCTION api.remove_request_image(p_request uuid,p_file uuid) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
BEGIN
  PERFORM 1 FROM app.service_requests WHERE id=p_request AND customer_id=internal.actor() AND status='draft' FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'request_not_editable'; END IF;
  DELETE FROM app.request_attachments WHERE request_id=p_request AND file_id=p_file;
  UPDATE app.service_requests SET moderation_status=CASE WHEN moderation_status='rejected' THEN 'rejected' ELSE 'pending' END
    WHERE id=p_request;
END $$;

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
  UPDATE app.service_requests SET moderation_status=CASE WHEN moderation_status='rejected' THEN 'rejected' ELSE 'pending' END
    WHERE id=p_request;
END $$;
