package com.soyvinko.app.ui.feature.explore

import com.soyvinko.app.ui.FakeApi
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import java.io.File

/** Datos ficticios con la misma forma que devuelven las funciones SQL (public_listing, professional_profile, my_requests…). */
object ExploreFixtures {
    const val PLOMERIA = "f9e07ca6-8153-4e3d-9ba5-1fade27bdc79"
    const val ELECTRICIDAD = "01fe513c-208e-4020-b925-cbdda50eea49"
    const val INFORMATICA = "a383579f-4d2e-4561-87c7-7b81d52f38f5"
    const val DISENO = "2c190c5e-345c-4d85-9da4-aab21488c61c"

    const val MATEO = "115aabef-0946-4ae0-a56c-06e71500afce"
    const val VALENTINA = "5b0c3a7e-91d2-4f0e-8c1b-2f6a9d4e7c10"
    const val DANIEL = "9d257f67-b55a-4319-8a43-7d88ef05a24b"
    const val ANDREA = "21f6c92f-8080-42a2-a95e-b0728d3facd2"
    const val CARLOS = "6e1f2d3c-4b5a-4c7d-8e9f-0a1b2c3d4e5f"
    const val MATEO_FUGAS = "74aee115-2085-4e53-b26b-b3ad46eadf09"
    const val MATEO_CALENTADOR = "3c9d2e1f-6a7b-4c8d-9e0f-1a2b3c4d5e6f"

    const val REQ_OPEN = "8014fa1f-ac93-4e60-9a75-f6dbf7e06b8b"
    const val REQ_REMOTE = "fbd993d4-361d-42b0-b31e-2c37e8880743"
    const val REQ_MATCHED = "0a7c2b4e-5d6f-4a1b-9c8d-7e6f5a4b3c2d"
    const val REQ_DRAFT = "1b2c3d4e-5f60-4718-8a9b-0c1d2e3f4a5b"
    const val REQ_CANCELLED = "2c3d4e5f-6071-4829-9bac-1d2e3f4a5b6c"
    const val CASA = "c6a1e1d2-0f3b-4a8e-9c7d-1b2a3c4d5e6f"
    const val OFICINA = "d7b2f2e3-1a4c-4b9f-8d0e-2c3b4d5e6f70"

    val catalog = """{"data":[
        {"id":"f413cef5-a65c-423f-a356-c70207de2a09","name":"Carpintería","slug":"carpinteria","active":true,"parent_id":null},
        {"id":"75741b10-7674-47be-93aa-c194ea6a16db","name":"Cerrajería","slug":"cerrajeria","active":true,"parent_id":null},
        {"id":"2b795883-2478-43c5-ba4e-cdcd5d7c757f","name":"Cerrajería automotriz","slug":"cerrajeria-automotriz","active":true,"parent_id":"75741b10-7674-47be-93aa-c194ea6a16db"},
        {"id":"b2e23ddf-ef77-4176-99a8-32f01d8d111b","name":"Desarrollo de software","slug":"programacion","active":true,"parent_id":null},
        {"id":"$DISENO","name":"Diseño gráfico","slug":"diseno-grafico","active":true,"parent_id":null},
        {"id":"$ELECTRICIDAD","name":"Electricidad","slug":"electricidad","active":true,"parent_id":null},
        {"id":"00543502-05ec-4c80-bbf3-6acb44225f99","name":"Limpieza","slug":"limpieza","active":true,"parent_id":null},
        {"id":"52c5d4eb-21a2-4072-b3a5-80c49d11616a","name":"Pintura","slug":"pintura","active":true,"parent_id":null},
        {"id":"$PLOMERIA","name":"Plomería","slug":"plomeria","active":true,"parent_id":null},
        {"id":"3a646026-bc9d-4c38-b92b-dec9bbc460d7","name":"Refrigeración y aire acondicionado","slug":"refrigeracion","active":true,"parent_id":null},
        {"id":"ec79956f-da83-4946-9965-3dc8c252b6cb","name":"Reparación de electrodomésticos","slug":"electrodomesticos","active":true,"parent_id":null},
        {"id":"$INFORMATICA","name":"Reparación y soporte de computadores","slug":"informatica","active":true,"parent_id":null},
        {"id":"ac7b52db-d2e0-4c8e-b724-75d1dee1f9d9","name":"Traducción","slug":"traduccion","active":true,"parent_id":null}
    ]}"""

    private val mateoListing = """{"id":"$MATEO","bio":"Soluciones de plomería, reparación de fugas e instalaciones. Atención cuidadosa y trabajo explicado paso a paso.",
        "city":"Caracas","name":"Mateo Rodríguez","slug":"mateo-rodriguez","rating":4.83,"verified":true,"review_count":12,"years_experience":8,
        "requires_platform_payment":true,"offerings":[{"id":"$MATEO_FUGAS","title":"Reparación de fugas y grifería","currency":"USD",
        "category_id":"$PLOMERIA","description":"Detección y reparación de fugas, cambio de grifería y llaves de paso.","pricing_type":"from",
        "delivery_mode":"onsite","price_min_minor":1300}]}"""

    private val valentinaListing = """{"id":"$VALENTINA","bio":"Electricista residencial certificada. Instalaciones, tableros, tomacorrientes y lámparas con garantía.",
        "city":"Caracas","name":"Valentina Pérez","slug":"valentina-perez","rating":5.0,"verified":true,"review_count":3,"years_experience":5,
        "requires_platform_payment":false,"offerings":[{"id":"9a8b7c6d-5e4f-4a3b-2c1d-0e9f8a7b6c5d","title":"Instalaciones eléctricas residenciales","currency":"VES",
        "category_id":"$ELECTRICIDAD","description":"Tableros, breakers, tomas y lámparas.","pricing_type":"from","delivery_mode":"onsite","price_min_minor":4500000}]}"""

    private val danielListing = """{"id":"$DANIEL","bio":"Diagnóstico y reparación de computadores. Te ayudo a recuperar la tranquilidad y a poner tus equipos a punto.",
        "city":"Los Teques","name":"Daniel Torres","slug":"tecnico-demo","rating":null,"verified":false,"review_count":0,"years_experience":6,
        "requires_platform_payment":false,"offerings":[{"id":"116ea961-2d49-4a0c-9fa3-74a3e9675c96","title":"Diagnóstico y reparación de computadores",
        "currency":"USD","category_id":"$INFORMATICA","description":"Formateo, limpieza y cambio de piezas.","pricing_type":"from",
        "delivery_mode":"onsite","price_min_minor":1600}]}"""

    private val andreaListing = """{"id":"$ANDREA","bio":"Diseñadora de marca e identidad visual. Logos, empaques y piezas para redes con entregas claras.",
        "city":null,"name":"Andrea Salazar","slug":"andrea-salazar","rating":4.67,"verified":true,"review_count":9,"years_experience":7,
        "requires_platform_payment":true,"offerings":[{"id":"fc84565b-869d-4c13-9c5e-b5418a9054f2","title":"Identidad visual y logotipos","currency":"USD",
        "category_id":"$DISENO","description":"Propuesta de logo, paleta y manual básico.","pricing_type":"from","delivery_mode":"remote","price_min_minor":2500}]}"""

    private fun withDistance(json: String, meters: Long) = json.trimEnd().dropLast(1) + ""","distance_m":$meters}"""

    val nearby = """{"data":[${withDistance(mateoListing, 1200)},${withDistance(valentinaListing, 2400)},${withDistance(danielListing, 14800)}]}"""
    val discoverRemote = """{"data":[$andreaListing]}"""
    val discoverAll = """{"data":[$mateoListing,$valentinaListing,$danielListing,$andreaListing]}"""

    val parish = """{"data":{"country_code":"VE","state_code":"VE-A","state_name":"Distrito Capital","municipality_code":"VE-A-01",
        "municipality_name":"Libertador","parish_code":"VE-A-01-19","parish_name":"El Recreo"}}"""

    val markers = """{"data":[
        {"professional_id":"$MATEO","lat":10.4961,"lon":-66.8836,"approximate":false,"label":"Av. Casanova, Sabana Grande"},
        {"professional_id":"$VALENTINA","lat":10.4880,"lon":-66.8790,"approximate":true,"label":"El Recreo, Caracas"},
        {"professional_id":"$DANIEL","lat":10.3450,"lon":-67.0410,"approximate":true,"label":"Los Teques, Los Teques"}
    ]}"""

    val account = """{"data":{"id":"u1","email":"maria.gonzalez@example.com","name":"María González","status":"active","account_type":"natural",
        "business_name":null,"rif":null,"contact_name":null,"roles":["customer","professional"],"contacts":null,"addresses":[
        {"id":"$CASA","user_id":"u1","label":"Casa","country_code":"VE","region":null,"city":"Caracas","address_line":"Calle Los Mangos, Edif. Sol, piso 4, apto 4B",
         "instructions":"Portón negro","is_default":true,"archived_at":null,"created_at":"2026-08-01T12:00:00+00:00","state_code":"VE-A",
         "municipality_code":"VE-A-01","parish_code":"VE-A-01-19","longitude":-66.8836,"latitude":10.4961,"state_name":"Distrito Capital",
         "municipality_name":"Libertador","parish_name":"El Recreo"},
        {"id":"$OFICINA","user_id":"u1","label":"Oficina","country_code":"VE","region":null,"city":"Caracas","address_line":"Av. Francisco de Miranda, Torre Lido, piso 9",
         "instructions":null,"is_default":false,"archived_at":null,"created_at":"2026-08-03T12:00:00+00:00","state_code":"VE-M",
         "municipality_code":"VE-M-02","parish_code":"VE-M-02-01","longitude":-66.8530,"latitude":10.4920,"state_name":"Miranda",
         "municipality_name":"Chacao","parish_name":"Chacao"}
    ]}}"""

    val accountNoAddresses = """{"data":{"id":"u1","email":"maria.gonzalez@example.com","name":"María González","status":"active",
        "account_type":"natural","roles":["customer"],"contacts":null,"addresses":null}}"""

    private val mateoProfile = """{"id":"$MATEO","slug":"mateo-rodriguez","name":"Mateo Rodríguez",
        "bio":"Soluciones de plomería, reparación de fugas e instalaciones. Atención cuidadosa y trabajo explicado paso a paso. Llevo mis herramientas y repuestos originales.",
        "years_experience":8,"accepts_requests":true,"verified":true,"rating":4.83,"review_count":12,"recommendations":11,
        "requires_platform_payment":true,
        "offerings":[
          {"id":"$MATEO_FUGAS","category_id":"$PLOMERIA","title":"Reparación de fugas y grifería","description":"Detección y reparación de fugas, cambio de grifería y llaves de paso. Incluye diagnóstico en sitio.","delivery_mode":"onsite","pricing_type":"from","price_min_minor":1300,"price_max_minor":null,"currency":"USD"},
          {"id":"$MATEO_CALENTADOR","category_id":"$PLOMERIA","title":"Instalación de calentador de agua","description":"Instalación de calentadores eléctricos o a gas de hasta 80 litros, con prueba de presión.","delivery_mode":"onsite","pricing_type":"fixed","price_min_minor":8500000,"price_max_minor":null,"currency":"VES"}
        ],
        "branches":[
          {"id":"b1","name":"Taller Sabana Grande","city":"Caracas","address":"Av. Casanova, local 12","latitude":10.4961,"longitude":-66.8836},
          {"id":"b2","name":"Atención a domicilio","city":"Los Teques","address":null,"latitude":null,"longitude":null}
        ],
        "portfolio":[
          {"id":"p1","title":"Cambio de tubería en cocina","description":"Reemplazo de tubería galvanizada por PVC.","file_id":"aaaaaaaa-0000-4000-8000-000000000001"},
          {"id":"p2","title":"Instalación de lavamanos","description":"Montaje y sellado con grifería nueva.","file_id":"aaaaaaaa-0000-4000-8000-000000000002"},
          {"id":"p3","title":"Reparación de bajante","description":null,"file_id":"aaaaaaaa-0000-4000-8000-000000000003"}
        ]}"""

    val profile = """{"data":$mateoProfile}"""

    val reviews = """{"data":[
        {"id":"r1","rating":5,"recommends":true,"comment":"Llegó puntual, explicó todo antes de empezar y dejó la cocina impecable. Muy recomendado.","response":"¡Gracias, María! Fue un gusto ayudarte.","created_at":"2026-09-28T15:10:00+00:00"},
        {"id":"r2","rating":5,"recommends":true,"comment":"Resolvió una fuga que otros no encontraron. Precio justo.","response":null,"created_at":"2026-09-12T10:00:00+00:00"},
        {"id":"r3","rating":4,"recommends":null,"comment":"Buen trabajo, tuvo que volver al día siguiente por un repuesto.","response":null,"created_at":"2026-08-20T18:30:00+00:00"}
    ]}"""

    val favorites = """{"data":[$mateoProfile,${andreaListing.trimEnd().dropLast(1)},"branches":[],"portfolio":[],"recommendations":8}]}"""

    val requests = """{"data":[
        {"id":"$REQ_OPEN","mode":"broadcast","title":"Reparar fuga en el baño principal","status":"open","currency":"VES","created_at":"2026-09-30T14:20:00+00:00",
         "expires_at":"2027-01-07T14:20:00+00:00","category_id":"$PLOMERIA","customer_id":"u1","description":"Hay una fuga constante debajo del lavamanos y la pared empezó a mancharse. Necesito revisar la tubería y cambiar la llave de paso.",
         "offering_id":null,"scheduled_at":null,"delivery_mode":"onsite","requires_quote":true,"idempotency_key":"38dc8fb7-caba-4458-912a-d9437b558ab9",
         "search_radius_m":25000,"budget_max_minor":4500000,"budget_min_minor":null,"moderation_status":"approved","offering_snapshot":null,
         "offered_price_minor":null,"target_professional_id":null,"requires_platform_payment":true,"contract_id":null,
         "attachments":[{"file_id":"bbbbbbbb-0000-4000-8000-000000000001","mime_type":"image/jpeg","moderation_status":"approved"},
                        {"file_id":"bbbbbbbb-0000-4000-8000-000000000002","mime_type":"image/jpeg","moderation_status":"approved"}]},
        {"id":"$REQ_REMOTE","mode":"direct","title":"Identidad visual y logotipos","status":"open","currency":"USD","created_at":"2026-09-29T00:14:48+00:00",
         "expires_at":"2027-01-06T00:14:07+00:00","category_id":"$DISENO","customer_id":"u1","description":"Logo y paleta de colores para mi panadería artesanal en Mérida.",
         "offering_id":"fc84565b-869d-4c13-9c5e-b5418a9054f2","scheduled_at":null,"delivery_mode":"remote","requires_quote":true,"idempotency_key":"5464c1ee-bfed-4fb4-a8cf-b4576011d670",
         "search_radius_m":null,"budget_max_minor":null,"budget_min_minor":null,"moderation_status":"approved",
         "offering_snapshot":{"id":"fc84565b-869d-4c13-9c5e-b5418a9054f2","title":"Identidad visual y logotipos","currency":"USD","category_id":"$DISENO","delivery_mode":"remote","price_min_minor":2500,"professional_id":"$ANDREA"},
         "offered_price_minor":null,"target_professional_id":"$ANDREA","requires_platform_payment":true,"contract_id":null,"attachments":[]},
        {"id":"$REQ_MATCHED","mode":"direct","title":"Instalaciones eléctricas residenciales","status":"matched","currency":"VES","created_at":"2026-09-20T09:00:00+00:00",
         "expires_at":"2026-09-27T09:00:00+00:00","category_id":"$ELECTRICIDAD","customer_id":"u1","description":"Instalar tres lámparas colgantes en la sala y revisar un tomacorriente.",
         "offering_id":"9a8b7c6d-5e4f-4a3b-2c1d-0e9f8a7b6c5d","scheduled_at":null,"delivery_mode":"onsite","requires_quote":true,"idempotency_key":"6464c1ee-bfed-4fb4-a8cf-b4576011d670",
         "search_radius_m":200000,"budget_max_minor":null,"budget_min_minor":null,"moderation_status":"approved","offering_snapshot":null,
         "offered_price_minor":null,"target_professional_id":"$VALENTINA","requires_platform_payment":false,"contract_id":"c0ffee00-0000-4000-8000-000000000001","attachments":[]},
        {"id":"$REQ_DRAFT","mode":"broadcast","title":"Pintar habitación de 12 m²","status":"draft","currency":"USD","created_at":"2026-09-18T19:00:00+00:00",
         "expires_at":"2027-01-01T19:00:00+00:00","category_id":"52c5d4eb-21a2-4072-b3a5-80c49d11616a","customer_id":"u1","description":"Pintar paredes y techo de una habitación, color blanco hueso.",
         "offering_id":null,"scheduled_at":null,"delivery_mode":"onsite","requires_quote":true,"idempotency_key":"7464c1ee-bfed-4fb4-a8cf-b4576011d670",
         "search_radius_m":10000,"budget_max_minor":12000,"budget_min_minor":null,"moderation_status":"pending","offering_snapshot":null,
         "offered_price_minor":null,"target_professional_id":null,"requires_platform_payment":false,"contract_id":null,"attachments":[]},
        {"id":"$REQ_CANCELLED","mode":"broadcast","title":"Mantenimiento de aire acondicionado split","status":"cancelled","currency":"VES","created_at":"2026-09-02T11:00:00+00:00",
         "expires_at":"2026-09-09T11:00:00+00:00","category_id":"3a646026-bc9d-4c38-b92b-dec9bbc460d7","customer_id":"u1","description":"Limpieza y carga de gas de un split de 12.000 BTU.",
         "offering_id":null,"scheduled_at":null,"delivery_mode":"onsite","requires_quote":true,"idempotency_key":"8464c1ee-bfed-4fb4-a8cf-b4576011d670",
         "search_radius_m":10000,"budget_max_minor":null,"budget_min_minor":null,"moderation_status":"approved","offering_snapshot":null,
         "offered_price_minor":null,"target_professional_id":null,"requires_platform_payment":false,"contract_id":null,"attachments":[]}
    ]}"""

    private val carlosProfile = """{"id":"$CARLOS","slug":"carlos-mendez","name":"Carlos Méndez","bio":"Plomero con 15 años de experiencia.",
        "years_experience":15,"accepts_requests":true,"verified":false,"rating":4.2,"review_count":5,"recommendations":4,"requires_platform_payment":false,
        "offerings":[],"branches":[],"portfolio":[]}"""

    val proposals = """{"data":[
        {"id":"9f1e2d3c-0000-4000-8000-000000000001","professional":$mateoProfile,"message":"Hola, María. Puedo ir mañana a las 9:00 a. m. con los repuestos. Incluye cambio de la llave de paso y prueba de presión.",
         "price_minor":3800000,"currency":"VES","status":"pending","expires_at":"2027-01-07T14:20:00+00:00"},
        {"id":"9f1e2d3c-0000-4000-8000-000000000002","professional":$carlosProfile,"message":"Reviso la tubería y te doy el diagnóstico en sitio. El precio incluye materiales básicos.",
         "price_minor":4200000,"currency":"VES","status":"pending","expires_at":"2027-01-05T10:00:00+00:00"}
    ]}"""

    private fun fixture(name: String): ByteArray? {
        val relative = "com/soyvinko/app/ui/feature/explore/fixtures/$name.jpg"
        return listOf("src/test/java/$relative", "app/src/test/java/$relative").map(::File).firstOrNull { it.exists() }?.readBytes()
    }

    /** Sirve las ilustraciones de categorías y las fotos privadas como imágenes reales para capturas más fieles. */
    fun serveImages(api: FakeApi) {
        val original = api.server.dispatcher
        api.server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path.orEmpty().substringBefore('?')
                val name = when {
                    path.startsWith("/images/services/") -> path.removePrefix("/images/services/").removeSuffix("-v1.webp")
                    path.startsWith("/api/v1/files/") -> "photo" + (Math.floorMod(path.last().code, 3) + 1)
                    else -> null
                }
                val fallback = original.dispatch(request) // también lo registra en api.requests
                val bytes = name?.let { fixture(it) }
                return if (bytes != null) MockResponse().setHeader("Content-Type", "image/jpeg").setBody(Buffer().write(bytes)) else fallback
            }
        }
    }
}
