// ZonesScreen.kt – Interface des zones avec design moderne aligné (Décembre 2025)
package tn.esprit.sansa.ui.screens

import tn.esprit.sansa.R

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.esprit.sansa.ui.theme.SansaTheme
import androidx.compose.runtime.saveable.rememberSaveable
import tn.esprit.sansa.ui.components.CoachMarkTooltip
import tn.esprit.sansa.ui.components.SwipeActionsContainer
import tn.esprit.sansa.ui.components.EmptyState
import tn.esprit.sansa.ui.components.StaggeredItem
import androidx.compose.ui.graphics.toArgb

import tn.esprit.sansa.ui.screens.models.*
import tn.esprit.sansa.ui.viewmodels.ZonesViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.ViewGroup
import androidx.compose.ui.viewinterop.AndroidView
import tn.esprit.sansa.ui.theme.NoorBlue

private val NoorIndigo = Color(0xFF6366F1)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZonesScreen(
    viewModel: ZonesViewModel = viewModel(),
    role: UserRole? = UserRole.CITIZEN,
    modifier: Modifier = Modifier,
    onNavigateToAddZone: () -> Unit = {},
    onNavigateToEditZone: (String) -> Unit = {}
) {
    val zones by viewModel.zones.collectAsState()
    val zoneWeather by viewModel.zoneWeather.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    ZonesMainScreen(
        zones = zones,
        zoneWeather = zoneWeather,
        isLoading = isLoading,
        modifier = modifier,
        onNavigateToAddZone = onNavigateToAddZone,
        onNavigateToEditZone = onNavigateToEditZone,
        onDeleteZone = { zone -> viewModel.deleteZone(zone.id) { viewModel.refresh() } },
        role = role
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZonesMainScreen(
    zones: List<Zone>,
    zoneWeather: Map<String, ZoneWeatherInfo>,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    onNavigateToAddZone: () -> Unit,
    onNavigateToEditZone: (String) -> Unit,
    onDeleteZone: (Zone) -> Unit,
    role: UserRole?
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf<ZoneType?>(null) }
    var selectedStatus by remember { mutableStateOf<ZoneStatus?>(null) }
    var mapExpanded by remember { mutableStateOf(true) }
    var showTutorial by rememberSaveable { mutableStateOf(true) }

    val filteredZones = remember(zones, searchQuery, selectedType, selectedStatus) {
        zones.filter { zone ->
            val matchesSearch = searchQuery.isEmpty() ||
                    zone.id.contains(searchQuery, ignoreCase = true) ||
                    zone.name.contains(searchQuery, ignoreCase = true) ||
                    zone.description.contains(searchQuery, ignoreCase = true)
            val matchesType = selectedType == null || zone.type == selectedType
            val matchesStatus = selectedStatus == null || zone.status == selectedStatus
            matchesSearch && matchesType && matchesStatus
        }.sortedBy { it.name }
    }

    val stats = remember(zones) {
        mapOf(
            "Total" to zones.size,
            "Actives" to zones.count { it.status == ZoneStatus.ACTIVE },
            "En maintenance" to zones.count { it.status == ZoneStatus.MAINTENANCE }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ZonesTopBarModern(stats = stats) },
        floatingActionButton = {
            if (role == UserRole.ADMIN) {
                FloatingActionButton(
                    onClick = onNavigateToAddZone,
                    containerColor = NoorBlue,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nouvelle zone")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            item { ZoneSearchBar(query = searchQuery, onQueryChange = { searchQuery = it }) }

            item {
                AnimatedVisibility(
                    visible = mapExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    ZonesInteractiveMap(zones = filteredZones)
                }
                
                Button(
                    onClick = { mapExpanded = !mapExpanded },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NoorBlue.copy(alpha = 0.1f),
                        contentColor = NoorBlue
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(if (mapExpanded) Icons.Default.Map else Icons.Default.Map, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (mapExpanded) "Masquer la carte" else "Afficher la carte", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            item {
                Text(
                    "Filtrer par type",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(12.dp))
                ZoneTypeFilters(
                    selectedType = selectedType,
                    onTypeSelected = { selectedType = if (selectedType == it) null else it }
                )
            }

            item {
                Text(
                    "Filtrer par statut",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(12.dp))
                ZoneStatusFilters(
                    selectedStatus = selectedStatus,
                    onStatusSelected = { selectedStatus = if (selectedStatus == it) null else it }
                )
            }

            item {
                Text(
                    "${filteredZones.size} zone${if (filteredZones.size != 1) "s" else ""} trouvé${if (filteredZones.size != 1) "es" else "e"}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NoorBlue)
                    }
                }
            } else if (filteredZones.isEmpty()) {
                item {
                    EmptyState(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        icon = Icons.Default.LocationCity,
                        title = "Aucune zone trouvée",
                        description = "Vérifiez vos filtres ou ajoutez une nouvelle zone.",
                        actionLabel = "Nouvelle zone",
                        onActionClick = onNavigateToAddZone,
                        iconColor = NoorIndigo
                    )
                }
            } else {
                itemsIndexed(
                    items = filteredZones,
                    key = { _, zone -> zone.id }
                ) { index, zone ->
                    Box {
                        StaggeredItem(index = index) {
                            SwipeActionsContainer(
                                item = zone,
                                onDelete = { onDeleteZone(zone) },
                                onEdit = if (role == UserRole.ADMIN) { { z -> onNavigateToEditZone(z.id) } } else null
                            ) { item ->
                                ZoneCard(
                                    zone = item,
                                    weatherInfo = zoneWeather[item.id],
                                    onEdit = if (role == UserRole.ADMIN) { { onNavigateToEditZone(item.id) } } else null
                                )
                            }
                        }

                        if (index == 0 && showTutorial && !mapExpanded) {
                            CoachMarkTooltip(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 16.dp)
                                    .offset(x = 16.dp, y = 32.dp),
                                text = "Glissez vers la gauche pour supprimer",
                                onDismiss = { showTutorial = false }
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(100.dp)) }
        }
    }
}

@Composable
fun ZonesInteractiveMap(zones: List<Zone>) {
    val markersJson = remember(zones) {
        zones.map { zone ->
            val radiusMeters = kotlin.math.sqrt((zone.area.takeIf { it > 0 } ?: 0.5) / Math.PI) * 1000
            "{lat: ${zone.latitude}, lng: ${zone.longitude}, name: '${zone.name.replace("'", "\\'")}', type: '${zone.type.displayName}', radius: $radiusMeters, color: '${String.format("#%06X", (0xFFFFFF and zone.type.color.toArgb()))}'}"
        }.joinToString(",")
    }

    val html = """
        <!DOCTYPE html>
        <html>
        <head>
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                #map { height: 100vh; width: 100vw; margin: 0; padding: 0; border-radius: 20px; }
                .leaflet-container { background: #f0f2f5; }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = L.map('map').setView([36.8065, 10.1815], 12);
                L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    attribution: '© OpenStreetMap'
                }).addTo(map);

                var zones = [$markersJson];
                var group = L.featureGroup();
                
                zones.forEach(function(z) {
                    var circle = L.circle([z.lat, z.lng], {
                        color: z.color,
                        fillColor: z.color,
                        fillOpacity: 0.3,
                        radius: z.radius
                    }).addTo(map)
                    .bindPopup("<b>" + z.name + "</b><br>" + z.type);
                    group.addLayer(circle);
                });

                if (zones.length > 0) {
                    map.fitBounds(group.getBounds().pad(0.1));
                }
            </script>
        </body>
        </html>
    """.trimIndent()

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                loadDataWithBaseURL("https://osm.org", html, "text/html", "UTF-8", null)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        update = { webView ->
            webView.loadDataWithBaseURL("https://osm.org", html, "text/html", "UTF-8", null)
        }
    )
}

@Composable
private fun ZonesTopBarModern(stats: Map<String, Int>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(NoorBlue.copy(alpha = 0.95f), NoorBlue.copy(alpha = 0.65f))
                )
            )
            .padding(horizontal = 20.dp, vertical = 28.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Gestion des zones",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                        letterSpacing = 0.4.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Organisation territoriale",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.6).sp
                    )
                }
                IconButton(onClick = { /* TODO: Refresh */ }) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Actualiser",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                stats.forEach { (label, value) ->
                    QuickStatCardCompact(
                        value = value.toString(),
                        label = label,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickStatCardCompact(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.22f)
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.8).sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 11.sp,
                letterSpacing = 0.3.sp
            )
        }
    }
}

@Composable
private fun ZoneSearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Rechercher par ID, nom ou description...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Effacer")
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NoorBlue,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            cursorColor = NoorBlue
        )
    )
}

@Composable
private fun ZoneTypeFilters(
    selectedType: ZoneType?,
    onTypeSelected: (ZoneType) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        ZoneType.entries.forEach { type ->
            val isSelected = selectedType == type
            FilterChip(
                onClick = { onTypeSelected(type) },
                label = { Text(type.displayName) },
                selected = isSelected,
                leadingIcon = {
                    Icon(type.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = type.color,
                    selectedLabelColor = Color.White,
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = type.color
                )
            )
        }
    }
}

@Composable
private fun ZoneStatusFilters(
    selectedStatus: ZoneStatus?,
    onStatusSelected: (ZoneStatus) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        ZoneStatus.entries.forEach { status ->
            val isSelected = selectedStatus == status
            FilterChip(
                onClick = { onStatusSelected(status) },
                label = { Text(status.displayName) },
                selected = isSelected,
                leadingIcon = if (isSelected) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = status.color,
                    selectedLabelColor = Color.White,
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = status.color
                )
            )
        }
    }
}

@Composable
private fun ZoneCard(zone: Zone, weatherInfo: ZoneWeatherInfo? = null, onEdit: (() -> Unit)? = null) {
    var expanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val elevation by animateDpAsState(
        targetValue = if (pressed) 4.dp else 1.dp,
        animationSpec = tween(200)
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = tween(200)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .clickable(interactionSource = interactionSource, indication = null) {
                    expanded = !expanded
                }
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(zone.type.color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        zone.type.icon,
                        contentDescription = null,
                        tint = zone.type.color,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = zone.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Badge(
                            containerColor = zone.status.color,
                            modifier = Modifier.height(22.dp)
                        ) {
                            Text(
                                text = zone.status.displayName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }

                        Badge(
                            containerColor = zone.type.color.copy(alpha = 0.15f),
                            modifier = Modifier.height(22.dp)
                        ) {
                            Text(
                                text = zone.type.displayName,
                                fontSize = 11.sp,
                                color = zone.type.color,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Réduire" else "Développer",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModernStatItem(
                    value = "${zone.associatedStreetlights.size}",
                    label = "Lampadaires",
                    modifier = Modifier.weight(1f)
                )
                ModernStatItem(
                    value = "${zone.activeStreetlights}",
                    label = "Actifs",
                    modifier = Modifier.weight(1f)
                )
                ModernStatItem(
                    value = "${zone.area} km²",
                    label = "Superficie",
                    modifier = Modifier.weight(1f)
                )
            }

            // Weather & Air Quality Widget
            if (weatherInfo != null) {
                Spacer(Modifier.height(16.dp))
                WeatherAirWidget(weatherInfo)
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 14.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    InfoRow(
                        icon = Icons.Default.Description,
                        label = "Description",
                        value = zone.description
                    )

                    Spacer(Modifier.height(10.dp))

                    InfoRow(
                        icon = Icons.Default.Person,
                        label = "Coordinateur",
                        value = zone.coordinator
                    )

                    Spacer(Modifier.height(10.dp))

                    InfoRow(
                        icon = Icons.Default.Groups,
                        label = "Population",
                        value = "${zone.population}"
                    )

                    Spacer(Modifier.height(10.dp))

                    Text("Lampadaires associés", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        zone.associatedStreetlights.forEach { light ->
                            AssistChip(
                                onClick = { /* TODO */ },
                                label = { Text(light, fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { onEdit?.invoke() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Modifier", fontSize = 13.sp)
                        }
                        Button(
                            onClick = { /* TODO */ },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = zone.type.color)
                        ) {
                            Icon(Icons.Default.Visibility, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Détails", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernStatItem(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(0.6f), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun WeatherAirWidget(info: ZoneWeatherInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Weather Part
        Row(verticalAlignment = Alignment.CenterVertically) {
            info.weather?.let { w ->
                Text(
                    text = "${w.temperature.toInt()}°C",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = NoorBlue
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = w.description.replaceFirstChar { it.uppercase() },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(tn.esprit.sansa.R.string.humidity_label, w.humidity),
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            } ?: Text(stringResource(tn.esprit.sansa.R.string.weather_loading), color = Color.Gray, fontSize = 12.sp)
        }

        @Composable
        fun VerticalDivider() {
            Box(
                modifier = Modifier
                    .height(30.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
        
        VerticalDivider()

        // Air Quality Part
        info.airQuality?.let { aq ->
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(aq.color)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(tn.esprit.sansa.R.string.air_label, stringResource(aq.statusResId)),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = aq.color
                    )
                }
                Text(
                    text = stringResource(tn.esprit.sansa.R.string.aqi_label, aq.aqi),
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        } ?: Text(stringResource(tn.esprit.sansa.R.string.air_quality_loading), color = Color.Gray, fontSize = 12.sp)
    }
}

@Preview(showBackground = true, name = "Mode Clair")
@Composable
private fun PreviewZonesLight() {
    SansaTheme(darkTheme = false) {
        ZonesScreen()
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Mode Sombre")
@Composable
private fun PreviewZonesDark() {
    SansaTheme(darkTheme = true) {
        ZonesScreen()
    }
}