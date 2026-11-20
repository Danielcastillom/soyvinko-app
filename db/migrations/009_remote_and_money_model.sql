-- Los créditos comerciales siguen separados del dinero real.
ALTER TABLE app.users ADD COLUMN requires_platform_payment boolean NOT NULL DEFAULT false;
ALTER TABLE app.offerings ADD COLUMN delivery_mode text NOT NULL DEFAULT 'onsite'
  CHECK(delivery_mode IN('onsite','remote'));
ALTER TABLE app.service_requests ADD COLUMN delivery_mode text NOT NULL DEFAULT 'onsite'
  CHECK(delivery_mode IN('onsite','remote'));
ALTER TABLE app.service_requests ADD COLUMN requires_platform_payment boolean NOT NULL DEFAULT false;
ALTER TABLE app.service_requests ADD COLUMN offered_price_minor bigint CHECK(offered_price_minor>0);
ALTER TABLE app.service_requests ALTER COLUMN search_radius_m DROP NOT NULL;
ALTER TABLE app.service_requests ADD CONSTRAINT remote_requires_platform
  CHECK(delivery_mode<>'remote' OR (requires_platform_payment AND search_radius_m IS NULL));
ALTER TABLE app.contracts ADD COLUMN delivery_mode text NOT NULL DEFAULT 'onsite' CHECK(delivery_mode IN('onsite','remote'));
ALTER TABLE app.contracts ADD COLUMN payment_protection text NOT NULL DEFAULT 'external' CHECK(payment_protection IN('external','guaranteed'));
-- La comisión del contrato permanece en cero. Solo hay comisión al retirar dinero.
ALTER TABLE app.contracts ADD CONSTRAINT remote_contract_guaranteed CHECK(delivery_mode<>'remote' OR payment_protection='guaranteed');

CREATE TABLE app.money_wallets (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), user_id uuid NOT NULL REFERENCES app.users,
  currency char(3) NOT NULL CHECK(currency ~ '^[A-Z]{3}$'),
  status text NOT NULL DEFAULT 'active' CHECK(status IN('active','frozen')),
  created_at timestamptz NOT NULL DEFAULT now(), UNIQUE(user_id,currency), UNIQUE(id,currency)
);
CREATE TABLE app.withdrawal_fee_policies (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), currency char(3) NOT NULL CHECK(currency ~ '^[A-Z]{3}$'),
  basis_points integer NOT NULL CHECK(basis_points BETWEEN 0 AND 9999),
  active boolean NOT NULL DEFAULT true, created_by uuid NOT NULL REFERENCES app.users,
  reason text NOT NULL CHECK(length(reason)>=5), created_at timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX withdrawal_fee_active_idx ON app.withdrawal_fee_policies(currency) WHERE active;
CREATE TABLE app.money_topups (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), wallet_id uuid NOT NULL REFERENCES app.money_wallets,
  amount_minor bigint NOT NULL CHECK(amount_minor BETWEEN 1 AND 1000000000000),
  fee_minor bigint NOT NULL DEFAULT 0 CHECK(fee_minor=0),
  status text NOT NULL DEFAULT 'pending' CHECK(status IN('pending','settled','review')),
  idempotency_key uuid NOT NULL, created_at timestamptz NOT NULL DEFAULT now(), settled_at timestamptz,
  UNIQUE(wallet_id,idempotency_key)
);
CREATE TABLE app.payout_methods (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), user_id uuid NOT NULL REFERENCES app.users,
  provider text NOT NULL, provider_token text NOT NULL, label text NOT NULL, masked_account text NOT NULL,
  currency char(3) NOT NULL CHECK(currency ~ '^[A-Z]{3}$'),
  status text NOT NULL DEFAULT 'pending' CHECK(status IN('pending','verified','disabled')),
  verified_at timestamptz, created_at timestamptz NOT NULL DEFAULT now(), UNIQUE(provider,provider_token),
  CHECK(status<>'verified' OR verified_at IS NOT NULL)
);
CREATE TABLE app.withdrawal_quotes (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), wallet_id uuid NOT NULL REFERENCES app.money_wallets,
  payout_method_id uuid NOT NULL REFERENCES app.payout_methods, policy_id uuid NOT NULL REFERENCES app.withdrawal_fee_policies,
  amount_minor bigint NOT NULL CHECK(amount_minor BETWEEN 1 AND 1000000000000),
  basis_points integer NOT NULL CHECK(basis_points BETWEEN 0 AND 9999),
  fee_minor bigint NOT NULL CHECK(fee_minor>=0), net_minor bigint NOT NULL CHECK(net_minor>0),
  created_at timestamptz NOT NULL DEFAULT now(), expires_at timestamptz NOT NULL,
  CHECK(amount_minor=fee_minor+net_minor)
);
CREATE TABLE app.money_withdrawals (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), wallet_id uuid NOT NULL REFERENCES app.money_wallets,
  payout_method_id uuid NOT NULL REFERENCES app.payout_methods,
  quote_id uuid NOT NULL UNIQUE REFERENCES app.withdrawal_quotes,
  amount_minor bigint NOT NULL CHECK(amount_minor BETWEEN 1 AND 1000000000000),
  fee_minor bigint NOT NULL CHECK(fee_minor>=0), net_minor bigint NOT NULL CHECK(net_minor>0),
  destination_provider text NOT NULL, destination_token text NOT NULL, destination_label text NOT NULL,
  status text NOT NULL DEFAULT 'pending' CHECK(status IN('pending','processing','paid','failed','cancelled','review')),
  idempotency_key uuid NOT NULL, created_at timestamptz NOT NULL DEFAULT now(), dispatched_at timestamptz, completed_at timestamptz,
  UNIQUE(wallet_id,idempotency_key), CHECK(amount_minor=fee_minor+net_minor)
);
CREATE TABLE app.funding_quotes (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), contract_id uuid NOT NULL REFERENCES app.contracts,
  customer_id uuid NOT NULL REFERENCES app.users,
  terms_version integer NOT NULL, price_minor bigint NOT NULL CHECK(price_minor BETWEEN 1 AND 1000000000000),
  currency char(3) NOT NULL, fee_minor bigint NOT NULL DEFAULT 0 CHECK(fee_minor=0),
  created_at timestamptz NOT NULL DEFAULT now(), expires_at timestamptz NOT NULL
);
CREATE TABLE app.service_escrows (
  contract_id uuid PRIMARY KEY REFERENCES app.contracts,
  status text NOT NULL DEFAULT 'unfunded' CHECK(status IN('unfunded','funded','disputed','released','refunded')),
  quote_id uuid UNIQUE REFERENCES app.funding_quotes, funded_at timestamptz, released_at timestamptz, refunded_at timestamptz,
  refund_requested_at timestamptz, refund_reason text, release_key uuid,
  CHECK(status='unfunded' OR (quote_id IS NOT NULL AND funded_at IS NOT NULL))
);
CREATE TABLE app.service_deliveries (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), contract_id uuid NOT NULL REFERENCES app.contracts,
  professional_id uuid NOT NULL REFERENCES app.users, version integer NOT NULL CHECK(version>0),
  summary text NOT NULL CHECK(length(summary) BETWEEN 10 AND 10000),
  requirements_snapshot text NOT NULL, terms_version integer NOT NULL,
  status text NOT NULL DEFAULT 'submitted' CHECK(status IN('submitted','changes_requested','accepted')),
  client_message_id uuid NOT NULL, created_at timestamptz NOT NULL DEFAULT now(), reviewed_at timestamptz, review_note text,
  UNIQUE(contract_id,version), UNIQUE(contract_id,client_message_id)
);
CREATE TABLE app.delivery_files (
  delivery_id uuid REFERENCES app.service_deliveries, file_id uuid REFERENCES app.files,
  PRIMARY KEY(delivery_id,file_id)
);
CREATE TABLE app.money_accounts (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), currency char(3) NOT NULL CHECK(currency ~ '^[A-Z]{3}$'),
  kind text NOT NULL CHECK(kind IN('available','contract_hold','withdrawal_hold','cash_clearing','platform_fees')),
  wallet_id uuid, contract_id uuid REFERENCES app.contracts, withdrawal_id uuid REFERENCES app.money_withdrawals,
  balance_minor bigint NOT NULL DEFAULT 0,
  FOREIGN KEY(wallet_id,currency) REFERENCES app.money_wallets(id,currency),
  CHECK(kind='cash_clearing' OR balance_minor>=0),
  CHECK((kind='available' AND wallet_id IS NOT NULL AND contract_id IS NULL AND withdrawal_id IS NULL)
    OR (kind='contract_hold' AND wallet_id IS NOT NULL AND contract_id IS NOT NULL AND withdrawal_id IS NULL)
    OR (kind='withdrawal_hold' AND wallet_id IS NOT NULL AND contract_id IS NULL AND withdrawal_id IS NOT NULL)
    OR (kind IN('cash_clearing','platform_fees') AND num_nonnulls(wallet_id,contract_id,withdrawal_id)=0)),
  UNIQUE NULLS NOT DISTINCT(kind,currency,wallet_id,contract_id,withdrawal_id)
);
CREATE INDEX money_accounts_wallet_idx ON app.money_accounts(wallet_id,kind);
CREATE TABLE app.money_transactions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), kind text NOT NULL,
  currency char(3) NOT NULL, idempotency_key text NOT NULL UNIQUE, reference_id uuid NOT NULL,
  entries_fingerprint jsonb NOT NULL, actor_id uuid REFERENCES app.users, metadata jsonb NOT NULL DEFAULT '{}',
  created_at timestamptz NOT NULL DEFAULT now(), UNIQUE(id,currency)
);
CREATE TABLE app.money_entries (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  transaction_id uuid NOT NULL REFERENCES app.money_transactions, account_id uuid NOT NULL REFERENCES app.money_accounts,
  delta_minor bigint NOT NULL CHECK(delta_minor<>0), balance_after_minor bigint NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(), UNIQUE(transaction_id,account_id)
);
CREATE INDEX money_entries_account_idx ON app.money_entries(account_id,id DESC);
CREATE TABLE app.provider_payment_registry (
  provider text NOT NULL, payment_id text NOT NULL, direction text NOT NULL CHECK(direction IN('incoming','outgoing')),
  purpose text NOT NULL CHECK(purpose IN('credits','topup','withdrawal')), reference_id uuid NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(), PRIMARY KEY(provider,payment_id,direction)
);
CREATE TABLE app.money_provider_events (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), provider text NOT NULL, event_id text NOT NULL, payment_id text NOT NULL,
  kind text NOT NULL CHECK(kind IN('topup_settled','withdrawal_paid','withdrawal_failed')),
  reference_id uuid NOT NULL, amount_minor bigint NOT NULL CHECK(amount_minor>0), currency char(3) NOT NULL,
  payload_hash text NOT NULL CHECK(payload_hash ~ '^[a-f0-9]{64}$'), created_at timestamptz NOT NULL DEFAULT now(), UNIQUE(provider,event_id)
);
CREATE TABLE app.money_incidents (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), wallet_id uuid NOT NULL REFERENCES app.money_wallets,
  provider text NOT NULL, event_id text NOT NULL, reason text NOT NULL,
  status text NOT NULL DEFAULT 'open' CHECK(status IN('open','resolved')), created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE(provider,event_id)
);

-- No hay una tasa comercial implícita: administración debe publicar una política.
INSERT INTO app.permissions(code,description) VALUES('money.configure','Configurar comisión exclusiva de retiros'),('money.refund','Devolver fondos retenidos con expediente de soporte');
INSERT INTO app.role_permissions(role_code,permission_code)
SELECT r,p FROM unnest(ARRAY['admin','superadmin']) r CROSS JOIN unnest(ARRAY['money.configure','money.refund']) p;
INSERT INTO app.categories(slug,name) VALUES('diseno-grafico','Diseño gráfico'),('programacion','Desarrollo de software'),('traduccion','Traducción') ON CONFLICT(slug) DO NOTHING;

DO $$ DECLARE t text; BEGIN
  FOREACH t IN ARRAY ARRAY['money_wallets','withdrawal_fee_policies','money_topups','payout_methods','withdrawal_quotes','money_withdrawals',
    'funding_quotes','service_escrows','service_deliveries','delivery_files','money_accounts','money_transactions','money_entries',
    'provider_payment_registry','money_provider_events','money_incidents'] LOOP
    EXECUTE format('ALTER TABLE app.%I ENABLE ROW LEVEL SECURITY',t);
  END LOOP;
  FOREACH t IN ARRAY ARRAY['money_transactions','money_entries','provider_payment_registry','money_provider_events','funding_quotes','withdrawal_quotes'] LOOP
    EXECUTE format('CREATE TRIGGER immutable BEFORE UPDATE OR DELETE ON app.%I FOR EACH ROW EXECUTE FUNCTION internal.reject_mutation()',t);
  END LOOP;
  FOREACH t IN ARRAY ARRAY['money_wallets','withdrawal_fee_policies','money_topups','payout_methods','money_withdrawals','service_escrows','service_deliveries','money_incidents'] LOOP
    EXECUTE format('CREATE TRIGGER audit_change AFTER INSERT OR UPDATE OR DELETE ON app.%I FOR EACH ROW EXECUTE FUNCTION internal.audit_row_change()',t);
  END LOOP;
END $$;

CREATE FUNCTION internal.check_money_transaction() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v_id uuid; v_currency char(3); v_sum numeric; v_count integer;
BEGIN
  v_id:=CASE WHEN TG_TABLE_NAME='money_transactions' THEN NEW.id ELSE NEW.transaction_id END;
  SELECT currency INTO v_currency FROM app.money_transactions WHERE id=v_id;
  SELECT sum(e.delta_minor),count(*) INTO v_sum,v_count FROM app.money_entries e JOIN app.money_accounts a ON a.id=e.account_id
    WHERE e.transaction_id=v_id AND a.currency=v_currency;
  IF v_count<2 OR v_sum<>0 OR v_count<>(SELECT count(*) FROM app.money_entries WHERE transaction_id=v_id) THEN
    RAISE EXCEPTION 'unbalanced_money_transaction';
  END IF;
  RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER balanced_transaction AFTER INSERT ON app.money_transactions DEFERRABLE INITIALLY DEFERRED
  FOR EACH ROW EXECUTE FUNCTION internal.check_money_transaction();
CREATE CONSTRAINT TRIGGER balanced_entries AFTER INSERT ON app.money_entries DEFERRABLE INITIALLY DEFERRED
  FOR EACH ROW EXECUTE FUNCTION internal.check_money_transaction();

CREATE FUNCTION internal.money_wallet(p_user uuid,p_currency text) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v_id uuid;
BEGIN
  IF p_currency IS NULL OR p_currency !~ '^[A-Z]{3}$' THEN RAISE EXCEPTION 'invalid_currency'; END IF;
  INSERT INTO app.money_wallets(user_id,currency) VALUES(p_user,p_currency) ON CONFLICT(user_id,currency) DO NOTHING;
  SELECT id INTO v_id FROM app.money_wallets WHERE user_id=p_user AND currency=p_currency;
  RETURN v_id;
END $$;

CREATE FUNCTION internal.money_account(p_kind text,p_currency text,p_wallet uuid DEFAULT NULL,p_contract uuid DEFAULT NULL,p_withdrawal uuid DEFAULT NULL) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE v_id uuid;
BEGIN
  INSERT INTO app.money_accounts(kind,currency,wallet_id,contract_id,withdrawal_id)
  VALUES(p_kind,p_currency,p_wallet,p_contract,p_withdrawal) ON CONFLICT DO NOTHING;
  SELECT id INTO v_id FROM app.money_accounts WHERE (kind,currency,wallet_id,contract_id,withdrawal_id)
    IS NOT DISTINCT FROM (p_kind,p_currency::char(3),p_wallet,p_contract,p_withdrawal);
  RETURN v_id;
END $$;

CREATE FUNCTION internal.post_money(p_kind text,p_currency text,p_key text,p_reference uuid,p_entries jsonb,p_metadata jsonb DEFAULT '{}') RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
DECLARE tx app.money_transactions; item record; acc app.money_accounts; v_canonical jsonb; v_id uuid; n integer;
BEGIN
  IF p_entries IS NULL OR jsonb_typeof(p_entries)<>'array' OR jsonb_array_length(p_entries)<2 THEN RAISE EXCEPTION 'invalid_money_entries'; END IF;
  SELECT jsonb_agg(jsonb_build_object('account_id',account_id,'delta',delta) ORDER BY account_id),count(*)
    INTO v_canonical,n FROM jsonb_to_recordset(p_entries) AS x(account_id uuid,delta bigint);
  IF EXISTS(SELECT 1 FROM jsonb_to_recordset(p_entries) AS x(account_id uuid,delta bigint) WHERE account_id IS NULL OR delta IS NULL OR delta=0)
    OR (SELECT count(DISTINCT account_id) FROM jsonb_to_recordset(p_entries) AS x(account_id uuid,delta bigint))<>n
    OR (SELECT sum(delta::numeric) FROM jsonb_to_recordset(p_entries) AS x(account_id uuid,delta bigint))<>0 THEN RAISE EXCEPTION 'unbalanced_money_entries'; END IF;
  -- Serializa reintentos antes de tocar cuentas, incluso si llegan con otro contenido.
  PERFORM pg_advisory_xact_lock(hashtextextended('money:'||p_key,0));
  SELECT * INTO tx FROM app.money_transactions WHERE idempotency_key=p_key;
  IF FOUND THEN
    IF (tx.kind,tx.currency,tx.reference_id,tx.entries_fingerprint) IS DISTINCT FROM (p_kind,p_currency::char(3),p_reference,v_canonical) THEN
      RAISE EXCEPTION 'money_idempotency_conflict'; END IF;
    RETURN tx.id;
  END IF;
  FOR item IN SELECT * FROM jsonb_to_recordset(v_canonical) AS x(account_id uuid,delta bigint) ORDER BY account_id LOOP
    SELECT * INTO acc FROM app.money_accounts WHERE id=item.account_id FOR UPDATE;
    IF NOT FOUND OR acc.currency::text<>p_currency THEN RAISE EXCEPTION 'money_account_currency_mismatch'; END IF;
    IF acc.kind<>'cash_clearing' AND acc.balance_minor+item.delta<0 THEN RAISE EXCEPTION 'insufficient_money'; END IF;
  END LOOP;
  INSERT INTO app.money_transactions(kind,currency,idempotency_key,reference_id,entries_fingerprint,actor_id,metadata)
  VALUES(p_kind,p_currency,p_key,p_reference,v_canonical,nullif(current_setting('app.user_id',true),'')::uuid,p_metadata) RETURNING id INTO v_id;
  FOR item IN SELECT * FROM jsonb_to_recordset(v_canonical) AS x(account_id uuid,delta bigint) ORDER BY account_id LOOP
    UPDATE app.money_accounts SET balance_minor=balance_minor+item.delta WHERE id=item.account_id RETURNING * INTO acc;
    INSERT INTO app.money_entries(transaction_id,account_id,delta_minor,balance_after_minor) VALUES(v_id,acc.id,item.delta,acc.balance_minor);
  END LOOP;
  RETURN v_id;
END $$;

CREATE FUNCTION internal.register_credit_payment() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public,pg_temp AS $$
BEGIN
  IF NEW.event_type='captured' THEN
    INSERT INTO app.provider_payment_registry(provider,payment_id,direction,purpose,reference_id)
    VALUES(NEW.provider,NEW.external_payment_id,'incoming','credits',NEW.purchase_id);
  END IF;
  RETURN NEW;
END $$;
INSERT INTO app.provider_payment_registry(provider,payment_id,direction,purpose,reference_id)
SELECT provider,external_payment_id,'incoming','credits',purchase_id FROM app.payment_events WHERE event_type='captured';
CREATE TRIGGER register_capture BEFORE INSERT ON app.payment_events FOR EACH ROW EXECUTE FUNCTION internal.register_credit_payment();
