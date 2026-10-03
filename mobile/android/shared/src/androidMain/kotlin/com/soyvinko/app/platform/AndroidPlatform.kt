package com.soyvinko.app.platform

import android.annotation.SuppressLint
import android.content.ContentProvider
import android.content.Context
import android.content.pm.ProviderInfo

/** Contexto de aplicación para las implementaciones Android (lo fija SoyVinkoApp.onCreate). */
@SuppressLint("StaticFieldLeak")
object AndroidPlatform {
    lateinit var context: Context
        private set
    var versionName: String = "1.0"
        private set

    fun init(context: Context, versionName: String) {
        this.context = context.applicationContext
        this.versionName = versionName
        initComposeResources(this.context)
    }

    /**
     * Los recursos multiplataforma (logo, textos) leen el contexto que guarda el ContentProvider de la
     * biblioteca al arrancar. Robolectric no siempre crea los proveedores de las bibliotecas, así que se
     * inicializa también aquí; en la app real es redundante y no tiene efecto.
     */
    private fun initComposeResources(context: Context) {
        runCatching {
            val provider = Class.forName("org.jetbrains.compose.resources.AndroidContextProvider")
                .getDeclaredConstructor().newInstance() as ContentProvider
            provider.attachInfo(context, ProviderInfo().apply { authority = "${context.packageName}.resources.AndroidContextProvider" })
        }
    }
}
