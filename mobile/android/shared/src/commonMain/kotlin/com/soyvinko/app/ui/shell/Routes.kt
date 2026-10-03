package com.soyvinko.app.ui.shell

import com.soyvinko.app.ui.common.encodeUrlComponent

/**
 * Rutas de toda la app. Navegar entre secciones SIEMPRE con estas funciones
 * (nav.navigate(Routes.contract(id))) para no romper enlaces de otra sección.
 */
object Routes {
    // Pestañas inferiores
    const val HOME = "home"
    const val EXPLORE = "explore"
    const val CONTRACTS = "contracts"
    const val WALLET = "wallet"
    const val PROFILE = "profile"

    // Explorar y solicitudes (feature.explore)
    const val PROFESSIONAL = "professional/{id}"
    const val REQUESTS = "requests"
    const val REQUEST = "requests/detail/{id}"
    const val NEW_REQUEST = "requests/new?professional={professional}&offering={offering}"
    const val FAVORITES = "favorites"

    // Servicios contratados (feature.contracts)
    const val CONTRACT = "contracts/{id}"
    const val CHAT = "contracts/{id}/chat"
    const val TRACKING = "contracts/{id}/tracking"

    // Wallet (feature.wallet)
    const val WALLET_HISTORY = "wallet/history"
    const val WITHDRAW = "wallet/withdraw"

    // Perfil (feature.profile)
    const val ACCOUNT = "profile/account"
    const val ADDRESSES = "profile/addresses"
    const val KYC = "profile/kyc"
    const val SECURITY = "profile/security"
    const val NOTIFICATIONS = "notifications"
    const val SUPPORT = "support"
    const val TICKET = "support/{id}"
    const val LEGAL = "legal/{doc}"

    // Profesional y marketplace (feature.pro, feature.marketplace)
    const val PRO = "pro"
    const val OPPORTUNITIES = "pro/opportunities"
    const val MY_PROPOSALS = "pro/proposals"
    const val PRO_SETUP = "pro/setup"
    const val MARKETPLACE = "marketplace"
    const val STORE = "marketplace/store/{id}"
    const val ORDERS = "marketplace/orders"
    const val MY_STORE = "marketplace/mine"

    // Administración, solo personal (feature.admin)
    const val ADMIN = "admin"
    const val ADMIN_RESOURCE = "admin/resource/{resource}"

    val TABS = setOf(HOME, EXPLORE, CONTRACTS, WALLET, PROFILE)

    fun professional(id: String) = "professional/$id"
    fun request(id: String) = "requests/detail/$id"
    fun newRequest(professional: String? = null, offering: String? = null) =
        "requests/new?professional=${professional.orEmpty()}&offering=${offering.orEmpty()}"
    fun contract(id: String) = "contracts/$id"
    fun chat(id: String) = "contracts/$id/chat"
    fun tracking(id: String) = "contracts/$id/tracking"
    fun ticket(id: String) = "support/$id"
    fun legal(doc: String) = "legal/${encodeUrlComponent(doc)}" // terminos | privacidad | cookies | aviso-legal
    fun store(id: String) = "marketplace/store/$id"
    fun adminResource(resource: String) = "admin/resource/$resource"
}
