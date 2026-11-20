-- El distintivo no afirma garantía mientras una incidencia congela el origen.
CREATE OR REPLACE FUNCTION api.escrow_details(p_contract uuid) RETURNS jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE c app.contracts:=internal.participant(p_contract); result jsonb;
BEGIN
  SELECT to_jsonb(e)||jsonb_build_object('price_minor',q.price_minor,'currency',coalesce(q.currency,c.currency),
    'service_fee_minor',0,'professional_receives_minor',q.price_minor,'requirements',c.work_requirements,
    'funds_guaranteed',e.status='funded' AND NOT EXISTS(SELECT 1 FROM app.money_wallets w WHERE w.user_id=c.customer_id AND w.currency=c.currency AND w.status='frozen'),
    'funding_on_hold',e.status='disputed' OR EXISTS(SELECT 1 FROM app.money_wallets w WHERE w.user_id=c.customer_id AND w.currency=c.currency AND w.status='frozen'),
    'deliveries',(SELECT jsonb_agg(to_jsonb(d)||jsonb_build_object('files',(SELECT jsonb_agg(file_id) FROM app.delivery_files WHERE delivery_id=d.id)) ORDER BY d.version)
      FROM app.service_deliveries d WHERE d.contract_id=c.id)) INTO result
  FROM app.service_escrows e LEFT JOIN app.funding_quotes q ON q.id=e.quote_id WHERE e.contract_id=c.id;
  RETURN result;
END $$;
