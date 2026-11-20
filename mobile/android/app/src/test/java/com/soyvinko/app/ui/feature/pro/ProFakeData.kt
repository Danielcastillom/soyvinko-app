package com.soyvinko.app.ui.feature.pro

import com.soyvinko.app.ui.FakeApi
import java.time.Instant
import java.time.temporal.ChronoUnit

/** Datos ficticios realistas para las capturas de la sección profesional y el marketplace. */
object ProFakeData {
    private fun inDays(days: Long) = Instant.now().plus(days, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString()
    private fun ago(hours: Long) = Instant.now().minus(hours, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS).toString()

    val catalog = """[
      {"id":"cat-elec","slug":"electricidad","name":"Electricidad","active":true},
      {"id":"cat-ac","slug":"refrigeracion","name":"Refrigeración y aire acondicionado","active":true},
      {"id":"cat-it","slug":"informatica","name":"Reparación y soporte de computadores","active":true},
      {"id":"cat-plo","slug":"plomeria","name":"Plomería","active":true}
    ]"""

    fun professional(store: String = "null") = """{
      "user_id":"u1","slug":"maria-gonzalez-electricidad","business_name":"María González Electricidad",
      "bio":"Electricista certificada con 12 años de experiencia en instalaciones residenciales y comerciales en Caracas.",
      "years_experience":12,"status":"active","accepts_requests":true,"plan_code":"PRO-PROMO",
      "branches":[{"id":"b1","name":"Taller Chacao","address_line":"Av. Francisco de Miranda, Edif. Centro Plaza, local 4",
        "city":"Chacao","public_address":true,"active":true,"state_name":"Miranda","municipality_name":"Chacao",
        "parish_name":"Chacao","latitude":10.4961,"longitude":-66.8531}],
      "contacts":[
        {"id":"c1","branch_id":"b1","kind":"whatsapp","label":"WhatsApp de atención","value":"0414 555 1234","verified_at":null},
        {"id":"c2","branch_id":null,"kind":"email","label":"Correo de presupuestos","value":"maria@electricidadmg.com","verified_at":null}],
      "coverage":[
        {"id":"cv1","branch_id":"b1","category_id":"cat-elec","radius_m":15000,"active":true},
        {"id":"cv2","branch_id":"b1","category_id":"cat-ac","radius_m":10000,"active":true}],
      "offerings":[
        {"id":"o1","category_id":"cat-elec","title":"Instalación y reparación eléctrica residencial",
         "description":"Diagnóstico de fallas, cableado, tomacorrientes, breakers y tableros. Incluye garantía de 30 días.",
         "pricing_type":"from","price_min_minor":2500,"currency":"USD","active":true,"delivery_mode":"onsite","created_at":"2026-09-01T10:00:00Z"},
        {"id":"o2","category_id":"cat-ac","title":"Mantenimiento de aires acondicionados split",
         "description":"Limpieza profunda, carga de gas y revisión del compresor.",
         "pricing_type":"from","price_min_minor":180000,"currency":"VES","active":true,"delivery_mode":"onsite","created_at":"2026-09-05T10:00:00Z"},
        {"id":"o3","category_id":"cat-it","title":"Asesoría remota de domótica",
         "description":"Planificación de iluminación inteligente y automatización del hogar.",
         "pricing_type":"quote","price_min_minor":null,"currency":"USD","active":false,"delivery_mode":"remote","created_at":"2026-09-10T10:00:00Z"}],
      "store":$store
    }"""

    const val store = """{"id":"s1","professional_id":"u1","slug":"ferreteria-el-valle","name":"Ferretería El Valle",
      "description":"Herramientas, material eléctrico y repuestos con entrega en Caracas.","status":"active","plan_code":"STORE-PROMO"}"""

    const val profile = """{"id":"u1","slug":"maria-gonzalez-electricidad","name":"María González Electricidad",
      "verified":false,"rating":4.8,"review_count":12,"recommendations":11,"offerings":[],"branches":[],"portfolio":[]}"""

    const val wallet = """{"balance":7,"credits":7,"ledger":[],"rates":[]}"""

    fun reviews() = """[
      {"id":"r1","rating":5,"recommends":true,"comment":"Excelente trabajo, puntual y muy ordenada. Resolvió la falla del tablero en una hora.",
       "response":"¡Gracias, Carlos! Fue un gusto ayudarte.","created_at":"${ago(30)}"},
      {"id":"r2","rating":4,"recommends":true,"comment":"Buen servicio, explicó todo con claridad y dejó todo limpio.",
       "response":null,"created_at":"${ago(200)}"}
    ]"""

    const val assets = """{
      "portfolio":[
        {"id":"pf1","title":"Tablero trifásico para panadería","description":"Reemplazo completo y balanceo de cargas.","file_id":"f-pf1","created_at":"2026-08-10T10:00:00Z"},
        {"id":"pf2","title":"Iluminación LED de fachada","description":"Instalación de 24 reflectores.","file_id":"f-pf2","created_at":"2026-07-02T10:00:00Z"}],
      "credentials":[
        {"id":"cr1","title":"Técnico Superior en Electricidad","issuer":"IUT Región Capital","issued_on":"2014-07-15","expires_on":null,"verified_at":"2026-06-01T10:00:00Z"}],
      "hours":[
        {"id":"h1","branch_id":"b1","weekday":1,"opens_at":"08:00:00","closes_at":"17:00:00"},
        {"id":"h2","branch_id":"b1","weekday":2,"opens_at":"08:00:00","closes_at":"17:00:00"},
        {"id":"h3","branch_id":"b1","weekday":3,"opens_at":"08:00:00","closes_at":"17:00:00"},
        {"id":"h4","branch_id":"b1","weekday":4,"opens_at":"08:00:00","closes_at":"17:00:00"},
        {"id":"h5","branch_id":"b1","weekday":5,"opens_at":"08:00:00","closes_at":"17:00:00"},
        {"id":"h6","branch_id":"b1","weekday":6,"opens_at":"09:00:00","closes_at":"13:00:00"}]
    }"""

    fun feed() = """[
      {"request_id":"rq1","category_id":"cat-elec","title":"Falla eléctrica en apartamento",
       "description":"Se disparan los breakers al encender el aire acondicionado. Necesito revisión del tablero y del cableado de la habitación principal.",
       "delivery_mode":"onsite","mode":"broadcast","requires_quote":false,"offering_id":null,"currency":"USD","offered_price_minor":null,
       "budget_min_minor":null,"budget_max_minor":6000,"expires_at":"${inDays(9)}","requires_platform_payment":false,
       "attachments":[{"file_id":"f1","mime_type":"image/jpeg"},{"file_id":"f2","mime_type":"image/jpeg"}]},
      {"request_id":"rq2","category_id":"cat-ac","title":"Mantenimiento de 2 aires split",
       "description":"Dos equipos de 12.000 BTU que no enfrían bien. Edificio con estacionamiento para visitantes.",
       "delivery_mode":"onsite","mode":"direct","requires_quote":false,"offering_id":"o2","currency":"VES","offered_price_minor":360000,
       "budget_min_minor":null,"budget_max_minor":null,"expires_at":"${inDays(4)}","requires_platform_payment":true,"attachments":[]},
      {"request_id":"rq3","category_id":"cat-elec","title":"Instalación de lámparas LED en local comercial",
       "description":"Local de 80 m² en Los Palos Grandes. Son 16 paneles LED empotrados; el material ya está comprado.",
       "delivery_mode":"onsite","mode":"broadcast","requires_quote":false,"offering_id":null,"currency":"VES","offered_price_minor":null,
       "budget_min_minor":null,"budget_max_minor":null,"expires_at":"${inDays(12)}","requires_platform_payment":false,"attachments":[]}
    ]"""

    fun proposals() = """[
      {"id":"p1","request_id":"rq3","title":"Instalación de lámparas LED en local comercial","status":"pending",
       "message":"Puedo instalar los 16 paneles en una jornada. Incluye revisión del circuito y canaletas.",
       "price_minor":4500000,"currency":"VES","expires_at":"${inDays(6)}","created_at":"${ago(3)}"},
      {"id":"p2","request_id":"rq8","title":"Cambio de tablero eléctrico en casa","status":"accepted",
       "message":"Reemplazo del tablero con breakers nuevos y certificación de la instalación.",
       "price_minor":18000,"currency":"USD","expires_at":"${inDays(2)}","created_at":"${ago(50)}"},
      {"id":"p3","request_id":"rq9","title":"Revisión de cortocircuito en oficina","status":"rejected",
       "message":"Diagnóstico con multímetro y termografía para ubicar el punto caliente.",
       "price_minor":4000,"currency":"USD","expires_at":"${inDays(1)}","created_at":"${ago(120)}"}
    ]"""

    const val marketplace = """[
      {"id":"pr1","store_id":"s1","store_name":"Ferretería El Valle","professional_id":"u9","sku":"TAL-650","name":"Taladro percutor 650 W",
       "description":"Taladro percutor con maletín y juego de 13 brocas. Garantía de 6 meses.","price_minor":5800,"currency":"USD","stock":8,"status":"active","images":["img-pr1"]},
      {"id":"pr2","store_id":"s1","store_name":"Ferretería El Valle","professional_id":"u9","sku":"CAB-THW12","name":"Cable THW calibre 12 (rollo 100 m)",
       "description":"Cable de cobre para instalaciones residenciales.","price_minor":7200,"currency":"USD","stock":2,"status":"active","images":null},
      {"id":"pr3","store_id":"s2","store_name":"Hogar Express","professional_id":"u8","sku":"KIT-LMP","name":"Kit de limpieza para el hogar",
       "description":"Desinfectante, lavaplatos, cloro y paños de microfibra.","price_minor":95000,"currency":"VES","stock":15,"status":"active","images":["img-pr3"]},
      {"id":"pr4","store_id":"s3","store_name":"TecnoCaracas","professional_id":"u7","sku":"SSD-480","name":"Disco SSD 480 GB",
       "description":"Unidad de estado sólido SATA III para computadores.","price_minor":3900,"currency":"USD","stock":0,"status":"active","images":null},
      {"id":"pr5","store_id":"s1","store_name":"Ferretería El Valle","professional_id":"u9","sku":"LLV-10","name":"Llave ajustable 10 pulgadas",
       "description":"Acero cromo vanadio con mango antideslizante.","price_minor":1250,"currency":"USD","stock":20,"status":"active","images":["img-pr5","img-pr5b"]},
      {"id":"pr6","store_id":"s2","store_name":"Hogar Express","professional_id":"u8","sku":"FIL-NEV","name":"Filtro de agua para nevera",
       "description":"Compatible con neveras Samsung y LG de dos puertas.","price_minor":145000,"currency":"VES","stock":5,"status":"active","images":null}
    ]"""

    const val storeProducts = """[
      {"id":"pr2","store_id":"s1","store_name":"Ferretería El Valle","professional_id":"u9","sku":"CAB-THW12","name":"Cable THW calibre 12 (rollo 100 m)","description":"Cable de cobre para instalaciones residenciales.","price_minor":7200,"currency":"USD","stock":2,"status":"active","images":[]},
      {"id":"pr5","store_id":"s1","store_name":"Ferretería El Valle","professional_id":"u9","sku":"LLV-10","name":"Llave ajustable 10 pulgadas","description":"Acero cromo vanadio con mango antideslizante.","price_minor":1250,"currency":"USD","stock":20,"status":"active","images":["img-pr5","img-pr5b"]},
      {"id":"pr1","store_id":"s1","store_name":"Ferretería El Valle","professional_id":"u9","sku":"TAL-650","name":"Taladro percutor 650 W","description":"Taladro percutor con maletín y juego de 13 brocas.","price_minor":5800,"currency":"USD","stock":8,"status":"active","images":["img-pr1"]}
    ]"""

    fun orders() = """[
      {"id":"a1b2c3d4-0000-4000-8000-000000000001","customer_id":"u1","store_id":"s1","status":"accepted","total_minor":13000,"currency":"USD",
       "payment_status":"not_reported","platform_fee_minor":0,"created_at":"${ago(5)}",
       "items":[{"id":"i1","product_id":"pr1","name_snapshot":"Taladro percutor 650 W","sku_snapshot":"TAL-650","quantity":1,"unit_price_minor":5800,"subtotal_minor":5800},
                {"id":"i2","product_id":"pr2","name_snapshot":"Cable THW calibre 12 (rollo 100 m)","sku_snapshot":"CAB-THW12","quantity":1,"unit_price_minor":7200,"subtotal_minor":7200}]},
      {"id":"e5f6a7b8-0000-4000-8000-000000000002","customer_id":"u7","store_id":"s1","status":"pending","total_minor":450000,"currency":"VES",
       "payment_status":"not_reported","platform_fee_minor":0,"created_at":"${ago(1)}",
       "items":[{"id":"i3","product_id":"mp1","name_snapshot":"Bombillo LED 12 W luz blanca","sku_snapshot":"BOM-LED12","quantity":10,"unit_price_minor":45000,"subtotal_minor":450000}]},
      {"id":"c9d0e1f2-0000-4000-8000-000000000003","customer_id":"u1","store_id":"s2","status":"delivered","total_minor":95000,"currency":"VES",
       "payment_status":"seller_confirmed","platform_fee_minor":0,"created_at":"${ago(240)}",
       "items":[{"id":"i4","product_id":"pr3","name_snapshot":"Kit de limpieza para el hogar","sku_snapshot":"KIT-LMP","quantity":1,"unit_price_minor":95000,"subtotal_minor":95000}]}
    ]"""

    const val myProducts = """[
      {"id":"mp1","store_id":"s1","store_name":"Ferretería El Valle","sku":"BOM-LED12","name":"Bombillo LED 12 W luz blanca","description":"Rosca E27, 1.100 lúmenes.","price_minor":45000,"currency":"VES","stock":120,"status":"active",
       "images":[{"file_id":"img-mp1","moderation_status":"approved"},{"file_id":"img-mp1b","moderation_status":"pending"}]},
      {"id":"mp2","store_id":"s1","store_name":"Ferretería El Valle","sku":"BRK-20A","name":"Breaker 20 A enchufable","description":"Para tableros residenciales.","price_minor":850,"currency":"USD","stock":3,"status":"active",
       "images":[{"file_id":"img-mp2","moderation_status":"approved"}]},
      {"id":"mp3","store_id":"s1","store_name":"Ferretería El Valle","sku":"TOM-DBL","name":"Tomacorriente doble con tierra","description":"Color blanco, 15 A.","price_minor":35000,"currency":"VES","stock":0,"status":"draft","images":[]}
    ]"""

    /** Ids de archivo que el servidor simulado responde con una imagen PNG. */
    val imageIds = listOf("img-pr1", "img-pr3", "img-pr5", "img-pr5b", "img-mp1", "img-mp1b", "img-mp2", "f-pf1", "f-pf2", "f1", "f2")

    fun install(api: FakeApi) {
        val r = api.responses
        r["/api/v1/public/catalog"] = data(catalog)
        r["/api/v1/professionals/my-professional"] = data(professional())
        r["/api/v1/professionals/professional-profile"] = data(profile)
        r["/api/v1/wallet/my-wallet"] = data(wallet)
        r["/api/v1/professionals/professional-reviews"] = data(reviews())
        r["/api/v1/professionals/professional-assets"] = data(assets)
        r["/api/v1/requests/service-feed"] = data(feed())
        r["/api/v1/requests/my-proposals"] = data(proposals())
        r["/api/v1/public/marketplace"] = data(marketplace)
        r["/api/v1/marketplace/store-products"] = data(storeProducts)
        r["/api/v1/marketplace/my-orders"] = data(orders())
        r["/api/v1/marketplace/my-products"] = data(myProducts)
    }

    fun data(json: String) = """{"data":$json}"""
}
