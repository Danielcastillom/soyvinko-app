package com.soyvinko.app.ui.feature.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.soyvinko.app.ui.common.EmptyBox
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.LocalUser
import com.soyvinko.app.ui.common.ResourceView
import com.soyvinko.app.ui.common.ScreenScaffold
import com.soyvinko.app.ui.common.VCard
import com.soyvinko.app.ui.common.b
import com.soyvinko.app.ui.common.objects
import com.soyvinko.app.ui.common.rememberResource
import com.soyvinko.app.ui.common.s
import com.soyvinko.app.ui.components.PrimaryButton
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.serialization.json.JsonObject

/** Categorías de servicios (catalog.manage): lista del catálogo y "Guardar categoría" (api.configure_category). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminCategoriesScreen(onBack: () -> Unit) {
    val app = LocalApp.current
    val user = LocalUser.current
    val cats = rememberResource { app.ops.call("catalog").objects() }
    var editing by remember { mutableStateOf<JsonObject?>(null) }
    var creating by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        ScreenScaffold(
            "Categorías de servicios",
            onBack = onBack,
            actions = { IconButton(onClick = cats.reload) { Icon(Icons.Outlined.Refresh, contentDescription = "Actualizar", tint = Vinko.Primary) } },
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                if (!user.can("catalog.manage")) {
                    EmptyBox("Sin acceso a esta sección", "Tu rol no incluye el permiso para gestionar el catálogo.")
                } else ResourceView(cats) { list ->
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(
                            "Categorías activas del catálogo. Toca una para editarla o crea una nueva con su código único.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Vinko.Muted,
                        )
                        PrimaryButton("Guardar categoría", onClick = { creating = true })
                        VCard {
                            Text("${list.size} categorías activas", style = MaterialTheme.typography.titleSmall)
                            FlowRow(
                                Modifier.padding(top = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                list.forEach { c -> ChoiceChip(c.s("name") ?: c.s("slug").orEmpty(), false) { editing = c } }
                            }
                        }
                    }
                }
            }
        }
        if (creating || editing != null) {
            CategorySheet(
                editing, cats.data.orEmpty(),
                onDismiss = { creating = false; editing = null },
                onDone = { creating = false; editing = null; cats.reload() },
            )
        }
    }
}

@Composable
private fun CategorySheet(row: JsonObject?, all: List<JsonObject>, onDismiss: () -> Unit, onDone: () -> Unit) {
    val app = LocalApp.current
    val op = rememberAdminOp()
    var slug by remember { mutableStateOf(row.s("slug").orEmpty()) }
    var name by remember { mutableStateOf(row.s("name").orEmpty()) }
    var parent by remember { mutableStateOf(row.s("parent_id") ?: "") }
    var active by remember { mutableStateOf(row?.b("active") ?: true) }
    var error by remember { mutableStateOf<String?>(null) }
    AdminSheet(
        if (row == null) "Guardar categoría" else "Editar categoría", onDismiss,
        confirmLabel = "Guardar categoría",
        onConfirm = {
            error = null
            op.run("Categoría guardada.", { error = it }, after = onDone) {
                app.ops.call(
                    "configure_category",
                    "slug" to slug.trim(),
                    "name" to name.trim(),
                    "active" to active,
                    "parent" to parent.ifBlank { null },
                )
            }
        },
        busy = op.busy,
        subtitle = "Si el código ya existe se actualiza esa categoría.",
        error = error,
        confirmEnabled = slug.isNotBlank() && name.isNotBlank(),
    ) {
        AdminTextField(slug, { slug = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '-' }.take(60) }, "Código único de categoría", supporting = "Ej.: plomeria-residencial")
        AdminTextField(name, { name = it.take(80) }, "Nombre", capitalization = KeyboardCapitalization.Sentences)
        FieldLabel("Categoría superior")
        ChoiceGroup(
            listOf("" to "Ninguna") + all.filter { it.s("id") != row.s("id") }.mapNotNull { c -> c.s("id")?.let { it to (c.s("name") ?: it) } },
            parent,
        ) { parent = it }
        SwitchRow("Categoría activa", if (active) "Visible en el catálogo." else "Oculta del catálogo.", active) { active = it }
    }
}

