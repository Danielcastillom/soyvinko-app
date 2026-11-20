package com.soyvinko.app.update

import com.soyvinko.app.data.ApiException
import com.soyvinko.app.data.ApiJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.security.MessageDigest

/**
 * Manifiesto publicado en /downloads/android/latest.json por scripts/publish-release.sh.
 * - versionCode > instalado y mandatory=true  -> actualización obligatoria (bloquea la app).
 * - versionCode instalado < minVersionCode    -> obligatoria aunque mandatory=false.
 */
@Serializable
data class ReleaseManifest(
    val versionCode: Int,
    val versionName: String,
    val minVersionCode: Int = 0,
    val mandatory: Boolean = true,
    val url: String,
    val sha256: String,
    val size: Long,
    val notes: List<String> = emptyList(),
    val publishedAt: String? = null,
)

sealed interface UpdateCheck {
    data object UpToDate : UpdateCheck
    data class Available(val release: ReleaseManifest, val mandatory: Boolean) : UpdateCheck
}

class UpdateRepository(
    private val manifestUrl: String,
    private val installedVersionCode: Int,
    private val dir: File,
    private val http: OkHttpClient = OkHttpClient(),
) {
    suspend fun check(): UpdateCheck = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(manifestUrl).header("Cache-Control", "no-cache").build()
        val text = try {
            http.newCall(request).execute().use { r ->
                if (!r.isSuccessful) throw ApiException("No se pudo consultar la versión disponible.", "update_check_failed", r.code)
                r.body?.string().orEmpty()
            }
        } catch (e: IOException) {
            throw ApiException("Sin conexión para buscar actualizaciones.", "network", 0)
        }
        val release = try {
            ApiJson.decodeFromString(ReleaseManifest.serializer(), text)
        } catch (e: IllegalArgumentException) {
            throw ApiException("El manifiesto de versiones no es válido.", "bad_manifest", 200)
        }
        if (release.versionCode <= installedVersionCode) {
            cleanup()
            UpdateCheck.UpToDate
        } else {
            requireTrustedUrl(release.url)
            UpdateCheck.Available(release, release.mandatory || installedVersionCode < release.minVersionCode)
        }
    }

    /**
     * Descarga el APK a la caché privada y verifica tamaño y SHA-256 antes de entregarlo.
     * Reutiliza una descarga previa válida de la misma versión.
     */
    suspend fun download(release: ReleaseManifest, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        requireTrustedUrl(release.url)
        dir.mkdirs()
        val target = File(dir, "soyvinko-${release.versionCode}.apk")
        if (target.exists() && target.length() == release.size && sha256(target).equals(release.sha256, ignoreCase = true)) {
            onProgress(1f)
            return@withContext target
        }
        val partial = File(dir, target.name + ".part").apply { delete() }
        try {
            http.newCall(Request.Builder().url(release.url).build()).execute().use { r ->
                if (!r.isSuccessful) throw ApiException("No se pudo descargar la actualización (HTTP ${r.code}).", "update_download_failed", r.code)
                val body = r.body ?: throw ApiException("La descarga llegó vacía.", "update_download_failed", r.code)
                val total = release.size.takeIf { it > 0 } ?: body.contentLength()
                body.byteStream().use { input ->
                    partial.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var read = 0L
                        var lastReported = -1
                        while (true) {
                            ensureActive()
                            val n = input.read(buffer)
                            if (n < 0) break
                            output.write(buffer, 0, n)
                            read += n
                            val pct = if (total > 0) ((read * 100) / total).toInt() else 0
                            if (pct != lastReported) {
                                lastReported = pct
                                onProgress((pct / 100f).coerceIn(0f, 1f))
                            }
                        }
                    }
                }
            }
        } catch (e: IOException) {
            partial.delete()
            throw ApiException("Se interrumpió la descarga. Revisa tu conexión e inténtalo de nuevo.", "network", 0)
        }
        if (partial.length() != release.size || !sha256(partial).equals(release.sha256, ignoreCase = true)) {
            partial.delete()
            throw ApiException("La descarga no pasó la verificación de seguridad. Inténtalo de nuevo.", "update_integrity", 200)
        }
        target.delete()
        if (!partial.renameTo(target)) throw ApiException("No se pudo guardar la actualización.", "update_storage", 0)
        dir.listFiles()?.filter { it != target }?.forEach { it.delete() }
        target
    }

    /** Solo se descarga desde el mismo origen (esquema, dominio y puerto) que el manifiesto. */
    private fun requireTrustedUrl(url: String) {
        val manifest = manifestUrl.toHttpUrl()
        val apk = url.toHttpUrlOrNull()
        if (apk == null || apk.scheme != manifest.scheme || apk.host != manifest.host || apk.port != manifest.port) {
            throw ApiException("La actualización no proviene de SoyVinko.", "update_untrusted", 200)
        }
    }

    private fun cleanup() {
        dir.listFiles()?.forEach { it.delete() }
    }

    companion object {
        fun sha256(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    digest.update(buffer, 0, n)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
    }
}
