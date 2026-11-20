package com.soyvinko.app

import com.soyvinko.app.data.AppServices

/** Servicios de la app (API, sesión, operaciones), creados por cada plataforma al arrancar. */
object AppGraph {
    private var current: AppServices? = null

    val services: AppServices
        get() = current ?: error("AppGraph.configure() no se ha llamado")

    /** Android lo llama en Application.onCreate; iOS al crear la pantalla; las pruebas con un servidor simulado. */
    fun configure(services: AppServices) {
        current = services
    }
}
