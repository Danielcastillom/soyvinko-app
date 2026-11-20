// Archivo generado a partir de las páginas legales de la web (frontend/src/pages/*.tsx).
// Mantener el texto sincronizado con la web; no editar a mano salvo correcciones puntuales.
package com.soyvinko.app.ui.feature.profile

import com.soyvinko.app.ui.feature.profile.LegalBlock.Bullets
import com.soyvinko.app.ui.feature.profile.LegalBlock.Callout
import com.soyvinko.app.ui.feature.profile.LegalBlock.Card
import com.soyvinko.app.ui.feature.profile.LegalBlock.Checks
import com.soyvinko.app.ui.feature.profile.LegalBlock.H
import com.soyvinko.app.ui.feature.profile.LegalBlock.P
import com.soyvinko.app.ui.feature.profile.LegalBlock.Table

val LEGAL_DOCS: List<LegalDoc> = listOf(
    LegalDoc(
        slug = "terminos",
        short = "Términos",
        eyebrow = "MARCO LEGAL Y CONTRACTUAL",
        title = "Términos y Condiciones de Uso de SoyVinko",
        description = "Reglas claras, derechos y obligaciones para contratar servicios, comercializar toda clase de productos y operar con garantía en SoyVinko.",
        updated = "1 de octubre de 2026",
        status = "Versión 2.0 Vigente",
        sections = listOf(
            LegalSection(
                "01", "Objeto y Rol de SoyVinko",
                listOf(
                    P("**SoyVinko** es una plataforma tecnológica que facilita el encuentro directo entre usuarios que requieren servicios para el hogar, empresas o proyectos digitales (\"Clientes\"), profesionales independientes o empresas prestadoras de servicios (\"Profesionales\") y comercios proveedores de productos, artículos, herramientas y bienes de cualquier categoría (\"Comercios\")."),
                    P("Salvo que se indique expresamente lo contrario en contratos corporativos específicos, **SoyVinko actúa como intermediario tecnológico** y proveedor del software de presupuestos, trazabilidad y custodia de pagos. La relación contractual sustantiva de la obra o servicio se establece directamente entre el Cliente y el Profesional."),
                ),
            ),
            LegalSection(
                "02", "Cuentas y Tipos de Usuario",
                listOf(
                    P("Para acceder a la contratación y publicación de servicios, el usuario debe crear una cuenta:"),
                    Bullets(listOf(
                        "**Persona Natural:** Usuarios individuales que solicitan servicios o profesionales independientes. Se requiere mayoría de edad legal (18 años) y correo electrónico verificado.",
                        "**Empresa / Persona Jurídica:** Sociedades mercantiles de Venezuela debidamente registradas con Registro de Información Fiscal (RIF) y persona de contacto autorizada.",
                        "**Verificación de Identidad (KYC):** Los profesionales que deseen la insignia de verificación oficial deben consignar documento de identidad y fotografía de confirmación en la sección de expediente.",
                    )),
                ),
            ),
            LegalSection(
                "03", "Contratación de Servicios y Pagos Protegidos en Custodia (Escrow)",
                listOf(
                    P("Para erradicar la incertidumbre en los pagos y garantizar la entrega conforme del trabajo:"),
                    Bullets(listOf(
                        "**Acuerdo de Presupuesto:** El contrato se formaliza cuando ambas partes convienen el precio, modalidad (Presencial o Remota) y los requerimientos del trabajo.",
                        "**Depósito en Custodia (Escrow):** En servicios garantizados, los fondos acordados quedan bloqueados en custodia en la plataforma. Ni el cliente puede retirar los fondos unilateralmente sin justificación, ni el profesional los cobra hasta la finalización satisfactoria.",
                        "**Liberación de Fondos:** Una vez entregado el trabajo y confirmado por el cliente (o transcurrido el plazo de confirmación de entrega), los fondos se liberan de inmediato a la billetera del profesional.",
                    )),
                ),
            ),
            LegalSection(
                "04", "Marketplace de Productos y Artículos Generales",
                listOf(
                    P("El Marketplace permite a los comercios registrados ofrecer cualquier clase de producto, artículo, herramienta, insumo, repuesto o tecnología de comercio lícito:"),
                    Bullets(listOf(
                        "Los comercios garantizan que los productos publicados disponen de stock físico real y especificaciones fidedignas.",
                        "Las órdenes se generan de forma idempotente para evitar duplicidades de cobro.",
                        "El comercio es responsable de coordinar el embalaje y despacho conforme a los datos de entrega aportados por el comprador.",
                    )),
                ),
            ),
            LegalSection(
                "05", "Modelo de Negocio, Membresías y Comisiones Oficiales",
                listOf(
                    P("Las tarifas aplicables en la plataforma se rigen por la estructura oficial del modelo de negocio de SoyVinko:"),
                    H("A. Para Comercios y Vendedores (Tiendas / Productos Generales)"),
                    Table(
                        listOf("Plan", "Costo Mensual", "Comisión por Venta", "Inventario"),
                        listOf(
                            listOf("**Plan Promocional Entrada** (Meses 1 al 3)", "\$0,00 USD", "0% (Mes 1) / 3% (Meses 2 y 3)", "Hasta 15 productos"),
                            listOf("**Membresía Comercio Estándar** (Desde Mes 4)", "\$30,00 USD", "Tasa Mínima 4%", "Hasta 30 productos"),
                            listOf("**Membresía Comercio Plus** (Desde Mes 4)", "\$50,00 USD", "Tasa Mínima 4%", "Hasta 70 productos"),
                            listOf("**Modalidad Base Sin Suscripción** (Desde Mes 4)", "\$0,00 USD", "10% por venta", "Hasta 10 productos"),
                        ),
                    ),
                    H("B. Para Proveedores (Servicios Contratados)"),
                    Table(
                        listOf("Plan", "Costo Mensual", "Comisión por Servicio", "Beneficios"),
                        listOf(
                            listOf("**Plan Promocional Entrada** (Meses 1 al 3)", "\$0,00 USD", "0% (Mes 1) / 3% (Meses 2 y 3)", "Certificación de perfil gratis y acceso a cartelera"),
                            listOf("**Membresía Profesional** (Desde Mes 4)", "\$15,00 USD", "Tasa Mínima 3% Presencial / 6% Remoto", "Prioridad en solicitudes y descuento de viáticos"),
                            listOf("**Modalidad Base Sin Suscripción** (Desde Mes 4)", "\$0,00 USD", "8% Presencial / 12% Remoto", "Sin pago mensual, ideal para trabajos ocasionales"),
                        ),
                    ),
                    P("Nota: Las recargas de saldo a favor de clientes no tienen comisión alguna.", muted = true),
                ),
            ),
            LegalSection(
                "06", "Normas de Conducta y Prohibiciones",
                listOf(
                    P("Queda terminantemente prohibido a cualquier usuario:"),
                    Bullets(listOf(
                        "Publicar servicios ilícitos, peligrosos, falsificados o contrarios al orden público.",
                        "Intentar evadir los mecanismos de custodia y seguridad de la plataforma para defraudar a la contraparte.",
                        "Suplantar la identidad de terceros o utilizar documentos de identificación ajenos o alterados.",
                        "Manipular las reseñas y valoraciones mediante cuentas ficticias o acuerdos colusorios.",
                    )),
                ),
            ),
            LegalSection(
                "07", "Mediación de Disputas y Centro de Soporte",
                listOf(
                    P("Si surge una desavenencia respecto a la calidad o cumplimiento del servicio:"),
                    Bullets(listOf(
                        "Cualquiera de las partes puede abrir una disputa desde el detalle del contrato en la app.",
                        "El equipo de mediación de SoyVinko revisará las pruebas aportadas (mensajes en la plataforma, fotografías de entrega y requerimientos).",
                        "El mediador emitirá una resolución vinculante ordenando la liberación total, parcial o el reembolso del saldo en custodia al cliente.",
                    ), ordered = true),
                ),
            ),
            LegalSection(
                "08", "Limitación de Responsabilidad",
                listOf(
                    P("SoyVinko responde del correcto funcionamiento técnico de su infraestructura digital. No responde por daños fortuitos, fuerza mayor o vicios ocultos derivados de la ejecución física de las obras realizadas por los profesionales independientes, más allá de la cobertura del importe en custodia acordado."),
                ),
            ),
            LegalSection(
                "09", "Modificaciones y Ley Aplicable",
                listOf(
                    P("Estos Términos se rigen por las leyes de la República Bolivariana de Venezuela y los principios generales del comercio electrónico internacional. Cualquier controversia se someterá prioritariamente al proceso de mediación interna y, subsidiariamente, a los tribunales competentes."),
                ),
            ),
        ),
    ),
    LegalDoc(
        slug = "privacidad",
        short = "Privacidad",
        eyebrow = "TRANSPARENCIA Y SEGURIDAD",
        title = "Política de Privacidad y Protección de Datos",
        description = "En SoyVinko protegemos tu información personal bajo los más altos estándares internacionales (RGPD / GDPR, LGPD y normativas de protección de datos).",
        updated = "1 de octubre de 2026",
        status = "En vigor",
        sections = listOf(
            LegalSection(
                "01", "Responsable del Tratamiento",
                listOf(
                    P("El responsable del tratamiento de los datos personales recabados a través del sitio web **soyvinko.com** y sus aplicaciones asociadas es **SoyVinko** (en adelante, “Vinko”, “la Plataforma” o “nosotros”), con domicilio de operaciones en Venezuela y alcance de servicios digitales y remotos."),
                    P("Para cualquier consulta o solicitud relativa a esta Política de Privacidad, puedes dirigirte a nuestro equipo de protección de datos a través de:"),
                    Callout("**Canal directo de Privacidad:** privacidad@soyvinko.com / contacto@soyvinko.com"),
                ),
            ),
            LegalSection(
                "02", "Datos Personales que Tratamos",
                listOf(
                    P("Recopilamos y tratamos únicamente los datos necesarios para proporcionar nuestros servicios de conexión profesional, mediación de presupuestos y marketplace de productos y artículos:"),
                    Bullets(listOf(
                        "**Datos de Registro e Identificación:** Nombre y apellido o Razón Social (para empresas), dirección de correo electrónico, tipo de cuenta (Persona Natural o Jurídica con RIF) y contraseñas fuertemente hasheadas con algoritmo Argon2id.",
                        "**Datos de Perfil Profesional:** Título comercial, especialidades técnicas, años de experiencia, biografía laboral, fotos de trabajos realizados y enlaces de portafolio.",
                        "**Datos de Expediente de Identidad (KYC):** Cédula de identidad, número de documento, fotografías de verificación y selfies con documento en mano (únicamente para profesionales que solicitan la verificación oficial).",
                        "**Datos de Contacto y Ubicación:** Número telefónico de WhatsApp/llamadas (compartido solo tras la aceptación mutua de un contrato), direcciones y coordenadas de geolocalización aproximadas para búsqueda de profesionales cercanos.",
                        "**Datos de Transacciones y Marketplace:** Comprobantes de pago reportados (referencia, banco emisor, moneda), cotizaciones acordadas, registro de pedidos de productos/artículos e historial de pagos en custodia (Escrow).",
                        "**Datos Técnicos y de Conexión:** Identificadores de sesión HttpOnly, tokens anti-falsificación `chamba_csrf` y registros mínimos de auditoría técnica con marcas de tiempo.",
                    )),
                ),
            ),
            LegalSection(
                "03", "Finalidades y Bases Jurídicas del Tratamiento",
                listOf(
                    P("Conforme al **Reglamento General de Protección de Datos (RGPD)** y normativas homólogas, tratamos tus datos bajo las siguientes bases legales:"),
                    Table(
                        listOf("Finalidad del Tratamiento", "Base Legal Aplicable"),
                        listOf(
                            listOf("Creación y mantenimiento de tu cuenta de usuario o perfil profesional.", "**Ejecución del contrato** de prestación de servicios (Art. 6.1.b RGPD)."),
                            listOf("Conexión entre clientes y técnicos, gestión de presupuestos y custodia de pagos (Escrow).", "**Ejecución del contrato** bilateral acordado."),
                            listOf("Verificación de identidad (KYC) y prevención de fraudes o suplantaciones.", "**Interés legítimo** y **Consentimiento expreso** del usuario."),
                            listOf("Búsqueda geoespacial de técnicos cercanos según el estado y municipio del cliente.", "**Consentimiento** otorgado al permitir la geolocalización en el navegador."),
                            listOf("Facturación de membresías, emisión de comprobantes y trazabilidad contable.", "**Cumplimiento de obligaciones legales** y fiscales aplicables."),
                            listOf("Auditoría de seguridad y resolución de incidencias en soporte.", "**Interés legítimo** en salvaguardar la integridad de la comunidad."),
                        ),
                    ),
                ),
            ),
            LegalSection(
                "04", "Seguridad, Cifrado y Arquitectura de Datos",
                listOf(
                    P("La seguridad es el pilar estructural de SoyVinko. Hemos implementado medidas técnicas y organizativas de nivel bancario:"),
                    Card("Contraseñas con Argon2id", "Tus contraseñas nunca se almacenan en texto claro. Se procesan con Argon2id, el estándar criptográfico más resistente contra ataques por fuerza bruta."),
                    Card("Cifrado en Tránsito (TLS 1.3)", "Todas las comunicaciones entre tu navegador y nuestros servidores están cifradas mediante HTTPS con certificados TLS modernos y políticas HSTS forzadas."),
                    Card("Row Level Security (RLS)", "Las bases de datos aíslan los datos a nivel de fila y función (`Security Definer`). Ningún usuario puede consultar registros ajenos ni contraseñas."),
                ),
            ),
            LegalSection(
                "05", "Expedientes de Identidad y Verificación (KYC)",
                listOf(
                    P("Para garantizar que los clientes contraten con personas reales y de confianza, los profesionales pueden presentar su expediente KYC. Tratamos estos documentos con salvaguardas especiales:"),
                    Bullets(listOf(
                        "Los documentos de identidad y fotos se almacenan en repositorios aislados con cifrado en reposo.",
                        "El personal administrativo accede únicamente a través de auditoría individual registrada con motivo justificativo.",
                        "Los documentos nunca se publican en el perfil público ni son accesibles para otros usuarios.",
                    )),
                ),
            ),
            LegalSection(
                "06", "Geolocalización y Coordenadas",
                listOf(
                    P("SoyVinko permite encontrar servicios según la cercanía geográfica:"),
                    Bullets(listOf(
                        "Las coordenadas exactas de los clientes nunca se revelan a terceros de forma pública en el mapa.",
                        "La búsqueda pública utiliza aproximaciones por parroquia o municipio para proteger la privacidad del hogar.",
                        "La ubicación precisa solo se comparte de forma temporal y voluntaria con el profesional contratado una vez iniciado el servicio presencial.",
                    )),
                ),
            ),
            LegalSection(
                "07", "Plazos de Conservación de Datos",
                listOf(
                    P("Conservamos los datos personales durante el tiempo estrictamente necesario para cumplir con las finalidades descritas:"),
                    Bullets(listOf(
                        "**Datos de cuenta activa:** Mientras mantengas tu cuenta abierta en la plataforma.",
                        "**Historial de transacciones y contratos:** Por los periodos legalmente exigidos en materia tributaria y mercantil (mínimo 5 años).",
                        "**Documentos de verificación aprobados:** Mientras el profesional mantenga su certificación vigente.",
                        "**Al solicitar la baja de cuenta:** Los datos personales se anonimizan o eliminan de forma segura, salvo aquellos requeridos por mandatos legales.",
                    )),
                ),
            ),
            LegalSection(
                "08", "No Venta de Datos y Destinatarios",
                listOf(
                    P("**SoyVinko no comercializa, no alquila y no vende datos personales a terceros con fines publicitarios.**"),
                    P("Tus datos solo se comunican a:"),
                    Bullets(listOf(
                        "El profesional o cliente con quien contrates un servicio o una orden de compra, únicamente para coordinar la entrega o ejecución.",
                        "Proveedores tecnológicos auxiliares necesarios (alojamiento seguro en servidores dedicados, servicios de cartografía Mapbox).",
                        "Autoridades competentes cuando exista un requerimiento judicial vinculante debidamente motivado.",
                    )),
                ),
            ),
            LegalSection(
                "09", "Tus Derechos (ARCO & RGPD)",
                listOf(
                    P("Como titular de los datos, dispones de los siguientes derechos reconocidos internacionalmente:"),
                    Checks(listOf(
                        "**Derecho de Acceso:** Consultar qué datos tratamos sobre ti y obtener una copia de los mismos.",
                        "**Derecho de Rectificación:** Corregir o actualizar cualquier dato inexacto o desactualizado desde tu perfil.",
                        "**Derecho de Supresión (\"Derecho al Olvido\"):** Solicitar el borrado definitivo de tu cuenta y datos asociados cuando ya no sean necesarios.",
                        "**Derecho de Oposición:** Oponerte al tratamiento de tus datos para finalidades específicas basadas en interés legítimo.",
                        "**Derecho a la Portabilidad:** Recibir tus datos personales en un formato estructurado, común y legible por máquina.",
                        "**Revocación del Consentimiento:** Retirar en cualquier momento el consentimiento previamente otorgado (como las cookies o geolocalización).",
                    )),
                ),
            ),
            LegalSection(
                "10", "Ejercicio de Derechos y Contacto",
                listOf(
                    P("Para ejercer cualquiera de tus derechos o solicitar la supresión de tu cuenta, puedes enviarnos una solicitud indicando tu nombre y correo registrado a:"),
                    Callout("**Oficina de Privacidad y Cumplimiento:**\nCorreo electrónico: privacidad@soyvinko.com\nRespuesta en un plazo máximo de 72 horas hábiles."),
                ),
            ),
        ),
    ),
    LegalDoc(
        slug = "cookies",
        short = "Cookies",
        eyebrow = "POLÍTICA DE COOKIES Y ALMACENAMIENTO",
        title = "Política de Cookies de SoyVinko",
        description = "Conoce en detalle qué cookies utilizamos, para qué sirven y cómo gestionar tus preferencias en cualquier momento.",
        updated = "1 de octubre de 2026",
        status = null,
        sections = listOf(
            LegalSection(
                "01", "¿Qué son las Cookies?",
                listOf(
                    P("Una cookie es un pequeño archivo de texto que un sitio web guarda en tu ordenador o dispositivo móvil cuando lo visitas. Permiten que la plataforma recuerde tus acciones y preferencias (como el inicio de sesión cifrado, moneda de consulta o filtros de búsqueda) durante un periodo de tiempo determinado, para que no tengas que volver a configurarlos cada vez que regresas o navegas de una página a otra."),
                ),
            ),
            LegalSection(
                "02", "Cookies que Utilizamos en SoyVinko",
                listOf(
                    P("En SoyVinko clasificamos las cookies en las siguientes categorías funcionales:"),
                    Table(
                        listOf("Nombre de la Cookie", "Tipo / Categoría", "Finalidad", "Duración"),
                        listOf(
                            listOf("`chamba_csrf`", "Técnica / Esencial", "Protección de seguridad estricta contra falsificación de peticiones en sitios cruzados (Cross-Site Request Forgery).", "Sesión del navegador"),
                            listOf("`chamba_session` / Token de acceso", "Técnica / Esencial", "Autenticación de cuenta segura mediante encabezados HttpOnly cifrados. Mantiene tu sesión protegida.", "Persistente / Cierre de sesión"),
                            listOf("`vinko_cookie_consent`", "Funcional / Preferencias", "Guarda tu selección y consentimiento en el banner de cookies para no mostrártelo de forma intrusiva.", "1 año"),
                            listOf("`vinko_preferred_currency`", "Funcional / Preferencias", "Recuerda si prefieres visualizar las tarifas y presupuestos en Bolívares (VES) o Dólares (USD).", "Persistente"),
                        ),
                    ),
                ),
            ),
            LegalSection(
                "03", "Tecnologías de Almacenamiento Local (LocalStorage)",
                listOf(
                    P("Además de las cookies convencionales, utilizamos **LocalStorage** en el navegador para mejorar sustancialmente la velocidad de carga de la plataforma y evitar consumo innecesario de datos móviles:"),
                    Bullets(listOf(
                        "**Caché de Catálogo y Categorías:** Permite que la lista de oficios y servicios cargue al instante sin esperar peticiones al servidor.",
                        "**Preferencias de Búsqueda:** Guarda temporalmente los filtros de modalidad (Presencial o Remoto) seleccionados durante la navegación.",
                    )),
                ),
            ),
            LegalSection(
                "04", "Servicios y Cookies de Terceros",
                listOf(
                    P("Para prestar funcionalidades avanzadas de mapas y geografía de Venezuela, colaboramos con proveedores tecnológicos que pueden procesar datos técnicos:"),
                    Bullets(listOf(
                        "**Mapbox Inc. (Mapas Interactivos):** Proporciona la cartografía interactiva para localizar municipios y parroquias. Mapbox puede generar identificadores de sesión técnica para la entrega eficiente de fragmentos de mapa.",
                        "**Google Fonts:** Tipografías optimizadas para una lectura clara y profesional, servidas de forma estática sin seguimiento de perfiles.",
                    )),
                    Callout("**Aviso de no seguimiento:** SoyVinko no integra píxeles invasivos de publicidad comportamental ni redes de compra de datos (Data Brokers)."),
                ),
            ),
            LegalSection(
                "05", "Cómo Controlar y Modificar tus Preferencias",
                listOf(
                    Callout("En la app de SoyVinko para Android no usamos cookies de navegador: tu sesión se guarda cifrada en el almacenamiento seguro del teléfono y se borra al cerrar sesión. Puedes reconfigurar las cookies del sitio web desde el Centro de Preferencias de Cookies en soyvinko.com."),
                ),
            ),
            LegalSection(
                "06", "Desactivación desde la Configuración de tu Navegador",
                listOf(
                    P("También puedes restringir, bloquear o borrar las cookies configurando las opciones de tu navegador web:"),
                    Bullets(listOf(
                        "**Google Chrome:** Configuración > Privacidad y seguridad > Cookies y otros datos de sitios.",
                        "**Mozilla Firefox:** Ajustes > Privacidad & Seguridad > Cookies y datos del sitio.",
                        "**Apple Safari:** Preferencias > Privacidad > Bloquear todas las cookies o gestionar datos del sitio.",
                        "**Microsoft Edge:** Configuración > Permisos de cookies y del sitio.",
                    )),
                    P("Nota: Si bloqueas las cookies técnicas esenciales (`chamba_csrf` o cookies de sesión), es posible que no puedas iniciar sesión ni contratar servicios con custodia de pagos.", muted = true),
                ),
            ),
        ),
    ),
    LegalDoc(
        slug = "aviso-legal",
        short = "Aviso legal",
        eyebrow = "INFORMACIÓN CORPORATIVA Y LEGAL",
        title = "Aviso Legal y Condiciones Generales",
        description = "Información identificativa de SoyVinko, titularidad de los derechos de propiedad intelectual y condiciones de uso del portal.",
        updated = "1 de octubre de 2026",
        status = "Vigente",
        sections = listOf(
            LegalSection(
                "01", "Datos Identificativos",
                listOf(
                    P("En cumplimiento del deber de información y transparencia para servicios de la sociedad de la información y comercio electrónico:"),
                    Bullets(listOf(
                        "**Denominación comercial:** SoyVinko (\"Vinko\")",
                        "**Dominio web:** `https://soyvinko.com`",
                        "**Actividad principal:** Portal tecnológico de intermediación, contratación de servicios y marketplace de productos, herramientas, insumos y artículos de cualquier categoría.",
                        "**Correo de contacto oficial:** contacto@soyvinko.com",
                        "**Atención al usuario y soporte:** Disponible a través del Centro de Soporte en plataforma.",
                    )),
                ),
            ),
            LegalSection(
                "02", "Propiedad Intelectual e Industrial",
                listOf(
                    P("Todos los elementos que forman parte de este sitio web —incluyendo el código fuente, la arquitectura de software, bases de datos, logotipos, marcas comerciales, iconografías, diseño gráfico y textos— son propiedad exclusiva de **SoyVinko** o de terceros que han autorizado legítimamente su inclusión."),
                    P("Queda expresamente prohibida la reproducción, distribución, comunicación pública, ingeniería inversa o transformación no autorizada de cualquier elemento del portal sin el previo consentimiento por escrito de SoyVinko."),
                ),
            ),
            LegalSection(
                "03", "Condiciones de Acceso y Uso",
                listOf(
                    P("El acceso a soyvinko.com atribuye la condición de usuario e implica la aceptación plena de este Aviso Legal, los Términos de Servicio y la Política de Privacidad."),
                    P("El usuario se compromete a hacer un uso lícito y diligente de los contenidos y servicios ofrecidos, absteniéndose de realizar actividades lesivas o contrarias a la buena fe comercial."),
                ),
            ),
            LegalSection(
                "04", "Exclusión de Responsabilidad",
                listOf(
                    P("SoyVinko no garantiza la disponibilidad ininterrumpida del sitio ante eventuales caídas de telecomunicaciones ajenas a su control directo, si bien implementa las mejores prácticas operativas y de respaldo para mantener la máxima disponibilidad del servicio."),
                ),
            ),
            LegalSection(
                "05", "Ley Aplicable y Jurisdicción",
                listOf(
                    P("Para la resolución de cualquier conflicto relativo al presente sitio web o a las actividades en él desarrolladas, será de aplicación la legislación venezolana y los estándares internacionales de comercio electrónico, sometiéndose las partes a los tribunales competentes."),
                ),
            ),
        ),
    ),
)
