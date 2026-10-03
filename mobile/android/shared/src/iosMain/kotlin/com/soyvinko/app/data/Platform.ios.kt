package com.soyvinko.app.data

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

actual fun platformHttpEngine(): HttpClientEngine = Darwin.create()

actual fun platformTokenStorage(): TokenStorage = KeychainTokenStorage()

/**
 * Refresh token en el Keychain de iOS, solo en este dispositivo (no se sincroniza con iCloud
 * ni se restaura en otro iPhone).
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class KeychainTokenStorage(private val service: String = "com.soyvinko.app.session") : TokenStorage {
    override var refreshToken: String?
        get() = read("refresh_token")
        set(value) = write("refresh_token", value)

    override var cachedUserJson: String?
        get() = read("user")
        set(value) = write("user", value)

    override fun clear() {
        write("refresh_token", null)
        write("user", null)
    }

    /** Arma la consulta del Keychain como CFDictionary y libera todo al terminar. */
    private fun <T> withQuery(account: String, extra: List<Pair<CFStringRef?, CFTypeRef?>>, block: (CFDictionaryRef?) -> T): T {
        val retained = mutableListOf<CFTypeRef?>()
        val dict = CFDictionaryCreateMutable(kCFAllocatorDefault, 0, kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr)
        try {
            CFDictionaryAddValue(dict, kSecClass, kSecClassGenericPassword)
            CFBridgingRetain(NSString.create(string = service)).also { retained += it; CFDictionaryAddValue(dict, kSecAttrService, it) }
            CFBridgingRetain(NSString.create(string = account)).also { retained += it; CFDictionaryAddValue(dict, kSecAttrAccount, it) }
            extra.forEach { (k, v) -> CFDictionaryAddValue(dict, k, v) }
            return block(dict)
        } finally {
            retained.forEach { CFRelease(it) }
            CFRelease(dict)
        }
    }

    private fun read(account: String): String? = memScoped {
        val result = alloc<CFTypeRefVar>()
        val status = withQuery(account, listOf(kSecReturnData to kCFBooleanTrue, kSecMatchLimit to kSecMatchLimitOne)) {
            SecItemCopyMatching(it, result.ptr)
        }
        if (status != errSecSuccess) return null
        val data = CFBridgingRelease(result.value) as? NSData ?: return null
        NSString.create(data = data, encoding = NSUTF8StringEncoding)?.toString()
    }

    private fun write(account: String, value: String?) {
        withQuery(account, emptyList()) { SecItemDelete(it) }
        if (value == null) return
        val data = NSString.create(string = value).dataUsingEncoding(NSUTF8StringEncoding) ?: return
        val retainedData = CFBridgingRetain(data)
        try {
            withQuery(account, listOf(kSecValueData to retainedData, kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)) {
                SecItemAdd(it, null)
            }
        } finally {
            CFRelease(retainedData)
        }
    }
}
