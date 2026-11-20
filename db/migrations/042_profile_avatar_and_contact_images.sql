-- 042_profile_avatar_and_contact_images.sql
--   1. Foto de perfil de usuario en app.users y api.auth_principal / api.my_account
--   2. Imagen / comprobante de contacto en app.user_contacts
--   3. api.set_avatar(p_file uuid) para actualizar la foto de perfil del usuario y profesional
--   4. api.save_contact con soporte para imagen/avatar
--   5. Auto-aprobación y lectura de archivos con propósito 'avatar'

ALTER TABLE app.users ADD COLUMN IF NOT EXISTS avatar_file_id uuid REFERENCES app.files;
ALTER TABLE app.user_contacts ADD COLUMN IF NOT EXISTS avatar_file_id uuid REFERENCES app.files;

-- 1. Auto-aprobación para 'avatar' limpio sin EXIF
CREATE OR REPLACE FUNCTION api.register_upload(p_owner uuid, p_object_key text, p_mime text, p_bytes bigint, p_sha256 text, p_purpose text, p_exif_removed boolean)
RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v_id uuid;
BEGIN
  INSERT INTO app.files(owner_id,object_key,mime_type,bytes,sha256,purpose,exif_removed,moderation_status)
  VALUES(p_owner,p_object_key,p_mime,p_bytes,p_sha256,p_purpose,p_exif_removed,
    CASE WHEN p_purpose IN ('avatar','portfolio','evidence','product','request','chat') AND p_exif_removed
         THEN 'approved' ELSE 'pending' END)
  RETURNING id INTO v_id;
  RETURN v_id;
END $$;

-- 2. api.set_avatar para actualizar el avatar del usuario y profesional
CREATE OR REPLACE FUNCTION api.set_avatar(p_file uuid) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid := internal.actor();
BEGIN
  IF p_file IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM app.files WHERE id=p_file AND owner_id=v AND deleted_at IS NULL
  ) THEN
    RAISE EXCEPTION 'file_access_denied' USING ERRCODE='42501';
  END IF;
  UPDATE app.users SET avatar_file_id = p_file, updated_at = now() WHERE id = v;
  UPDATE app.professionals SET avatar_file_id = p_file, updated_at = now() WHERE user_id = v;
  PERFORM internal.audit('user.avatar_updated', 'user', v, jsonb_build_object('file_id', p_file));
END $$;

REVOKE ALL ON FUNCTION api.set_avatar(uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION api.set_avatar(uuid) TO chamba_api;

-- 3. api.save_contact con parámetro opcional p_avatar
DROP FUNCTION IF EXISTS api.save_contact(text, text, text, boolean);
DROP FUNCTION IF EXISTS api.save_contact(text, text, text, boolean, uuid);
CREATE OR REPLACE FUNCTION api.save_contact(p_kind text, p_label text, p_value text, p_share boolean DEFAULT true, p_avatar uuid DEFAULT NULL)
RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid := internal.actor(); v_id uuid;
BEGIN
  IF p_avatar IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM app.files WHERE id=p_avatar AND owner_id=v AND deleted_at IS NULL
  ) THEN
    RAISE EXCEPTION 'file_access_denied' USING ERRCODE='42501';
  END IF;

  INSERT INTO app.user_contacts(user_id, kind, label, value, share_on_contract, avatar_file_id)
  VALUES(v, p_kind, p_label, p_value, p_share, p_avatar)
  ON CONFLICT(user_id, kind, value) DO UPDATE
    SET label=EXCLUDED.label, share_on_contract=EXCLUDED.share_on_contract, avatar_file_id=coalesce(EXCLUDED.avatar_file_id, app.user_contacts.avatar_file_id)
  RETURNING id INTO v_id;
  RETURN v_id;
END $$;

REVOKE ALL ON FUNCTION api.save_contact(text, text, text, boolean, uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION api.save_contact(text, text, text, boolean, uuid) TO chamba_api;

-- 4. api.auth_principal con avatar_file_id
CREATE OR REPLACE FUNCTION api.auth_principal(p_session uuid, p_user uuid) RETURNS jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  SELECT jsonb_build_object(
    'id', u.id,
    'email', u.email,
    'name', u.display_name,
    'email_verified', u.email_verified_at IS NOT NULL,
    'account_type', coalesce(u.account_type, 'natural'),
    'business_name', u.business_name,
    'rif', u.rif,
    'contact_name', u.contact_name,
    'avatar_file_id', u.avatar_file_id,
    'roles', coalesce((SELECT jsonb_agg(role_code) FROM app.user_roles WHERE user_id=u.id), '[]'::jsonb),
    'permissions', coalesce((SELECT jsonb_agg(DISTINCT rp.permission_code) FROM app.user_roles ur JOIN app.role_permissions rp ON rp.role_code=ur.role_code WHERE ur.user_id=u.id), '[]'::jsonb)
  )
  FROM app.sessions s JOIN app.users u ON u.id=s.user_id
  WHERE s.id=p_session AND u.id=p_user AND u.status='active' AND s.revoked_at IS NULL AND s.expires_at>now();
$$;

-- 5. api.my_account con avatar_file_id
CREATE OR REPLACE FUNCTION api.my_account() RETURNS jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  SELECT jsonb_build_object(
    'id', u.id,
    'email', u.email,
    'name', u.display_name,
    'status', u.status,
    'account_type', coalesce(u.account_type, 'natural'),
    'business_name', u.business_name,
    'rif', u.rif,
    'contact_name', u.contact_name,
    'avatar_file_id', u.avatar_file_id,
    'roles', (SELECT jsonb_agg(role_code) FROM app.user_roles WHERE user_id=u.id),
    'contacts', (SELECT jsonb_agg(to_jsonb(c)) FROM app.user_contacts c WHERE c.user_id=u.id),
    'addresses', (SELECT jsonb_agg((to_jsonb(a)-'location')||jsonb_build_object('longitude', public.ST_X(a.location::public.geometry), 'latitude', public.ST_Y(a.location::public.geometry))) FROM app.addresses a WHERE a.user_id=u.id AND a.archived_at IS NULL)
  )
  FROM app.users u WHERE u.id=internal.actor();
$$;

-- 6. Lectura de avatares públicos
CREATE OR REPLACE FUNCTION api.file_object_key(p_file uuid) RETURNS text
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor(); f app.files; allowed boolean:=false;
BEGIN
  SELECT * INTO f FROM app.files WHERE id=p_file AND deleted_at IS NULL;
  IF NOT FOUND THEN RAISE EXCEPTION 'file_access_denied' USING ERRCODE='42501'; END IF;
  IF f.owner_id=v THEN allowed:=true;
  ELSIF f.purpose = 'avatar' AND f.moderation_status='approved' THEN
    allowed:=true;
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
