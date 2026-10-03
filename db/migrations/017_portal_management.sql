INSERT INTO app.permissions(code,description) VALUES('platform.read','Consultar métricas operativas'),('catalog.manage','Gestionar categorías');
INSERT INTO app.role_permissions SELECT r.code,p.code FROM app.roles r CROSS JOIN app.permissions p WHERE r.code IN('admin','superadmin') AND p.code IN('platform.read','catalog.manage');
CREATE FUNCTION api.admin_overview() RETURNS jsonb LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
BEGIN
 PERFORM internal.require_permission('platform.read');
 RETURN jsonb_build_object('users',(SELECT count(*) FROM app.users),'professionals',(SELECT count(*) FROM app.professionals),
 'contracts',(SELECT count(*) FROM app.contracts),'pending_requests',(SELECT count(*) FROM app.service_requests WHERE moderation_status='pending'),
 'pending_kyc',(SELECT count(*) FROM app.kyc_cases WHERE status IN('pending','in_review')),'open_tickets',(SELECT count(*) FROM app.support_tickets WHERE status NOT IN('closed','resolved')));
END $$;
CREATE FUNCTION api.admin_list(p_resource text,p_limit integer DEFAULT 50,p_offset integer DEFAULT 0) RETURNS SETOF jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE tbl text; perm text;
BEGIN
 IF p_limit NOT BETWEEN 1 AND 100 OR p_offset<0 THEN RAISE EXCEPTION 'invalid_pagination'; END IF;
 SELECT t,p INTO tbl,perm FROM (VALUES ('users','users','users.manage'),('requests','service_requests','content.moderate'),
 ('reviews','reviews','content.moderate'),('files','files','content.moderate'),('kyc','kyc_cases','kyc.review'),
 ('plans','plans','plans.manage'),('fees','withdrawal_fee_policies','money.configure'),('tickets','support_tickets','support.read'),
 ('withdrawals','withdrawals','money.configure'),('audit','audit_events','platform.read')) x(k,t,p) WHERE k=p_resource;
 IF tbl IS NULL THEN RAISE EXCEPTION 'unknown_resource'; END IF;
 PERFORM internal.require_permission(perm);
 RETURN QUERY EXECUTE format('SELECT to_jsonb(t)-ARRAY[''document_ciphertext'',''identity_fingerprint'',''encryption_key_id'',''object_key'',''destination_token''] FROM app.%I t ORDER BY 1 LIMIT $1 OFFSET $2',tbl) USING p_limit,p_offset;
END $$;
CREATE FUNCTION api.configure_category(p_slug text,p_name text,p_active boolean,p_parent uuid DEFAULT NULL) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid;
BEGIN
 PERFORM internal.require_permission('catalog.manage');
 INSERT INTO app.categories(slug,name,active,parent_id) VALUES(p_slug,p_name,p_active,p_parent)
 ON CONFLICT(slug) DO UPDATE SET name=EXCLUDED.name,active=EXCLUDED.active,parent_id=EXCLUDED.parent_id RETURNING id INTO v;
 PERFORM internal.audit('category.configured','category',v); RETURN v;
END $$;
CREATE FUNCTION api.marketplace(p_limit integer DEFAULT 50,p_offset integer DEFAULT 0) RETURNS SETOF jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
BEGIN
 IF p_limit NOT BETWEEN 1 AND 100 OR p_offset<0 THEN RAISE EXCEPTION 'invalid_pagination'; END IF;
 RETURN QUERY SELECT to_jsonb(p)||jsonb_build_object('store_name',s.name,'professional_id',s.professional_id,
 'images',(SELECT jsonb_agg(i.file_id) FROM app.product_images i JOIN app.files f ON f.id=i.file_id WHERE i.product_id=p.id AND f.moderation_status='approved' AND f.deleted_at IS NULL))
 FROM app.products p JOIN app.stores s ON s.id=p.store_id JOIN app.professionals pro ON pro.user_id=s.professional_id JOIN app.users u ON u.id=pro.user_id
 WHERE p.status='active' AND s.status='active' AND pro.status='active' AND u.status='active' AND internal.has_entitlement(s.professional_id,'marketplace') ORDER BY p.name,p.id LIMIT p_limit OFFSET p_offset;
END $$;
CREATE FUNCTION api.my_products() RETURNS SETOF jsonb LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
 SELECT to_jsonb(p) FROM app.products p JOIN app.stores s ON s.id=p.store_id WHERE s.professional_id=internal.actor() ORDER BY p.created_at DESC LIMIT 200;
$$;
CREATE FUNCTION api.update_product(p_product uuid,p_name text,p_description text,p_price bigint,p_stock integer,p_status text) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
BEGIN
 UPDATE app.products p SET name=p_name,description=p_description,price_minor=p_price,stock=p_stock,status=p_status,updated_at=now()
 FROM app.stores s WHERE p.id=p_product AND s.id=p.store_id AND s.professional_id=internal.actor();
 IF NOT FOUND THEN RAISE EXCEPTION 'product_not_owned' USING ERRCODE='42501'; END IF;
END $$;
CREATE FUNCTION api.add_product_image(p_product uuid,p_file uuid) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor();
BEGIN
 IF NOT EXISTS(SELECT 1 FROM app.products p JOIN app.stores s ON s.id=p.store_id WHERE p.id=p_product AND s.professional_id=v)
 OR NOT EXISTS(SELECT 1 FROM app.files WHERE id=p_file AND owner_id=v AND purpose='product' AND deleted_at IS NULL) THEN RAISE EXCEPTION 'resource_not_owned' USING ERRCODE='42501'; END IF;
 INSERT INTO app.product_images(product_id,file_id) VALUES(p_product,p_file) ON CONFLICT DO NOTHING;
END $$;
CREATE FUNCTION api.add_portfolio(p_title text,p_description text,p_file uuid) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor(); n uuid;
BEGIN
 IF NOT EXISTS(SELECT 1 FROM app.files WHERE id=p_file AND owner_id=v AND purpose='portfolio' AND deleted_at IS NULL) THEN RAISE EXCEPTION 'file_not_owned' USING ERRCODE='42501'; END IF;
 INSERT INTO app.portfolio_items(professional_id,title,description,file_id) VALUES(v,p_title,p_description,p_file) RETURNING id INTO n; RETURN n;
END $$;
CREATE FUNCTION api.add_credential(p_title text,p_issuer text,p_file uuid DEFAULT NULL,p_issued date DEFAULT NULL,p_expires date DEFAULT NULL) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor(); n uuid;
BEGIN
 IF p_file IS NOT NULL AND NOT EXISTS(SELECT 1 FROM app.files WHERE id=p_file AND owner_id=v AND deleted_at IS NULL) THEN RAISE EXCEPTION 'file_not_owned' USING ERRCODE='42501'; END IF;
 INSERT INTO app.professional_credentials(professional_id,title,issuer,evidence_file_id,issued_on,expires_on) VALUES(v,p_title,p_issuer,p_file,p_issued,p_expires) RETURNING id INTO n; RETURN n;
END $$;
CREATE FUNCTION api.set_branch_hours(p_branch uuid,p_weekday smallint,p_opens time,p_closes time) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE n uuid;
BEGIN
 IF NOT EXISTS(SELECT 1 FROM app.branches WHERE id=p_branch AND professional_id=internal.actor()) THEN RAISE EXCEPTION 'branch_not_owned' USING ERRCODE='42501'; END IF;
 INSERT INTO app.branch_hours(branch_id,weekday,opens_at,closes_at) VALUES(p_branch,p_weekday,p_opens,p_closes)
 ON CONFLICT(branch_id,weekday,opens_at) DO UPDATE SET closes_at=EXCLUDED.closes_at RETURNING id INTO n; RETURN n;
END $$;
CREATE FUNCTION api.update_offering(p_offering uuid,p_title text,p_description text,p_price bigint,p_active boolean) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
BEGIN
 UPDATE app.offerings SET title=p_title,description=p_description,price_min_minor=p_price,price_max_minor=NULL,active=p_active WHERE id=p_offering AND professional_id=internal.actor();
 IF NOT FOUND THEN RAISE EXCEPTION 'offering_not_owned' USING ERRCODE='42501'; END IF;
END $$;
CREATE FUNCTION api.set_favorite(p_professional uuid,p_enabled boolean) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
BEGIN
 IF p_enabled THEN INSERT INTO app.favorites(customer_id,professional_id) VALUES(internal.actor(),p_professional) ON CONFLICT DO NOTHING;
 ELSE DELETE FROM app.favorites WHERE customer_id=internal.actor() AND professional_id=p_professional; END IF;
END $$;
CREATE FUNCTION api.my_favorites() RETURNS SETOF jsonb LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
 SELECT api.professional_profile(professional_id) FROM app.favorites WHERE customer_id=internal.actor() ORDER BY created_at DESC;
$$;
CREATE FUNCTION api.respond_review(p_review uuid,p_response text) RETURNS void LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
BEGIN
 IF length(p_response) NOT BETWEEN 1 AND 4000 THEN RAISE EXCEPTION 'invalid_response'; END IF;
 UPDATE app.reviews SET response=p_response,responded_at=now() WHERE id=p_review AND subject_id=internal.actor();
 IF NOT FOUND THEN RAISE EXCEPTION 'review_not_owned' USING ERRCODE='42501'; END IF;
END $$;
CREATE FUNCTION api.professional_assets() RETURNS jsonb LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
 SELECT jsonb_build_object('portfolio',(SELECT jsonb_agg(to_jsonb(t)) FROM app.portfolio_items t WHERE professional_id=internal.actor()),
 'credentials',(SELECT jsonb_agg(to_jsonb(t)-'reference_ciphertext') FROM app.professional_credentials t WHERE professional_id=internal.actor()),
 'hours',(SELECT jsonb_agg(to_jsonb(h)) FROM app.branch_hours h JOIN app.branches b ON b.id=h.branch_id WHERE b.professional_id=internal.actor()));
$$;
CREATE FUNCTION api.send_attachment(p_contract uuid,p_file uuid,p_key uuid) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE c app.contracts:=internal.participant(p_contract); v uuid:=internal.actor(); n uuid;
BEGIN
 IF c.status IN('cancelled','completed') THEN RAISE EXCEPTION 'chat_closed'; END IF;
 IF NOT EXISTS(SELECT 1 FROM app.files WHERE id=p_file AND owner_id=v AND purpose='chat' AND moderation_status='approved' AND deleted_at IS NULL) THEN RAISE EXCEPTION 'file_unavailable'; END IF;
 INSERT INTO app.messages(conversation_id,sender_id,client_message_id,kind,file_id) SELECT id,v,p_key,'attachment',p_file FROM app.conversations WHERE contract_id=c.id
 ON CONFLICT(conversation_id,sender_id,client_message_id) DO NOTHING RETURNING id INTO n;
 IF n IS NULL THEN SELECT m.id INTO n FROM app.messages m JOIN app.conversations cv ON cv.id=m.conversation_id WHERE cv.contract_id=c.id AND m.sender_id=v AND m.client_message_id=p_key; END IF; RETURN n;
END $$;
GRANT EXECUTE ON FUNCTION api.admin_overview(),api.admin_list(text,integer,integer),api.configure_category(text,text,boolean,uuid) TO chamba_admin;
GRANT EXECUTE ON FUNCTION api.marketplace(integer,integer),api.my_products(),api.update_product(uuid,text,text,bigint,integer,text),api.add_product_image(uuid,uuid),
 api.add_portfolio(text,text,uuid),api.add_credential(text,text,uuid,date,date),api.set_branch_hours(uuid,smallint,time,time),api.update_offering(uuid,text,text,bigint,boolean),
 api.set_favorite(uuid,boolean),api.my_favorites(),api.respond_review(uuid,text),api.professional_assets(),api.send_attachment(uuid,uuid,uuid) TO chamba_api;
