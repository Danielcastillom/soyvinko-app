package com.soyvinko.app.update

import com.soyvinko.app.data.ApiException
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.security.MessageDigest

class UpdateRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var server: MockWebServer
    private val apk = ByteArray(300_000) { (it % 251).toByte() }
    private val sha = MessageDigest.getInstance("SHA-256").digest(apk).joinToString("") { "%02x".format(it) }

    @Before fun setUp() { server = MockWebServer().apply { start() } }
    @After fun tearDown() = server.shutdown()

    private fun repo(installed: Int) = UpdateRepository(server.url("/downloads/android/latest.json").toString(), installed, tmp.newFolder())

    private fun manifest(code: Int, mandatory: Boolean = true, min: Int = 0, url: String = server.url("/downloads/android/soyvinko-$code.apk").toString(), hash: String = sha) =
        """{"versionCode":$code,"versionName":"1.$code.0","minVersionCode":$min,"mandatory":$mandatory,"url":"$url","sha256":"$hash","size":${apk.size},"notes":["Mejoras"]}"""

    @Test fun sameVersionIsUpToDate() = runTest {
        server.enqueue(MockResponse().setBody(manifest(2)))
        assertEquals(UpdateCheck.UpToDate, repo(2).check())
    }

    @Test fun newerMandatoryVersionIsRequired() = runTest {
        server.enqueue(MockResponse().setBody(manifest(3)))
        val result = repo(2).check() as UpdateCheck.Available
        assertTrue(result.mandatory)
        assertEquals("1.3.0", result.release.versionName)
    }

    @Test fun optionalBecomesMandatoryBelowMinimum() = runTest {
        server.enqueue(MockResponse().setBody(manifest(5, mandatory = false, min = 2)))
        assertFalse((repo(3).check() as UpdateCheck.Available).mandatory)
        server.enqueue(MockResponse().setBody(manifest(5, mandatory = false, min = 4)))
        assertTrue((repo(3).check() as UpdateCheck.Available).mandatory)
    }

    @Test fun downloadReportsProgressAndVerifiesHash() = runTest {
        server.enqueue(MockResponse().setBody(manifest(3)))
        val r = repo(2)
        val release = (r.check() as UpdateCheck.Available).release
        server.enqueue(MockResponse().setBody(Buffer().write(apk)))
        val progress = mutableListOf<Float>()
        val file = r.download(release) { progress += it }
        assertEquals(apk.size.toLong(), file.length())
        assertEquals(1f, progress.last())
        assertTrue(progress.size > 2)
        // Segunda vez: reutiliza el archivo verificado, sin volver a descargar.
        val requests = server.requestCount
        r.download(release) {}
        assertEquals(requests, server.requestCount)
    }

    @Test fun tamperedApkIsRejected() = runTest {
        server.enqueue(MockResponse().setBody(manifest(3, hash = "0".repeat(64))))
        val r = repo(2)
        val release = (r.check() as UpdateCheck.Available).release
        server.enqueue(MockResponse().setBody(Buffer().write(apk)))
        try {
            r.download(release) {}
            fail("Debía rechazar el APK")
        } catch (e: ApiException) {
            assertEquals("update_integrity", e.code)
        }
    }

    @Test fun apkFromAnotherHostIsRejected() = runTest {
        server.enqueue(MockResponse().setBody(manifest(3, url = "https://malicioso.example/soyvinko.apk")))
        try {
            repo(2).check()
            fail("Debía rechazar el origen")
        } catch (e: ApiException) {
            assertEquals("update_untrusted", e.code)
        }
    }
}
