package com.soyvinko.app.ui.feature.admin

import com.soyvinko.app.ui.common.divHalfUp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.soyvinko.app.ui.common.money
import com.soyvinko.app.ui.theme.Manrope
import com.soyvinko.app.ui.theme.Vinko

/** Tarjeta de un plan del modelo de negocio oficial (contenido fijo de la web). */
private data class BusinessPlan(
    val badge: String,
    val tag: String,
    val name: String,
    val price: String,
    val commissionTitle: String,
    val commission: String,
    val benefits: List<String>,
    val featured: Boolean = false,
)

private val STORE_PLANS = listOf(
    BusinessPlan(
        "Meses 1 al 3", "Comercios", "Plan Promocional Entrada", "\$0,00", "Comisión por Venta",
        "0% (Primer mes) / 3% (Meses 2 y 3)",
        listOf("Registro gratis de la tienda en marketplace.", "Indexación en motor de búsqueda instantánea.", "Carga de inventario de hasta 15 productos."),
    ),
    BusinessPlan(
        "Desde Mes 4", "Membresía Estándar", "Membresía Comercio Estándar", "\$30,00", "Comisión por Venta", "Tasa Mínima 4%",
        listOf(
            "Mayor visibilidad en búsquedas y destacados.", "Botón de compra exprés directa de productos.",
            "Acceso a panel de estadísticas de ventas.", "Carga de inventario de hasta 30 productos.",
        ),
        featured = true,
    ),
    BusinessPlan(
        "Desde Mes 4", "Membresía Plus", "Membresía Comercio Plus", "\$50,00", "Comisión por Venta", "Tasa Mínima 4%",
        listOf(
            "Máxima visibilidad en catálogo y búsquedas.", "Botón de compra exprés directa de productos.",
            "Acceso a estadísticas de ventas avanzadas.", "Carga de inventario extendido de hasta 70 productos.",
        ),
        featured = true,
    ),
    BusinessPlan(
        "Desde Mes 4", "Sin Suscripción", "Modalidad Base Sin Costo Fijo", "\$0,00", "Comisión por Venta", "10% por venta",
        listOf(
            "Permanencia en la app sin costo fijo mensual.", "Ideal para comercios con muy bajo volumen inicial.",
            "Carga de inventario básico (hasta 10 productos).",
        ),
    ),
)

private val PROVIDER_PLANS = listOf(
    BusinessPlan(
        "Meses 1 al 3", "Proveedores", "Plan Promocional Entrada", "\$0,00", "Comisión por Servicio",
        "0% (Primer mes) / 3% (Meses 2 y 3)",
        listOf(
            "Certificación de perfil profesional gratis.", "Acceso completo a la cartelera de solicitudes.",
            "Recepción de solicitudes directas de clientes.",
        ),
    ),
    BusinessPlan(
        "Desde Mes 4", "Membresía Profesional", "Membresía Profesional", "\$15,00", "Comisión por Servicio",
        "Tasa Mínima: 3% Presencial / 6% Remoto",
        listOf(
            "Prioridad exclusiva para recibir solicitudes directas.",
            "Descuento en comisión para mitigar gastos presenciales (gasolina, viáticos).",
            "Mayor posicionamiento en el directorio de profesionales.",
        ),
        featured = true,
    ),
    BusinessPlan(
        "Desde Mes 4", "Sin Suscripción", "Modalidad Base Sin Mensualidad", "\$0,00", "Comisión por Servicio",
        "8% Presencial / 12% Remoto",
        listOf(
            "No paga mensualidad ni tarifa fija.",
            "Ideal para trabajadores ocasionales o remotos con menos costos físicos.",
            "Cobro únicamente al concertar y liquidar el servicio.",
        ),
    ),
)

/** "Modelo de Negocio Oficial de Vinko": planes por tipo de afiliado (vista especial de la sección Planes). */
@Composable
fun BusinessModelCard() {
    var tab by rememberSaveable { mutableStateOf("stores") }
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(18.dp))
            .border(1.dp, Vinko.Line, RoundedCornerShape(18.dp))
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Vinko.Primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Modelo de Negocio Oficial de Vinko", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                "Estructura de membresías mensuales, comisiones por venta/servicio y beneficios para Comercios y Proveedores.",
                style = MaterialTheme.typography.bodySmall,
                color = Vinko.Muted,
            )
            Spacer(Modifier.height(2.dp))
            Segmented(listOf("stores" to "Comercios y Vendedores", "providers" to "Proveedores"), tab, { tab = it })
        }
        Row(
            Modifier.horizontalScroll(rememberScrollState()).height(IntrinsicSize.Max).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            (if (tab == "stores") STORE_PLANS else PROVIDER_PLANS).forEach { PlanCard(it) }
        }
    }
}

@Composable
private fun PlanCard(p: BusinessPlan) {
    Column(
        Modifier
            .width(250.dp)
            .fillMaxHeight()
            .background(if (p.featured) Vinko.Tint else Vinko.Cream, RoundedCornerShape(16.dp))
            .border(if (p.featured) 1.5.dp else 1.dp, if (p.featured) Vinko.Lavender else Vinko.Line, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Tag(p.tag)
            Spacer(Modifier.weight(1f))
            Text(p.badge, style = MaterialTheme.typography.labelMedium, color = if (p.featured) Vinko.Primary else Vinko.Muted)
        }
        Text(p.name, style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(p.price, fontFamily = Manrope, style = MaterialTheme.typography.headlineSmall, color = Vinko.Ink)
            Spacer(Modifier.width(6.dp))
            Text("USD / mes", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted, modifier = Modifier.padding(bottom = 4.dp))
        }
        Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(12.dp)).padding(10.dp)) {
            Text(p.commissionTitle, style = MaterialTheme.typography.labelMedium, color = Vinko.Muted)
            Text(p.commission, style = MaterialTheme.typography.titleSmall, color = Vinko.Primary)
        }
        p.benefits.forEach { b ->
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Vinko.Mint, modifier = Modifier.padding(top = 2.dp).size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text(b, style = MaterialTheme.typography.bodySmall, color = Vinko.Ink)
            }
        }
    }
}

/** Resultado del simulador: porcentaje y explicación (misma regla que CommissionSimulator de la web). */
data class SimResult(val percent: Int, val label: String)

fun simulateCommission(type: String, mode: String, servicePlan: String, storePlan: String): SimResult {
    val where = if (mode == "onsite") "Presencial" else "Remoto"
    return if (type == "service") when (servicePlan) {
        "promo_m1" -> SimResult(0, "Plan Promocional Entrada (Mes 1 · 0%)")
        "promo_m2" -> SimResult(3, "Plan Promocional Entrada (Meses 2 y 3 · 3%)")
        "pro" -> (if (mode == "onsite") 3 else 6).let { SimResult(it, "Membresía Profesional (\$15,00 USD) · $it% ($where)") }
        else -> (if (mode == "onsite") 8 else 12).let { SimResult(it, "Modalidad Base Sin Suscripción · $it% ($where)") }
    } else when (storePlan) {
        "promo_m1" -> SimResult(0, "Plan Promocional Entrada (Mes 1 · 0%)")
        "promo_m2" -> SimResult(3, "Plan Promocional Entrada (Meses 2 y 3 · 3%)")
        "standard" -> SimResult(4, "Membresía Comercio Estándar (\$30,00 USD) · Tasa Mínima 4%")
        "plus" -> SimResult(4, "Membresía Comercio Plus (\$50,00 USD) · Tasa Mínima 4%")
        else -> SimResult(10, "Modalidad Base Sin Suscripción · 10% por venta")
    }
}

/** Simulador interactivo del modelo de negocio: retención de la plataforma y neto del afiliado. */
@Composable
fun CommissionSimulator() {
    var type by rememberSaveable { mutableStateOf("service") }
    var amount by rememberSaveable { mutableStateOf("100") }
    var mode by rememberSaveable { mutableStateOf("onsite") }
    var servicePlan by rememberSaveable { mutableStateOf("pro") }
    var storePlan by rememberSaveable { mutableStateOf("standard") }
    val minor = parseDecimal(amount)?.let(::toMinor) ?: 0L
    val result = simulateCommission(type, mode, servicePlan, storePlan)
    val fee = minor * result.percent / 100
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(18.dp))
            .border(1.dp, Vinko.Line, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBox(Icons.Outlined.Calculate)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Simulador Interactivo del Modelo de Negocio", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Calcula en tiempo real las retenciones de la plataforma y el neto a percibir por el afiliado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Vinko.Muted,
                )
            }
        }
        FieldLabel("Operación a simular")
        Segmented(listOf("service" to "Servicio contratado", "store" to "Venta marketplace"), type, { type = it })
        AdminTextField(
            amount, { v -> amount = v.filter { it.isDigit() || it == ',' || it == '.' }.take(12) },
            "Monto de la transacción", keyboardType = KeyboardType.Decimal, prefix = "US$ ",
        )
        if (type == "service") {
            FieldLabel("Modalidad de ejecución")
            Segmented(listOf("onsite" to "Presencial", "remote" to "Remoto"), mode, { mode = it })
            FieldLabel("Plan del proveedor")
            RadioList(
                listOf(
                    Triple("promo_m1", "Plan Promocional (Mes 1)", "0%"),
                    Triple("promo_m2", "Plan Promocional (Meses 2 y 3)", "3%"),
                    Triple("pro", "Membresía Profesional", "\$15 USD · 3% presencial / 6% remoto"),
                    Triple("base", "Modalidad Base Sin Suscripción", "\$0 USD · 8% presencial / 12% remoto"),
                ),
                servicePlan,
            ) { servicePlan = it }
        } else {
            FieldLabel("Plan del comercio / tienda")
            RadioList(
                listOf(
                    Triple("promo_m1", "Plan Promocional (Mes 1)", "0%"),
                    Triple("promo_m2", "Plan Promocional (Meses 2 y 3)", "3%"),
                    Triple("standard", "Membresía Comercio Estándar", "\$30 USD · Tasa mínima 4%"),
                    Triple("plus", "Membresía Comercio Plus", "\$50 USD · Tasa mínima 4%"),
                    Triple("base", "Modalidad Base Sin Suscripción", "\$0 USD · 10% por venta"),
                ),
                storePlan,
            ) { storePlan = it }
        }
        SimulatorResults(
            "${result.percent}%", result.label,
            money(fee, "USD"), "Comisión retenida por intermediación",
            money(minor - fee, "USD"), "Saldo neto liquidable o cobrado",
            feeTitle = "Ingreso Plataforma (Vinko)", netTitle = "Neto para el Afiliado",
        )
    }
}

/** Tres resultados del simulador: tasa, ingreso de la plataforma y neto. */
@Composable
fun SimulatorResults(
    rate: String,
    rateCaption: String,
    fee: String,
    feeCaption: String,
    net: String,
    netCaption: String,
    feeTitle: String,
    netTitle: String,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(Vinko.Dark, Vinko.DarkSurface)), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column {
            Text("Tasa de comisión", style = MaterialTheme.typography.labelMedium, color = Vinko.Lilac)
            Text(rate, fontFamily = Manrope, style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Text(rateCaption, style = MaterialTheme.typography.bodySmall, color = Vinko.Lilac)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ResultBox(feeTitle, fee, feeCaption, Color(0xFFFFD76A), Modifier.weight(1f))
            ResultBox(netTitle, net, netCaption, Color(0xFF4ADE80), Modifier.weight(1f))
        }
    }
}

@Composable
private fun ResultBox(title: String, value: String, caption: String, accent: Color, modifier: Modifier) {
    Column(modifier.background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp)).padding(10.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = Vinko.Lilac)
        Text(value, style = MaterialTheme.typography.titleMedium, color = accent)
        Text(caption, style = MaterialTheme.typography.bodySmall, color = Vinko.Lilac.copy(alpha = 0.85f))
    }
}

/** Simulador de la comisión de retiro vigente: cuánto paga y cuánto recibe quien retira. */
@Composable
fun WithdrawalFeeSimulator(activeBasisPoints: Map<String, Long>) {
    var currency by rememberSaveable { mutableStateOf("VES") }
    var amount by rememberSaveable { mutableStateOf("1000") }
    val minor = parseDecimal(amount)?.let(::toMinor) ?: 0L
    val bp = activeBasisPoints[currency]
    val fee = if (bp == null) 0L else divHalfUp(minor * bp, 10_000)
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(18.dp))
            .border(1.dp, Vinko.Line, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBox(Icons.Outlined.Calculate)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Simulador de comisión de retiro", style = MaterialTheme.typography.titleMedium)
                Text("Con la tarifa vigente de cada moneda.", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
            }
        }
        Segmented(listOf("VES" to "Bolívares (VES)", "USD" to "Dólares (USD)"), currency, { currency = it })
        AdminTextField(
            amount, { v -> amount = v.filter { it.isDigit() || it == ',' || it == '.' }.take(14) },
            "Monto a retirar", keyboardType = KeyboardType.Decimal, prefix = currencyPrefix(currency),
        )
        SimulatorResults(
            if (bp == null) "Sin tarifa" else percentText(bp),
            if (bp == null) "No hay una comisión vigente para esta moneda." else "Tarifa vigente para ${currencyName(currency)}",
            money(fee, currency), "Comisión retenida por SoyVinko",
            money(minor - fee, currency), "Neto que recibe en su cuenta",
            feeTitle = "Comisión", netTitle = "Neto a recibir",
        )
    }
}
