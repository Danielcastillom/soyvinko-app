package com.soyvinko.app.ui.common

import kotlin.math.absoluteValue
import kotlin.math.sign

/**
 * Decimal exacto para importes y porcentajes escritos por la persona (valor = unscaled / 10^scale).
 * Cubre lo que la app usaba de BigDecimal, que no existe en código común (Android + iOS).
 */
class Decimal private constructor(val unscaled: Long, val scale: Int) : Comparable<Decimal> {
    val signum: Int get() = unscaled.sign

    /** Redondeo HALF_UP a `decimals` decimales como entero escalado: (12,345; 2) -> 1235. Null si desborda. */
    fun scaledHalfUp(decimals: Int): Long? {
        if (decimals >= scale) {
            var v = unscaled
            repeat(decimals - scale) {
                if (v.absoluteValue > Long.MAX_VALUE / 10) return null
                v *= 10
            }
            return v
        }
        var d = 1L
        repeat(scale - decimals) { d *= 10 }
        val q = unscaled.absoluteValue / d
        val r = unscaled.absoluteValue % d
        val rounded = if (r * 2 >= d) q + 1 else q
        return if (unscaled < 0) -rounded else rounded
    }

    override fun compareTo(other: Decimal): Int {
        val s = maxOf(scale, other.scale)
        val a = scaledHalfUp(s) ?: return signum
        val b = other.scaledHalfUp(s) ?: return -other.signum
        return a.compareTo(b)
    }

    override fun equals(other: Any?) = other is Decimal && compareTo(other) == 0
    override fun hashCode() = (scaledHalfUp(6) ?: unscaled).hashCode()
    override fun toString(): String = if (scale == 0) unscaled.toString() else {
        val digits = unscaled.absoluteValue.toString().padStart(scale + 1, '0')
        (if (unscaled < 0) "-" else "") + digits.dropLast(scale) + "." + digits.takeLast(scale)
    }

    companion object {
        val ZERO = Decimal(0, 0)
        private val PLAIN = Regex("^([+-]?)(\\d*)(?:\\.(\\d*))?$")

        /** "1234.50", "-3", ".5" (punto decimal, sin miles). Null si no es un número válido. */
        fun parse(text: String): Decimal? {
            val m = PLAIN.matchEntire(text.trim()) ?: return null
            val (sign, int, frac) = m.destructured
            if (int.isEmpty() && frac.isEmpty()) return null
            val digits = (int + frac).trimStart('0').ifEmpty { "0" }
            if (digits.length > 18) return null
            return Decimal(if (sign == "-") -digits.toLong() else digits.toLong(), frac.length)
        }
    }
}

fun String.toDecimalOrNull(): Decimal? = Decimal.parse(this)

/** Unidades menores a texto editable con coma decimal: 450000 -> "4500,00". */
fun minorToPlain(minor: Long, decimals: Int = 2): String {
    val digits = minor.absoluteValue.toString().padStart(decimals + 1, '0')
    val text = if (decimals == 0) digits else digits.dropLast(decimals) + "," + digits.takeLast(decimals)
    return if (minor < 0) "-$text" else text
}

/** División entera con redondeo HALF_UP (comisiones en puntos básicos). */
fun divHalfUp(numerator: Long, denominator: Long): Long {
    val q = numerator.absoluteValue / denominator
    val r = numerator.absoluteValue % denominator
    val rounded = if (r * 2 >= denominator) q + 1 else q
    return if ((numerator < 0) != (denominator < 0)) -rounded else rounded
}
