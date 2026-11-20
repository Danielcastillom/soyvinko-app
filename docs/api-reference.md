# Referencia SQL del backend

167 funciones del esquema `api`, generadas desde PostgreSQL.

Las funciones no son endpoints HTTP. Consultar [integración](integration.md) para contexto de usuario, roles y responsabilidades del servidor.

| Función | Rol autorizado | Devuelve |
|---|---|---|
| [`accept_direct_request`](#accept_direct_request) | chamba_api | `uuid` |
| [`accept_proposal`](#accept_proposal) | chamba_api | `uuid` |
| [`accept_terms`](#accept_terms) | chamba_api | `void` |
| [`activate_professional`](#activate_professional) | chamba_api | `uuid` |
| [`activate_store`](#activate_store) | chamba_api | `uuid` |
| [`active_plans`](#active_plans) | chamba_api | `SETOF jsonb` |
| [`add_address`](#add_address) | chamba_api | `uuid` |
| [`add_address_ve`](#add_address_ve) | chamba_api | `uuid` |
| [`add_branch`](#add_branch) | chamba_api | `uuid` |
| [`add_branch_ve`](#add_branch_ve) | chamba_api | `uuid` |
| [`add_credential`](#add_credential) | chamba_api | `uuid` |
| [`add_offering`](#add_offering) | chamba_api | `uuid` |
| [`add_payout_method`](#add_payout_method) | chamba_api | `uuid` |
| [`add_portfolio`](#add_portfolio) | chamba_api | `uuid` |
| [`add_product`](#add_product) | chamba_api | `uuid` |
| [`add_product_image`](#add_product_image) | chamba_api | `void` |
| [`add_professional_contact`](#add_professional_contact) | chamba_api | `uuid` |
| [`admin_file_key`](#admin_file_key) | chamba_admin | `text` |
| [`admin_list`](#admin_list) | chamba_admin | `SETOF jsonb` |
| [`admin_overview`](#admin_overview) | chamba_admin | `jsonb` |
| [`advance_contract`](#advance_contract) | chamba_api | `text` |
| [`advance_order`](#advance_order) | chamba_api | `text` |
| [`approve_escrow_refund`](#approve_escrow_refund) | chamba_api | `void` |
| [`archive_address`](#archive_address) | chamba_api | `void` |
| [`assign_role`](#assign_role) | chamba_admin | `void` |
| [`attach_kyc_file`](#attach_kyc_file) | chamba_api | `void` |
| [`attach_request_file`](#attach_request_file) | chamba_api | `void` |
| [`auth_account`](#auth_account) | chamba_auth | `jsonb` |
| [`auth_challenge`](#auth_challenge) | chamba_auth | `jsonb` |
| [`auth_change_password`](#auth_change_password) | chamba_auth | `void` |
| [`auth_consume_challenge`](#auth_consume_challenge) | chamba_auth | `boolean` |
| [`auth_mfa`](#auth_mfa) | chamba_auth | `jsonb` |
| [`auth_principal`](#auth_principal) | chamba_auth | `jsonb` |
| [`auth_rate_limit`](#auth_rate_limit) | chamba_auth | `boolean` |
| [`auth_record_attempt`](#auth_record_attempt) | chamba_auth | `void` |
| [`auth_revoke_refresh`](#auth_revoke_refresh) | chamba_auth | `void` |
| [`auth_rotate_session`](#auth_rotate_session) | chamba_auth | `jsonb` |
| [`auth_set_mfa`](#auth_set_mfa) | chamba_auth | `void` |
| [`available_requests`](#available_requests) | chamba_api | `TABLE(request_id uuid, category_id uuid, title text, description text, mode text, budget_min_minor bigint, budget_max_minor bigint, currency character, expires_at timestamp with time zone, attachments jsonb)` |
| [`cancel_contract`](#cancel_contract) | chamba_api | `void` |
| [`cancel_request`](#cancel_request) | chamba_api | `void` |
| [`cancel_withdrawal`](#cancel_withdrawal) | chamba_api | `void` |
| [`catalog`](#catalog) | chamba_api | `SETOF jsonb` |
| [`chat_messages`](#chat_messages) | chamba_api | `SETOF jsonb` |
| [`claim_notifications`](#claim_notifications) | chamba_worker | `SETOF jsonb` |
| [`configure_category`](#configure_category) | chamba_admin | `uuid` |
| [`configure_plan`](#configure_plan) | chamba_admin | `uuid` |
| [`configure_withdrawal_fee`](#configure_withdrawal_fee) | chamba_admin | `uuid` |
| [`confirm_external_payment`](#confirm_external_payment) | chamba_api | `void` |
| [`confirm_purchase`](#confirm_purchase) | chamba_billing | `uuid` |
| [`confirm_topup`](#confirm_topup) | chamba_billing | `uuid` |
| [`confirm_withdrawal`](#confirm_withdrawal) | chamba_billing | `void` |
| [`contract_contacts`](#contract_contacts) | chamba_api | `jsonb` |
| [`contract_detail`](#contract_detail) | chamba_api | `jsonb` |
| [`create_customer_request`](#create_customer_request) | chamba_api | `uuid` |
| [`create_order`](#create_order) | chamba_api | `uuid` |
| [`create_remote_request`](#create_remote_request) | chamba_api | `uuid` |
| [`create_request`](#create_request) | chamba_api | `uuid` |
| [`create_session`](#create_session) | chamba_auth | `uuid` |
| [`create_topup`](#create_topup) | chamba_api | `uuid` |
| [`dashboard`](#dashboard) | chamba_api | `jsonb` |
| [`deliver_in_app`](#deliver_in_app) | chamba_worker | `void` |
| [`disable_payout_method`](#disable_payout_method) | chamba_api | `void` |
| [`discover`](#discover) | chamba_api | `SETOF jsonb` |
| [`dispute_contract`](#dispute_contract) | chamba_api | `uuid` |
| [`escrow_details`](#escrow_details) | chamba_api | `jsonb` |
| [`expire_requests`](#expire_requests) | chamba_worker | `integer` |
| [`external_payment_reports`](#external_payment_reports) | chamba_api | `SETOF jsonb` |
| [`file_object_key`](#file_object_key) | chamba_api | `text` |
| [`finish_notification`](#finish_notification) | chamba_worker | `void` |
| [`flag_money_incident`](#flag_money_incident) | chamba_billing | `uuid` |
| [`fund_contract`](#fund_contract) | chamba_api | `uuid` |
| [`geo_municipalities`](#geo_municipalities) | chamba_api | `SETOF jsonb` |
| [`geo_parish`](#geo_parish) | chamba_api | `jsonb` |
| [`geo_parishes`](#geo_parishes) | chamba_api | `SETOF jsonb` |
| [`geo_states`](#geo_states) | chamba_api | `SETOF jsonb` |
| [`kyc_case`](#kyc_case) | chamba_admin | `jsonb` |
| [`locate_parish`](#locate_parish) | chamba_api | `jsonb` |
| [`manage_ticket`](#manage_ticket) | chamba_admin | `void` |
| [`marketplace`](#marketplace) | chamba_api | `SETOF jsonb` |
| [`moderate_file`](#moderate_file) | chamba_admin | `void` |
| [`moderate_request`](#moderate_request) | chamba_admin | `void` |
| [`moderate_review`](#moderate_review) | chamba_admin | `void` |
| [`money_history`](#money_history) | chamba_api | `SETOF jsonb` |
| [`money_wallet`](#money_wallet) | chamba_api | `jsonb` |
| [`my_account`](#my_account) | chamba_api | `jsonb` |
| [`my_contracts`](#my_contracts) | chamba_api | `SETOF jsonb` |
| [`my_credit_purchases`](#my_credit_purchases) | chamba_api | `SETOF jsonb` |
| [`my_favorites`](#my_favorites) | chamba_api | `SETOF jsonb` |
| [`my_files`](#my_files) | chamba_api | `SETOF jsonb` |
| [`my_kyc`](#my_kyc) | chamba_api | `SETOF jsonb` |
| [`my_notifications`](#my_notifications) | chamba_api | `SETOF jsonb` |
| [`my_orders`](#my_orders) | chamba_api | `SETOF jsonb` |
| [`my_payout_methods`](#my_payout_methods) | chamba_api | `SETOF jsonb` |
| [`my_products`](#my_products) | chamba_api | `SETOF jsonb` |
| [`my_professional`](#my_professional) | chamba_api | `jsonb` |
| [`my_proposals`](#my_proposals) | chamba_api | `SETOF jsonb` |
| [`my_requests`](#my_requests) | chamba_api | `SETOF jsonb` |
| [`my_sessions`](#my_sessions) | chamba_api | `SETOF jsonb` |
| [`my_tickets`](#my_tickets) | chamba_api | `SETOF jsonb` |
| [`my_topups`](#my_topups) | chamba_api | `SETOF jsonb` |
| [`my_wallet`](#my_wallet) | chamba_api | `jsonb` |
| [`my_withdrawals`](#my_withdrawals) | chamba_api | `SETOF jsonb` |
| [`navigate`](#navigate) | chamba_api | `jsonb` |
| [`nearby_professionals`](#nearby_professionals) | chamba_api | `SETOF jsonb` |
| [`open_ticket`](#open_ticket) | chamba_api | `uuid` |
| [`order_delivery`](#order_delivery) | chamba_api | `jsonb` |
| [`payment_preferences`](#payment_preferences) | chamba_api | `jsonb` |
| [`prepare_withdrawal`](#prepare_withdrawal) | chamba_billing | `jsonb` |
| [`professional_assets`](#professional_assets) | chamba_api | `jsonb` |
| [`professional_profile`](#professional_profile) | chamba_api | `jsonb` |
| [`professional_reviews`](#professional_reviews) | chamba_api | `SETOF jsonb` |
| [`propose_terms`](#propose_terms) | chamba_api | `uuid` |
| [`protect_service_request`](#protect_service_request) | chamba_api | `void` |
| [`publish_request`](#publish_request) | chamba_api | `void` |
| [`quote_contract_funding`](#quote_contract_funding) | chamba_api | `jsonb` |
| [`quote_withdrawal`](#quote_withdrawal) | chamba_api | `jsonb` |
| [`rate_contract`](#rate_contract) | chamba_api | `uuid` |
| [`read_notification`](#read_notification) | chamba_api | `void` |
| [`refund_contract_credit`](#refund_contract_credit) | chamba_admin | `bigint` |
| [`register_upload`](#register_upload) | chamba_worker | `uuid` |
| [`register_user`](#register_user) | chamba_auth | `uuid` |
| [`release_contract_funds`](#release_contract_funds) | chamba_api | `uuid` |
| [`remove_contact`](#remove_contact) | chamba_api | `void` |
| [`remove_request_image`](#remove_request_image) | chamba_api | `void` |
| [`reply_ticket`](#reply_ticket) | chamba_api | `uuid` |
| [`report_external_payment`](#report_external_payment) | chamba_api | `uuid` |
| [`report_position`](#report_position) | chamba_api | `void` |
| [`request_delivery_changes`](#request_delivery_changes) | chamba_api | `void` |
| [`request_escrow_refund`](#request_escrow_refund) | chamba_api | `uuid` |
| [`request_offering`](#request_offering) | chamba_api | `uuid` |
| [`request_proposals`](#request_proposals) | chamba_api | `SETOF jsonb` |
| [`request_withdrawal`](#request_withdrawal) | chamba_api | `uuid` |
| [`resolve_escrow_refund`](#resolve_escrow_refund) | chamba_admin | `void` |
| [`resolve_session`](#resolve_session) | chamba_auth | `uuid` |
| [`respond_review`](#respond_review) | chamba_api | `void` |
| [`review_kyc`](#review_kyc) | chamba_admin | `void` |
| [`revoke_location`](#revoke_location) | chamba_api | `void` |
| [`revoke_session`](#revoke_session) | chamba_api | `void` |
| [`save_contact`](#save_contact) | chamba_api | `uuid` |
| [`search_professionals`](#search_professionals) | chamba_api | `TABLE(professional_id uuid, profile jsonb, distance_m bigint, verified boolean, sponsored boolean)` |
| [`search_remote_professionals`](#search_remote_professionals) | chamba_api | `SETOF jsonb` |
| [`send_attachment`](#send_attachment) | chamba_api | `uuid` |
| [`send_message`](#send_message) | chamba_api | `uuid` |
| [`service_feed`](#service_feed) | chamba_api | `SETOF jsonb` |
| [`set_branch_hours`](#set_branch_hours) | chamba_api | `uuid` |
| [`set_coverage`](#set_coverage) | chamba_api | `uuid` |
| [`set_favorite`](#set_favorite) | chamba_api | `void` |
| [`set_offering_delivery`](#set_offering_delivery) | chamba_api | `void` |
| [`set_payment_preference`](#set_payment_preference) | chamba_api | `void` |
| [`set_user_status`](#set_user_status) | chamba_admin | `void` |
| [`share_location`](#share_location) | chamba_api | `uuid` |
| [`share_order_delivery`](#share_order_delivery) | chamba_api | `void` |
| [`start_purchase`](#start_purchase) | chamba_api | `uuid` |
| [`store_products`](#store_products) | chamba_api | `SETOF jsonb` |
| [`submit_delivery`](#submit_delivery) | chamba_api | `uuid` |
| [`submit_kyc`](#submit_kyc) | chamba_api | `uuid` |
| [`submit_proposal`](#submit_proposal) | chamba_api | `uuid` |
| [`support_ticket`](#support_ticket) | chamba_admin | `jsonb` |
| [`ticket_thread`](#ticket_thread) | chamba_api | `jsonb` |
| [`update_account`](#update_account) | chamba_api | `void` |
| [`update_offering`](#update_offering) | chamba_api | `void` |
| [`update_product`](#update_product) | chamba_api | `void` |
| [`update_professional`](#update_professional) | chamba_api | `void` |
| [`verify_payout_method`](#verify_payout_method) | chamba_billing | `void` |
| [`withdraw_proposal`](#withdraw_proposal) | chamba_api | `void` |
| [`worker_housekeeping`](#worker_housekeeping) | chamba_worker | `void` |

## accept_direct_request

```sql
api.accept_direct_request(p_request uuid)
```

Devuelve: `uuid`. Rol: chamba_api.

## accept_proposal

```sql
api.accept_proposal(p_proposal uuid)
```

Devuelve: `uuid`. Rol: chamba_api.

## accept_terms

```sql
api.accept_terms(p_terms uuid)
```

Devuelve: `void`. Rol: chamba_api.

## activate_professional

```sql
api.activate_professional(p_slug text, p_business_name text, p_bio text DEFAULT ''::text)
```

Devuelve: `uuid`. Rol: chamba_api.

## activate_store

```sql
api.activate_store(p_slug text, p_name text, p_description text DEFAULT ''::text)
```

Devuelve: `uuid`. Rol: chamba_api.

## active_plans

```sql
api.active_plans()
```

Devuelve: `SETOF jsonb`. Rol: chamba_api.

## add_address

```sql
api.add_address(p_label text, p_country text, p_city text, p_line text, p_lon double precision, p_lat double precision, p_instructions text DEFAULT NULL::text)
```

Devuelve: `uuid`. Rol: chamba_api.

## add_address_ve

```sql
api.add_address_ve(p_label text, p_country text, p_state text, p_municipality text, p_parish text, p_line text, p_lon double precision, p_lat double precision, p_instructions text DEFAULT NULL::text)
```

Devuelve: `uuid`. Rol: chamba_api.

## add_branch

```sql
api.add_branch(p_name text, p_country text, p_city text, p_line text, p_lon double precision, p_lat double precision, p_public_address boolean DEFAULT false)
```

Devuelve: `uuid`. Rol: chamba_api.

## add_branch_ve

```sql
api.add_branch_ve(p_name text, p_country text, p_state text, p_municipality text, p_parish text, p_line text, p_lon double precision, p_lat double precision, p_public_address boolean DEFAULT false)
```

Devuelve: `uuid`. Rol: chamba_api.

## add_credential

```sql
api.add_credential(p_title text, p_issuer text, p_file uuid DEFAULT NULL::uuid, p_issued date DEFAULT NULL::date, p_expires date DEFAULT NULL::date)
```

Devuelve: `uuid`. Rol: chamba_api.

## add_offering

```sql
api.add_offering(p_category uuid, p_title text, p_currency text, p_price_minor bigint DEFAULT NULL::bigint, p_description text DEFAULT ''::text)
```

Devuelve: `uuid`. Rol: chamba_api.

## add_payout_method

```sql
api.add_payout_method(p_provider text, p_token text, p_label text, p_masked text, p_currency text)
```

Devuelve: `uuid`. Rol: chamba_api.

## add_portfolio

```sql
api.add_portfolio(p_title text, p_description text, p_file uuid)
```

Devuelve: `uuid`. Rol: chamba_api.

## add_product

```sql
api.add_product(p_store uuid, p_sku text, p_name text, p_description text, p_price bigint, p_currency text, p_stock integer)
```

Devuelve: `uuid`. Rol: chamba_api.

## add_product_image

```sql
api.add_product_image(p_product uuid, p_file uuid)
```

Devuelve: `void`. Rol: chamba_api.

## add_professional_contact

```sql
api.add_professional_contact(p_branch uuid, p_kind text, p_label text, p_value text)
```

Devuelve: `uuid`. Rol: chamba_api.

## admin_file_key

```sql
api.admin_file_key(p_file uuid, p_reason text)
```

Devuelve: `text`. Rol: chamba_admin.

## admin_list

```sql
api.admin_list(p_resource text, p_limit integer DEFAULT 50, p_offset integer DEFAULT 0)
```

Devuelve: `SETOF jsonb`. Rol: chamba_admin.

## admin_overview

```sql
api.admin_overview()
```

Devuelve: `jsonb`. Rol: chamba_admin.

## advance_contract

```sql
api.advance_contract(p_contract uuid, p_action text)
```

Devuelve: `text`. Rol: chamba_api.

## advance_order

```sql
api.advance_order(p_order uuid, p_action text)
```

Devuelve: `text`. Rol: chamba_api.

## approve_escrow_refund

```sql
api.approve_escrow_refund(p_contract uuid)
```

Devuelve: `void`. Rol: chamba_api.

## archive_address

```sql
api.archive_address(p_address uuid)
```

Devuelve: `void`. Rol: chamba_api.

## assign_role

```sql
api.assign_role(p_user uuid, p_role text, p_enable boolean, p_reason text)
```

Devuelve: `void`. Rol: chamba_admin.

## attach_kyc_file

```sql
api.attach_kyc_file(p_case uuid, p_file uuid, p_kind text)
```

Devuelve: `void`. Rol: chamba_api.

## attach_request_file

```sql
api.attach_request_file(p_request uuid, p_file uuid)
```

Devuelve: `void`. Rol: chamba_api.

## auth_account

```sql
api.auth_account(p_email text)
```

Devuelve: `jsonb`. Rol: chamba_auth.

## auth_challenge

```sql
api.auth_challenge(p_email text, p_purpose text, p_hash text)
```

Devuelve: `jsonb`. Rol: chamba_auth.

## auth_change_password

```sql
api.auth_change_password(p_user uuid, p_hash text)
```

Devuelve: `void`. Rol: chamba_auth.

## auth_consume_challenge

```sql
api.auth_consume_challenge(p_hash text, p_purpose text, p_password_hash text DEFAULT NULL::text)
```

Devuelve: `boolean`. Rol: chamba_auth.

## auth_mfa

```sql
api.auth_mfa(p_user uuid)
```

Devuelve: `jsonb`. Rol: chamba_auth.

## auth_principal

```sql
api.auth_principal(p_session uuid, p_user uuid)
```

Devuelve: `jsonb`. Rol: chamba_auth.

## auth_rate_limit

```sql
api.auth_rate_limit(p_bucket text, p_limit integer, p_seconds integer)
```

Devuelve: `boolean`. Rol: chamba_auth.

## auth_record_attempt

```sql
api.auth_record_attempt(p_user uuid, p_success boolean)
```

Devuelve: `void`. Rol: chamba_auth.

## auth_revoke_refresh

```sql
api.auth_revoke_refresh(p_hash text)
```

Devuelve: `void`. Rol: chamba_auth.

## auth_rotate_session

```sql
api.auth_rotate_session(p_old_hash text, p_new_hash text, p_expires timestamp with time zone)
```

Devuelve: `jsonb`. Rol: chamba_auth.

## auth_set_mfa

```sql
api.auth_set_mfa(p_user uuid, p_encrypted bytea, p_enabled boolean)
```

Devuelve: `void`. Rol: chamba_auth.

## available_requests

```sql
api.available_requests(p_limit integer DEFAULT 30, p_offset integer DEFAULT 0)
```

Devuelve: `TABLE(request_id uuid, category_id uuid, title text, description text, mode text, budget_min_minor bigint, budget_max_minor bigint, currency character, expires_at timestamp with time zone, attachments jsonb)`. Rol: chamba_api.

## cancel_contract

```sql
api.cancel_contract(p_contract uuid, p_reason text)
```

Devuelve: `void`. Rol: chamba_api.

## cancel_request

```sql
api.cancel_request(p_request uuid, p_reason text)
```

Devuelve: `void`. Rol: chamba_api.

## cancel_withdrawal

```sql
api.cancel_withdrawal(p_withdrawal uuid)
```

Devuelve: `void`. Rol: chamba_api.

## catalog

```sql
api.catalog()
```

Devuelve: `SETOF jsonb`. Rol: chamba_api.

## chat_messages

```sql
api.chat_messages(p_contract uuid, p_limit integer DEFAULT 50, p_before timestamp with time zone DEFAULT 'infinity'::timestamp with time zone, p_before_id uuid DEFAULT 'ffffffff-ffff-ffff-ffff-ffffffffffff'::uuid)
```

Devuelve: `SETOF jsonb`. Rol: chamba_api.

## claim_notifications

```sql
api.claim_notifications(p_limit integer DEFAULT 50)
```

Devuelve: `SETOF jsonb`. Rol: chamba_worker.

## configure_category

```sql
api.configure_category(p_slug text, p_name text, p_active boolean, p_parent uuid DEFAULT NULL::uuid)
```

Devuelve: `uuid`. Rol: chamba_admin.

## configure_plan

```sql
api.configure_plan(p_code text, p_name text, p_credits integer, p_price bigint, p_currency text, p_weight integer, p_positioning_days integer, p_marketplace_days integer, p_active boolean)
```

Devuelve: `uuid`. Rol: chamba_admin.

## configure_withdrawal_fee

```sql
api.configure_withdrawal_fee(p_currency text, p_basis_points integer, p_reason text)
```

Devuelve: `uuid`. Rol: chamba_admin.

## confirm_external_payment

```sql
api.confirm_external_payment(p_report uuid)
```

Devuelve: `void`. Rol: chamba_api.

## confirm_purchase

```sql
api.confirm_purchase(p_purchase uuid, p_provider text, p_event text, p_payment text, p_amount bigint, p_currency text, p_payload_hash text)
```

Devuelve: `uuid`. Rol: chamba_billing.

## confirm_topup

```sql
api.confirm_topup(p_topup uuid, p_provider text, p_event text, p_payment text, p_amount bigint, p_currency text, p_hash text)
```

Devuelve: `uuid`. Rol: chamba_billing.

## confirm_withdrawal

```sql
api.confirm_withdrawal(p_withdrawal uuid, p_provider text, p_event text, p_payment text, p_result text, p_net bigint, p_currency text, p_hash text)
```

Devuelve: `void`. Rol: chamba_billing.

## contract_contacts

```sql
api.contract_contacts(p_contract uuid)
```

Devuelve: `jsonb`. Rol: chamba_api.

## contract_detail

```sql
api.contract_detail(p_contract uuid)
```

Devuelve: `jsonb`. Rol: chamba_api.

## create_customer_request

```sql
api.create_customer_request(p_category uuid, p_delivery_mode text, p_title text, p_description text, p_currency text, p_address uuid, p_budget bigint, p_radius_m integer, p_expires_at timestamp with time zone, p_key uuid, p_files uuid[] DEFAULT '{}'::uuid[], p_protect boolean DEFAULT false)
```

Devuelve: `uuid`. Rol: chamba_api.

## create_order

```sql
api.create_order(p_store uuid, p_items jsonb, p_key uuid)
```

Devuelve: `uuid`. Rol: chamba_api.

## create_remote_request

```sql
api.create_remote_request(p_category uuid, p_mode text, p_target uuid, p_title text, p_requirements text, p_currency text, p_offered_price bigint, p_expires_at timestamp with time zone, p_key uuid)
```

Devuelve: `uuid`. Rol: chamba_api.

## create_request

```sql
api.create_request(p_category uuid, p_mode text, p_target uuid, p_title text, p_description text, p_address uuid, p_radius_m integer, p_currency text, p_budget_min bigint, p_budget_max bigint, p_expires_at timestamp with time zone, p_key uuid)
```

Devuelve: `uuid`. Rol: chamba_api.

## create_session

```sql
api.create_session(p_user uuid, p_refresh_hash text, p_device text, p_expires_at timestamp with time zone)
```

Devuelve: `uuid`. Rol: chamba_auth.

## create_topup

```sql
api.create_topup(p_currency text, p_amount bigint, p_key uuid)
```

Devuelve: `uuid`. Rol: chamba_api.

## dashboard

```sql
api.dashboard()
```

Devuelve: `jsonb`. Rol: chamba_api.

## deliver_in_app

```sql
api.deliver_in_app(p_outbox uuid, p_attempt integer)
```

Devuelve: `void`. Rol: chamba_worker.

## disable_payout_method

```sql
api.disable_payout_method(p_method uuid)
```

Devuelve: `void`. Rol: chamba_api.

## discover

```sql
api.discover(p_category uuid DEFAULT NULL::uuid, p_mode text DEFAULT NULL::text, p_limit integer DEFAULT 30, p_offset integer DEFAULT 0)
```

Devuelve: `SETOF jsonb`. Rol: chamba_api.

## dispute_contract

```sql
api.dispute_contract(p_contract uuid, p_reason text)
```

Devuelve: `uuid`. Rol: chamba_api.

## escrow_details

```sql
api.escrow_details(p_contract uuid)
```

Devuelve: `jsonb`. Rol: chamba_api.

## expire_requests

```sql
api.expire_requests()
```

Devuelve: `integer`. Rol: chamba_worker.

## external_payment_reports

```sql
api.external_payment_reports(p_contract uuid DEFAULT NULL::uuid, p_order uuid DEFAULT NULL::uuid)
```

Devuelve: `SETOF jsonb`. Rol: chamba_api.

## file_object_key

```sql
api.file_object_key(p_file uuid)
```

Devuelve: `text`. Rol: chamba_api.

## finish_notification

```sql
api.finish_notification(p_id uuid, p_attempt integer, p_success boolean, p_channel text, p_provider_id text, p_error text DEFAULT NULL::text)
```

Devuelve: `void`. Rol: chamba_worker.

## flag_money_incident

```sql
api.flag_money_incident(p_wallet uuid, p_provider text, p_event text, p_reason text)
```

Devuelve: `uuid`. Rol: chamba_billing.

## fund_contract

```sql
api.fund_contract(p_quote uuid, p_key uuid)
```

Devuelve: `uuid`. Rol: chamba_api.

## geo_municipalities

```sql
api.geo_municipalities(p_state text)
```

Devuelve: `SETOF jsonb`. Rol: chamba_api.

## geo_parish

```sql
api.geo_parish(p_parish text)
```

Devuelve: `jsonb`. Rol: chamba_api.

## geo_parishes

```sql
api.geo_parishes(p_municipality text)
```

Devuelve: `SETOF jsonb`. Rol: chamba_api.

## geo_states

```sql
api.geo_states()
```

Devuelve: `SETOF jsonb`. Rol: chamba_api.

## kyc_case

```sql
api.kyc_case(p_case uuid, p_reason text)
```

Devuelve: `jsonb`. Rol: chamba_admin.

## locate_parish

```sql
api.locate_parish(p_lon double precision, p_lat double precision)
```

Devuelve: `jsonb`. Rol: chamba_api.

## manage_ticket

```sql
api.manage_ticket(p_ticket uuid, p_status text, p_assignee uuid, p_note text)
```

Devuelve: `void`. Rol: chamba_admin.

## marketplace

```sql
api.marketplace(p_limit integer DEFAULT 50, p_offset integer DEFAULT 0)
```

Devuelve: `SETOF jsonb`. Rol: chamba_api.

## moderate_file

```sql
api.moderate_file(p_file uuid, p_approve boolean, p_reason text)
```

Devuelve: `void`. Rol: chamba_admin.

## moderate_request

```sql
api.moderate_request(p_request uuid, p_approve boolean, p_reason text)
```

Devuelve: `void`. Rol: chamba_admin.

## moderate_review

```sql
api.moderate_review(p_review uuid, p_status text, p_reason text)
```

Devuelve: `void`. Rol: chamba_admin.

## money_history

```sql
api.money_history(p_currency text, p_limit integer DEFAULT 100, p_before bigint DEFAULT '9223372036854775807'::bigint)
```

Devuelve: `SETOF jsonb`. Rol: chamba_api.

## money_wallet

```sql
api.money_wallet(p_currency text)
```

Devuelve: `jsonb`. Rol: chamba_api.

## my_account

```sql
api.my_account()
```

Devuelve: `jsonb`. Rol: chamba_api.

## my_contracts

```sql
api.my_contracts()
```

Devuelve: `SETOF jsonb`. Rol: chamba_api.

## my_credit_purchases

```sql
api.my_credit_purchases()
```

Devuelve: `SETOF jsonb`. Rol: chamba_api.

## my_favorites

```sql
api.my_favorites()
```

Devuelve: `SETOF jsonb`. Rol: chamba_api.

## my_files

```sql
api.my_files()
```

Devuelve: `SETOF jsonb`. Rol: chamba_api.

## my_kyc

```sql
api.my_kyc()
```

Devuelve: `SETOF jsonb`. Rol: chamba_api.

## my_notifications

```sql
api.my_notifications()
```

Devuelve: `SETOF jsonb`. Rol: chamba_api.

## my_orders

```sql
api.my_orders()
```

Devuelve: `SETOF jsonb`. Rol: chamba_api.

## my_payout_methods

```sql
api.my_payout_methods()
```

Devuelve: `SETOF jsonb`. Rol: chamba_api.

## my_products

```sql
api.my_products()
```

Devuelve: `SETOF jsonb`. Rol: chamba_api.

## my_professional

```sql
api.my_professional()
```

Devuelve: `jsonb`. Rol: chamba_api.

## my_proposals

```sql
api.my_proposals()
```

Devuelve: `SETOF jsonb`. Rol: chamba_api.

## my_requests

```sql
api.my_requests()
```

Devuelve: `SETOF jsonb`. Rol: chamba_api.

## my_sessions

```sql
api.my_sessions()
```

Devuelve: `SETOF jsonb`. Rol: chamba_api.

## my_tickets

```sql
api.my_tickets()
```

Devuelve: `SETOF jsonb`. Rol: chamba_api.

## my_topups

```sql
api.my_topups()
```

Devuelve: `SETOF jsonb`. Rol: chamba_api.

## my_wallet

```sql
api.my_wallet()
```

Devuelve: `jsonb`. Rol: chamba_api.

## my_withdrawals

```sql
api.my_withdrawals()
```

Devuelve: `SETOF jsonb`. Rol: chamba_api.

## navigate

```sql
api.navigate(p_contract uuid)
```

Devuelve: `jsonb`. Rol: chamba_api.

## nearby_professionals

```sql
api.nearby_professionals(p_lon double precision, p_lat double precision, p_category uuid DEFAULT NULL::uuid, p_radius_m integer DEFAULT 10000, p_verified_only boolean DEFAULT false, p_limit integer DEFAULT 100)
```

Devuelve: `SETOF jsonb`. Rol: chamba_api.

## open_ticket

```sql
api.open_ticket(p_category text, p_subject text, p_description text, p_contract uuid DEFAULT NULL::uuid, p_order uuid DEFAULT NULL::uuid)
```

Devuelve: `uuid`. Rol: chamba_api.

## order_delivery

```sql
api.order_delivery(p_order uuid)
```

Devuelve: `jsonb`. Rol: chamba_api.

## payment_preferences

```sql
api.payment_preferences()
```

Devuelve: `jsonb`. Rol: chamba_api.

## prepare_withdrawal

```sql
api.prepare_withdrawal(p_withdrawal uuid)
```

Devuelve: `jsonb`. Rol: chamba_billing.

## professional_assets

```sql
api.professional_assets()
```

Devuelve: `jsonb`. Rol: chamba_api.

## professional_profile

```sql
api.professional_profile(p_professional uuid)
```

Devuelve: `jsonb`. Rol: chamba_api.

## professional_reviews

```sql
api.professional_reviews(p_professional uuid, p_limit integer DEFAULT 30, p_offset integer DEFAULT 0)
```

Devuelve: `SETOF jsonb`. Rol: chamba_api.

## propose_terms

```sql
api.propose_terms(p_contract uuid, p_price bigint, p_method text, p_terms text)
```

Devuelve: `uuid`. Rol: chamba_api.

## protect_service_request

```sql
api.protect_service_request(p_request uuid)
```

Devuelve: `void`. Rol: chamba_api.

## publish_request

```sql
api.publish_request(p_request uuid)
```

Devuelve: `void`. Rol: chamba_api.

## quote_contract_funding

```sql
api.quote_contract_funding(p_contract uuid)
```

Devuelve: `jsonb`. Rol: chamba_api.

## quote_withdrawal

```sql
api.quote_withdrawal(p_currency text, p_method uuid, p_amount bigint DEFAULT NULL::bigint)
```

Devuelve: `jsonb`. Rol: chamba_api.

## rate_contract

```sql
api.rate_contract(p_contract uuid, p_rating smallint, p_recommends boolean, p_comment text)
```

Devuelve: `uuid`. Rol: chamba_api.

## read_notification

```sql
api.read_notification(p_notification uuid)
```

Devuelve: `void`. Rol: chamba_api.

## refund_contract_credit

```sql
api.refund_contract_credit(p_contract uuid, p_ticket uuid, p_reason text)
```

Devuelve: `bigint`. Rol: chamba_admin.

## register_upload

```sql
api.register_upload(p_owner uuid, p_object_key text, p_mime text, p_bytes bigint, p_sha256 text, p_purpose text, p_exif_removed boolean)
```

Devuelve: `uuid`. Rol: chamba_worker.

## register_user

```sql
api.register_user(p_email text, p_name text, p_argon2id_hash text)
```

Devuelve: `uuid`. Rol: chamba_auth.

## release_contract_funds

```sql
api.release_contract_funds(p_contract uuid, p_delivery uuid, p_key uuid)
```

Devuelve: `uuid`. Rol: chamba_api.

## remove_contact

```sql
api.remove_contact(p_contact uuid)
```

Devuelve: `void`. Rol: chamba_api.

## remove_request_image

```sql
api.remove_request_image(p_request uuid, p_file uuid)
```

Devuelve: `void`. Rol: chamba_api.

## reply_ticket

```sql
api.reply_ticket(p_ticket uuid, p_body text)
```

Devuelve: `uuid`. Rol: chamba_api.

## report_external_payment

```sql
api.report_external_payment(p_contract uuid, p_order uuid, p_amount bigint, p_currency text, p_method text)
```

Devuelve: `uuid`. Rol: chamba_api.

## report_position

```sql
api.report_position(p_contract uuid, p_lon double precision, p_lat double precision, p_accuracy integer)
```

Devuelve: `void`. Rol: chamba_api.

## request_delivery_changes

```sql
api.request_delivery_changes(p_delivery uuid, p_reason text)
```

Devuelve: `void`. Rol: chamba_api.

## request_escrow_refund

```sql
api.request_escrow_refund(p_contract uuid, p_reason text)
```

Devuelve: `uuid`. Rol: chamba_api.

## request_offering

```sql
api.request_offering(p_offering uuid, p_description text, p_currency text, p_address uuid, p_expires_at timestamp with time zone, p_key uuid, p_files uuid[] DEFAULT '{}'::uuid[], p_protect boolean DEFAULT false)
```

Devuelve: `uuid`. Rol: chamba_api.

## request_proposals

```sql
api.request_proposals(p_request uuid)
```

Devuelve: `SETOF jsonb`. Rol: chamba_api.

## request_withdrawal

```sql
api.request_withdrawal(p_quote uuid, p_key uuid)
```

Devuelve: `uuid`. Rol: chamba_api.

## resolve_escrow_refund

```sql
api.resolve_escrow_refund(p_contract uuid, p_ticket uuid, p_reason text)
```

Devuelve: `void`. Rol: chamba_admin.

## resolve_session

```sql
api.resolve_session(p_refresh_hash text)
```

Devuelve: `uuid`. Rol: chamba_auth.

## respond_review

```sql
api.respond_review(p_review uuid, p_response text)
```

Devuelve: `void`. Rol: chamba_api.

## review_kyc

```sql
api.review_kyc(p_case uuid, p_status text, p_reason text, p_expires_at timestamp with time zone DEFAULT NULL::timestamp with time zone)
```

Devuelve: `void`. Rol: chamba_admin.

## revoke_location

```sql
api.revoke_location(p_contract uuid)
```

Devuelve: `void`. Rol: chamba_api.

## revoke_session

```sql
api.revoke_session(p_session uuid)
```

Devuelve: `void`. Rol: chamba_api.

## save_contact

```sql
api.save_contact(p_kind text, p_label text, p_value text, p_share boolean DEFAULT true)
```

Devuelve: `uuid`. Rol: chamba_api.

## search_professionals

```sql
api.search_professionals(p_address uuid, p_category uuid, p_radius_m integer DEFAULT 10000, p_sort text DEFAULT 'distance'::text, p_verified_only boolean DEFAULT false, p_currency text DEFAULT NULL::text, p_max_price bigint DEFAULT NULL::bigint, p_min_rating numeric DEFAULT 0, p_limit integer DEFAULT 30, p_offset integer DEFAULT 0)
```

Devuelve: `TABLE(professional_id uuid, profile jsonb, distance_m bigint, verified boolean, sponsored boolean)`. Rol: chamba_api.

## search_remote_professionals

```sql
api.search_remote_professionals(p_category uuid, p_currency text DEFAULT NULL::text, p_max_price bigint DEFAULT NULL::bigint, p_verified_only boolean DEFAULT false, p_limit integer DEFAULT 30, p_offset integer DEFAULT 0)
```

Devuelve: `SETOF jsonb`. Rol: chamba_api.

## send_attachment

```sql
api.send_attachment(p_contract uuid, p_file uuid, p_key uuid)
```

Devuelve: `uuid`. Rol: chamba_api.

## send_message

```sql
api.send_message(p_contract uuid, p_body text, p_key uuid)
```

Devuelve: `uuid`. Rol: chamba_api.

## service_feed

```sql
api.service_feed(p_delivery_mode text, p_limit integer DEFAULT 30, p_offset integer DEFAULT 0)
```

Devuelve: `SETOF jsonb`. Rol: chamba_api.

## set_branch_hours

```sql
api.set_branch_hours(p_branch uuid, p_weekday smallint, p_opens time without time zone, p_closes time without time zone)
```

Devuelve: `uuid`. Rol: chamba_api.

## set_coverage

```sql
api.set_coverage(p_branch uuid, p_category uuid, p_radius_m integer)
```

Devuelve: `uuid`. Rol: chamba_api.

## set_favorite

```sql
api.set_favorite(p_professional uuid, p_enabled boolean)
```

Devuelve: `void`. Rol: chamba_api.

## set_offering_delivery

```sql
api.set_offering_delivery(p_offering uuid, p_mode text)
```

Devuelve: `void`. Rol: chamba_api.

## set_payment_preference

```sql
api.set_payment_preference(p_required boolean)
```

Devuelve: `void`. Rol: chamba_api.

## set_user_status

```sql
api.set_user_status(p_user uuid, p_status text, p_reason text)
```

Devuelve: `void`. Rol: chamba_admin.

## share_location

```sql
api.share_location(p_contract uuid, p_hours integer DEFAULT 8)
```

Devuelve: `uuid`. Rol: chamba_api.

## share_order_delivery

```sql
api.share_order_delivery(p_order uuid, p_name text, p_phone text, p_address_ciphertext bytea, p_key_id text)
```

Devuelve: `void`. Rol: chamba_api.

## start_purchase

```sql
api.start_purchase(p_plan uuid, p_key uuid)
```

Devuelve: `uuid`. Rol: chamba_api.

## store_products

```sql
api.store_products(p_store uuid)
```

Devuelve: `SETOF jsonb`. Rol: chamba_api.

## submit_delivery

```sql
api.submit_delivery(p_contract uuid, p_summary text, p_key uuid, p_files uuid[] DEFAULT '{}'::uuid[])
```

Devuelve: `uuid`. Rol: chamba_api.

## submit_kyc

```sql
api.submit_kyc(p_provider text, p_external_reference text)
```

Devuelve: `uuid`. Rol: chamba_api.

## submit_proposal

```sql
api.submit_proposal(p_request uuid, p_message text, p_price_minor bigint, p_expires_at timestamp with time zone)
```

Devuelve: `uuid`. Rol: chamba_api.

## support_ticket

```sql
api.support_ticket(p_ticket uuid, p_reason text)
```

Devuelve: `jsonb`. Rol: chamba_admin.

## ticket_thread

```sql
api.ticket_thread(p_ticket uuid)
```

Devuelve: `jsonb`. Rol: chamba_api.

## update_account

```sql
api.update_account(p_name text, p_locale text DEFAULT 'es'::text, p_timezone text DEFAULT 'UTC'::text)
```

Devuelve: `void`. Rol: chamba_api.

## update_offering

```sql
api.update_offering(p_offering uuid, p_title text, p_description text, p_price bigint, p_active boolean)
```

Devuelve: `void`. Rol: chamba_api.

## update_product

```sql
api.update_product(p_product uuid, p_name text, p_description text, p_price bigint, p_stock integer, p_status text)
```

Devuelve: `void`. Rol: chamba_api.

## update_professional

```sql
api.update_professional(p_name text, p_bio text, p_years integer, p_accepts boolean)
```

Devuelve: `void`. Rol: chamba_api.

## verify_payout_method

```sql
api.verify_payout_method(p_method uuid, p_owner uuid, p_provider text, p_token text)
```

Devuelve: `void`. Rol: chamba_billing.

## withdraw_proposal

```sql
api.withdraw_proposal(p_proposal uuid)
```

Devuelve: `void`. Rol: chamba_api.

## worker_housekeeping

```sql
api.worker_housekeeping()
```

Devuelve: `void`. Rol: chamba_worker.
