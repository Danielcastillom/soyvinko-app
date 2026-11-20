#!/usr/bin/env python3
"""Generación de contratos revisables; no introspección ni SQL arbitrario en runtime."""
import json
from pathlib import Path
import db
import argparse

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--database', help='Base aislada para revisar contratos antes de publicar')
args = parser.parse_args()

query = """SELECT jsonb_agg(jsonb_build_object('name',p.proname,'set',p.proretset,
 'returns',format_type(p.prorettype,NULL),'volatility',p.provolatile,
 'args',(SELECT coalesce(jsonb_agg(jsonb_build_object('name',p.proargnames[i+1],
 'type',format_type(p.proargtypes[i],NULL),'required',i<p.pronargs-p.pronargdefaults) ORDER BY i),'[]') FROM generate_series(0,p.pronargs-1) i),
 'roles',(SELECT jsonb_agg(r.rolname) FROM pg_roles r WHERE r.rolname IN('chamba_api','chamba_auth','chamba_admin','chamba_worker','chamba_billing') AND has_function_privilege(r.oid,p.oid,'EXECUTE')))
 ORDER BY p.proname) FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace WHERE n.nspname='api';"""
specs = json.loads(db.sql(query, database=args.database, capture=True).stdout)
# Each module owns an explicit list; additions require regeneration + code review.
groups = {
 'account': 'my_account dashboard save_contact remove_contact add_address archive_address update_account set_avatar my_sessions revoke_session payment_preferences set_payment_preference my_notifications read_notification',
 'professionals': 'activate_professional update_professional my_professional professional_profile professional_reviews search_professionals search_remote_professionals add_branch add_offering set_coverage add_professional_contact set_offering_delivery add_portfolio add_credential set_branch_hours update_offering set_favorite my_favorites respond_review professional_assets remove_offering remove_branch remove_portfolio_item remove_credential remove_professional_contact',
 'requests': 'create_request create_remote_request protect_service_request publish_request my_requests available_requests service_feed submit_proposal my_proposals request_proposals accept_direct_request accept_proposal withdraw_proposal cancel_request attach_request_file',
 'contracts': 'my_contracts contract_detail contract_contacts chat_messages send_message send_attachment share_location update_shared_location location_status revoke_location navigate report_position advance_contract cancel_contract rate_contract propose_terms accept_terms escrow_details quote_contract_funding fund_contract submit_delivery request_delivery_changes release_contract_funds request_escrow_refund approve_escrow_refund dispute_contract report_external_payment confirm_external_payment external_payment_reports',
 'wallet': 'my_wallet active_plans start_purchase my_credit_purchases money_wallet money_history create_topup my_topups my_payout_methods disable_payout_method quote_withdrawal request_withdrawal cancel_withdrawal my_withdrawals get_vinko_exchange_rates convert_money_to_vinkos',
 'marketplace': 'store_products my_orders activate_store add_product create_order advance_order my_products update_product add_product_image remove_product_image pay_order_with_wallet',
 # submit_kyc no tiene ruta genérica: solo /support/kyc-personal (OCR y validaciones).
 'support': 'open_ticket my_tickets ticket_thread reply_ticket my_kyc attach_kyc_file my_files',
 'admin': 'admin_overview admin_list configure_category moderate_request moderate_review moderate_file review_kyc set_user_status assign_role support_ticket manage_ticket configure_plan configure_withdrawal_fee configure_vinko_exchange_rate refund_contract_credit resolve_escrow_refund kyc_case set_store_status set_store_plan set_professional_plan admin_advance_order',
 'public': 'catalog discover marketplace geo_states geo_municipalities geo_parishes geo_parish locate_parish nearby_professionals professional_map_markers',
 'transport': 'create_shipment available_shipments take_shipment update_shipment_location advance_shipment shipment_tracking my_shipments',
}
groups['account'] = groups['account'].replace('add_address ', 'add_address_ve ')
groups['requests'] += ' request_offering create_customer_request remove_request_image'
groups['professionals'] = groups['professionals'].replace('add_branch ', 'add_branch_ve ')
for s in specs:
    s['module'] = next((m for m, names in groups.items() if s['name'] in names.split()), None)
    s['method'] = 'GET' if s['volatility'] == 's' or s['name'] in {'money_wallet','navigate','contract_contacts','escrow_details','payment_preferences'} else 'POST'
    if s['name'] in {'locate_parish', 'nearby_professionals', 'professional_map_markers'}:
        s['method'] = 'POST'  # Precise coordinates never appear in URL/access logs.
    s['path'] = '/api/v1/' + (s['module'] or 'internal') + '/' + s['name'].replace('_','-')
    if s['name'] in {'add_address_ve','add_branch_ve'}:
        s['path'] = s['path'].removesuffix('-ve')
root = Path(__file__).resolve().parents[1]
(root/'backend/app/operations.json').write_text(json.dumps(specs, indent=2)+'\n')
(root/'frontend/src/operations.json').write_text(json.dumps({s['name']: {'path': s['path'], 'method': s['method']} for s in specs if s['module'] is not None}, indent=2)+'\n')
print(f'{sum(s["module"] is not None for s in specs)} operaciones HTTP y {sum(s["module"] is None for s in specs)} internas/adaptadas.')
