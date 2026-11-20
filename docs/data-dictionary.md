# Diccionario de datos

85 tablas de negocio del esquema `app`. Generado con `python3 scripts/export_schema_docs.py`.

Los tiempos son `timestamptz`; dinero en unidades menores; distancias en metros. `meta.migrations` y las tablas de extensiones no se incluyen.

## addresses

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `user_id` | `uuid` | No | — |
| `label` | `text` | No | — |
| `country_code` | `character(2)` | No | — |
| `region` | `text` | Sí | — |
| `city` | `text` | No | — |
| `address_line` | `text` | No | — |
| `instructions` | `text` | Sí | — |
| `location` | `geography(Point,4326)` | No | — |
| `is_default` | `boolean` | No | false |
| `archived_at` | `timestamp with time zone` | Sí | — |
| `created_at` | `timestamp with time zone` | No | now() |
| `state_code` | `text` | Sí | — |
| `municipality_code` | `text` | Sí | — |
| `parish_code` | `text` | Sí | — |

Restricciones:

- `NOT NULL address_line`
- `CHECK ((((state_code IS NULL) AND (municipality_code IS NULL) AND (parish_code IS NULL)) OR ((country_code = 'VE'::bpchar) AND (state_code IS NOT NULL) AND (municipality_code IS NOT NULL) AND (parish_code IS NOT NULL))))`
- `NOT NULL city`
- `NOT NULL country_code`
- `NOT NULL created_at`
- `NOT NULL id`
- `UNIQUE (id, user_id)`
- `NOT NULL is_default`
- `NOT NULL label`
- `NOT NULL location`
- `FOREIGN KEY (municipality_code, state_code) REFERENCES app.geo_municipalities(code, state_code)`
- `FOREIGN KEY (parish_code, municipality_code) REFERENCES app.geo_parishes(code, municipality_code)`
- `PRIMARY KEY (id)`
- `FOREIGN KEY (state_code) REFERENCES app.geo_states(code)`
- `FOREIGN KEY (user_id) REFERENCES app.users(id)`
- `NOT NULL user_id`

Índices:

```sql
CREATE UNIQUE INDEX address_default_idx ON app.addresses USING btree (user_id) WHERE (is_default AND (archived_at IS NULL))
CREATE UNIQUE INDEX addresses_id_user_id_key ON app.addresses USING btree (id, user_id)
CREATE UNIQUE INDEX addresses_pkey ON app.addresses USING btree (id)
```

## audit_events

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `bigint` | No | IDENTITY |
| `actor_id` | `uuid` | Sí | — |
| `action` | `text` | No | — |
| `entity_type` | `text` | No | — |
| `entity_id` | `uuid` | Sí | — |
| `correlation_id` | `text` | Sí | — |
| `metadata` | `jsonb` | No | '{}'::jsonb |
| `created_at` | `timestamp with time zone` | No | now() |

Restricciones:

- `NOT NULL action`
- `FOREIGN KEY (actor_id) REFERENCES app.users(id)`
- `NOT NULL created_at`
- `NOT NULL entity_type`
- `NOT NULL id`
- `NOT NULL metadata`
- `PRIMARY KEY (id)`

Índices:

```sql
CREATE INDEX audit_entity_idx ON app.audit_events USING btree (entity_type, entity_id, created_at)
CREATE UNIQUE INDEX audit_events_pkey ON app.audit_events USING btree (id)
```

## auth_challenges

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `user_id` | `uuid` | No | — |
| `purpose` | `text` | No | — |
| `token_hash` | `text` | No | — |
| `expires_at` | `timestamp with time zone` | No | — |
| `consumed_at` | `timestamp with time zone` | Sí | — |
| `attempts` | `integer` | No | 0 |
| `created_at` | `timestamp with time zone` | No | now() |

Restricciones:

- `CHECK ((attempts >= 0))`
- `NOT NULL attempts`
- `NOT NULL created_at`
- `NOT NULL expires_at`
- `NOT NULL id`
- `PRIMARY KEY (id)`
- `CHECK ((purpose = ANY (ARRAY['email_verification'::text, 'phone_verification'::text, 'password_reset'::text, 'mfa'::text])))`
- `NOT NULL purpose`
- `UNIQUE (token_hash)`
- `NOT NULL token_hash`
- `FOREIGN KEY (user_id) REFERENCES app.users(id)`
- `NOT NULL user_id`

Índices:

```sql
CREATE UNIQUE INDEX auth_challenges_pkey ON app.auth_challenges USING btree (id)
CREATE UNIQUE INDEX auth_challenges_token_hash_key ON app.auth_challenges USING btree (token_hash)
```

## auth_identities

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `user_id` | `uuid` | No | — |
| `provider` | `text` | No | — |
| `subject` | `text` | No | — |
| `created_at` | `timestamp with time zone` | No | now() |

Restricciones:

- `NOT NULL created_at`
- `NOT NULL id`
- `PRIMARY KEY (id)`
- `NOT NULL provider`
- `UNIQUE (provider, subject)`
- `NOT NULL subject`
- `FOREIGN KEY (user_id) REFERENCES app.users(id)`
- `NOT NULL user_id`

Índices:

```sql
CREATE UNIQUE INDEX auth_identities_pkey ON app.auth_identities USING btree (id)
CREATE UNIQUE INDEX auth_identities_provider_subject_key ON app.auth_identities USING btree (provider, subject)
```

## branch_hours

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `branch_id` | `uuid` | No | — |
| `weekday` | `smallint` | No | — |
| `opens_at` | `time without time zone` | No | — |
| `closes_at` | `time without time zone` | No | — |

Restricciones:

- `FOREIGN KEY (branch_id) REFERENCES app.branches(id)`
- `NOT NULL branch_id`
- `UNIQUE (branch_id, weekday, opens_at)`
- `CHECK ((closes_at > opens_at))`
- `NOT NULL closes_at`
- `NOT NULL id`
- `NOT NULL opens_at`
- `PRIMARY KEY (id)`
- `CHECK (((weekday >= 0) AND (weekday <= 6)))`
- `NOT NULL weekday`

Índices:

```sql
CREATE UNIQUE INDEX branch_hours_branch_id_weekday_opens_at_key ON app.branch_hours USING btree (branch_id, weekday, opens_at)
CREATE UNIQUE INDEX branch_hours_pkey ON app.branch_hours USING btree (id)
```

## branches

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `professional_id` | `uuid` | No | — |
| `name` | `text` | No | — |
| `country_code` | `character(2)` | No | — |
| `city` | `text` | No | — |
| `address_line` | `text` | No | — |
| `location` | `geography(Point,4326)` | No | — |
| `public_address` | `boolean` | No | false |
| `active` | `boolean` | No | true |
| `timezone` | `text` | No | 'America/Caracas'::text |
| `created_at` | `timestamp with time zone` | No | now() |
| `state_code` | `text` | Sí | — |
| `municipality_code` | `text` | Sí | — |
| `parish_code` | `text` | Sí | — |

Restricciones:

- `NOT NULL active`
- `NOT NULL address_line`
- `CHECK ((((state_code IS NULL) AND (municipality_code IS NULL) AND (parish_code IS NULL)) OR ((country_code = 'VE'::bpchar) AND (state_code IS NOT NULL) AND (municipality_code IS NOT NULL) AND (parish_code IS NOT NULL))))`
- `NOT NULL city`
- `NOT NULL country_code`
- `NOT NULL created_at`
- `NOT NULL id`
- `UNIQUE (id, professional_id)`
- `NOT NULL location`
- `FOREIGN KEY (municipality_code, state_code) REFERENCES app.geo_municipalities(code, state_code)`
- `NOT NULL name`
- `FOREIGN KEY (parish_code, municipality_code) REFERENCES app.geo_parishes(code, municipality_code)`
- `PRIMARY KEY (id)`
- `FOREIGN KEY (professional_id) REFERENCES app.professionals(user_id)`
- `NOT NULL professional_id`
- `NOT NULL public_address`
- `FOREIGN KEY (state_code) REFERENCES app.geo_states(code)`
- `NOT NULL timezone`

Índices:

```sql
CREATE UNIQUE INDEX branches_id_professional_id_key ON app.branches USING btree (id, professional_id)
CREATE INDEX branches_location_idx ON app.branches USING gist (location)
CREATE UNIQUE INDEX branches_pkey ON app.branches USING btree (id)
CREATE INDEX branches_professional_idx ON app.branches USING btree (professional_id) WHERE active
```

## categories

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `parent_id` | `uuid` | Sí | — |
| `slug` | `text` | No | — |
| `name` | `text` | No | — |
| `active` | `boolean` | No | true |

Restricciones:

- `NOT NULL active`
- `CHECK ((parent_id IS DISTINCT FROM id))`
- `NOT NULL id`
- `NOT NULL name`
- `FOREIGN KEY (parent_id) REFERENCES app.categories(id)`
- `PRIMARY KEY (id)`
- `UNIQUE (slug)`
- `NOT NULL slug`

Índices:

```sql
CREATE UNIQUE INDEX categories_pkey ON app.categories USING btree (id)
CREATE UNIQUE INDEX categories_slug_key ON app.categories USING btree (slug)
```

## contract_events

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `bigint` | No | IDENTITY |
| `contract_id` | `uuid` | No | — |
| `actor_id` | `uuid` | Sí | — |
| `event_type` | `text` | No | — |
| `from_status` | `text` | Sí | — |
| `to_status` | `text` | Sí | — |
| `metadata` | `jsonb` | No | '{}'::jsonb |
| `created_at` | `timestamp with time zone` | No | now() |

Restricciones:

- `FOREIGN KEY (actor_id) REFERENCES app.users(id)`
- `FOREIGN KEY (contract_id) REFERENCES app.contracts(id)`
- `NOT NULL contract_id`
- `NOT NULL created_at`
- `NOT NULL event_type`
- `NOT NULL id`
- `NOT NULL metadata`
- `PRIMARY KEY (id)`

Índices:

```sql
CREATE UNIQUE INDEX contract_events_pkey ON app.contract_events USING btree (id)
```

## contract_terms

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `contract_id` | `uuid` | No | — |
| `version` | `integer` | No | — |
| `proposed_by` | `uuid` | No | — |
| `price_minor` | `bigint` | Sí | — |
| `currency` | `character(3)` | No | — |
| `payment_method` | `text` | No | — |
| `payment_terms` | `text` | Sí | — |
| `customer_accepted_at` | `timestamp with time zone` | Sí | — |
| `professional_accepted_at` | `timestamp with time zone` | Sí | — |
| `created_at` | `timestamp with time zone` | No | now() |

Restricciones:

- `FOREIGN KEY (contract_id) REFERENCES app.contracts(id)`
- `NOT NULL contract_id`
- `UNIQUE (contract_id, version)`
- `NOT NULL created_at`
- `NOT NULL currency`
- `NOT NULL id`
- `NOT NULL payment_method`
- `PRIMARY KEY (id)`
- `CHECK ((price_minor >= 0))`
- `FOREIGN KEY (proposed_by) REFERENCES app.users(id)`
- `NOT NULL proposed_by`
- `CHECK ((version > 0))`
- `NOT NULL version`

Índices:

```sql
CREATE UNIQUE INDEX contract_terms_contract_id_version_key ON app.contract_terms USING btree (contract_id, version)
CREATE UNIQUE INDEX contract_terms_pkey ON app.contract_terms USING btree (id)
```

## contracts

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `request_id` | `uuid` | No | — |
| `customer_id` | `uuid` | No | — |
| `professional_id` | `uuid` | No | — |
| `proposal_id` | `uuid` | Sí | — |
| `status` | `text` | No | 'accepted'::text |
| `agreed_price_minor` | `bigint` | Sí | — |
| `currency` | `character(3)` | No | — |
| `payment_method` | `text` | Sí | — |
| `payment_terms` | `text` | Sí | — |
| `terms_version` | `integer` | No | 1 |
| `customer_accepted_at` | `timestamp with time zone` | No | — |
| `professional_accepted_at` | `timestamp with time zone` | No | — |
| `accepted_at` | `timestamp with time zone` | No | now() |
| `en_route_at` | `timestamp with time zone` | Sí | — |
| `arrived_at` | `timestamp with time zone` | Sí | — |
| `started_at` | `timestamp with time zone` | Sí | — |
| `professional_finished_at` | `timestamp with time zone` | Sí | — |
| `customer_finished_at` | `timestamp with time zone` | Sí | — |
| `completed_at` | `timestamp with time zone` | Sí | — |
| `cancelled_at` | `timestamp with time zone` | Sí | — |
| `cancellation_reason` | `text` | Sí | — |
| `platform_fee_minor` | `bigint` | No | 0 |
| `delivery_mode` | `text` | No | 'onsite'::text |
| `payment_protection` | `text` | No | 'external'::text |
| `work_requirements` | `text` | No | ''::text |

Restricciones:

- `UNIQUE (id, customer_id)`
- `NOT NULL accepted_at`
- `CHECK ((agreed_price_minor >= 0))`
- `CHECK ((customer_id <> professional_id))`
- `CHECK (((status <> 'completed'::text) OR ((professional_finished_at IS NOT NULL) AND (customer_finished_at IS NOT NULL) AND (completed_at IS NOT NULL))))`
- `NOT NULL currency`
- `NOT NULL customer_accepted_at`
- `FOREIGN KEY (customer_id) REFERENCES app.users(id)`
- `NOT NULL customer_id`
- `CHECK ((delivery_mode = ANY (ARRAY['onsite'::text, 'remote'::text])))`
- `NOT NULL delivery_mode`
- `NOT NULL id`
- `CHECK ((payment_protection = ANY (ARRAY['external'::text, 'guaranteed'::text])))`
- `NOT NULL payment_protection`
- `PRIMARY KEY (id)`
- `CHECK ((platform_fee_minor = 0))`
- `NOT NULL platform_fee_minor`
- `NOT NULL professional_accepted_at`
- `FOREIGN KEY (professional_id) REFERENCES app.professionals(user_id)`
- `NOT NULL professional_id`
- `FOREIGN KEY (proposal_id, request_id, professional_id) REFERENCES app.proposals(id, request_id, professional_id)`
- `FOREIGN KEY (request_id, customer_id) REFERENCES app.service_requests(id, customer_id)`
- `UNIQUE (request_id)`
- `NOT NULL request_id`
- `CHECK ((status = ANY (ARRAY['accepted'::text, 'en_route'::text, 'arrived'::text, 'in_progress'::text, 'awaiting_confirmation'::text, 'completed'::text, 'cancelled'::text, 'disputed'::text])))`
- `NOT NULL status`
- `NOT NULL terms_version`
- `NOT NULL work_requirements`
- `CHECK (((delivery_mode <> 'remote'::text) OR (payment_protection = 'guaranteed'::text)))`

Índices:

```sql
CREATE UNIQUE INDEX contract_customer_identity ON app.contracts USING btree (id, customer_id)
CREATE INDEX contracts_customer_idx ON app.contracts USING btree (customer_id, accepted_at DESC)
CREATE UNIQUE INDEX contracts_pkey ON app.contracts USING btree (id)
CREATE INDEX contracts_professional_idx ON app.contracts USING btree (professional_id, status)
CREATE UNIQUE INDEX contracts_request_id_key ON app.contracts USING btree (request_id)
```

## conversations

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `contract_id` | `uuid` | No | — |
| `created_at` | `timestamp with time zone` | No | now() |
| `closed_at` | `timestamp with time zone` | Sí | — |

Restricciones:

- `FOREIGN KEY (contract_id) REFERENCES app.contracts(id)`
- `UNIQUE (contract_id)`
- `NOT NULL contract_id`
- `NOT NULL created_at`
- `NOT NULL id`
- `PRIMARY KEY (id)`

Índices:

```sql
CREATE UNIQUE INDEX conversations_contract_id_key ON app.conversations USING btree (contract_id)
CREATE UNIQUE INDEX conversations_pkey ON app.conversations USING btree (id)
```

## coverage_areas

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `professional_id` | `uuid` | No | — |
| `branch_id` | `uuid` | No | — |
| `category_id` | `uuid` | No | — |
| `radius_m` | `integer` | No | — |
| `polygon` | `geography(MultiPolygon,4326)` | Sí | — |
| `active` | `boolean` | No | true |

Restricciones:

- `NOT NULL active`
- `UNIQUE (branch_id, category_id)`
- `NOT NULL branch_id`
- `FOREIGN KEY (branch_id, professional_id) REFERENCES app.branches(id, professional_id)`
- `FOREIGN KEY (category_id) REFERENCES app.categories(id)`
- `NOT NULL category_id`
- `NOT NULL id`
- `PRIMARY KEY (id)`
- `FOREIGN KEY (professional_id) REFERENCES app.professionals(user_id)`
- `NOT NULL professional_id`
- `CHECK (((radius_m >= 100) AND (radius_m <= 200000)))`
- `NOT NULL radius_m`

Índices:

```sql
CREATE UNIQUE INDEX coverage_areas_branch_id_category_id_key ON app.coverage_areas USING btree (branch_id, category_id)
CREATE UNIQUE INDEX coverage_areas_pkey ON app.coverage_areas USING btree (id)
CREATE INDEX coverage_category_idx ON app.coverage_areas USING btree (category_id, professional_id) WHERE active
```

## credentials

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `user_id` | `uuid` | No | — |
| `password_hash` | `text` | No | — |
| `password_changed_at` | `timestamp with time zone` | No | now() |
| `failed_attempts` | `integer` | No | 0 |
| `locked_until` | `timestamp with time zone` | Sí | — |
| `mfa_secret_ciphertext` | `bytea` | Sí | — |
| `mfa_enabled_at` | `timestamp with time zone` | Sí | — |

Restricciones:

- `CHECK ((failed_attempts >= 0))`
- `NOT NULL failed_attempts`
- `NOT NULL password_changed_at`
- `CHECK ((password_hash ~~ '$argon2id$%'::text))`
- `NOT NULL password_hash`
- `PRIMARY KEY (user_id)`
- `FOREIGN KEY (user_id) REFERENCES app.users(id)`
- `NOT NULL user_id`

Índices:

```sql
CREATE UNIQUE INDEX credentials_pkey ON app.credentials USING btree (user_id)
```

## credit_ledger

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `bigint` | No | IDENTITY |
| `professional_id` | `uuid` | No | — |
| `delta` | `bigint` | No | — |
| `balance_after` | `bigint` | No | — |
| `kind` | `text` | No | — |
| `contract_id` | `uuid` | Sí | — |
| `purchase_id` | `uuid` | Sí | — |
| `reverses_id` | `bigint` | Sí | — |
| `idempotency_key` | `text` | No | — |
| `actor_id` | `uuid` | Sí | — |
| `reason` | `text` | No | — |
| `created_at` | `timestamp with time zone` | No | now() |

Restricciones:

- `FOREIGN KEY (actor_id) REFERENCES app.users(id)`
- `CHECK ((balance_after >= 0))`
- `NOT NULL balance_after`
- `CHECK (((kind <> 'welcome'::text) OR (delta = 3)))`
- `CHECK (((kind <> 'contract_debit'::text) OR ((delta = '-1'::integer) AND (contract_id IS NOT NULL))))`
- `CHECK (((kind <> 'purchase'::text) OR ((delta > 0) AND (purchase_id IS NOT NULL))))`
- `CHECK (((kind <> 'contract_refund'::text) OR ((delta = 1) AND (reverses_id IS NOT NULL) AND (contract_id IS NOT NULL))))`
- `FOREIGN KEY (contract_id) REFERENCES app.contracts(id)`
- `NOT NULL created_at`
- `CHECK ((delta <> 0))`
- `NOT NULL delta`
- `NOT NULL id`
- `UNIQUE (idempotency_key)`
- `NOT NULL idempotency_key`
- `CHECK ((kind = ANY (ARRAY['welcome'::text, 'purchase'::text, 'contract_debit'::text, 'contract_refund'::text, 'adjustment'::text, 'purchase_reversal'::text])))`
- `NOT NULL kind`
- `PRIMARY KEY (id)`
- `FOREIGN KEY (professional_id) REFERENCES app.credit_wallets(professional_id)`
- `NOT NULL professional_id`
- `FOREIGN KEY (purchase_id) REFERENCES app.credit_purchases(id)`
- `NOT NULL reason`
- `FOREIGN KEY (reverses_id) REFERENCES app.credit_ledger(id)`

Índices:

```sql
CREATE INDEX credit_history_idx ON app.credit_ledger USING btree (professional_id, id DESC)
CREATE UNIQUE INDEX credit_ledger_idempotency_key_key ON app.credit_ledger USING btree (idempotency_key)
CREATE UNIQUE INDEX credit_ledger_pkey ON app.credit_ledger USING btree (id)
CREATE UNIQUE INDEX credits_contract_once_idx ON app.credit_ledger USING btree (contract_id) WHERE (kind = 'contract_debit'::text)
CREATE UNIQUE INDEX credits_purchase_once_idx ON app.credit_ledger USING btree (purchase_id) WHERE (kind = 'purchase'::text)
CREATE UNIQUE INDEX credits_reversal_once_idx ON app.credit_ledger USING btree (reverses_id) WHERE (reverses_id IS NOT NULL)
CREATE UNIQUE INDEX credits_welcome_once_idx ON app.credit_ledger USING btree (professional_id) WHERE (kind = 'welcome'::text)
```

## credit_purchases

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `professional_id` | `uuid` | No | — |
| `plan_id` | `uuid` | No | — |
| `idempotency_key` | `uuid` | No | — |
| `credits` | `integer` | No | — |
| `amount_minor` | `bigint` | No | — |
| `currency` | `character(3)` | No | — |
| `positioning_weight` | `integer` | No | — |
| `positioning_days` | `integer` | No | — |
| `marketplace_days` | `integer` | No | — |
| `status` | `text` | No | 'pending'::text |
| `created_at` | `timestamp with time zone` | No | now() |
| `paid_at` | `timestamp with time zone` | Sí | — |

Restricciones:

- `CHECK ((amount_minor > 0))`
- `NOT NULL amount_minor`
- `NOT NULL created_at`
- `CHECK ((credits >= 0))`
- `NOT NULL credits`
- `NOT NULL currency`
- `NOT NULL id`
- `NOT NULL idempotency_key`
- `CHECK ((marketplace_days >= 0))`
- `NOT NULL marketplace_days`
- `PRIMARY KEY (id)`
- `FOREIGN KEY (plan_id) REFERENCES app.plans(id)`
- `NOT NULL plan_id`
- `CHECK ((positioning_days >= 0))`
- `NOT NULL positioning_days`
- `CHECK (((positioning_weight >= 0) AND (positioning_weight <= 100)))`
- `NOT NULL positioning_weight`
- `FOREIGN KEY (professional_id) REFERENCES app.professionals(user_id)`
- `UNIQUE (professional_id, idempotency_key)`
- `NOT NULL professional_id`
- `CHECK ((status = ANY (ARRAY['pending'::text, 'paid'::text, 'failed'::text, 'refund_pending'::text, 'refunded'::text, 'chargeback'::text])))`
- `NOT NULL status`

Índices:

```sql
CREATE UNIQUE INDEX credit_purchases_pkey ON app.credit_purchases USING btree (id)
CREATE UNIQUE INDEX credit_purchases_professional_id_idempotency_key_key ON app.credit_purchases USING btree (professional_id, idempotency_key)
```

## credit_wallets

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `professional_id` | `uuid` | No | — |
| `balance` | `bigint` | No | 0 |
| `updated_at` | `timestamp with time zone` | No | now() |

Restricciones:

- `CHECK ((balance >= 0))`
- `NOT NULL balance`
- `PRIMARY KEY (professional_id)`
- `FOREIGN KEY (professional_id) REFERENCES app.professionals(user_id)`
- `NOT NULL professional_id`
- `NOT NULL updated_at`

Índices:

```sql
CREATE UNIQUE INDEX credit_wallets_pkey ON app.credit_wallets USING btree (professional_id)
```

## delivery_files

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `delivery_id` | `uuid` | No | — |
| `file_id` | `uuid` | No | — |

Restricciones:

- `FOREIGN KEY (delivery_id) REFERENCES app.service_deliveries(id)`
- `NOT NULL delivery_id`
- `FOREIGN KEY (file_id) REFERENCES app.files(id)`
- `NOT NULL file_id`
- `PRIMARY KEY (delivery_id, file_id)`

Índices:

```sql
CREATE UNIQUE INDEX delivery_files_pkey ON app.delivery_files USING btree (delivery_id, file_id)
```

## device_tokens

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `user_id` | `uuid` | No | — |
| `platform` | `text` | No | — |
| `token_ciphertext` | `bytea` | No | — |
| `token_fingerprint` | `text` | No | — |
| `last_seen_at` | `timestamp with time zone` | No | now() |
| `revoked_at` | `timestamp with time zone` | Sí | — |

Restricciones:

- `NOT NULL id`
- `NOT NULL last_seen_at`
- `PRIMARY KEY (id)`
- `CHECK ((platform = ANY (ARRAY['android'::text, 'ios'::text, 'web'::text])))`
- `NOT NULL platform`
- `NOT NULL token_ciphertext`
- `UNIQUE (token_fingerprint)`
- `NOT NULL token_fingerprint`
- `FOREIGN KEY (user_id) REFERENCES app.users(id)`
- `NOT NULL user_id`

Índices:

```sql
CREATE UNIQUE INDEX device_tokens_pkey ON app.device_tokens USING btree (id)
CREATE UNIQUE INDEX device_tokens_token_fingerprint_key ON app.device_tokens USING btree (token_fingerprint)
```

## entitlements

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `professional_id` | `uuid` | No | — |
| `purchase_id` | `uuid` | No | — |
| `kind` | `text` | No | — |
| `weight` | `integer` | No | 0 |
| `starts_at` | `timestamp with time zone` | No | — |
| `ends_at` | `timestamp with time zone` | No | — |
| `revoked_at` | `timestamp with time zone` | Sí | — |

Restricciones:

- `CHECK ((ends_at > starts_at))`
- `NOT NULL ends_at`
- `NOT NULL id`
- `CHECK ((kind = ANY (ARRAY['positioning'::text, 'marketplace'::text])))`
- `NOT NULL kind`
- `PRIMARY KEY (id)`
- `FOREIGN KEY (professional_id) REFERENCES app.professionals(user_id)`
- `NOT NULL professional_id`
- `FOREIGN KEY (purchase_id) REFERENCES app.credit_purchases(id)`
- `UNIQUE (purchase_id, kind)`
- `NOT NULL purchase_id`
- `NOT NULL starts_at`
- `NOT NULL weight`

Índices:

```sql
CREATE INDEX entitlements_owner_idx ON app.entitlements USING btree (professional_id, kind, ends_at)
CREATE UNIQUE INDEX entitlements_pkey ON app.entitlements USING btree (id)
CREATE UNIQUE INDEX entitlements_purchase_id_kind_key ON app.entitlements USING btree (purchase_id, kind)
```

## external_payment_reports

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `contract_id` | `uuid` | Sí | — |
| `order_id` | `uuid` | Sí | — |
| `reported_by` | `uuid` | No | — |
| `amount_minor` | `bigint` | No | — |
| `currency` | `character(3)` | No | — |
| `method` | `text` | No | — |
| `reference_ciphertext` | `bytea` | Sí | — |
| `confirmed_by` | `uuid` | Sí | — |
| `reported_at` | `timestamp with time zone` | No | now() |
| `confirmed_at` | `timestamp with time zone` | Sí | — |

Restricciones:

- `CHECK ((amount_minor >= 0))`
- `NOT NULL amount_minor`
- `CHECK ((num_nonnulls(contract_id, order_id) = 1))`
- `FOREIGN KEY (confirmed_by) REFERENCES app.users(id)`
- `FOREIGN KEY (contract_id) REFERENCES app.contracts(id)`
- `NOT NULL currency`
- `NOT NULL id`
- `NOT NULL method`
- `FOREIGN KEY (order_id) REFERENCES app.orders(id)`
- `PRIMARY KEY (id)`
- `NOT NULL reported_at`
- `FOREIGN KEY (reported_by) REFERENCES app.users(id)`
- `NOT NULL reported_by`

Índices:

```sql
CREATE UNIQUE INDEX external_payment_reports_pkey ON app.external_payment_reports USING btree (id)
```

## favorites

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `customer_id` | `uuid` | No | — |
| `professional_id` | `uuid` | No | — |
| `created_at` | `timestamp with time zone` | No | now() |

Restricciones:

- `CHECK ((customer_id <> professional_id))`
- `NOT NULL created_at`
- `FOREIGN KEY (customer_id) REFERENCES app.users(id)`
- `NOT NULL customer_id`
- `PRIMARY KEY (customer_id, professional_id)`
- `FOREIGN KEY (professional_id) REFERENCES app.professionals(user_id)`
- `NOT NULL professional_id`

Índices:

```sql
CREATE UNIQUE INDEX favorites_pkey ON app.favorites USING btree (customer_id, professional_id)
```

## files

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `owner_id` | `uuid` | No | — |
| `object_key` | `text` | No | — |
| `mime_type` | `text` | No | — |
| `bytes` | `bigint` | No | — |
| `sha256` | `text` | No | — |
| `purpose` | `text` | No | — |
| `moderation_status` | `text` | No | 'pending'::text |
| `exif_removed` | `boolean` | No | false |
| `deleted_at` | `timestamp with time zone` | Sí | — |
| `created_at` | `timestamp with time zone` | No | now() |

Restricciones:

- `CHECK ((bytes > 0))`
- `NOT NULL bytes`
- `NOT NULL created_at`
- `NOT NULL exif_removed`
- `NOT NULL id`
- `NOT NULL mime_type`
- `CHECK ((moderation_status = ANY (ARRAY['pending'::text, 'approved'::text, 'rejected'::text])))`
- `NOT NULL moderation_status`
- `UNIQUE (object_key)`
- `NOT NULL object_key`
- `FOREIGN KEY (owner_id) REFERENCES app.users(id)`
- `NOT NULL owner_id`
- `PRIMARY KEY (id)`
- `CHECK ((purpose = ANY (ARRAY['avatar'::text, 'portfolio'::text, 'request'::text, 'chat'::text, 'product'::text, 'kyc'::text, 'evidence'::text])))`
- `NOT NULL purpose`
- `CHECK ((sha256 ~ '^[a-f0-9]{64}$'::text))`
- `NOT NULL sha256`

Índices:

```sql
CREATE UNIQUE INDEX files_object_key_key ON app.files USING btree (object_key)
CREATE UNIQUE INDEX files_pkey ON app.files USING btree (id)
```

## funding_quotes

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `contract_id` | `uuid` | No | — |
| `customer_id` | `uuid` | No | — |
| `terms_version` | `integer` | No | — |
| `price_minor` | `bigint` | No | — |
| `currency` | `character(3)` | No | — |
| `fee_minor` | `bigint` | No | 0 |
| `created_at` | `timestamp with time zone` | No | now() |
| `expires_at` | `timestamp with time zone` | No | — |
| `terms_fingerprint` | `jsonb` | No | '{}'::jsonb |

Restricciones:

- `FOREIGN KEY (contract_id) REFERENCES app.contracts(id)`
- `NOT NULL contract_id`
- `NOT NULL created_at`
- `NOT NULL currency`
- `FOREIGN KEY (customer_id) REFERENCES app.users(id)`
- `NOT NULL customer_id`
- `NOT NULL expires_at`
- `CHECK ((fee_minor = 0))`
- `NOT NULL fee_minor`
- `NOT NULL id`
- `PRIMARY KEY (id)`
- `CHECK (((price_minor >= 1) AND (price_minor <= '1000000000000'::bigint)))`
- `NOT NULL price_minor`
- `NOT NULL terms_fingerprint`
- `NOT NULL terms_version`

Índices:

```sql
CREATE UNIQUE INDEX funding_quotes_pkey ON app.funding_quotes USING btree (id)
```

## geo_countries

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `code` | `text` | No | — |
| `name` | `text` | No | — |
| `active` | `boolean` | No | false |
| `boundary` | `geometry(MultiPolygon,4326)` | No | — |

Restricciones:

- `NOT NULL active`
- `NOT NULL boundary`
- `CHECK ((code ~ '^[A-Z]{2}$'::text))`
- `NOT NULL code`
- `NOT NULL name`
- `PRIMARY KEY (code)`

Índices:

```sql
CREATE UNIQUE INDEX geo_countries_pkey ON app.geo_countries USING btree (code)
```

## geo_municipalities

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `code` | `text` | No | — |
| `state_code` | `text` | No | — |
| `name` | `text` | No | — |
| `kind` | `text` | No | 'municipality'::text |
| `latitude` | `double precision` | No | — |
| `longitude` | `double precision` | No | — |

Restricciones:

- `NOT NULL code`
- `UNIQUE (code, state_code)`
- `NOT NULL kind`
- `NOT NULL latitude`
- `NOT NULL longitude`
- `NOT NULL name`
- `PRIMARY KEY (code)`
- `FOREIGN KEY (state_code) REFERENCES app.geo_states(code)`
- `NOT NULL state_code`

Índices:

```sql
CREATE UNIQUE INDEX geo_municipalities_code_state_code_key ON app.geo_municipalities USING btree (code, state_code)
CREATE UNIQUE INDEX geo_municipalities_pkey ON app.geo_municipalities USING btree (code)
CREATE INDEX geo_municipality_parent_idx ON app.geo_municipalities USING btree (state_code)
```

## geo_parishes

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `code` | `text` | No | — |
| `municipality_code` | `text` | No | — |
| `name` | `text` | No | — |
| `kind` | `text` | No | 'parish'::text |
| `latitude` | `double precision` | No | — |
| `longitude` | `double precision` | No | — |
| `boundary` | `geometry(MultiPolygon,4326)` | No | — |

Restricciones:

- `NOT NULL boundary`
- `UNIQUE (code, municipality_code)`
- `NOT NULL code`
- `NOT NULL kind`
- `NOT NULL latitude`
- `NOT NULL longitude`
- `FOREIGN KEY (municipality_code) REFERENCES app.geo_municipalities(code)`
- `NOT NULL municipality_code`
- `NOT NULL name`
- `PRIMARY KEY (code)`

Índices:

```sql
CREATE INDEX geo_parish_boundary_idx ON app.geo_parishes USING gist (boundary)
CREATE INDEX geo_parish_geography_idx ON app.geo_parishes USING gist (((boundary)::geography))
CREATE INDEX geo_parish_parent_idx ON app.geo_parishes USING btree (municipality_code)
CREATE UNIQUE INDEX geo_parishes_code_municipality_code_key ON app.geo_parishes USING btree (code, municipality_code)
CREATE UNIQUE INDEX geo_parishes_pkey ON app.geo_parishes USING btree (code)
```

## geo_states

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `code` | `text` | No | — |
| `country_code` | `text` | No | — |
| `name` | `text` | No | — |
| `kind` | `text` | No | 'state'::text |
| `latitude` | `double precision` | No | — |
| `longitude` | `double precision` | No | — |

Restricciones:

- `NOT NULL code`
- `FOREIGN KEY (country_code) REFERENCES app.geo_countries(code)`
- `NOT NULL country_code`
- `NOT NULL kind`
- `NOT NULL latitude`
- `NOT NULL longitude`
- `NOT NULL name`
- `PRIMARY KEY (code)`

Índices:

```sql
CREATE UNIQUE INDEX geo_states_pkey ON app.geo_states USING btree (code)
```

## http_rate_limits

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `bucket` | `text` | No | — |
| `hits` | `integer` | No | — |
| `expires_at` | `timestamp with time zone` | No | — |

Restricciones:

- `NOT NULL bucket`
- `NOT NULL expires_at`
- `NOT NULL hits`
- `PRIMARY KEY (bucket)`

Índices:

```sql
CREATE UNIQUE INDEX http_rate_limits_pkey ON app.http_rate_limits USING btree (bucket)
```

## inventory_movements

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `bigint` | No | IDENTITY |
| `product_id` | `uuid` | No | — |
| `order_id` | `uuid` | Sí | — |
| `delta` | `integer` | No | — |
| `reason` | `text` | No | — |
| `actor_id` | `uuid` | No | — |
| `created_at` | `timestamp with time zone` | No | now() |

Restricciones:

- `FOREIGN KEY (actor_id) REFERENCES app.users(id)`
- `NOT NULL actor_id`
- `NOT NULL created_at`
- `CHECK ((delta <> 0))`
- `NOT NULL delta`
- `NOT NULL id`
- `FOREIGN KEY (order_id) REFERENCES app.orders(id)`
- `PRIMARY KEY (id)`
- `FOREIGN KEY (product_id) REFERENCES app.products(id)`
- `NOT NULL product_id`
- `NOT NULL reason`

Índices:

```sql
CREATE UNIQUE INDEX inventory_movements_pkey ON app.inventory_movements USING btree (id)
```

## kyc_cases

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `user_id` | `uuid` | No | — |
| `provider` | `text` | No | — |
| `external_reference` | `text` | Sí | — |
| `status` | `text` | No | 'pending'::text |
| `identity_fingerprint` | `text` | Sí | — |
| `document_ciphertext` | `bytea` | Sí | — |
| `encryption_key_id` | `text` | Sí | — |
| `submitted_at` | `timestamp with time zone` | No | now() |
| `reviewed_at` | `timestamp with time zone` | Sí | — |
| `reviewed_by` | `uuid` | Sí | — |
| `expires_at` | `timestamp with time zone` | Sí | — |
| `reason` | `text` | Sí | — |

Restricciones:

- `CHECK (((status <> 'approved'::text) OR ((reviewed_at IS NOT NULL) AND (expires_at > reviewed_at))))`
- `NOT NULL id`
- `PRIMARY KEY (id)`
- `UNIQUE (provider, external_reference)`
- `NOT NULL provider`
- `FOREIGN KEY (reviewed_by) REFERENCES app.users(id)`
- `CHECK ((status = ANY (ARRAY['pending'::text, 'in_review'::text, 'approved'::text, 'rejected'::text, 'expired'::text, 'revoked'::text])))`
- `NOT NULL status`
- `NOT NULL submitted_at`
- `FOREIGN KEY (user_id) REFERENCES app.users(id)`
- `NOT NULL user_id`

Índices:

```sql
CREATE UNIQUE INDEX kyc_cases_pkey ON app.kyc_cases USING btree (id)
CREATE UNIQUE INDEX kyc_cases_provider_external_reference_key ON app.kyc_cases USING btree (provider, external_reference)
CREATE UNIQUE INDEX kyc_one_approved_idx ON app.kyc_cases USING btree (user_id) WHERE (status = 'approved'::text)
CREATE INDEX kyc_user_idx ON app.kyc_cases USING btree (user_id, status, expires_at)
```

## kyc_documents

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `case_id` | `uuid` | No | — |
| `file_id` | `uuid` | No | — |
| `kind` | `text` | No | — |

Restricciones:

- `FOREIGN KEY (case_id) REFERENCES app.kyc_cases(id)`
- `NOT NULL case_id`
- `FOREIGN KEY (file_id) REFERENCES app.files(id)`
- `NOT NULL file_id`
- `NOT NULL kind`
- `PRIMARY KEY (case_id, file_id)`

Índices:

```sql
CREATE UNIQUE INDEX kyc_documents_pkey ON app.kyc_documents USING btree (case_id, file_id)
```

## legal_documents

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `kind` | `text` | No | — |
| `version` | `text` | No | — |
| `content_hash` | `text` | No | — |
| `object_key` | `text` | No | — |
| `effective_at` | `timestamp with time zone` | No | — |

Restricciones:

- `NOT NULL content_hash`
- `NOT NULL effective_at`
- `NOT NULL id`
- `NOT NULL kind`
- `UNIQUE (kind, version)`
- `NOT NULL object_key`
- `PRIMARY KEY (id)`
- `NOT NULL version`

Índices:

```sql
CREATE UNIQUE INDEX legal_documents_kind_version_key ON app.legal_documents USING btree (kind, version)
CREATE UNIQUE INDEX legal_documents_pkey ON app.legal_documents USING btree (id)
```

## location_shares

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `contract_id` | `uuid` | No | — |
| `customer_id` | `uuid` | No | — |
| `purpose` | `text` | No | 'service_navigation'::text |
| `shared_at` | `timestamp with time zone` | No | now() |
| `expires_at` | `timestamp with time zone` | No | — |
| `revoked_at` | `timestamp with time zone` | Sí | — |

Restricciones:

- `FOREIGN KEY (contract_id, customer_id) REFERENCES app.contracts(id, customer_id)`
- `CHECK ((expires_at > shared_at))`
- `FOREIGN KEY (contract_id) REFERENCES app.contracts(id)`
- `NOT NULL contract_id`
- `FOREIGN KEY (customer_id) REFERENCES app.users(id)`
- `NOT NULL customer_id`
- `NOT NULL expires_at`
- `NOT NULL id`
- `PRIMARY KEY (id)`
- `NOT NULL purpose`
- `NOT NULL shared_at`

Índices:

```sql
CREATE INDEX location_shares_contract_idx ON app.location_shares USING btree (contract_id, expires_at) WHERE (revoked_at IS NULL)
CREATE UNIQUE INDEX location_shares_pkey ON app.location_shares USING btree (id)
```

## message_receipts

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `message_id` | `uuid` | No | — |
| `user_id` | `uuid` | No | — |
| `delivered_at` | `timestamp with time zone` | Sí | — |
| `read_at` | `timestamp with time zone` | Sí | — |

Restricciones:

- `FOREIGN KEY (message_id) REFERENCES app.messages(id)`
- `NOT NULL message_id`
- `PRIMARY KEY (message_id, user_id)`
- `FOREIGN KEY (user_id) REFERENCES app.users(id)`
- `NOT NULL user_id`

Índices:

```sql
CREATE UNIQUE INDEX message_receipts_pkey ON app.message_receipts USING btree (message_id, user_id)
```

## messages

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `conversation_id` | `uuid` | No | — |
| `sender_id` | `uuid` | No | — |
| `client_message_id` | `uuid` | No | — |
| `kind` | `text` | No | 'text'::text |
| `body` | `text` | Sí | — |
| `file_id` | `uuid` | Sí | — |
| `created_at` | `timestamp with time zone` | No | now() |
| `redacted_at` | `timestamp with time zone` | Sí | — |

Restricciones:

- `CHECK (((length(body) >= 1) AND (length(body) <= 10000)))`
- `CHECK (((body IS NOT NULL) OR (file_id IS NOT NULL)))`
- `NOT NULL client_message_id`
- `FOREIGN KEY (conversation_id) REFERENCES app.conversations(id)`
- `NOT NULL conversation_id`
- `UNIQUE (conversation_id, sender_id, client_message_id)`
- `NOT NULL created_at`
- `FOREIGN KEY (file_id) REFERENCES app.files(id)`
- `NOT NULL id`
- `CHECK ((kind = ANY (ARRAY['text'::text, 'attachment'::text, 'location_shared'::text, 'system'::text])))`
- `NOT NULL kind`
- `PRIMARY KEY (id)`
- `FOREIGN KEY (sender_id) REFERENCES app.users(id)`
- `NOT NULL sender_id`

Índices:

```sql
CREATE UNIQUE INDEX messages_conversation_id_sender_id_client_message_id_key ON app.messages USING btree (conversation_id, sender_id, client_message_id)
CREATE UNIQUE INDEX messages_pkey ON app.messages USING btree (id)
CREATE INDEX messages_timeline_idx ON app.messages USING btree (conversation_id, created_at, id)
```

## moderation_actions

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `actor_id` | `uuid` | No | — |
| `target_type` | `text` | No | — |
| `target_id` | `uuid` | No | — |
| `action` | `text` | No | — |
| `reason` | `text` | No | — |
| `ticket_id` | `uuid` | Sí | — |
| `created_at` | `timestamp with time zone` | No | now() |

Restricciones:

- `NOT NULL action`
- `FOREIGN KEY (actor_id) REFERENCES app.users(id)`
- `NOT NULL actor_id`
- `NOT NULL created_at`
- `NOT NULL id`
- `PRIMARY KEY (id)`
- `NOT NULL reason`
- `NOT NULL target_id`
- `NOT NULL target_type`
- `FOREIGN KEY (ticket_id) REFERENCES app.support_tickets(id)`

Índices:

```sql
CREATE UNIQUE INDEX moderation_actions_pkey ON app.moderation_actions USING btree (id)
```

## money_accounts

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `currency` | `character(3)` | No | — |
| `kind` | `text` | No | — |
| `wallet_id` | `uuid` | Sí | — |
| `contract_id` | `uuid` | Sí | — |
| `withdrawal_id` | `uuid` | Sí | — |
| `balance_minor` | `bigint` | No | 0 |

Restricciones:

- `NOT NULL balance_minor`
- `CHECK (((kind = 'cash_clearing'::text) OR (balance_minor >= 0)))`
- `CHECK ((((kind = 'available'::text) AND (wallet_id IS NOT NULL) AND (contract_id IS NULL) AND (withdrawal_id IS NULL)) OR ((kind = 'contract_hold'::text) AND (wallet_id IS NOT NULL) AND (contract_id IS NOT NULL) AND (withdrawal_id IS NULL)) OR ((kind = 'withdrawal_hold'::text) AND (wallet_id IS NOT NULL) AND (contract_id IS NULL) AND (withdrawal_id IS NOT NULL)) OR ((kind = ANY (ARRAY['cash_clearing'::text, 'platform_fees'::text])) AND (num_nonnulls(wallet_id, contract_id, withdrawal_id) = 0))))`
- `FOREIGN KEY (contract_id) REFERENCES app.contracts(id)`
- `CHECK ((currency ~ '^[A-Z]{3}$'::text))`
- `NOT NULL currency`
- `NOT NULL id`
- `CHECK ((kind = ANY (ARRAY['available'::text, 'contract_hold'::text, 'withdrawal_hold'::text, 'cash_clearing'::text, 'platform_fees'::text])))`
- `UNIQUE NULLS NOT DISTINCT (kind, currency, wallet_id, contract_id, withdrawal_id)`
- `NOT NULL kind`
- `PRIMARY KEY (id)`
- `FOREIGN KEY (wallet_id, currency) REFERENCES app.money_wallets(id, currency)`
- `FOREIGN KEY (withdrawal_id) REFERENCES app.money_withdrawals(id)`

Índices:

```sql
CREATE UNIQUE INDEX money_accounts_kind_currency_wallet_id_contract_id_withdraw_key ON app.money_accounts USING btree (kind, currency, wallet_id, contract_id, withdrawal_id) NULLS NOT DISTINCT
CREATE UNIQUE INDEX money_accounts_pkey ON app.money_accounts USING btree (id)
CREATE INDEX money_accounts_wallet_idx ON app.money_accounts USING btree (wallet_id, kind)
```

## money_entries

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `bigint` | No | IDENTITY |
| `transaction_id` | `uuid` | No | — |
| `account_id` | `uuid` | No | — |
| `delta_minor` | `bigint` | No | — |
| `balance_after_minor` | `bigint` | No | — |
| `created_at` | `timestamp with time zone` | No | now() |

Restricciones:

- `TRIGGER DEFERRABLE INITIALLY DEFERRED`
- `FOREIGN KEY (account_id) REFERENCES app.money_accounts(id)`
- `NOT NULL account_id`
- `NOT NULL balance_after_minor`
- `NOT NULL created_at`
- `CHECK ((delta_minor <> 0))`
- `NOT NULL delta_minor`
- `NOT NULL id`
- `PRIMARY KEY (id)`
- `UNIQUE (transaction_id, account_id)`
- `FOREIGN KEY (transaction_id) REFERENCES app.money_transactions(id)`
- `NOT NULL transaction_id`

Índices:

```sql
CREATE INDEX money_entries_account_idx ON app.money_entries USING btree (account_id, id DESC)
CREATE UNIQUE INDEX money_entries_pkey ON app.money_entries USING btree (id)
CREATE UNIQUE INDEX money_entries_transaction_id_account_id_key ON app.money_entries USING btree (transaction_id, account_id)
```

## money_incidents

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `wallet_id` | `uuid` | No | — |
| `provider` | `text` | No | — |
| `event_id` | `text` | No | — |
| `reason` | `text` | No | — |
| `status` | `text` | No | 'open'::text |
| `created_at` | `timestamp with time zone` | No | now() |

Restricciones:

- `NOT NULL created_at`
- `NOT NULL event_id`
- `NOT NULL id`
- `PRIMARY KEY (id)`
- `UNIQUE (provider, event_id)`
- `NOT NULL provider`
- `NOT NULL reason`
- `CHECK ((status = ANY (ARRAY['open'::text, 'resolved'::text])))`
- `NOT NULL status`
- `FOREIGN KEY (wallet_id) REFERENCES app.money_wallets(id)`
- `NOT NULL wallet_id`

Índices:

```sql
CREATE UNIQUE INDEX money_incidents_pkey ON app.money_incidents USING btree (id)
CREATE UNIQUE INDEX money_incidents_provider_event_id_key ON app.money_incidents USING btree (provider, event_id)
```

## money_provider_events

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `provider` | `text` | No | — |
| `event_id` | `text` | No | — |
| `payment_id` | `text` | No | — |
| `kind` | `text` | No | — |
| `reference_id` | `uuid` | No | — |
| `amount_minor` | `bigint` | No | — |
| `currency` | `character(3)` | No | — |
| `payload_hash` | `text` | No | — |
| `created_at` | `timestamp with time zone` | No | now() |

Restricciones:

- `CHECK ((amount_minor > 0))`
- `NOT NULL amount_minor`
- `NOT NULL created_at`
- `NOT NULL currency`
- `NOT NULL event_id`
- `NOT NULL id`
- `CHECK ((kind = ANY (ARRAY['topup_settled'::text, 'withdrawal_paid'::text, 'withdrawal_failed'::text])))`
- `NOT NULL kind`
- `CHECK ((payload_hash ~ '^[a-f0-9]{64}$'::text))`
- `NOT NULL payload_hash`
- `NOT NULL payment_id`
- `PRIMARY KEY (id)`
- `UNIQUE (provider, event_id)`
- `NOT NULL provider`
- `NOT NULL reference_id`

Índices:

```sql
CREATE UNIQUE INDEX money_provider_events_pkey ON app.money_provider_events USING btree (id)
CREATE UNIQUE INDEX money_provider_events_provider_event_id_key ON app.money_provider_events USING btree (provider, event_id)
```

## money_topups

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `wallet_id` | `uuid` | No | — |
| `amount_minor` | `bigint` | No | — |
| `fee_minor` | `bigint` | No | 0 |
| `status` | `text` | No | 'pending'::text |
| `idempotency_key` | `uuid` | No | — |
| `created_at` | `timestamp with time zone` | No | now() |
| `settled_at` | `timestamp with time zone` | Sí | — |

Restricciones:

- `CHECK (((amount_minor >= 1) AND (amount_minor <= '1000000000000'::bigint)))`
- `NOT NULL amount_minor`
- `NOT NULL created_at`
- `CHECK ((fee_minor = 0))`
- `NOT NULL fee_minor`
- `NOT NULL id`
- `NOT NULL idempotency_key`
- `PRIMARY KEY (id)`
- `CHECK ((status = ANY (ARRAY['pending'::text, 'settled'::text, 'review'::text])))`
- `NOT NULL status`
- `FOREIGN KEY (wallet_id) REFERENCES app.money_wallets(id)`
- `UNIQUE (wallet_id, idempotency_key)`
- `NOT NULL wallet_id`

Índices:

```sql
CREATE UNIQUE INDEX money_topups_pkey ON app.money_topups USING btree (id)
CREATE UNIQUE INDEX money_topups_wallet_id_idempotency_key_key ON app.money_topups USING btree (wallet_id, idempotency_key)
```

## money_transactions

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `kind` | `text` | No | — |
| `currency` | `character(3)` | No | — |
| `idempotency_key` | `text` | No | — |
| `reference_id` | `uuid` | No | — |
| `entries_fingerprint` | `jsonb` | No | — |
| `actor_id` | `uuid` | Sí | — |
| `metadata` | `jsonb` | No | '{}'::jsonb |
| `created_at` | `timestamp with time zone` | No | now() |

Restricciones:

- `TRIGGER DEFERRABLE INITIALLY DEFERRED`
- `FOREIGN KEY (actor_id) REFERENCES app.users(id)`
- `NOT NULL created_at`
- `NOT NULL currency`
- `NOT NULL entries_fingerprint`
- `UNIQUE (id, currency)`
- `NOT NULL id`
- `UNIQUE (idempotency_key)`
- `NOT NULL idempotency_key`
- `NOT NULL kind`
- `NOT NULL metadata`
- `PRIMARY KEY (id)`
- `NOT NULL reference_id`

Índices:

```sql
CREATE UNIQUE INDEX money_transactions_id_currency_key ON app.money_transactions USING btree (id, currency)
CREATE UNIQUE INDEX money_transactions_idempotency_key_key ON app.money_transactions USING btree (idempotency_key)
CREATE UNIQUE INDEX money_transactions_pkey ON app.money_transactions USING btree (id)
```

## money_wallets

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `user_id` | `uuid` | No | — |
| `currency` | `character(3)` | No | — |
| `status` | `text` | No | 'active'::text |
| `created_at` | `timestamp with time zone` | No | now() |

Restricciones:

- `NOT NULL created_at`
- `CHECK ((currency ~ '^[A-Z]{3}$'::text))`
- `NOT NULL currency`
- `UNIQUE (id, currency)`
- `NOT NULL id`
- `PRIMARY KEY (id)`
- `CHECK ((status = ANY (ARRAY['active'::text, 'frozen'::text])))`
- `NOT NULL status`
- `UNIQUE (user_id, currency)`
- `FOREIGN KEY (user_id) REFERENCES app.users(id)`
- `NOT NULL user_id`

Índices:

```sql
CREATE UNIQUE INDEX money_wallets_id_currency_key ON app.money_wallets USING btree (id, currency)
CREATE UNIQUE INDEX money_wallets_pkey ON app.money_wallets USING btree (id)
CREATE UNIQUE INDEX money_wallets_user_id_currency_key ON app.money_wallets USING btree (user_id, currency)
```

## money_withdrawals

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `wallet_id` | `uuid` | No | — |
| `payout_method_id` | `uuid` | No | — |
| `quote_id` | `uuid` | No | — |
| `amount_minor` | `bigint` | No | — |
| `fee_minor` | `bigint` | No | — |
| `net_minor` | `bigint` | No | — |
| `destination_provider` | `text` | No | — |
| `destination_token` | `text` | No | — |
| `destination_label` | `text` | No | — |
| `status` | `text` | No | 'pending'::text |
| `idempotency_key` | `uuid` | No | — |
| `created_at` | `timestamp with time zone` | No | now() |
| `dispatched_at` | `timestamp with time zone` | Sí | — |
| `completed_at` | `timestamp with time zone` | Sí | — |

Restricciones:

- `CHECK (((amount_minor >= 1) AND (amount_minor <= '1000000000000'::bigint)))`
- `NOT NULL amount_minor`
- `CHECK ((amount_minor = (fee_minor + net_minor)))`
- `NOT NULL created_at`
- `NOT NULL destination_label`
- `NOT NULL destination_provider`
- `NOT NULL destination_token`
- `CHECK ((fee_minor >= 0))`
- `NOT NULL fee_minor`
- `NOT NULL id`
- `NOT NULL idempotency_key`
- `CHECK ((net_minor > 0))`
- `NOT NULL net_minor`
- `FOREIGN KEY (payout_method_id) REFERENCES app.payout_methods(id)`
- `NOT NULL payout_method_id`
- `PRIMARY KEY (id)`
- `FOREIGN KEY (quote_id) REFERENCES app.withdrawal_quotes(id)`
- `UNIQUE (quote_id)`
- `NOT NULL quote_id`
- `CHECK ((status = ANY (ARRAY['pending'::text, 'processing'::text, 'paid'::text, 'failed'::text, 'cancelled'::text, 'review'::text])))`
- `NOT NULL status`
- `FOREIGN KEY (wallet_id) REFERENCES app.money_wallets(id)`
- `UNIQUE (wallet_id, idempotency_key)`
- `NOT NULL wallet_id`

Índices:

```sql
CREATE UNIQUE INDEX money_withdrawals_pkey ON app.money_withdrawals USING btree (id)
CREATE UNIQUE INDEX money_withdrawals_quote_id_key ON app.money_withdrawals USING btree (quote_id)
CREATE UNIQUE INDEX money_withdrawals_wallet_id_idempotency_key_key ON app.money_withdrawals USING btree (wallet_id, idempotency_key)
```

## notification_deliveries

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `outbox_id` | `uuid` | No | — |
| `channel` | `text` | No | — |
| `provider_message_id` | `text` | Sí | — |
| `attempt` | `integer` | No | — |
| `status` | `text` | No | — |
| `created_at` | `timestamp with time zone` | No | now() |

Restricciones:

- `CHECK ((attempt > 0))`
- `NOT NULL attempt`
- `CHECK ((channel = ANY (ARRAY['push'::text, 'email'::text, 'in_app'::text, 'sms'::text])))`
- `NOT NULL channel`
- `NOT NULL created_at`
- `NOT NULL id`
- `UNIQUE (outbox_id, channel, attempt)`
- `FOREIGN KEY (outbox_id) REFERENCES app.outbox(id)`
- `NOT NULL outbox_id`
- `PRIMARY KEY (id)`
- `NOT NULL status`

Índices:

```sql
CREATE UNIQUE INDEX notification_deliveries_outbox_id_channel_attempt_key ON app.notification_deliveries USING btree (outbox_id, channel, attempt)
CREATE UNIQUE INDEX notification_deliveries_pkey ON app.notification_deliveries USING btree (id)
```

## notification_preferences

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `user_id` | `uuid` | No | — |
| `topic` | `text` | No | — |
| `channel` | `text` | No | — |
| `enabled` | `boolean` | No | true |

Restricciones:

- `NOT NULL channel`
- `NOT NULL enabled`
- `PRIMARY KEY (user_id, topic, channel)`
- `NOT NULL topic`
- `FOREIGN KEY (user_id) REFERENCES app.users(id)`
- `NOT NULL user_id`

Índices:

```sql
CREATE UNIQUE INDEX notification_preferences_pkey ON app.notification_preferences USING btree (user_id, topic, channel)
```

## notifications

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `user_id` | `uuid` | No | — |
| `outbox_id` | `uuid` | Sí | — |
| `topic` | `text` | No | — |
| `aggregate_id` | `uuid` | No | — |
| `read_at` | `timestamp with time zone` | Sí | — |
| `created_at` | `timestamp with time zone` | No | now() |

Restricciones:

- `NOT NULL aggregate_id`
- `NOT NULL created_at`
- `NOT NULL id`
- `FOREIGN KEY (outbox_id) REFERENCES app.outbox(id)`
- `UNIQUE (outbox_id)`
- `PRIMARY KEY (id)`
- `NOT NULL topic`
- `FOREIGN KEY (user_id) REFERENCES app.users(id)`
- `NOT NULL user_id`

Índices:

```sql
CREATE UNIQUE INDEX notifications_outbox_id_key ON app.notifications USING btree (outbox_id)
CREATE UNIQUE INDEX notifications_pkey ON app.notifications USING btree (id)
CREATE INDEX notifications_user_idx ON app.notifications USING btree (user_id, created_at DESC)
```

## offerings

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `professional_id` | `uuid` | No | — |
| `category_id` | `uuid` | No | — |
| `title` | `text` | No | — |
| `description` | `text` | No | ''::text |
| `pricing_type` | `text` | No | 'quote'::text |
| `price_min_minor` | `bigint` | Sí | — |
| `price_max_minor` | `bigint` | Sí | — |
| `currency` | `character(3)` | No | — |
| `active` | `boolean` | No | true |
| `created_at` | `timestamp with time zone` | No | now() |
| `delivery_mode` | `text` | No | 'onsite'::text |

Restricciones:

- `NOT NULL active`
- `FOREIGN KEY (category_id) REFERENCES app.categories(id)`
- `NOT NULL category_id`
- `CHECK ((price_max_minor >= price_min_minor))`
- `NOT NULL created_at`
- `CHECK ((currency ~ '^[A-Z]{3}$'::text))`
- `NOT NULL currency`
- `CHECK ((delivery_mode = ANY (ARRAY['onsite'::text, 'remote'::text])))`
- `NOT NULL delivery_mode`
- `NOT NULL description`
- `NOT NULL id`
- `UNIQUE (id, professional_id, category_id)`
- `PRIMARY KEY (id)`
- `CHECK ((price_min_minor >= 0))`
- `CHECK ((pricing_type = ANY (ARRAY['quote'::text, 'fixed'::text, 'hourly'::text, 'from'::text])))`
- `NOT NULL pricing_type`
- `FOREIGN KEY (professional_id) REFERENCES app.professionals(user_id)`
- `NOT NULL professional_id`
- `NOT NULL title`

Índices:

```sql
CREATE INDEX offerings_category_idx ON app.offerings USING btree (category_id, professional_id) WHERE active
CREATE UNIQUE INDEX offerings_id_professional_id_category_id_key ON app.offerings USING btree (id, professional_id, category_id)
CREATE UNIQUE INDEX offerings_pkey ON app.offerings USING btree (id)
```

## operating_currencies

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `code` | `text` | No | — |
| `label` | `text` | No | — |
| `symbol` | `text` | No | — |

Restricciones:

- `NOT NULL code`
- `NOT NULL label`
- `PRIMARY KEY (code)`
- `NOT NULL symbol`

Índices:

```sql
CREATE UNIQUE INDEX operating_currencies_pkey ON app.operating_currencies USING btree (code)
```

## order_delivery

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `order_id` | `uuid` | No | — |
| `recipient_name` | `text` | No | — |
| `phone` | `text` | Sí | — |
| `address_ciphertext` | `bytea` | No | — |
| `encryption_key_id` | `text` | No | — |
| `buyer_shared_at` | `timestamp with time zone` | No | now() |

Restricciones:

- `NOT NULL address_ciphertext`
- `NOT NULL buyer_shared_at`
- `NOT NULL encryption_key_id`
- `FOREIGN KEY (order_id) REFERENCES app.orders(id)`
- `NOT NULL order_id`
- `PRIMARY KEY (order_id)`
- `NOT NULL recipient_name`

Índices:

```sql
CREATE UNIQUE INDEX order_delivery_pkey ON app.order_delivery USING btree (order_id)
```

## order_events

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `bigint` | No | IDENTITY |
| `order_id` | `uuid` | No | — |
| `actor_id` | `uuid` | No | — |
| `event_type` | `text` | No | — |
| `metadata` | `jsonb` | No | '{}'::jsonb |
| `created_at` | `timestamp with time zone` | No | now() |

Restricciones:

- `FOREIGN KEY (actor_id) REFERENCES app.users(id)`
- `NOT NULL actor_id`
- `NOT NULL created_at`
- `NOT NULL event_type`
- `NOT NULL id`
- `NOT NULL metadata`
- `FOREIGN KEY (order_id) REFERENCES app.orders(id)`
- `NOT NULL order_id`
- `PRIMARY KEY (id)`

Índices:

```sql
CREATE UNIQUE INDEX order_events_pkey ON app.order_events USING btree (id)
```

## order_items

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `order_id` | `uuid` | No | — |
| `product_id` | `uuid` | No | — |
| `sku_snapshot` | `text` | No | — |
| `name_snapshot` | `text` | No | — |
| `quantity` | `integer` | No | — |
| `unit_price_minor` | `bigint` | No | — |
| `subtotal_minor` | `bigint` | Sí | GENERATED: ((quantity)::bigint * unit_price_minor) |

Restricciones:

- `NOT NULL id`
- `NOT NULL name_snapshot`
- `FOREIGN KEY (order_id) REFERENCES app.orders(id)`
- `NOT NULL order_id`
- `UNIQUE (order_id, product_id)`
- `PRIMARY KEY (id)`
- `FOREIGN KEY (product_id) REFERENCES app.products(id)`
- `NOT NULL product_id`
- `CHECK ((quantity > 0))`
- `NOT NULL quantity`
- `NOT NULL sku_snapshot`
- `CHECK ((unit_price_minor >= 0))`
- `NOT NULL unit_price_minor`

Índices:

```sql
CREATE UNIQUE INDEX order_items_order_id_product_id_key ON app.order_items USING btree (order_id, product_id)
CREATE UNIQUE INDEX order_items_pkey ON app.order_items USING btree (id)
```

## orders

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `customer_id` | `uuid` | No | — |
| `store_id` | `uuid` | No | — |
| `status` | `text` | No | 'pending'::text |
| `total_minor` | `bigint` | No | — |
| `currency` | `character(3)` | No | — |
| `payment_method` | `text` | Sí | — |
| `payment_status` | `text` | No | 'not_reported'::text |
| `platform_fee_minor` | `bigint` | No | 0 |
| `idempotency_key` | `uuid` | No | — |
| `created_at` | `timestamp with time zone` | No | now() |

Restricciones:

- `NOT NULL created_at`
- `NOT NULL currency`
- `FOREIGN KEY (customer_id) REFERENCES app.users(id)`
- `UNIQUE (customer_id, idempotency_key)`
- `NOT NULL customer_id`
- `NOT NULL id`
- `NOT NULL idempotency_key`
- `CHECK ((payment_status = ANY (ARRAY['not_reported'::text, 'buyer_reported'::text, 'seller_confirmed'::text, 'disputed'::text])))`
- `NOT NULL payment_status`
- `PRIMARY KEY (id)`
- `CHECK ((platform_fee_minor = 0))`
- `NOT NULL platform_fee_minor`
- `CHECK ((status = ANY (ARRAY['pending'::text, 'accepted'::text, 'dispatched'::text, 'delivered'::text, 'cancelled'::text, 'disputed'::text])))`
- `NOT NULL status`
- `FOREIGN KEY (store_id) REFERENCES app.stores(id)`
- `NOT NULL store_id`
- `CHECK ((total_minor >= 0))`
- `NOT NULL total_minor`

Índices:

```sql
CREATE UNIQUE INDEX orders_customer_id_idempotency_key_key ON app.orders USING btree (customer_id, idempotency_key)
CREATE INDEX orders_customer_idx ON app.orders USING btree (customer_id, created_at DESC)
CREATE UNIQUE INDEX orders_pkey ON app.orders USING btree (id)
CREATE INDEX orders_store_idx ON app.orders USING btree (store_id, status)
```

## outbox

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `topic` | `text` | No | — |
| `aggregate_id` | `uuid` | No | — |
| `recipient_id` | `uuid` | Sí | — |
| `payload` | `jsonb` | No | '{}'::jsonb |
| `deduplication_key` | `text` | No | — |
| `status` | `text` | No | 'pending'::text |
| `attempts` | `integer` | No | 0 |
| `available_at` | `timestamp with time zone` | No | now() |
| `locked_until` | `timestamp with time zone` | Sí | — |
| `last_error` | `text` | Sí | — |
| `created_at` | `timestamp with time zone` | No | now() |
| `delivered_at` | `timestamp with time zone` | Sí | — |

Restricciones:

- `NOT NULL aggregate_id`
- `CHECK ((attempts >= 0))`
- `NOT NULL attempts`
- `NOT NULL available_at`
- `NOT NULL created_at`
- `UNIQUE (deduplication_key)`
- `NOT NULL deduplication_key`
- `NOT NULL id`
- `NOT NULL payload`
- `PRIMARY KEY (id)`
- `FOREIGN KEY (recipient_id) REFERENCES app.users(id)`
- `CHECK ((status = ANY (ARRAY['pending'::text, 'processing'::text, 'delivered'::text, 'failed'::text])))`
- `NOT NULL status`
- `NOT NULL topic`

Índices:

```sql
CREATE UNIQUE INDEX outbox_deduplication_key_key ON app.outbox USING btree (deduplication_key)
CREATE INDEX outbox_pending_idx ON app.outbox USING btree (available_at) WHERE (status = ANY (ARRAY['pending'::text, 'processing'::text]))
CREATE UNIQUE INDEX outbox_pkey ON app.outbox USING btree (id)
```

## payment_events

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `provider` | `text` | No | — |
| `external_event_id` | `text` | No | — |
| `external_payment_id` | `text` | No | — |
| `purchase_id` | `uuid` | No | — |
| `event_type` | `text` | No | — |
| `amount_minor` | `bigint` | No | — |
| `currency` | `character(3)` | No | — |
| `payload_hash` | `text` | No | — |
| `received_at` | `timestamp with time zone` | No | now() |

Restricciones:

- `CHECK ((amount_minor > 0))`
- `NOT NULL amount_minor`
- `NOT NULL currency`
- `NOT NULL event_type`
- `NOT NULL external_event_id`
- `NOT NULL external_payment_id`
- `NOT NULL id`
- `NOT NULL payload_hash`
- `PRIMARY KEY (id)`
- `UNIQUE (provider, external_event_id)`
- `NOT NULL provider`
- `FOREIGN KEY (purchase_id) REFERENCES app.credit_purchases(id)`
- `NOT NULL purchase_id`
- `NOT NULL received_at`

Índices:

```sql
CREATE UNIQUE INDEX payment_capture_once_idx ON app.payment_events USING btree (provider, external_payment_id) WHERE (event_type = 'captured'::text)
CREATE UNIQUE INDEX payment_events_pkey ON app.payment_events USING btree (id)
CREATE UNIQUE INDEX payment_events_provider_external_event_id_key ON app.payment_events USING btree (provider, external_event_id)
```

## payout_methods

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `user_id` | `uuid` | No | — |
| `provider` | `text` | No | — |
| `provider_token` | `text` | No | — |
| `label` | `text` | No | — |
| `masked_account` | `text` | No | — |
| `currency` | `character(3)` | No | — |
| `status` | `text` | No | 'pending'::text |
| `verified_at` | `timestamp with time zone` | Sí | — |
| `created_at` | `timestamp with time zone` | No | now() |

Restricciones:

- `CHECK (((status <> 'verified'::text) OR (verified_at IS NOT NULL)))`
- `NOT NULL created_at`
- `CHECK ((currency ~ '^[A-Z]{3}$'::text))`
- `NOT NULL currency`
- `NOT NULL id`
- `NOT NULL label`
- `NOT NULL masked_account`
- `PRIMARY KEY (id)`
- `NOT NULL provider`
- `UNIQUE (provider, provider_token)`
- `NOT NULL provider_token`
- `CHECK ((status = ANY (ARRAY['pending'::text, 'verified'::text, 'disabled'::text])))`
- `NOT NULL status`
- `FOREIGN KEY (user_id) REFERENCES app.users(id)`
- `NOT NULL user_id`

Índices:

```sql
CREATE UNIQUE INDEX payout_methods_pkey ON app.payout_methods USING btree (id)
CREATE UNIQUE INDEX payout_methods_provider_provider_token_key ON app.payout_methods USING btree (provider, provider_token)
```

## permissions

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `code` | `text` | No | — |
| `description` | `text` | No | — |

Restricciones:

- `NOT NULL code`
- `NOT NULL description`
- `PRIMARY KEY (code)`

Índices:

```sql
CREATE UNIQUE INDEX permissions_pkey ON app.permissions USING btree (code)
```

## plans

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `code` | `text` | No | — |
| `name` | `text` | No | — |
| `credits` | `integer` | No | — |
| `price_minor` | `bigint` | No | — |
| `currency` | `character(3)` | No | — |
| `positioning_weight` | `integer` | No | 0 |
| `positioning_days` | `integer` | No | 0 |
| `marketplace_days` | `integer` | No | 0 |
| `active` | `boolean` | No | false |
| `created_at` | `timestamp with time zone` | No | now() |

Restricciones:

- `NOT NULL active`
- `UNIQUE (code)`
- `NOT NULL code`
- `NOT NULL created_at`
- `CHECK ((credits >= 0))`
- `NOT NULL credits`
- `CHECK ((currency ~ '^[A-Z]{3}$'::text))`
- `NOT NULL currency`
- `NOT NULL id`
- `CHECK ((marketplace_days >= 0))`
- `NOT NULL marketplace_days`
- `NOT NULL name`
- `PRIMARY KEY (id)`
- `CHECK ((positioning_days >= 0))`
- `NOT NULL positioning_days`
- `CHECK (((positioning_weight >= 0) AND (positioning_weight <= 100)))`
- `NOT NULL positioning_weight`
- `CHECK ((price_minor > 0))`
- `NOT NULL price_minor`

Índices:

```sql
CREATE UNIQUE INDEX plans_code_key ON app.plans USING btree (code)
CREATE UNIQUE INDEX plans_pkey ON app.plans USING btree (id)
```

## portfolio_items

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `professional_id` | `uuid` | No | — |
| `title` | `text` | No | — |
| `description` | `text` | Sí | — |
| `file_id` | `uuid` | No | — |
| `created_at` | `timestamp with time zone` | No | now() |

Restricciones:

- `NOT NULL created_at`
- `FOREIGN KEY (file_id) REFERENCES app.files(id)`
- `NOT NULL file_id`
- `NOT NULL id`
- `PRIMARY KEY (id)`
- `FOREIGN KEY (professional_id) REFERENCES app.professionals(user_id)`
- `NOT NULL professional_id`
- `NOT NULL title`

Índices:

```sql
CREATE UNIQUE INDEX portfolio_items_pkey ON app.portfolio_items USING btree (id)
```

## product_images

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `product_id` | `uuid` | No | — |
| `file_id` | `uuid` | No | — |
| `sort_order` | `integer` | No | 0 |

Restricciones:

- `FOREIGN KEY (file_id) REFERENCES app.files(id)`
- `NOT NULL file_id`
- `PRIMARY KEY (product_id, file_id)`
- `FOREIGN KEY (product_id) REFERENCES app.products(id)`
- `NOT NULL product_id`
- `NOT NULL sort_order`

Índices:

```sql
CREATE UNIQUE INDEX product_images_pkey ON app.product_images USING btree (product_id, file_id)
```

## products

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `store_id` | `uuid` | No | — |
| `category_id` | `uuid` | Sí | — |
| `sku` | `text` | No | — |
| `name` | `text` | No | — |
| `description` | `text` | No | — |
| `price_minor` | `bigint` | No | — |
| `currency` | `character(3)` | No | — |
| `stock` | `integer` | No | 0 |
| `status` | `text` | No | 'draft'::text |
| `created_at` | `timestamp with time zone` | No | now() |
| `updated_at` | `timestamp with time zone` | No | now() |

Restricciones:

- `FOREIGN KEY (category_id) REFERENCES app.categories(id)`
- `NOT NULL created_at`
- `CHECK ((currency ~ '^[A-Z]{3}$'::text))`
- `NOT NULL currency`
- `NOT NULL description`
- `NOT NULL id`
- `NOT NULL name`
- `PRIMARY KEY (id)`
- `CHECK ((price_minor >= 0))`
- `NOT NULL price_minor`
- `NOT NULL sku`
- `CHECK ((status = ANY (ARRAY['draft'::text, 'active'::text, 'archived'::text])))`
- `NOT NULL status`
- `CHECK ((stock >= 0))`
- `NOT NULL stock`
- `FOREIGN KEY (store_id) REFERENCES app.stores(id)`
- `NOT NULL store_id`
- `UNIQUE (store_id, sku)`
- `NOT NULL updated_at`

Índices:

```sql
CREATE UNIQUE INDEX products_pkey ON app.products USING btree (id)
CREATE UNIQUE INDEX products_store_id_sku_key ON app.products USING btree (store_id, sku)
```

## professional_contacts

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `professional_id` | `uuid` | No | — |
| `branch_id` | `uuid` | Sí | — |
| `kind` | `text` | No | — |
| `label` | `text` | No | — |
| `value` | `text` | No | — |
| `verified_at` | `timestamp with time zone` | Sí | — |

Restricciones:

- `FOREIGN KEY (branch_id, professional_id) REFERENCES app.branches(id, professional_id)`
- `NOT NULL id`
- `CHECK ((kind = ANY (ARRAY['phone'::text, 'whatsapp'::text, 'email'::text, 'website'::text])))`
- `NOT NULL kind`
- `NOT NULL label`
- `PRIMARY KEY (id)`
- `FOREIGN KEY (professional_id) REFERENCES app.professionals(user_id)`
- `NOT NULL professional_id`
- `NOT NULL value`

Índices:

```sql
CREATE UNIQUE INDEX professional_contacts_pkey ON app.professional_contacts USING btree (id)
```

## professional_credentials

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `professional_id` | `uuid` | No | — |
| `title` | `text` | No | — |
| `issuer` | `text` | No | — |
| `reference_ciphertext` | `bytea` | Sí | — |
| `evidence_file_id` | `uuid` | Sí | — |
| `issued_on` | `date` | Sí | — |
| `expires_on` | `date` | Sí | — |
| `verified_at` | `timestamp with time zone` | Sí | — |
| `verified_by` | `uuid` | Sí | — |

Restricciones:

- `FOREIGN KEY (evidence_file_id) REFERENCES app.files(id)`
- `NOT NULL id`
- `NOT NULL issuer`
- `PRIMARY KEY (id)`
- `FOREIGN KEY (professional_id) REFERENCES app.professionals(user_id)`
- `NOT NULL professional_id`
- `NOT NULL title`
- `FOREIGN KEY (verified_by) REFERENCES app.users(id)`

Índices:

```sql
CREATE UNIQUE INDEX professional_credentials_pkey ON app.professional_credentials USING btree (id)
```

## professional_positions

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `contract_id` | `uuid` | No | — |
| `location` | `geography(Point,4326)` | No | — |
| `accuracy_m` | `integer` | Sí | — |
| `recorded_at` | `timestamp with time zone` | No | now() |
| `expires_at` | `timestamp with time zone` | No | — |

Restricciones:

- `CHECK ((accuracy_m >= 0))`
- `FOREIGN KEY (contract_id) REFERENCES app.contracts(id)`
- `NOT NULL contract_id`
- `NOT NULL expires_at`
- `NOT NULL location`
- `PRIMARY KEY (contract_id)`
- `NOT NULL recorded_at`

Índices:

```sql
CREATE UNIQUE INDEX professional_positions_pkey ON app.professional_positions USING btree (contract_id)
```

## professionals

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `user_id` | `uuid` | No | — |
| `slug` | `text` | No | — |
| `business_name` | `text` | No | — |
| `bio` | `text` | No | ''::text |
| `years_experience` | `integer` | Sí | — |
| `avatar_file_id` | `uuid` | Sí | — |
| `status` | `text` | No | 'active'::text |
| `accepts_requests` | `boolean` | No | true |
| `created_at` | `timestamp with time zone` | No | now() |
| `updated_at` | `timestamp with time zone` | No | now() |

Restricciones:

- `NOT NULL accepts_requests`
- `FOREIGN KEY (avatar_file_id) REFERENCES app.files(id)`
- `NOT NULL bio`
- `NOT NULL business_name`
- `NOT NULL created_at`
- `PRIMARY KEY (user_id)`
- `CHECK ((slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'::text))`
- `UNIQUE (slug)`
- `NOT NULL slug`
- `CHECK ((status = ANY (ARRAY['active'::text, 'paused'::text, 'suspended'::text])))`
- `NOT NULL status`
- `NOT NULL updated_at`
- `FOREIGN KEY (user_id) REFERENCES app.users(id)`
- `NOT NULL user_id`
- `CHECK (((years_experience >= 0) AND (years_experience <= 90)))`

Índices:

```sql
CREATE UNIQUE INDEX professionals_pkey ON app.professionals USING btree (user_id)
CREATE UNIQUE INDEX professionals_slug_key ON app.professionals USING btree (slug)
```

## proposals

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `request_id` | `uuid` | No | — |
| `professional_id` | `uuid` | No | — |
| `offering_id` | `uuid` | Sí | — |
| `message` | `text` | No | — |
| `price_minor` | `bigint` | Sí | — |
| `currency` | `character(3)` | No | — |
| `status` | `text` | No | 'pending'::text |
| `professional_accepted_at` | `timestamp with time zone` | No | now() |
| `customer_accepted_at` | `timestamp with time zone` | Sí | — |
| `expires_at` | `timestamp with time zone` | No | — |
| `created_at` | `timestamp with time zone` | No | now() |

Restricciones:

- `CHECK ((expires_at > created_at))`
- `NOT NULL created_at`
- `NOT NULL currency`
- `NOT NULL expires_at`
- `NOT NULL id`
- `UNIQUE (id, request_id, professional_id)`
- `CHECK (((length(message) >= 1) AND (length(message) <= 4000)))`
- `NOT NULL message`
- `FOREIGN KEY (offering_id) REFERENCES app.offerings(id)`
- `PRIMARY KEY (id)`
- `CHECK ((price_minor >= 0))`
- `NOT NULL professional_accepted_at`
- `FOREIGN KEY (professional_id) REFERENCES app.professionals(user_id)`
- `NOT NULL professional_id`
- `FOREIGN KEY (request_id) REFERENCES app.service_requests(id)`
- `NOT NULL request_id`
- `UNIQUE (request_id, professional_id)`
- `CHECK ((status = ANY (ARRAY['pending'::text, 'accepted'::text, 'rejected'::text, 'withdrawn'::text])))`
- `NOT NULL status`

Índices:

```sql
CREATE UNIQUE INDEX proposals_id_request_id_professional_id_key ON app.proposals USING btree (id, request_id, professional_id)
CREATE UNIQUE INDEX proposals_pkey ON app.proposals USING btree (id)
CREATE INDEX proposals_professional_idx ON app.proposals USING btree (professional_id, status)
CREATE UNIQUE INDEX proposals_request_id_professional_id_key ON app.proposals USING btree (request_id, professional_id)
```

## provider_payment_registry

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `provider` | `text` | No | — |
| `payment_id` | `text` | No | — |
| `direction` | `text` | No | — |
| `purpose` | `text` | No | — |
| `reference_id` | `uuid` | No | — |
| `created_at` | `timestamp with time zone` | No | now() |

Restricciones:

- `NOT NULL created_at`
- `CHECK ((direction = ANY (ARRAY['incoming'::text, 'outgoing'::text])))`
- `NOT NULL direction`
- `NOT NULL payment_id`
- `PRIMARY KEY (provider, payment_id, direction)`
- `NOT NULL provider`
- `CHECK ((purpose = ANY (ARRAY['credits'::text, 'topup'::text, 'withdrawal'::text])))`
- `NOT NULL purpose`
- `NOT NULL reference_id`

Índices:

```sql
CREATE UNIQUE INDEX provider_payment_registry_pkey ON app.provider_payment_registry USING btree (provider, payment_id, direction)
```

## request_attachments

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `request_id` | `uuid` | No | — |
| `file_id` | `uuid` | No | — |

Restricciones:

- `FOREIGN KEY (file_id) REFERENCES app.files(id)`
- `NOT NULL file_id`
- `PRIMARY KEY (request_id, file_id)`
- `FOREIGN KEY (request_id) REFERENCES app.service_requests(id)`
- `NOT NULL request_id`

Índices:

```sql
CREATE UNIQUE INDEX request_attachments_pkey ON app.request_attachments USING btree (request_id, file_id)
```

## request_locations

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `request_id` | `uuid` | No | — |
| `customer_id` | `uuid` | No | — |
| `source_address_id` | `uuid` | Sí | — |
| `location` | `geography(Point,4326)` | No | — |
| `address_line` | `text` | No | — |
| `instructions` | `text` | Sí | — |
| `country_code` | `character(2)` | No | — |
| `city` | `text` | No | — |

Restricciones:

- `NOT NULL address_line`
- `NOT NULL city`
- `NOT NULL country_code`
- `FOREIGN KEY (customer_id) REFERENCES app.users(id)`
- `NOT NULL customer_id`
- `NOT NULL location`
- `PRIMARY KEY (request_id)`
- `FOREIGN KEY (request_id, customer_id) REFERENCES app.service_requests(id, customer_id)`
- `FOREIGN KEY (request_id) REFERENCES app.service_requests(id)`
- `NOT NULL request_id`
- `FOREIGN KEY (source_address_id, customer_id) REFERENCES app.addresses(id, user_id)`

Índices:

```sql
CREATE INDEX request_locations_geo_idx ON app.request_locations USING gist (location)
CREATE UNIQUE INDEX request_locations_pkey ON app.request_locations USING btree (request_id)
```

## reviews

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `contract_id` | `uuid` | No | — |
| `author_id` | `uuid` | No | — |
| `subject_id` | `uuid` | No | — |
| `rating` | `smallint` | No | — |
| `recommends` | `boolean` | Sí | — |
| `comment` | `text` | Sí | — |
| `response` | `text` | Sí | — |
| `responded_at` | `timestamp with time zone` | Sí | — |
| `moderation_status` | `text` | No | 'pending'::text |
| `created_at` | `timestamp with time zone` | No | now() |

Restricciones:

- `FOREIGN KEY (author_id) REFERENCES app.users(id)`
- `NOT NULL author_id`
- `CHECK ((author_id <> subject_id))`
- `CHECK ((length(comment) <= 4000))`
- `UNIQUE (contract_id, author_id)`
- `FOREIGN KEY (contract_id) REFERENCES app.contracts(id)`
- `NOT NULL contract_id`
- `NOT NULL created_at`
- `NOT NULL id`
- `CHECK ((moderation_status = ANY (ARRAY['pending'::text, 'published'::text, 'hidden'::text])))`
- `NOT NULL moderation_status`
- `PRIMARY KEY (id)`
- `CHECK (((rating >= 1) AND (rating <= 5)))`
- `NOT NULL rating`
- `FOREIGN KEY (subject_id) REFERENCES app.users(id)`
- `NOT NULL subject_id`

Índices:

```sql
CREATE UNIQUE INDEX reviews_contract_id_author_id_key ON app.reviews USING btree (contract_id, author_id)
CREATE UNIQUE INDEX reviews_pkey ON app.reviews USING btree (id)
CREATE INDEX reviews_subject_idx ON app.reviews USING btree (subject_id) WHERE (moderation_status = 'published'::text)
```

## role_permissions

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `role_code` | `text` | No | — |
| `permission_code` | `text` | No | — |

Restricciones:

- `FOREIGN KEY (permission_code) REFERENCES app.permissions(code)`
- `NOT NULL permission_code`
- `PRIMARY KEY (role_code, permission_code)`
- `FOREIGN KEY (role_code) REFERENCES app.roles(code)`
- `NOT NULL role_code`

Índices:

```sql
CREATE UNIQUE INDEX role_permissions_pkey ON app.role_permissions USING btree (role_code, permission_code)
```

## roles

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `code` | `text` | No | — |
| `description` | `text` | No | — |

Restricciones:

- `NOT NULL code`
- `NOT NULL description`
- `PRIMARY KEY (code)`

Índices:

```sql
CREATE UNIQUE INDEX roles_pkey ON app.roles USING btree (code)
```

## service_deliveries

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `contract_id` | `uuid` | No | — |
| `professional_id` | `uuid` | No | — |
| `version` | `integer` | No | — |
| `summary` | `text` | No | — |
| `requirements_snapshot` | `text` | No | — |
| `terms_version` | `integer` | No | — |
| `status` | `text` | No | 'submitted'::text |
| `client_message_id` | `uuid` | No | — |
| `created_at` | `timestamp with time zone` | No | now() |
| `reviewed_at` | `timestamp with time zone` | Sí | — |
| `review_note` | `text` | Sí | — |

Restricciones:

- `NOT NULL client_message_id`
- `UNIQUE (contract_id, client_message_id)`
- `FOREIGN KEY (contract_id) REFERENCES app.contracts(id)`
- `NOT NULL contract_id`
- `UNIQUE (contract_id, version)`
- `NOT NULL created_at`
- `NOT NULL id`
- `PRIMARY KEY (id)`
- `FOREIGN KEY (professional_id) REFERENCES app.users(id)`
- `NOT NULL professional_id`
- `NOT NULL requirements_snapshot`
- `CHECK ((status = ANY (ARRAY['submitted'::text, 'changes_requested'::text, 'accepted'::text])))`
- `NOT NULL status`
- `CHECK (((length(summary) >= 10) AND (length(summary) <= 10000)))`
- `NOT NULL summary`
- `NOT NULL terms_version`
- `CHECK ((version > 0))`
- `NOT NULL version`

Índices:

```sql
CREATE UNIQUE INDEX service_deliveries_contract_id_client_message_id_key ON app.service_deliveries USING btree (contract_id, client_message_id)
CREATE UNIQUE INDEX service_deliveries_contract_id_version_key ON app.service_deliveries USING btree (contract_id, version)
CREATE UNIQUE INDEX service_deliveries_pkey ON app.service_deliveries USING btree (id)
```

## service_escrows

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `contract_id` | `uuid` | No | — |
| `status` | `text` | No | 'unfunded'::text |
| `quote_id` | `uuid` | Sí | — |
| `funded_at` | `timestamp with time zone` | Sí | — |
| `released_at` | `timestamp with time zone` | Sí | — |
| `refunded_at` | `timestamp with time zone` | Sí | — |
| `refund_requested_at` | `timestamp with time zone` | Sí | — |
| `refund_reason` | `text` | Sí | — |
| `release_key` | `uuid` | Sí | — |

Restricciones:

- `CHECK (((status = 'unfunded'::text) OR ((quote_id IS NOT NULL) AND (funded_at IS NOT NULL))))`
- `FOREIGN KEY (contract_id) REFERENCES app.contracts(id)`
- `NOT NULL contract_id`
- `PRIMARY KEY (contract_id)`
- `FOREIGN KEY (quote_id) REFERENCES app.funding_quotes(id)`
- `UNIQUE (quote_id)`
- `CHECK ((status = ANY (ARRAY['unfunded'::text, 'funded'::text, 'disputed'::text, 'released'::text, 'refunded'::text])))`
- `NOT NULL status`

Índices:

```sql
CREATE UNIQUE INDEX service_escrows_pkey ON app.service_escrows USING btree (contract_id)
CREATE UNIQUE INDEX service_escrows_quote_id_key ON app.service_escrows USING btree (quote_id)
```

## service_requests

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `customer_id` | `uuid` | No | — |
| `category_id` | `uuid` | No | — |
| `mode` | `text` | No | — |
| `target_professional_id` | `uuid` | Sí | — |
| `title` | `text` | No | — |
| `description` | `text` | No | — |
| `status` | `text` | No | 'draft'::text |
| `moderation_status` | `text` | No | 'pending'::text |
| `budget_min_minor` | `bigint` | Sí | — |
| `budget_max_minor` | `bigint` | Sí | — |
| `currency` | `character(3)` | No | — |
| `search_radius_m` | `integer` | Sí | — |
| `scheduled_at` | `timestamp with time zone` | Sí | — |
| `expires_at` | `timestamp with time zone` | No | — |
| `created_at` | `timestamp with time zone` | No | now() |
| `idempotency_key` | `uuid` | No | — |
| `delivery_mode` | `text` | No | 'onsite'::text |
| `requires_platform_payment` | `boolean` | No | false |
| `offered_price_minor` | `bigint` | Sí | — |
| `offering_id` | `uuid` | Sí | — |
| `offering_snapshot` | `jsonb` | Sí | — |
| `requires_quote` | `boolean` | No | false |
| `creation_payload` | `jsonb` | Sí | — |

Restricciones:

- `CHECK (((delivery_mode <> 'remote'::text) OR (requires_platform_payment AND (search_radius_m IS NULL))))`
- `CHECK ((budget_max_minor >= 0))`
- `CHECK ((budget_min_minor >= 0))`
- `FOREIGN KEY (category_id) REFERENCES app.categories(id)`
- `NOT NULL category_id`
- `CHECK (((budget_max_minor IS NULL) OR (budget_min_minor IS NULL) OR (budget_max_minor >= budget_min_minor)))`
- `CHECK (((mode = 'direct'::text) = (target_professional_id IS NOT NULL)))`
- `CHECK ((customer_id IS DISTINCT FROM target_professional_id))`
- `CHECK ((expires_at > created_at))`
- `NOT NULL created_at`
- `CHECK ((currency ~ '^[A-Z]{3}$'::text))`
- `NOT NULL currency`
- `FOREIGN KEY (customer_id) REFERENCES app.users(id)`
- `UNIQUE (customer_id, idempotency_key)`
- `NOT NULL customer_id`
- `CHECK ((delivery_mode = ANY (ARRAY['onsite'::text, 'remote'::text])))`
- `NOT NULL delivery_mode`
- `CHECK (((length(description) >= 10) AND (length(description) <= 10000)))`
- `NOT NULL description`
- `NOT NULL expires_at`
- `UNIQUE (id, customer_id)`
- `NOT NULL id`
- `NOT NULL idempotency_key`
- `CHECK ((mode = ANY (ARRAY['direct'::text, 'broadcast'::text])))`
- `NOT NULL mode`
- `CHECK ((moderation_status = ANY (ARRAY['pending'::text, 'approved'::text, 'rejected'::text])))`
- `NOT NULL moderation_status`
- `CHECK ((offered_price_minor > 0))`
- `FOREIGN KEY (offering_id) REFERENCES app.offerings(id)`
- `PRIMARY KEY (id)`
- `NOT NULL requires_platform_payment`
- `NOT NULL requires_quote`
- `CHECK (((search_radius_m >= 100) AND (search_radius_m <= 200000)))`
- `CHECK ((status = ANY (ARRAY['draft'::text, 'open'::text, 'matched'::text, 'cancelled'::text, 'expired'::text, 'completed'::text])))`
- `NOT NULL status`
- `FOREIGN KEY (target_professional_id) REFERENCES app.professionals(user_id)`
- `CHECK (((length(title) >= 5) AND (length(title) <= 160)))`
- `NOT NULL title`

Índices:

```sql
CREATE INDEX requests_customer_idx ON app.service_requests USING btree (customer_id, created_at DESC)
CREATE INDEX requests_feed_idx ON app.service_requests USING btree (category_id, expires_at) WHERE (status = 'open'::text)
CREATE INDEX requests_offering_idx ON app.service_requests USING btree (offering_id) WHERE (offering_id IS NOT NULL)
CREATE UNIQUE INDEX service_requests_customer_id_idempotency_key_key ON app.service_requests USING btree (customer_id, idempotency_key)
CREATE UNIQUE INDEX service_requests_id_customer_id_key ON app.service_requests USING btree (id, customer_id)
CREATE UNIQUE INDEX service_requests_pkey ON app.service_requests USING btree (id)
```

## sessions

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `user_id` | `uuid` | No | — |
| `refresh_token_hash` | `text` | No | — |
| `device_label` | `text` | Sí | — |
| `expires_at` | `timestamp with time zone` | No | — |
| `revoked_at` | `timestamp with time zone` | Sí | — |
| `created_at` | `timestamp with time zone` | No | now() |
| `family_id` | `uuid` | No | gen_random_uuid() |
| `rotated_from` | `uuid` | Sí | — |

Restricciones:

- `CHECK ((expires_at > created_at))`
- `NOT NULL created_at`
- `NOT NULL expires_at`
- `NOT NULL family_id`
- `NOT NULL id`
- `PRIMARY KEY (id)`
- `UNIQUE (refresh_token_hash)`
- `NOT NULL refresh_token_hash`
- `FOREIGN KEY (rotated_from) REFERENCES app.sessions(id)`
- `FOREIGN KEY (user_id) REFERENCES app.users(id)`
- `NOT NULL user_id`

Índices:

```sql
CREATE INDEX sessions_family_idx ON app.sessions USING btree (family_id)
CREATE UNIQUE INDEX sessions_pkey ON app.sessions USING btree (id)
CREATE UNIQUE INDEX sessions_refresh_token_hash_key ON app.sessions USING btree (refresh_token_hash)
CREATE INDEX sessions_user_idx ON app.sessions USING btree (user_id) WHERE (revoked_at IS NULL)
```

## stores

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `professional_id` | `uuid` | No | — |
| `slug` | `text` | No | — |
| `name` | `text` | No | — |
| `description` | `text` | Sí | — |
| `status` | `text` | No | 'inactive'::text |
| `created_at` | `timestamp with time zone` | No | now() |

Restricciones:

- `NOT NULL created_at`
- `NOT NULL id`
- `NOT NULL name`
- `PRIMARY KEY (id)`
- `FOREIGN KEY (professional_id) REFERENCES app.professionals(user_id)`
- `UNIQUE (professional_id)`
- `NOT NULL professional_id`
- `UNIQUE (slug)`
- `NOT NULL slug`
- `CHECK ((status = ANY (ARRAY['inactive'::text, 'active'::text, 'suspended'::text])))`
- `NOT NULL status`

Índices:

```sql
CREATE UNIQUE INDEX stores_pkey ON app.stores USING btree (id)
CREATE UNIQUE INDEX stores_professional_id_key ON app.stores USING btree (professional_id)
CREATE UNIQUE INDEX stores_slug_key ON app.stores USING btree (slug)
```

## support_messages

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `ticket_id` | `uuid` | No | — |
| `author_id` | `uuid` | No | — |
| `body` | `text` | No | — |
| `internal_only` | `boolean` | No | false |
| `created_at` | `timestamp with time zone` | No | now() |

Restricciones:

- `FOREIGN KEY (author_id) REFERENCES app.users(id)`
- `NOT NULL author_id`
- `NOT NULL body`
- `NOT NULL created_at`
- `NOT NULL id`
- `NOT NULL internal_only`
- `PRIMARY KEY (id)`
- `FOREIGN KEY (ticket_id) REFERENCES app.support_tickets(id)`
- `NOT NULL ticket_id`

Índices:

```sql
CREATE UNIQUE INDEX support_messages_pkey ON app.support_messages USING btree (id)
```

## support_tickets

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `opened_by` | `uuid` | No | — |
| `assigned_to` | `uuid` | Sí | — |
| `contract_id` | `uuid` | Sí | — |
| `order_id` | `uuid` | Sí | — |
| `category` | `text` | No | — |
| `subject` | `text` | No | — |
| `description` | `text` | No | — |
| `status` | `text` | No | 'open'::text |
| `priority` | `text` | No | 'normal'::text |
| `created_at` | `timestamp with time zone` | No | now() |
| `resolved_at` | `timestamp with time zone` | Sí | — |

Restricciones:

- `FOREIGN KEY (assigned_to) REFERENCES app.users(id)`
- `CHECK ((category = ANY (ARRAY['service'::text, 'credits'::text, 'kyc'::text, 'review'::text, 'marketplace'::text, 'account'::text, 'abuse'::text, 'other'::text])))`
- `NOT NULL category`
- `FOREIGN KEY (contract_id) REFERENCES app.contracts(id)`
- `NOT NULL created_at`
- `NOT NULL description`
- `NOT NULL id`
- `FOREIGN KEY (opened_by) REFERENCES app.users(id)`
- `NOT NULL opened_by`
- `FOREIGN KEY (order_id) REFERENCES app.orders(id)`
- `PRIMARY KEY (id)`
- `CHECK ((priority = ANY (ARRAY['low'::text, 'normal'::text, 'high'::text, 'urgent'::text])))`
- `NOT NULL priority`
- `CHECK ((status = ANY (ARRAY['open'::text, 'in_progress'::text, 'waiting_user'::text, 'resolved'::text, 'closed'::text])))`
- `NOT NULL status`
- `NOT NULL subject`

Índices:

```sql
CREATE UNIQUE INDEX support_tickets_pkey ON app.support_tickets USING btree (id)
CREATE INDEX tickets_queue_idx ON app.support_tickets USING btree (status, priority, created_at)
```

## user_consents

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `user_id` | `uuid` | No | — |
| `document_id` | `uuid` | No | — |
| `accepted_at` | `timestamp with time zone` | No | now() |
| `revoked_at` | `timestamp with time zone` | Sí | — |
| `evidence_hash` | `text` | No | — |

Restricciones:

- `NOT NULL accepted_at`
- `FOREIGN KEY (document_id) REFERENCES app.legal_documents(id)`
- `NOT NULL document_id`
- `NOT NULL evidence_hash`
- `NOT NULL id`
- `PRIMARY KEY (id)`
- `UNIQUE (user_id, document_id)`
- `FOREIGN KEY (user_id) REFERENCES app.users(id)`
- `NOT NULL user_id`

Índices:

```sql
CREATE UNIQUE INDEX user_consents_pkey ON app.user_consents USING btree (id)
CREATE UNIQUE INDEX user_consents_user_id_document_id_key ON app.user_consents USING btree (user_id, document_id)
```

## user_contacts

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `user_id` | `uuid` | No | — |
| `kind` | `text` | No | — |
| `label` | `text` | No | — |
| `value` | `text` | No | — |
| `is_primary` | `boolean` | No | false |
| `verified_at` | `timestamp with time zone` | Sí | — |
| `share_on_contract` | `boolean` | No | true |
| `created_at` | `timestamp with time zone` | No | now() |

Restricciones:

- `NOT NULL created_at`
- `NOT NULL id`
- `NOT NULL is_primary`
- `CHECK ((kind = ANY (ARRAY['phone'::text, 'whatsapp'::text, 'email'::text])))`
- `NOT NULL kind`
- `NOT NULL label`
- `PRIMARY KEY (id)`
- `NOT NULL share_on_contract`
- `FOREIGN KEY (user_id) REFERENCES app.users(id)`
- `UNIQUE (user_id, kind, value)`
- `NOT NULL user_id`
- `CHECK (((length(value) >= 3) AND (length(value) <= 254)))`
- `NOT NULL value`

Índices:

```sql
CREATE UNIQUE INDEX contacts_primary_idx ON app.user_contacts USING btree (user_id, kind) WHERE is_primary
CREATE UNIQUE INDEX user_contacts_pkey ON app.user_contacts USING btree (id)
CREATE UNIQUE INDEX user_contacts_user_id_kind_value_key ON app.user_contacts USING btree (user_id, kind, value)
```

## user_roles

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `user_id` | `uuid` | No | — |
| `role_code` | `text` | No | — |
| `granted_by` | `uuid` | Sí | — |
| `granted_at` | `timestamp with time zone` | No | now() |

Restricciones:

- `NOT NULL granted_at`
- `FOREIGN KEY (granted_by) REFERENCES app.users(id)`
- `PRIMARY KEY (user_id, role_code)`
- `FOREIGN KEY (role_code) REFERENCES app.roles(code)`
- `NOT NULL role_code`
- `FOREIGN KEY (user_id) REFERENCES app.users(id)`
- `NOT NULL user_id`

Índices:

```sql
CREATE UNIQUE INDEX user_roles_pkey ON app.user_roles USING btree (user_id, role_code)
```

## users

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `email` | `citext` | No | — |
| `display_name` | `text` | No | — |
| `status` | `text` | No | 'active'::text |
| `locale` | `text` | No | 'es-VE'::text |
| `timezone` | `text` | No | 'America/Caracas'::text |
| `email_verified_at` | `timestamp with time zone` | Sí | — |
| `created_at` | `timestamp with time zone` | No | now() |
| `updated_at` | `timestamp with time zone` | No | now() |
| `closed_at` | `timestamp with time zone` | Sí | — |
| `requires_platform_payment` | `boolean` | No | false |

Restricciones:

- `NOT NULL created_at`
- `CHECK (((length(display_name) >= 2) AND (length(display_name) <= 120)))`
- `NOT NULL display_name`
- `CHECK ((POSITION(('@'::text) IN ((email)::text)) > 1))`
- `UNIQUE (email)`
- `NOT NULL email`
- `NOT NULL id`
- `NOT NULL locale`
- `PRIMARY KEY (id)`
- `NOT NULL requires_platform_payment`
- `CHECK ((status = ANY (ARRAY['active'::text, 'suspended'::text, 'closed'::text])))`
- `NOT NULL status`
- `NOT NULL timezone`
- `NOT NULL updated_at`

Índices:

```sql
CREATE UNIQUE INDEX users_email_key ON app.users USING btree (email)
CREATE UNIQUE INDEX users_pkey ON app.users USING btree (id)
```

## withdrawal_fee_policies

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `currency` | `character(3)` | No | — |
| `basis_points` | `integer` | No | — |
| `active` | `boolean` | No | true |
| `created_by` | `uuid` | No | — |
| `reason` | `text` | No | — |
| `created_at` | `timestamp with time zone` | No | now() |

Restricciones:

- `NOT NULL active`
- `CHECK (((basis_points >= 0) AND (basis_points <= 9999)))`
- `NOT NULL basis_points`
- `NOT NULL created_at`
- `FOREIGN KEY (created_by) REFERENCES app.users(id)`
- `NOT NULL created_by`
- `CHECK ((currency ~ '^[A-Z]{3}$'::text))`
- `NOT NULL currency`
- `NOT NULL id`
- `PRIMARY KEY (id)`
- `CHECK ((length(reason) >= 5))`
- `NOT NULL reason`

Índices:

```sql
CREATE UNIQUE INDEX withdrawal_fee_active_idx ON app.withdrawal_fee_policies USING btree (currency) WHERE active
CREATE UNIQUE INDEX withdrawal_fee_policies_pkey ON app.withdrawal_fee_policies USING btree (id)
```

## withdrawal_quotes

| Columna | Tipo | Acepta NULL | Predeterminado / expresión |
|---|---|---|---|
| `id` | `uuid` | No | gen_random_uuid() |
| `wallet_id` | `uuid` | No | — |
| `payout_method_id` | `uuid` | No | — |
| `policy_id` | `uuid` | No | — |
| `amount_minor` | `bigint` | No | — |
| `basis_points` | `integer` | No | — |
| `fee_minor` | `bigint` | No | — |
| `net_minor` | `bigint` | No | — |
| `created_at` | `timestamp with time zone` | No | now() |
| `expires_at` | `timestamp with time zone` | No | — |

Restricciones:

- `CHECK (((amount_minor >= 1) AND (amount_minor <= '1000000000000'::bigint)))`
- `NOT NULL amount_minor`
- `CHECK (((basis_points >= 0) AND (basis_points <= 9999)))`
- `NOT NULL basis_points`
- `CHECK ((amount_minor = (fee_minor + net_minor)))`
- `NOT NULL created_at`
- `NOT NULL expires_at`
- `CHECK ((fee_minor >= 0))`
- `NOT NULL fee_minor`
- `NOT NULL id`
- `CHECK ((net_minor > 0))`
- `NOT NULL net_minor`
- `FOREIGN KEY (payout_method_id) REFERENCES app.payout_methods(id)`
- `NOT NULL payout_method_id`
- `PRIMARY KEY (id)`
- `FOREIGN KEY (policy_id) REFERENCES app.withdrawal_fee_policies(id)`
- `NOT NULL policy_id`
- `FOREIGN KEY (wallet_id) REFERENCES app.money_wallets(id)`
- `NOT NULL wallet_id`

Índices:

```sql
CREATE UNIQUE INDEX withdrawal_quotes_pkey ON app.withdrawal_quotes USING btree (id)
```
