package com.soyvinko.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Iconografía moderna de Majesticons (https://majesticons.com/).
 * Licencia MIT (Gerrit Halfmann).
 * Iconos vectoriales 24x24 limpios, estéticos y profesionales.
 */
object Majesticons {

    private fun lineIcon(name: String, pathData: String): ImageVector =
        ImageVector.Builder(
            name = "Majesticons.$name",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).addPath(
            pathData = addPathNodes(pathData),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ).build()

    private fun multiLineIcon(name: String, paths: List<String>): ImageVector {
        val builder = ImageVector.Builder(
            name = "Majesticons.$name",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        )
        for (p in paths) {
            builder.addPath(
                pathData = addPathNodes(p),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return builder.build()
    }

    private fun solidIcon(name: String, pathData: String, fillType: PathFillType = PathFillType.NonZero): ImageVector =
        ImageVector.Builder(
            name = "Majesticons.$name",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).addPath(
            pathData = addPathNodes(pathData),
            fill = SolidColor(Color.Black),
            pathFillType = fillType,
        ).build()

    private fun multiSolidIcon(name: String, paths: List<String>): ImageVector {
        val builder = ImageVector.Builder(
            name = "Majesticons.$name",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        )
        for (p in paths) {
            builder.addPath(
                pathData = addPathNodes(p),
                fill = SolidColor(Color.Black),
            )
        }
        return builder.build()
    }

    // --- HOME (Inicio) ---
    val Home: ImageVector by lazy {
        lineIcon(
            "Home",
            "M20 19v-8.5a1 1 0 0 0-.4-.8l-7-5.25a1 1 0 0 0-1.2 0l-7 5.25a1 1 0 0 0-.4.8V19a1 1 0 0 0 1 1h4a1 1 0 0 0 1-1v-3a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1v3a1 1 0 0 0 1 1h4a1 1 0 0 0 1-1z"
        )
    }

    val HomeSolid: ImageVector by lazy {
        solidIcon(
            "HomeSolid",
            "M20 19v-8.5a1 1 0 0 0-.4-.8l-7-5.25a1 1 0 0 0-1.2 0l-7 5.25a1 1 0 0 0-.4.8V19a1 1 0 0 0 1 1h4a1 1 0 0 0 1-1v-3a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1v3a1 1 0 0 0 1 1h4a1 1 0 0 0 1-1z"
        )
    }

    // --- DOCUMENT / SOLICITUDES ---
    val Document: ImageVector by lazy {
        lineIcon(
            "Document",
            "M19 20H5c-1.6 0-2-1.333-2-2V6a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v6m2 8c-.667 0-2-.4-2-2v-6m2 8c1.6 0 2-1.333 2-2v-6h-4"
        )
    }

    val DocumentSolid: ImageVector by lazy {
        solidIcon(
            "DocumentSolid",
            "M19 21H5c-1.126 0-1.926-.491-2.412-1.166A3.233 3.233 0 0 1 2 18V6a3 3 0 0 1 3-3h10a3 3 0 0 1 3 3v5h3a1 1 0 0 1 1 1v6c0 .493-.14 1.211-.588 1.834C20.925 20.51 20.125 21 19 21zm1-7a1 1 0 1 0-2 0v4a1 1 0 1 0 2 0v-4z",
            fillType = PathFillType.EvenOdd
        )
    }

    // --- MEGAPHONE (Anuncios / Solicitudes abiertas) ---
    val Megaphone: ImageVector by lazy {
        lineIcon(
            "Megaphone",
            "M13 15V7m0 8 5.504 3.145A1 1 0 0 0 20 17.277V4.723a1 1 0 0 0-1.496-.868L13 7m0 8h-3m3-8H7a4 4 0 0 0-4 4v0a4 4 0 0 0 4 4v0m0 0v4.5A1.5 1.5 0 0 0 8.5 21v0a1.5 1.5 0 0 0 1.5-1.5V15m-3 0h3"
        )
    }

    val MegaphoneSolid: ImageVector by lazy {
        solidIcon(
            "MegaphoneSolid",
            "M18.008 2.987C19.34 2.225 21 3.187 21 4.723v12.554c0 1.535-1.659 2.498-2.992 1.736L14 16.723V5.277l4.008-2.29zM12 6H7a5 5 0 0 0-1 9.9v3.6a2.5 2.5 0 0 0 5 0V16h1V6z",
            fillType = PathFillType.EvenOdd
        )
    }

    // --- BRIEFCASE (Servicios / Contratos) ---
    val Briefcase: ImageVector by lazy {
        lineIcon(
            "Briefcase",
            "M15 8V6a2 2 0 0 0-2-2h-2a2 2 0 0 0-2 2v2m6 0h4a2 2 0 0 1 2 2v3m-6-5H9m0 0H5a2 2 0 0 0-2 2v3m0 0v5a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-5M3 13h7m11 0h-7m0 0v-2h-4v2m4 0v2h-4v-2"
        )
    }

    val BriefcaseSolid: ImageVector by lazy {
        solidIcon(
            "BriefcaseSolid",
            "M8 6a3 3 0 0 1 3-3h2a3 3 0 0 1 3 3v1h3a3 3 0 0 1 3 3v2h-7v-1a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v1H2v-2a3 3 0 0 1 3-3h3V6zm-6 8v4a3 3 0 0 0 3 3h14a3 3 0 0 0 3-3v-4h-7v1a1 1 0 0 1-1 1h-4a1 1 0 0 1-1-1v-1H2zm8-7h4V6a1 1 0 0 0-1-1h-2a1 1 0 0 0-1 1v1zm3 7h-2v-2h2v2z",
            fillType = PathFillType.EvenOdd
        )
    }

    // --- LIST BOX (Servicios / Lista / Contratos) ---
    val ListBox: ImageVector by lazy {
        lineIcon(
            "ListBox",
            "M11 8h5m0 4h-5m5 4h-5m-5 4h12a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2zM8 8h.001M8 12h.001M8 16h.001"
        )
    }

    val ListBoxSolid: ImageVector by lazy {
        solidIcon(
            "ListBoxSolid",
            "M6 3a3 3 0 0 0-3 3v12a3 3 0 0 0 3 3h12a3 3 0 0 0 3-3V6a3 3 0 0 0-3-3H6zm4 5a1 1 0 0 1 1-1h6a1 1 0 1 1 0 2h-6a1 1 0 0 1-1-1zm0 4a1 1 0 0 1 1-1h6a1 1 0 1 1 0 2h-6a1 1 0 0 1-1-1zm0 4a1 1 0 0 1 1-1h6a1 1 0 1 1 0 2h-6a1 1 0 0 1-1-1zM7 7a1 1 0 0 0 0 2h.001a1 1 0 0 0 0-2H7zm-1 5a1 1 0 0 1 1-1h.001a1 1 0 1 1 0 2H7a1 1 0 0 1-1-1zm1 3a1 1 0 1 0 0 2h.001a1 1 0 1 0 0-2H7z",
            fillType = PathFillType.EvenOdd
        )
    }

    // --- WALLET / CREDIT CARD ---
    val CreditCard: ImageVector by lazy {
        lineIcon(
            "CreditCard",
            "M3 9v8a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V9M3 9V7a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v2M3 9h18M7 13h5"
        )
    }

    val CreditCardSolid: ImageVector by lazy {
        solidIcon(
            "CreditCardSolid",
            "M2 7a3 3 0 0 1 3-3h14a3 3 0 0 1 3 3v1H2V7zm0 3v7a3 3 0 0 0 3 3h14a3 3 0 0 0 3-3v-7H2zm5 2a1 1 0 1 0 0 2h5a1 1 0 1 0 0-2H7z",
            fillType = PathFillType.EvenOdd
        )
    }

    // --- USER (Perfil) ---
    val User: ImageVector by lazy {
        multiLineIcon(
            "User",
            listOf(
                "M12 3a5 5 0 1 0 0 10 5 5 0 0 0 0-10z",
                "M20 21a8 8 0 1 0-16 0"
            )
        )
    }

    val UserSolid: ImageVector by lazy {
        multiSolidIcon(
            "UserSolid",
            listOf(
                "M12 3a5 5 0 1 0 0 10 5 5 0 0 0 0-10z",
                "M12 13a8 8 0 0 0-8 8h16a8 8 0 0 0-8-8z"
            )
        )
    }

    // --- CLOCK (Historial) ---
    val Clock: ImageVector by lazy {
        multiLineIcon(
            "Clock",
            listOf(
                "M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18z",
                "M12 7v3.764a2 2 0 0 0 1.106 1.789L16 14"
            )
        )
    }

    val ClockSolid: ImageVector by lazy {
        solidIcon(
            "ClockSolid",
            "M2 12C2 6.477 6.477 2 12 2s10 4.477 10 10-4.477 10-10 10S2 17.523 2 12zm11-5a1 1 0 1 0-2 0v3.764a3 3 0 0 0 1.658 2.683l2.895 1.447a1 1 0 1 0 .894-1.788l-2.894-1.448a1 1 0 0 1-.553-.894V7z",
            fillType = PathFillType.EvenOdd
        )
    }

    // --- BELL (Notificaciones) ---
    val Bell: ImageVector by lazy {
        lineIcon(
            "Bell",
            "M12 22a2 2 0 0 0 2-2h-4a2 2 0 0 0 2 2zm6-6V10a6 6 0 1 0-12 0v6l-2 2v1h16v-1l-2-2z"
        )
    }

    val BellSolid: ImageVector by lazy {
        solidIcon(
            "BellSolid",
            "M12 22a2 2 0 0 0 2-2h-4a2 2 0 0 0 2 2zm6-6V10a6 6 0 1 0-12 0v6l-2 2v1h16v-1l-2-2z",
            fillType = PathFillType.EvenOdd
        )
    }

    // --- PIN (Ubicación / Direcciones) ---
    val Pin: ImageVector by lazy {
        lineIcon(
            "Pin",
            "M12 21s-6-5.333-6-10a6 6 0 1 1 12 0c0 4.667-6 10-6 10zm0-8a2 2 0 1 0 0-4 2 2 0 0 0 0 4z"
        )
    }

    val PinSolid: ImageVector by lazy {
        solidIcon(
            "PinSolid",
            "M12 2a7 7 0 0 0-7 7c0 5.25 7 13 7 13s7-7.75 7-13a7 7 0 0 0-7-7zm0 9.5a2.5 2.5 0 1 1 0-5 2.5 2.5 0 0 1 0 5z",
            fillType = PathFillType.EvenOdd
        )
    }

    // --- LOCK (Seguridad) ---
    val Lock: ImageVector by lazy {
        lineIcon(
            "Lock",
            "M17 11V7a5 5 0 0 0-10 0v4H5v10h14V11h-2zm-8-4a3 3 0 0 1 6 0v4H9V7z"
        )
    }

    val LockSolid: ImageVector by lazy {
        solidIcon(
            "LockSolid",
            "M17 11V7a5 5 0 0 0-10 0v4H5v10h14V11h-2zm-8-4a3 3 0 0 1 6 0v4H9V7z",
            fillType = PathFillType.EvenOdd
        )
    }

    // --- TRUCK (Transporte / Envíos / Delivery) ---
    val Truck: ImageVector by lazy {
        lineIcon(
            "Truck",
            "M16 8h4l3 3v5h-2M1 5h15v11H1zm5 11a2 2 0 1 1-4 0 2 2 0 0 1 4 0zm14 0a2 2 0 1 1-4 0 2 2 0 0 1 4 0z"
        )
    }

    val TruckSolid: ImageVector by lazy {
        solidIcon(
            "TruckSolid",
            "M1 5a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v2h3.5a1 1 0 0 1 .7.3l2.5 2.5a1 1 0 0 1 .3.7V15a2 2 0 0 1-2 2h-1a3 3 0 0 1-6 0H9a3 3 0 0 1-6 0H2a2 2 0 0 1-2-2V5zm5 12a1 1 0 1 0 0-2 1 1 0 0 0 0 2zm13 0a1 1 0 1 0 0-2 1 1 0 0 0 0 2z",
            fillType = PathFillType.EvenOdd
        )
    }
}

