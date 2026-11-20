-- Unidades: dinero en unidades menores enteras; distancias en metros; fechas UTC.
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS citext;
CREATE SCHEMA app;
CREATE SCHEMA api;
CREATE SCHEMA internal;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA app, api, internal FROM PUBLIC;
ALTER DEFAULT PRIVILEGES IN SCHEMA app, api, internal REVOKE EXECUTE ON FUNCTIONS FROM PUBLIC;
DO $$ BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname='chamba_api') THEN CREATE ROLE chamba_api NOLOGIN; END IF;
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname='chamba_auth') THEN CREATE ROLE chamba_auth NOLOGIN; END IF;
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname='chamba_billing') THEN CREATE ROLE chamba_billing NOLOGIN; END IF;
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname='chamba_admin') THEN CREATE ROLE chamba_admin NOLOGIN; END IF;
END $$;

CREATE TABLE app.users (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  email public.citext NOT NULL UNIQUE,
  display_name text NOT NULL CHECK (length(display_name) BETWEEN 2 AND 120),
  status text NOT NULL DEFAULT 'active' CHECK (status IN ('active','suspended','closed')),
  locale text NOT NULL DEFAULT 'es', timezone text NOT NULL DEFAULT 'UTC',
  email_verified_at timestamptz, created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(), closed_at timestamptz,
  CHECK (position('@' IN email::text)>1)
);
CREATE TABLE app.credentials (
  user_id uuid PRIMARY KEY REFERENCES app.users,
  password_hash text NOT NULL CHECK (password_hash LIKE '$argon2id$%'),
  password_changed_at timestamptz NOT NULL DEFAULT now(),
  failed_attempts integer NOT NULL DEFAULT 0 CHECK(failed_attempts>=0), locked_until timestamptz,
  mfa_secret_ciphertext bytea, mfa_enabled_at timestamptz
);
CREATE TABLE app.auth_identities (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), user_id uuid NOT NULL REFERENCES app.users,
  provider text NOT NULL, subject text NOT NULL, created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE(provider,subject)
);
CREATE TABLE app.sessions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), user_id uuid NOT NULL REFERENCES app.users,
  refresh_token_hash text NOT NULL UNIQUE, device_label text, expires_at timestamptz NOT NULL,
  revoked_at timestamptz, created_at timestamptz NOT NULL DEFAULT now(),
  CHECK(expires_at>created_at)
);
CREATE INDEX sessions_user_idx ON app.sessions(user_id) WHERE revoked_at IS NULL;
CREATE TABLE app.auth_challenges (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), user_id uuid NOT NULL REFERENCES app.users,
  purpose text NOT NULL CHECK(purpose IN ('email_verification','phone_verification','password_reset','mfa')),
  token_hash text NOT NULL UNIQUE, expires_at timestamptz NOT NULL, consumed_at timestamptz,
  attempts integer NOT NULL DEFAULT 0 CHECK(attempts>=0), created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE app.roles (code text PRIMARY KEY, description text NOT NULL);
CREATE TABLE app.permissions (code text PRIMARY KEY, description text NOT NULL);
CREATE TABLE app.role_permissions (
  role_code text REFERENCES app.roles, permission_code text REFERENCES app.permissions,
  PRIMARY KEY(role_code,permission_code)
);
CREATE TABLE app.user_roles (
  user_id uuid REFERENCES app.users, role_code text REFERENCES app.roles,
  granted_by uuid REFERENCES app.users, granted_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY(user_id,role_code)
);
CREATE TABLE app.user_contacts (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), user_id uuid NOT NULL REFERENCES app.users,
  kind text NOT NULL CHECK(kind IN ('phone','whatsapp','email')),
  label text NOT NULL, value text NOT NULL CHECK(length(value) BETWEEN 3 AND 254),
  is_primary boolean NOT NULL DEFAULT false, verified_at timestamptz,
  share_on_contract boolean NOT NULL DEFAULT true, created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE(user_id,kind,value)
);
CREATE UNIQUE INDEX contacts_primary_idx ON app.user_contacts(user_id,kind) WHERE is_primary;
CREATE TABLE app.addresses (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), user_id uuid NOT NULL REFERENCES app.users,
  label text NOT NULL, country_code char(2) NOT NULL, region text, city text NOT NULL,
  address_line text NOT NULL, instructions text, location public.geography(Point,4326) NOT NULL,
  is_default boolean NOT NULL DEFAULT false, archived_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(), UNIQUE(id,user_id)
);
CREATE UNIQUE INDEX address_default_idx ON app.addresses(user_id) WHERE is_default AND archived_at IS NULL;
CREATE TABLE app.legal_documents (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), kind text NOT NULL, version text NOT NULL,
  content_hash text NOT NULL, object_key text NOT NULL, effective_at timestamptz NOT NULL,
  UNIQUE(kind,version)
);
CREATE TABLE app.user_consents (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), user_id uuid NOT NULL REFERENCES app.users,
  document_id uuid NOT NULL REFERENCES app.legal_documents, accepted_at timestamptz NOT NULL DEFAULT now(),
  revoked_at timestamptz, evidence_hash text NOT NULL, UNIQUE(user_id,document_id)
);
CREATE TABLE app.files (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), owner_id uuid NOT NULL REFERENCES app.users,
  object_key text NOT NULL UNIQUE, mime_type text NOT NULL, bytes bigint NOT NULL CHECK(bytes>0),
  sha256 text NOT NULL CHECK(sha256 ~ '^[a-f0-9]{64}$'),
  purpose text NOT NULL CHECK(purpose IN ('avatar','portfolio','request','chat','product','kyc','evidence')),
  moderation_status text NOT NULL DEFAULT 'pending' CHECK(moderation_status IN ('pending','approved','rejected')),
  exif_removed boolean NOT NULL DEFAULT false, deleted_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE app.categories (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), parent_id uuid REFERENCES app.categories,
  slug text NOT NULL UNIQUE, name text NOT NULL, active boolean NOT NULL DEFAULT true,
  CHECK(parent_id IS DISTINCT FROM id)
);
CREATE TABLE app.professionals (
  user_id uuid PRIMARY KEY REFERENCES app.users,
  slug text NOT NULL UNIQUE CHECK(slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
  business_name text NOT NULL, bio text NOT NULL DEFAULT '',
  years_experience integer CHECK(years_experience BETWEEN 0 AND 90), avatar_file_id uuid REFERENCES app.files,
  status text NOT NULL DEFAULT 'active' CHECK(status IN ('active','paused','suspended')),
  accepts_requests boolean NOT NULL DEFAULT true, created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE app.branches (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), professional_id uuid NOT NULL REFERENCES app.professionals,
  name text NOT NULL, country_code char(2) NOT NULL, city text NOT NULL,
  address_line text NOT NULL, location public.geography(Point,4326) NOT NULL,
  public_address boolean NOT NULL DEFAULT false, active boolean NOT NULL DEFAULT true,
  timezone text NOT NULL DEFAULT 'UTC', created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE(id,professional_id)
);
CREATE INDEX branches_location_idx ON app.branches USING gist(location);
CREATE INDEX branches_professional_idx ON app.branches(professional_id) WHERE active;
CREATE TABLE app.professional_contacts (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), professional_id uuid NOT NULL REFERENCES app.professionals,
  branch_id uuid, kind text NOT NULL CHECK(kind IN ('phone','whatsapp','email','website')),
  label text NOT NULL, value text NOT NULL, verified_at timestamptz,
  FOREIGN KEY(branch_id,professional_id) REFERENCES app.branches(id,professional_id)
);
CREATE TABLE app.branch_hours (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), branch_id uuid NOT NULL REFERENCES app.branches,
  weekday smallint NOT NULL CHECK(weekday BETWEEN 0 AND 6), opens_at time NOT NULL, closes_at time NOT NULL,
  CHECK(closes_at>opens_at), UNIQUE(branch_id,weekday,opens_at)
);
CREATE TABLE app.offerings (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), professional_id uuid NOT NULL REFERENCES app.professionals,
  category_id uuid NOT NULL REFERENCES app.categories, title text NOT NULL, description text NOT NULL DEFAULT '',
  pricing_type text NOT NULL DEFAULT 'quote' CHECK(pricing_type IN ('quote','fixed','hourly','from')),
  price_min_minor bigint CHECK(price_min_minor>=0), price_max_minor bigint CHECK(price_max_minor>=price_min_minor),
  currency char(3) NOT NULL CHECK(currency ~ '^[A-Z]{3}$'), active boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now(), UNIQUE(id,professional_id,category_id)
);
CREATE INDEX offerings_category_idx ON app.offerings(category_id,professional_id) WHERE active;
CREATE TABLE app.coverage_areas (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), professional_id uuid NOT NULL REFERENCES app.professionals,
  branch_id uuid NOT NULL, category_id uuid NOT NULL REFERENCES app.categories,
  radius_m integer NOT NULL CHECK(radius_m BETWEEN 100 AND 200000),
  polygon public.geography(MultiPolygon,4326), active boolean NOT NULL DEFAULT true,
  FOREIGN KEY(branch_id,professional_id) REFERENCES app.branches(id,professional_id),
  UNIQUE(branch_id,category_id)
);
CREATE INDEX coverage_category_idx ON app.coverage_areas(category_id,professional_id) WHERE active;
CREATE TABLE app.portfolio_items (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), professional_id uuid NOT NULL REFERENCES app.professionals,
  title text NOT NULL, description text, file_id uuid NOT NULL REFERENCES app.files,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE app.professional_credentials (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), professional_id uuid NOT NULL REFERENCES app.professionals,
  title text NOT NULL, issuer text NOT NULL, reference_ciphertext bytea, evidence_file_id uuid REFERENCES app.files,
  issued_on date, expires_on date, verified_at timestamptz, verified_by uuid REFERENCES app.users
);
CREATE TABLE app.kyc_cases (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), user_id uuid NOT NULL REFERENCES app.users,
  provider text NOT NULL, external_reference text, status text NOT NULL DEFAULT 'pending'
    CHECK(status IN ('pending','in_review','approved','rejected','expired','revoked')),
  identity_fingerprint text, document_ciphertext bytea, encryption_key_id text,
  submitted_at timestamptz NOT NULL DEFAULT now(), reviewed_at timestamptz, reviewed_by uuid REFERENCES app.users,
  expires_at timestamptz, reason text,
  UNIQUE(provider,external_reference),
  CHECK(status <> 'approved' OR (reviewed_at IS NOT NULL AND expires_at>reviewed_at))
);
CREATE INDEX kyc_user_idx ON app.kyc_cases(user_id,status,expires_at);
CREATE TABLE app.kyc_documents (
  case_id uuid REFERENCES app.kyc_cases, file_id uuid REFERENCES app.files,
  kind text NOT NULL, PRIMARY KEY(case_id,file_id)
);

CREATE TABLE app.credit_wallets (
  professional_id uuid PRIMARY KEY REFERENCES app.professionals,
  balance bigint NOT NULL DEFAULT 0 CHECK(balance>=0), updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE app.plans (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), code text NOT NULL UNIQUE, name text NOT NULL,
  credits integer NOT NULL CHECK(credits>=0), price_minor bigint NOT NULL CHECK(price_minor>0),
  currency char(3) NOT NULL CHECK(currency ~ '^[A-Z]{3}$'),
  positioning_weight integer NOT NULL DEFAULT 0 CHECK(positioning_weight BETWEEN 0 AND 100),
  positioning_days integer NOT NULL DEFAULT 0 CHECK(positioning_days>=0),
  marketplace_days integer NOT NULL DEFAULT 0 CHECK(marketplace_days>=0), active boolean NOT NULL DEFAULT false,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE app.credit_purchases (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), professional_id uuid NOT NULL REFERENCES app.professionals,
  plan_id uuid NOT NULL REFERENCES app.plans, idempotency_key uuid NOT NULL,
  credits integer NOT NULL CHECK(credits>=0), amount_minor bigint NOT NULL CHECK(amount_minor>0), currency char(3) NOT NULL,
  positioning_weight integer NOT NULL CHECK(positioning_weight BETWEEN 0 AND 100),
  positioning_days integer NOT NULL CHECK(positioning_days>=0), marketplace_days integer NOT NULL CHECK(marketplace_days>=0),
  status text NOT NULL DEFAULT 'pending' CHECK(status IN ('pending','paid','failed','refund_pending','refunded','chargeback')),
  created_at timestamptz NOT NULL DEFAULT now(), paid_at timestamptz,
  UNIQUE(professional_id,idempotency_key)
);
CREATE TABLE app.payment_events (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), provider text NOT NULL, external_event_id text NOT NULL,
  external_payment_id text NOT NULL, purchase_id uuid NOT NULL REFERENCES app.credit_purchases,
  event_type text NOT NULL, amount_minor bigint NOT NULL CHECK(amount_minor>0), currency char(3) NOT NULL,
  payload_hash text NOT NULL, received_at timestamptz NOT NULL DEFAULT now(), UNIQUE(provider,external_event_id)
);
CREATE UNIQUE INDEX payment_capture_once_idx ON app.payment_events(provider,external_payment_id) WHERE event_type='captured';
CREATE TABLE app.entitlements (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), professional_id uuid NOT NULL REFERENCES app.professionals,
  purchase_id uuid NOT NULL REFERENCES app.credit_purchases,
  kind text NOT NULL CHECK(kind IN ('positioning','marketplace')), weight integer NOT NULL DEFAULT 0,
  starts_at timestamptz NOT NULL, ends_at timestamptz NOT NULL, revoked_at timestamptz,
  CHECK(ends_at>starts_at), UNIQUE(purchase_id,kind)
);
CREATE INDEX entitlements_owner_idx ON app.entitlements(professional_id,kind,ends_at);

CREATE TABLE app.service_requests (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), customer_id uuid NOT NULL REFERENCES app.users,
  category_id uuid NOT NULL REFERENCES app.categories,
  mode text NOT NULL CHECK(mode IN ('direct','broadcast')), target_professional_id uuid REFERENCES app.professionals,
  title text NOT NULL CHECK(length(title) BETWEEN 5 AND 160), description text NOT NULL CHECK(length(description) BETWEEN 10 AND 10000),
  status text NOT NULL DEFAULT 'draft' CHECK(status IN ('draft','open','matched','cancelled','expired','completed')),
  moderation_status text NOT NULL DEFAULT 'pending' CHECK(moderation_status IN ('pending','approved','rejected')),
  budget_min_minor bigint CHECK(budget_min_minor>=0), budget_max_minor bigint CHECK(budget_max_minor>=0),
  currency char(3) NOT NULL CHECK(currency ~ '^[A-Z]{3}$'), search_radius_m integer NOT NULL CHECK(search_radius_m BETWEEN 100 AND 200000),
  scheduled_at timestamptz, expires_at timestamptz NOT NULL, created_at timestamptz NOT NULL DEFAULT now(),
  idempotency_key uuid NOT NULL, UNIQUE(customer_id,idempotency_key), UNIQUE(id,customer_id),
  CHECK(budget_max_minor IS NULL OR budget_min_minor IS NULL OR budget_max_minor>=budget_min_minor),
  CHECK((mode='direct')=(target_professional_id IS NOT NULL)), CHECK(customer_id IS DISTINCT FROM target_professional_id),
  CHECK(expires_at>created_at)
);
CREATE INDEX requests_feed_idx ON app.service_requests(category_id,expires_at) WHERE status='open';
CREATE INDEX requests_customer_idx ON app.service_requests(customer_id,created_at DESC);
CREATE TABLE app.request_locations (
  request_id uuid PRIMARY KEY REFERENCES app.service_requests, customer_id uuid NOT NULL REFERENCES app.users,
  source_address_id uuid, location public.geography(Point,4326) NOT NULL,
  address_line text NOT NULL, instructions text, country_code char(2) NOT NULL, city text NOT NULL,
  FOREIGN KEY(request_id,customer_id) REFERENCES app.service_requests(id,customer_id),
  FOREIGN KEY(source_address_id,customer_id) REFERENCES app.addresses(id,user_id)
);
CREATE INDEX request_locations_geo_idx ON app.request_locations USING gist(location);
CREATE TABLE app.request_attachments (
  request_id uuid REFERENCES app.service_requests, file_id uuid REFERENCES app.files,
  PRIMARY KEY(request_id,file_id)
);
CREATE TABLE app.proposals (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), request_id uuid NOT NULL REFERENCES app.service_requests,
  professional_id uuid NOT NULL REFERENCES app.professionals, offering_id uuid REFERENCES app.offerings,
  message text NOT NULL CHECK(length(message) BETWEEN 1 AND 4000),
  price_minor bigint CHECK(price_minor>=0), currency char(3) NOT NULL,
  status text NOT NULL DEFAULT 'pending' CHECK(status IN ('pending','accepted','rejected','withdrawn')),
  professional_accepted_at timestamptz NOT NULL DEFAULT now(), customer_accepted_at timestamptz,
  expires_at timestamptz NOT NULL, created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE(request_id,professional_id), UNIQUE(id,request_id,professional_id),
  CHECK(expires_at>created_at)
);
CREATE INDEX proposals_professional_idx ON app.proposals(professional_id,status);
CREATE TABLE app.contracts (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), request_id uuid NOT NULL UNIQUE,
  customer_id uuid NOT NULL REFERENCES app.users, professional_id uuid NOT NULL REFERENCES app.professionals,
  proposal_id uuid,
  status text NOT NULL DEFAULT 'accepted' CHECK(status IN ('accepted','en_route','arrived','in_progress','awaiting_confirmation','completed','cancelled','disputed')),
  agreed_price_minor bigint CHECK(agreed_price_minor>=0), currency char(3) NOT NULL,
  payment_method text, payment_terms text, terms_version integer NOT NULL DEFAULT 1,
  customer_accepted_at timestamptz NOT NULL, professional_accepted_at timestamptz NOT NULL,
  accepted_at timestamptz NOT NULL DEFAULT now(), en_route_at timestamptz, arrived_at timestamptz, started_at timestamptz,
  professional_finished_at timestamptz, customer_finished_at timestamptz, completed_at timestamptz,
  cancelled_at timestamptz, cancellation_reason text,
  platform_fee_minor bigint NOT NULL DEFAULT 0 CHECK(platform_fee_minor=0),
  FOREIGN KEY(request_id,customer_id) REFERENCES app.service_requests(id,customer_id),
  FOREIGN KEY(proposal_id,request_id,professional_id) REFERENCES app.proposals(id,request_id,professional_id),
  CHECK(customer_id<>professional_id),
  CHECK(status<>'completed' OR (professional_finished_at IS NOT NULL AND customer_finished_at IS NOT NULL AND completed_at IS NOT NULL))
);
CREATE INDEX contracts_customer_idx ON app.contracts(customer_id,accepted_at DESC);
CREATE INDEX contracts_professional_idx ON app.contracts(professional_id,status);
CREATE TABLE app.contract_events (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY, contract_id uuid NOT NULL REFERENCES app.contracts,
  actor_id uuid REFERENCES app.users, event_type text NOT NULL, from_status text, to_status text,
  metadata jsonb NOT NULL DEFAULT '{}', created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE app.contract_terms (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), contract_id uuid NOT NULL REFERENCES app.contracts,
  version integer NOT NULL CHECK(version>0), proposed_by uuid NOT NULL REFERENCES app.users,
  price_minor bigint CHECK(price_minor>=0), currency char(3) NOT NULL, payment_method text NOT NULL, payment_terms text,
  customer_accepted_at timestamptz, professional_accepted_at timestamptz, created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE(contract_id,version)
);
CREATE TABLE app.location_shares (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), contract_id uuid NOT NULL REFERENCES app.contracts,
  customer_id uuid NOT NULL REFERENCES app.users, purpose text NOT NULL DEFAULT 'service_navigation',
  shared_at timestamptz NOT NULL DEFAULT now(), expires_at timestamptz NOT NULL, revoked_at timestamptz,
  CHECK(expires_at>shared_at)
);
CREATE INDEX location_shares_contract_idx ON app.location_shares(contract_id,expires_at) WHERE revoked_at IS NULL;
CREATE TABLE app.professional_positions (
  contract_id uuid PRIMARY KEY REFERENCES app.contracts,
  location public.geography(Point,4326) NOT NULL, accuracy_m integer CHECK(accuracy_m>=0),
  recorded_at timestamptz NOT NULL DEFAULT now(), expires_at timestamptz NOT NULL
);
CREATE TABLE app.credit_ledger (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY, professional_id uuid NOT NULL REFERENCES app.credit_wallets,
  delta bigint NOT NULL CHECK(delta<>0), balance_after bigint NOT NULL CHECK(balance_after>=0),
  kind text NOT NULL CHECK(kind IN ('welcome','purchase','contract_debit','contract_refund','adjustment','purchase_reversal')),
  contract_id uuid REFERENCES app.contracts, purchase_id uuid REFERENCES app.credit_purchases,
  reverses_id bigint REFERENCES app.credit_ledger, idempotency_key text NOT NULL UNIQUE,
  actor_id uuid REFERENCES app.users, reason text NOT NULL, created_at timestamptz NOT NULL DEFAULT now(),
  CHECK(kind<>'welcome' OR delta=3), CHECK(kind<>'contract_debit' OR (delta=-1 AND contract_id IS NOT NULL)),
  CHECK(kind<>'purchase' OR (delta>0 AND purchase_id IS NOT NULL)),
  CHECK(kind<>'contract_refund' OR (delta=1 AND reverses_id IS NOT NULL AND contract_id IS NOT NULL))
);
CREATE UNIQUE INDEX credits_welcome_once_idx ON app.credit_ledger(professional_id) WHERE kind='welcome';
CREATE UNIQUE INDEX credits_contract_once_idx ON app.credit_ledger(contract_id) WHERE kind='contract_debit';
CREATE UNIQUE INDEX credits_purchase_once_idx ON app.credit_ledger(purchase_id) WHERE kind='purchase';
CREATE UNIQUE INDEX credits_reversal_once_idx ON app.credit_ledger(reverses_id) WHERE reverses_id IS NOT NULL;
CREATE INDEX credit_history_idx ON app.credit_ledger(professional_id,id DESC);

CREATE TABLE app.conversations (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), contract_id uuid NOT NULL UNIQUE REFERENCES app.contracts,
  created_at timestamptz NOT NULL DEFAULT now(), closed_at timestamptz
);
CREATE TABLE app.messages (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), conversation_id uuid NOT NULL REFERENCES app.conversations,
  sender_id uuid NOT NULL REFERENCES app.users, client_message_id uuid NOT NULL,
  kind text NOT NULL DEFAULT 'text' CHECK(kind IN ('text','attachment','location_shared','system')),
  body text CHECK(length(body) BETWEEN 1 AND 10000), file_id uuid REFERENCES app.files,
  created_at timestamptz NOT NULL DEFAULT now(), redacted_at timestamptz,
  UNIQUE(conversation_id,sender_id,client_message_id), CHECK(body IS NOT NULL OR file_id IS NOT NULL)
);
CREATE INDEX messages_timeline_idx ON app.messages(conversation_id,created_at,id);
CREATE TABLE app.message_receipts (
  message_id uuid REFERENCES app.messages, user_id uuid REFERENCES app.users,
  delivered_at timestamptz, read_at timestamptz, PRIMARY KEY(message_id,user_id)
);
CREATE TABLE app.reviews (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), contract_id uuid NOT NULL REFERENCES app.contracts,
  author_id uuid NOT NULL REFERENCES app.users, subject_id uuid NOT NULL REFERENCES app.users,
  rating smallint NOT NULL CHECK(rating BETWEEN 1 AND 5), recommends boolean, comment text CHECK(length(comment)<=4000),
  response text, responded_at timestamptz, moderation_status text NOT NULL DEFAULT 'pending'
    CHECK(moderation_status IN ('pending','published','hidden')),
  created_at timestamptz NOT NULL DEFAULT now(), UNIQUE(contract_id,author_id), CHECK(author_id<>subject_id)
);
CREATE INDEX reviews_subject_idx ON app.reviews(subject_id) WHERE moderation_status='published';
CREATE TABLE app.favorites (
  customer_id uuid REFERENCES app.users, professional_id uuid REFERENCES app.professionals,
  created_at timestamptz NOT NULL DEFAULT now(), PRIMARY KEY(customer_id,professional_id), CHECK(customer_id<>professional_id)
);

CREATE TABLE app.stores (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), professional_id uuid NOT NULL UNIQUE REFERENCES app.professionals,
  slug text NOT NULL UNIQUE, name text NOT NULL, description text, status text NOT NULL DEFAULT 'inactive'
    CHECK(status IN ('inactive','active','suspended')),
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE app.products (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), store_id uuid NOT NULL REFERENCES app.stores,
  category_id uuid REFERENCES app.categories, sku text NOT NULL, name text NOT NULL, description text NOT NULL,
  price_minor bigint NOT NULL CHECK(price_minor>=0), currency char(3) NOT NULL CHECK(currency ~ '^[A-Z]{3}$'),
  stock integer NOT NULL DEFAULT 0 CHECK(stock>=0), status text NOT NULL DEFAULT 'draft'
    CHECK(status IN ('draft','active','archived')),
  created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now(), UNIQUE(store_id,sku)
);
CREATE TABLE app.product_images (
  product_id uuid REFERENCES app.products, file_id uuid REFERENCES app.files, sort_order integer NOT NULL DEFAULT 0,
  PRIMARY KEY(product_id,file_id)
);
CREATE TABLE app.orders (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), customer_id uuid NOT NULL REFERENCES app.users,
  store_id uuid NOT NULL REFERENCES app.stores, status text NOT NULL DEFAULT 'pending'
    CHECK(status IN ('pending','accepted','dispatched','delivered','cancelled','disputed')),
  total_minor bigint NOT NULL CHECK(total_minor>=0), currency char(3) NOT NULL,
  payment_method text, payment_status text NOT NULL DEFAULT 'not_reported'
    CHECK(payment_status IN ('not_reported','buyer_reported','seller_confirmed','disputed')),
  platform_fee_minor bigint NOT NULL DEFAULT 0 CHECK(platform_fee_minor=0),
  idempotency_key uuid NOT NULL, created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE(customer_id,idempotency_key)
);
CREATE INDEX orders_customer_idx ON app.orders(customer_id,created_at DESC);
CREATE INDEX orders_store_idx ON app.orders(store_id,status);
CREATE TABLE app.order_items (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), order_id uuid NOT NULL REFERENCES app.orders,
  product_id uuid NOT NULL REFERENCES app.products, sku_snapshot text NOT NULL, name_snapshot text NOT NULL,
  quantity integer NOT NULL CHECK(quantity>0), unit_price_minor bigint NOT NULL CHECK(unit_price_minor>=0),
  subtotal_minor bigint GENERATED ALWAYS AS (quantity::bigint*unit_price_minor) STORED,
  UNIQUE(order_id,product_id)
);
CREATE TABLE app.order_delivery (
  order_id uuid PRIMARY KEY REFERENCES app.orders, recipient_name text NOT NULL, phone text,
  address_ciphertext bytea NOT NULL, encryption_key_id text NOT NULL,
  buyer_shared_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE app.order_events (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY, order_id uuid NOT NULL REFERENCES app.orders,
  actor_id uuid NOT NULL REFERENCES app.users, event_type text NOT NULL, metadata jsonb NOT NULL DEFAULT '{}',
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE app.inventory_movements (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY, product_id uuid NOT NULL REFERENCES app.products,
  order_id uuid REFERENCES app.orders, delta integer NOT NULL CHECK(delta<>0), reason text NOT NULL,
  actor_id uuid NOT NULL REFERENCES app.users, created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE app.external_payment_reports (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), contract_id uuid REFERENCES app.contracts, order_id uuid REFERENCES app.orders,
  reported_by uuid NOT NULL REFERENCES app.users, amount_minor bigint NOT NULL CHECK(amount_minor>=0),
  currency char(3) NOT NULL, method text NOT NULL, reference_ciphertext bytea, confirmed_by uuid REFERENCES app.users,
  reported_at timestamptz NOT NULL DEFAULT now(), confirmed_at timestamptz,
  CHECK(num_nonnulls(contract_id,order_id)=1)
);

CREATE TABLE app.support_tickets (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), opened_by uuid NOT NULL REFERENCES app.users,
  assigned_to uuid REFERENCES app.users, contract_id uuid REFERENCES app.contracts, order_id uuid REFERENCES app.orders,
  category text NOT NULL CHECK(category IN ('service','credits','kyc','review','marketplace','account','abuse','other')),
  subject text NOT NULL, description text NOT NULL, status text NOT NULL DEFAULT 'open'
    CHECK(status IN ('open','in_progress','waiting_user','resolved','closed')),
  priority text NOT NULL DEFAULT 'normal' CHECK(priority IN ('low','normal','high','urgent')),
  created_at timestamptz NOT NULL DEFAULT now(), resolved_at timestamptz
);
CREATE INDEX tickets_queue_idx ON app.support_tickets(status,priority,created_at);
CREATE TABLE app.support_messages (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), ticket_id uuid NOT NULL REFERENCES app.support_tickets,
  author_id uuid NOT NULL REFERENCES app.users, body text NOT NULL, internal_only boolean NOT NULL DEFAULT false,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE app.moderation_actions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), actor_id uuid NOT NULL REFERENCES app.users,
  target_type text NOT NULL, target_id uuid NOT NULL, action text NOT NULL, reason text NOT NULL,
  ticket_id uuid REFERENCES app.support_tickets, created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE app.audit_events (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY, actor_id uuid REFERENCES app.users,
  action text NOT NULL, entity_type text NOT NULL, entity_id uuid,
  correlation_id text, metadata jsonb NOT NULL DEFAULT '{}', created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX audit_entity_idx ON app.audit_events(entity_type,entity_id,created_at);
CREATE TABLE app.outbox (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), topic text NOT NULL, aggregate_id uuid NOT NULL,
  recipient_id uuid REFERENCES app.users, payload jsonb NOT NULL DEFAULT '{}', deduplication_key text NOT NULL UNIQUE,
  status text NOT NULL DEFAULT 'pending' CHECK(status IN ('pending','processing','delivered','failed')),
  attempts integer NOT NULL DEFAULT 0 CHECK(attempts>=0), available_at timestamptz NOT NULL DEFAULT now(),
  locked_until timestamptz, last_error text, created_at timestamptz NOT NULL DEFAULT now(), delivered_at timestamptz
);
CREATE INDEX outbox_pending_idx ON app.outbox(available_at) WHERE status IN ('pending','processing');
CREATE TABLE app.notification_deliveries (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), outbox_id uuid NOT NULL REFERENCES app.outbox,
  channel text NOT NULL CHECK(channel IN ('push','email','in_app','sms')), provider_message_id text,
  attempt integer NOT NULL CHECK(attempt>0), status text NOT NULL, created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE(outbox_id,channel,attempt)
);
CREATE TABLE app.device_tokens (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), user_id uuid NOT NULL REFERENCES app.users,
  platform text NOT NULL CHECK(platform IN ('android','ios','web')), token_ciphertext bytea NOT NULL,
  token_fingerprint text NOT NULL UNIQUE, last_seen_at timestamptz NOT NULL DEFAULT now(), revoked_at timestamptz
);
CREATE TABLE app.notification_preferences (
  user_id uuid REFERENCES app.users, topic text NOT NULL, channel text NOT NULL,
  enabled boolean NOT NULL DEFAULT true, PRIMARY KEY(user_id,topic,channel)
);

-- Inmutabilidad para trazabilidad. El propietario de la BD sigue siendo una
-- autoridad privilegiada: exportar auditoría a almacenamiento externo en producción.
CREATE FUNCTION internal.reject_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'append_only: %', TG_TABLE_NAME; END $$;
DO $$ DECLARE t text; BEGIN
  FOREACH t IN ARRAY ARRAY['credit_ledger','contract_events','order_events','inventory_movements',
      'payment_events','audit_events','moderation_actions','notification_deliveries'] LOOP
    EXECUTE format('CREATE TRIGGER immutable BEFORE UPDATE OR DELETE ON app.%I FOR EACH ROW EXECUTE FUNCTION internal.reject_mutation()',t);
  END LOOP;
  FOR t IN SELECT tablename FROM pg_tables WHERE schemaname='app' LOOP
    EXECUTE format('ALTER TABLE app.%I ENABLE ROW LEVEL SECURITY',t);
  END LOOP;
END $$;
