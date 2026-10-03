"""Servicios remotos, custodia contable y comisión exclusivamente al retirar."""
from concurrent.futures import ThreadPoolExecutor
import json
import threading
import unittest
import uuid


def build_suite(h):
    call, execute, scalar, q = h.call, h.execute, h.scalar, h.q

    class MoneyTests(unittest.TestCase):
        setUp = h.DatabaseTests.setUp
        user = h.DatabaseTests.user
        prepare_professional = h.DatabaseTests.prepare_professional
        request = h.DatabaseTests.request
        contract = h.DatabaseTests.contract
        plan_and_purchase = h.DatabaseTests.plan_and_purchase
        pay = h.DatabaseTests.pay
        expect_denied = h.DatabaseTests.expect_denied

        def balance(self, user=None, currency="VES"):
            return json.loads(call("money_wallet", currency, actor=user or self.customer))

        def topup(self, amount=100000, user=None, currency="VES", pay=True):
            user = user or self.customer
            t = call("create_topup", currency, amount, str(uuid.uuid4()), actor=user)
            if pay:
                call("confirm_topup", t, "money-test", uuid.uuid4().hex, uuid.uuid4().hex, amount, currency, "a"*64, role="chamba_billing")
            return t

        def remote_request(self, mode="direct", amount=30000):
            self.remote_category = scalar("SELECT id FROM app.categories WHERE slug='diseno-grafico'")
            offering = call("add_offering", self.remote_category, "Diseño de logotipo", "VES", amount, actor=self.professional)
            call("set_offering_delivery", offering, "remote", actor=self.professional)
            req = call("create_remote_request", self.remote_category, mode, self.professional if mode == "direct" else None,
                       "Diseñar un logotipo", "Entregar el logotipo vectorial y dos variantes en color.", "VES", amount,
                       "2099-01-01", str(uuid.uuid4()), actor=self.customer)
            call("moderate_request", req, True, "Requisitos revisados", actor=self.admin, role="chamba_admin")
            call("publish_request", req, actor=self.customer)
            return req

        def remote_contract(self, amount=30000, funded=True):
            req = self.remote_request(amount=amount)
            contract = call("accept_direct_request", req, actor=self.professional)
            if funded:
                self.topup(amount)
                quote = json.loads(call("quote_contract_funding", contract, actor=self.customer))
                call("fund_contract", quote["id"], str(uuid.uuid4()), actor=self.customer)
            return contract

        def delivery(self, contract):
            call("advance_contract", contract, "start", actor=self.professional)
            return call("submit_delivery", contract, "Entrego el diseño y las variantes solicitadas.", str(uuid.uuid4()), actor=self.professional)

        def payout_setup(self, user=None, bps=200, currency="VES"):
            user = user or self.customer
            k = call("submit_kyc", "manual", uuid.uuid4().hex, actor=user)
            call("review_kyc", k, "approved", "Identidad revisada", "2099-01-01", actor=self.admin, role="chamba_admin")
            token = uuid.uuid4().hex
            m = call("add_payout_method", "money-test", token, "Mi cuenta", "****1234", currency, actor=user)
            call("verify_payout_method", m, user, "money-test", token, role="chamba_billing")
            call("configure_withdrawal_fee", currency, bps, "Comisión exclusiva al retirar", actor=self.admin, role="chamba_admin")
            return m

        def withdrawal(self, method, amount=None, user=None, currency="VES"):
            user = user or self.customer
            quote = json.loads(call("quote_withdrawal", currency, method, amount, actor=user))
            wd = call("request_withdrawal", quote["id"], str(uuid.uuid4()), actor=user)
            return wd,quote

        def settle_withdrawal(self, wd, result="paid", event=None):
            prepared = json.loads(call("prepare_withdrawal", wd, role="chamba_billing"))
            call("confirm_withdrawal", wd, "money-test", event or uuid.uuid4().hex, "payout-"+wd,
                 result, prepared["net_minor"], prepared["currency"], "b"*64, role="chamba_billing")
            return prepared

        def test_01_remote_without_location_or_branch(self):
            req = self.remote_request()
            self.assertEqual(scalar(f"SELECT count(*) FROM app.request_locations WHERE request_id={q(req)}"), "0")
            self.assertIn(req, call("service_feed", "remote", actor=self.professional))
            remote = call("search_remote_professionals", self.remote_category, actor=self.customer)
            self.assertIn(self.professional, remote)
            self.assertEqual(call("search_professionals", self.address, self.remote_category, actor=self.customer), "")
            contract = call("accept_direct_request", req, actor=self.professional)
            self.assertEqual(scalar(f"SELECT delivery_mode||':'||payment_protection FROM app.contracts WHERE id={q(contract)}"), "remote:guaranteed")
            self.expect_denied(lambda: call("share_location", contract, actor=self.customer), "remote_service_has_no_location")

        def test_02_profile_preferences_protect_onsite_contract(self):
            call("set_payment_preference", True, actor=self.professional)
            self.assertTrue(json.loads(call("professional_profile", self.professional, actor=self.customer))["requires_platform_payment"])
            contract = self.contract()
            self.assertEqual(scalar(f"SELECT payment_protection FROM app.contracts WHERE id={q(contract)}"), "guaranteed")
            call("share_location", contract, actor=self.customer)
            self.expect_denied(lambda: call("advance_contract", contract, "depart", actor=self.professional), "funds_not_guaranteed")
            call("set_payment_preference", False, actor=self.professional)
            self.assertEqual(scalar(f"SELECT payment_protection FROM app.contracts WHERE id={q(contract)}"), "guaranteed")

        def test_03_topup_requires_settlement_and_is_free(self):
            t = self.topup(10000, pay=False)
            self.assertEqual(self.balance()["available_minor"], 0)
            self.expect_denied(lambda: call("confirm_topup", t, "money-test", "x", "x", 10000, "VES", "a"*64, actor=self.customer), "permission denied")
            self.expect_denied(lambda: call("confirm_topup", t, "money-test", "x", "x", 1, "VES", "a"*64, role="chamba_billing"), "amount_mismatch")
            event,payment = uuid.uuid4().hex,uuid.uuid4().hex
            for _ in range(2):
                call("confirm_topup", t, "money-test", event, payment, 10000, "VES", "a"*64, role="chamba_billing")
            call("confirm_topup", t, "money-test", uuid.uuid4().hex, payment, 10000, "VES", "a"*64, role="chamba_billing")
            self.assertEqual(self.balance()["available_minor"], 10000)
            self.assertEqual(scalar(f"SELECT fee_minor FROM app.money_topups WHERE id={q(t)}"), "0")

        def test_04_guarantee_blocks_start_until_funded(self):
            contract = self.remote_contract(funded=False)
            self.expect_denied(lambda: call("advance_contract", contract, "start", actor=self.professional), "funds_not_guaranteed")
            quote = json.loads(call("quote_contract_funding", contract, actor=self.customer))
            self.expect_denied(lambda: call("fund_contract", quote["id"], str(uuid.uuid4()), actor=self.customer), "insufficient_money")
            self.assertEqual(json.loads(call("escrow_details", contract, actor=self.professional))["status"], "unfunded")

        def test_05_reservation_is_not_spendable(self):
            contract = self.remote_contract()
            self.assertEqual(self.balance()["available_minor"], 0)
            self.assertEqual(self.balance()["reserved_for_services_minor"], 30000)
            method = self.payout_setup()
            self.expect_denied(lambda: call("quote_withdrawal", "VES", method, 30000, actor=self.customer), "insufficient_money")
            self.expect_denied(lambda: call("cancel_contract", contract, "Cancelar y sacar dinero", actor=self.customer), "escrow_refund_required")

        def test_06_only_customer_releases_full_amount_without_fee(self):
            contract = self.remote_contract()
            delivery = self.delivery(contract)
            self.expect_denied(lambda: call("advance_contract", contract, "finish", actor=self.professional), "use_delivery")
            self.expect_denied(lambda: call("release_contract_funds", contract, delivery, str(uuid.uuid4()), actor=self.professional), "only_customer")
            self.expect_denied(lambda: call("release_contract_funds", contract, delivery, str(uuid.uuid4()), actor=self.outsider), "only_customer")
            key = str(uuid.uuid4())
            call("release_contract_funds", contract, delivery, key, actor=self.customer)
            call("release_contract_funds", contract, delivery, key, actor=self.customer)
            self.assertEqual(self.balance(self.professional)["available_minor"], 30000)
            self.assertEqual(self.balance()["reserved_for_services_minor"], 0)
            self.assertEqual(scalar(f"SELECT status||':'||platform_fee_minor FROM app.contracts WHERE id={q(contract)}"), "completed:0")
            self.assertEqual(scalar(f"SELECT count(*) FROM app.money_transactions WHERE reference_id={q(contract)} AND kind='contract_released'"), "1")

        def test_07_delivery_changes_preserve_funds_and_latest_version(self):
            contract = self.remote_contract()
            first = self.delivery(contract)
            call("request_delivery_changes", first, "Falta una variante del logotipo", actor=self.customer)
            self.assertEqual(self.balance()["reserved_for_services_minor"], 30000)
            self.expect_denied(lambda: call("release_contract_funds", contract, first, str(uuid.uuid4()), actor=self.customer), "not_releasable")
            second = call("submit_delivery", contract, "Entrego la segunda versión completa del diseño.", str(uuid.uuid4()), actor=self.professional)
            self.expect_denied(lambda: call("release_contract_funds", contract, first, str(uuid.uuid4()), actor=self.customer), "not_releasable")
            call("release_contract_funds", contract, second, str(uuid.uuid4()), actor=self.customer)
            self.assertEqual(self.balance(self.professional)["available_minor"], 30000)

        def test_08_funded_terms_cannot_change(self):
            contract = self.remote_contract()
            terms = call("propose_terms", contract, 60000, "platform_wallet", "Nuevo precio", actor=self.professional)
            self.expect_denied(lambda: call("accept_terms", terms, actor=self.customer), "funded_terms_immutable")
            self.assertEqual(scalar(f"SELECT agreed_price_minor FROM app.contracts WHERE id={q(contract)}"), "30000")
            self.expect_denied(lambda: call("report_external_payment", contract, None, 30000, "VES", "cash", actor=self.customer), "platform_payment_required")

        def test_09_refund_needs_provider_or_support(self):
            contract = self.remote_contract()
            call("request_escrow_refund", contract, "Solicito cancelar el proyecto y devolver el saldo", actor=self.customer)
            self.expect_denied(lambda: call("approve_escrow_refund", contract, actor=self.customer), "only_professional")
            call("approve_escrow_refund", contract, actor=self.professional)
            call("approve_escrow_refund", contract, actor=self.professional)
            self.assertEqual(self.balance()["available_minor"], 30000)
            self.assertEqual(self.balance()["reserved_for_services_minor"], 0)
            self.assertEqual(self.balance(self.professional)["available_minor"], 0)

        def test_10_dispute_blocks_release_support_can_refund(self):
            contract = self.remote_contract()
            delivery = self.delivery(contract)
            ticket = call("dispute_contract", contract, "El resultado no cumple los requisitos acordados", actor=self.customer)
            self.expect_denied(lambda: call("release_contract_funds", contract, delivery, str(uuid.uuid4()), actor=self.customer), "not_releasable")
            self.expect_denied(lambda: call("resolve_escrow_refund", contract, ticket, "Motivo suficientemente largo", actor=self.customer), "permission denied")
            call("resolve_escrow_refund", contract, ticket, "Soporte valida la devolución del importe reservado", actor=self.admin, role="chamba_admin")
            self.assertEqual(self.balance()["available_minor"], 30000)

        def test_11_withdrawal_fee_only_when_paid(self):
            self.topup(100000)
            method = self.payout_setup(bps=250)
            wd,quote = self.withdrawal(method, 40000)
            self.assertEqual((quote["amount_minor"],quote["fee_minor"],quote["net_minor"]), (40000,1000,39000))
            self.assertEqual(self.balance()["available_minor"], 60000)
            self.assertEqual(self.balance()["withdrawal_pending_minor"], 40000)
            self.assertEqual(scalar(f"SELECT count(*) FROM app.money_transactions WHERE reference_id={q(wd)} AND kind='withdrawal_paid'"), "0")
            self.settle_withdrawal(wd)
            self.assertEqual(self.balance()["withdrawal_pending_minor"], 0)
            self.assertEqual(scalar(f"SELECT sum(e.delta_minor) FROM app.money_entries e JOIN app.money_transactions t ON t.id=e.transaction_id JOIN app.money_accounts a ON a.id=e.account_id WHERE t.reference_id={q(wd)} AND a.kind='platform_fees'"), "1000")

        def test_12_cancelled_and_failed_withdrawals_return_full_gross(self):
            self.topup(10000)
            method = self.payout_setup()
            wd,_ = self.withdrawal(method, 10000)
            call("cancel_withdrawal", wd, actor=self.customer)
            call("cancel_withdrawal", wd, actor=self.customer)
            self.assertEqual(self.balance()["available_minor"], 10000)
            wd,_ = self.withdrawal(method, 10000)
            self.settle_withdrawal(wd, "definitively_failed")
            self.assertEqual(self.balance()["available_minor"], 10000)
            self.assertEqual(self.balance()["withdrawal_pending_minor"], 0)

        def test_13_fee_quote_survives_admin_change(self):
            self.topup(10000)
            method = self.payout_setup(bps=100)
            quote = json.loads(call("quote_withdrawal", "VES", method, None, actor=self.customer))
            call("configure_withdrawal_fee", "VES", 500, "Nueva tasa para nuevos retiros", actor=self.admin, role="chamba_admin")
            wd = call("request_withdrawal", quote["id"], str(uuid.uuid4()), actor=self.customer)
            self.assertEqual(scalar(f"SELECT fee_minor FROM app.money_withdrawals WHERE id={q(wd)}"), "100")

        def test_14_full_withdrawal_and_zero_fee_configuration(self):
            self.topup(23000)
            method = self.payout_setup(bps=0)
            wd,quote = self.withdrawal(method)
            self.assertEqual(quote["amount_minor"], 23000)
            self.assertEqual(quote["fee_minor"], 0)
            self.settle_withdrawal(wd)
            self.assertEqual(self.balance()["available_minor"], 0)

        def test_15_identity_method_owner_currency_and_bank_verification(self):
            self.topup(10000)
            raw = call("add_payout_method", "money-test", uuid.uuid4().hex, "Cuenta", "****4444", "VES", actor=self.customer)
            self.expect_denied(lambda: call("quote_withdrawal", "VES", raw, 100, actor=self.customer), "verified_identity")
            other = self.payout_setup(self.outsider)
            own = self.payout_setup()
            self.expect_denied(lambda: call("quote_withdrawal", "VES", raw, 100, actor=self.customer), "verified_payout")
            self.expect_denied(lambda: call("quote_withdrawal", "VES", other, 100, actor=self.customer), "verified_payout")
            self.expect_denied(lambda: call("quote_withdrawal", "USD", own, 100, actor=self.customer), "verified_payout")
            self.expect_denied(lambda: call("verify_payout_method", raw, self.customer, "money-test", "x", actor=self.customer), "permission denied")

        def test_16_unknown_payout_keeps_reservation(self):
            self.topup(10000)
            method = self.payout_setup()
            wd,_ = self.withdrawal(method)
            first = call("prepare_withdrawal", wd, role="chamba_billing")
            self.assertEqual(call("prepare_withdrawal", wd, role="chamba_billing"), first)
            self.expect_denied(lambda: call("cancel_withdrawal", wd, actor=self.customer), "already_processing")
            self.expect_denied(lambda: call("confirm_withdrawal", wd, "money-test", "event", "payment", "timeout", 9800, "VES", "b"*64, role="chamba_billing"), "definitive_provider_result_required")
            self.assertEqual(self.balance()["available_minor"], 0)
            self.assertEqual(self.balance()["withdrawal_pending_minor"], 10000)

        def test_17_payment_cannot_credit_points_and_cash(self):
            _,purchase = self.plan_and_purchase()
            provider,payment = "shared-gateway",uuid.uuid4().hex
            call("confirm_purchase", purchase, provider, uuid.uuid4().hex, payment, 50000, "VES", "a"*64, role="chamba_billing")
            topup = self.topup(50000, pay=False)
            self.expect_denied(lambda: call("confirm_topup", topup, provider, uuid.uuid4().hex, payment, 50000, "VES", "a"*64, role="chamba_billing"), "duplicate key")
            self.assertEqual(self.balance()["available_minor"], 0)

        def test_18_balanced_ledger_and_append_only(self):
            self.topup(10000)
            self.assertEqual(scalar("SELECT count(*) FROM (SELECT transaction_id FROM app.money_entries GROUP BY transaction_id HAVING sum(delta_minor)<>0) t"), "0")
            self.assertEqual(scalar("SELECT count(*) FROM app.money_accounts a WHERE a.balance_minor<>coalesce((SELECT sum(e.delta_minor) FROM app.money_entries e WHERE e.account_id=a.id),0)"), "0")
            self.expect_denied(lambda: execute("INSERT INTO app.money_transactions(kind,currency,idempotency_key,reference_id,entries_fingerprint) VALUES('invalid','VES','unbalanced-test',gen_random_uuid(),'[]')"), "unbalanced_money_transaction")
            self.expect_denied(lambda: execute("UPDATE app.money_entries SET delta_minor=1"), "append_only")
            self.expect_denied(lambda: execute("SELECT * FROM app.money_accounts", self.customer,"chamba_api"), "permission denied")

        def test_19_topup_and_wallet_stay_separate_from_credits(self):
            self.topup(30000, self.professional)
            self.assertEqual(self.balance(self.professional)["available_minor"], 30000)
            self.assertEqual(json.loads(call("my_wallet", actor=self.professional))["balance"], 3)
            self.assertEqual(call("money_history", "VES", actor=self.outsider), "")
            self.assertNotIn("provider_token", call("my_payout_methods", actor=self.professional))

        def test_20_concurrent_last_balance_funding_vs_withdrawal(self):
            contract = self.remote_contract(funded=False)
            self.topup(30000)
            fquote = json.loads(call("quote_contract_funding", contract, actor=self.customer))
            method = self.payout_setup()
            wquote = json.loads(call("quote_withdrawal", "VES", method, 30000, actor=self.customer))
            barrier = threading.Barrier(2)
            def compete(action):
                barrier.wait()
                try:
                    if action == "fund": call("fund_contract", fquote["id"], str(uuid.uuid4()), actor=self.customer)
                    else: call("request_withdrawal", wquote["id"], str(uuid.uuid4()), actor=self.customer)
                    return "ok"
                except RuntimeError as error:
                    self.assertIn("insufficient_money", str(error))
                    return "insufficient"
            with ThreadPoolExecutor(2) as pool:
                self.assertCountEqual(list(pool.map(compete,["fund","withdraw"])),["ok","insufficient"])
            b = self.balance()
            self.assertEqual(b["available_minor"], 0)
            self.assertEqual(b["reserved_for_services_minor"]+b["withdrawal_pending_minor"], 30000)

        def test_21_concurrent_release_once(self):
            contract = self.remote_contract()
            delivery = self.delivery(contract)
            barrier = threading.Barrier(2)
            key = str(uuid.uuid4())
            def release():
                barrier.wait()
                return call("release_contract_funds", contract, delivery, key, actor=self.customer)
            with ThreadPoolExecutor(2) as pool:
                self.assertEqual(list(pool.map(lambda _: release(),range(2))), [contract,contract])
            self.assertEqual(self.balance(self.professional)["available_minor"], 30000)

        def test_22_concurrent_topup_once(self):
            topup = self.topup(10000,pay=False)
            barrier = threading.Barrier(2)
            event,payment = uuid.uuid4().hex,uuid.uuid4().hex
            def settle():
                barrier.wait()
                return call("confirm_topup", topup, "money-test", event, payment, 10000, "VES", "a"*64, role="chamba_billing")
            with ThreadPoolExecutor(2) as pool:
                self.assertEqual(list(pool.map(lambda _: settle(),range(2))),[topup,topup])
            self.assertEqual(self.balance()["available_minor"], 10000)

        def test_23_incident_freezes_outgoing_funds(self):
            contract = self.remote_contract()
            delivery = self.delivery(contract)
            self.assertTrue(json.loads(call("escrow_details", contract, actor=self.professional))["funds_guaranteed"])
            wallet = self.balance()
            call("flag_money_incident", wallet["id"], "money-test", uuid.uuid4().hex, "Contracargo pendiente de conciliación", role="chamba_billing")
            self.assertEqual(self.balance()["status"], "frozen")
            escrow = json.loads(call("escrow_details", contract, actor=self.professional))
            self.assertFalse(escrow["funds_guaranteed"])
            self.assertTrue(escrow["funding_on_hold"])
            self.expect_denied(lambda: call("release_contract_funds", contract, delivery, str(uuid.uuid4()), actor=self.customer), "wallet_frozen")
            self.expect_denied(lambda: call("create_topup", "VES", 10000, str(uuid.uuid4()), actor=self.customer), "wallet_frozen")

        def test_24_remote_broadcast_and_delivery_access(self):
            req = self.remote_request(mode="broadcast")
            proposal = call("submit_proposal", req, "Diseñaré las variantes", 25000, "2098-01-01", actor=self.professional)
            contract = call("accept_proposal", proposal, actor=self.customer)
            self.topup(25000)
            quote = json.loads(call("quote_contract_funding", contract, actor=self.customer))
            self.assertEqual(quote["price_minor"],25000)
            call("fund_contract",quote["id"],str(uuid.uuid4()),actor=self.customer)
            call("advance_contract",contract,"start",actor=self.professional)
            file = call("register_upload",self.professional,"design/"+uuid.uuid4().hex,"application/pdf",100,"a"*64,"evidence",True,role="chamba_worker")
            call("moderate_file",file,True,"Entregable revisado",actor=self.admin,role="chamba_admin")
            delivery = call("submit_delivery",contract,"Entrego el PDF con el diseño solicitado",str(uuid.uuid4()),"{"+file+"}",actor=self.professional)
            self.assertIn("design/",call("file_object_key",file,actor=self.customer))
            self.expect_denied(lambda: call("file_object_key",file,actor=self.outsider),"file_access_denied")
            self.expect_denied(lambda: call("escrow_details",contract,actor=self.outsider),"access_denied")
            call("release_contract_funds",contract,delivery,str(uuid.uuid4()),actor=self.customer)

        def test_25_delivery_cannot_attach_another_users_private_file(self):
            contract = self.remote_contract()
            call("advance_contract",contract,"start",actor=self.professional)
            own = call("register_upload",self.professional,"own/"+uuid.uuid4().hex,"application/pdf",100,"b"*64,"evidence",True,role="chamba_worker")
            foreign = call("register_upload",self.outsider,"foreign/"+uuid.uuid4().hex,"application/pdf",100,"c"*64,"evidence",True,role="chamba_worker")
            for file in (own,foreign):
                call("moderate_file",file,True,"Documento revisado",actor=self.admin,role="chamba_admin")
            self.expect_denied(lambda: call("submit_delivery",contract,"Documento de otra persona",str(uuid.uuid4()),"{"+foreign+"}",actor=self.professional),"unsafe_delivery_file")
            self.expect_denied(lambda: call("file_object_key",foreign,actor=self.customer),"file_access_denied")

        def test_26_quote_invalidated_by_changed_terms_even_same_price(self):
            contract = self.remote_contract(funded=False)
            self.topup(30000)
            quote = json.loads(call("quote_contract_funding",contract,actor=self.customer))
            terms = call("propose_terms",contract,30000,"platform_wallet","Nuevas condiciones acordadas",actor=self.professional)
            call("accept_terms",terms,actor=self.customer)
            self.expect_denied(lambda: call("fund_contract",quote["id"],str(uuid.uuid4()),actor=self.customer),"funding_quote_stale")
            self.assertEqual(self.balance()["available_minor"],30000)

        def test_27_frozen_processing_payout_requires_reconciliation(self):
            self.topup(10000)
            method = self.payout_setup()
            wd,_ = self.withdrawal(method)
            call("prepare_withdrawal",wd,role="chamba_billing")
            call("flag_money_incident",self.balance()["id"],"money-test",uuid.uuid4().hex,"Revisar pago de origen",role="chamba_billing")
            self.expect_denied(lambda: call("prepare_withdrawal",wd,role="chamba_billing"),"wallet_frozen_requires_reconciliation")
            self.assertEqual(self.balance()["withdrawal_pending_minor"],10000)

    return unittest.defaultTestLoader.loadTestsFromTestCase(MoneyTests)
