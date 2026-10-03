CREATE OR REPLACE FUNCTION api.external_payment_reports(p_contract uuid DEFAULT NULL,p_order uuid DEFAULT NULL) RETURNS SETOF jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor();
BEGIN
 IF num_nonnulls(p_contract,p_order)<>1 THEN RAISE EXCEPTION 'one_transaction_required'; END IF;
 IF p_contract IS NOT NULL THEN PERFORM internal.participant(p_contract);
 ELSE IF NOT EXISTS(SELECT 1 FROM app.orders o JOIN app.stores s ON s.id=o.store_id WHERE o.id=p_order AND v IN(o.customer_id,s.professional_id)) THEN RAISE EXCEPTION 'order_access_denied' USING ERRCODE='42501'; END IF; END IF;
 RETURN QUERY SELECT to_jsonb(r)-'reference_ciphertext' FROM app.external_payment_reports r WHERE contract_id=p_contract OR order_id=p_order ORDER BY reported_at DESC LIMIT 100;
END $$;
