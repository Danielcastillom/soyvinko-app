package com.soyvinko.app.ui.feature.explore

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Architecture
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FormatPaint
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Handyman
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material.icons.outlined.Yard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.soyvinko.app.ui.components.HomeSearchPill
import com.soyvinko.app.ui.shell.Routes
import com.soyvinko.app.ui.theme.Manrope
import com.soyvinko.app.ui.theme.Vinko

data class Rubro(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
)

data class MainCategory(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val bg: Color,
    val rubros: List<Rubro>,
)

/**
 * Pantalla de Categorías y Rubros oficial de SoyVinko:
 * Organizada en las 3 categorías principales (Profesionales, Técnicos y Generales)
 * con menús desglosables (acordeones) para cada rubro.
 */
@Composable
fun CategoriesScreen(nav: NavHostController) {
    var searchQuery by remember { mutableStateOf("") }
    val expandedStates = remember { mutableStateMapOf("Profesionales" to true, "Técnicos" to true, "Generales" to true) }

    val categories = remember {
        listOf(
            MainCategory(
                title = "Profesionales",
                description = "Especialistas titulados y servicios profesionales calificados",
                icon = Icons.Outlined.Work,
                color = Color(0xFF6F3DFF),
                bg = Color(0xFFF0EBFF),
                rubros = listOf(
                    Rubro("arquitectura", "Arquitectura / Diseño de Interiores", "Diseño de planos y supervisión de obras", Icons.Outlined.Architecture),
                    Rubro("community", "Community Manager / Especialista en Redes", "Gestión de comunidades y estrategia digital", Icons.Outlined.Brush),
                    Rubro("comunicaciones", "Comunicaciones / Relaciones Públicas", "Prensa, comunicación corporativa y vocería", Icons.Outlined.BusinessCenter),
                    Rubro("contabilidad", "Contabilidad / Administración", "Declaraciones fiscales, balances y asesoría contable", Icons.Outlined.BusinessCenter),
                    Rubro("desarrollo", "Desarrollo de Software / Programación", "Sitios web, apps móviles y sistemas a medida", Icons.Outlined.Code),
                    Rubro("diseno-grafico", "Diseño Gráfico", "Branding, identidad visual y piezas publicitarias", Icons.Outlined.Brush),
                    Rubro("diseno-web", "Diseño Web", "Interfaces UI/UX y sitios web corporativos", Icons.Outlined.Computer),
                    Rubro("fotografia", "Fotografía", "Sesiones fotográficas, productos y eventos", Icons.Outlined.Work),
                    Rubro("ing-ambiental", "Ingeniería Ambiental", "Estudios de impacto y gestión ambiental", Icons.Outlined.Yard),
                    Rubro("ing-biomedica", "Ingeniería Biomédica", "Equipamiento médico y tecnología en salud", Icons.Outlined.Build),
                    Rubro("ing-civil", "Ingeniería Civil", "Cálculo estructural y dirección de proyectos", Icons.Outlined.Construction),
                    Rubro("ing-telecom", "Ingeniería de Telecomunicaciones", "Redes, conectividad y sistemas de transmisión", Icons.Outlined.Bolt),
                    Rubro("ing-electrica", "Ingeniería Eléctrica", "Diseño de redes eléctricas y tableros", Icons.Outlined.Bolt),
                    Rubro("ing-sistemas", "Ingeniería en Sistemas", "Arquitectura de software y optimización IT", Icons.Outlined.Computer),
                    Rubro("ing-industrial", "Ingeniería Industrial", "Procesos, logística y optimización de plantas", Icons.Outlined.Settings),
                    Rubro("ing-mecanica", "Ingeniería Mecánica", "Diseño mecánico y sistemas térmicos", Icons.Outlined.Build),
                    Rubro("legal", "Legal", "Asesoría jurídica, mercantil, civil y laboral", Icons.Outlined.Gavel),
                    Rubro("medicina", "Medicina", "Consultas médicas especializadas y salud", Icons.Outlined.Person),
                    Rubro("mercadeo", "Mercadeo", "Estrategias de mercado, lanzamientos y growth", Icons.Outlined.BusinessCenter),
                    Rubro("nutricionista", "Nutricionista", "Planes de alimentación y salud nutricional", Icons.Outlined.Person),
                    Rubro("odontologia", "Odontología", "Salud bucal y tratamientos dentales", Icons.Outlined.Person),
                    Rubro("psicologia", "Psicología", "Terapia y apoyo psicológico profesional", Icons.Outlined.Person),
                    Rubro("publicidad", "Publicidad", "Campañas publicitarias y pauta comercial", Icons.Outlined.Brush),
                    Rubro("tutoria", "Tutoría / Clases Particulares", "Apoyo académico y materias especializadas", Icons.Outlined.Work),
                    Rubro("videografia", "Videografía / Edición de Video", "Producción audiovisual y postproducción", Icons.Outlined.Work),
                ),
            ),
            MainCategory(
                title = "Técnicos",
                description = "Servicios técnicos especializados, instalaciones y reparaciones",
                icon = Icons.Outlined.Build,
                color = Color(0xFF0284C7),
                bg = Color(0xFFEBF8FF),
                rubros = listOf(
                    Rubro("aire", "Aire Acondicionado / Refrigeración", "Mantenimiento, gas, instalación y reparación", Icons.Outlined.Construction),
                    Rubro("albanileria-tec", "Albañilería / Construcción", "Construcción, reparaciones y acabados", Icons.Outlined.Construction),
                    Rubro("carpinteria", "Carpintería", "Muebles a medida, puertas y reparaciones", Icons.Outlined.Handyman),
                    Rubro("cerrajeria", "Cerrajería", "Aperturas, cambio de cerraduras y seguridad", Icons.Outlined.Key),
                    Rubro("computadoras", "Computadoras / Celulares", "Reparación de equipos y dispositivos móviles", Icons.Outlined.Computer),
                    Rubro("electricidad", "Electricidad", "Instalaciones, cableado y emergencias eléctricas", Icons.Outlined.Bolt),
                    Rubro("fumigacion", "Fumigación / Control de Plagas", "Control de plagas y desinfección", Icons.Outlined.CleaningServices),
                    Rubro("herreria", "Herrería / Soldadura", "Estructuras metálicas, portones y reparaciones", Icons.Outlined.Build),
                    Rubro("linea-blanca", "Línea Blanca", "Reparación de lavadoras, neveras y secadoras", Icons.Outlined.Build),
                    Rubro("mecanica", "Mecánica Automotríz", "Diagnóstico, frenos, motor y afinación", Icons.Outlined.Handyman),
                    Rubro("pintores", "Pintores de Interiores / Exteriores", "Pintura y acabados en paredes y techos", Icons.Outlined.FormatPaint),
                    Rubro("plomeria", "Plomería / Fontanería", "Reparación de fugas, destapes y tuberías", Icons.Outlined.Build),
                ),
            ),
            MainCategory(
                title = "Generales",
                description = "Oficios generales, asistencia y servicios cotidianos",
                icon = Icons.Outlined.CleaningServices,
                color = Color(0xFF0D9488),
                bg = Color(0xFFF0FDFA),
                rubros = listOf(
                    Rubro("chofer", "Chofer", "Servicios de conducción particular o empresarial", Icons.Outlined.LocalShipping),
                    Rubro("cisterna", "Cisterna", "Suministro y transporte de agua", Icons.Outlined.LocalShipping),
                    Rubro("adultos-mayores", "Cuidado de Adultos Mayores", "Asistencia y acompañamiento geriátrico", Icons.Outlined.Person),
                    Rubro("ninyera", "Cuidado de Niños (Niñera)", "Cuidado infantil y atención en casa", Icons.Outlined.Person),
                    Rubro("grua", "Grúa", "Servicio de remolque y traslado vehicular", Icons.Outlined.LocalShipping),
                    Rubro("jardineria", "Jardinería", "Poda, paisajismo y mantenimiento de áreas verdes", Icons.Outlined.Yard),
                    Rubro("lavado-autos", "Lavado de Autos", "Limpieza y detailing vehicular a domicilio", Icons.Outlined.CleaningServices),
                    Rubro("limpieza", "Limpieza", "Limpieza profunda del hogar u oficinas", Icons.Outlined.CleaningServices),
                    Rubro("mandados", "Mandados", "Gestión de diligencias y compras", Icons.Outlined.Work),
                    Rubro("manicura", "Manicura", "Cuidado de manos y pies a domicilio", Icons.Outlined.Person),
                    Rubro("mudanzas", "Mudanzas / Fletes", "Transporte de cargas, encomiendas y traslados", Icons.Outlined.LocalShipping),
                    Rubro("paseo-mascotas", "Paseo de Mascotas", "Cuidado y paseo canino", Icons.Outlined.Yard),
                    Rubro("peluqueria", "Peluquería", "Corte, estilismo y belleza personal", Icons.Outlined.Person),
                    Rubro("zapatero", "Zapatero", "Reparación de calzado y marroquinería", Icons.Outlined.Handyman),
                ),
            ),
        )
    }

    val filteredCategories = remember(searchQuery) {
        if (searchQuery.isBlank()) categories
        else {
            categories.mapNotNull { cat ->
                val matchingRubros = cat.rubros.filter {
                    it.name.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true)
                }
                if (matchingRubros.isNotEmpty()) cat.copy(rubros = matchingRubros) else null
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Vinko.Cream)
            .statusBarsPadding(),
    ) {
        // Barra superior
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver", tint = Vinko.Ink)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Categorías y Rubros",
                style = MaterialTheme.typography.titleLarge,
                fontFamily = Manrope,
                fontWeight = FontWeight.ExtraBold,
                color = Vinko.Ink,
            )
        }

        // Buscador interactivo
        Box(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
            HomeSearchPill(
                placeholder = "¿Qué servicio necesitas hoy?",
                onClick = { nav.navigate(Routes.EXPLORE) },
            )
        }

        Spacer(Modifier.height(10.dp))

        // Lista de categorías con menús desglosables (acordeón)
        LazyColumn(
            modifier = Modifier.fillMaxSize().weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(filteredCategories, key = { it.title }) { category ->
                val expanded = expandedStates[category.title] ?: true

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Cabecera de la categoría desplegable
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedStates[category.title] = !expanded }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(category.bg, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(category.icon, contentDescription = category.title, tint = category.color, modifier = Modifier.size(24.dp))
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = category.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Vinko.Ink,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "${category.rubros.size} rubros",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Vinko.Muted,
                                )
                            }
                            IconButton(onClick = { expandedStates[category.title] = !expanded }) {
                                Icon(
                                    imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                    contentDescription = if (expanded) "Contraer" else "Expandir",
                                    tint = Vinko.Primary,
                                )
                            }
                        }

                        // Menú desglosable (rubros)
                        AnimatedVisibility(
                            visible = expanded,
                            enter = expandVertically(),
                            exit = shrinkVertically(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                category.rubros.forEach { rubro ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Vinko.FieldBg,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                nav.previousBackStackEntry?.savedStateHandle?.set("selected_category", rubro.id)
                                                nav.popBackStack()
                                            },
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(Color.White, CircleShape),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(rubro.icon, contentDescription = null, tint = category.color, modifier = Modifier.size(18.dp))
                                            }
                                            Spacer(Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = rubro.name,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp,
                                                    color = Vinko.Ink,
                                                )
                                                Spacer(Modifier.height(1.dp))
                                                Text(
                                                    text = rubro.description,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontSize = 11.sp,
                                                    color = Vinko.Muted,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                            Icon(
                                                Icons.AutoMirrored.Outlined.ArrowForward,
                                                contentDescription = null,
                                                tint = Vinko.Primary,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Barra inferior: Botón para solicitar trabajo personalizado
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .border(1.dp, Vinko.Line)
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            Button(
                onClick = { nav.navigate(Routes.newRequest()) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Vinko.Primary),
            ) {
                Text(
                    text = "Publicar Solicitud Personalizada",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
