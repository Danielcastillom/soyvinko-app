import React, { useState, useEffect } from "react";
import {
  Wrench,
  ShoppingBag,
  Truck,
  MessageSquare,
  ShieldCheck,
  CheckCircle2,
} from "lucide-react";

export type ShowcaseItem = {
  id: string;
  pillLabel: string;
  categoryTag: string;
  headline: string;
  badgeTitle: string;
  badgeDetail: string;
  statusText: string;
  icon: React.ReactNode;
  image: string;
  fallbackImage: string;
  alt: string;
  accentColor: string;
};

export const AUTH_SHOWCASES: ShowcaseItem[] = [
  {
    id: "contratacion",
    pillLabel: "Servicios",
    categoryTag: "CONTRATACIÓN DIRECTA",
    headline: "Técnicos calificados para tu hogar y oficina",
    badgeTitle: "Servicio Confirmado",
    badgeDetail: "Técnico verificado · Llegada hoy",
    statusText: "Trato directo sin intermediarios",
    icon: <Wrench size={15} />,
    image: "/images/auth-showcase/contratacion.webp",
    fallbackImage: "/images/auth-showcase/contratacion.jpg",
    alt: "Usuaria verificando servicio agendado con confirmación en su teléfono móvil",
    accentColor: "#10B981",
  },
  {
    id: "compras",
    pillLabel: "Market",
    categoryTag: "COMPRAS & ARTÍCULOS",
    headline: "Herramientas, tecnología y repuestos oficiales",
    badgeTitle: "Compra en Marketplace",
    badgeDetail: "Pedido directo · 0% comisión",
    statusText: "Catálogo de comercios verificados",
    icon: <ShoppingBag size={15} />,
    image: "/images/auth-showcase/compras.webp",
    fallbackImage: "/images/auth-showcase/compras.jpg",
    alt: "Cliente con bolsa de compras mostrando productos comprados en su celular",
    accentColor: "#8B5CF6",
  },
  {
    id: "delivery",
    pillLabel: "Delivery",
    categoryTag: "DELIVERY & ENVÍOS",
    headline: "Despacho coordinado y entregas seguras",
    badgeTitle: "Envío en Camino",
    badgeDetail: "Ruta optimizada · Entrega puntual",
    statusText: "Rastreo y confirmación inmediata",
    icon: <Truck size={15} />,
    image: "/images/auth-showcase/delivery.webp",
    fallbackImage: "/images/auth-showcase/delivery.jpg",
    alt: "Repartidora con paquete en mano mostrando mapa de ruta en smartphone",
    accentColor: "#3B82F6",
  },
  {
    id: "presupuestos",
    pillLabel: "Cotizar",
    categoryTag: "PRESUPUESTOS AL INSTANTE",
    headline: "Cotizaciones claras acordadas en tiempo real",
    badgeTitle: "Presupuesto Aprobado",
    badgeDetail: "Cotización pactada · Trato directo",
    statusText: "Chat directo con el profesional",
    icon: <MessageSquare size={15} />,
    image: "/images/auth-showcase/presupuestos.webp",
    fallbackImage: "/images/auth-showcase/presupuestos.jpg",
    alt: "Profesional sonriente dando pulgar arriba con cotización aprobada en pantalla",
    accentColor: "#F59E0B",
  },
  {
    id: "pagos",
    pillLabel: "Garantía",
    categoryTag: "PAGOS EN CUSTODIA (ESCROW)",
    headline: "Tu dinero 100% protegido hasta tu conformidad",
    badgeTitle: "Pago Seguro en Custodia",
    badgeDetail: "Fondos protegidos hasta conformidad",
    statusText: "Garantía total de satisfacción",
    icon: <ShieldCheck size={15} />,
    image: "/images/auth-showcase/pagos.webp",
    fallbackImage: "/images/auth-showcase/pagos.jpg",
    alt: "Usuaria corporativa mostrando escudo de pago seguro en su smartphone",
    accentColor: "#059669",
  },
];

export function AuthShowcase() {
  // Inicialización aleatoria en cada recarga o refresco de página
  const [index, setIndex] = useState(() =>
    Math.floor(Math.random() * AUTH_SHOWCASES.length)
  );
  const [isTransitioning, setIsTransitioning] = useState(false);

  // Auto-rotación suave cada 7.5 segundos
  useEffect(() => {
    const timer = setInterval(() => {
      handleSelect((prev) => (prev + 1) % AUTH_SHOWCASES.length);
    }, 7500);
    return () => clearInterval(timer);
  }, []);

  const handleSelect = (nextIdx: number | ((curr: number) => number)) => {
    setIsTransitioning(true);
    setTimeout(() => {
      setIndex(nextIdx);
      setIsTransitioning(false);
    }, 180);
  };

  const current = AUTH_SHOWCASES[index];

  return (
    <div className="auth-showcase-card">
      {/* Cabecera de la tarjeta con estilo limpio y luminoso */}
      <div className="auth-card-top-bar">
        <div className="card-category-chip">
          <span className="chip-icon">{current.icon}</span>
          <span>{current.categoryTag}</span>
        </div>
        <div className="card-live-indicator">
          <span className="live-dot" />
          <span>En plataforma</span>
        </div>
      </div>

      {/* Escenario visual en fondo blanco puro de estudio */}
      <div className="auth-card-stage">
        <div
          className={`stage-character-wrapper ${
            isTransitioning ? "fade-out" : "fade-in"
          }`}
        >
          <picture>
            <source srcSet={current.image} type="image/webp" />
            <img
              src={current.fallbackImage}
              alt={current.alt}
              className="stage-person-img"
              loading="eager"
            />
          </picture>

          {/* Notificación flotante inferior de alto contraste */}
          <div className="stage-push-notification">
            <div className="push-icon-circle" style={{ background: current.accentColor }}>
              <CheckCircle2 size={16} color="#ffffff" />
            </div>
            <div className="push-text-col">
              <strong className="push-title">{current.badgeTitle}</strong>
              <span className="push-subtitle">{current.badgeDetail}</span>
            </div>
          </div>
        </div>
      </div>

      {/* Barra de 5 pestañas distribuidas en cuadrícula equilibrada */}
      <div className="auth-card-tabs">
        {AUTH_SHOWCASES.map((item, idx) => (
          <button
            key={item.id}
            type="button"
            className={`card-tab-btn ${idx === index ? "active" : ""}`}
            onClick={() => handleSelect(idx)}
            title={item.headline}
          >
            {item.icon}
            <span>{item.pillLabel}</span>
          </button>
        ))}
      </div>
    </div>
  );
}
