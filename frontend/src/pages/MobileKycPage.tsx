import { useState, useRef, useEffect, type FormEvent, type ChangeEvent } from "react";
import { useSearchParams, Link } from "react-router-dom";
import {
  Camera,
  CheckCircle2,
  AlertTriangle,
  Loader2,
  RefreshCw,
  ArrowRight,
  ShieldCheck,
  Check,
  Sparkles,
  Lock,
  UserCheck,
} from "lucide-react";
import { request, setSession, type User } from "../api";
import { useAuth } from "../context";

export function MobileKycPage() {
  const [params] = useSearchParams();
  // El QR nuevo trae el token en el fragmento (#token=) para que no llegue a logs ni Referer;
  // se acepta ?token= por compatibilidad con códigos generados antes del cambio.
  const [token] = useState(
    () =>
      new URLSearchParams(window.location.hash.slice(1)).get("token") ||
      params.get("token") ||
      params.get("kyc_auth"),
  );
  useEffect(() => {
    if (window.location.hash.includes("token="))
      window.history.replaceState(null, "", window.location.pathname);
  }, []);
  const { user: authUser } = useAuth();

  const [loadingAuth, setLoadingAuth] = useState(true);
  const [authError, setAuthError] = useState<string | null>(null);

  // Pasos: 1 = Cédula, 2 = Selfie, 3 = Confirmar y Enviar, 4 = Éxito
  const [step, setStep] = useState<1 | 2 | 3 | 4>(1);

  // Cámara WebRTC
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const [cameraActive, setCameraActive] = useState(false);
  const [cameraLoading, setCameraLoading] = useState(false);

  // Fallback nativo
  const frontInputRef = useRef<HTMLInputElement | null>(null);
  const selfieInputRef = useRef<HTMLInputElement | null>(null);

  // Capturas
  const [frontBlob, setFrontBlob] = useState<Blob | null>(null);
  const [frontPreview, setFrontPreview] = useState<string | null>(null);

  const [selfieBlob, setSelfieBlob] = useState<Blob | null>(null);
  const [selfiePreview, setSelfiePreview] = useState<string | null>(null);

  // OCR
  const [scanning, setScanning] = useState(false);
  const [ocrData, setOcrData] = useState<any | null>(null);
  const [ocrError, setOcrError] = useState<string | null>(null);

  // Campos de confirmación
  const [docType, setDocType] = useState("V");
  const [docNumber, setDocNumber] = useState("");
  const [fullName, setFullName] = useState("");
  const [birthdate, setBirthdate] = useState("");
  const [phonePrefix, setPhonePrefix] = useState("0414");
  const [phoneRest, setPhoneRest] = useState("");
  const [address, setAddress] = useState("");

  // Estado de envío
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  // 1. Inicialización y Autenticación con Token de URL
  useEffect(() => {
    let isMounted = true;
    const initAuth = async () => {
      setLoadingAuth(true);
      setAuthError(null);

      if (token) {
        setSession({ access_token: token });
      }

      try {
        const me = await request<User>("/api/v1/auth/me");
        if (isMounted) {
          if (me?.name) {
            setFullName(me.name);
          }
          if (me?.rif) {
            const parts = me.rif.split("-");
            if (parts.length === 2) {
              setDocType(parts[0]);
              setDocNumber(parts[1]);
            }
          }
        }
      } catch (err: any) {
        console.warn("Auth initialization error:", err);
        if (isMounted) {
          if (!authUser) {
            setAuthError(
              "El enlace de verificación móvil es inválido o ha expirado. Por favor genera un nuevo código QR en tu computadora."
            );
          }
        }
      } finally {
        if (isMounted) setLoadingAuth(false);
      }
    };

    initAuth();
    return () => {
      isMounted = false;
    };
  }, [token, authUser]);

  // 2. Control del Visor de Cámara WebRTC
  const stopCamera = () => {
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((track: any) => track.stop());
      streamRef.current = null;
    }
    setCameraActive(false);
  };

  const startCamera = async (facing: "environment" | "user") => {
    stopCamera();
    setCameraLoading(true);

    if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
      try {
        const constraints: MediaStreamConstraints = {
          video: {
            facingMode: facing === "environment" ? { ideal: "environment" } : "user",
            width: { ideal: 1920, min: 640 },
            height: { ideal: 1080, min: 480 },
          },
          audio: false,
        };
        const stream = await navigator.mediaDevices.getUserMedia(constraints);
        streamRef.current = stream;
        setCameraActive(true);
        setCameraLoading(false);

        setTimeout(() => {
          if (videoRef.current) {
            videoRef.current.srcObject = stream;
            videoRef.current.play().catch(() => {});
          }
        }, 80);
        return;
      } catch (err: any) {
        console.warn("getUserMedia failed or blocked:", err);
        setCameraLoading(false);
      }
    } else {
      setCameraLoading(false);
    }
  };

  // Gestión de cámara al cambiar paso
  useEffect(() => {
    if (loadingAuth || authError) return;

    if (step === 1 && !frontPreview) {
      startCamera("environment");
    } else if (step === 2 && !selfiePreview) {
      startCamera("user");
    } else {
      stopCamera();
    }

    return () => stopCamera();
  }, [step, frontPreview, selfiePreview, loadingAuth, authError]);

  // Capturar fotograma desde video a Blob
  const captureFrame = (target: "front" | "selfie") => {
    if (!videoRef.current) return;
    const video = videoRef.current;
    const canvas = document.createElement("canvas");
    canvas.width = video.videoWidth || 1280;
    canvas.height = video.videoHeight || 720;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    if (target === "selfie") {
      ctx.translate(canvas.width, 0);
      ctx.scale(-1, 1);
    }
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);

    canvas.toBlob(
      async (blob) => {
        if (!blob) return;
        stopCamera();
        const url = URL.createObjectURL(blob);
        if (target === "front") {
          setFrontBlob(blob);
          setFrontPreview(url);
          await triggerOcr(blob);
        } else {
          setSelfieBlob(blob);
          setSelfiePreview(url);
        }
      },
      "image/jpeg",
      0.92
    );
  };

  // Fallback nativo de cámara
  const handleNativeFallback = async (
    e: ChangeEvent<HTMLInputElement>,
    target: "front" | "selfie"
  ) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const url = URL.createObjectURL(file);
    if (target === "front") {
      setFrontBlob(file);
      setFrontPreview(url);
      await triggerOcr(file);
    } else {
      setSelfieBlob(file);
      setSelfiePreview(url);
    }
  };

  // 3. OCR con Tesseract
  const triggerOcr = async (blob: Blob) => {
    setScanning(true);
    setOcrError(null);
    try {
      const formData = new FormData();
      formData.set("file", blob, "cedula_anverso.jpg");
      const res = await request<any>("/api/v1/support/kyc-ocr-scan", "POST", formData);
      const data = (res as any)?.data ?? res;
      setOcrData(data);

      if (data.is_expired) {
        setOcrError(
          data.expiration_message ||
            `Tu cédula está vencida (${data.expiration_date}). Debes usar un documento vigente.`
        );
      } else {
        if (data.doc_type) setDocType(data.doc_type);
        if (data.doc_number) setDocNumber(data.doc_number);
        if (data.full_name) setFullName(data.full_name);
        if (data.birthdate) setBirthdate(data.birthdate);
      }
    } catch (err: any) {
      console.warn("OCR non-fatal error:", err);
    } finally {
      setScanning(false);
    }
  };

  const retakeFront = () => {
    if (frontPreview) URL.revokeObjectURL(frontPreview);
    setFrontBlob(null);
    setFrontPreview(null);
    setOcrData(null);
    setOcrError(null);
    startCamera("environment");
  };

  const retakeSelfie = () => {
    if (selfiePreview) URL.revokeObjectURL(selfiePreview);
    setSelfieBlob(null);
    setSelfiePreview(null);
    startCamera("user");
  };

  // 4. Envío Final a la API
  const handleFinalSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setSubmitError(null);

    if (!frontBlob || !selfieBlob) {
      setSubmitError("Faltan las fotos de tu cédula o tu selfie.");
      return;
    }
    if (!docNumber || docNumber.length < 5) {
      setSubmitError("Ingresa un número de cédula válido.");
      return;
    }
    if (!fullName || fullName.trim().length < 3) {
      setSubmitError("Ingresa tu nombre y apellido completos.");
      return;
    }
    if (!birthdate) {
      setSubmitError("Ingresa tu fecha de nacimiento.");
      return;
    }
    if (!phoneRest || phoneRest.length < 7) {
      setSubmitError("Ingresa tu número de teléfono celular.");
      return;
    }

    setSubmitting(true);
    try {
      const formData = new FormData();
      formData.set("identity_front", frontBlob, "cedula_anverso.jpg");
      formData.set("selfie", selfieBlob, "selfie_con_cedula.jpg");
      formData.set("document_type", docType);
      formData.set("document_number", docNumber);
      formData.set("full_name", fullName.trim());
      formData.set("birthdate", birthdate);
      formData.set("phone", `${phonePrefix}${phoneRest}`);
      if (address.trim()) {
        formData.set("address", address.trim());
      }

      await request("/api/v1/support/kyc-personal", "POST", formData);
      setStep(4); // Éxito
    } catch (err: any) {
      setSubmitError(
        err?.message ||
          "Error al enviar la verificación. Asegúrate de tener buena conexión e inténtalo nuevamente."
      );
    } finally {
      setSubmitting(false);
    }
  };

  // Pantalla de Carga de Autenticación
  if (loadingAuth) {
    return (
      <div className="mobile-kyc-container center-content">
        <Loader2 size={44} className="kyc-spin primary-color" />
        <p className="muted" style={{ marginTop: 16 }}>
          Iniciando sesión segura para verificación móvil...
        </p>
      </div>
    );
  }

  // Pantalla de Error de Token
  if (authError) {
    return (
      <div className="mobile-kyc-container center-content" style={{ padding: "32px 20px" }}>
        <div className="mobile-kyc-error-icon">
          <AlertTriangle size={48} className="coral" />
        </div>
        <h2 style={{ marginTop: 16, marginBottom: 8, fontSize: 22 }}>Enlace no disponible</h2>
        <p className="muted small" style={{ textAlign: "center", lineHeight: 1.5, marginBottom: 24 }}>
          {authError}
        </p>
        <Link to="/login" className="button primary full-width">
          Iniciar sesión en SoyVinko
        </Link>
      </div>
    );
  }

  return (
    <div className="mobile-kyc-page">
      {/* Encabezado Superior Móvil */}
      <header className="mobile-kyc-header">
        <div className="mobile-kyc-brand">
          <img src="/logo.svg" alt="Vinko" className="brand-logo" />
        </div>
        <div className="mobile-kyc-badge">
          <ShieldCheck size={14} />
          <span>Verificación en Vivo</span>
        </div>
      </header>

      {/* Barra de Progreso */}
      <div className="mobile-kyc-progress-bar">
        <div className={`step-item ${step >= 1 ? "active" : ""} ${step > 1 ? "completed" : ""}`}>
          <div className="step-circle">{step > 1 ? <Check size={12} /> : "1"}</div>
          <span>Cédula</span>
        </div>
        <div className="step-connector" />
        <div className={`step-item ${step >= 2 ? "active" : ""} ${step > 2 ? "completed" : ""}`}>
          <div className="step-circle">{step > 2 ? <Check size={12} /> : "2"}</div>
          <span>Selfie</span>
        </div>
        <div className="step-connector" />
        <div className={`step-item ${step >= 3 ? "active" : ""} ${step === 4 ? "completed" : ""}`}>
          <div className="step-circle">{step === 4 ? <Check size={12} /> : "3"}</div>
          <span>Confirmar</span>
        </div>
      </div>

      {/* Contenido Principal según el paso */}
      <main className="mobile-kyc-content">
        {/* ===================================================================
            PASO 1: FOTO DE LA CÉDULA DE IDENTIDAD (ANVERSO)
        =================================================================== */}
        {step === 1 && (
          <div className="mobile-kyc-step-box">
            <div className="mobile-kyc-title-area">
              <span className="mobile-step-tag">Paso 1 de 2</span>
              <h2>Foto de tu Cédula de Identidad</h2>
              <p className="muted small">
                Enfoca el <strong>frente de tu documento</strong> dentro del recuadro con buena iluminación.
              </p>
            </div>

            {/* Visor de Cámara o Previsualización */}
            <div className="mobile-camera-card">
              {frontPreview ? (
                <div className="mobile-preview-box">
                  <img src={frontPreview} alt="Cédula capturada" className="mobile-preview-img" />
                  {scanning && (
                    <div className="mobile-scanning-overlay">
                      <Loader2 size={36} className="kyc-spin" />
                      <span>Analizando documento con OCR...</span>
                    </div>
                  )}
                </div>
              ) : (
                <div className="mobile-live-camera-wrap">
                  {cameraLoading && (
                    <div className="mobile-camera-loading">
                      <Loader2 size={32} className="kyc-spin" />
                      <span>Iniciando cámara trasera...</span>
                    </div>
                  )}
                  <video ref={videoRef} playsInline muted autoPlay className="mobile-video-feed" />

                  {/* Marco guía de la Cédula */}
                  <div className="mobile-id-guide">
                    <div className="corner-tl" />
                    <div className="corner-tr" />
                    <div className="corner-bl" />
                    <div className="corner-br" />
                    <span className="guide-text">Encuadra el frente de la cédula</span>
                  </div>

                  <div className="live-pill">
                    <span className="pulse-dot" /> EN VIVO
                  </div>
                </div>
              )}
            </div>

            {/* Input nativo oculto para fallback */}
            <input
              ref={frontInputRef}
              type="file"
              accept="image/*"
              capture="environment"
              hidden
              onChange={(e) => handleNativeFallback(e, "front")}
            />

            {/* Resultados de OCR o Errores */}
            {ocrError && (
              <div className="mobile-alert error">
                <AlertTriangle size={18} />
                <span>{ocrError}</span>
              </div>
            )}

            {ocrData && !ocrError && (
              <div className="mobile-ocr-success-card">
                <div className="ocr-row-header">
                  <CheckCircle2 size={18} className="green-accent" />
                  <strong>Documento detectado correctamente</strong>
                </div>
                <div className="ocr-tags">
                  <span className="ocr-badge">
                    {ocrData.doc_type}-{ocrData.doc_number}
                  </span>
                  {ocrData.full_name && <span className="ocr-badge name">{ocrData.full_name}</span>}
                  <span className="ocr-badge green">Vigente</span>
                </div>
              </div>
            )}

            {/* Botonera de Acción */}
            <div className="mobile-action-bar">
              {!frontPreview ? (
                <>
                  <button
                    type="button"
                    className="mobile-shutter-btn"
                    onClick={() => {
                      if (cameraActive) {
                        captureFrame("front");
                      } else {
                        frontInputRef.current?.click();
                      }
                    }}
                  >
                    <div className="shutter-inner">
                      <Camera size={26} />
                    </div>
                    <span>Tomar foto a la cédula</span>
                  </button>

                  <button
                    type="button"
                    className="mobile-secondary-action"
                    onClick={() => frontInputRef.current?.click()}
                  >
                    ¿Problemas con el visor? Usar cámara nativa
                  </button>
                </>
              ) : (
                <div className="mobile-btn-group">
                  <button
                    type="button"
                    className="button secondary mobile-btn"
                    onClick={retakeFront}
                    disabled={scanning}
                  >
                    <RefreshCw size={16} /> Repetir foto
                  </button>
                  <button
                    type="button"
                    className="button primary mobile-btn"
                    onClick={() => setStep(2)}
                    disabled={scanning}
                  >
                    Continuar a la Selfie <ArrowRight size={16} />
                  </button>
                </div>
              )}
            </div>
          </div>
        )}

        {/* ===================================================================
            PASO 2: SELFIE EN VIVO (CÁMARA FRONTAL)
        =================================================================== */}
        {step === 2 && (
          <div className="mobile-kyc-step-box">
            <div className="mobile-kyc-title-area">
              <span className="mobile-step-tag">Paso 2 de 2</span>
              <h2>Selfie de Identidad en Vivo</h2>
              <p className="muted small">
                Sostén tu teléfono al frente y mira a la cámara sosteniendo tu cédula a la altura de tu pecho.
              </p>
            </div>

            {/* Visor de Cámara Frontal */}
            <div className="mobile-camera-card">
              {selfiePreview ? (
                <div className="mobile-preview-box">
                  <img src={selfiePreview} alt="Selfie capturada" className="mobile-preview-img" />
                  <div className="selfie-check-badge">
                    <CheckCircle2 size={20} />
                  </div>
                </div>
              ) : (
                <div className="mobile-live-camera-wrap">
                  {cameraLoading && (
                    <div className="mobile-camera-loading">
                      <Loader2 size={32} className="kyc-spin" />
                      <span>Iniciando cámara frontal...</span>
                    </div>
                  )}
                  <video
                    ref={videoRef}
                    playsInline
                    muted
                    autoPlay
                    className="mobile-video-feed mirror"
                  />

                  {/* Silueta Ovalada para el Rostro */}
                  <div className="mobile-oval-guide">
                    <div className="oval-border" />
                    <span className="guide-text">Centra tu rostro aquí</span>
                  </div>

                  <div className="live-pill">
                    <span className="pulse-dot" /> CÁMARA FRONTAL
                  </div>
                </div>
              )}
            </div>

            {/* Input nativo oculto para selfie */}
            <input
              ref={selfieInputRef}
              type="file"
              accept="image/*"
              capture="user"
              hidden
              onChange={(e) => handleNativeFallback(e, "selfie")}
            />

            {/* Botones de Acción Paso 2 */}
            <div className="mobile-action-bar">
              {!selfiePreview ? (
                <>
                  <button
                    type="button"
                    className="mobile-shutter-btn selfie"
                    onClick={() => {
                      if (cameraActive) {
                        captureFrame("selfie");
                      } else {
                        selfieInputRef.current?.click();
                      }
                    }}
                  >
                    <div className="shutter-inner selfie">
                      <Camera size={26} />
                    </div>
                    <span>Tomar Selfie en Vivo</span>
                  </button>

                  <button
                    type="button"
                    className="mobile-secondary-action"
                    onClick={() => selfieInputRef.current?.click()}
                  >
                    ¿Problemas con el visor? Usar cámara nativa
                  </button>
                </>
              ) : (
                <div className="mobile-btn-group">
                  <button type="button" className="button secondary mobile-btn" onClick={retakeSelfie}>
                    <RefreshCw size={16} /> Repetir selfie
                  </button>
                  <button
                    type="button"
                    className="button primary mobile-btn"
                    onClick={() => setStep(3)}
                  >
                    Revisar y Enviar <ArrowRight size={16} />
                  </button>
                </div>
              )}
            </div>
          </div>
        )}

        {/* ===================================================================
            PASO 3: CONFIRMACIÓN Y ENVÍO FINAL
        =================================================================== */}
        {step === 3 && (
          <form onSubmit={handleFinalSubmit} className="mobile-kyc-step-box">
            <div className="mobile-kyc-title-area">
              <span className="mobile-step-tag">Paso Final</span>
              <h2>Confirma tus Datos</h2>
              <p className="muted small">
                Verifica la información extraída de tu documento antes de enviar el expediente oficial.
              </p>
            </div>

            {/* Miniaturas de Fotos Capturadas */}
            <div className="mobile-photos-summary">
              <div className="summary-thumb-item">
                <img src={frontPreview || ""} alt="Cédula" />
                <div className="thumb-label">
                  <Check size={12} /> Cédula
                </div>
                <button type="button" className="thumb-edit-btn" onClick={() => setStep(1)}>
                  Cambiar
                </button>
              </div>

              <div className="summary-thumb-item">
                <img src={selfiePreview || ""} alt="Selfie" />
                <div className="thumb-label">
                  <Check size={12} /> Selfie
                </div>
                <button type="button" className="thumb-edit-btn" onClick={() => setStep(2)}>
                  Cambiar
                </button>
              </div>
            </div>

            {submitError && (
              <div className="mobile-alert error">
                <AlertTriangle size={18} />
                <span>{submitError}</span>
              </div>
            )}

            {/* Formulario de Datos Oficiales */}
            <div className="mobile-form-fields">
              <div className="field-group">
                <label>Número de Documento</label>
                <div className="document-input-row">
                  <select
                    value={docType}
                    onChange={(e) => setDocType(e.target.value)}
                    className="doc-type-select"
                  >
                    <option value="V">V - Venezolano</option>
                    <option value="E">E - Extranjero</option>
                    <option value="P">P - Pasaporte</option>
                  </select>
                  <input
                    type="text"
                    required
                    placeholder="Ej. 17265900"
                    value={docNumber}
                    onChange={(e) => setDocNumber(e.target.value.replace(/\D/g, ""))}
                    className="doc-number-input"
                  />
                </div>
              </div>

              <div className="field-group">
                <label>Nombres y Apellidos Completos</label>
                <input
                  type="text"
                  required
                  placeholder="Tal como figura en tu cédula"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                />
              </div>

              <div className="field-group">
                <label>Fecha de Nacimiento</label>
                <input
                  type="date"
                  required
                  value={birthdate}
                  onChange={(e) => setBirthdate(e.target.value)}
                />
              </div>

              <div className="field-group">
                <label>Teléfono Celular de Contacto</label>
                <div className="phone-input-row">
                  <select
                    value={phonePrefix}
                    onChange={(e) => setPhonePrefix(e.target.value)}
                    className="phone-prefix-select"
                  >
                    <option value="0414">0414</option>
                    <option value="0424">0424</option>
                    <option value="0412">0412</option>
                    <option value="0416">0416</option>
                    <option value="0426">0426</option>
                  </select>
                  <input
                    type="tel"
                    required
                    placeholder="1234567"
                    maxLength={7}
                    value={phoneRest}
                    onChange={(e) => setPhoneRest(e.target.value.replace(/\D/g, ""))}
                    className="phone-rest-input"
                  />
                </div>
              </div>

              <div className="field-group">
                <label>Dirección o Estado (Opcional)</label>
                <input
                  type="text"
                  placeholder="Ej. Caracas, Miranda"
                  value={address}
                  onChange={(e) => setAddress(e.target.value)}
                />
              </div>
            </div>

            <div className="mobile-security-notice">
              <Lock size={15} />
              <span>
                Tus datos y fotos están protegidos y solo se usan para validar tu cuenta personal en SoyVinko.
              </span>
            </div>

            <div className="mobile-action-bar">
              <button
                type="submit"
                className="button primary full-width mobile-submit-btn"
                disabled={submitting}
              >
                {submitting ? (
                  <>
                    <Loader2 size={18} className="kyc-spin" /> Enviando verificación...
                  </>
                ) : (
                  <>
                    <UserCheck size={18} /> Confirmar y Enviar Verificación
                  </>
                )}
              </button>
            </div>
          </form>
        )}

        {/* ===================================================================
            PASO 4: PANTALLA DE ÉXITO FINAL
        =================================================================== */}
        {step === 4 && (
          <div className="mobile-kyc-success-screen">
            <div className="success-icon-container">
              <CheckCircle2 size={64} className="green-accent" />
            </div>
            <h2>¡Verificación Enviada con Éxito!</h2>
            <p className="muted">
              Hemos recibido tus fotografías y datos oficiales. Tu solicitud se encuentra en revisión.
            </p>

            <div className="mobile-success-card">
              <div className="success-card-header">
                <Sparkles size={18} className="purple-star" />
                <strong>Expediente Sincronizado</strong>
              </div>
              <p className="small muted">
                Tu sesión en la computadora detectará esta confirmación automáticamente en unos momentos.
              </p>
            </div>

            <div className="mobile-close-prompt">
              <span>✅ Ya puedes cerrar esta pestaña en tu teléfono móvil.</span>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}
export default MobileKycPage;
