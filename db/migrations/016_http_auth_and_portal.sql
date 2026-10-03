ALTER TABLE app.sessions ADD COLUMN family_id uuid NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE app.sessions ADD COLUMN rotated_from uuid REFERENCES app.sessions;
CREATE TABLE app.http_rate_limits(bucket text PRIMARY KEY, hits integer NOT NULL, expires_at timestamptz NOT NULL);
CREATE TABLE app.notifications (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), user_id uuid NOT NULL REFERENCES app.users,
  outbox_id uuid UNIQUE REFERENCES app.outbox, topic text NOT NULL, aggregate_id uuid NOT NULL,
  read_at timestamptz, created_at timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE app.http_rate_limits ENABLE ROW LEVEL SECURITY;
ALTER TABLE app.notifications ENABLE ROW LEVEL SECURITY;
CREATE INDEX notifications_user_idx ON app.notifications(user_id,created_at DESC);

CREATE FUNCTION api.auth_rate_limit(p_bucket text,p_limit integer,p_seconds integer) RETURNS boolean
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v integer;
BEGIN
  INSERT INTO app.http_rate_limits(bucket,hits,expires_at) VALUES(p_bucket,1,now()+make_interval(secs=>p_seconds))
  ON CONFLICT(bucket) DO UPDATE SET hits=CASE WHEN app.http_rate_limits.expires_at<=now() THEN 1 ELSE app.http_rate_limits.hits+1 END,
    expires_at=CASE WHEN app.http_rate_limits.expires_at<=now() THEN EXCLUDED.expires_at ELSE app.http_rate_limits.expires_at END RETURNING hits INTO v;
  RETURN v<=p_limit;
END $$;

CREATE FUNCTION api.auth_record_attempt(p_user uuid,p_success boolean) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
BEGIN
  UPDATE app.credentials SET failed_attempts=CASE WHEN p_success THEN 0 ELSE failed_attempts+1 END,
    locked_until=CASE WHEN NOT p_success AND failed_attempts+1>=5 THEN now()+interval '15 minutes' ELSE NULL END WHERE user_id=p_user;
END $$;

CREATE FUNCTION api.auth_principal(p_session uuid,p_user uuid) RETURNS jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  SELECT jsonb_build_object('id',u.id,'email',u.email,'name',u.display_name,'email_verified',u.email_verified_at IS NOT NULL,
    'roles',coalesce((SELECT jsonb_agg(role_code) FROM app.user_roles WHERE user_id=u.id),'[]'::jsonb),
    'permissions',coalesce((SELECT jsonb_agg(DISTINCT rp.permission_code) FROM app.user_roles ur JOIN app.role_permissions rp ON rp.role_code=ur.role_code WHERE ur.user_id=u.id),'[]'::jsonb))
  FROM app.sessions s JOIN app.users u ON u.id=s.user_id WHERE s.id=p_session AND u.id=p_user AND u.status='active' AND s.revoked_at IS NULL AND s.expires_at>now();
$$;

CREATE FUNCTION api.auth_rotate_session(p_old_hash text,p_new_hash text,p_expires timestamptz) RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE s app.sessions; n uuid;
BEGIN
  SELECT * INTO s FROM app.sessions WHERE refresh_token_hash=p_old_hash FOR UPDATE;
  IF NOT FOUND THEN RETURN jsonb_build_object('error','invalid_session'); END IF;
  IF s.revoked_at IS NOT NULL THEN
    UPDATE app.sessions SET revoked_at=coalesce(revoked_at,now()) WHERE family_id=s.family_id;
    RETURN jsonb_build_object('error','refresh_reuse_detected');
  END IF;
  IF s.expires_at<=now() OR NOT EXISTS(SELECT 1 FROM app.users WHERE id=s.user_id AND status='active') THEN RETURN jsonb_build_object('error','invalid_session'); END IF;
  UPDATE app.sessions SET revoked_at=now() WHERE id=s.id;
  INSERT INTO app.sessions(user_id,refresh_token_hash,device_label,expires_at,family_id,rotated_from)
  VALUES(s.user_id,p_new_hash,s.device_label,p_expires,s.family_id,s.id) RETURNING id INTO n;
  RETURN jsonb_build_object('session_id',n,'user_id',s.user_id);
END $$;

CREATE FUNCTION api.auth_revoke_refresh(p_hash text) RETURNS void
LANGUAGE sql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  UPDATE app.sessions SET revoked_at=coalesce(revoked_at,now()) WHERE family_id IN(SELECT family_id FROM app.sessions WHERE refresh_token_hash=p_hash);
$$;

CREATE FUNCTION api.auth_mfa(p_user uuid) RETURNS jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  SELECT jsonb_build_object('enabled',mfa_enabled_at IS NOT NULL,'secret',encode(mfa_secret_ciphertext,'base64')) FROM app.credentials WHERE user_id=p_user;
$$;
CREATE FUNCTION api.auth_set_mfa(p_user uuid,p_encrypted bytea,p_enabled boolean) RETURNS void
LANGUAGE sql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  UPDATE app.credentials SET mfa_secret_ciphertext=p_encrypted,mfa_enabled_at=CASE WHEN p_enabled THEN now() END WHERE user_id=p_user;
$$;

CREATE FUNCTION api.auth_change_password(p_user uuid,p_hash text) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
BEGIN
  UPDATE app.credentials SET password_hash=p_hash,password_changed_at=now(),failed_attempts=0,locked_until=NULL WHERE user_id=p_user;
  UPDATE app.sessions SET revoked_at=now() WHERE user_id=p_user AND revoked_at IS NULL;
END $$;

CREATE FUNCTION api.auth_challenge(p_email text,p_purpose text,p_hash text) RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE u app.users;
BEGIN
  IF p_purpose NOT IN('email_verification','password_reset') THEN RAISE EXCEPTION 'invalid_challenge'; END IF;
  SELECT * INTO u FROM app.users WHERE email=lower(trim(p_email)) AND status='active';
  IF NOT FOUND THEN RETURN NULL; END IF;
  INSERT INTO app.auth_challenges(user_id,purpose,token_hash,expires_at) VALUES(u.id,p_purpose,p_hash,now()+interval '30 minutes');
  RETURN jsonb_build_object('user_id',u.id,'email',u.email);
END $$;

CREATE FUNCTION api.auth_consume_challenge(p_hash text,p_purpose text,p_password_hash text DEFAULT NULL) RETURNS boolean
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE c app.auth_challenges;
BEGIN
  SELECT * INTO c FROM app.auth_challenges WHERE token_hash=p_hash AND purpose=p_purpose FOR UPDATE;
  IF NOT FOUND OR c.consumed_at IS NOT NULL OR c.expires_at<=now() THEN RETURN false; END IF;
  IF p_purpose='password_reset' THEN
    IF p_password_hash IS NULL THEN RETURN false; END IF;
    PERFORM api.auth_change_password(c.user_id,p_password_hash);
  ELSIF p_purpose='email_verification' THEN UPDATE app.users SET email_verified_at=now() WHERE id=c.user_id;
  ELSE RETURN false; END IF;
  UPDATE app.auth_challenges SET consumed_at=now() WHERE id=c.id;
  RETURN true;
END $$;

CREATE FUNCTION api.catalog() RETURNS SETOF jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  SELECT to_jsonb(c) FROM app.categories c WHERE active ORDER BY name;
$$;
CREATE FUNCTION api.discover(p_category uuid DEFAULT NULL,p_mode text DEFAULT NULL,p_limit integer DEFAULT 30,p_offset integer DEFAULT 0) RETURNS SETOF jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
BEGIN
  IF p_limit IS NULL OR p_limit NOT BETWEEN 1 AND 100 OR p_offset IS NULL OR p_offset<0 THEN RAISE EXCEPTION 'invalid_pagination'; END IF;
  RETURN QUERY SELECT jsonb_build_object('id',p.user_id,'slug',p.slug,'name',p.business_name,'bio',p.bio,'years_experience',p.years_experience,
    'requires_platform_payment',u.requires_platform_payment,
    'verified',EXISTS(SELECT 1 FROM app.kyc_cases k WHERE k.user_id=p.user_id AND k.status='approved' AND k.expires_at>now()),
    'rating',(SELECT round(avg(rating),2) FROM app.reviews WHERE subject_id=p.user_id AND moderation_status='published'),
    'review_count',(SELECT count(*) FROM app.reviews WHERE subject_id=p.user_id AND moderation_status='published'),
    'city',(SELECT city FROM app.branches WHERE professional_id=p.user_id AND active ORDER BY created_at LIMIT 1),
    'offerings',coalesce((SELECT jsonb_agg(jsonb_build_object('id',o.id,'category_id',o.category_id,'title',o.title,'description',o.description,
      'delivery_mode',o.delivery_mode,'currency',o.currency,'price_min_minor',o.price_min_minor,'pricing_type',o.pricing_type))
      FROM app.offerings o WHERE o.professional_id=p.user_id AND o.active),'[]'::jsonb))
  FROM app.professionals p JOIN app.users u ON u.id=p.user_id WHERE p.status='active' AND u.status='active'
    AND EXISTS(SELECT 1 FROM app.offerings o WHERE o.professional_id=p.user_id AND o.active AND (p_category IS NULL OR o.category_id=p_category) AND (p_mode IS NULL OR o.delivery_mode=p_mode))
  ORDER BY p.created_at,p.user_id LIMIT p_limit OFFSET p_offset;
END $$;

CREATE FUNCTION api.dashboard() RETURNS jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor();
BEGIN
  RETURN jsonb_build_object('requests',(SELECT count(*) FROM app.service_requests WHERE customer_id=v AND status IN('draft','open')),
    'active_contracts',(SELECT count(*) FROM app.contracts WHERE v IN(customer_id,professional_id) AND status NOT IN('completed','cancelled')),
    'completed_contracts',(SELECT count(*) FROM app.contracts WHERE v IN(customer_id,professional_id) AND status='completed'),
    'credit_balance',(SELECT balance FROM app.credit_wallets WHERE professional_id=v),
    'unread_notifications',(SELECT count(*) FROM app.notifications WHERE user_id=v AND read_at IS NULL));
END $$;

CREATE FUNCTION api.contract_detail(p_contract uuid) RETURNS jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE c app.contracts:=internal.participant(p_contract);
BEGIN
  RETURN to_jsonb(c)||jsonb_build_object('title',(SELECT title FROM app.service_requests WHERE id=c.request_id),
    'customer_name',(SELECT display_name FROM app.users WHERE id=c.customer_id),
    'professional_name',(SELECT business_name FROM app.professionals WHERE user_id=c.professional_id),
    'terms',(SELECT jsonb_agg(to_jsonb(t) ORDER BY version DESC) FROM app.contract_terms t WHERE contract_id=c.id),
    'events',(SELECT jsonb_agg(to_jsonb(e) ORDER BY id) FROM app.contract_events e WHERE contract_id=c.id));
END $$;

CREATE FUNCTION api.my_proposals() RETURNS SETOF jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  SELECT to_jsonb(p)||jsonb_build_object('title',r.title) FROM app.proposals p JOIN app.service_requests r ON r.id=p.request_id
  WHERE p.professional_id=internal.actor() ORDER BY p.created_at DESC LIMIT 200;
$$;
CREATE FUNCTION api.my_sessions() RETURNS SETOF jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  SELECT jsonb_build_object('id',id,'device_label',device_label,'created_at',created_at,'expires_at',expires_at)
  FROM app.sessions WHERE user_id=internal.actor() AND revoked_at IS NULL AND expires_at>now() ORDER BY created_at DESC;
$$;
CREATE FUNCTION api.my_files() RETURNS SETOF jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  SELECT to_jsonb(f)-'object_key' FROM app.files f WHERE owner_id=internal.actor() AND deleted_at IS NULL ORDER BY created_at DESC LIMIT 200;
$$;
CREATE FUNCTION api.my_kyc() RETURNS SETOF jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  SELECT to_jsonb(k)-'document_ciphertext'-'identity_fingerprint' FROM app.kyc_cases k WHERE user_id=internal.actor() ORDER BY submitted_at DESC;
$$;
CREATE FUNCTION api.my_topups() RETURNS SETOF jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  SELECT to_jsonb(t)||jsonb_build_object('currency',w.currency) FROM app.money_topups t JOIN app.money_wallets w ON w.id=t.wallet_id
  WHERE w.user_id=internal.actor() ORDER BY t.created_at DESC LIMIT 100;
$$;
CREATE FUNCTION api.my_credit_purchases() RETURNS SETOF jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  SELECT to_jsonb(p) FROM app.credit_purchases p WHERE professional_id=internal.actor() ORDER BY created_at DESC LIMIT 100;
$$;

CREATE FUNCTION api.update_account(p_name text,p_locale text DEFAULT 'es',p_timezone text DEFAULT 'UTC') RETURNS void
LANGUAGE sql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  UPDATE app.users SET display_name=p_name,locale=p_locale,timezone=p_timezone,updated_at=now() WHERE id=internal.actor();
$$;
CREATE FUNCTION api.archive_address(p_address uuid) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
BEGIN
  UPDATE app.addresses SET archived_at=now(),is_default=false WHERE id=p_address AND user_id=internal.actor();
  IF NOT FOUND THEN RAISE EXCEPTION 'address_not_owned'; END IF;
END $$;
CREATE FUNCTION api.remove_contact(p_contact uuid) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
BEGIN
  DELETE FROM app.user_contacts WHERE id=p_contact AND user_id=internal.actor();
  IF NOT FOUND THEN RAISE EXCEPTION 'contact_not_owned'; END IF;
END $$;

CREATE FUNCTION api.my_tickets() RETURNS SETOF jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  SELECT to_jsonb(t) FROM app.support_tickets t WHERE opened_by=internal.actor() ORDER BY created_at DESC LIMIT 200;
$$;
CREATE FUNCTION api.ticket_thread(p_ticket uuid) RETURNS jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor(); t app.support_tickets;
BEGIN
  SELECT * INTO t FROM app.support_tickets WHERE id=p_ticket AND opened_by=v;
  IF NOT FOUND THEN RAISE EXCEPTION 'ticket_not_owned' USING ERRCODE='42501'; END IF;
  RETURN to_jsonb(t)||jsonb_build_object('messages',(SELECT jsonb_agg(to_jsonb(m) ORDER BY created_at) FROM app.support_messages m WHERE ticket_id=t.id AND NOT internal_only));
END $$;
CREATE FUNCTION api.reply_ticket(p_ticket uuid,p_body text) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor(); v_id uuid;
BEGIN
  IF length(trim(p_body)) NOT BETWEEN 1 AND 10000 THEN RAISE EXCEPTION 'invalid_message'; END IF;
  IF NOT EXISTS(SELECT 1 FROM app.support_tickets WHERE id=p_ticket AND opened_by=v) THEN PERFORM internal.require_permission('support.manage'); END IF;
  INSERT INTO app.support_messages(ticket_id,author_id,body) VALUES(p_ticket,v,p_body) RETURNING id INTO v_id;
  RETURN v_id;
END $$;

CREATE FUNCTION api.my_notifications() RETURNS SETOF jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  SELECT to_jsonb(n) FROM app.notifications n WHERE user_id=internal.actor() ORDER BY created_at DESC LIMIT 100;
$$;
CREATE FUNCTION api.read_notification(p_notification uuid) RETURNS void
LANGUAGE sql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  UPDATE app.notifications SET read_at=coalesce(read_at,now()) WHERE id=p_notification AND user_id=internal.actor();
$$;
CREATE FUNCTION api.deliver_in_app(p_outbox uuid,p_attempt integer) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE o app.outbox;
BEGIN
  SELECT * INTO o FROM app.outbox WHERE id=p_outbox AND status='processing' AND attempts=p_attempt FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'notification_lease_lost'; END IF;
  IF o.recipient_id IS NOT NULL THEN
    INSERT INTO app.notifications(user_id,outbox_id,topic,aggregate_id) VALUES(o.recipient_id,o.id,o.topic,o.aggregate_id) ON CONFLICT(outbox_id) DO NOTHING;
  END IF;
  PERFORM api.finish_notification(o.id,p_attempt,true,'in_app',o.id::text);
END $$;

GRANT EXECUTE ON FUNCTION api.auth_rate_limit(text,integer,integer),api.auth_record_attempt(uuid,boolean),api.auth_principal(uuid,uuid),
  api.auth_rotate_session(text,text,timestamptz),api.auth_revoke_refresh(text),api.auth_mfa(uuid),api.auth_set_mfa(uuid,bytea,boolean),
  api.auth_change_password(uuid,text),api.auth_challenge(text,text,text),api.auth_consume_challenge(text,text,text) TO chamba_auth;
GRANT EXECUTE ON FUNCTION api.catalog(),api.discover(uuid,text,integer,integer),api.dashboard(),api.contract_detail(uuid),api.my_proposals(),
  api.my_sessions(),api.my_files(),api.my_kyc(),api.my_topups(),api.my_credit_purchases(),api.update_account(text,text,text),api.archive_address(uuid),
  api.remove_contact(uuid),api.my_tickets(),api.ticket_thread(uuid),api.reply_ticket(uuid,text),api.my_notifications(),api.read_notification(uuid) TO chamba_api;
GRANT EXECUTE ON FUNCTION api.deliver_in_app(uuid,integer) TO chamba_worker;
