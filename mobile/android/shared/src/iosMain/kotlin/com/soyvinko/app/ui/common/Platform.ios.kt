package com.soyvinko.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties
import coil3.PlatformContext
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun openExternalUrl(context: PlatformContext, url: String): Boolean {
    val nsUrl = NSURL.URLWithString(url) ?: return false
    val app = UIApplication.sharedApplication
    // canOpenURL solo responde por los esquemas declarados en LSApplicationQueriesSchemes (Info.plist).
    if (!app.canOpenURL(nsUrl)) return false
    app.openURL(nsUrl, options = emptyMap<Any?, Any>(), completionHandler = null)
    return true
}

/** iOS no tiene botón Atrás: se vuelve con los botones de la propia pantalla. */
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) = Unit

actual val platformName: String = "iOS"

actual fun fullScreenDialogProperties() = DialogProperties(usePlatformDefaultWidth = false)
