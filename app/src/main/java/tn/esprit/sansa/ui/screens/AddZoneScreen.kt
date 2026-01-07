package tn.esprit.sansa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.ViewGroup
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import tn.esprit.sansa.ui.screens.models.*
import tn.esprit.sansa.ui.viewmodels.ZonesViewModel
import tn.esprit.sansa.ui.components.ModernSectionCard
import tn.esprit.sansa.ui.theme.NoorBlue
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.launch

import androidx.compose.ui.tooling.preview.Preview
import tn.esprit.sansa.ui.theme.SansaTheme

private val NoorIndigo = Color(0xFF6366F1)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddZoneScreen(
    editingZoneId: String? = null,
    viewModel: ZonesViewModel = viewModel(),
    onAddSuccess: () -> Unit = {},
    onBackPressed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isEditMode = editingZoneId != null
    var initialZone by remember { mutableStateOf<Zone?>(null) }
    
    var zoneId by remember { mutableStateOf("") }
    var zoneName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var zoneType by remember { mutableStateOf(ZoneType.RESIDENTIAL) }
    var status by remember { mutableStateOf(ZoneStatus.PLANNING) }
    var area by remember { mutableStateOf("") }
    var population by remember { mutableStateOf("") }
    var coordinator by remember { mutableStateOf("") }
    var streetlightIds by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    
    // Coordinates
    var latitude by remember { mutableStateOf(36.8065) }
    var longitude by remember { mutableStateOf(10.1815) }

    val geocodingResults by viewModel.geocodingResults.collectAsState()
    val scope = rememberCoroutineScope()

    // Auto-generate ID on screen load OR load existing data
    LaunchedEffect(editingZoneId) {
        if (isEditMode) {
            val zone = viewModel.getZoneById(editingZoneId!!)
            if (zone != null && initialZone == null) {
                initialZone = zone
                zoneId = zone.id
                zoneName = zone.name
                description = zone.description
                zoneType = zone.type
                status = zone.status
                area = zone.area.toString()
                population = zone.population.toString()
                coordinator = zone.coordinator
                latitude = zone.latitude
                longitude = zone.longitude
            }
        } else {
            if (zoneId.isEmpty()) {
                zoneId = viewModel.generateNextZoneId()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AddZoneTopBar(
                title = if (isEditMode) "Modifier la zone" else "Nouvelle zone",
                onBackPressed = onBackPressed
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ModernSectionCard(
                    title = "Identification",
                    icon = Icons.Default.Tag
                ) {
                    OutlinedTextField(
                        value = zoneId,
                        onValueChange = {},
                        label = { Text("ID de la zone (auto-généré)") },
                        leadingIcon = { Icon(Icons.Default.Tag, null) },
                        trailingIcon = { Icon(Icons.Default.Lock, null, tint = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledBorderColor = NoorBlue.copy(alpha = 0.5f),
                            disabledTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = zoneName,
                        onValueChange = { 
                            zoneName = it
                            viewModel.searchLocation(it)
                        },
                        label = { Text("Nom ou Adresse de la zone") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NoorBlue)
                    )

                    if (geocodingResults.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column {
                                geocodingResults.forEach { result ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { 
                                                zoneName = result.displayName.split(",").first()
                                                latitude = result.lat
                                                longitude = result.lon
                                                viewModel.clearGeocodingResults()
                                            }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Place, null, tint = NoorBlue, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(result.displayName, fontSize = 12.sp, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                ModernSectionCard(
                    title = "Positionnement",
                    icon = Icons.Default.Map
                ) {
                    ZonePreviewMap(lat = latitude, lng = longitude, name = zoneName, color = zoneType.color)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Lat: ${String.format("%.4f", latitude)}", fontSize = 12.sp, color = NoorBlue)
                        Text("Lon: ${String.format("%.4f", longitude)}", fontSize = 12.sp, color = NoorBlue)
                    }
                }
            }

            item {
                ModernSectionCard(
                    title = "Classification",
                    icon = Icons.Default.Category
                ) {
                    Text("Type de zone", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ZoneType.entries) { type ->
                            FilterChip(
                                onClick = { zoneType = type },
                                label = { Text(type.displayName, fontSize = 12.sp) },
                                selected = zoneType == type,
                                leadingIcon = { Icon(type.icon, null, modifier = Modifier.size(16.dp)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = type.color,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Statut", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ZoneStatus.entries) { currentStatus ->
                            FilterChip(
                                onClick = { status = currentStatus },
                                label = { Text(currentStatus.displayName, fontSize = 12.sp) },
                                selected = status == currentStatus,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = currentStatus.color,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            item {
                ModernSectionCard(
                    title = "Détails Techniques",
                    icon = Icons.Default.Description
                ) {
                    OutlinedTextField(
                        value = area,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) area = it },
                        label = { Text("Superficie (km²)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = coordinator,
                        onValueChange = { coordinator = it },
                        label = { Text("Coordinateur") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        val zone = Zone(
                            id = zoneId,
                            name = zoneName,
                            description = description,
                            type = zoneType,
                            status = status,
                            area = area.toDoubleOrNull() ?: 0.0,
                            population = population.toIntOrNull() ?: 0,
                            coordinator = coordinator,
                            latitude = latitude,
                            longitude = longitude,
                            associatedStreetlights = streetlightIds.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        )
                        if (isEditMode) {
                            viewModel.updateZone(zone) { onAddSuccess() }
                        } else {
                            viewModel.addZone(zone) { onAddSuccess() }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    enabled = zoneId.isNotBlank() && zoneName.isNotBlank(),
                    shape = RoundedCornerShape(30.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NoorBlue)
                ) {
                    Icon(if (isEditMode) Icons.Default.Update else Icons.Default.Check, null)
                    Spacer(Modifier.width(12.dp))
                    Text(if (isEditMode) "Enregistrer" else "Créer la zone", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            item { Spacer(Modifier.height(100.dp)) }
        }
    }
}

@Composable
fun ZonePreviewMap(lat: Double, lng: Double, name: String, color: Color = NoorBlue) {
    val hexColor = String.format("#%06X", (0xFFFFFF and color.toArgb()))
    val html = """
        <!DOCTYPE html>
        <html>
        <head>
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                #map { height: 100vh; width: 100vw; margin: 0; padding: 0; border-radius: 12px; }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = L.map('map').setView([$lat, $lng], 15);
                L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);
                var zoneCircle = L.circle([$lat, $lng], {
                    color: '$hexColor',
                    fillColor: '$hexColor',
                    fillOpacity: 0.4,
                    radius: 300
                }).addTo(map).bindPopup('$name');
                
                function updateMap(newLat, newLng, newName) {
                    map.setView([newLat, newLng], 15);
                    zoneCircle.setLatLng([newLat, newLng]).setPopupContent(newName);
                }
            </script>
        </body>
        </html>
    """.trimIndent()

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                loadDataWithBaseURL("https://osm.org", html, "text/html", "UTF-8", null)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp)),
        update = { webView ->
            webView.loadUrl("javascript:updateMap($lat, $lng, '${name.replace("'", "\\'")}')")
        }
    )
}

@Composable
private fun AddZoneTopBar(
    title: String = "Nouvelle zone",
    onBackPressed: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(Brush.verticalGradient(colors = listOf(NoorBlue, NoorBlue.copy(alpha = 0.7f))))
            .padding(horizontal = 24.dp, vertical = 56.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBackPressed,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.2f))
            ) {
                Icon(Icons.Default.ArrowBack, "Retour", tint = Color.White)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, color = Color.White.copy(0.9f), fontSize = 16.sp)
                Text("Configuration urbaine", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddZoneScreenPreview() {
    SansaTheme {
        AddZoneScreen()
    }
}
