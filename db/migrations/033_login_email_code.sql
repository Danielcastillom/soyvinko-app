-- 033_login_email_code.sql: Soporte para códigos de autorización por correo electrónico (MFA / Login OTP)

CREATE OR REPLACE FUNCTION api.auth_challenge(p_email text,p_purpose text,p_hash text) RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE u app.users;
BEGIN
  IF p_purpose NOT IN('email_verification','password_reset','mfa') THEN RAISE EXCEPTION 'invalid_challenge'; END IF;
  SELECT * INTO u FROM app.users WHERE email=lower(trim(p_email)) AND status='active';
  IF NOT FOUND THEN RETURN NULL; END IF;
  
  -- Para códigos de autorización de inicio de sesión (mfa), invalidar códigos pendientes anteriores
  IF p_purpose = 'mfa' THEN
    UPDATE app.auth_challenges 
    SET consumed_at = now() 
    WHERE user_id = u.id AND purpose = 'mfa' AND consumed_at IS NULL;
    
    INSERT INTO app.auth_challenges(user_id,purpose,token_hash,expires_at) 
    VALUES(u.id, p_purpose, p_hash, now() + interval '10 minutes');
  ELSE
    INSERT INTO app.auth_challenges(user_id,purpose,token_hash,expires_at) 
    VALUES(u.id, p_purpose, p_hash, now() + interval '30 minutes');
  END IF;
  
  RETURN jsonb_build_object('user_id',u.id,'email',u.email);
END $$;

CREATE OR REPLACE FUNCTION api.auth_consume_challenge(p_hash text,p_purpose text,p_password_hash text DEFAULT NULL) RETURNS boolean
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE c app.auth_challenges;
BEGIN
  SELECT * INTO c FROM app.auth_challenges WHERE token_hash=p_hash AND purpose=p_purpose FOR UPDATE;
  IF NOT FOUND OR c.consumed_at IS NOT NULL OR c.expires_at<=now() THEN RETURN false; END IF;
  
  IF p_purpose='password_reset' THEN
    IF p_password_hash IS NULL THEN RETURN false; END IF;
    PERFORM api.auth_change_password(c.user_id,p_password_hash);
  ELSIF p_purpose='email_verification' THEN 
    UPDATE app.users SET email_verified_at=now() WHERE id=c.user_id;
  ELSIF p_purpose='mfa' THEN 
    -- Código de autorización de inicio de sesión verificado con éxito
    NULL;
  ELSE 
    RETURN false; 
  END IF;
  
  UPDATE app.auth_challenges SET consumed_at=now() WHERE id=c.id;
  RETURN true;
END $$;

GRANT EXECUTE ON FUNCTION api.auth_challenge(text,text,text), api.auth_consume_challenge(text,text,text) TO chamba_auth;
