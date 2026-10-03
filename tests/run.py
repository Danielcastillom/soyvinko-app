#!/usr/bin/env python3
"""Pruebas reales contra una base desechable propia; nunca vacían chamba."""
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timedelta, timezone
import json
from pathlib import Path
import subprocess
import sys
import threading
import unittest
import uuid

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))
import db

DATABASE = "chamba_test_" + uuid.uuid4().hex[:12]


def q(value):
    if value is None:
        return "NULL"
    if isinstance(value, bool):
        return "true" if value else "false"
    if isinstance(value, (int, float)):
        return str(value)
    return "'" + str(value).replace("'", "''") + "'"


def execute(statement, actor=None, role=None):
    prefix = "BEGIN;"
    if role:
        assert role in {"chamba_api", "chamba_auth", "chamba_admin", "chamba_billing", "chamba_worker"}
        prefix += f"SET LOCAL ROLE {role};"
    if actor:
        prefix += f"SET LOCAL app.user_id={q(actor)};"
    try:
        return db.sql(prefix + statement + ";COMMIT;", database=DATABASE, capture=True).stdout.strip()
    except subprocess.CalledProcessError as error:
        raise RuntimeError(error.stderr.strip()) from None


def call(name, *args, actor=None, role="chamba_api"):
    return execute(f"SELECT api.{name}({','.join(q(x) for x in args)})", actor, role)


def scalar(statement):
    return execute(statement)


class DatabaseTests(unittest.TestCase):
    def setUp(self):
        self.customer = self.user("Cliente")
        self.professional = self.user("Profesional")
        self.outsider = self.user("Tercero")
        self.admin = self.user("Administrador")
        execute(f"INSERT INTO app.user_roles(user_id,role_code) VALUES({q(self.admin)},'superadmin')")
        self.category = scalar("SELECT id FROM app.categories WHERE slug='plomeria'")
        self.address = call("add_address", "Casa", "VE", "Caracas", "Calle privada 123", -66.877, 10.516,
                            "Referencia privada", actor=self.customer)
        self.prepare_professional(self.professional)

    def user(self, name):
        return call("register_user", uuid.uuid4().hex + "@example.test", name,
                    "$argon2id$v=19$m=65536,t=3,p=1$c2FsdA$aGFzaA", role="chamba_auth")

    def prepare_professional(self, user, lon=-66.878, radius=10000):
        call("activate_professional", "pro-" + uuid.uuid4().hex, "Servicios", "Experiencia técnica", actor=user)
        branch = call("add_branch", "Sede", "VE", "Caracas", "Dirección privada profesional", lon, 10.516, actor=user)
        call("add_offering", self.category, "Plomería", "VES", 10000, actor=user)
        call("set_coverage", branch, self.category, radius, actor=user)
        return branch

    def request(self, mode="direct", target=None, publish=True):
        req = call("create_request", self.category, mode, (target or self.professional) if mode == "direct" else None,
                   "Reparar tubería", "Se dañó un tubo de la cocina.", self.address, 10000, "VES", 10000, 50000,
                   "2099-01-01T00:00:00Z", str(uuid.uuid4()), actor=self.customer)
        if publish:
            call("publish_request", req, actor=self.customer)
        return req

    def contract(self):
        req = self.request()
        return call("accept_direct_request", req, actor=self.professional)

    def plan_and_purchase(self, user=None, credits=10, marketplace_days=30):
        user = user or self.professional
        plan = call("configure_plan", uuid.uuid4().hex, "Plan de prueba", credits, 50000, "VES", 50, 30,
                    marketplace_days, True, actor=self.admin, role="chamba_admin")
        purchase = call("start_purchase", plan, str(uuid.uuid4()), actor=user)
        return plan, purchase

    def pay(self, purchase, event=None, payment=None, amount=50000):
        event = event or uuid.uuid4().hex
        payment = payment or uuid.uuid4().hex
        return call("confirm_purchase", purchase, "test-gateway", event, payment, amount, "VES", "a"*64, role="chamba_billing")

    def store_product(self, stock=5):
        _, purchase = self.plan_and_purchase()
        self.pay(purchase)
        store = call("activate_store", "store-" + uuid.uuid4().hex, "Ferretería", actor=self.professional)
        product = call("add_product", store, "SKU1", "Tubo", "Tubo de repuesto", 2300, "VES", stock, actor=self.professional)
        return store, product

    def expect_denied(self, func, contains):
        with self.assertRaisesRegex(RuntimeError, contains):
            func()

    def test_01_welcome_is_once(self):
        call("activate_professional", "unused-slug", "Nombre", actor=self.professional)
        wallet = json.loads(call("my_wallet", actor=self.professional))
        self.assertEqual(wallet["balance"], 3)
        self.assertEqual(len(wallet["ledger"]), 1)

    def test_02_direct_acceptance_chat_and_one_debit(self):
        request = self.request()
        contract = call("accept_direct_request", request, actor=self.professional)
        self.assertEqual(call("accept_direct_request", request, actor=self.professional), contract)
        self.assertEqual(json.loads(call("my_wallet", actor=self.professional))["balance"], 2)
        message_key = str(uuid.uuid4())
        first = call("send_message", contract, "Hola", message_key, actor=self.customer)
        self.assertEqual(call("send_message", contract, "Hola", message_key, actor=self.customer), first)
        self.expect_denied(lambda: call("send_message", contract, "Otro texto", message_key, actor=self.customer), "idempotency_conflict")
        self.assertEqual(scalar(f"SELECT count(*) FROM app.conversations WHERE contract_id={q(contract)}"), "1")

    def test_03_broadcast_has_manual_selection(self):
        request = self.request("broadcast")
        pro2 = self.user("Otro profesional")
        self.prepare_professional(pro2)
        proposal1 = call("submit_proposal", request, "Puedo atender", 20000, "2098-01-01T00:00:00Z", actor=self.professional)
        proposal2 = call("submit_proposal", request, "Disponible", 22000, "2098-01-01T00:00:00Z", actor=pro2)
        self.assertEqual(json.loads(call("my_wallet", actor=self.professional))["balance"], 3)
        contract = call("accept_proposal", proposal2, actor=self.customer)
        self.assertEqual(call("accept_proposal", proposal2, actor=self.customer), contract)
        self.expect_denied(lambda: call("accept_proposal", proposal1, actor=self.customer), "already_assigned")
        self.assertEqual(json.loads(call("my_wallet", actor=self.professional))["balance"], 3)
        self.assertEqual(json.loads(call("my_wallet", actor=pro2))["balance"], 2)

    def test_04_feed_hides_identity_and_location(self):
        request = self.request("broadcast")
        feed = call("available_requests", actor=self.professional)
        self.assertIn(request, feed)
        for private in (self.customer, "Calle privada", "Referencia privada", "distance", "latitude", "longitude", "customer_id", self.address):
            self.assertNotIn(private, feed)
        self.assertEqual(call("available_requests", actor=self.outsider), "")
        self.expect_denied(lambda: execute("SELECT * FROM app.request_locations", self.professional, "chamba_api"), "permission denied")
        self.expect_denied(lambda: call("contract_contacts", str(uuid.uuid4()), actor=self.professional), "access_denied")

    def test_05_explicit_location_consent_and_revoke(self):
        contract = self.contract()
        self.expect_denied(lambda: call("navigate", contract, actor=self.professional), "location_not_shared")
        self.expect_denied(lambda: call("share_location", contract, actor=self.professional), "only_customer")
        call("share_location", contract, actor=self.customer)
        self.assertIn("Calle privada", call("navigate", contract, actor=self.professional))
        self.expect_denied(lambda: call("navigate", contract, actor=self.outsider), "access_denied")
        call("revoke_location", contract, actor=self.customer)
        self.expect_denied(lambda: call("navigate", contract, actor=self.professional), "location_not_shared")

    def test_05b_live_customer_gps_and_status(self):
        contract = self.contract()
        status = json.loads(call("location_status", contract, actor=self.professional))
        self.assertFalse(status["active"])
        call("share_location", contract, 2, -66.9, 10.5, 15, actor=self.customer)
        status = json.loads(call("location_status", contract, actor=self.customer))
        self.assertTrue(status["active"])
        self.assertEqual("gps", status["source"])
        self.assertIsNotNone(status["expires_at"])
        nav = json.loads(call("navigate", contract, actor=self.professional))
        self.assertEqual("gps", nav["destination"]["source"])
        self.assertAlmostEqual(10.5, nav["destination"]["latitude"], places=4)
        self.assertEqual("gps", nav["share"]["source"])
        self.expect_denied(lambda: call("update_shared_location", contract, -66.8, 10.6, 10, actor=self.professional), "location_not_shared")
        call("update_shared_location", contract, -66.8, 10.6, 10, actor=self.customer)
        nav = json.loads(call("navigate", contract, actor=self.professional))
        self.assertAlmostEqual(10.6, nav["destination"]["latitude"], places=4)
        self.expect_denied(lambda: call("location_status", contract, actor=self.outsider), "access_denied")
        self.expect_denied(lambda: call("share_location", contract, 2, -66.9, None, None, actor=self.customer), "invalid_coordinates")
        call("revoke_location", contract, actor=self.customer)
        status = json.loads(call("location_status", contract, actor=self.professional))
        self.assertFalse(status["active"])
        self.assertIsNotNone(status["revoked_at"])
        self.expect_denied(lambda: call("update_shared_location", contract, -66.8, 10.6, 10, actor=self.customer), "location_not_shared")
        # Sin coordenadas sigue funcionando como antes: se revela la dirección de la solicitud.
        call("share_location", contract, actor=self.customer)
        nav = json.loads(call("navigate", contract, actor=self.professional))
        self.assertEqual("address", nav["destination"]["source"])
        self.assertIn("Calle privada", nav["destination"]["address"])

    def test_05c_professional_can_remove_profile_items(self):
        pro = self.professional
        profile = json.loads(call("my_professional", actor=pro))
        offering = profile["offerings"][0]["id"]
        branch = profile["branches"][0]["id"]
        # Servicio con historial: se archiva y deja de mostrarse; no se puede reactivar.
        req = self.request()
        execute(f"UPDATE app.service_requests SET offering_id={q(offering)} WHERE id={q(req)}")
        self.expect_denied(lambda: call("remove_offering", offering, actor=self.outsider), "offering_not_owned")
        self.assertEqual("archived", call("remove_offering", offering, actor=pro))
        self.assertFalse(any(o["id"] == offering for o in json.loads(call("my_professional", actor=pro))["offerings"] or []))
        self.expect_denied(lambda: execute(f"UPDATE app.offerings SET active=true WHERE id={q(offering)}"), "offering_archived")
        # Servicio sin historial: se borra.
        fresh = call("add_offering", self.category, "Destapes", "VES", 5000, actor=pro)
        self.assertEqual("deleted", call("remove_offering", fresh, actor=pro))
        # Contacto, portafolio y credencial.
        contact = call("add_professional_contact", branch, "phone", "Oficina", "04141234567", actor=pro)
        call("remove_professional_contact", contact, actor=pro)
        self.assertEqual("0", scalar(f"SELECT count(*) FROM app.professional_contacts WHERE id={q(contact)}"))
        photo = call("register_upload", pro, uuid.uuid4().hex + ".jpg", "image/jpeg", 100, "a" * 64, "portfolio", True, role="chamba_worker")
        item = call("add_portfolio", "Trabajo", "Antes y después", photo, actor=pro)
        self.expect_denied(lambda: call("remove_portfolio_item", item, actor=self.outsider), "resource_not_owned")
        call("remove_portfolio_item", item, actor=pro)
        self.assertNotEqual("", scalar(f"SELECT deleted_at FROM app.files WHERE id={q(photo)}"))
        # Sede: también se quitan su cobertura y horarios.
        self.expect_denied(lambda: call("remove_branch", branch, actor=self.outsider), "branch_not_owned")
        call("remove_branch", branch, actor=pro)
        self.assertEqual("0", scalar(f"SELECT count(*) FROM app.coverage_areas WHERE branch_id={q(branch)}"))
        self.assertEqual("0", scalar(f"SELECT count(*) FROM app.branches WHERE id={q(branch)}"))

    def test_05d_product_photos_in_store_and_seller_views(self):
        store, product = self.store_product()
        seller = self.professional
        photo = call("register_upload", seller, uuid.uuid4().hex + ".jpg", "image/jpeg", 100, "b" * 64, "product", True, role="chamba_worker")
        call("add_product_image", product, photo, actor=seller)
        mine = json.loads(call("my_products", actor=seller).splitlines()[0])
        self.assertEqual(photo, mine["images"][0]["file_id"])
        self.assertTrue(mine["store_name"])
        public = [json.loads(line) for line in call("store_products", store, actor=self.customer).splitlines() if line]
        self.assertIn(photo, public[0]["images"])
        self.expect_denied(lambda: call("remove_product_image", product, photo, actor=self.customer), "product_not_owned")
        call("remove_product_image", product, photo, actor=seller)
        public = [json.loads(line) for line in call("store_products", store, actor=self.customer).splitlines() if line]
        self.assertEqual([], public[0]["images"])
        self.assertNotEqual("", scalar(f"SELECT deleted_at FROM app.files WHERE id={q(photo)}"))

    def test_06_both_confirm_finish_before_reviews(self):
        contract = self.contract()
        self.expect_denied(lambda: execute(f"SELECT api.rate_contract({q(contract)},5::smallint,true,'Bien')", self.customer, "chamba_api"), "not_completed")
        self.expect_denied(lambda: call("advance_contract", contract, "start", actor=self.professional), "invalid_transition")
        call("share_location", contract, actor=self.customer)
        for action in ("depart", "arrive", "start"):
            call("advance_contract", contract, action, actor=self.professional)
        self.assertEqual(call("advance_contract", contract, "finish", actor=self.professional), "awaiting_confirmation")
        self.assertEqual(call("advance_contract", contract, "finish", actor=self.customer), "completed")
        review = execute(f"SELECT api.rate_contract({q(contract)},5::smallint,true,'Buen trabajo')", self.customer, "chamba_api")
        self.expect_denied(lambda: execute(f"SELECT api.rate_contract({q(contract)},4::smallint,true,'Otro')", self.customer, "chamba_api"), "duplicate key")
        self.assertIsNone(json.loads(call("professional_profile", self.professional, actor=self.customer))["rating"])
        call("moderate_review", review, "published", "Reseña revisada", actor=self.admin, role="chamba_admin")
        self.assertEqual(json.loads(call("professional_profile", self.professional, actor=self.customer))["rating"], 5)
        self.expect_denied(lambda: call("navigate", contract, actor=self.professional), "location_not_shared")

    def test_07_insufficient_credits_rolls_back_everything(self):
        execute(f"SELECT internal.post_credit({q(self.professional)},-3,'adjustment','test:{uuid.uuid4()}','Prueba de saldo cero')")
        req = self.request()
        self.expect_denied(lambda: call("accept_direct_request", req, actor=self.professional), "insufficient_credits")
        self.assertEqual(scalar(f"SELECT status FROM app.service_requests WHERE id={q(req)}"), "open")
        self.assertEqual(scalar(f"SELECT count(*) FROM app.contracts WHERE request_id={q(req)}"), "0")

    def test_08_payment_idempotency_and_gateway_authority(self):
        _, purchase = self.plan_and_purchase()
        self.expect_denied(lambda: call("confirm_purchase", purchase, "x", "event", "pay", 50000, "VES", "a"*64, actor=self.professional), "permission denied")
        self.expect_denied(lambda: self.pay(purchase, amount=1), "amount_mismatch")
        self.pay(purchase, "event-a", "payment-a")
        self.pay(purchase, "event-a", "payment-a")
        self.pay(purchase, "event-b", "payment-a")
        self.assertEqual(json.loads(call("my_wallet", actor=self.professional))["balance"], 13)
        self.assertEqual(scalar(f"SELECT count(*) FROM app.credit_ledger WHERE purchase_id={q(purchase)}"), "1")
        _, second = self.plan_and_purchase()
        self.expect_denied(lambda: self.pay(second, "event-c", "payment-a"), "duplicate key")

    def test_09_geo_distance_verified_and_sponsored_order(self):
        far = self.user("Más lejano")
        self.prepare_professional(far, lon=-66.91)
        kyc = call("submit_kyc", "manual", uuid.uuid4().hex, actor=far)
        call("review_kyc", kyc, "approved", "Identidad verificada", "2099-01-01", actor=self.admin, role="chamba_admin")
        _, purchase = self.plan_and_purchase(far)
        self.pay(purchase)
        result = execute(f"SELECT professional_id FROM api.search_professionals({q(self.address)},{q(self.category)})", self.customer, "chamba_api").splitlines()
        # Hay otros fixtures, pero solo se compara el orden relativo de estos dos.
        self.assertLess(result.index(self.professional), result.index(far))
        result = execute(f"SELECT professional_id FROM api.search_professionals({q(self.address)},{q(self.category)},10000,'recommended')", self.customer, "chamba_api").splitlines()
        self.assertLess(result.index(far), result.index(self.professional))
        self.expect_denied(lambda: call("search_professionals", self.address, self.category, actor=self.outsider), "address_not_owned")

    def test_10_foreign_addresses_and_branches_rejected(self):
        self.expect_denied(lambda: call("set_coverage", str(uuid.uuid4()), self.category, 1000, actor=self.professional), "branch_not_owned")
        self.expect_denied(lambda: call("create_request", self.category, "direct", self.professional,
            "Solicitud ajena", "Descripción suficientemente larga", self.address, 10000, "VES", 0, 1,
            "2099-01-01", str(uuid.uuid4()), actor=self.outsider), "address_not_owned")
        self.expect_denied(lambda: call("add_address", "Mal", "VE", "Caracas", "Lugar", 181, 4, actor=self.customer), "invalid_coordinates")

    def test_11_customer_publishes_without_prior_moderation(self):
        req = self.request(publish=False)
        self.assertNotIn(req, call("available_requests", actor=self.professional))
        call("publish_request", req, actor=self.customer)
        self.assertIn(req, call("available_requests", actor=self.professional))
        call("moderate_request", req, False, "Desautorizado por administración", actor=self.admin, role="chamba_admin")
        self.assertNotIn(req, call("available_requests", actor=self.professional))
        self.expect_denied(lambda: call("publish_request", req, actor=self.customer), "not_publishable")
        self.expect_denied(lambda: call("moderate_request", req, True, "Aprobado", actor=self.customer), "permission denied")
        self.expect_denied(lambda: call("moderate_request", req, True, "Aprobado", actor=self.customer, role="chamba_admin"), "permission_denied")

    def test_12_kyc_cannot_be_self_approved_or_stale(self):
        kyc = call("submit_kyc", "manual", uuid.uuid4().hex, actor=self.professional)
        self.expect_denied(lambda: call("review_kyc", kyc, "approved", "Verificado", "2099-01-01", actor=self.professional), "permission denied")
        call("review_kyc", kyc, "approved", "Verificado por revisión", "2099-01-01", actor=self.admin, role="chamba_admin")
        self.assertTrue(json.loads(call("professional_profile", self.professional, actor=self.customer))["verified"])
        call("review_kyc", kyc, "revoked", "Documento revocado", actor=self.admin, role="chamba_admin")
        self.assertFalse(json.loads(call("professional_profile", self.professional, actor=self.customer))["verified"])

    def test_13_cancel_and_audited_refund(self):
        contract = self.contract()
        call("cancel_contract", contract, "El cliente ya no necesita la visita", actor=self.customer)
        self.assertEqual(json.loads(call("my_wallet", actor=self.professional))["balance"], 2)
        ticket = call("open_ticket", "credits", "Revisar crédito", "Servicio cancelado por el cliente", contract, actor=self.professional)
        call("refund_contract_credit", contract, ticket, "Devolución aprobada por soporte", actor=self.admin, role="chamba_admin")
        call("refund_contract_credit", contract, ticket, "Devolución aprobada por soporte", actor=self.admin, role="chamba_admin")
        self.assertEqual(json.loads(call("my_wallet", actor=self.professional))["balance"], 3)
        self.expect_denied(lambda: execute(f"UPDATE app.credit_ledger SET reason='alterado' WHERE professional_id={q(self.professional)}"), "append_only")

    def test_14_store_orders_stock_and_zero_commission(self):
        store, product = self.store_product()
        items = json.dumps([{"product_id": product, "quantity": 2}])
        key = str(uuid.uuid4())
        order = call("create_order", store, items, key, actor=self.customer)
        self.assertEqual(call("create_order", store, items, key, actor=self.customer), order)
        self.assertEqual(scalar(f"SELECT stock FROM app.products WHERE id={q(product)}"), "3")
        self.assertEqual(scalar(f"SELECT total_minor||':'||platform_fee_minor FROM app.orders WHERE id={q(order)}"), "4600:0")
        call("advance_order", order, "accept", actor=self.professional)
        call("advance_order", order, "cancel", actor=self.customer)
        call("advance_order", order, "cancel", actor=self.customer)
        self.assertEqual(scalar(f"SELECT stock FROM app.products WHERE id={q(product)}"), "5")
        self.assertEqual(json.loads(call("my_wallet", actor=self.professional))["balance"], 13)

    def test_15_store_entitlement_required(self):
        self.expect_denied(lambda: call("activate_store", "no-plan", "Tienda", actor=self.professional), "entitlement_required")

    def test_16_inactive_sessions_and_privileges(self):
        token_hash = uuid.uuid4().hex*2
        expiry = (datetime.now(timezone.utc) + timedelta(days=7)).isoformat()
        session = call("create_session", self.customer, token_hash, "Web", expiry, role="chamba_auth")
        self.assertEqual(call("resolve_session", token_hash, role="chamba_auth"), self.customer)
        call("revoke_session", session, actor=self.customer)
        self.assertEqual(call("resolve_session", token_hash, role="chamba_auth"), "")
        call("set_user_status", self.customer, "suspended", "Suspensión por caso de prueba", actor=self.admin, role="chamba_admin")
        self.expect_denied(lambda: call("my_account", actor=self.customer), "authentication_required")
        self.expect_denied(lambda: call("auth_account", "nobody@example.test", actor=self.professional), "permission denied")
        self.expect_denied(lambda: execute("SELECT internal.post_credit(NULL,10,'adjustment','hack','hack')", self.professional, "chamba_api"), "permission denied")

    def test_17_request_files_ownership_moderation_and_access(self):
        req = self.request("broadcast", publish=False)
        file = call("register_upload", self.customer, "private/"+uuid.uuid4().hex, "image/jpeg", 100,
                    "b"*64, "request", True, role="chamba_worker")
        call("attach_request_file", req, file, actor=self.customer)
        call("publish_request", req, actor=self.customer)
        self.assertIn("private/", call("file_object_key", file, actor=self.professional))
        self.expect_denied(lambda: call("file_object_key", file, actor=self.outsider), "file_access_denied")
        call("moderate_file", file, False, "Retirada posterior de imagen", actor=self.admin, role="chamba_admin")
        self.expect_denied(lambda: call("file_object_key", file, actor=self.professional), "file_access_denied")

    def test_18_concurrent_same_acceptance_one_charge(self):
        req = self.request()
        barrier = threading.Barrier(2)
        def accept():
            barrier.wait()
            return call("accept_direct_request", req, actor=self.professional)
        with ThreadPoolExecutor(2) as pool:
            a,b = list(pool.map(lambda _: accept(), range(2)))
        self.assertEqual(a,b)
        self.assertEqual(json.loads(call("my_wallet", actor=self.professional))["balance"], 2)

    def test_19_concurrent_competition_for_last_credit(self):
        execute(f"SELECT internal.post_credit({q(self.professional)},-2,'adjustment','test:{uuid.uuid4()}','Dejar un crédito')")
        requests = [self.request(), self.request()]
        barrier = threading.Barrier(2)
        def accept(req):
            barrier.wait()
            try:
                call("accept_direct_request", req, actor=self.professional)
                return "ok"
            except RuntimeError as error:
                self.assertIn("insufficient_credits", str(error))
                return "no_credit"
        with ThreadPoolExecutor(2) as pool:
            self.assertCountEqual(list(pool.map(accept, requests)), ["ok","no_credit"])
        self.assertEqual(json.loads(call("my_wallet", actor=self.professional))["balance"], 0)

    def test_20_concurrent_proposals_one_winner(self):
        request = self.request("broadcast")
        pro2 = self.user("Competidor")
        self.prepare_professional(pro2)
        proposals = [call("submit_proposal", request, "Disponible", 10000, "2098-01-01", actor=p) for p in (self.professional,pro2)]
        barrier = threading.Barrier(2)
        def accept(proposal):
            barrier.wait()
            try:
                call("accept_proposal", proposal, actor=self.customer)
                return "ok"
            except RuntimeError as error:
                self.assertIn("already_assigned", str(error))
                return "lost"
        with ThreadPoolExecutor(2) as pool:
            self.assertCountEqual(list(pool.map(accept, proposals)), ["ok","lost"])
        self.assertEqual(scalar(f"SELECT sum(balance) FROM app.credit_wallets WHERE professional_id IN({q(self.professional)},{q(pro2)})"), "5")

    def test_21_concurrent_inventory_no_oversell(self):
        store, product = self.store_product(stock=1)
        barrier = threading.Barrier(2)
        def buy(customer):
            barrier.wait()
            try:
                call("create_order", store, json.dumps([{"product_id":product,"quantity":1}]), str(uuid.uuid4()), actor=customer)
                return "ok"
            except RuntimeError as error:
                self.assertIn("insufficient_stock", str(error))
                return "sold_out"
        with ThreadPoolExecutor(2) as pool:
            self.assertCountEqual(list(pool.map(buy, [self.customer,self.outsider])), ["ok","sold_out"])
        self.assertEqual(scalar(f"SELECT stock FROM app.products WHERE id={q(product)}"), "0")

    def test_22_concurrent_payment_once(self):
        _, purchase = self.plan_and_purchase()
        barrier = threading.Barrier(2)
        event, payment = uuid.uuid4().hex, uuid.uuid4().hex
        def pay():
            barrier.wait()
            return self.pay(purchase,event,payment)
        with ThreadPoolExecutor(2) as pool:
            results = list(pool.map(lambda _: pay(), range(2)))
        self.assertEqual(results, [purchase,purchase])
        self.assertEqual(json.loads(call("my_wallet", actor=self.professional))["balance"], 13)

    def test_23_default_deny_tables_and_functions(self):
        self.assertEqual(scalar("SELECT count(*) FROM pg_tables WHERE schemaname='app' AND NOT rowsecurity"), "0")
        self.assertEqual(scalar("SELECT count(*) FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace "
            "WHERE n.nspname IN('api','internal') AND EXISTS(SELECT 1 FROM aclexplode(coalesce(p.proacl,acldefault('f',p.proowner))) x WHERE x.grantee=0 AND x.privilege_type='EXECUTE')"), "0")
        self.expect_denied(lambda: call("my_account"), "authentication_required")
        self.expect_denied(lambda: execute("SELECT * FROM app.users", self.customer,"chamba_api"), "permission denied")
        self.expect_denied(lambda: call("claim_notifications", actor=self.customer), "permission denied")
        self.expect_denied(lambda: call("register_upload", self.customer, "bad-key", "image/jpeg", 100, "a"*64, "request", True, actor=self.customer), "permission denied")

    def test_24_agreed_terms_need_both_parties(self):
        contract = self.contract()
        terms = call("propose_terms", contract, 25000, "cash", "Al terminar el trabajo", actor=self.professional)
        self.assertEqual(scalar(f"SELECT agreed_price_minor FROM app.contracts WHERE id={q(contract)}"), "")
        self.expect_denied(lambda: call("accept_terms", terms, actor=self.outsider), "access_denied")
        call("accept_terms", terms, actor=self.customer)
        self.assertEqual(scalar(f"SELECT agreed_price_minor||':'||payment_method FROM app.contracts WHERE id={q(contract)}"), "25000:cash")

    def test_25_notification_claims_do_not_overlap(self):
        self.request()
        barrier = threading.Barrier(2)
        def claim():
            barrier.wait()
            return [json.loads(line) for line in call("claim_notifications", 100, role="chamba_worker").splitlines() if line]
        with ThreadPoolExecutor(2) as pool:
            a,b = list(pool.map(lambda _: claim(), range(2)))
        self.assertFalse({x['id'] for x in a} & {x['id'] for x in b})
        self.assertTrue(a or b)
        message = (a+b)[0]
        self.expect_denied(lambda: call("finish_notification", message["id"], message["attempts"]+1, True, "push", "test", role="chamba_worker"), "lease_lost")
        call("finish_notification", message["id"], message["attempts"], True, "push", "test", role="chamba_worker")

    def test_26_external_payment_needs_counterparty(self):
        contract = self.contract()
        report = call("report_external_payment", contract, None, 30000, "VES", "cash", actor=self.customer)
        self.expect_denied(lambda: call("confirm_external_payment", report, actor=self.customer), "counterparty")
        self.expect_denied(lambda: call("confirm_external_payment", report, actor=self.outsider), "access_denied")
        call("confirm_external_payment", report, actor=self.professional)
        self.assertEqual(scalar(f"SELECT confirmed_by FROM app.external_payment_reports WHERE id={q(report)}"), self.professional)
        self.assertEqual(json.loads(call("my_wallet", actor=self.professional))["balance"], 2)

    def test_27_product_delivery_is_explicit_and_private(self):
        store, product = self.store_product()
        order = call("create_order", store, json.dumps([{"product_id":product,"quantity":1}]), str(uuid.uuid4()), actor=self.customer)
        self.expect_denied(lambda: call("share_order_delivery", order, "Cliente", "+57000", "\\xabcdef", "test-key", actor=self.customer), "not_allowed")
        call("advance_order", order, "accept", actor=self.professional)
        call("share_order_delivery", order, "Cliente", "+57000", "\\xabcdef", "test-key", actor=self.customer)
        self.assertIn("abcdef", call("order_delivery", order, actor=self.professional))
        self.expect_denied(lambda: call("order_delivery", order, actor=self.outsider), "access_denied")

    def test_28_terms_cannot_change_completed_contract(self):
        contract = self.contract()
        old = call("propose_terms", contract, 10000, "cash", "Inicial", actor=self.professional)
        new = call("propose_terms", contract, 20000, "transfer", "Actualizado", actor=self.professional)
        self.expect_denied(lambda: call("accept_terms", old, actor=self.customer), "superseded")
        call("accept_terms", new, actor=self.customer)
        self.assertEqual(scalar(f"SELECT agreed_price_minor FROM app.contracts WHERE id={q(contract)}"), "20000")
        call("cancel_contract", contract, "Cancelado por el cliente", actor=self.customer)
        self.expect_denied(lambda: call("accept_terms", new, actor=self.customer), "terms_not_allowed")


def main():
    cfg = db.config()
    created = False
    try:
        db.compose("exec", "-T", "db", "createdb", "-U", cfg["POSTGRES_USER"], DATABASE)
        created = True
        db.migrate(DATABASE, quiet=True)
        db.migrate(DATABASE, quiet=True)  # El segundo pase debe ser inocuo.
        suite = unittest.TestSuite()
        if "--money-only" not in sys.argv:
            suite.addTests(unittest.defaultTestLoader.loadTestsFromTestCase(DatabaseTests))
        import test_money
        suite.addTests(test_money.build_suite(sys.modules[__name__]))
        if "--match" in sys.argv:
            pattern = sys.argv[sys.argv.index("--match")+1]
            def matching(group):
                for case in group:
                    if isinstance(case, unittest.TestSuite):
                        yield from matching(case)
                    elif pattern in case.id():
                        yield case
            suite = unittest.TestSuite(matching(suite))
            if not suite.countTestCases():
                raise ValueError("El filtro no coincide con ninguna prueba")
        result = unittest.TextTestRunner(verbosity=2).run(suite)
        sys.exit(0 if result.wasSuccessful() else 1)
    except subprocess.CalledProcessError as error:
        print(error.stderr or str(error), file=sys.stderr)
        raise
    finally:
        if created:
            # Solo la BD aleatoria que este proceso acaba de crear; ningún volumen.
            db.compose("exec", "-T", "db", "dropdb", "-U", cfg["POSTGRES_USER"], DATABASE)


if __name__ == "__main__":
    main()
