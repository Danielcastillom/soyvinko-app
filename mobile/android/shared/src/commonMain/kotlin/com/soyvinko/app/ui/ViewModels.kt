package com.soyvinko.app.ui

import com.soyvinko.app.ui.common.isValidEmail
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.soyvinko.app.AppGraph
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.soyvinko.app.data.ApiException
import com.soyvinko.app.data.AuthRepository
import com.soyvinko.app.data.Dashboard
import com.soyvinko.app.data.LoginOutcome
import com.soyvinko.app.data.RegisterRequest
import com.soyvinko.app.data.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private fun isEmail(value: String) = isValidEmail(value)

/** Fábrica común: todos los ViewModels obtienen los servicios de [AppGraph]. */
val AppViewModels: ViewModelProvider.Factory = viewModelFactory {
    initializer { AppGraph.services.let { app -> SessionViewModel(app.auth).also { vm -> app.api.onSessionExpired = { vm.onExpired() } } } }
    initializer { LoginViewModel(AppGraph.services.auth) }
    initializer { RegisterViewModel(AppGraph.services.auth) }
    initializer { ForgotViewModel(AppGraph.services.auth) }
}

sealed interface SessionState {
    data object Loading : SessionState
    data object SignedOut : SessionState
    data class SignedIn(val user: User) : SessionState
}

class SessionViewModel(private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow<SessionState>(SessionState.Loading)
    val state: StateFlow<SessionState> = _state.asStateFlow()
    var expiredNotice by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            _state.value = auth.restore()?.let { SessionState.SignedIn(it) } ?: SessionState.SignedOut
        }
    }

    fun signedIn(user: User) {
        expiredNotice = false
        _state.value = SessionState.SignedIn(user)
    }

    fun updateUser(user: User) {
        if (_state.value is SessionState.SignedIn) _state.value = SessionState.SignedIn(user)
    }

    fun reloadUser() {
        viewModelScope.launch { runCatching { auth.me() }.getOrNull()?.let(::updateUser) }
    }

    fun logout() = viewModelScope.launch {
        auth.logout()
        _state.value = SessionState.SignedOut
    }

    fun onExpired() {
        if (_state.value is SessionState.SignedIn) {
            expiredNotice = true
            _state.value = SessionState.SignedOut
        }
    }
}

class LoginViewModel(private val auth: AuthRepository) : ViewModel() {
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var code by mutableStateOf("")
    var step by mutableStateOf(Step.Credentials)
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var fieldErrors by mutableStateOf<Map<String, String>>(emptyMap())
        private set
    var info by mutableStateOf<String?>(null)
        private set
    var resendIn by mutableStateOf(0)
        private set

    val isBiometricAvailable: Boolean get() = auth.isBiometricEnabled()
    val biometricEmail: String? get() = auth.biometricEmail()

    init {
        auth.biometricEmail()?.let { savedEmail ->
            if (savedEmail.isNotBlank()) email = savedEmail
        }
    }

    fun loginWithBiometrics(onLoggedIn: (User) -> Unit) {
        run {
            val user = auth.loginWithBiometrics()
            onLoggedIn(user)
        }
    }

    enum class Step { Credentials, Code }

    fun submitCredentials(onLoggedIn: (User) -> Unit) {
        val errors = buildMap {
            if (!isEmail(email)) put("email", "Escribe un correo válido.")
            if (password.isEmpty()) put("password", "Escribe tu contraseña.")
        }
        fieldErrors = errors
        if (errors.isNotEmpty()) return
        run {
            when (val outcome = auth.login(email, password)) {
                is LoginOutcome.CodeRequired -> {
                    info = outcome.message
                    code = ""
                    step = Step.Code
                    startResendTimer()
                }
                is LoginOutcome.LoggedIn -> onLoggedIn(outcome.user)
            }
        }
    }

    fun submitCode(onLoggedIn: (User) -> Unit) {
        if (code.length != 6 || loading) return
        run {
            when (val outcome = auth.login(email, password, code)) {
                is LoginOutcome.LoggedIn -> {
                    password = ""
                    onLoggedIn(outcome.user)
                }
                is LoginOutcome.CodeRequired -> error = "El código no es válido. Revisa tu correo."
            }
        }
    }

    fun resend() {
        if (resendIn > 0) return
        run {
            info = auth.resendCode(email, password)
            code = ""
            startResendTimer()
        }
    }

    fun back() {
        step = Step.Credentials
        code = ""
        error = null
        info = null
    }

    private fun startResendTimer() = viewModelScope.launch {
        resendIn = 60
        while (resendIn > 0) {
            delay(1000)
            resendIn--
        }
    }

    private fun run(block: suspend () -> Unit) {
        if (loading) return
        viewModelScope.launch {
            loading = true
            error = null
            try {
                block()
            } catch (e: ApiException) {
                fieldErrors = e.fieldErrors
                error = if (e.fieldErrors.isEmpty()) e.message else null
                if (step == Step.Code) code = ""
            } finally {
                loading = false
            }
        }
    }
}

class RegisterViewModel(private val auth: AuthRepository) : ViewModel() {
    var accountType by mutableStateOf("natural")
    var name by mutableStateOf("")
    var businessName by mutableStateOf("")
    var rif by mutableStateOf("")
    var contactName by mutableStateOf("")
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var confirm by mutableStateOf("")
    var acceptedTerms by mutableStateOf(false)
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var fieldErrors by mutableStateOf<Map<String, String>>(emptyMap())
        private set

    val isCompany get() = accountType == "juridico"

    fun submit(onRegistered: (User) -> Unit) {
        val rifPattern = Regex("^[JGCVEjgcve]-?[0-9]{7,9}-?[0-9]?$")
        val errors = buildMap {
            if (isCompany) {
                if (businessName.trim().length < 2) put("business_name", "Indica la razón social.")
                if (!rifPattern.matches(rif.trim())) put("rif", "RIF válido, por ejemplo J-12345678-9.")
                if (contactName.trim().length < 2) put("name", "Indica el nombre de la persona de contacto.")
            } else if (name.trim().length < 2) put("name", "Escribe tu nombre y apellido.")
            if (!isEmail(email)) put("email", "Escribe un correo válido.")
            if (password.length < 12) put("password", "Usa al menos 12 caracteres.")
            if (confirm != password) put("confirm_password", "Las contraseñas no coinciden.")
        }
        fieldErrors = errors
        error = if (!acceptedTerms) "Acepta los términos y la política de privacidad para continuar." else null
        if (errors.isNotEmpty() || !acceptedTerms || loading) return
        viewModelScope.launch {
            loading = true
            try {
                val user = auth.register(
                    RegisterRequest(
                        email = email.trim(),
                        password = password,
                        confirmPassword = confirm,
                        // En empresas, el servidor usa la razón social como nombre visible.
                        name = if (isCompany) contactName.trim() else name.trim(),
                        accountType = accountType,
                        businessName = businessName.trim().takeIf { isCompany },
                        rif = rif.trim().uppercase().takeIf { isCompany },
                        contactName = contactName.trim().takeIf { isCompany },
                    ),
                )
                onRegistered(user)
            } catch (e: ApiException) {
                fieldErrors = e.fieldErrors
                error = if (e.fieldErrors.isEmpty()) e.message else "Revisa los campos marcados."
            } finally {
                loading = false
            }
        }
    }
}

class ForgotViewModel(private val auth: AuthRepository) : ViewModel() {
    var email by mutableStateOf("")
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var sent by mutableStateOf<String?>(null)
        private set

    fun submit() {
        if (!isEmail(email)) {
            error = "Escribe un correo válido."
            return
        }
        viewModelScope.launch {
            loading = true
            error = null
            try {
                sent = auth.forgotPassword(email)
            } catch (e: ApiException) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }
}
