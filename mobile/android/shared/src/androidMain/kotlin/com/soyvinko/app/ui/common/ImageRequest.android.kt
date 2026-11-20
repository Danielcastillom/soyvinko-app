package com.soyvinko.app.ui.common

import coil3.ComponentRegistry
import coil3.decode.BitmapFactoryDecoder
import coil3.request.ImageRequest
import coil3.request.allowHardware

actual fun ImageRequest.Builder.softwareBitmaps(): ImageRequest.Builder = allowHardware(false)

/**
 * BitmapFactory, como en las versiones anteriores de la app (Coil 2). Coil 3 usaría ImageDecoder en
 * Android 10+, que en las pruebas de Robolectric hace caer la JVM al decodificar en segundo plano.
 */
actual fun ComponentRegistry.Builder.addPlatformDecoders(): ComponentRegistry.Builder = add(BitmapFactoryDecoder.Factory())
