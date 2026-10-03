import { useState, useRef, useEffect, type FormEvent } from "react";
import {
  ShieldCheck,
  CheckCircle2,
  Camera,
  X,
  Loader2,
  ArrowRight,
  ArrowLeft,
  Info,
  Lock,
  User,
  AlertTriangle,
  RotateCcw,
  Sparkles,
  Smartphone,
  Calendar,
  FileCheck,
  Copy,
  Check,
  QrCode,
  Monitor,
  RefreshCw,
} from "lucide-react";
import { request, api } from "./api";
import { ErrorBox } from "./components";

interface PersonalKycWizardProps {
  onSuccess: () => void;
  onCancel?: () => void;
  initialName?: string;
  initialPhone?: string;
}

interface OcrData {
  success: boolean;
  doc_type: string | null;
  doc_number: string | null;
  first_name: string | null;
  last_name: string | null;
  full_name: string | null;
  birthdate: string | null;
  expiration_date: string | null;
  is_expired: boolean;
  expiration_message: string | null;
  raw_text?: string;
}

interface MobileSessionData {
  token: string;
  url: string;
  qr_data_url: string;
  expires_at: string;
}

export function PersonalKycWizard({
  onSuccess,
  onCancel,
  initialName = "",
  initialPhone = "",
}: PersonalKycWizardProps) {
  // Detección de dispositivo móvil nativo o pantalla pequeña
  const isMobileInitial =
    typeof navigator !== "undefined" &&
    (/Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent) ||
      window.innerWidth <= 768);

  // Si está en desktop, por defecto ofrece "mobile_handoff" para escanear con el teléfono.
  // Si está en móvil (o el usuario elige cámara web en PC), entra a "camera".
  const [activeView, setActiveView] = useState<"mobile_handoff" | "camera">(
    isMobileInitial ? "camera" : "mobile_handoff"
  );

  // Sesión móvil y código QR para Desktop
  const [mobileSession, setMobileSession] = useState<MobileSessionData | null>(null);
  const [mobileLoading, setMobileLoading] = useState(false);
  const [copiedLink, setCopiedLink] = useState(false);
  const [waitingMobile, setWaitingMobile] = useState(false);

  // Stepper de 3 pasos (Cédula -> Selfie -> Confirmar)
  const [step, setStep] = useState<1 | 2 | 3>(1);

  // Cámara en vivo (WebRTC)
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const [cameraActive, setCameraActive] = useState(false);
  const [cameraLoading, setCameraLoading] = useState(false);
  const [cameraError, setCameraError] = useState<string | null>(null);
  const fallbackCamRef = useRef<HTMLInputElement | null>(null);

  // Paso 1: Foto del anverso de la Cédula (en memoria) & OCR
  const [frontBlob, setFrontBlob] = useState<Blob | null>(null);
  const [frontPreview, setFrontPreview] = useState<string | null>(null);
  const [scanning, setScanning] = useState(false);
  const [ocrData, setOcrData] = useState<OcrData | null>(null);

  // Datos extraídos / confirmables
  const [docType, setDocType] = useState<"V" | "E" | "P">("V");
  const [docNumber, setDocNumber] = useState("");
  const [fullName, setFullName] = useState(initialName);
  const [birthdate, setBirthdate] = useState("");
  const [phonePrefix, setPhonePrefix] = useState("0414");
  const [phoneRest, setPhoneRest] = useState(initialPhone.replace(/\D/g, "").slice(-7));
  const [address, setAddress] = useState("");

  // Paso 2: Selfie en vivo con documento en mano
  const [selfieBlob, setSelfieBlob] = useState<Blob | null>(null);
  const [selfiePreview, setSelfiePreview] = useState<string | null>(null);

  // Estado general
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submissionSuccess, setSubmissionSuccess] = useState(false);

  // =========================================================================
  // 1. GESTIÓN DEL CÓDIGO QR Y SESIÓN MÓVIL (DESKTOP)
  // =========================================================================
  const fetchMobileSession = async () => {
    setMobileLoading(true);
    setError(null);
    try {
      const res = await request<any>(
        "/api/v1/support/kyc-mobile-session",
        "GET"
      );
      const session = (res as any)?.data ?? res;
      if (session && session.qr_data_url) {
        setMobileSession(session);
        setWaitingMobile(true);
      } else {
        throw new Error("No se pudo obtener la imagen del código QR.");
      }
    } catch (err: any) {
      console.error("fetchMobileSession error:", err);
      setError(err?.message || "No se pudo generar la sesión para dispositivo móvil.");
    } finally {
      setMobileLoading(false);
    }
  };

  useEffect(() => {
    if (activeView === "mobile_handoff" && !mobileSession) {
      fetchMobileSession();
    }
  }, [activeView]);

  // Sondeo en tiempo real en desktop esperando la subida desde el teléfono
  useEffect(() => {
    if (activeView !== "mobile_handoff" || !waitingMobile) return;

    const interval = setInterval(async () => {
      try {
        const myKyc = await api<any[]>("my_kyc");
        if (myKyc && myKyc.length > 0) {
          const latest = myKyc[0];
          if (latest.status === "pending" || latest.status === "in_review") {
            setWaitingMobile(false);
            setSubmissionSuccess(true);
            setTimeout(() => {
              onSuccess();
            }, 2500);
          }
        }
      } catch {}
    }, 3500);

    return () => clearInterval(interval);
  }, [activeView, waitingMobile, onSuccess]);

  const copyMobileLink = () => {
    if (!mobileSession?.url) return;
    navigator.clipboard.writeText(mobileSession.url);
    setCopiedLink(true);
    setTimeout(() => setCopiedLink(false), 3000);
  };

  // =========================================================================
  // 2. CÁMARA EN VIVO WEBRTC (CAPTURA ESTRICTA - SIN ARCHIVOS LOCALES)
  // =========================================================================
  const stopLiveCamera = () => {
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((track) => track.stop());
      streamRef.current = null;
    }
    setCameraActive(false);
  };

  const startLiveCamera = async (facing: "environment" | "user") => {
    stopLiveCamera();
    setCameraLoading(true);
    setCameraError(null);

    // Intentar WebRTC getUserMedia
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
        const s = await navigator.mediaDevices.getUserMedia(constraints);
        streamRef.current = s;
        setCameraActive(true);
        setCameraLoading(false);

        // Conectar al video ref cuando esté disponible
        setTimeout(() => {
          if (videoRef.current) {
            videoRef.current.srcObject = s;
            videoRef.current.play().catch(() => {});
          }
        }, 100);
        return;
      } catch (err: any) {
        console.warn("getUserMedia failed or denied:", err);
        setCameraLoading(false);
        setCameraError(
          "No se pudo iniciar el visor directo. Toca el botón para usar el disparador de cámara del teléfono."
        );
      }
    } else {
      setCameraLoading(false);
      setCameraError(
        "Tu navegador no soporta visor directo. Toca el botón para usar la cámara nativa."
      );
    }
  };

  // Iniciar la cámara adecuada al cambiar de paso
  useEffect(() => {
    if (activeView === "camera") {
      if (step === 1 && !frontPreview) {
        startLiveCamera("environment");
      } else if (step === 2 && !selfiePreview) {
        startLiveCamera("user");
      } else {
        stopLiveCamera();
      }
    }
    return () => stopLiveCamera();
  }, [step, activeView, frontPreview, selfiePreview]);

  // Capturar fotograma en vivo desde el stream <video> a un Blob en memoria
  const captureFrameFromVideo = async (target: "front" | "selfie") => {
    if (!videoRef.current) return;
    const video = videoRef.current;
    const canvas = document.createElement("canvas");
    canvas.width = video.videoWidth || 1280;
    canvas.height = video.videoHeight || 720;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    // Si es cámara frontal (selfie), voltear horizontalmente para efecto espejo natural
    if (target === "selfie") {
      ctx.translate(canvas.width, 0);
      ctx.scale(-1, 1);
    }
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);

    canvas.toBlob(
      async (blob) => {
        if (!blob) return;
        stopLiveCamera();
        const previewUrl = URL.createObjectURL(blob);

        if (target === "front") {
          setFrontBlob(blob);
          setFrontPreview(previewUrl);
          await runOcrScan(blob);
        } else {
          setSelfieBlob(blob);
          setSelfiePreview(previewUrl);
        }
      },
      "image/jpeg",
      0.92
    );
  };

  // Disparador de fallback estricto de cámara nativa (con capture="environment" o "user")
  const handleNativeCameraFallback = async (e: React.ChangeEvent<HTMLInputElement>, target: "front" | "selfie") => {
    const file = e.target.files?.[0];
    if (!file) return;
    const previewUrl = URL.createObjectURL(file);
    if (target === "front") {
      setFrontBlob(file);
      setFrontPreview(previewUrl);
      await runOcrScan(file);
    } else {
      setSelfieBlob(file);
      setSelfiePreview(previewUrl);
    }
  };

  // =========================================================================
  // 3. ANÁLISIS OCR CON TESSERACT
  // =========================================================================
  const runOcrScan = async (blob: Blob) => {
    setScanning(true);
    setError(null);
    try {
      const formData = new FormData();
      formData.set("file", blob, "cedula_anverso.jpg");
      const res = await request<any>("/api/v1/support/kyc-ocr-scan", "POST", formData);
      const ocr = (res as any)?.data ?? res;
      setOcrData(ocr);

      if (ocr.is_expired) {
        setError(
          ocr.expiration_message ||
            `Tu cédula venció en ${ocr.expiration_date}. No es posible verificar con un documento vencido.`
        );
        return;
      }

      if (ocr.doc_type === "V" || ocr.doc_type === "E" || ocr.doc_type === "P") {
        setDocType(ocr.doc_type);
      }
      if (ocr.doc_number) {
        setDocNumber(ocr.doc_number);
      }
      if (ocr.full_name) {
        setFullName(ocr.full_name);
      }
      if (ocr.birthdate) {
        setBirthdate(ocr.birthdate);
      }
    } catch (err: any) {
      console.warn("OCR non-critical issue:", err);
    } finally {
      setScanning(false);
    }
  };

  const retakeFront = () => {
    if (frontPreview) URL.revokeObjectURL(frontPreview);
    setFrontBlob(null);
    setFrontPreview(null);
    setOcrData(null);
    setError(null);
    startLiveCamera("environment");
  };

  const retakeSelfie = () => {
    if (selfiePreview) URL.revokeObjectURL(selfiePreview);
    setSelfieBlob(null);
    setSelfiePreview(null);
    setError(null);
    startLiveCamera("user");
  };

  // =========================================================================
  // 4. TRANSICIONES Y ENVÍO FINAL
  // =========================================================================
  const handleNextFromStep1 = (e: FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!frontBlob) {
      setError("Debes tomar la foto del anverso de tu cédula para continuar.");
      return;
    }
    if (ocrData?.is_expired) {
      setError("Documento vencido detectado. Debes presentar una cédula vigente.");
      return;
    }
    if (!docNumber || docNumber.length < 5) {
      setError("Ingresa un número de cédula válido.");
      return;
    }
    if (!fullName || fullName.trim().length < 3) {
      setError("Ingresa tus nombres y apellidos completos.");
      return;
    }
    if (!birthdate) {
      setError("Ingresa tu fecha de nacimiento.");
      return;
    }
    if (!phoneRest || phoneRest.length < 7) {
      setError("Ingresa tu número de teléfono celular (7 dígitos después del código).");
      return;
    }

    setStep(2);
  };

  const handleNextFromStep2 = (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    if (!selfieBlob) {
      setError("Debes capturar tu selfie sosteniendo la cédula para continuar.");
      return;
    }
    setStep(3);
  };

  const handleSubmitFinal = async () => {
    if (!frontBlob || !selfieBlob) {
      setError("Faltan fotografías obligatorias. Completa los pasos anteriores.");
      return;
    }

    setLoading(true);
    setError(null);

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
      setSubmissionSuccess(true);
      setTimeout(() => {
        onSuccess();
      }, 2000);
    } catch (err: any) {
      setError(
        err?.message ||
          "Ocurrió un error al procesar tu verificación. Revisa la calidad de las fotos e intenta nuevamente."
      );
      setLoading(false);
    }
  };

  // =========================================================================
  // RENDER: PANTALLA DE ÉXITO FINAL
  // =========================================================================
  if (submissionSuccess) {
    return (
      <div className="kyc-wizard-box kyc-success-screen">
        <div className="kyc-success-icon-wrap">
          <CheckCircle2 size={56} className="green-accent" />
        </div>
        <h3>¡Verificación Enviada con Éxito!</h3>
        <p className="muted">
          Tu expediente oficial ha sido creado y sincronizado con tu perfil. El equipo de cumplimiento
          revisará tus datos en las próximas horas.
        </p>
        <div className="kyc-success-details">
          <div>
            <span className="muted small">Titular Oficial:</span>
            <strong>{fullName}</strong>
          </div>
          <div>
            <span className="muted small">Documento Registrado:</span>
            <strong>{docType}-{docNumber}</strong>
          </div>
        </div>
        <div className="row center" style={{ marginTop: 20 }}>
          <button type="button" className="button primary" onClick={onSuccess}>
            Entendido, ver estado
          </button>
        </div>
      </div>
    );
  }

  // =========================================================================
  // RENDER: PANTALLA 1 - CONTINUAR EN TELÉFONO MÓVIL (DESKTOP QR HANDOFF)
  // =========================================================================
  if (activeView === "mobile_handoff") {
    return (
      <div className="kyc-wizard-box kyc-handoff-box">
        <div className="kyc-wizard-header">
          <div className="kyc-header-title">
            <div className="kyc-header-icon-wrap">
              <Smartphone size={30} />
            </div>
            <div>
              <div className="kyc-header-badge">VERIFICACIÓN OFICIAL EN VIVO</div>
              <h3>Continúa la verificación en tu teléfono móvil</h3>
              <p className="muted small">
                Para garantizar la autenticidad y prevenir fraudes, las fotos de tu cédula y selfie
                deben tomarse <strong>en vivo</strong> con la cámara de tu smartphone.
              </p>
            </div>
          </div>
        </div>

        {error && <ErrorBox message={error} />}

        <div className="kyc-handoff-grid">
          {/* Columna Izquierda: QR Code */}
          <div className="kyc-qr-card">
            <div className="kyc-qr-badge">
              <Lock size={13} />
              <span>Sesión cifrada de 30 minutos</span>
            </div>

            {mobileLoading ? (
              <div className="kyc-qr-placeholder">
                <Loader2 size={36} className="kyc-spin" />
                <span className="small muted">Generando acceso seguro...</span>
              </div>
            ) : mobileSession?.qr_data_url ? (
              <div className="kyc-qr-frame">
                <img
                  src={mobileSession.qr_data_url}
                  alt="Código QR para verificación móvil"
                  className="kyc-qr-image"
                />
              </div>
            ) : (
              <div className="kyc-qr-placeholder">
                <AlertTriangle size={30} className="coral" />
                <span className="small muted">Error al generar código QR</span>
                <button
                  type="button"
                  className="button secondary small kyc-reintentar-btn"
                  onClick={fetchMobileSession}
                >
                  <RefreshCw size={13} /> Reintentar
                </button>
              </div>
            )}

            <div className="kyc-qr-caption">
              <strong>Escanea este código con tu cámara</strong>
              <p className="muted small">
                Apunta con la cámara de tu teléfono para ingresar directamente sin contraseñas.
              </p>
            </div>

            {/* Estado de espera en tiempo real */}
            <div className="kyc-waiting-pill">
              <span className="kyc-pulse-dot" />
              <span>Esperando captura desde tu teléfono...</span>
            </div>
          </div>

          {/* Columna Derecha: Instrucciones y opciones */}
          <div className="kyc-handoff-instructions">
            <h4>¿Cómo continuar en tu teléfono?</h4>
            <ol className="kyc-handoff-steps">
              <li>
                <div className="kyc-step-badge">1</div>
                <div>
                  <strong>Abre la app de cámara</strong>
                  <p className="muted small">
                    Usa la cámara estándar de tu iPhone o teléfono Android.
                  </p>
                </div>
              </li>
              <li>
                <div className="kyc-step-badge">2</div>
                <div>
                  <strong>Apunta hacia el código QR</strong>
                  <p className="muted small">
                    Aparecerá un enlace seguro de SoyVinko. Tócalo para abrir la verificación.
                  </p>
                </div>
              </li>
              <li>
                <div className="kyc-step-badge">3</div>
                <div>
                  <strong>Captura tu Cédula y Selfie en vivo</strong>
                  <p className="muted small">
                    El sistema OCR leerá tus datos automáticamente y esta pantalla se actualizará sola.
                  </p>
                </div>
              </li>
            </ol>

            {/* Alternativa: Copiar enlace */}
            <div className="kyc-link-share-box">
              <span className="muted small">¿No puedes escanear el QR?</span>
              <div className="kyc-copy-link-row">
                <input
                  type="text"
                  readOnly
                  value={mobileSession?.url || "Cargando enlace..."}
                  className="kyc-link-input"
                />
                <button
                  type="button"
                  className={`button ${copiedLink ? "success" : "primary"} kyc-copy-btn`}
                  onClick={copyMobileLink}
                  disabled={!mobileSession?.url}
                >
                  {copiedLink ? (
                    <>
                      <Check size={16} /> Copiado
                    </>
                  ) : (
                    <>
                      <Copy size={16} /> Copiar
                    </>
                  )}
                </button>
              </div>
            </div>

            {/* Opción para usar webcam de laptop/desktop */}
            <div className="kyc-desktop-cam-fallback">
              <div className="row between align-center">
                <div>
                  <strong>¿Tienes cámara web en este equipo?</strong>
                  <p className="muted small">
                    Puedes tomar las fotos en vivo usando la webcam de esta computadora.
                  </p>
                </div>
                <button
                  type="button"
                  className="button secondary kyc-switch-btn"
                  onClick={() => setActiveView("camera")}
                >
                  <Monitor size={16} /> Usar cámara web
                </button>
              </div>
            </div>
          </div>
        </div>

        <div className="kyc-handoff-footer">
          <div className="kyc-security-note">
            <ShieldCheck size={18} className="green-accent" />
            <span>
              <strong>Autenticidad Garantizada:</strong> Para proteger tu identidad y prevenir suplantaciones,
              las imágenes no pueden cargarse desde el disco local.
            </span>
          </div>
          {onCancel && (
            <button type="button" className="button outline-light" onClick={onCancel}>
              Volver
            </button>
          )}
        </div>
      </div>
    );
  }

  // =========================================================================
  // RENDER: PANTALLA 2 - FLUJO DE CÁMARA EN VIVO (MÓVIL O WEBCAM)
  // =========================================================================
  return (
    <div className="kyc-wizard-box">
      {/* Encabezado Principal */}
      <div className="kyc-wizard-header">
        <div className="row between align-center">
          <div className="kyc-header-title" style={{ margin: 0 }}>
            <div className="kyc-header-icon-wrap">
              <ShieldCheck size={28} />
            </div>
            <div>
              <div className="kyc-header-badge">CAPTURA EN VIVO CON CÁMARA</div>
              <h3>Verificación de Identidad Oficial</h3>
              <p className="muted small">
                Captura la cédula y tu selfie en tiempo real. No se admiten archivos guardados previamente.
              </p>
            </div>
          </div>

          {!isMobileInitial && (
            <button
              type="button"
              className="button secondary small kyc-switch-btn"
              onClick={() => {
                stopLiveCamera();
                setActiveView("mobile_handoff");
              }}
            >
              <QrCode size={15} /> Abrir en teléfono móvil
            </button>
          )}
        </div>

        {/* Stepper Móvil / Desktop de 3 pasos */}
        <div className="kyc-steps-indicator" role="tablist" style={{ marginTop: 18 }}>
          <div className={`kyc-step-dot ${step === 1 ? "active" : step > 1 ? "completed" : ""}`}>
            <span className="kyc-step-num">{step > 1 ? <CheckCircle2 size={13} /> : "1"}</span>
            <span className="kyc-step-label">1. Cédula (Anverso)</span>
          </div>

          <div className={`kyc-step-line ${step >= 2 ? "active" : ""}`} />

          <div className={`kyc-step-dot ${step === 2 ? "active" : step > 2 ? "completed" : ""}`}>
            <span className="kyc-step-num">{step > 2 ? <CheckCircle2 size={13} /> : "2"}</span>
            <span className="kyc-step-label">2. Selfie con Cédula</span>
          </div>

          <div className={`kyc-step-line ${step === 3 ? "active" : ""}`} />

          <div className={`kyc-step-dot ${step === 3 ? "active" : ""}`}>
            <span className="kyc-step-num">3</span>
            <span className="kyc-step-label">3. Confirmar</span>
          </div>
        </div>
      </div>

      {error && <ErrorBox message={error} />}

      {/* ========================================================
          PASO 1: Cámara en vivo de la Cédula (Anverso) y Lectura OCR
          ======================================================== */}
      {step === 1 && (
        <form onSubmit={handleNextFromStep1} className="kyc-form">
          <div className="kyc-step-section">
            <div className="kyc-section-header">
              <Camera size={22} className="green-accent" />
              <div>
                <strong>Cámara en vivo: Anverso de tu Cédula de Identidad</strong>
                <p className="muted small">
                  Apunta hacia el anverso con buena iluminación. Nuestro visor detectará y leerá tus datos automáticamente con OCR.
                </p>
              </div>
            </div>

            {/* ZONA DE CAPTURA EXCLUSIVAMENTE CON CÁMARA */}
            {frontPreview ? (
              <div className="kyc-captured-card">
                <div className="kyc-preview-img-box">
                  <img src={frontPreview} alt="Anverso de Cédula" className="kyc-id-preview-img" />
                  {scanning && (
                    <div className="kyc-ocr-scanning-overlay">
                      <div className="kyc-laser-line" />
                      <div className="kyc-scan-text">
                        <Sparkles size={18} className="kyc-spin" />
                        <span>Analizando cédula con OCR Tesseract...</span>
                      </div>
                    </div>
                  )}
                  <button
                    type="button"
                    className="kyc-retake-btn"
                    onClick={retakeFront}
                    title="Tomar otra foto"
                  >
                    <RotateCcw size={15} />
                    <span>Repetir captura</span>
                  </button>
                </div>

                {/* Resultados del OCR */}
                {ocrData && !scanning && (
                  <div className={`kyc-ocr-result-box ${ocrData.is_expired ? "expired" : "valid"}`}>
                    {ocrData.is_expired ? (
                      <div className="kyc-ocr-alert-expired">
                        <AlertTriangle size={24} />
                        <div>
                          <strong>Documento Vencido detectado</strong>
                          <p>
                            {ocrData.expiration_message ||
                              `Tu cédula venció en ${ocrData.expiration_date}. No se admiten documentos vencidos.`}
                          </p>
                        </div>
                      </div>
                    ) : (
                      <div>
                        <div className="kyc-ocr-success-header">
                          <CheckCircle2 size={18} className="green-accent" />
                          <strong>Datos extraídos exitosamente por OCR en vivo:</strong>
                        </div>
                        <div className="kyc-ocr-chips-grid">
                          <div className="kyc-ocr-chip">
                            <span className="kyc-chip-label">Cédula:</span>
                            <span className="kyc-chip-val">
                              {ocrData.doc_type}-{ocrData.doc_number}
                            </span>
                          </div>
                          <div className="kyc-ocr-chip">
                            <span className="kyc-chip-label">Titular Oficial:</span>
                            <span className="kyc-chip-val">{ocrData.full_name}</span>
                          </div>
                          {ocrData.birthdate && (
                            <div className="kyc-ocr-chip">
                              <span className="kyc-chip-label">Nacimiento:</span>
                              <span className="kyc-chip-val">{ocrData.birthdate}</span>
                            </div>
                          )}
                          {ocrData.expiration_date && (
                            <div className="kyc-ocr-chip highlight">
                              <span className="kyc-chip-label">Vigencia:</span>
                              <span className="kyc-chip-val">
                                {ocrData.expiration_date} (Vigente)
                              </span>
                            </div>
                          )}
                        </div>
                      </div>
                    )}
                  </div>
                )}
              </div>
            ) : (
              <div className="kyc-live-camera-container">
                {cameraActive ? (
                  <div className="kyc-viewfinder-box">
                    <video
                      ref={videoRef}
                      autoPlay
                      playsInline
                      muted
                      className="kyc-live-video"
                    />

                    {/* Guía visual de alineación de Cédula */}
                    <div className="kyc-id-card-frame">
                      <div className="kyc-corner tl" />
                      <div className="kyc-corner tr" />
                      <div className="kyc-corner bl" />
                      <div className="kyc-corner br" />
                      <span className="kyc-frame-label">Alinea el anverso de la cédula aquí</span>
                    </div>

                    {/* Barra de controles de disparo */}
                    <div className="kyc-shutter-bar">
                      <button
                        type="button"
                        className="kyc-shutter-btn"
                        onClick={() => captureFrameFromVideo("front")}
                      >
                        <span className="kyc-shutter-inner" />
                      </button>
                      <span className="kyc-shutter-hint">Toca el botón para capturar</span>
                    </div>
                  </div>
                ) : (
                  <div className="kyc-camera-launch-box">
                    {cameraLoading ? (
                      <div className="row center align-center gap" style={{ padding: 40 }}>
                        <Loader2 size={32} className="kyc-spin" />
                        <span>Iniciando cámara trasera...</span>
                      </div>
                    ) : (
                      <div className="kyc-camera-launch-content">
                        <div className="kyc-camera-bubble">
                          <Camera size={32} />
                        </div>
                        <strong>Acceso a la cámara trasera requerido</strong>
                        <p className="muted small">
                          Por seguridad oficial no se permite seleccionar fotos guardadas. Toma la foto en tiempo real.
                        </p>

                        <div className="kyc-camera-actions-row">
                          <button
                            type="button"
                            className="button primary kyc-cam-btn"
                            onClick={() => startLiveCamera("environment")}
                          >
                            <Camera size={18} /> Iniciar visor de cámara
                          </button>

                          {/* Fallback de disparo nativo directo a la cámara */}
                          <label className="button outline-light kyc-cam-btn">
                            <Smartphone size={18} /> Usar cámara nativa
                            <input
                              ref={fallbackCamRef}
                              type="file"
                              accept="image/*"
                              capture="environment"
                              hidden
                              onChange={(e) => handleNativeCameraFallback(e, "front")}
                            />
                          </label>
                        </div>
                      </div>
                    )}
                  </div>
                )}
              </div>
            )}

            {/* Formulario de confirmación de datos */}
            <div className="kyc-subform">
              <h4 className="kyc-subform-title">Verifica tus datos de contacto:</h4>
              <div className="kyc-form-grid">
                {/* Cédula */}
                <div className="form-group">
                  <label htmlFor="kyc-doc-num" className="kyc-label">
                    <span>Documento de Identidad</span>
                    <span className="kyc-label-req">*</span>
                  </label>
                  <div className="doc-input-combo">
                    <select
                      id="kyc-doc-type"
                      value={docType}
                      onChange={(e) => setDocType(e.target.value as "V" | "E" | "P")}
                      className="kyc-select"
                    >
                      <option value="V">V · Venezolano</option>
                      <option value="E">E · Extranjero</option>
                      <option value="P">P · Pasaporte</option>
                    </select>
                    <input
                      id="kyc-doc-num"
                      type="text"
                      inputMode="numeric"
                      placeholder="Número de cédula"
                      value={docNumber}
                      onChange={(e) => setDocNumber(e.target.value.replace(/\D/g, "").slice(0, 10))}
                      required
                      className="kyc-input"
                    />
                  </div>
                </div>

                {/* Nombre completo */}
                <div className="form-group">
                  <label htmlFor="kyc-fullname" className="kyc-label">
                    <span>Nombres y Apellidos Oficiales</span>
                    <span className="kyc-label-req">*</span>
                  </label>
                  <input
                    id="kyc-fullname"
                    type="text"
                    placeholder="Tal como figura en la cédula"
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                    required
                    className="kyc-input"
                  />
                </div>

                {/* Fecha de nacimiento */}
                <div className="form-group">
                  <label htmlFor="kyc-birthdate" className="kyc-label">
                    <span>Fecha de Nacimiento</span>
                    <span className="kyc-label-req">*</span>
                  </label>
                  <input
                    id="kyc-birthdate"
                    type="date"
                    value={birthdate}
                    onChange={(e) => setBirthdate(e.target.value)}
                    required
                    className="kyc-input"
                  />
                </div>

                {/* Teléfono celular */}
                <div className="form-group">
                  <label htmlFor="kyc-phone" className="kyc-label">
                    <span>Teléfono Móvil de Contacto</span>
                    <span className="kyc-label-req">*</span>
                  </label>
                  <div className="phone-input-combo">
                    <select
                      id="kyc-phone-prefix"
                      value={phonePrefix}
                      onChange={(e) => setPhonePrefix(e.target.value)}
                      className="kyc-select"
                    >
                      <option value="0414">0414</option>
                      <option value="0424">0424</option>
                      <option value="0412">0412</option>
                      <option value="0416">0416</option>
                      <option value="0426">0426</option>
                    </select>
                    <input
                      id="kyc-phone"
                      type="text"
                      inputMode="numeric"
                      placeholder="1234567"
                      value={phoneRest}
                      onChange={(e) => setPhoneRest(e.target.value.replace(/\D/g, "").slice(0, 7))}
                      required
                      className="kyc-input"
                    />
                  </div>
                </div>

                {/* Dirección */}
                <div className="form-group full-width">
                  <label htmlFor="kyc-address" className="kyc-label">
                    <span>Dirección Residencial en Venezuela</span>
                    <span className="kyc-label-opt">(Opcional)</span>
                  </label>
                  <input
                    id="kyc-address"
                    type="text"
                    placeholder="Ciudad, Estado, Municipio o Urbanización"
                    value={address}
                    onChange={(e) => setAddress(e.target.value)}
                    className="kyc-input"
                  />
                </div>
              </div>
            </div>
          </div>

          <div className="kyc-actions-bar">
            {onCancel ? (
              <button type="button" className="button outline-light" onClick={onCancel}>
                Cancelar
              </button>
            ) : (
              <div />
            )}
            <button
              type="submit"
              className="button primary kyc-btn-action"
              disabled={scanning || !frontBlob || (ocrData?.is_expired ?? false)}
            >
              <span>Continuar a Selfie</span>
              <ArrowRight size={17} />
            </button>
          </div>
        </form>
      )}

      {/* ========================================================
          PASO 2: Selfie en vivo sosteniendo la Cédula
          ======================================================== */}
      {step === 2 && (
        <form onSubmit={handleNextFromStep2} className="kyc-form">
          <div className="kyc-step-section">
            <div className="kyc-section-header">
              <User size={22} className="green-accent" />
              <div>
                <strong>Cámara frontal: Selfie en vivo sosteniendo tu Cédula</strong>
                <p className="muted small">
                  Ubica tu rostro en el centro y sostén tu documento al lado de tu pecho con los datos visibles.
                </p>
              </div>
            </div>

            <div className="kyc-selfie-guide-card">
              <div className="kyc-guide-items">
                <div className="kyc-guide-item">
                  <CheckCircle2 size={16} className="green-accent" />
                  <span>Tu rostro debe verse nítido e iluminado (sin lentes oscuros ni gorras)</span>
                </div>
                <div className="kyc-guide-item">
                  <CheckCircle2 size={16} className="green-accent" />
                  <span>Sostén tu cédula firme sin tapar datos con tus dedos</span>
                </div>
              </div>
            </div>

            {selfiePreview ? (
              <div className="kyc-captured-card">
                <div className="kyc-preview-img-box selfie">
                  <img src={selfiePreview} alt="Selfie en vivo con cédula" className="kyc-selfie-preview-img" />
                  <button
                    type="button"
                    className="kyc-retake-btn"
                    onClick={retakeSelfie}
                    title="Tomar otra selfie"
                  >
                    <RotateCcw size={15} />
                    <span>Repetir selfie</span>
                  </button>
                </div>
                <div className="kyc-file-info-badge">
                  <CheckCircle2 size={16} className="green-accent" />
                  <span>Selfie capturada en vivo correctamente</span>
                </div>
              </div>
            ) : (
              <div className="kyc-live-camera-container">
                {cameraActive ? (
                  <div className="kyc-viewfinder-box selfie">
                    <video
                      ref={videoRef}
                      autoPlay
                      playsInline
                      muted
                      className="kyc-live-video selfie-mirror"
                    />

                    {/* Guía ovalada facial */}
                    <div className="kyc-face-oval-frame">
                      <span className="kyc-frame-label">Ubica tu rostro aquí y sostén tu cédula</span>
                    </div>

                    {/* Botón disparador */}
                    <div className="kyc-shutter-bar">
                      <button
                        type="button"
                        className="kyc-shutter-btn"
                        onClick={() => captureFrameFromVideo("selfie")}
                      >
                        <span className="kyc-shutter-inner" />
                      </button>
                      <span className="kyc-shutter-hint">Toca para capturar selfie</span>
                    </div>
                  </div>
                ) : (
                  <div className="kyc-camera-launch-box">
                    {cameraLoading ? (
                      <div className="row center align-center gap" style={{ padding: 40 }}>
                        <Loader2 size={32} className="kyc-spin" />
                        <span>Iniciando cámara frontal...</span>
                      </div>
                    ) : (
                      <div className="kyc-camera-launch-content">
                        <div className="kyc-camera-bubble selfie">
                          <User size={32} />
                        </div>
                        <strong>Cámara frontal requerida</strong>
                        <p className="muted small">
                          Toma tu selfie en vivo sosteniendo tu documento de identidad.
                        </p>

                        <div className="kyc-camera-actions-row">
                          <button
                            type="button"
                            className="button primary kyc-cam-btn"
                            onClick={() => startLiveCamera("user")}
                          >
                            <Camera size={18} /> Iniciar cámara frontal
                          </button>

                          <label className="button outline-light kyc-cam-btn">
                            <Smartphone size={18} /> Usar cámara frontal nativa
                            <input
                              type="file"
                              accept="image/*"
                              capture="user"
                              hidden
                              onChange={(e) => handleNativeCameraFallback(e, "selfie")}
                            />
                          </label>
                        </div>
                      </div>
                    )}
                  </div>
                )}
              </div>
            )}
          </div>

          <div className="kyc-actions-bar">
            <button
              type="button"
              className="button outline-light kyc-btn-back"
              onClick={() => {
                stopLiveCamera();
                setStep(1);
              }}
            >
              <ArrowLeft size={16} />
              <span>Volver a la Cédula</span>
            </button>
            <button
              type="submit"
              className="button primary kyc-btn-action"
              disabled={!selfieBlob}
            >
              <span>Revisar y Confirmar</span>
              <ArrowRight size={17} />
            </button>
          </div>
        </form>
      )}

      {/* ========================================================
          PASO 3: Resumen Final y Envío
          ======================================================== */}
      {step === 3 && (
        <div className="kyc-confirm-step">
          <div className="kyc-confirm-card">
            <div className="kyc-confirm-header">
              <FileCheck size={26} className="green-accent" />
              <div>
                <h4 style={{ margin: 0 }}>Confirmación de Expediente</h4>
                <p className="muted small">
                  Estos datos serán vinculados oficialmente a tu cuenta para contrataciones y retiros bancarios.
                </p>
              </div>
            </div>

            <div className="kyc-confirm-grid">
              <div className="kyc-confirm-item">
                <span className="muted small">Nombre Oficial:</span>
                <strong>{fullName}</strong>
              </div>
              <div className="kyc-confirm-item">
                <span className="muted small">Documento de Identidad:</span>
                <strong>
                  {docType}-{docNumber}
                </strong>
              </div>
              <div className="kyc-confirm-item">
                <span className="muted small">Fecha de Nacimiento:</span>
                <strong>{birthdate}</strong>
              </div>
              <div className="kyc-confirm-item">
                <span className="muted small">Teléfono de Contacto:</span>
                <strong>
                  {phonePrefix}-{phoneRest}
                </strong>
              </div>
              {address && (
                <div className="kyc-confirm-item full-width">
                  <span className="muted small">Dirección Residencial:</span>
                  <strong>{address}</strong>
                </div>
              )}
            </div>

            <div className="kyc-confirm-previews">
              <div className="kyc-confirm-preview">
                <span className="muted small">Foto del Anverso:</span>
                {frontPreview && (
                  <img src={frontPreview} alt="Cédula capturada" className="kyc-confirm-thumb" />
                )}
              </div>
              <div className="kyc-confirm-preview">
                <span className="muted small">Selfie con Cédula:</span>
                {selfiePreview && (
                  <img src={selfiePreview} alt="Selfie capturada" className="kyc-confirm-thumb" />
                )}
              </div>
            </div>
          </div>

          <div className="kyc-terms-box">
            <p>
              Declaro bajo fe de juramento que la información suministrada es verídica y que las fotos corresponden
              a mi persona y a mi documento de identidad legal vigente en la República Bolivariana de Venezuela.
            </p>
          </div>

          <div className="kyc-actions-bar">
            <button
              type="button"
              className="button outline-light kyc-btn-back"
              disabled={loading}
              onClick={() => setStep(2)}
            >
              <ArrowLeft size={16} />
              <span>Volver a la Selfie</span>
            </button>
            <button
              type="button"
              className="button primary kyc-btn-action"
              disabled={loading}
              onClick={handleSubmitFinal}
            >
              {loading ? (
                <>
                  <Loader2 size={18} className="kyc-spin" />
                  <span>Enviando verificación...</span>
                </>
              ) : (
                <>
                  <ShieldCheck size={18} />
                  <span>Confirmar y Enviar Verificación</span>
                </>
              )}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
