package com.soyvinko.app.ui.feature.profile

import com.soyvinko.app.ui.common.sendEmail
import coil3.PlatformContext
import coil3.compose.LocalPlatformContext
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soyvinko.app.ui.common.ScreenScaffold
import com.soyvinko.app.ui.common.VCard
import com.soyvinko.app.ui.components.SectionLabel
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.coroutines.launch

/** Documento legal portado desde la web (LegalContent.kt). */
data class LegalDoc(
    val slug: String,
    val short: String,
    val eyebrow: String,
    val title: String,
    val description: String,
    val updated: String,
    val status: String?,
    val sections: List<LegalSection>,
)

data class LegalSection(val number: String, val title: String, val blocks: List<LegalBlock>)

/** Bloques de texto; admiten **negrita** y `código`. */
sealed interface LegalBlock {
    data class P(val text: String, val muted: Boolean = false) : LegalBlock
    data class H(val text: String) : LegalBlock
    data class Bullets(val items: List<String>, val ordered: Boolean = false) : LegalBlock
    data class Callout(val text: String) : LegalBlock
    data class Card(val title: String, val text: String) : LegalBlock
    data class Checks(val items: List<String>) : LegalBlock
    data class Table(val headers: List<String>, val rows: List<List<String>>) : LegalBlock
}

private fun docIcon(slug: String) = when (slug) {
    "privacidad" -> Icons.Outlined.PrivacyTip
    "cookies" -> Icons.Outlined.Cookie
    "aviso-legal" -> Icons.Outlined.Gavel
    else -> Icons.AutoMirrored.Outlined.Article
}

@Composable
fun LegalScreen(initial: String?, onBack: () -> Unit) {
    var slug by rememberSaveable { mutableStateOf(LEGAL_DOCS.firstOrNull { it.slug == initial }?.slug ?: LEGAL_DOCS.first().slug) }
    val doc = LEGAL_DOCS.first { it.slug == slug }
    ScreenScaffold("Legales", onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LEGAL_DOCS.forEach { d ->
                    val selected = d.slug == slug
                    FilterChip(
                        selected = selected,
                        onClick = { slug = d.slug },
                        label = { Text(d.short) },
                        leadingIcon = { Icon(docIcon(d.slug), contentDescription = null, modifier = Modifier.size(18.dp)) },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.White,
                            selectedContainerColor = Vinko.Primary,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White,
                            iconColor = Vinko.Primary,
                            labelColor = Vinko.Ink,
                        ),
                        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected, borderColor = Vinko.Line, selectedBorderColor = Vinko.Primary),
                    )
                }
            }
            key(slug) { LegalDocument(doc) }
        }
    }
}

@Composable
private fun LegalDocument(doc: LegalDoc) {
    val state = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalPlatformContext.current
    LazyColumn(
        state = state,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "head") {
            Column {
                Text(doc.eyebrow, style = MaterialTheme.typography.labelSmall, color = Vinko.Primary)
                Spacer(Modifier.height(8.dp))
                Text(doc.title, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text(doc.description, style = MaterialTheme.typography.bodyMedium, color = Vinko.Muted)
                Spacer(Modifier.height(10.dp))
                Text("Última actualización: ${doc.updated}", style = MaterialTheme.typography.labelMedium, color = Vinko.Subtle)
                doc.status?.let {
                    Spacer(Modifier.height(8.dp))
                    Pill(it, Vinko.Mint, Vinko.MintBg)
                }
            }
        }
        item(key = "toc") {
            VCard(padding = 14.dp) {
                SectionLabel("Índice")
                Spacer(Modifier.height(6.dp))
                doc.sections.forEachIndexed { i, s ->
                    Row(
                        Modifier.fillMaxWidth().clickable { scope.launch { state.animateScrollToItem(i + 2) } }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(s.number, style = MaterialTheme.typography.labelMedium, color = Vinko.Primary, modifier = Modifier.width(28.dp))
                        Text(s.title, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        itemsIndexed(doc.sections, key = { i, s -> "s$i-${s.number}" }) { _, section ->
            VCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(34.dp).background(Vinko.Tint, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                        Text(section.number, style = MaterialTheme.typography.labelLarge, color = Vinko.Primary)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(section.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    section.blocks.forEach { LegalBlockView(it, context) }
                }
            }
        }
        item(key = "foot") {
            Text(
                rich("¿Tienes preguntas sobre estos documentos? Escríbenos a contacto@soyvinko.com.", context),
                style = MaterialTheme.typography.bodySmall,
                color = Vinko.Muted,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun LegalBlockView(block: LegalBlock, context: PlatformContext) {
    val body = MaterialTheme.typography.bodyMedium
    when (block) {
        is LegalBlock.P -> Text(rich(block.text, context), style = if (block.muted) MaterialTheme.typography.bodySmall else body, color = if (block.muted) Vinko.Muted else Vinko.Ink)
        is LegalBlock.H -> Text(block.text, style = MaterialTheme.typography.titleSmall, color = Vinko.Primary)
        is LegalBlock.Bullets -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            block.items.forEachIndexed { i, item ->
                Row(verticalAlignment = Alignment.Top) {
                    if (block.ordered) {
                        Text("${i + 1}.", style = body, color = Vinko.Primary, fontWeight = FontWeight.Bold, modifier = Modifier.width(22.dp))
                    } else {
                        Box(Modifier.padding(top = 8.dp, end = 10.dp).size(6.dp).background(Vinko.Lavender, CircleShape))
                    }
                    Text(rich(item, context), style = body, modifier = Modifier.weight(1f))
                }
            }
        }
        is LegalBlock.Callout -> Row(
            Modifier.fillMaxWidth().background(Vinko.Tint, RoundedCornerShape(14.dp)).padding(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = Vinko.Primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(rich(block.text, context), style = body)
        }
        is LegalBlock.Card -> Column(
            Modifier.fillMaxWidth().background(Vinko.Cream, RoundedCornerShape(14.dp)).border(1.dp, Vinko.Line, RoundedCornerShape(14.dp)).padding(14.dp),
        ) {
            Text(block.title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(rich(block.text, context), style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
        }
        is LegalBlock.Checks -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            block.items.forEach { item ->
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Vinko.Mint, modifier = Modifier.padding(top = 2.dp).size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(rich(item, context), style = body)
                }
            }
        }
        is LegalBlock.Table -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // En pantallas de teléfono, cada fila de la tabla se muestra como una ficha.
            block.rows.forEach { row ->
                Column(
                    Modifier.fillMaxWidth().background(Vinko.Cream, RoundedCornerShape(14.dp)).border(1.dp, Vinko.Line, RoundedCornerShape(14.dp)).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(rich(row.firstOrNull().orEmpty(), context), style = MaterialTheme.typography.titleSmall)
                    row.drop(1).forEachIndexed { i, cell ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                block.headers.getOrNull(i + 1).orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = Vinko.Muted,
                                modifier = Modifier.weight(0.4f),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(rich(cell, context), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(0.6f))
                        }
                    }
                }
            }
        }
    }
}

private val INLINE = Regex("\\*\\*(.+?)\\*\\*|`([^`]+)`")
private val EMAIL = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")

/** **negrita**, `código` y correos que abren la app de correo (mailto). */
fun rich(text: String, context: PlatformContext?): AnnotatedString = buildAnnotatedString {
    fun plain(segment: String) {
        var last = 0
        EMAIL.findAll(segment).forEach { m ->
            append(segment.substring(last, m.range.first))
            val email = m.value
            if (context != null) {
                withLink(
                    LinkAnnotation.Clickable(
                        tag = email,
                        styles = TextLinkStyles(SpanStyle(color = Vinko.Primary, fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline)),
                        linkInteractionListener = { openMail(context, email) },
                    ),
                ) { append(email) }
            } else {
                append(email)
            }
            last = m.range.last + 1
        }
        append(segment.substring(last))
    }
    var index = 0
    INLINE.findAll(text).forEach { m ->
        plain(text.substring(index, m.range.first))
        val bold = m.groups[1]?.value
        if (bold != null) {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { plain(bold) }
        } else {
            withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Vinko.Tint, color = Vinko.Primary, fontSize = 13.sp)) {
                append(m.groups[2]?.value.orEmpty())
            }
        }
        index = m.range.last + 1
    }
    plain(text.substring(index))
}

private fun openMail(context: PlatformContext, email: String) {
    // Sin app de correo instalada no pasa nada: el texto sigue visible para copiarlo.
    sendEmail(context, email)
}
