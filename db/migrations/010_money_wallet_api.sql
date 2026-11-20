CREATE FUNCTION api.set_payment_preference(p_required boolean) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
BEGIN
  UPDATE app.users SET requires_platform_payment=p_required,updated_at=now() WHERE id=internal.actor();
END $$;

CREATE FUNCTION api.configure_withdrawal_fee(p_currency text,p_basis_points integer,p_reason text) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.require_permission('money.configure'); v_id uuid;
BEGIN
  IF p_currency IS NULL OR p_currency !~ '^[A-Z]{3}$' THEN RAISE EXCEPTION 'invalid_currency'; END IF;
  PERFORM pg_advisory_xact_lock(hashtextextended('withdrawal-policy:'||p_currency,0));
  UPDATE app.withdrawal_fee_policies SET active=false WHERE currency=p_currency AND active;
  INSERT INTO app.withdrawal_fee_policies(currency,basis_points,created_by,reason)
  VALUES(p_currency,p_basis_points,v,p_reason) RETURNING id INTO v_id;
  RETURN v_id;
END $$;

CREATE FUNCTION api.create_topup(p_currency text,p_amount bigint,p_key uuid) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor(); w uuid; old app.money_topups; v_id uuid;
BEGIN
  w:=internal.money_wallet(v,p_currency);
  PERFORM 1 FROM app.money_wallets WHERE id=w AND status='active' FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'wallet_frozen'; END IF;
  SELECT * INTO old FROM app.money_topups WHERE wallet_id=w AND idempotency_key=p_key;
  IF FOUND THEN
    IF old.amount_minor IS DISTINCT FROM p_amount THEN RAISE EXCEPTION 'topup_idempotency_conflict'; END IF;
    RETURN old.id;
  END IF;
  INSERT INTO app.money_topups(wallet_id,amount_minor,idempotency_key) VALUES(w,p_amount,p_key) RETURNING id INTO v_id;
  RETURN v_id;
END $$;

CREATE FUNCTION api.confirm_topup(p_topup uuid,p_provider text,p_event text,p_payment text,p_amount bigint,p_currency text,p_hash text) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE t app.money_topups; w app.money_wallets; e app.money_provider_events; a uuid; cash uuid;
BEGIN
  SELECT * INTO t FROM app.money_topups WHERE id=p_topup FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'topup_not_found'; END IF;
  SELECT * INTO w FROM app.money_wallets WHERE id=t.wallet_id FOR UPDATE;
  SELECT * INTO e FROM app.money_provider_events WHERE provider=p_provider AND event_id=p_event;
  IF FOUND THEN
    IF (e.kind,e.reference_id,e.payment_id,e.amount_minor,e.currency,e.payload_hash)
      IS DISTINCT FROM ('topup_settled'::text,t.id,p_payment,p_amount,p_currency::char(3),p_hash) THEN RAISE EXCEPTION 'provider_event_conflict'; END IF;
    RETURN t.id;
  END IF;
  IF p_amount IS DISTINCT FROM t.amount_minor OR p_currency IS DISTINCT FROM w.currency::text THEN RAISE EXCEPTION 'topup_amount_mismatch'; END IF;
  IF t.status='settled' THEN
    IF NOT EXISTS(SELECT 1 FROM app.provider_payment_registry WHERE provider=p_provider AND payment_id=p_payment
      AND direction='incoming' AND purpose='topup' AND reference_id=t.id) THEN RAISE EXCEPTION 'topup_already_paid'; END IF;
  ELSIF t.status='pending' THEN
    INSERT INTO app.provider_payment_registry(provider,payment_id,direction,purpose,reference_id) VALUES(p_provider,p_payment,'incoming','topup',t.id);
    a:=internal.money_account('available',w.currency,w.id); cash:=internal.money_account('cash_clearing',w.currency);
    PERFORM internal.post_money('topup',w.currency,'topup:'||t.id,t.id,
      jsonb_build_array(jsonb_build_object('account_id',cash,'delta',-t.amount_minor),jsonb_build_object('account_id',a,'delta',t.amount_minor)));
    UPDATE app.money_topups SET status='settled',settled_at=now() WHERE id=t.id;
  ELSE RAISE EXCEPTION 'topup_requires_review'; END IF;
  INSERT INTO app.money_provider_events(provider,event_id,payment_id,kind,reference_id,amount_minor,currency,payload_hash)
  VALUES(p_provider,p_event,p_payment,'topup_settled',t.id,p_amount,p_currency,p_hash);
  RETURN t.id;
END $$;

CREATE FUNCTION api.money_wallet(p_currency text) RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE w uuid; result jsonb;
BEGIN
  w:=internal.money_wallet(internal.actor(),p_currency);
  SELECT jsonb_build_object('id',mw.id,'currency',mw.currency,'status',mw.status,
    'available_minor',coalesce(sum(a.balance_minor) FILTER(WHERE a.kind='available'),0),
    'reserved_for_services_minor',coalesce(sum(a.balance_minor) FILTER(WHERE a.kind='contract_hold'),0),
    'withdrawal_pending_minor',coalesce(sum(a.balance_minor) FILTER(WHERE a.kind='withdrawal_hold'),0))
  INTO result FROM app.money_wallets mw LEFT JOIN app.money_accounts a ON a.wallet_id=mw.id WHERE mw.id=w GROUP BY mw.id;
  RETURN result;
END $$;

CREATE FUNCTION api.money_history(p_currency text,p_limit integer DEFAULT 100,p_before bigint DEFAULT 9223372036854775807) RETURNS SETOF jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor();
BEGIN
  IF p_limit IS NULL OR p_limit NOT BETWEEN 1 AND 200 THEN RAISE EXCEPTION 'invalid_pagination'; END IF;
  RETURN QUERY SELECT jsonb_build_object('entry_id',e.id,'transaction_id',t.id,'kind',t.kind,'account_kind',a.kind,'currency',a.currency,
    'amount_minor',e.delta_minor,'balance_after_minor',e.balance_after_minor,'reference_id',t.reference_id,'created_at',t.created_at)
  FROM app.money_entries e JOIN app.money_accounts a ON a.id=e.account_id JOIN app.money_wallets w ON w.id=a.wallet_id
  JOIN app.money_transactions t ON t.id=e.transaction_id WHERE w.user_id=v AND w.currency=p_currency AND e.id<p_before ORDER BY e.id DESC LIMIT p_limit;
END $$;

CREATE FUNCTION api.add_payout_method(p_provider text,p_token text,p_label text,p_masked text,p_currency text) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v_id uuid;
BEGIN
  INSERT INTO app.payout_methods(user_id,provider,provider_token,label,masked_account,currency)
  VALUES(internal.actor(),p_provider,p_token,p_label,p_masked,p_currency) RETURNING id INTO v_id;
  RETURN v_id;
END $$;

CREATE FUNCTION api.verify_payout_method(p_method uuid,p_owner uuid,p_provider text,p_token text) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
BEGIN
  -- Trabajador de pagos: confirmar titularidad y habilitación con el proveedor.
  UPDATE app.payout_methods SET status='verified',verified_at=now()
  WHERE id=p_method AND user_id=p_owner AND provider=p_provider AND provider_token=p_token AND status IN('pending','verified');
  IF NOT FOUND THEN RAISE EXCEPTION 'payout_method_verification_mismatch'; END IF;
END $$;

CREATE FUNCTION api.my_payout_methods() RETURNS SETOF jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  SELECT to_jsonb(m)-'provider_token' FROM app.payout_methods m WHERE user_id=internal.actor() ORDER BY created_at,id;
$$;

CREATE FUNCTION api.disable_payout_method(p_method uuid) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
BEGIN
  UPDATE app.payout_methods SET status='disabled' WHERE id=p_method AND user_id=internal.actor();
  IF NOT FOUND THEN RAISE EXCEPTION 'payout_method_not_owned'; END IF;
END $$;

CREATE FUNCTION internal.withdrawal_identity(p_user uuid) RETURNS void
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
BEGIN
  IF NOT EXISTS(SELECT 1 FROM app.users u JOIN app.kyc_cases k ON k.user_id=u.id WHERE u.id=p_user AND u.status='active'
    AND k.status='approved' AND k.expires_at>now()) THEN RAISE EXCEPTION 'verified_identity_required_for_withdrawal'; END IF;
END $$;

CREATE FUNCTION api.quote_withdrawal(p_currency text,p_method uuid,p_amount bigint DEFAULT NULL) RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor(); w uuid; f app.withdrawal_fee_policies; amount bigint; fee bigint; result app.withdrawal_quotes;
BEGIN
  PERFORM internal.withdrawal_identity(v);
  w:=internal.money_wallet(v,p_currency);
  PERFORM 1 FROM app.money_wallets WHERE id=w AND status='active' FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'wallet_frozen'; END IF;
  IF NOT EXISTS(SELECT 1 FROM app.payout_methods WHERE id=p_method AND user_id=v AND currency=p_currency AND status='verified') THEN
    RAISE EXCEPTION 'verified_payout_method_required'; END IF;
  SELECT * INTO f FROM app.withdrawal_fee_policies WHERE currency=p_currency AND active;
  IF NOT FOUND THEN RAISE EXCEPTION 'withdrawal_fee_not_configured'; END IF;
  SELECT coalesce(p_amount,coalesce(sum(balance_minor),0)) INTO amount FROM app.money_accounts WHERE wallet_id=w AND kind='available';
  IF amount IS NULL OR amount<=0 OR amount>coalesce((SELECT balance_minor FROM app.money_accounts WHERE wallet_id=w AND kind='available'),0) THEN
    RAISE EXCEPTION 'insufficient_money'; END IF;
  fee:=ceil(amount::numeric*f.basis_points/10000)::bigint;
  IF fee>=amount THEN RAISE EXCEPTION 'withdrawal_amount_too_small'; END IF;
  INSERT INTO app.withdrawal_quotes(wallet_id,payout_method_id,policy_id,amount_minor,basis_points,fee_minor,net_minor,expires_at)
  VALUES(w,p_method,f.id,amount,f.basis_points,fee,amount-fee,now()+interval '15 minutes') RETURNING * INTO result;
  RETURN to_jsonb(result);
END $$;

CREATE FUNCTION api.request_withdrawal(p_quote uuid,p_key uuid) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor(); q app.withdrawal_quotes; w app.money_wallets; m app.payout_methods; old app.money_withdrawals;
  v_id uuid; a uuid; held uuid;
BEGIN
  SELECT * INTO q FROM app.withdrawal_quotes WHERE id=p_quote;
  SELECT * INTO w FROM app.money_wallets WHERE id=q.wallet_id AND user_id=v FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'withdrawal_quote_not_owned' USING ERRCODE='42501'; END IF;
  SELECT * INTO old FROM app.money_withdrawals WHERE wallet_id=w.id AND idempotency_key=p_key;
  IF FOUND THEN
    IF old.quote_id<>p_quote THEN RAISE EXCEPTION 'withdrawal_idempotency_conflict'; END IF;
    RETURN old.id;
  END IF;
  IF w.status<>'active' THEN RAISE EXCEPTION 'wallet_frozen'; END IF;
  IF q.expires_at<=now() THEN RAISE EXCEPTION 'withdrawal_quote_expired'; END IF;
  PERFORM internal.withdrawal_identity(v);
  SELECT * INTO m FROM app.payout_methods WHERE id=q.payout_method_id AND user_id=v AND currency=w.currency AND status='verified' FOR SHARE;
  IF NOT FOUND THEN RAISE EXCEPTION 'verified_payout_method_required'; END IF;
  INSERT INTO app.money_withdrawals(wallet_id,payout_method_id,quote_id,amount_minor,fee_minor,net_minor,destination_provider,destination_token,destination_label,idempotency_key)
  VALUES(w.id,m.id,q.id,q.amount_minor,q.fee_minor,q.net_minor,m.provider,m.provider_token,m.masked_account,p_key) RETURNING id INTO v_id;
  a:=internal.money_account('available',w.currency,w.id); held:=internal.money_account('withdrawal_hold',w.currency,w.id,NULL,v_id);
  PERFORM internal.post_money('withdrawal_reserved',w.currency,'withdrawal:reserve:'||v_id,v_id,
    jsonb_build_array(jsonb_build_object('account_id',a,'delta',-q.amount_minor),jsonb_build_object('account_id',held,'delta',q.amount_minor)));
  INSERT INTO app.outbox(topic,aggregate_id,recipient_id,deduplication_key) VALUES('withdrawal.requested',v_id,v,'withdrawal:'||v_id);
  RETURN v_id;
END $$;

CREATE FUNCTION api.cancel_withdrawal(p_withdrawal uuid) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v uuid:=internal.actor(); wd app.money_withdrawals; w app.money_wallets; a uuid; held uuid;
BEGIN
  SELECT * INTO wd FROM app.money_withdrawals WHERE id=p_withdrawal FOR UPDATE;
  SELECT * INTO w FROM app.money_wallets WHERE id=wd.wallet_id AND user_id=v FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'withdrawal_not_owned' USING ERRCODE='42501'; END IF;
  IF wd.status='cancelled' THEN RETURN; END IF;
  IF wd.status<>'pending' THEN RAISE EXCEPTION 'withdrawal_already_processing'; END IF;
  a:=internal.money_account('available',w.currency,w.id); held:=internal.money_account('withdrawal_hold',w.currency,w.id,NULL,wd.id);
  PERFORM internal.post_money('withdrawal_cancelled',w.currency,'withdrawal:cancel:'||wd.id,wd.id,
    jsonb_build_array(jsonb_build_object('account_id',held,'delta',-wd.amount_minor),jsonb_build_object('account_id',a,'delta',wd.amount_minor)));
  UPDATE app.money_withdrawals SET status='cancelled',completed_at=now() WHERE id=wd.id;
END $$;

CREATE FUNCTION api.prepare_withdrawal(p_withdrawal uuid) RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE wd app.money_withdrawals; w app.money_wallets;
BEGIN
  SELECT * INTO wd FROM app.money_withdrawals WHERE id=p_withdrawal FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'withdrawal_not_found'; END IF;
  SELECT * INTO w FROM app.money_wallets WHERE id=wd.wallet_id FOR UPDATE;
  IF wd.status='pending' THEN
    IF w.status<>'active' THEN RAISE EXCEPTION 'wallet_frozen'; END IF;
    PERFORM internal.withdrawal_identity(w.user_id);
    IF NOT EXISTS(SELECT 1 FROM app.payout_methods WHERE id=wd.payout_method_id AND status='verified') THEN RAISE EXCEPTION 'payout_method_disabled'; END IF;
    UPDATE app.money_withdrawals SET status='processing',dispatched_at=now() WHERE id=wd.id RETURNING * INTO wd;
  ELSIF wd.status<>'processing' THEN RAISE EXCEPTION 'withdrawal_not_dispatchable'; END IF;
  RETURN jsonb_build_object('id',wd.id,'provider',wd.destination_provider,'destination_token',wd.destination_token,
    'currency',w.currency,'net_minor',wd.net_minor,'gross_minor',wd.amount_minor,'fee_minor',wd.fee_minor,
    'provider_idempotency_key','withdrawal:'||wd.id,'status',wd.status);
END $$;

CREATE FUNCTION api.confirm_withdrawal(p_withdrawal uuid,p_provider text,p_event text,p_payment text,p_result text,p_net bigint,p_currency text,p_hash text) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE wd app.money_withdrawals; w app.money_wallets; e app.money_provider_events; held uuid; other uuid; fees uuid; entries jsonb; kind text;
BEGIN
  IF p_result IS NULL OR p_result NOT IN('paid','definitively_failed') THEN RAISE EXCEPTION 'definitive_provider_result_required'; END IF;
  SELECT * INTO wd FROM app.money_withdrawals WHERE id=p_withdrawal FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'withdrawal_not_found'; END IF;
  SELECT * INTO w FROM app.money_wallets WHERE id=wd.wallet_id FOR UPDATE;
  kind:=CASE WHEN p_result='paid' THEN 'withdrawal_paid' ELSE 'withdrawal_failed' END;
  SELECT * INTO e FROM app.money_provider_events WHERE provider=p_provider AND event_id=p_event;
  IF FOUND THEN
    IF (e.kind,e.reference_id,e.payment_id,e.amount_minor,e.currency,e.payload_hash)
      IS DISTINCT FROM (kind,wd.id,p_payment,p_net,p_currency::char(3),p_hash) THEN RAISE EXCEPTION 'provider_event_conflict'; END IF;
    RETURN;
  END IF;
  IF p_provider IS DISTINCT FROM wd.destination_provider OR p_net IS DISTINCT FROM wd.net_minor OR p_currency IS DISTINCT FROM w.currency::text THEN
    RAISE EXCEPTION 'withdrawal_confirmation_mismatch'; END IF;
  IF wd.status='processing' THEN
    INSERT INTO app.provider_payment_registry(provider,payment_id,direction,purpose,reference_id) VALUES(p_provider,p_payment,'outgoing','withdrawal',wd.id);
    held:=internal.money_account('withdrawal_hold',w.currency,w.id,NULL,wd.id);
    IF p_result='paid' THEN
      other:=internal.money_account('cash_clearing',w.currency);
      entries:=jsonb_build_array(jsonb_build_object('account_id',held,'delta',-wd.amount_minor),jsonb_build_object('account_id',other,'delta',wd.net_minor));
      IF wd.fee_minor>0 THEN
        fees:=internal.money_account('platform_fees',w.currency);
        entries:=entries||jsonb_build_array(jsonb_build_object('account_id',fees,'delta',wd.fee_minor));
      END IF;
    ELSE
      other:=internal.money_account('available',w.currency,w.id);
      entries:=jsonb_build_array(jsonb_build_object('account_id',held,'delta',-wd.amount_minor),jsonb_build_object('account_id',other,'delta',wd.amount_minor));
    END IF;
    PERFORM internal.post_money(kind,w.currency,'withdrawal:result:'||wd.id,wd.id,entries);
    UPDATE app.money_withdrawals SET status=CASE WHEN p_result='paid' THEN 'paid' ELSE 'failed' END,completed_at=now() WHERE id=wd.id;
  ELSIF NOT ((wd.status='paid' AND p_result='paid') OR (wd.status='failed' AND p_result='definitively_failed'))
    OR NOT EXISTS(SELECT 1 FROM app.provider_payment_registry WHERE provider=p_provider AND payment_id=p_payment AND direction='outgoing' AND reference_id=wd.id) THEN
    RAISE EXCEPTION 'withdrawal_result_conflict_requires_reconciliation';
  END IF;
  INSERT INTO app.money_provider_events(provider,event_id,payment_id,kind,reference_id,amount_minor,currency,payload_hash)
  VALUES(p_provider,p_event,p_payment,kind,wd.id,p_net,p_currency,p_hash);
END $$;

CREATE FUNCTION api.my_withdrawals() RETURNS SETOF jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
  SELECT to_jsonb(wd)-'destination_token' FROM app.money_withdrawals wd JOIN app.money_wallets w ON w.id=wd.wallet_id
  WHERE w.user_id=internal.actor() ORDER BY wd.created_at DESC LIMIT 100;
$$;

GRANT EXECUTE ON FUNCTION api.set_payment_preference(boolean),api.create_topup(text,bigint,uuid),api.money_wallet(text),
  api.money_history(text,integer,bigint),api.add_payout_method(text,text,text,text,text),api.my_payout_methods(),api.disable_payout_method(uuid),
  api.quote_withdrawal(text,uuid,bigint),api.request_withdrawal(uuid,uuid),api.cancel_withdrawal(uuid),api.my_withdrawals() TO chamba_api;
GRANT EXECUTE ON FUNCTION api.configure_withdrawal_fee(text,integer,text) TO chamba_admin;
GRANT EXECUTE ON FUNCTION api.confirm_topup(uuid,text,text,text,bigint,text,text),api.verify_payout_method(uuid,uuid,text,text),
  api.prepare_withdrawal(uuid),api.confirm_withdrawal(uuid,text,text,text,text,bigint,text,text) TO chamba_billing;
