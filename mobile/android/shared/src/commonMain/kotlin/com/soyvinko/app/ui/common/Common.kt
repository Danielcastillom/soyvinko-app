package com.soyvinko.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.soyvinko.app.data.AppServices
import com.soyvinko.app.data.ApiException
import com.soyvinko.app.data.User
import com.soyvinko.app.ui.components.PrimaryButton
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Acceso a la API (`LocalApp.current.ops`), al usuario y a la sesión desde cualquier pantalla. */
val LocalApp = staticCompositionLocalOf<AppServices> { error("AppServices no disponible") }
val LocalUser = staticCompositionLocalOf<User> { error("Usuario no disponible") }
val LocalSnackbar = staticCompositionLocalOf<SnackbarHostState> { error("Snackbar no disponible") }

/** Ámbito del shell: los mensajes se muestran aunque la pantalla que los originó ya se haya cerrado. */
val LocalShellScope = staticCompositionLocalOf<CoroutineScope?> { null }

class SessionActions(val reloadUser: () -> Unit, val logout: () -> Unit)
val LocalSession = staticCompositionLocalOf<SessionActions> { error("Sesión no disponible") }

/** Estado de una carga remota con recarga. */
class Resource<T>(val data: T?, val error: String?, val loading: Boolean, val reload: () -> Unit)

/**
 * Carga datos al entrar y cuando cambian las `keys`. Uso:
 * `val wallet = rememberResource { ops.call("money_wallet", "currency" to "VES") }`
 */
@Composable
fun <T> rememberResource(vararg keys: Any?, loader: suspend () -> T): Resource<T> {
    var data by remember(*keys) { mutableStateOf<T?>(null) }
    var error by remember(*keys) { mutableStateOf<String?>(null) }
    var loading by remember(*keys) { mutableStateOf(true) }
    var version by remember { mutableIntStateOf(0) }
    LaunchedEffect(*keys, version) {
        // Siempre en el hilo principal: el estado de la UI no se toca desde hilos de red.
        withContext(Dispatchers.Main.immediate) {
            loading = true
            try {
                data = loader()
                error = null
            } catch (e: ApiException) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }
    return Resource(data, error, loading) { version++ }
}

/** Ejecuta una acción (POST) mostrando progreso y el resultado en el Snackbar. */
class ActionRunner(val busy: Boolean, val run: (success: String?, block: suspend () -> Unit) -> Unit)

@Composable
fun rememberAction(): ActionRunner {
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val messages = LocalShellScope.current ?: scope
    var busy by remember { mutableStateOf(false) }
    return ActionRunner(busy) { success, block ->
        if (!busy) scope.launch(Dispatchers.Main.immediate) {
            busy = true
            try {
                block()
                success?.let { messages.launch { snackbar.showSnackbar(it) } }
            } catch (e: ApiException) {
                messages.launch { snackbar.showSnackbar(e.message) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Respuesta inesperada u otro fallo local: mensaje amable en vez de cerrar la app.
                messages.launch { snackbar.showSnackbar("No se pudo completar la operación. Inténtalo de nuevo.") }
            } finally {
                busy = false
            }
        }
    }
}

/** Pantalla interna con barra superior y botón Volver. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
    floatingAction: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = Vinko.Cream,
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    if (onBack != null) IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = { actions() },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Vinko.Cream),
            )
        },
        floatingActionButton = floatingAction,
        bottomBar = bottomBar,
        content = content,
    )
}

@Composable
fun VCard(modifier: Modifier = Modifier, padding: Dp = 16.dp, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(18.dp))
            .border(1.dp, Vinko.Line, RoundedCornerShape(18.dp))
            .padding(padding),
        content = content,
    )
}

@Composable
fun IconTile(icon: ImageVector, tint: Color = Vinko.Primary, bg: Color = Vinko.Tint, size: Dp = 40.dp) {
    Box(Modifier.size(size).background(bg, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size / 2))
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Vinko.Primary)
    }
}

@Composable
fun ErrorBox(message: String, onRetry: (() -> Unit)? = null) {
    StateBox(Icons.Outlined.CloudOff, "No pudimos cargar esta sección", message) {
        if (onRetry != null) TextButton(onClick = onRetry) { Text("Reintentar", color = Vinko.Primary) }
    }
}

@Composable
fun EmptyBox(title: String, text: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    StateBox(Icons.Outlined.Inbox, title, text) {
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(8.dp))
            PrimaryButton(actionLabel, onClick = onAction)
        }
    }
}

@Composable
private fun StateBox(icon: ImageVector, title: String, text: String, extra: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconTile(icon, size = 56.dp)
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Vinko.Muted, textAlign = TextAlign.Center)
        extra()
    }
}

/** Resource con estados estándar: carga, error con reintento, o contenido. */
@Composable
fun <T> ResourceView(resource: Resource<T>, content: @Composable (T) -> Unit) {
    val data = resource.data
    when {
        data != null -> content(data)
        resource.loading -> LoadingBox()
        else -> ErrorBox(resource.error ?: "Inténtalo de nuevo.", resource.reload)
    }
}

@Composable
fun FullScreen(content: @Composable () -> Unit) = Box(Modifier.fillMaxSize()) { content() }
