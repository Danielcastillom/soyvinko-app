CREATE INDEX sessions_family_idx ON app.sessions(family_id);
CREATE FUNCTION api.admin_file_key(p_file uuid,p_reason text) RETURNS text
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE f app.files;
BEGIN
 SELECT * INTO f FROM app.files WHERE id=p_file AND deleted_at IS NULL;
 IF NOT FOUND THEN RAISE EXCEPTION 'file_unavailable'; END IF;
 PERFORM internal.require_permission(CASE WHEN f.purpose='kyc' THEN 'kyc.review' ELSE 'content.moderate' END);
 IF length(trim(coalesce(p_reason,'')))<10 THEN RAISE EXCEPTION 'access_reason_required'; END IF;
 PERFORM internal.audit('file.admin_read','file',f.id,jsonb_build_object('reason',p_reason));
 RETURN f.object_key;
END $$;
GRANT EXECUTE ON FUNCTION api.admin_file_key(uuid,text) TO chamba_admin;
CREATE OR REPLACE FUNCTION api.update_product(p_product uuid,p_name text,p_description text,p_price bigint,p_stock integer,p_status text) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor(); old_stock integer;
BEGIN
 SELECT p.stock INTO old_stock FROM app.products p JOIN app.stores s ON s.id=p.store_id
 WHERE p.id=p_product AND s.professional_id=v FOR UPDATE OF p;
 IF NOT FOUND THEN RAISE EXCEPTION 'product_not_owned' USING ERRCODE='42501'; END IF;
 UPDATE app.products SET name=p_name,description=p_description,price_minor=p_price,stock=p_stock,status=p_status,updated_at=now() WHERE id=p_product;
 IF old_stock<>p_stock THEN INSERT INTO app.inventory_movements(product_id,delta,reason,actor_id) VALUES(p_product,p_stock-old_stock,'stock_adjustment',v); END IF;
 PERFORM internal.audit('product.updated','product',p_product);
END $$;
CREATE FUNCTION api.external_payment_reports(p_contract uuid DEFAULT NULL,p_order uuid DEFAULT NULL) RETURNS SETOF jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor();
BEGIN
 IF num_nonnulls(p_contract,p_order)<>1 THEN RAISE EXCEPTION 'one_transaction_required'; END IF;
 IF p_contract IS NOT NULL THEN PERFORM internal.participant(p_contract);
 ELSE IF NOT EXISTS(SELECT 1 FROM app.orders o JOIN app.stores s ON s.id=o.store_id WHERE o.id=p_order AND v IN(o.customer_id,s.professional_id)) THEN RAISE EXCEPTION 'order_access_denied' USING ERRCODE='42501'; END IF; END IF;
 RETURN QUERY SELECT to_jsonb(r) FROM app.external_payment_reports r WHERE contract_id=p_contract OR order_id=p_order ORDER BY created_at DESC LIMIT 100;
END $$;
GRANT EXECUTE ON FUNCTION api.external_payment_reports(uuid,uuid) TO chamba_api;
CREATE FUNCTION api.worker_housekeeping() RETURNS void LANGUAGE sql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
 DELETE FROM app.http_rate_limits WHERE expires_at<now()-interval '1 day';
 DELETE FROM app.auth_challenges WHERE expires_at<now()-interval '30 days';
$$;
GRANT EXECUTE ON FUNCTION api.worker_housekeeping() TO chamba_worker;
