package tn.esprit.sansa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.tooling.preview.Preview
import tn.esprit.sansa.ui.theme.SansaTheme
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import tn.esprit.sansa.ui.screens.models.*
import tn.esprit.sansa.ui.viewmodels.StreetlightsViewModel
import tn.esprit.sansa.ui.components.ModernSectionCard
import androidx.compose.ui.graphics.toArgb
import android.webkit.JavascriptInterface
import kotlinx.coroutines.launch

import tn.esprit.sansa.ui.theme.*
import tn.esprit.sansa.ui.utils.QRCodeGenerator
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import tn.esprit.sansa.data.services.GeocodingService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStreetlightScreen(
    editingStreetlightId: String? = null,
    viewModel: StreetlightsViewModel = viewModel(),
    onAddSuccess: () -> Unit = {},
    onBackPressed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isEditMode = editingStreetlightId != null
    var streetlightId by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("Sélectionnez un emplacement sur la carte...") }
    var selectedZone by remember { mutableStateOf<Zone?>(null) }
    var latitude by remember { mutableStateOf(36.8065) }
    var longitude by remember { mutableStateOf(10.1815) }
    var bulbType by remember { mutableStateOf(BulbType.LED) }
    var status by remember { mutableStateOf(StreetlightStatus.ON) }
    var powerConsumption by remember { mutableStateOf("45") }
    var installationDate by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var showQRDialog by remember { mutableStateOf(false) }
    var generatedQRBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isLoadingAddress by remember { mutableStateOf(false) }
    var hasCharger by remember { mutableStateOf(false) }

    val zones by viewModel.zones.collectAsState()
    val scope = rememberCoroutineScope()

    // Auto-generate ID on screen load OR load existing data
    var initialStreetlight by remember { mutableStateOf<Streetlight?>(null) }

    LaunchedEffect(editingStreetlightId, zones) {
        if (isEditMode) {
            val streetlight = viewModel.getStreetlightById(editingStreetlightId!!)
            if (streetlight != null) {
                // Initialize state only once
                if (initialStreetlight == null) {
                    initialStreetlight = streetlight
                    streetlightId = streetlight.id
                    address = streetlight.address
                    latitude = streetlight.latitude
                    longitude = streetlight.longitude
                    bulbType = streetlight.bulbType
                    status = streetlight.status
                    powerConsumption = streetlight.powerConsumption.toString()
                    hasCharger = streetlight.hasCharger
                }

                // Try to resolve zone whenever zones or streetlight are available
                if (selectedZone == null) {
                    selectedZone = zones.find { it.id == streetlight.zoneId }
                }
            }
        } else {
            if (streetlightId.isEmpty()) {
                streetlightId = viewModel.generateNextStreetlightId()
            }
        }
    }

    val hasChanges by remember(
        initialStreetlight, address, latitude, longitude, bulbType, status,
        powerConsumption, selectedZone, hasCharger
    ) {
        derivedStateOf {
            if (!isEditMode) return@derivedStateOf true
            val initial = initialStreetlight ?: return@derivedStateOf false

            val currentConsumption = powerConsumption.toDoubleOrNull() ?: 0.0
            
            initial.address != address ||
            initial.latitude != latitude ||
            initial.longitude != longitude ||
            initial.bulbType != bulbType ||
            initial.status != status ||
            initial.powerConsumption != currentConsumption ||
            initial.zoneId != selectedZone?.id ||
            initial.hasCharger != hasCharger
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AddStreetlightTopBar(
                title = if (isEditMode) "Modifier Lampadaire" else "Nouveau Lampadaire",
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
            // General Info
            item {
                ModernSectionCard(title = "Informations Générales", icon = Icons.Default.Info) {
                    OutlinedTextField(
                        value = streetlightId,
                        onValueChange = {},
                        label = { Text("ID du lampadaire (auto-généré)") },
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
                    
                    Spacer(Modifier.height(16.dp))
                    Text("Type d'ampoule", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                        items(BulbType.entries) { type ->
                            FilterChip(
                                onClick = { bulbType = type },
                                label = { Text(type.displayName) },
                                selected = bulbType == type,
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NoorBlue, selectedLabelColor = Color.White)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text("Statut", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                        items(StreetlightStatus.entries) { s ->
                            FilterChip(
                                onClick = { status = s },
                                label = { Text(s.displayName) },
                                selected = status == s,
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = s.color, selectedLabelColor = Color.White)
                            )
                        }
                    }
                }
            }

            // Zone & Map
            item {
                ModernSectionCard(title = "Zone & Localisation", icon = Icons.Default.Map) {
                    Text("1. Choisir la zone", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                        items(zones) { zone ->
                            FilterChip(
                                onClick = { 
                                    selectedZone = zone 
                                    latitude = zone.latitude
                                    longitude = zone.longitude
                                },
                                label = { Text(zone.name) },
                                selected = selectedZone?.id == zone.id,
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NoorIndigo, selectedLabelColor = Color.White)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("2. Marquer l'emplacement exact", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    
                    StreetlightMarkingMap(
                        lat = latitude,
                        lng = longitude,
                        address = address,
                        statusColor = status.color,
                        onLocationChanged = { lat, lng ->
                            latitude = lat
                            longitude = lng
                            // Fetch address automatically
                            isLoadingAddress = true
                            scope.launch {
                                address = GeocodingService.getAddressFromCoordinates(lat, lng)
                                isLoadingAddress = false
                            }
                        }
                    )
                    
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Lat: ${String.format("%.5f", latitude)}", fontSize = 11.sp, color = NoorBlue)
                        Text("Lon: ${String.format("%.5f", longitude)}", fontSize = 11.sp, color = NoorBlue)
                    }

                }
            }

            // Consumption & Extra
            item {
                ModernSectionCard(title = "Détails Techniques", icon = Icons.Default.Settings) {
                    OutlinedTextField(
                        value = powerConsumption,
                        onValueChange = { powerConsumption = it },
                        label = { Text("Consommation (Watts)") },
                        leadingIcon = { Icon(Icons.Default.Power, null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (optionnel)") },
                        leadingIcon = { Icon(Icons.Default.Note, null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            // Noor Charge Toggle
            item {
                ModernSectionCard(title = "Options Noor Charge", icon = Icons.Default.ElectricBolt) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (hasCharger) NoorBlue.copy(alpha = 0.1f) else Color.Transparent)
                            .border(1.dp, if (hasCharger) NoorBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            .clickable { hasCharger = !hasCharger }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (hasCharger) NoorBlue else Color.Gray.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Power,
                                null,
                                tint = if (hasCharger) Color.White else Color.Gray
                            )
                        }
                        
                        Spacer(Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Borne de recharge",
                                fontWeight = FontWeight.Bold,
                                color = if (hasCharger) NoorBlue else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Équiper ce lampadaire d'un hub de recharge",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Switch(
                            checked = hasCharger,
                            onCheckedChange = { hasCharger = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NoorBlue
                            )
                        )
                    }
                }
            }

            // Submit
            item {
                Button(
                    onClick = {
                        val existing = if (isEditMode) viewModel.getStreetlightById(editingStreetlightId!!) else null
                        val streetlight = existing?.copy(
                            address = address,
                            latitude = latitude,
                            longitude = longitude,
                            bulbType = bulbType,
                            status = status,
                            powerConsumption = powerConsumption.toDoubleOrNull() ?: 0.0,
                            zoneId = selectedZone?.id ?: "",
                            hasCharger = hasCharger,
                            chargerStatus = if (hasCharger) (existing.chargerStatus ?: "AVAILABLE") else "IDLE"
                        ) ?: Streetlight(
                            id = streetlightId,
                            address = address,
                            latitude = latitude,
                            longitude = longitude,
                            bulbType = bulbType,
                            status = status,
                            powerConsumption = powerConsumption.toDoubleOrNull() ?: 0.0,
                            zoneId = selectedZone?.id ?: "",
                            hasCharger = hasCharger,
                            chargerStatus = if (hasCharger) "AVAILABLE" else "IDLE"
                        )
                        if (isEditMode) {
                            viewModel.updateStreetlight(streetlight) { success ->
                                if (success) onAddSuccess()
                            }
                        } else {
                            viewModel.addStreetlight(streetlight) { success ->
                                if (success) {
                                    generatedQRBitmap = QRCodeGenerator.generateQRCode(streetlightId)
                                    showQRDialog = true
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NoorIndigo),
                    shape = RoundedCornerShape(16.dp),
                    enabled = (if (isEditMode) hasChanges else true) && 
                            streetlightId.isNotBlank() && 
                            selectedZone != null && 
                            !address.contains("Sélectionnez") && 
                            !isLoadingAddress
                ) {
                    Icon(if (isEditMode) Icons.Default.Update else Icons.Default.Save, null)
                    Spacer(Modifier.width(12.dp))
                    Text(if (isEditMode) "Enregistrer les modifications" else "Enregistrer le lampadaire", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            item { Spacer(Modifier.height(100.dp)) }
        }

        if (showQRDialog && generatedQRBitmap != null) {
            AlertDialog(
                onDismissRequest = { 
                    showQRDialog = false 
                    onAddSuccess()
                },
                confirmButton = {
                    Button(
                        onClick = { 
                            showQRDialog = false 
                            onAddSuccess()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NoorIndigo)
                    ) {
                        Text("Terminer", color = Color.White)
                    }
                },
                title = {
                    Text("Lampadaire Ajouté !", fontWeight = FontWeight.Bold, color = NoorIndigo)
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Voici le QR Code unique généré pour ce lampadaire. Les citoyens pourront le scanner pour signaler des pannes ou pannes.", 
                            fontSize = 14.sp, 
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White)
                                .padding(12.dp)
                                .border(1.dp, Color.LightGray, RoundedCornerShape(20.dp))
                        ) {
                            Image(
                                bitmap = generatedQRBitmap!!.asImageBitmap(),
                                contentDescription = "QR Code",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(streetlightId, fontWeight = FontWeight.Black, fontSize = 24.sp, color = NoorIndigo)
                    }
                },
                shape = RoundedCornerShape(28.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            )
        }
    }
}

@Composable
fun StreetlightMarkingMap(
    lat: Double,
    lng: Double,
    address: String,
    statusColor: Color,
    onLocationChanged: (Double, Double) -> Unit
) {
    val hexColor = String.format("#%06X", (0xFFFFFF and statusColor.toArgb()))
    
    val html = """
        <!DOCTYPE html>
        <html>
        <head>
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                #map { height: 100vh; width: 100vw; margin: 0; padding: 0; }
                .marker-icon {
                    width: 14px; height: 14px;
                    border-radius: 50%;
                    border: 2px solid white;
                    box-shadow: 0 0 5px rgba(0,0,0,0.3);
                }
                .leaflet-container { background: #f0f2f5; }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = L.map('map').setView([$lat, $lng], 16);
                L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);
                
                var icon = L.divIcon({
                    className: 'custom-div-icon',
                    html: '<div class="marker-icon" style="background-color: $hexColor"></div>',
                    iconSize: [14, 14],
                    iconAnchor: [7, 7]
                });

                var marker = L.marker([$lat, $lng], {icon: icon, draggable: true}).addTo(map);
                
                if ("${address.replace("'", "\\'")}" !== "Sélectionnez un emplacement sur la carte...") {
                    marker.bindPopup("${address.replace("'", "\\'")}").openPopup();
                }

                marker.on('dragend', function(e) {
                    var pos = marker.getLatLng();
                    if (window.Android && window.Android.onLocationPicked) {
                        window.Android.onLocationPicked(pos.lat, pos.lng);
                    }
                });

                map.on('click', function(e) {
                    marker.setLatLng(e.latlng);
                    if (window.Android && window.Android.onLocationPicked) {
                        window.Android.onLocationPicked(e.latlng.lat, e.latlng.lng);
                    }
                });

                function updateMarker(newLat, newLng, newColor, newAddress) {
                    map.setView([newLat, newLng]);
                    marker.setLatLng([newLat, newLng]);
                    var newIcon = L.divIcon({
                        className: 'custom-div-icon',
                        html: '<div class="marker-icon" style="background-color: ' + newColor + '"></div>',
                        iconSize: [14, 14],
                        iconAnchor: [7, 7]
                    });
                    marker.setIcon(newIcon);
                    
                    if (newAddress && newAddress !== "Sélectionnez un emplacement sur la carte...") {
                        marker.bindPopup(newAddress).openPopup();
                    }
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
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onLocationPicked(lat: Double, lng: Double) {
                        onLocationChanged(lat, lng)
                    }
                }, "Android")
                loadDataWithBaseURL("https://osm.org", html, "text/html", "UTF-8", null)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .clip(RoundedCornerShape(16.dp)),
        update = { webView ->
            webView.loadUrl("javascript:updateMarker($lat, $lng, '$hexColor', '${address.replace("'", "\\'")}')")
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddStreetlightTopBar(
    title: String = "Nouveau Lampadaire",
    onBackPressed: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(NoorBlue.copy(alpha = 0.9f), NoorBlue.copy(alpha = 0.6f))
                )
            )
            .padding(horizontal = 24.dp, vertical = 56.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBackPressed,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Icon(Icons.Default.ArrowBack, "Retour", tint = Color.White)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, color = Color.White.copy(0.9f), fontSize = 16.sp)
                Text("Gestion Réseau", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddStreetlightScreenPreview() {
    SansaTheme {
        AddStreetlightScreen()
    }
}