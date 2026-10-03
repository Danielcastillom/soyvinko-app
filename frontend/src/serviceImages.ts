export type ServiceImage = {
  src: string;
  alt: string;
  label: string;
};

// Illustrative category artwork, not portraits or portfolio evidence.
export const serviceImages: Record<string, ServiceImage> = {
  carpinteria: {
    src: "/images/services/carpinteria-v1.webp",
    alt: "Imagen de ejemplo de trabajo artesanal en un mueble de madera",
    label: "Carpintería",
  },
  cerrajeria: {
    src: "/images/services/cerrajeria-v1.webp",
    alt: "Imagen de ejemplo de instalación de una cerradura residencial",
    label: "Cerrajería",
  },
  "cerrajeria-automotriz": {
    src: "/images/services/cerrajeria-automotriz-v1.webp",
    alt: "Imagen de ejemplo de mantenimiento de una llave y control de automóvil",
    label: "Cerrajería automotriz",
  },
  programacion: {
    src: "/images/services/programacion-v1.webp",
    alt: "Imagen de ejemplo de un espacio de desarrollo de aplicaciones",
    label: "Desarrollo de software",
  },
  electricidad: {
    src: "/images/services/electricidad-v1.webp",
    alt: "Imagen de ejemplo de instalación de un interruptor de pared",
    label: "Electricidad",
  },
  limpieza: {
    src: "/images/services/limpieza-v1.webp",
    alt: "Imagen de ejemplo de limpieza de superficies en una cocina",
    label: "Limpieza",
  },
  pintura: {
    src: "/images/services/pintura-v1.webp",
    alt: "Imagen de ejemplo de renovación de una pared con pintura",
    label: "Pintura",
  },
  refrigeracion: {
    src: "/images/services/refrigeracion-v1.webp",
    alt: "Imagen de ejemplo de mantenimiento de filtros de aire acondicionado",
    label: "Refrigeración",
  },
  electrodomesticos: {
    src: "/images/services/electrodomesticos-v1.webp",
    alt: "Imagen de ejemplo de revisión técnica de una lavadora",
    label: "Electrodomésticos",
  },
  traduccion: {
    src: "/images/services/traduccion-v1.webp",
    alt: "Imagen de ejemplo de revisión y traducción de documentos",
    label: "Traducción",
  },
  plomeria: {
    src: "/images/services/plomeria-v1.webp",
    alt: "Imagen de ejemplo de una reparación de grifería en una cocina",
    label: "Plomería",
  },
  informatica: {
    src: "/images/services/informatica-v1.webp",
    alt: "Imagen de ejemplo del mantenimiento de un computador portátil",
    label: "Tecnología",
  },
  "diseno-grafico": {
    src: "/images/services/diseno-grafico-v1.webp",
    alt: "Imagen de ejemplo de un espacio de diseño gráfico con tableta y muestras de color",
    label: "Diseño gráfico",
  },
};
