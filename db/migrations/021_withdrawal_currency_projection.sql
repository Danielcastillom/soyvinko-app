CREATE OR REPLACE FUNCTION api.my_withdrawals() RETURNS SETOF jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
 SELECT (to_jsonb(wd)-'destination_token')||jsonb_build_object('currency',w.currency)
 FROM app.money_withdrawals wd JOIN app.money_wallets w ON w.id=wd.wallet_id
 WHERE w.user_id=internal.actor() ORDER BY wd.created_at DESC,wd.id LIMIT 100;
$$;

CREATE OR REPLACE FUNCTION api.admin_list(p_resource text,p_limit integer DEFAULT 50,p_offset integer DEFAULT 0) RETURNS SETOF jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE tbl text; perm text;
BEGIN
 IF p_limit NOT BETWEEN 1 AND 100 OR p_offset<0 THEN RAISE EXCEPTION 'invalid_pagination'; END IF;
 SELECT t,p INTO tbl,perm FROM (VALUES ('users','users','users.manage'),('requests','service_requests','content.moderate'),
 ('reviews','reviews','content.moderate'),('files','files','content.moderate'),('kyc','kyc_cases','kyc.review'),
 ('plans','plans','plans.manage'),('fees','withdrawal_fee_policies','money.configure'),('tickets','support_tickets','support.read'),
 ('withdrawals','money_withdrawals','money.configure'),('audit','audit_events','platform.read')) x(k,t,p) WHERE k=p_resource;
 IF tbl IS NULL THEN RAISE EXCEPTION 'unknown_resource'; END IF;
 PERFORM internal.require_permission(perm);
 IF p_resource='withdrawals' THEN
  RETURN QUERY SELECT (to_jsonb(wd)-'destination_token')||jsonb_build_object('currency',w.currency,'owner_name',u.display_name)
  FROM app.money_withdrawals wd JOIN app.money_wallets w ON w.id=wd.wallet_id JOIN app.users u ON u.id=w.user_id
  ORDER BY wd.created_at DESC,wd.id LIMIT p_limit OFFSET p_offset;
  RETURN;
 END IF;
 RETURN QUERY EXECUTE format('SELECT to_jsonb(t)-ARRAY[''document_ciphertext'',''identity_fingerprint'',''encryption_key_id'',''object_key'',''destination_token''] FROM app.%I t ORDER BY 1 LIMIT $1 OFFSET $2',tbl) USING p_limit,p_offset;
END $$;
