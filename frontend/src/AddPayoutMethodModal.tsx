import { useState, type FormEvent } from "react";
import { Landmark, Smartphone, CheckCircle2, ShieldCheck, ArrowRight, Loader2 } from "lucide-react";
import {
  VENEZUELAN_BANKS,
  findBankByCode,
  detectBankFromAccountNumber,
  VENEZUELAN_PHONE_PREFIXES,
  DOCUMENT_TYPES,
  type VenezuelanBank,
} from "./venezuelanBanks";
import { request } from "./api";
import { Modal, ErrorBox } from "./components";

interface AddPayoutMethodModalProps {
  onClose: () => void;
  onSuccess: () => void;
  defaultCurrency?: string;
}

export function AddPayoutMethodModal({
  onClose,
  onSuccess,
  defaultCurrency = "VES",
}: AddPayoutMethodModalProps) {
  const [tab, setTab] = useState<"bank_transfer" | "pago_movil">("bank_transfer");

  // Bank transfer state
  const [bankAccount, setBankAccount] = useState("");
  const [bankCode, setBankCode] = useState(VENEZUELAN_BANKS[0].code);
  const [accountType, setAccountType] = useState<"corriente" | "ahorro">("corriente");
  const [bankCurrency, setBankCurrency] = useState(defaultCurrency === "USD" ? "USD" : "VES");

  // Pago Móvil state
  const [pmBankCode, setPmBankCode] = useState(VENEZUELAN_BANKS[0].code);
  const [phonePrefix, setPhonePrefix] = useState(VENEZUELAN_PHONE_PREFIXES[0]);
  const [phoneRest, setPhoneRest] = useState("");

  // Common Identity state
  const [docType, setDocType] = useState("V");
  const [docNumber, setDocNumber] = useState("");
  const [holderName, setHolderName] = useState("");
  const [customLabel, setCustomLabel] = useState("");

  // Loading & Error states
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Auto-detect bank from account number
  const cleanAccount = bankAccount.replace(/\D/g, "");
  const detectedBank: VenezuelanBank | undefined =
    cleanAccount.length >= 4 ? detectBankFromAccountNumber(cleanAccount) : undefined;

  const handleAccountChange = (val: string) => {
    const raw = val.replace(/\D/g, "").slice(0, 20);
    setBankAccount(raw);
    if (raw.length >= 4) {
      const detected = detectBankFromAccountNumber(raw);
      if (detected) {
        setBankCode(detected.code);
      }
    }
  };

  const handlePhoneRestChange = (val: string) => {
    const raw = val.replace(/\D/g, "").slice(0, 7);
    setPhoneRest(raw);
  };

  const handleDocNumberChange = (val: string) => {
    const raw = val.replace(/\D/g, "").slice(0, 10);
    setDocNumber(raw);
  };

  const selectedBank = findBankByCode(tab === "bank_transfer" ? bankCode : pmBankCode);

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setError(null);

    // Common validations
    const cleanDoc = docNumber.trim().replace(/\D/g, "");
    if (!cleanDoc || cleanDoc.length < 5) {
      setError("Por favor ingresa un número de cédula o RIF válido (mínimo 5 dígitos).");
      return;
    }

    const cleanHolder = holderName.trim();
    if (cleanHolder.length < 3) {
      setError("Por favor ingresa el nombre y apellido o razón social del titular.");
      return;
    }

    if (tab === "bank_transfer") {
      if (cleanAccount.length !== 20) {
        setError(`El número de cuenta bancaria debe tener 20 dígitos (actualmente tiene ${cleanAccount.length}).`);
        return;
      }
      if (!cleanAccount.startsWith(bankCode)) {
        setError(
          `Los primeros 4 dígitos de la cuenta (${cleanAccount.slice(0, 4)}) no coinciden con el banco seleccionado (${bankCode}).`
        );
        return;
      }

      setLoading(true);
      try {
        await request("/api/v1/integrations/payout-methods", "POST", {
          method_type: "bank_transfer",
          currency: bankCurrency,
          bank_code: bankCode,
          account_number: cleanAccount,
          account_type: accountType,
          document_type: docType,
          document_number: cleanDoc,
          holder_name: cleanHolder,
          label: customLabel.trim() || undefined,
        });
        onSuccess();
        onClose();
      } catch (err: any) {
        setError(err?.message || "Ocurrió un error al registrar la cuenta bancaria.");
      } finally {
        setLoading(false);
      }
    } else {
      // Pago Móvil validation
      const cleanPhone = phoneRest.trim().replace(/\D/g, "");
      if (cleanPhone.length !== 7) {
        setError(`El número de celular debe tener 7 dígitos después del prefijo ${phonePrefix}.`);
        return;
      }

      setLoading(true);
      try {
        await request("/api/v1/integrations/payout-methods", "POST", {
          method_type: "pago_movil",
          currency: "VES",
          bank_code: pmBankCode,
          phone_prefix: phonePrefix,
          phone_number: cleanPhone,
          document_type: docType,
          document_number: cleanDoc,
          holder_name: cleanHolder,
          label: customLabel.trim() || undefined,
        });
        onSuccess();
        onClose();
      } catch (err: any) {
        setError(err?.message || "Ocurrió un error al registrar el Pago Móvil.");
      } finally {
        setLoading(false);
      }
    }
  };

  return (
    <Modal title="Añadir método de retiro nacional" onClose={onClose}>
      <div className="payout-modal-container">
        {/* Type Selector Tabs */}
        <div className="payout-type-tabs" role="tablist">
          <button
            type="button"
            role="tab"
            aria-selected={tab === "bank_transfer"}
            className={`payout-tab-btn ${tab === "bank_transfer" ? "active" : ""}`}
            onClick={() => {
              setTab("bank_transfer");
              setError(null);
            }}
          >
            <Landmark size={18} />
            <span>Transferencia Bancaria</span>
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={tab === "pago_movil"}
            className={`payout-tab-btn ${tab === "pago_movil" ? "active" : ""}`}
            onClick={() => {
              setTab("pago_movil");
              setError(null);
            }}
          >
            <Smartphone size={18} />
            <span>Pago Móvil</span>
          </button>
        </div>

        <small className="form-hint">
          Por seguridad, la cuenta debe estar a tu nombre: el documento del titular tiene que coincidir con tu
          verificación de identidad aprobada y tu correo debe estar confirmado.
        </small>

        {error && <ErrorBox message={error} />}

        <form onSubmit={handleSubmit} className="payout-form">
          {tab === "bank_transfer" ? (
            <>
              {/* Bank Account Number */}
              <div className="form-group">
                <label htmlFor="pm-account">
                  Número de cuenta bancaria (20 dígitos)
                  <span className="digits-counter">
                    {cleanAccount.length}/20 dígitos
                  </span>
                </label>
                <div className="input-with-badge">
                  <input
                    id="pm-account"
                    type="text"
                    inputMode="numeric"
                    placeholder="01340000000000000000"
                    value={bankAccount}
                    onChange={(e) => handleAccountChange(e.target.value)}
                    required
                    autoFocus
                  />
                  {detectedBank && (
                    <div className="bank-detected-badge" title="Banco auto-detectado">
                      <CheckCircle2 size={15} />
                      <span>{detectedBank.shortName}</span>
                    </div>
                  )}
                </div>
                <small className="form-hint">
                  Ingresa los 20 dígitos continuos de tu cuenta bancaria venezolana.
                </small>
              </div>

              {/* Bank Selector */}
              <div className="form-group">
                <label htmlFor="pm-bank">Banco receptor</label>
                <select
                  id="pm-bank"
                  value={bankCode}
                  onChange={(e) => setBankCode(e.target.value)}
                >
                  {VENEZUELAN_BANKS.map((b) => (
                    <option key={b.code} value={b.code}>
                      {b.code} · {b.shortName}
                    </option>
                  ))}
                </select>
              </div>

              {/* Account Type & Currency */}
              <div className="form-row two-col-row">
                <div className="form-group">
                  <label>Tipo de cuenta</label>
                  <div className="segmented-toggle">
                    <button
                      type="button"
                      className={`seg-btn ${accountType === "corriente" ? "active" : ""}`}
                      onClick={() => setAccountType("corriente")}
                    >
                      Corriente
                    </button>
                    <button
                      type="button"
                      className={`seg-btn ${accountType === "ahorro" ? "active" : ""}`}
                      onClick={() => setAccountType("ahorro")}
                    >
                      Ahorro
                    </button>
                  </div>
                </div>

                <div className="form-group">
                  <label htmlFor="pm-currency">Moneda de cuenta</label>
                  <select
                    id="pm-currency"
                    value={bankCurrency}
                    onChange={(e) => setBankCurrency(e.target.value)}
                  >
                    <option value="VES">Bs · Bolívares (VES)</option>
                    <option value="USD">USD · Cuenta en Divisas</option>
                  </select>
                </div>
              </div>
            </>
          ) : (
            <>
              {/* Pago Móvil Bank Selector */}
              <div className="form-group">
                <label htmlFor="pm-pago-bank">Banco de Pago Móvil</label>
                <select
                  id="pm-pago-bank"
                  value={pmBankCode}
                  onChange={(e) => setPmBankCode(e.target.value)}
                >
                  {VENEZUELAN_BANKS.filter((b) => b.pagoMovil).map((b) => (
                    <option key={b.code} value={b.code}>
                      {b.code} · {b.shortName}
                    </option>
                  ))}
                </select>
              </div>

              {/* Pago Móvil Phone */}
              <div className="form-group">
                <label htmlFor="pm-phone-rest">Número de teléfono móvil</label>
                <div className="phone-input-combo">
                  <select
                    id="pm-phone-prefix"
                    aria-label="Prefijo de operador"
                    value={phonePrefix}
                    onChange={(e) => setPhonePrefix(e.target.value)}
                  >
                    {VENEZUELAN_PHONE_PREFIXES.map((prefix) => (
                      <option key={prefix} value={prefix}>
                        {prefix}
                      </option>
                    ))}
                  </select>
                  <input
                    id="pm-phone-rest"
                    type="text"
                    inputMode="numeric"
                    placeholder="1234567"
                    value={phoneRest}
                    onChange={(e) => handlePhoneRestChange(e.target.value)}
                    required
                    maxLength={7}
                    autoFocus
                  />
                </div>
                <small className="form-hint">
                  Prefijo móvil (0414, 0424, 0412, 0416, 0426) y 7 dígitos.
                </small>
              </div>
            </>
          )}

          {/* Document Type and Number */}
          <div className="form-group">
            <label htmlFor="pm-doc-num">Documento de identidad del titular</label>
            <div className="doc-input-combo">
              <select
                id="pm-doc-type"
                aria-label="Tipo de documento"
                value={docType}
                onChange={(e) => setDocType(e.target.value)}
              >
                {DOCUMENT_TYPES.map((d) => (
                  <option key={d.value} value={d.value}>
                    {d.label}
                  </option>
                ))}
              </select>
              <input
                id="pm-doc-num"
                type="text"
                inputMode="numeric"
                placeholder="Número de cédula o RIF"
                value={docNumber}
                onChange={(e) => handleDocNumberChange(e.target.value)}
                required
              />
            </div>
          </div>

          {/* Holder Name */}
          <div className="form-group">
            <label htmlFor="pm-holder">Nombre completo / Razón social del titular</label>
            <input
              id="pm-holder"
              type="text"
              placeholder="Ej: Pedro Pérez o Vinko Solutions C.A."
              value={holderName}
              onChange={(e) => setHolderName(e.target.value)}
              required
            />
            <small className="form-hint">
              Debe coincidir exactamente con el titular registrado en la entidad bancaria.
            </small>
          </div>

          {/* Optional Custom Label / Alias */}
          <div className="form-group">
            <label htmlFor="pm-label">
              Alias personalizado <span className="muted">(Opcional)</span>
            </label>
            <input
              id="pm-label"
              type="text"
              placeholder={
                tab === "bank_transfer"
                  ? `${selectedBank?.shortName || "Banco"} personal`
                  : `Pago Móvil ${selectedBank?.shortName || ""}`
              }
              value={customLabel}
              onChange={(e) => setCustomLabel(e.target.value)}
            />
          </div>

          <div className="payout-security-notice">
            <ShieldCheck size={18} />
            <span>
              Verificación instantánea para retiros. Tus fondos estarán protegidos bajo los protocolos bancarios nacionales.
            </span>
          </div>

          <div className="modal-actions-bar">
            <button
              type="button"
              className="button outline-light"
              onClick={onClose}
              disabled={loading}
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="button primary"
              disabled={loading}
            >
              {loading ? (
                <>
                  <Loader2 size={16} className="spin" /> Guardando...
                </>
              ) : (
                <>
                  <span>Guardar método de retiro</span>
                  <ArrowRight size={16} />
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </Modal>
  );
}
