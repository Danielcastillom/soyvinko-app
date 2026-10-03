# Cobertura de operaciones SQL y HTTP

La lista estática del servidor se genera con `python3 scripts/export_http_registry.py`. La entrada HTTP nunca decide el nombre SQL, actor ni rol de conexión.

| Operación SQL | Módulo / frontera | Acceso HTTP |
|---|---|---|
| `accept_direct_request` | requests | `POST /api/v1/requests/accept-direct-request` |
| `accept_proposal` | requests | `POST /api/v1/requests/accept-proposal` |
| `accept_terms` | contracts | `POST /api/v1/contracts/accept-terms` |
| `activate_professional` | professionals | `POST /api/v1/professionals/activate-professional` |
| `activate_store` | marketplace | `POST /api/v1/marketplace/activate-store` |
| `active_plans` | wallet | `GET /api/v1/wallet/active-plans` |
| `add_address` | Compatibilidad interna | Sin ruta HTTP; las altas web usan `add_address_ve` y validan la jerarquía venezolana. |
| `add_address_ve` | account | `POST /api/v1/account/add-address` |
| `add_branch` | Compatibilidad interna | Sin ruta HTTP; las altas web usan `add_branch_ve` y validan la jerarquía venezolana. |
| `add_branch_ve` | professionals | `POST /api/v1/professionals/add-branch` |
| `add_credential` | professionals | `POST /api/v1/professionals/add-credential` |
| `add_offering` | professionals | `POST /api/v1/professionals/add-offering` |
| `add_payout_method` | Interna, sin ruta SQL directa | Adaptador `/api/v1/integrations/*`; en pagos solo sandbox local. |
| `add_portfolio` | professionals | `POST /api/v1/professionals/add-portfolio` |
| `add_product` | marketplace | `POST /api/v1/marketplace/add-product` |
| `add_product_image` | marketplace | `POST /api/v1/marketplace/add-product-image` |
| `add_professional_contact` | professionals | `POST /api/v1/professionals/add-professional-contact` |
| `admin_file_key` | Interna, sin ruta SQL directa | Adaptador `/api/v1/files/*`; valida almacenamiento, acceso y metadatos. |
| `admin_list` | admin | `GET /api/v1/admin/admin-list` |
| `admin_overview` | admin | `GET /api/v1/admin/admin-overview` |
| `advance_contract` | contracts | `POST /api/v1/contracts/advance-contract` |
| `advance_order` | marketplace | `POST /api/v1/marketplace/advance-order` |
| `approve_escrow_refund` | contracts | `POST /api/v1/contracts/approve-escrow-refund` |
| `archive_address` | account | `POST /api/v1/account/archive-address` |
| `assign_role` | admin | `POST /api/v1/admin/assign-role` |
| `attach_kyc_file` | support | `POST /api/v1/support/attach-kyc-file` |
| `attach_request_file` | requests | `POST /api/v1/requests/attach-request-file` |
| `auth_account` | Interna, sin ruta SQL directa | Adaptador `/api/v1/auth/*`; autenticación del servidor. |
| `auth_challenge` | Interna, sin ruta SQL directa | Adaptador `/api/v1/auth/*`; autenticación del servidor. |
| `auth_change_password` | Interna, sin ruta SQL directa | Adaptador `/api/v1/auth/*`; autenticación del servidor. |
| `auth_consume_challenge` | Interna, sin ruta SQL directa | Adaptador `/api/v1/auth/*`; autenticación del servidor. |
| `auth_mfa` | Interna, sin ruta SQL directa | Adaptador `/api/v1/auth/*`; autenticación del servidor. |
| `auth_principal` | Interna, sin ruta SQL directa | Adaptador `/api/v1/auth/*`; autenticación del servidor. |
| `auth_rate_limit` | Interna, sin ruta SQL directa | Adaptador `/api/v1/auth/*`; autenticación del servidor. |
| `auth_record_attempt` | Interna, sin ruta SQL directa | Adaptador `/api/v1/auth/*`; autenticación del servidor. |
| `auth_revoke_refresh` | Interna, sin ruta SQL directa | Adaptador `/api/v1/auth/*`; autenticación del servidor. |
| `auth_rotate_session` | Interna, sin ruta SQL directa | Adaptador `/api/v1/auth/*`; autenticación del servidor. |
| `auth_set_mfa` | Interna, sin ruta SQL directa | Adaptador `/api/v1/auth/*`; autenticación del servidor. |
| `available_requests` | requests | `GET /api/v1/requests/available-requests` |
| `cancel_contract` | contracts | `POST /api/v1/contracts/cancel-contract` |
| `cancel_request` | requests | `POST /api/v1/requests/cancel-request` |
| `cancel_withdrawal` | wallet | `POST /api/v1/wallet/cancel-withdrawal` |
| `catalog` | public | `GET /api/v1/public/catalog` |
| `chat_messages` | contracts | `GET /api/v1/contracts/chat-messages` |
| `claim_notifications` | Interna, sin ruta SQL directa | Trabajador Python; sin acceso desde navegador. |
| `configure_category` | admin | `POST /api/v1/admin/configure-category` |
| `configure_plan` | admin | `POST /api/v1/admin/configure-plan` |
| `configure_withdrawal_fee` | admin | `POST /api/v1/admin/configure-withdrawal-fee` |
| `confirm_external_payment` | contracts | `POST /api/v1/contracts/confirm-external-payment` |
| `confirm_purchase` | Interna, sin ruta SQL directa | Adaptador `/api/v1/integrations/*`; en pagos solo sandbox local. |
| `confirm_topup` | Interna, sin ruta SQL directa | Adaptador `/api/v1/integrations/*`; en pagos solo sandbox local. |
| `confirm_withdrawal` | Interna, sin ruta SQL directa | Adaptador `/api/v1/integrations/*`; en pagos solo sandbox local. |
| `contract_contacts` | contracts | `GET /api/v1/contracts/contract-contacts` |
| `contract_detail` | contracts | `GET /api/v1/contracts/contract-detail` |
| `create_customer_request` | requests | `POST /api/v1/requests/create-customer-request` |
| `create_order` | marketplace | `POST /api/v1/marketplace/create-order` |
| `create_remote_request` | requests | `POST /api/v1/requests/create-remote-request` |
| `create_request` | requests | `POST /api/v1/requests/create-request` |
| `create_session` | Interna, sin ruta SQL directa | Adaptador `/api/v1/auth/*`; autenticación del servidor. |
| `create_topup` | wallet | `POST /api/v1/wallet/create-topup` |
| `dashboard` | account | `GET /api/v1/account/dashboard` |
| `deliver_in_app` | Interna, sin ruta SQL directa | Trabajador Python; sin acceso desde navegador. |
| `disable_payout_method` | wallet | `POST /api/v1/wallet/disable-payout-method` |
| `discover` | public | `GET /api/v1/public/discover` |
| `dispute_contract` | contracts | `POST /api/v1/contracts/dispute-contract` |
| `escrow_details` | contracts | `GET /api/v1/contracts/escrow-details` |
| `expire_requests` | Interna, sin ruta SQL directa | Trabajador Python; sin acceso desde navegador. |
| `external_payment_reports` | contracts | `GET /api/v1/contracts/external-payment-reports` |
| `file_object_key` | Interna, sin ruta SQL directa | Adaptador `/api/v1/files/*`; valida almacenamiento, acceso y metadatos. |
| `finish_notification` | Interna, sin ruta SQL directa | Trabajador Python; sin acceso desde navegador. |
| `flag_money_incident` | Interna, sin ruta SQL directa | Frontera de conciliación bancaria; reservada al adaptador de proveedor futuro, sin endpoint de usuario. |
| `fund_contract` | contracts | `POST /api/v1/contracts/fund-contract` |
| `geo_municipalities` | public | `GET /api/v1/public/geo-municipalities` |
| `geo_parish` | public | `GET /api/v1/public/geo-parish` |
| `geo_parishes` | public | `GET /api/v1/public/geo-parishes` |
| `geo_states` | public | `GET /api/v1/public/geo-states` |
| `kyc_case` | admin | `POST /api/v1/admin/kyc-case` |
| `locate_parish` | public | `POST /api/v1/public/locate-parish` |
| `manage_ticket` | admin | `POST /api/v1/admin/manage-ticket` |
| `marketplace` | public | `GET /api/v1/public/marketplace` |
| `moderate_file` | admin | `POST /api/v1/admin/moderate-file` |
| `moderate_request` | admin | `POST /api/v1/admin/moderate-request` |
| `moderate_review` | admin | `POST /api/v1/admin/moderate-review` |
| `money_history` | wallet | `GET /api/v1/wallet/money-history` |
| `money_wallet` | wallet | `GET /api/v1/wallet/money-wallet` |
| `my_account` | account | `GET /api/v1/account/my-account` |
| `my_contracts` | contracts | `GET /api/v1/contracts/my-contracts` |
| `my_credit_purchases` | wallet | `GET /api/v1/wallet/my-credit-purchases` |
| `my_favorites` | professionals | `GET /api/v1/professionals/my-favorites` |
| `my_files` | support | `GET /api/v1/support/my-files` |
| `my_kyc` | support | `GET /api/v1/support/my-kyc` |
| `my_notifications` | account | `GET /api/v1/account/my-notifications` |
| `my_orders` | marketplace | `GET /api/v1/marketplace/my-orders` |
| `my_payout_methods` | wallet | `GET /api/v1/wallet/my-payout-methods` |
| `my_products` | marketplace | `GET /api/v1/marketplace/my-products` |
| `my_professional` | professionals | `GET /api/v1/professionals/my-professional` |
| `my_proposals` | requests | `GET /api/v1/requests/my-proposals` |
| `my_requests` | requests | `GET /api/v1/requests/my-requests` |
| `my_sessions` | account | `GET /api/v1/account/my-sessions` |
| `my_tickets` | support | `GET /api/v1/support/my-tickets` |
| `my_topups` | wallet | `GET /api/v1/wallet/my-topups` |
| `my_wallet` | wallet | `GET /api/v1/wallet/my-wallet` |
| `my_withdrawals` | wallet | `GET /api/v1/wallet/my-withdrawals` |
| `navigate` | contracts | `GET /api/v1/contracts/navigate` |
| `nearby_professionals` | public | `POST /api/v1/public/nearby-professionals` |
| `open_ticket` | support | `POST /api/v1/support/open-ticket` |
| `order_delivery` | Interna, sin ruta SQL directa | Adaptador `/api/v1/integrations/*`; en pagos solo sandbox local. |
| `payment_preferences` | account | `GET /api/v1/account/payment-preferences` |
| `prepare_withdrawal` | Interna, sin ruta SQL directa | Adaptador `/api/v1/integrations/*`; en pagos solo sandbox local. |
| `professional_assets` | professionals | `GET /api/v1/professionals/professional-assets` |
| `professional_profile` | professionals | `GET /api/v1/professionals/professional-profile` |
| `professional_reviews` | professionals | `GET /api/v1/professionals/professional-reviews` |
| `propose_terms` | contracts | `POST /api/v1/contracts/propose-terms` |
| `protect_service_request` | requests | `POST /api/v1/requests/protect-service-request` |
| `publish_request` | requests | `POST /api/v1/requests/publish-request` |
| `quote_contract_funding` | contracts | `POST /api/v1/contracts/quote-contract-funding` |
| `quote_withdrawal` | wallet | `POST /api/v1/wallet/quote-withdrawal` |
| `rate_contract` | contracts | `POST /api/v1/contracts/rate-contract` |
| `read_notification` | account | `POST /api/v1/account/read-notification` |
| `refund_contract_credit` | admin | `POST /api/v1/admin/refund-contract-credit` |
| `register_upload` | Interna, sin ruta SQL directa | Adaptador `/api/v1/files/*`; valida almacenamiento, acceso y metadatos. |
| `register_user` | Interna, sin ruta SQL directa | Adaptador `/api/v1/auth/*`; autenticación del servidor. |
| `release_contract_funds` | contracts | `POST /api/v1/contracts/release-contract-funds` |
| `remove_contact` | account | `POST /api/v1/account/remove-contact` |
| `remove_request_image` | requests | `POST /api/v1/requests/remove-request-image` |
| `reply_ticket` | support | `POST /api/v1/support/reply-ticket` |
| `report_external_payment` | contracts | `POST /api/v1/contracts/report-external-payment` |
| `report_position` | contracts | `POST /api/v1/contracts/report-position` |
| `request_delivery_changes` | contracts | `POST /api/v1/contracts/request-delivery-changes` |
| `request_escrow_refund` | contracts | `POST /api/v1/contracts/request-escrow-refund` |
| `request_offering` | requests | `POST /api/v1/requests/request-offering` |
| `request_proposals` | requests | `GET /api/v1/requests/request-proposals` |
| `request_withdrawal` | wallet | `POST /api/v1/wallet/request-withdrawal` |
| `resolve_escrow_refund` | admin | `POST /api/v1/admin/resolve-escrow-refund` |
| `resolve_session` | Interna, sin ruta SQL directa | Compatibilidad SQL; sustituida por rotación de sesión y validación JWT. |
| `respond_review` | professionals | `POST /api/v1/professionals/respond-review` |
| `review_kyc` | admin | `POST /api/v1/admin/review-kyc` |
| `revoke_location` | contracts | `POST /api/v1/contracts/revoke-location` |
| `revoke_session` | account | `POST /api/v1/account/revoke-session` |
| `save_contact` | account | `POST /api/v1/account/save-contact` |
| `search_professionals` | professionals | `GET /api/v1/professionals/search-professionals` |
| `search_remote_professionals` | professionals | `GET /api/v1/professionals/search-remote-professionals` |
| `send_attachment` | contracts | `POST /api/v1/contracts/send-attachment` |
| `send_message` | contracts | `POST /api/v1/contracts/send-message` |
| `service_feed` | requests | `GET /api/v1/requests/service-feed` |
| `set_branch_hours` | professionals | `POST /api/v1/professionals/set-branch-hours` |
| `set_coverage` | professionals | `POST /api/v1/professionals/set-coverage` |
| `set_favorite` | professionals | `POST /api/v1/professionals/set-favorite` |
| `set_offering_delivery` | professionals | `POST /api/v1/professionals/set-offering-delivery` |
| `set_payment_preference` | account | `POST /api/v1/account/set-payment-preference` |
| `set_user_status` | admin | `POST /api/v1/admin/set-user-status` |
| `share_location` | contracts | `POST /api/v1/contracts/share-location` |
| `share_order_delivery` | Interna, sin ruta SQL directa | Adaptador `/api/v1/integrations/*`; en pagos solo sandbox local. |
| `start_purchase` | wallet | `POST /api/v1/wallet/start-purchase` |
| `store_products` | marketplace | `GET /api/v1/marketplace/store-products` |
| `submit_delivery` | contracts | `POST /api/v1/contracts/submit-delivery` |
| `submit_kyc` | support | `POST /api/v1/support/submit-kyc` |
| `submit_proposal` | requests | `POST /api/v1/requests/submit-proposal` |
| `support_ticket` | admin | `POST /api/v1/admin/support-ticket` |
| `ticket_thread` | support | `GET /api/v1/support/ticket-thread` |
| `update_account` | account | `POST /api/v1/account/update-account` |
| `update_offering` | professionals | `POST /api/v1/professionals/update-offering` |
| `update_product` | marketplace | `POST /api/v1/marketplace/update-product` |
| `update_professional` | professionals | `POST /api/v1/professionals/update-professional` |
| `verify_payout_method` | Interna, sin ruta SQL directa | Adaptador `/api/v1/integrations/*`; en pagos solo sandbox local. |
| `withdraw_proposal` | requests | `POST /api/v1/requests/withdraw-proposal` |
| `worker_housekeeping` | Interna, sin ruta SQL directa | Trabajador Python; sin acceso desde navegador. |
