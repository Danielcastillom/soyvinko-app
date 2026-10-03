import urllib.request
import urllib.error
import json
import uuid

API_BASE = "http://127.0.0.1:8181"

def request(path, method="GET", body=None, token=None):
    url = f"{API_BASE}{path}"
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as resp:
            return resp.status, json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        content = e.read().decode("utf-8")
        try:
            return e.code, json.loads(content)
        except Exception:
            return e.code, content

def main():
    print("1. Registering test user...")
    test_email = f"test_ve_{uuid.uuid4().hex[:8]}@example.com"
    status, res = request("/api/v1/auth/register", "POST", {
        "email": test_email,
        "name": "Carlos Pérez",
        "password": "Password123!",
        "client": "web"
    })
    assert status == 201, f"Registration failed: {res}"
    token = res["data"]["access_token"]
    print(f"   Registered successfully! User token: {token[:15]}...")

    print("\n2. Testing Venezuelan Bank Transfer Payout Method (Banesco 0134)...")
    bank_payload = {
        "method_type": "bank_transfer",
        "currency": "VES",
        "bank_code": "0134",
        "account_number": "01340123456789012345",
        "account_type": "corriente",
        "document_type": "V",
        "document_number": "12345678",
        "holder_name": "Carlos Pérez",
        "label": "Mi Banesco Corriente"
    }
    status, res = request("/api/v1/integrations/payout-methods", "POST", bank_payload, token)
    print(f"   Bank Transfer response: {status} -> {res}")
    assert status == 200, f"Bank transfer creation failed: {res}"
    bank_method_id = res["data"]
    print(f"   Created Bank Method ID: {bank_method_id}")

    print("\n3. Testing Pago Móvil Payout Method (Banco de Venezuela 0102)...")
    pm_payload = {
        "method_type": "pago_movil",
        "currency": "VES",
        "bank_code": "0102",
        "phone_prefix": "0414",
        "phone_number": "1234567",
        "document_type": "V",
        "document_number": "12345678",
        "holder_name": "Carlos Pérez",
        "label": "Mi Pago Móvil BDV"
    }
    status, res = request("/api/v1/integrations/payout-methods", "POST", pm_payload, token)
    print(f"   Pago Móvil response: {status} -> {res}")
    assert status == 200, f"Pago Móvil creation failed: {res}"
    pm_method_id = res["data"]
    print(f"   Created Pago Móvil Method ID: {pm_method_id}")

    print("\n4. Checking my_payout_methods database function...")
    status, res = request("/api/v1/wallet/my-payout-methods", "GET", None, token)
    print(f"   my_payout_methods response: {status} -> {res}")
    assert status == 200, f"Failed getting payout methods: {res}"
    methods = res["data"]
    assert len(methods) >= 2, f"Expected at least 2 payout methods, got {len(methods)}"
    
    for m in methods:
        print(f"   - [{m['provider']}] ID: {m['id']} | Status: {m['status']} | Label: {m['label']} | Masked: {m['masked_account']}")
        assert m["status"] == "verified", f"Method {m['id']} is not verified: {m['status']}"

    print("\n5. Testing Venezuelan Bank Validation Rules...")
    # Bad account length (19 digits)
    bad_bank = dict(bank_payload, account_number="0134012345678901234")
    status, res = request("/api/v1/integrations/payout-methods", "POST", bad_bank, token)
    print(f"   19-digit account check: {status} (Expected 422) -> {res}")
    assert status == 422

    # Mismatched bank code (0102 selected but account starts with 0134)
    bad_match = dict(bank_payload, bank_code="0102", account_number="01340123456789012345")
    status, res = request("/api/v1/integrations/payout-methods", "POST", bad_match, token)
    print(f"   Mismatched bank code check: {status} (Expected 422) -> {res}")
    assert status == 422

    # Bad Pago Movil phone (0413 prefix does not exist in Venezuela)
    bad_pm = dict(pm_payload, phone_prefix="0413")
    status, res = request("/api/v1/integrations/payout-methods", "POST", bad_pm, token)
    print(f"   Invalid phone prefix check: {status} (Expected 422) -> {res}")
    assert status == 422

    print("\nALL VENEZUELAN PAYOUT METHODS VERIFICATIONS PASSED SUCCESSFULLY!")

if __name__ == "__main__":
    main()
