package com.soyvinko.app.ui.common

import coil3.ComponentRegistry
import coil3.request.ImageRequest

/** En iOS las imágenes ya se decodifican en memoria normal. */
actual fun ImageRequest.Builder.softwareBitmaps(): ImageRequest.Builder = this

/** En iOS Coil decodifica con Skia; no hace falta nada más. */
actual fun ComponentRegistry.Builder.addPlatformDecoders(): ComponentRegistry.Builder = this
