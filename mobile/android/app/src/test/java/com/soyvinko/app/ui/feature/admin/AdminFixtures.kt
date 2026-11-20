package com.soyvinko.app.ui.feature.admin

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.Base64
import com.soyvinko.app.ui.TestUser
import java.io.ByteArrayOutputStream
import java.time.OffsetDateTime

/** Datos ficticios del panel de administración (formas reales de api.admin_list y compañía). */
internal object AdminFixtures {
    val ALL_PERMISSIONS = listOf(
        "catalog.manage", "content.moderate", "credits.refund", "kyc.review", "money.configure", "money.refund",
        "plans.manage", "platform.read", "roles.manage", "support.manage", "support.read", "users.manage",
    )
    val superadmin = TestUser.copy(id = "a0000000-0000-4000-8000-000000000001", name = "Daniela Rivas", email = "daniela@soyvinko.com",
        roles = listOf("superadmin"), permissions = ALL_PERMISSIONS)
    val support = TestUser.copy(id = "a0000000-0000-4000-8000-000000000002", name = "Luis Medina", email = "luis.soporte@soyvinko.com",
        roles = listOf("support"), permissions = listOf("support.manage", "support.read"))

    fun ago(minutes: Long): String = OffsetDateTime.now().minusMinutes(minutes).toString()
    private fun days(d: Long) = ago(60L * 24 * d)

    fun list(resource: String) = "/api/v1/admin/admin-list?resource=$resource&limit=25&offset=0"

    val overview = """{"data":{"users":1284,"professionals":312,"contracts":486,"active_contracts":37,"pending_requests":6,
        "pending_kyc":4,"open_tickets":9,"stores":58,"active_stores":51,"products":742,"orders":1093,
        "marketplace_sales_minor":4825000,"contracts_volume_minor":19870050}}"""

    const val USER_ANA = "5f1d2c3b-1111-4a5b-9c8d-000000000001"
    val users = """{"data":[
      {"id":"$USER_ANA","email":"ana.morales@gmail.com","display_name":"Ana Morales","status":"active","locale":"es-VE","timezone":"America/Caracas",
       "email_verified_at":"${days(120)}","created_at":"${days(121)}","account_type":"natural","business_name":null},
      {"id":"5f1d2c3b-1111-4a5b-9c8d-000000000002","email":"administracion@ferreteriaelavila.com","display_name":"Ferretería El Ávila C.A.","status":"active",
       "email_verified_at":"${days(60)}","created_at":"${days(61)}","account_type":"juridico","business_name":"Ferretería El Ávila C.A."},
      {"id":"5f1d2c3b-1111-4a5b-9c8d-000000000003","email":"jose.perez@hotmail.com","display_name":"José Pérez","status":"suspended",
       "email_verified_at":null,"created_at":"${days(30)}","account_type":"natural"},
      {"id":"5f1d2c3b-1111-4a5b-9c8d-000000000004","email":"carla.mendez@gmail.com","display_name":"Carla Méndez","status":"active",
       "email_verified_at":"${days(5)}","created_at":"${days(6)}","account_type":"natural"},
      {"id":"${superadmin.id}","email":"daniela@soyvinko.com","display_name":"Daniela Rivas","status":"active",
       "email_verified_at":"${days(400)}","created_at":"${days(400)}","account_type":"natural"}]}"""

    const val CASE_1 = "c1a2b3c4-2222-4d5e-8f90-000000000001"
    const val DOC_FRONT = "f0000000-0000-4000-8000-0000000000a1"
    private val meta = """{\"id\":\"27d2e7ff-458a-4c1e-9d0a-1f2b3c4d5e6f\",\"document_type\":\"V\",\"document_number\":\"18456789\",\"full_name\":\"MARÍA ALEJANDRA GONZÁLEZ PÉREZ\",\"birthdate\":\"1990-05-21\",\"phone\":\"0414-5551234\",\"address\":\"Av. Francisco de Miranda, Edif. Parque Cristal, piso 8, Caracas\",\"account_type\":\"natural\",\"expiration_date\":\"2031-03-14\",\"is_expired\":false,\"ocr_success\":true}"""
    val kycList = """{"data":[
      {"id":"$CASE_1","user_id":"$USER_ANA","provider":"personal_ve","external_reference":"$meta","status":"pending",
       "submitted_at":"${ago(95)}","reviewed_at":null,"reviewed_by":null,"expires_at":null,"reason":null},
      {"id":"c1a2b3c4-2222-4d5e-8f90-000000000002","user_id":"u2","provider":"personal_ve","external_reference":null,"status":"in_review",
       "submitted_at":"${days(1)}","reviewed_at":null,"expires_at":null,"reason":null},
      {"id":"c1a2b3c4-2222-4d5e-8f90-000000000003","user_id":"u3","provider":"manual","status":"approved",
       "submitted_at":"${days(40)}","reviewed_at":"${days(39)}","expires_at":"${OffsetDateTime.now().plusYears(1)}","reason":"Documento legible y selfie coincide."},
      {"id":"c1a2b3c4-2222-4d5e-8f90-000000000004","user_id":"u4","provider":"personal_ve","status":"rejected",
       "submitted_at":"${days(12)}","reviewed_at":"${days(11)}","expires_at":null,"reason":"La foto de la cédula está borrosa."}]}"""

    val kycCase = """{"data":{"id":"$CASE_1","user_id":"$USER_ANA","provider":"personal_ve","external_reference":"$meta","status":"pending",
       "submitted_at":"${ago(95)}","reviewed_at":null,"expires_at":null,"reason":null,
       "documents":[{"kind":"identity_front","file_id":"$DOC_FRONT","object_key":"k1"},
                    {"kind":"identity_back","file_id":"f0000000-0000-4000-8000-0000000000a2","object_key":"k2"},
                    {"kind":"selfie","file_id":"f0000000-0000-4000-8000-0000000000a3","object_key":"k3"}]}}"""

    /** Imagen de una cédula ficticia en base64, como la devuelve /api/v1/files/moderation/{id}. */
    fun idCardBase64(): String {
        val bmp = Bitmap.createBitmap(640, 404, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(222, 232, 246))
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = Color.rgb(250, 214, 64); c.drawRect(0f, 0f, 640f, 18f, p)
        p.color = Color.rgb(36, 84, 170); c.drawRect(0f, 18f, 640f, 36f, p)
        p.color = Color.rgb(206, 38, 52); c.drawRect(0f, 36f, 640f, 54f, p)
        p.color = Color.rgb(24, 17, 44); p.textSize = 22f; p.isFakeBoldText = true
        c.drawText("REPÚBLICA BOLIVARIANA DE VENEZUELA", 40f, 92f, p)
        p.textSize = 18f; c.drawText("CÉDULA DE IDENTIDAD", 40f, 120f, p)
        p.color = Color.rgb(180, 188, 204); c.drawRoundRect(RectF(40f, 150f, 210f, 360f), 12f, 12f, p)
        p.color = Color.rgb(120, 128, 150); c.drawCircle(125f, 220f, 42f, p); c.drawRoundRect(RectF(65f, 270f, 185f, 360f), 50f, 50f, p)
        p.color = Color.rgb(24, 17, 44); p.textSize = 30f
        c.drawText("V 18.456.789", 240f, 190f, p)
        p.textSize = 20f; p.isFakeBoldText = false
        c.drawText("GONZÁLEZ PÉREZ", 240f, 240f, p)
        c.drawText("MARÍA ALEJANDRA", 240f, 270f, p)
        c.drawText("F. NAC. 21/05/1990", 240f, 310f, p)
        c.drawText("VENCE 14/03/2031", 240f, 340f, p)
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    const val W1 = "d0000000-3333-4000-8000-000000000001"
    val withdrawals = """{"data":[
      {"id":"$W1","wallet_id":"w1","amount_minor":1250000,"fee_minor":31250,"net_minor":1218750,"destination_provider":"sandbox",
       "destination_label":"Banesco · ****4821","status":"pending","created_at":"${ago(45)}","currency":"VES","owner_name":"Carlos Pérez"},
      {"id":"d0000000-3333-4000-8000-000000000002","wallet_id":"w2","amount_minor":15000,"fee_minor":375,"net_minor":14625,"destination_provider":"sandbox",
       "destination_label":"Zelle · an***@gmail.com","status":"processing","created_at":"${ago(300)}","currency":"USD","owner_name":"Ana Morales"},
      {"id":"d0000000-3333-4000-8000-000000000003","wallet_id":"w3","amount_minor":480000,"fee_minor":12000,"net_minor":468000,
       "destination_label":"Banco de Venezuela · ****1180","status":"paid","created_at":"${days(3)}","currency":"VES","owner_name":"Luisa Fernández"},
      {"id":"d0000000-3333-4000-8000-000000000004","wallet_id":"w4","amount_minor":90000,"fee_minor":2250,"net_minor":87750,
       "destination_label":"Mercantil · ****0098","status":"failed","created_at":"${days(6)}","currency":"VES","owner_name":"Pedro Salas"}]}"""

    const val T1 = "e0000000-4444-4000-8000-000000000001"
    const val CONTRACT = "b0000000-5555-4000-8000-000000000001"
    val tickets = """{"data":[
      {"id":"$T1","opened_by":"$USER_ANA","assigned_to":null,"contract_id":"$CONTRACT","order_id":null,"category":"service",
       "subject":"El plomero no se presentó","description":"Pagué la reserva del servicio y el profesional no llegó en el horario acordado.",
       "status":"open","priority":"high","created_at":"${ago(130)}","resolved_at":null},
      {"id":"e0000000-4444-4000-8000-000000000002","opened_by":"u3","assigned_to":"${support.id}","category":"credits",
       "subject":"No veo los créditos de mi plan","description":"Compré el plan Crece y aún no aparecen.","status":"in_progress","priority":"normal","created_at":"${days(1)}"},
      {"id":"e0000000-4444-4000-8000-000000000003","opened_by":"u4","category":"marketplace","subject":"Pedido incompleto",
       "description":"Llegaron 2 de 3 productos.","status":"waiting_user","priority":"low","created_at":"${days(2)}","order_id":"o1"},
      {"id":"e0000000-4444-4000-8000-000000000004","opened_by":"u5","category":"account","subject":"Cambio de correo",
       "description":"Necesito actualizar mi correo de acceso.","status":"resolved","priority":"normal","created_at":"${days(9)}","resolved_at":"${days(8)}"}]}"""

    val ticketThread = """{"data":{"id":"$T1","opened_by":"$USER_ANA","assigned_to":null,"contract_id":"$CONTRACT","order_id":null,"category":"service",
       "subject":"El plomero no se presentó","description":"Pagué la reserva del servicio y el profesional no llegó en el horario acordado. Necesito que me devuelvan el dinero.",
       "status":"open","priority":"high","created_at":"${ago(130)}","resolved_at":null,
       "messages":[
         {"id":"m1","ticket_id":"$T1","author_id":"$USER_ANA","body":"Lo esperé toda la mañana y no responde el chat.","internal_only":false,"created_at":"${ago(125)}"},
         {"id":"m2","ticket_id":"$T1","author_id":"${support.id}","body":"Revisado el seguimiento: el profesional nunca salió en camino.","internal_only":true,"created_at":"${ago(90)}"},
         {"id":"m3","ticket_id":"$T1","author_id":"${support.id}","body":"Hola Ana, ya estamos revisando tu caso con el profesional. Te escribimos en breve.","internal_only":false,"created_at":"${ago(85)}"}]}}"""

    val plans = """{"data":[
      {"id":"p1","code":"PRO-BASE","name":"Modalidad Base Sin Suscripción (Desde Mes 4)","affiliate_type":"provider","price_minor":0,"currency":"USD","credits":5,
       "fee_summary":"8% Presencial / 12% Remoto","max_products":0,"positioning_weight":0,"active":true},
      {"id":"p2","code":"PRO-PROMO","name":"Plan Promocional Entrada (Meses 1 al 3)","affiliate_type":"provider","price_minor":0,"currency":"USD","credits":15,
       "fee_summary":"0% (Primer mes) / 3% (Meses 2 y 3)","max_products":0,"positioning_weight":10,"active":true},
      {"id":"p3","code":"PRO-MEMBERSHIP-15","name":"Membresía Profesional (Desde Mes 4)","affiliate_type":"provider","price_minor":1500,"currency":"USD","credits":30,
       "fee_summary":"Tasa Mínima: 3% Presencial / 6% Remoto","max_products":0,"positioning_weight":30,"active":true},
      {"id":"p4","code":"STORE-STANDARD-30","name":"Membresía Comercio Estándar (Desde Mes 4)","affiliate_type":"store","price_minor":3000,"currency":"USD","credits":0,
       "fee_summary":"Tasa Mínima 4%","max_products":30,"positioning_weight":25,"active":true},
      {"id":"p5","code":"DEMO-CRECE-VE-VES","name":"Crece · demostración Bs","affiliate_type":"provider","price_minor":100000,"currency":"VES","credits":15,
       "fee_summary":null,"max_products":0,"positioning_weight":10,"active":false}]}"""

    val fees = """{"data":[
      {"id":"fe1","currency":"VES","basis_points":250,"active":true,"created_by":"a1","reason":"Ajuste por costos bancarios de transferencias","created_at":"${days(20)}"},
      {"id":"fe2","currency":"USD","basis_points":300,"active":true,"created_by":"a1","reason":"Tarifa Zelle y procesamiento","created_at":"${days(20)}"},
      {"id":"fe3","currency":"VES","basis_points":200,"active":false,"created_by":"a1","reason":"Tarifa de lanzamiento","created_at":"${days(90)}"}]}"""

    val vinkoRates = """{"data":[
      {"id":"r1","currency":"VES","rate_minor":5250,"active":true,"status":"active","reason":"Ajuste según tasa oficial BCV","created_at":"${days(2)}","creator_name":"Daniela Rivas"},
      {"id":"r2","currency":"USD","rate_minor":100,"active":true,"status":"active","reason":"Paridad de lanzamiento","created_at":"${days(60)}","creator_name":"Administración"},
      {"id":"r3","currency":"VES","rate_minor":5000,"active":false,"status":"archived","reason":"Tasa inicial","created_at":"${days(60)}","creator_name":"Administración"}]}"""

    val contracts = """{"data":[
      {"id":"b0000000-5555-4000-8000-000000000001","request_title":"Reparación de fuga en baño principal","category_name":"Plomería",
       "customer_name":"Ana Morales","customer_email":"ana.morales@gmail.com","professional_id":"pr1","professional_name":"Carlos Pérez","professional_email":"carlos@gmail.com",
       "status":"disputed","delivery_mode":"onsite","agreed_price_minor":4500000,"currency":"VES","payment_protection":"guaranteed","plan_code":"PRO-MEMBERSHIP-15",
       "accepted_at":"${days(2)}"},
      {"id":"b0000000-5555-4000-8000-000000000002","request_title":"Diseño de logo para panadería","category_name":"Diseño gráfico",
       "customer_name":"José Pérez","professional_id":"pr2","professional_name":"Valentina Rojas","status":"in_progress","delivery_mode":"remote",
       "agreed_price_minor":12000,"currency":"USD","payment_protection":"guaranteed","plan_code":"PRO-BASE","accepted_at":"${days(4)}"}]}"""

    val stores = """{"data":[
      {"id":"s1","professional_id":"pr1","owner_name":"Carlos Pérez","owner_email":"carlos@gmail.com","slug":"ferreteria-el-avila","name":"Ferretería El Ávila",
       "description":"Herramientas y materiales de construcción.","status":"active","plan_code":"STORE-STANDARD-30","created_at":"${days(80)}","products_count":28,"orders_count":143},
      {"id":"s2","professional_id":"pr3","owner_name":"Luisa Fernández","slug":"dulces-luisa","name":"Dulcería Luisa","status":"suspended","plan_code":"STORE-PROMO",
       "created_at":"${days(20)}","products_count":9,"orders_count":12}]}"""

    val orders = """{"data":[
      {"id":"0a1b2c3d-6666-4000-8000-000000000001","customer_name":"Ana Morales","customer_email":"ana.morales@gmail.com","store_name":"Ferretería El Ávila",
       "seller_name":"Carlos Pérez","seller_email":"carlos@gmail.com","status":"disputed","total_minor":850000,"currency":"VES","payment_method":"pago_movil",
       "payment_status":"buyer_reported","created_at":"${days(1)}","items_count":3,"delivery_contact":"Ana Morales (0414-5551234)"},
      {"id":"0a1b2c3d-6666-4000-8000-000000000002","customer_name":"José Pérez","store_name":"Dulcería Luisa","status":"delivered","total_minor":2400,"currency":"USD",
       "payment_status":"seller_confirmed","created_at":"${days(5)}","items_count":1}]}"""
}
