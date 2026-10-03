# Muestras de servicios por categoría

El portal sandbox contiene tres muestras por cada una de las 13 categorías: **39 profesionales y 39 servicios**. Se conservaron los tres perfiles originales y se añadieron 36. No se crearon reseñas ni verificaciones KYC ficticias.

Cada muestra tiene su perfil, un servicio, modalidad, precio de ejemplo en Bs o USD y descripción. Los presenciales cuentan con sedes ficticias privadas en Caracas, Maracaibo y Valencia, vinculadas al catálogo de estado, municipio y parroquia y con cobertura de 25 km; los remotos no necesitan ubicación. Los precios son valores independientes de demostración, sin conversión ni tipo de cambio. Los nuevos profesionales reciben los tres créditos de bienvenida mediante la función habitual de activación.

| Categoría | Servicio | Profesional | Precio de ejemplo | Ubicación de instalación base |
|---|---|---|---|---|
| plomeria | Reparaciones e instalaciones de plomería | Mateo Rodríguez | US$ 13,00 | Caracas |
| plomeria | Instalación de sanitarios y grifería | Valentina Ríos | Bs 1.700,00 | Maracaibo |
| plomeria | Destape y mantenimiento de desagües | Andrés Salazar | US$ 19,00 | Valencia |
| informatica | Diagnóstico y reparación de computadores | Daniel Torres | US$ 16,00 | Caracas |
| informatica | Mantenimiento preventivo de computadores | Camila Duarte | Bs 2.200,00 | Maracaibo |
| informatica | Instalación de SSD y ampliación de memoria | Nicolás Pardo | US$ 19,00 | Valencia |
| diseno-grafico | Diseño de identidad visual | Lucía Martínez | US$ 50,00 | Remoto |
| diseno-grafico | Diseño de piezas para redes sociales | Sofía Vega | Bs 3.600,00 | Remoto |
| diseno-grafico | Presentaciones y catálogos digitales | Martín Cano | US$ 48,00 | Remoto |
| programacion | Páginas web para pequeños negocios | Sebastián Lara | US$ 130,00 | Remoto |
| programacion | Automatización de tareas y reportes | Paula Méndez | Bs 9.600,00 | Remoto |
| programacion | Desarrollo de tiendas virtuales | Diego Roldán | US$ 240,00 | Remoto |
| carpinteria | Muebles de madera a medida | Miguel Herrera | US$ 84,00 | Caracas |
| carpinteria | Reparación de puertas y cajones | Elena Rubio | Bs 3.000,00 | Maracaibo |
| carpinteria | Instalación de repisas y organizadores | Óscar Medina | US$ 19,00 | Valencia |
| cerrajeria | Instalación de cerraduras residenciales | Juan Castillo | US$ 24,00 | Caracas |
| cerrajeria | Cambio de chapas y herrajes | Sara Peña | Bs 1.800,00 | Maracaibo |
| cerrajeria | Mantenimiento de cerraduras y puertas | Ricardo León | US$ 17,00 | Valencia |
| cerrajeria-automotriz | Duplicado de llaves para automóvil | Manuel Suárez | US$ 36,00 | Caracas |
| cerrajeria-automotriz | Reparación de controles y carcasas | Diana Acosta | Bs 1.900,00 | Maracaibo |
| cerrajeria-automotriz | Diagnóstico y configuración de controles | Luis Montoya | US$ 50,00 | Valencia |
| electricidad | Instalación de lámparas y luminarias | Fernando Gil | US$ 17,00 | Caracas |
| electricidad | Diagnóstico de fallas eléctricas | Laura Navarro | Bs 3.200,00 | Maracaibo |
| electricidad | Instalación de tomacorrientes e interruptores | Esteban Cruz | US$ 22,00 | Valencia |
| limpieza | Limpieza general de apartamentos | Mariela Ponce | US$ 30,00 | Caracas |
| limpieza | Limpieza profunda de cocinas y baños | Natalia Beltrán | Bs 3.800,00 | Maracaibo |
| limpieza | Lavado de muebles y tapizados | Julián Ortiz | US$ 36,00 | Valencia |
| pintura | Pintura de interiores por ambiente | Ana Castillo | US$ 56,00 | Caracas |
| pintura | Resane y reparación de paredes | Mauricio Rey | Bs 3.600,00 | Maracaibo |
| pintura | Acabados decorativos para paredes | Verónica López | US$ 70,00 | Valencia |
| refrigeracion | Mantenimiento de aire acondicionado | Héctor Sánchez | US$ 32,00 | Caracas |
| refrigeracion | Diagnóstico y reparación de neveras | Carolina Patiño | Bs 3.600,00 | Maracaibo |
| refrigeracion | Instalación de aire acondicionado minisplit | Felipe Campos | US$ 76,00 | Valencia |
| electrodomesticos | Diagnóstico y reparación de lavadoras | Beatriz Cárdenas | US$ 30,00 | Caracas |
| electrodomesticos | Mantenimiento de secadoras | Cristian Melo | Bs 3.600,00 | Maracaibo |
| electrodomesticos | Revisión de hornos y estufas eléctricas | Mónica Vargas | US$ 32,00 | Valencia |
| traduccion | Traducción de inglés a español | Gabriela Rivas | US$ 18,00 | Remoto |
| traduccion | Traducción de portugués a español | Tomás Ferrer | Bs 1.900,00 | Remoto |
| traduccion | Revisión y corrección de textos bilingües | Isabel Fuentes | US$ 15,00 | Remoto |

## Área del portal publicado

Desde el 11 de septiembre de 2026, las 30 sedes presenciales del dominio publicado están alrededor de Valera: entre 2,5 y 42,5 km de la dirección de referencia autorizada y con cobertura de 50 km. Se mantienen 39 servicios, tres por categoría, y los nueve remotos no necesitan sede. La tabla anterior describe la semilla de instalación base; el entorno publicado conserva su distribución local mediante `.demo-geography-override.json` (0600), sin guardar coordenadas de la referencia en ese archivo. La dirección del cliente ficticio también se adaptó para mantener coherentes las pruebas de contratación.

[Activación de perfiles y reubicación de las muestras](profile-activation-and-demo-area.md).

## Repetir la carga

```bash
cd /home/davebas/chamba
backend/.venv/bin/python scripts/seed_service_samples.py          # Vista previa
backend/.venv/bin/python scripts/seed_service_samples.py --apply  # Carga sandbox
```

La carga se ejecuta en una transacción con bloqueo de concurrencia y utiliza las funciones existentes de registro, activación, servicios y cobertura. Repetirla conserva los perfiles y no duplica usuarios, servicios ni créditos. Se rechaza en producción o con pagos fuera del sandbox. Los datos fuente están en `scripts/demo_services.json` y `scripts/dev.py setup` los incorpora a las instalaciones de prueba.

Los accesos adicionales se guardan en `.demo-services-access.json` (0600), excluido del repositorio y del directorio web. Los accesos originales siguen en `.local-access.md`. No se cambian sus contraseñas.

La adaptación de muestras existentes de Colombia a Venezuela se realiza mediante `scripts/localize_venezuela_samples.py`, con vista previa por defecto y `--apply` para confirmar en sandbox. Solo modifica muestras reconocidas; conserva contratos y registros financieros históricos. [Procedencia geográfica y controles](venezuela-geolocation.md).

## Imágenes

Las 13 categorías usan imágenes ilustrativas de 1280 × 720 generadas con la herramienta integrada Imagegen y codificadas en WebP, todas menores a 100 KB. Una portada por categoría se comparte entre sus tres servicios.

- Archivos publicados: `frontend/public/images/services/`.
- Originales: `output/imagegen/services/`.
- [Prompts de las diez categorías nuevas](../output/imagegen/services/category-prompts.md).
- [Prompts de las tres categorías iniciales](../output/imagegen/services/prompts.md).

La consulta de exploración solicita hasta 100 perfiles para mostrar las 39 muestras; cada filtro de categoría devuelve sus tres perfiles. Las pruebas de navegador verifican los 39 servicios, sus imágenes y la apertura de un perfil de cada categoría.
