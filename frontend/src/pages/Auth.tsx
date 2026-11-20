import { useState, useEffect, type FormEvent } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import {
  ArrowLeft,
  ShieldCheck,
  Sparkles,
  UserRound,
  Building2,
  Check,
  AlertCircle,
  Eye,
  EyeOff,
  MailCheck,
  Mail,
  ArrowRight,
  LoaderCircle,
} from "lucide-react";
import { FieldsForm, ErrorBox } from "../components";
import { AuthShowcase } from "../components/AuthShowcase";
import { useAuth } from "../context";
import { request, setSession, ApiError } from "../api";

export type PasswordEvaluation = {
  score: number; // 0 to 4
  label: string;
  colorClass: "very-weak" | "weak" | "medium" | "strong" | "very-strong";
  hasMinLength: boolean;
  hasUpper: boolean;
  hasLower: boolean;
  hasNumber: boolean;
  hasSpecial: boolean;
};

export function evaluatePassword(pwd: string): PasswordEvaluation {
  const hasMinLength = pwd.length >= 12;
  const hasUpper = /[A-Z]/.test(pwd);
  const hasLower = /[a-z]/.test(pwd);
  const hasNumber = /[0-9]/.test(pwd);
  const hasSpecial = /[^A-Za-z0-9]/.test(pwd);

  let criteriaCount = 0;
  if (hasUpper) criteriaCount++;
  if (hasLower) criteriaCount++;
  if (hasNumber) criteriaCount++;
  if (hasSpecial) criteriaCount++;

  let score = 0;
  let label = "Insegura";
  let colorClass: PasswordEvaluation["colorClass"] = "very-weak";

  if (!pwd || pwd.length < 12) {
    score = 0;
    label = "Muy insegura";
    colorClass = "very-weak";
  } else {
    // length >= 12
    if (criteriaCount <= 1) {
      score = 1;
      label = "Insegura / Débil";
      colorClass = "weak";
    } else if (criteriaCount === 2) {
      score = 2;
      label = "Aceptable / Media";
      colorClass = "medium";
    } else if (criteriaCount === 3) {
      score = 3;
      label = "Segura";
      colorClass = "strong";
    } else {
      score = 4;
      label = "Muy segura";
      colorClass = "very-strong";
    }
  }

  return {
    score,
    label,
    colorClass,
    hasMinLength,
    hasUpper,
    hasLower,
    hasNumber,
    hasSpecial,
  };
}

export function PasswordStrengthMeter({ password }: { password: string }) {
  if (!password) return null;
  const evaluation = evaluatePassword(password);

  return (
    <div
      className="password-strength-container"
      role="status"
      aria-label={`Seguridad de contraseña: ${evaluation.label}`}
    >
      <div className="password-strength-header">
        <span className="strength-title">Nivel de seguridad:</span>
        <span className={`strength-badge ${evaluation.colorClass}`}>
          {evaluation.label}
        </span>
      </div>
      <div className="password-meter-bar">
        {[1, 2, 3, 4].map((seg) => (
          <div
            key={seg}
            className={`password-meter-segment ${
              evaluation.score >= seg ? `filled ${evaluation.colorClass}` : ""
            }`}
          />
        ))}
      </div>
      <div className="password-checklist">
        <div
          className={`checklist-item ${evaluation.hasMinLength ? "valid" : ""}`}
        >
          <Check size={13} className="check-icon" />
          <span>Mínimo 12 caracteres ({password.length}/12)</span>
        </div>
        <div
          className={`checklist-item ${
            evaluation.hasUpper && evaluation.hasLower ? "valid" : ""
          }`}
        >
          <Check size={13} className="check-icon" />
          <span>Mayúsculas y minúsculas</span>
        </div>
        <div
          className={`checklist-item ${evaluation.hasNumber ? "valid" : ""}`}
        >
          <Check size={13} className="check-icon" />
          <span>Al menos un número (0-9)</span>
        </div>
        <div
          className={`checklist-item ${evaluation.hasSpecial ? "valid" : ""}`}
        >
          <Check size={13} className="check-icon" />
          <span>Carácter especial (!@#$...)</span>
        </div>
      </div>
    </div>
  );
}

export function PasswordMatchIndicator({
  password,
  confirmPassword,
}: {
  password: string;
  confirmPassword: string;
}) {
  if (!confirmPassword) return null;
  const matches = password === confirmPassword;

  return (
    <div
      className={`password-match-indicator ${
        matches ? "matches" : "mismatch"
      }`}
      role="status"
    >
      {matches ? (
        <>
          <Check size={15} />
          <span>Las contraseñas coinciden ✓</span>
        </>
      ) : (
        <>
          <AlertCircle size={15} />
          <span>Las contraseñas no coinciden</span>
        </>
      )}
    </div>
  );
}

function RegistrationActivationNotice({
  email,
  onContinue,
}: {
  email: string;
  onContinue: () => void;
}) {
  const [resending, setResending] = useState(false);
  const [resendStatus, setResendStatus] = useState("");

  const handleResend = async () => {
    setResending(true);
    setResendStatus("");
    try {
      await request("/api/v1/auth/verification-email", "POST");
      setResendStatus("¡Correo reenviado con éxito! Revisa tu bandeja de entrada.");
    } catch (err: any) {
      setResendStatus(err?.message || "No se pudo reenviar el correo de activación.");
    } finally {
      setResending(false);
    }
  };

  return (
    <div className="activation-notice-card">
      <div className="activation-icon-wrap">
        <MailCheck size={42} className="activation-icon" />
      </div>
      <h2>¡Cuenta creada exitosamente!</h2>
      <p className="activation-lead">
        Hemos enviado un correo de activación a <strong>{email}</strong>.
      </p>
      <p className="activation-hint">
        Por favor revisa tu bandeja de entrada y haz clic en el enlace para verificar tu correo y activar todas las funciones de tu cuenta.
      </p>
      {resendStatus && <div className="activation-status-msg">{resendStatus}</div>}
      <div className="activation-actions">
        <button
          type="button"
          className="button secondary"
          onClick={handleResend}
          disabled={resending}
        >
          <Mail size={16} /> {resending ? "Enviando…" : "Reenviar correo de activación"}
        </button>
        <button type="button" className="button primary" onClick={onContinue}>
          Continuar al panel principal <ArrowRight size={16} />
        </button>
      </div>
    </div>
  );
}

export function AuthPage({ register = false }: { register?: boolean }) {
  const auth = useAuth(),
    navigate = useNavigate(),
    [accountType, setAccountType] = useState<"natural" | "juridico">("natural"),
    [forgot, setForgot] = useState(false),
    [message, setMessage] = useState(""),
    [formError, setFormError] = useState(""),
    [busy, setBusy] = useState(false),
    [registeredNotice, setRegisteredNotice] = useState<{
      email: string;
    } | null>(null),
    // Estados para la autorización de inicio de sesión por código de correo (Email OTP)
    [awaitingCode, setAwaitingCode] = useState<{ email: string; password: string } | null>(null),
    [authCode, setAuthCode] = useState(""),
    [codeResendCooldown, setCodeResendCooldown] = useState(0),
    [resendingCode, setResendingCode] = useState(false),
    [codeError, setCodeError] = useState(""),
    [codeSuccess, setCodeSuccess] = useState(""),
    // Form fields for registration
    [name, setName] = useState(""),
    [businessName, setBusinessName] = useState(""),
    [rif, setRif] = useState(""),
    [contactName, setContactName] = useState(""),
    [email, setEmail] = useState(""),
    [password, setPassword] = useState(""),
    [confirmPassword, setConfirmPassword] = useState(""),
    [showPassword, setShowPassword] = useState(false),
    [showConfirmPassword, setShowConfirmPassword] = useState(false);

  // Contador para reenvío de código
  useEffect(() => {
    if (codeResendCooldown <= 0) return;
    const timer = setInterval(() => {
      setCodeResendCooldown((prev: number) => (prev > 0 ? prev - 1 : 0));
    }, 1000);
    return () => clearInterval(timer);
  }, [codeResendCooldown]);

  const handleVerifyCode = async (e: FormEvent) => {
    e.preventDefault();
    if (!awaitingCode) return;
    const cleanCode = authCode.trim();
    if (cleanCode.length < 6) {
      setCodeError("Introduce el código de 6 dígitos enviado a tu correo.");
      return;
    }
    setBusy(true);
    setCodeError("");
    try {
      await auth.login({
        email: awaitingCode.email,
        password: awaitingCode.password,
        code: cleanCode,
      });
      navigate("/dashboard");
    } catch (err: any) {
      if (err instanceof ApiError) {
        setCodeError(err.message || "Código incorrecto o expirado.");
      } else {
        setCodeError(err?.message || "No se pudo verificar el código.");
      }
    } finally {
      setBusy(false);
    }
  };

  const handleResendCode = async () => {
    if (!awaitingCode || codeResendCooldown > 0 || resendingCode) return;
    setResendingCode(true);
    setCodeError("");
    try {
      await request("/api/v1/auth/resend-login-code", "POST", {
        email: awaitingCode.email,
        password: awaitingCode.password,
        client: "web",
      });
      setCodeSuccess("¡Nuevo código de 6 dígitos enviado! Revisa tu correo.");
      setCodeResendCooldown(60);
      setTimeout(() => setCodeSuccess(""), 4500);
    } catch (err: any) {
      setCodeError(err?.message || "No se pudo reenviar el código. Intenta de nuevo.");
    } finally {
      setResendingCode(false);
    }
  };

  const handleRegisterSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError("");

    if (password.length < 12) {
      setFormError("La contraseña debe tener al menos 12 caracteres.");
      return;
    }

    if (password !== confirmPassword) {
      setFormError("Las contraseñas no coinciden. Por favor corrígelas.");
      return;
    }

    if (accountType === "juridico") {
      if (!businessName.trim()) {
        setFormError("Indica la razón social de la empresa.");
        return;
      }
      if (!rif.trim()) {
        setFormError("Indica el RIF de la empresa (ej: J-12345678-9).");
        return;
      }
      if (!contactName.trim()) {
        setFormError("Indica el nombre de la persona de contacto.");
        return;
      }
    } else {
      if (!name.trim()) {
        setFormError("Indica tu nombre completo.");
        return;
      }
    }

    setBusy(true);
    try {
      const payload =
        accountType === "juridico"
          ? {
              account_type: "juridico",
              business_name: businessName.trim(),
              rif: rif.trim().toUpperCase(),
              contact_name: contactName.trim(),
              name: businessName.trim(),
              email: email.trim(),
              password,
              confirm_password: confirmPassword,
              client: "web",
            }
          : {
              account_type: "natural",
              name: name.trim(),
              email: email.trim(),
              password,
              confirm_password: confirmPassword,
              client: "web",
            };

      const result = await request("/api/v1/auth/register", "POST", payload);
      setSession(result);

      // Show activation notice
      setRegisteredNotice({
        email: email.trim(),
      });
    } catch (err: any) {
      if (err instanceof ApiError) {
        setFormError(err.message || "Error en los datos suministrados.");
      } else {
        setFormError(err?.message || "No se pudo completar el registro.");
      }
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="auth-layout">
      <div className="auth-story">
        <div className="auth-story-brand-row">
          <Link to="/explore" className="brand light" aria-label="SoyVinko">
            <img src="/logo-white.svg" alt="Vinko" className="brand-logo" />
          </Link>
        </div>

        <div className="auth-story-main-grid">
          <div className="auth-story-text-col">
            <div className="eyebrow">
              BUENAS CONEXIONES. GRANDES POSIBILIDADES.
            </div>
            <h1>
              Hay alguien
              <br />
              que sabe cómo
              <br />
              <em>hacerlo realidad.</em>
            </h1>
            <p>
              Encuentra ayuda para tu día a día, compra en marketplace y coordina tus servicios y envíos con total seguridad.
            </p>
            <div className="auth-trust">
              <ShieldCheck /> Tú decides con quién trabajar.
            </div>
          </div>

          <div className="auth-story-visual-col">
            <AuthShowcase />
          </div>
        </div>

        <div className="auth-story-footer">
          <span>Personas que conectan. Proyectos que avanzan.</span>
        </div>
      </div>
      <div className="auth-form">
        <Link className="back-link" to="/explore">
          <ArrowLeft size={16} /> Volver a explorar
        </Link>
        <div className="auth-form-inner">
          <Sparkles className="green" size={32} />
          <h1>
            {forgot
              ? "Recupera tu acceso"
              : register
                ? registeredNotice
                  ? "Verifica tu correo"
                  : "Empieza algo bueno."
                : awaitingCode
                  ? "Autoriza tu acceso"
                  : "Qué bueno verte."}
          </h1>
          <p className="muted">
            {forgot
              ? "Te ayudamos a volver a tu cuenta."
              : register
                ? registeredNotice
                  ? "Te hemos enviado las instrucciones de activación."
                  : "Una cuenta para contratar y ofrecer tus servicios."
                : awaitingCode
                  ? "Ingresa el código que te enviamos por correo para continuar."
                  : "Entra a tu cuenta y continuemos."}
          </p>

          {message && (
            <p className="success-box" role="status">
              {message}
            </p>
          )}

          {register && registeredNotice ? (
            <RegistrationActivationNotice
              email={registeredNotice.email}
              onContinue={() => navigate("/dashboard")}
            />
          ) : register && !forgot ? (
            <>
              <div
                className="account-type-tabs"
                role="tablist"
                aria-label="Tipo de cliente"
              >
                <button
                  type="button"
                  role="tab"
                  aria-selected={accountType === "natural"}
                  className={`account-type-tab ${
                    accountType === "natural" ? "active" : ""
                  }`}
                  onClick={() => {
                    setAccountType("natural");
                    setFormError("");
                  }}
                >
                  <UserRound size={19} />
                  <div className="tab-info">
                    <strong>Persona Natural</strong>
                    <small>Cuenta particular</small>
                  </div>
                </button>
                <button
                  type="button"
                  role="tab"
                  aria-selected={accountType === "juridico"}
                  className={`account-type-tab ${
                    accountType === "juridico" ? "active" : ""
                  }`}
                  onClick={() => {
                    setAccountType("juridico");
                    setFormError("");
                  }}
                >
                  <Building2 size={19} />
                  <div className="tab-info">
                    <strong>Empresa / Jurídico</strong>
                    <small>Empresas de Venezuela (RIF)</small>
                  </div>
                </button>
              </div>

              <form onSubmit={handleRegisterSubmit} className="fields-form">
                {accountType === "juridico" ? (
                  <>
                    <label>
                      <span>Razón Social de la empresa *</span>
                      <input
                        type="text"
                        name="business_name"
                        required
                        minLength={2}
                        maxLength={160}
                        autoComplete="organization"
                        value={businessName}
                        onChange={(e) => setBusinessName(e.target.value)}
                        placeholder="Ej: Inversiones Vinko, C.A."
                      />
                      <small className="field-hint">
                        Nombre legal registrado
                      </small>
                    </label>

                    <label>
                      <span>RIF de la empresa *</span>
                      <input
                        type="text"
                        name="rif"
                        required
                        minLength={6}
                        maxLength={20}
                        value={rif}
                        onChange={(e) => setRif(e.target.value)}
                        placeholder="Ej: J-12345678-9"
                      />
                      <small className="field-hint">
                        Registro de Información Fiscal (ej: J-12345678-9)
                      </small>
                    </label>

                    <label>
                      <span>Persona de contacto / Representante *</span>
                      <input
                        type="text"
                        name="contact_name"
                        required
                        minLength={2}
                        maxLength={120}
                        autoComplete="name"
                        value={contactName}
                        onChange={(e) => setContactName(e.target.value)}
                        placeholder="Nombre y apellido del responsable"
                      />
                    </label>

                    <label>
                      <span>Correo electrónico corporativo *</span>
                      <input
                        type="email"
                        name="email"
                        required
                        autoComplete="email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        placeholder="contacto@empresa.com"
                      />
                    </label>
                  </>
                ) : (
                  <>
                    <label>
                      <span>Tu nombre *</span>
                      <input
                        type="text"
                        name="name"
                        required
                        minLength={2}
                        maxLength={120}
                        autoComplete="name"
                        value={name}
                        onChange={(e) => setName(e.target.value)}
                        placeholder="Ej: Carlos Gómez"
                      />
                    </label>

                    <label>
                      <span>Correo electrónico *</span>
                      <input
                        type="email"
                        name="email"
                        required
                        autoComplete="email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        placeholder="tu@correo.com"
                      />
                    </label>
                  </>
                )}

                {/* Password field with strength meter */}
                <label className="password-field-wrap">
                  <span>Contraseña *</span>
                  <div className="password-input-row">
                    <input
                      type={showPassword ? "text" : "password"}
                      name="password"
                      required
                      minLength={12}
                      maxLength={128}
                      autoComplete="new-password"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      placeholder="Mínimo 12 caracteres"
                    />
                    <button
                      type="button"
                      className="password-toggle-btn"
                      onClick={() => setShowPassword(!showPassword)}
                      tabIndex={-1}
                      title={showPassword ? "Ocultar contraseña" : "Ver contraseña"}
                    >
                      {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                    </button>
                  </div>
                  <PasswordStrengthMeter password={password} />
                </label>

                {/* Password confirmation with comparison */}
                <label className="password-field-wrap">
                  <span>Confirmar contraseña *</span>
                  <div className="password-input-row">
                    <input
                      type={showConfirmPassword ? "text" : "password"}
                      name="confirm_password"
                      required
                      minLength={12}
                      maxLength={128}
                      autoComplete="new-password"
                      value={confirmPassword}
                      onChange={(e) => setConfirmPassword(e.target.value)}
                      placeholder="Repite tu contraseña exactamente"
                      className={
                        confirmPassword
                          ? password === confirmPassword
                            ? "input-match"
                            : "input-mismatch"
                          : ""
                      }
                    />
                    <button
                      type="button"
                      className="password-toggle-btn"
                      onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                      tabIndex={-1}
                      title={
                        showConfirmPassword
                          ? "Ocultar contraseña"
                          : "Ver contraseña"
                      }
                    >
                      {showConfirmPassword ? (
                        <EyeOff size={18} />
                      ) : (
                        <Eye size={18} />
                      )}
                    </button>
                  </div>
                  <PasswordMatchIndicator
                    password={password}
                    confirmPassword={confirmPassword}
                  />
                  {!confirmPassword && (
                    <small className="field-hint">
                      Vuelve a escribir la contraseña para confirmar que coincidan.
                    </small>
                  )}
                </label>

                {formError && <ErrorBox message={formError} />}

                <button
                  type="submit"
                  className="button primary"
                  disabled={
                    busy ||
                    (!!confirmPassword && password !== confirmPassword)
                  }
                >
                  {busy ? (
                    <LoaderCircle size={18} className="spin" />
                  ) : (
                    <Check size={18} />
                  )}{" "}
                  {busy
                    ? "Creando cuenta…"
                    : accountType === "juridico"
                      ? "Crear cuenta de empresa"
                      : "Crear mi cuenta"}
                </button>

                <p className="auth-legal-terms-notice">
                  Al registrarte, confirmas que has leído y aceptas nuestros{" "}
                  <Link to="/terminos" target="_blank">Términos de Servicio</Link>, la{" "}
                  <Link to="/privacidad" target="_blank">Política de Privacidad</Link> y el uso de cookies técnicas esenciales.
                </p>
              </form>
            </>
          ) : awaitingCode ? (
            <div className="login-otp-card">
              <div className="login-otp-icon-wrap">
                <ShieldCheck size={38} className="login-otp-icon" />
              </div>
              <h2>Código de autorización</h2>
              <p className="login-otp-lead">
                Hemos enviado un código de 6 dígitos a <strong>{awaitingCode.email}</strong>.
              </p>
              <p className="login-otp-hint">
                Revisa tu bandeja de entrada o spam e ingresa el código numérico para acceder.
              </p>

              {codeError && (
                <div className="error-box" role="alert" style={{ marginBottom: "16px" }}>
                  {codeError}
                </div>
              )}
              {codeSuccess && (
                <div className="activation-status-msg">{codeSuccess}</div>
              )}

              <form onSubmit={handleVerifyCode} className="login-otp-form">
                <div className="field">
                  <label htmlFor="login-auth-code">Código de 6 dígitos</label>
                  <input
                    id="login-auth-code"
                    type="text"
                    inputMode="numeric"
                    pattern="[0-9]*"
                    maxLength={6}
                    autoFocus
                    className="login-otp-input"
                    placeholder="000000"
                    value={authCode}
                    onChange={(e) => {
                      const val = e.target.value.replace(/\D/g, "").slice(0, 6);
                      setAuthCode(val);
                      setCodeError("");
                    }}
                    disabled={busy}
                    required
                  />
                </div>

                <div className="login-otp-actions">
                  <button
                    type="submit"
                    className="button primary"
                    disabled={busy || authCode.length < 6}
                  >
                    {busy ? (
                      <>
                        <LoaderCircle className="spin" size={18} /> Verificando código…
                      </>
                    ) : (
                      <>
                        <Check size={18} /> Autorizar e Iniciar sesión
                      </>
                    )}
                  </button>

                  <button
                    type="button"
                    className="button secondary"
                    onClick={handleResendCode}
                    disabled={resendingCode || codeResendCooldown > 0}
                  >
                    <Mail size={16} />{" "}
                    {resendingCode
                      ? "Enviando nuevo código…"
                      : codeResendCooldown > 0
                        ? `Reenviar código (${codeResendCooldown}s)`
                        : "Reenviar código al correo"}
                  </button>

                  <button
                    type="button"
                    className="link-button"
                    style={{ marginTop: "12px", alignSelf: "center" }}
                    onClick={() => {
                      setAwaitingCode(null);
                      setAuthCode("");
                      setCodeError("");
                      setCodeSuccess("");
                    }}
                  >
                    ← Volver al inicio de sesión
                  </button>
                </div>
              </form>
            </div>
          ) : (
            <>
              {formError && !register && (
                <div className="error-box" role="alert" style={{ marginBottom: "16px" }}>
                  {formError}
                </div>
              )}
              <FieldsForm
                key={`${register}-${forgot}`}
                fields={
                  forgot
                    ? [
                        {
                          name: "email",
                          label: "Correo electrónico",
                          type: "email",
                        },
                      ]
                    : [
                        {
                          name: "email",
                          label: "Correo electrónico",
                          type: "email",
                          autoComplete: "email",
                        },
                        {
                          name: "password",
                          label: "Contraseña",
                          type: "password",
                          autoComplete: "current-password",
                        },
                      ]
                }
                submit={forgot ? "Enviar instrucciones" : "Iniciar sesión"}
                onSubmit={async (values) => {
                  if (forgot) {
                    await request("/api/v1/auth/forgot-password", "POST", values);
                    setMessage(
                      "Si la cuenta existe, recibirás las instrucciones en tu correo electrónico.",
                    );
                    return;
                  }
                  setFormError("");
                  setMessage("");
                  setBusy(true);
                  try {
                    const res: any = await auth.login({
                      email: values.email.trim(),
                      password: values.password,
                    });
                    if (res?.status === "code_required") {
                      setAwaitingCode({
                        email: values.email.trim(),
                        password: values.password,
                      });
                      setCodeResendCooldown(60);
                      setAuthCode("");
                      setCodeError("");
                      return;
                    }
                    navigate("/dashboard");
                  } catch (err: any) {
                    if (err instanceof ApiError) {
                      setFormError(err.message || "Credenciales incorrectas.");
                    } else {
                      setFormError(err?.message || "No se pudo iniciar sesión.");
                    }
                  } finally {
                    setBusy(false);
                  }
                }}
              />
            </>
          )}

          {!register && !awaitingCode && (
            <button
              className="link-button"
              onClick={() => {
                setForgot(!forgot);
                setMessage("");
              }}
            >
              {forgot ? "Volver al inicio de sesión" : "Olvidé mi contraseña"}
            </button>
          )}

          {!awaitingCode && (
            <p className="auth-switch">
              {register ? "¿Ya tienes una cuenta?" : "¿Es tu primera vez?"}{" "}
              <Link to={register ? "/login" : "/register"}>
                {register ? "Inicia sesión" : "Crea tu cuenta"}
              </Link>
            </p>
          )}
        </div>
      </div>
    </div>
  );
}

export function VerifyPage() {
  const [params] = useSearchParams(),
    auth = useAuth(),
    [message, setMessage] = useState(""),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(false),
    [password, setPassword] = useState(""),
    [confirmPassword, setConfirmPassword] = useState(""),
    [showPassword, setShowPassword] = useState(false),
    [showConfirmPassword, setShowConfirmPassword] = useState(false);

  const purpose = params.get("purpose") ?? "email_verification";
  const token = params.get("token");

  const handleConfirmEmail = async () => {
    if (!token) {
      setError("Falta el código de verificación en el enlace.");
      return;
    }
    setBusy(true);
    setError("");
    try {
      await request("/api/v1/auth/consume-challenge", "POST", {
        token,
        purpose: "email_verification",
      });
      setMessage("¡Tu correo ha sido verificado y tu cuenta está activada!");
      await auth.reload().catch(() => {});
    } catch (err: any) {
      setError(err?.message || "El enlace venció o ya fue utilizado.");
    } finally {
      setBusy(false);
    }
  };

  const handleResetPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!token) {
      setError("Falta el código de restablecimiento en el enlace.");
      return;
    }
    if (password.length < 12) {
      setError("La nueva contraseña debe tener al menos 12 caracteres.");
      return;
    }
    if (password !== confirmPassword) {
      setError("Las contraseñas no coinciden.");
      return;
    }
    setBusy(true);
    setError("");
    try {
      await request("/api/v1/auth/consume-challenge", "POST", {
        token,
        purpose: "password_reset",
        password,
      });
      setMessage("Contraseña actualizada correctamente. Ya puedes iniciar sesión.");
    } catch (err: any) {
      setError(err?.message || "El enlace venció o ya fue utilizado.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="auth-form standalone">
      <div className="auth-form-inner">
        {purpose === "email_verification" ? (
          <>
            <div className="verify-header">
              <MailCheck size={44} className="green" />
              <h1>Activación de cuenta</h1>
              <p className="muted">
                Confirma la verificación de tu correo electrónico en SoyVinko.
              </p>
            </div>

            {message ? (
              <div className="verify-success-box">
                <Check size={32} className="green" />
                <p>
                  <strong>{message}</strong>
                </p>
                <div className="verify-actions">
                  <Link to="/dashboard" className="button primary">
                    Ir al panel principal
                  </Link>
                  <Link to="/explore" className="button secondary">
                    Explorar servicios
                  </Link>
                </div>
              </div>
            ) : (
              <div className="verify-card">
                {error && <ErrorBox message={error} />}
                <p>
                  Haz clic en el botón a continuación para completar la activación de tu cuenta y verificar tu correo electrónico.
                </p>
                <button
                  type="button"
                  className="button primary full-width"
                  onClick={handleConfirmEmail}
                  disabled={busy}
                >
                  {busy ? (
                    <LoaderCircle size={18} className="spin" />
                  ) : (
                    <Check size={18} />
                  )}{" "}
                  {busy ? "Activando…" : "Confirmar y activar cuenta"}
                </button>
              </div>
            )}
          </>
        ) : (
          <>
            <h1>Nueva contraseña</h1>
            <p className="muted">Define tu nueva clave de acceso para SoyVinko.</p>

            {message ? (
              <div className="verify-success-box">
                <Check size={32} className="green" />
                <p>
                  <strong>{message}</strong>
                </p>
                <div className="verify-actions">
                  <Link to="/login" className="button primary">
                    Iniciar sesión
                  </Link>
                </div>
              </div>
            ) : (
              <form onSubmit={handleResetPassword} className="fields-form">
                <label className="password-field-wrap">
                  <span>Nueva contraseña *</span>
                  <div className="password-input-row">
                    <input
                      type={showPassword ? "text" : "password"}
                      name="password"
                      required
                      minLength={12}
                      maxLength={128}
                      autoComplete="new-password"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      placeholder="Mínimo 12 caracteres"
                    />
                    <button
                      type="button"
                      className="password-toggle-btn"
                      onClick={() => setShowPassword(!showPassword)}
                      tabIndex={-1}
                      title={showPassword ? "Ocultar contraseña" : "Ver contraseña"}
                    >
                      {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                    </button>
                  </div>
                  <PasswordStrengthMeter password={password} />
                </label>

                <label className="password-field-wrap">
                  <span>Confirmar nueva contraseña *</span>
                  <div className="password-input-row">
                    <input
                      type={showConfirmPassword ? "text" : "password"}
                      name="confirm_password"
                      required
                      minLength={12}
                      maxLength={128}
                      autoComplete="new-password"
                      value={confirmPassword}
                      onChange={(e) => setConfirmPassword(e.target.value)}
                      placeholder="Repite tu contraseña exactamente"
                      className={
                        confirmPassword
                          ? password === confirmPassword
                            ? "input-match"
                            : "input-mismatch"
                          : ""
                      }
                    />
                    <button
                      type="button"
                      className="password-toggle-btn"
                      onClick={() =>
                        setShowConfirmPassword(!showConfirmPassword)
                      }
                      tabIndex={-1}
                      title={
                        showConfirmPassword
                          ? "Ocultar contraseña"
                          : "Ver contraseña"
                      }
                    >
                      {showConfirmPassword ? (
                        <EyeOff size={18} />
                      ) : (
                        <Eye size={18} />
                      )}
                    </button>
                  </div>
                  <PasswordMatchIndicator
                    password={password}
                    confirmPassword={confirmPassword}
                  />
                </label>

                {error && <ErrorBox message={error} />}

                <button
                  type="submit"
                  className="button primary"
                  disabled={
                    busy ||
                    (!!confirmPassword && password !== confirmPassword)
                  }
                >
                  {busy ? (
                    <LoaderCircle size={18} className="spin" />
                  ) : (
                    <Check size={18} />
                  )}{" "}
                  {busy ? "Guardando…" : "Actualizar contraseña"}
                </button>
              </form>
            )}
          </>
        )}
      </div>
    </div>
  );
}
