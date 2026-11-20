CREATE OR REPLACE FUNCTION internal.check_money_transaction() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v_id uuid; v_currency char(3); v_sum numeric; v_count integer;
BEGIN
  -- Los registros NEW tienen tipos diferentes en ambas tablas.
  IF TG_TABLE_NAME='money_transactions' THEN v_id:=NEW.id; ELSE v_id:=NEW.transaction_id; END IF;
  SELECT currency INTO v_currency FROM app.money_transactions WHERE id=v_id;
  SELECT sum(e.delta_minor),count(*) INTO v_sum,v_count FROM app.money_entries e JOIN app.money_accounts a ON a.id=e.account_id
    WHERE e.transaction_id=v_id AND a.currency=v_currency;
  IF v_count<2 OR v_sum<>0 OR v_count<>(SELECT count(*) FROM app.money_entries WHERE transaction_id=v_id) THEN
    RAISE EXCEPTION 'unbalanced_money_transaction';
  END IF;
  RETURN NULL;
END $$;
