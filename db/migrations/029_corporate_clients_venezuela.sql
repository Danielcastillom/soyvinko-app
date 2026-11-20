-- Soporte para clientes corporativos / jurídicos de Venezuela (empresas con RIF).
DO $$ BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema='app' AND table_name='users' AND column_name='account_type'
  ) THEN
    ALTER TABLE app.users ADD COLUMN account_type text NOT NULL DEFAULT 'natural' CHECK (account_type IN ('natural', 'juridico'));
    ALTER TABLE app.users ADD COLUMN business_name text;
    ALTER TABLE app.users ADD COLUMN rif text;
    ALTER TABLE app.users ADD COLUMN contact_name text;
  END IF;
END $$;

INSERT INTO app.roles (code, description)
VALUES ('company', 'Cliente corporativo / Empresa')
ON CONFLICT (code) DO UPDATE SET description = EXCLUDED.description;

DROP FUNCTION IF EXISTS api.register_user(text, text, text);
CREATE OR REPLACE FUNCTION api.register_user(
  p_email text,
  p_name text,
  p_argon2id_hash text,
  p_account_type text DEFAULT 'natural',
  p_business_name text DEFAULT NULL,
  p_rif text DEFAULT NULL,
  p_contact_name text DEFAULT NULL
) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE
  v uuid;
  v_display text;
BEGIN
  IF p_account_type NOT IN ('natural', 'juridico') THEN
    RAISE EXCEPTION 'invalid_account_type';
  END IF;

  IF p_account_type = 'juridico' THEN
    IF p_business_name IS NULL OR length(trim(p_business_name)) < 2 THEN
      RAISE EXCEPTION 'invalid_business_name';
    END IF;
    IF p_rif IS NULL OR length(trim(p_rif)) < 6 THEN
      RAISE EXCEPTION 'invalid_rif';
    END IF;
    v_display := trim(p_business_name);
  ELSE
    v_display := trim(p_name);
  END IF;

  INSERT INTO app.users(
    email,
    display_name,
    account_type,
    business_name,
    rif,
    contact_name
  ) VALUES (
    lower(trim(p_email)),
    v_display,
    p_account_type,
    nullif(trim(p_business_name), ''),
    nullif(upper(trim(p_rif)), ''),
    nullif(trim(p_contact_name), '')
  ) RETURNING id INTO v;

  INSERT INTO app.credentials(user_id, password_hash) VALUES(v, p_argon2id_hash);
  INSERT INTO app.user_roles(user_id, role_code) VALUES(v, 'customer');
  IF p_account_type = 'juridico' THEN
    INSERT INTO app.user_roles(user_id, role_code) VALUES(v, 'company') ON CONFLICT DO NOTHING;
  END IF;

  PERFORM internal.audit('user.registered', 'user', v, jsonb_build_object('account_type', p_account_type));
  RETURN v;
END $$;

GRANT EXECUTE ON FUNCTION api.register_user(text, text, text, text, text, text, text) TO chamba_auth;

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
    'roles', coalesce((SELECT jsonb_agg(role_code) FROM app.user_roles WHERE user_id=u.id), '[]'::jsonb),
    'permissions', coalesce((SELECT jsonb_agg(DISTINCT rp.permission_code) FROM app.user_roles ur JOIN app.role_permissions rp ON rp.role_code=ur.role_code WHERE ur.user_id=u.id), '[]'::jsonb)
  )
  FROM app.sessions s JOIN app.users u ON u.id=s.user_id
  WHERE s.id=p_session AND u.id=p_user AND u.status='active' AND s.revoked_at IS NULL AND s.expires_at>now();
$$;

GRANT EXECUTE ON FUNCTION api.auth_principal(uuid, uuid) TO chamba_auth, chamba_api;

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
    'roles', (SELECT jsonb_agg(role_code) FROM app.user_roles WHERE user_id=u.id),
    'contacts', (SELECT jsonb_agg(to_jsonb(c)) FROM app.user_contacts c WHERE c.user_id=u.id),
    'addresses', (SELECT jsonb_agg((to_jsonb(a)-'location')||jsonb_build_object('longitude', public.ST_X(a.location::public.geometry), 'latitude', public.ST_Y(a.location::public.geometry))) FROM app.addresses a WHERE a.user_id=u.id AND a.archived_at IS NULL)
  )
  FROM app.users u WHERE u.id=internal.actor();
$$;

GRANT EXECUTE ON FUNCTION api.my_account() TO chamba_api;

DROP FUNCTION IF EXISTS api.update_account(text, text, text);
CREATE OR REPLACE FUNCTION api.update_account(
  p_name text,
  p_locale text DEFAULT 'es'::text,
  p_timezone text DEFAULT 'UTC'::text,
  p_business_name text DEFAULT NULL,
  p_rif text DEFAULT NULL,
  p_contact_name text DEFAULT NULL
) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE
  v_user app.users;
  v_display text;
BEGIN
  SELECT * INTO v_user FROM app.users WHERE id=internal.actor() FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'user_not_found'; END IF;

  IF v_user.account_type = 'juridico' THEN
    v_display := coalesce(nullif(trim(p_business_name), ''), nullif(trim(p_name), ''), v_user.display_name);
  ELSE
    v_display := coalesce(nullif(trim(p_name), ''), v_user.display_name);
  END IF;

  UPDATE app.users SET
    display_name = v_display,
    business_name = CASE WHEN v_user.account_type = 'juridico' AND p_business_name IS NOT NULL THEN nullif(trim(p_business_name), '') ELSE business_name END,
    rif = CASE WHEN v_user.account_type = 'juridico' AND p_rif IS NOT NULL THEN nullif(upper(trim(p_rif)), '') ELSE rif END,
    contact_name = CASE WHEN v_user.account_type = 'juridico' AND p_contact_name IS NOT NULL THEN nullif(trim(p_contact_name), '') ELSE contact_name END,
    locale = coalesce(p_locale, locale),
    timezone = coalesce(p_timezone, timezone),
    updated_at = now()
  WHERE id = v_user.id;
END $$;

GRANT EXECUTE ON FUNCTION api.update_account(text, text, text, text, text, text) TO chamba_api;
