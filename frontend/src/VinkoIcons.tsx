import type { SVGProps, ComponentType } from "react";

export interface VinkoIconProps extends SVGProps<SVGSVGElement> {
  size?: number | string;
  strokeWidth?: number;
}

// 1. Plomería: Grifo curvo moderno con gota de agua orgánica
export function VinkoIconPlumbing({
  size = 24,
  strokeWidth = 2,
  ...props
}: VinkoIconProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <path d="M4 14h6a4 4 0 0 0 4-4V5a2 2 0 0 1 2-2h1" />
      <path d="M4 11v6" />
      <path d="M14 6h4" />
      <path d="M16 14a2.5 2.5 0 0 0 5 0c0-1.8-2.5-4-2.5-4s-2.5 2.2-2.5 4z" />
      <circle cx="8" cy="14" r="1" fill="currentColor" />
    </svg>
  );
}

// 2. Electricidad: Bombilla suave redondeada con destello / energía
export function VinkoIconElectricity({
  size = 24,
  strokeWidth = 2,
  ...props
}: VinkoIconProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <path d="M9 18h6" />
      <path d="M10 21h4" />
      <path d="M12 2a7 7 0 0 0-4.9 12c.9.9 1.4 1.8 1.4 3h7c0-1.2.5-2.1 1.4-3A7 7 0 0 0 12 2z" />
      <path d="M12 6v4l-2 2h4l-1 3" />
    </svg>
  );
}

// 3. Informática / Soporte Técnico: Pantalla curva con terminal y chip
export function VinkoIconTech({
  size = 24,
  strokeWidth = 2,
  ...props
}: VinkoIconProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <rect x="3" y="3" width="18" height="13" rx="3" />
      <path d="M8 21h8" />
      <path d="M12 16v5" />
      <path d="m8 8 2.5 2.5L8 13" />
      <path d="M13 13h3" />
    </svg>
  );
}

// 4. Cerrajería: Llave redondeada moderna de seguridad
export function VinkoIconLocksmith({
  size = 24,
  strokeWidth = 2,
  ...props
}: VinkoIconProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <circle cx="8" cy="15" r="5" />
      <circle cx="8" cy="15" r="2" />
      <path d="m11.5 11.5 7.5-7.5" />
      <path d="m16 4 4 4" />
      <path d="m14 6 2 2" />
      <path d="M19 7v2" />
    </svg>
  );
}

// 5. Diseño Gráfico: Paleta orgánica con puntos de color y pincel suave
export function VinkoIconDesign({
  size = 24,
  strokeWidth = 2,
  ...props
}: VinkoIconProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <path d="M12 2C6.5 2 2 6.5 2 12c0 3.3 2.1 5.9 5 6.7 1 .3 1.5-.7 1.5-1.4v-.8c0-1.4 1.1-2.5 2.5-2.5h1.2c4 0 7.8-3.2 7.8-7.2C20 4.6 16.4 2 12 2z" />
      <circle cx="7" cy="8" r="1.5" fill="currentColor" />
      <circle cx="12" cy="6" r="1.5" fill="currentColor" />
      <circle cx="16.5" cy="9.5" r="1.5" fill="currentColor" />
    </svg>
  );
}

// 6. Programación / Software: Código orbital y enlaces de red
export function VinkoIconProgramming({
  size = 24,
  strokeWidth = 2,
  ...props
}: VinkoIconProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <path d="m7 8-4 4 4 4" />
      <path d="m17 8 4 4-4 4" />
      <path d="m14 4-4 16" />
      <circle cx="12" cy="12" r="1" fill="currentColor" />
    </svg>
  );
}

// 7. Carpintería: Herramienta artesanal de madera
export function VinkoIconCarpentry({
  size = 24,
  strokeWidth = 2,
  ...props
}: VinkoIconProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <path d="M3 17h14a4 4 0 0 0 4-4v-2H7a4 4 0 0 0-4 4v2z" />
      <path d="M7 11V6a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v5" />
      <circle cx="6" cy="14" r="1" fill="currentColor" />
      <path d="M19 13v4" />
    </svg>
  );
}

// 8. Limpieza: Botella rociadora con burbujas y destellos
export function VinkoIconCleaning({
  size = 24,
  strokeWidth = 2,
  ...props
}: VinkoIconProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <path d="M6 10h8l-1 11H7L6 10z" />
      <path d="M10 6v4" />
      <path d="M8 6h4" />
      <path d="M12 6a3 3 0 0 0 3-3V2H9" />
      <circle cx="18" cy="8" r="1.5" />
      <circle cx="20" cy="13" r="1" />
      <path d="m17 18 1-1 1 1-1 1z" fill="currentColor" />
    </svg>
  );
}

// 9. Pintura: Rodillo de acabado suave
export function VinkoIconPainting({
  size = 24,
  strokeWidth = 2,
  ...props
}: VinkoIconProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <rect x="4" y="3" width="16" height="6" rx="2" />
      <path d="M12 9v4a2 2 0 0 1-2 2H8a2 2 0 0 0-2 2v4" />
      <rect x="4" y="19" width="4" height="3" rx="1" />
    </svg>
  );
}

// 10. Refrigeración: Copo de nieve y brisa fresca
export function VinkoIconCooling({
  size = 24,
  strokeWidth = 2,
  ...props
}: VinkoIconProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <path d="M12 2v20" />
      <path d="M4.93 4.93l14.14 14.14" />
      <path d="M2 12h20" />
      <path d="M4.93 19.07l14.14-14.14" />
      <circle cx="12" cy="12" r="2.5" />
    </svg>
  );
}

// 11. Electrodomésticos: Lavadora inteligente con tambor
export function VinkoIconAppliance({
  size = 24,
  strokeWidth = 2,
  ...props
}: VinkoIconProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <rect x="4" y="2" width="16" height="20" rx="3" />
      <circle cx="12" cy="13" r="5" />
      <circle cx="12" cy="13" r="2" />
      <circle cx="8" cy="5.5" r="1" fill="currentColor" />
      <circle cx="12" cy="5.5" r="1" fill="currentColor" />
    </svg>
  );
}

// 12. Traducción: Globos de diálogo que conectan culturas
export function VinkoIconTranslation({
  size = 24,
  strokeWidth = 2,
  ...props
}: VinkoIconProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <path d="M5 8h10a4 4 0 0 1 4 4v1a4 4 0 0 1-4 4h-4l-4 3v-3H5a4 4 0 0 1-4-4v-1a4 4 0 0 1 4-4z" />
      <path d="M7 11h6" />
      <path d="M10 9v4" />
    </svg>
  );
}

// 13. Conexión Humana / Trato Directo: Manos dándose la mano
export function VinkoIconHandshake({
  size = 24,
  strokeWidth = 2,
  ...props
}: VinkoIconProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <path d="M18 11l-4-4a2.5 2.5 0 0 0-3.5 0L8 9.5" />
      <path d="M6 13l4 4a2.5 2.5 0 0 0 3.5 0L16 14.5" />
      <path d="m2 8 4 4" />
      <path d="m22 16-4-4" />
      <circle cx="12" cy="12" r="1" fill="currentColor" />
    </svg>
  );
}

// 14. Pagos Protegidos: Escudo suave con check
export function VinkoIconShieldSafe({
  size = 24,
  strokeWidth = 2,
  ...props
}: VinkoIconProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
      <path d="m9 12 2 2 4-4" />
    </svg>
  );
}

// 15. Estrella Vinko / Reconocimiento: 4 puntas suaves como la referencia
export function VinkoIconSparkle({
  size = 24,
  strokeWidth = 2,
  ...props
}: VinkoIconProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <path d="M12 2c0 4.5 3.5 8 8 8-4.5 0-8 3.5-8 8 0-4.5-3.5-8-8-8 4.5 0 8-3.5 8-8z" />
    </svg>
  );
}

// 16. Comunidad / Profesionales Conectados
export function VinkoIconUsers({
  size = 24,
  strokeWidth = 2,
  ...props
}: VinkoIconProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <circle cx="9" cy="8" r="4" />
      <path d="M3 20a6 6 0 0 1 12 0" />
      <circle cx="17" cy="9" r="3" />
      <path d="M16 16.5a5 5 0 0 1 6 3.5" />
    </svg>
  );
}

// 17. Teléfono inteligente con la App SoyVinko
export function VinkoIconPhoneApp({
  size = 24,
  strokeWidth = 2,
  ...props
}: VinkoIconProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <rect x="6" y="2" width="12" height="20" rx="3" />
      <circle cx="12" cy="18" r="1" fill="currentColor" />
      <path d="m10 9 1.5 1.5L14 8" />
    </svg>
  );
}

// 18. Billetera y pagos
export function VinkoIconWallet({
  size = 24,
  strokeWidth = 2,
  ...props
}: VinkoIconProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <rect x="3" y="6" width="18" height="14" rx="3" />
      <path d="M3 10h18" />
      <circle cx="16" cy="14" r="1.5" fill="currentColor" />
      <path d="M7 6V4a2 2 0 0 1 2-2h6a2 2 0 0 1 2 2v2" />
    </svg>
  );
}

// 19. Lupa de búsqueda con curvas suaves
export function VinkoIconSearchLens({
  size = 24,
  strokeWidth = 2,
  ...props
}: VinkoIconProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <circle cx="11" cy="11" r="7" />
      <path d="m20 20-4-4" />
      <path d="M8.5 8.5a3.5 3.5 0 0 1 4 0" />
    </svg>
  );
}

// 20. Conversación y acuerdo directo
export function VinkoIconChatConversation({
  size = 24,
  strokeWidth = 2,
  ...props
}: VinkoIconProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z" />
      <circle cx="9" cy="12" r="1" fill="currentColor" />
      <circle cx="13" cy="12" r="1" fill="currentColor" />
      <circle cx="17" cy="12" r="1" fill="currentColor" />
    </svg>
  );
}

// 21. Check de confirmación y satisfacción
export function VinkoIconCheckCircle({
  size = 24,
  strokeWidth = 2,
  ...props
}: VinkoIconProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <circle cx="12" cy="12" r="9" />
      <path d="m8.5 12 2.5 2.5 5-5" />
    </svg>
  );
}

// 22. Créditos Vinkos: Moneda oficial de la plataforma con insignia V
export function VinkoIconCoin({
  size = 24,
  strokeWidth = 2,
  ...props
}: VinkoIconProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <circle cx="12" cy="12" r="9" />
      <path d="M8 8.5 12 16.5l4-8" />
      <circle cx="12" cy="12" r="6" strokeDasharray="2 2" strokeWidth={1.2} />
    </svg>
  );
}

// Mapeo automático de categorías por slug
export const vinkoCategoryIconMap: Record<string, ComponentType<VinkoIconProps>> = {
  plomeria: VinkoIconPlumbing,
  electricidad: VinkoIconElectricity,
  informatica: VinkoIconTech,
  cerrajeria: VinkoIconLocksmith,
  "cerrajeria-automotriz": VinkoIconLocksmith,
  "diseno-grafico": VinkoIconDesign,
  programacion: VinkoIconProgramming,
  carpinteria: VinkoIconCarpentry,
  limpieza: VinkoIconCleaning,
  pintura: VinkoIconPainting,
  refrigeracion: VinkoIconCooling,
  electrodomesticos: VinkoIconAppliance,
  traduccion: VinkoIconTranslation,
};
