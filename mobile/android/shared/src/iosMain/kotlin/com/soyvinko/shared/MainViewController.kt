package com.soyvinko.shared

import androidx.compose.ui.window.ComposeUIViewController
import com.soyvinko.app.AppGraph
import com.soyvinko.app.data.AppInfo
import com.soyvinko.app.data.AppServices
import com.soyvinko.app.ui.root.SoyVinkoRoot
import com.soyvinko.app.ui.theme.SoyVinkoTheme
import platform.Foundation.NSBundle
import platform.UIKit.UIDevice
import platform.UIKit.UIViewController

private const val API_BASE_URL = "https://soyvinko.com/"
private const val WEB_BASE_URL = "https://soyvinko.com"

private fun bundleValue(key: String): String? = NSBundle.mainBundle.objectForInfoDictionaryKey(key) as? String

/** Punto de entrada de iOS: lo llama SwiftUI (iosApp/SoyVinko/ContentView.swift). Misma app que Android. */
fun MainViewController(): UIViewController {
    configureServices()
    return ComposeUIViewController {
        SoyVinkoTheme { SoyVinkoRoot() }
    }
}

private var configured = false

private fun configureServices() {
    if (configured) return
    configured = true
    val version = bundleValue("CFBundleShortVersionString") ?: "1.0"
    val build = bundleValue("CFBundleVersion")?.toIntOrNull() ?: 1
    val device = UIDevice.currentDevice
    AppGraph.configure(
        AppServices(
            AppInfo(
                versionName = version,
                versionCode = build,
                apiBaseUrl = API_BASE_URL,
                webBaseUrl = WEB_BASE_URL,
                // El servidor guarda este texto como nombre del dispositivo de la sesión.
                userAgent = "SoyVinko-iOS/$version (${device.model}; iOS ${device.systemVersion})",
                platform = "iOS",
            ),
        ),
    )
}
