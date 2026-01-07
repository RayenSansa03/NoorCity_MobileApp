// AddSensorScreen.kt – VERSION ALTERNATIVE (Décembre 2025)
package tn.esprit.sansa.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.esprit.sansa.ui.theme.SansaTheme
import tn.esprit.sansa.ui.theme.*

import tn.esprit.sansa.ui.screens.models.*
import tn.esprit.sansa.ui.viewmodels.SensorsViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

// Palette Noor centralisée dans tn.esprit.sansa.ui.theme


// Liste fictive de lampadaires
private val mockStreetlights = listOf(
    "L001 - Lampadaire #001",
    "L002 - Lampadaire #002",
    "L003 - Lampadaire #003",
    "L004 - Lampadaire #004",
    "L005 - Lampadaire #005",
    "L006 - Lampadaire #006",
    "L007 - Lampadaire #007",
    "L008 - Lampadaire #008"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSensorScreen(
    editingSensorId: String? = null,
    onBack: () -> Unit,
    viewModel: SensorsViewModel = viewModel(),
    streetlightsViewModel: tn.esprit.sansa.ui.viewmodels.StreetlightsViewModel = viewModel()
) {
    val isEditMode = editingSensorId != null
    var initialSensor by remember { mutableStateOf<Sensor?>(null) }
    
    var selectedType by remember { mutableStateOf<SensorType?>(null) }
    var selectedStreetlight by remember { mutableStateOf("") }
    var batteryLevel by remember { mutableStateOf(100f) }
    var selectedStatus by remember { mutableStateOf(SensorStatus.ACTIVE) }
    var hardwareId by remember { mutableStateOf("") }
 
    var showTypeError by remember { mutableStateOf(false) }
    var showStreetlightError by remember { mutableStateOf(false) }
    var showIdError by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val streetlights by streetlightsViewModel.streetlights.collectAsState()

    var showSuccessAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(editingSensorId, streetlights) {
        if (isEditMode) {
            val sensor = viewModel.getSensorById(editingSensorId!!)
            if (sensor != null && initialSensor == null) {
                initialSensor = sensor
                hardwareId = sensor.id
                selectedType = sensor.type
                batteryLevel = sensor.batteryLevel.toFloat()
                selectedStatus = sensor.status
                
                selectedStreetlight = streetlights.find { it.id == sensor.streetlightId }?.let { "${it.id} - ${it.address}" } ?: ""
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            if (isEditMode) "Modifier le capteur" else "Ajouter un capteur",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            "Configurez un nouveau capteur intelligent",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                FormSection(
                    title = "Identifiant Matériel (ESP32)",
                    icon = Icons.Default.Fingerprint,
                    isError = showIdError
                ) {
                    CustomTextField(
                        value = hardwareId,
                        onValueChange = {
                            if (!isEditMode) {
                                hardwareId = it
                                showIdError = false
                            }
                        },
                        label = "ID du Capteur",
                        placeholder = "Ex: TEMP_A1B2C3",
                        isError = showIdError,
                        keyboardType = KeyboardType.Text,
                        enabled = !isEditMode
                    )
                    Text(
                        "Saisissez l'ID affiché dans le moniteur série de l'ESP32",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                    AnimatedVisibility(visible = showIdError) {
                        Text(
                            "Veuillez entrer l'identifiant du matériel",
                            color = NoorRed,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }
                }

                FormSection(
                    title = "Type de capteur",
                    icon = Icons.Default.Category,
                    isError = showTypeError
                ) {
                    SensorTypeSelector(
                        selectedType = selectedType,
                        onTypeSelected = {
                            selectedType = it
                            showTypeError = false
                        }
                    )
                    AnimatedVisibility(visible = showTypeError) {
                        Text(
                            "Veuillez sélectionner un type de capteur",
                            color = NoorRed,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }
                }

                FormSection(
                    title = "Lampadaire associé",
                    icon = Icons.Default.Lightbulb,
                    isError = showStreetlightError
                ) {
                    StreetlightDropdown(
                        selectedStreetlight = selectedStreetlight,
                        streetlights = streetlights,
                        onStreetlightSelected = {
                            selectedStreetlight = it
                            showStreetlightError = false
                        },
                        isError = showStreetlightError
                    )
                    AnimatedVisibility(visible = showStreetlightError) {
                        Text(
                            "Veuillez sélectionner un lampadaire",
                            color = NoorRed,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }
                }

                FormSection(
                    title = "Niveau de batterie",
                    icon = Icons.Default.BatteryFull
                ) {
                    BatterySlider(
                        batteryLevel = batteryLevel,
                        onBatteryChange = { batteryLevel = it }
                    )
                }

                FormSection(
                    title = "Statut",
                    icon = Icons.Default.CheckCircle
                ) {
                    StatusChips(
                        selectedStatus = selectedStatus,
                        onStatusSelected = { selectedStatus = it }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                ActionButtons(
                    isEditMode = isEditMode,
                    onCancel = onBack,
                    onAdd = {
                        var hasError = false

                        if (hardwareId.isEmpty()) {
                            showIdError = true
                            hasError = true
                        }
                        if (selectedType == null) {
                            showTypeError = true
                            hasError = true
                        }
                        if (selectedStreetlight.isEmpty()) {
                            showStreetlightError = true
                            hasError = true
                        }

                        if (!hasError) {
                             val sensorToSave = if (isEditMode && initialSensor != null) {
                                initialSensor!!.copy(
                                    type = selectedType!!,
                                    streetlightId = selectedStreetlight.split(" - ").first(),
                                    streetlightName = selectedStreetlight.split(" - ").last(),
                                    status = selectedStatus,
                                    batteryLevel = batteryLevel.toInt()
                                )
                            } else {
                                Sensor(
                                    id = hardwareId.trim(),
                                    type = selectedType!!,
                                    streetlightId = selectedStreetlight.split(" - ").first(),
                                    streetlightName = selectedStreetlight.split(" - ").last(),
                                    currentValue = "--", // Valeur d'attente
                                    status = selectedStatus,
                                    lastUpdate = "En attente...",
                                    batteryLevel = batteryLevel.toInt()
                                )
                            }
                            
                            if (isEditMode) {
                                viewModel.updateSensor(sensorToSave) { onBack() }
                            } else {
                                viewModel.addSensor(sensorToSave) { onBack() }
                            }
                            
                            showSuccessAnimation = true
                            scope.launch {
                                delay(1200)
                                onBack()
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            AnimatedVisibility(
                visible = showSuccessAnimation,
                enter = fadeIn() + scaleIn(initialScale = 0.8f),
                exit = fadeOut()
            ) {
                SuccessAnimation()
            }
        }
    }
}

@Composable
private fun FormSection(
    title: String,
    icon: ImageVector,
    isError: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (isError) BorderStroke(2.dp, NoorRed) else null
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isError) NoorRed else NoorBlue,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}

@Composable
private fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    suffix: String = "",
    isError: Boolean = false,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isError) NoorRed else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = 2.dp,
                    color = if (isError) NoorRed else MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(12.dp)
                )
                .background(if (enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    enabled = enabled,
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = keyboardType,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(NoorBlue),
                    decorationBox = { innerTextField ->
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        innerTextField()
                    }
                )

                if (suffix.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = suffix,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun SensorTypeSelector(
    selectedType: SensorType?,
    onTypeSelected: (SensorType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SensorType.entries.forEach { type ->
            val isSelected = selectedType == type
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTypeSelected(type) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) type.color.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) type.color else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(type.icon, null, tint = type.color, modifier = Modifier.size(24.dp))
                        Column {
                            Text(type.displayName, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text("Unité: ${type.unit}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (isSelected) {
                        Icon(Icons.Default.CheckCircle, null, tint = type.color, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StreetlightDropdown(
    selectedStreetlight: String,
    streetlights: List<Streetlight>,
    onStreetlightSelected: (String) -> Unit,
    isError: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = 2.dp,
                    color = if (isError) NoorRed else MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(12.dp)
                )
                .background(MaterialTheme.colorScheme.surface)
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = NoorAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = selectedStreetlight.ifEmpty { "Sélectionner un lampadaire" },
                        fontSize = 16.sp,
                        color = if (selectedStreetlight.isEmpty())
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            streetlights.forEach { streetlight ->
                val display = "${streetlight.id} - ${streetlight.address}"
                DropdownMenuItem(
                    text = {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.Lightbulb, null, tint = NoorAmber, modifier = Modifier.size(20.dp))
                            Text(display)
                        }
                    },
                    onClick = {
                        onStreetlightSelected(display)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun BatterySlider(
    batteryLevel: Float,
    onBatteryChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    when {
                        batteryLevel > 80 -> Icons.Default.BatteryFull
                        batteryLevel > 50 -> Icons.Default.Battery6Bar
                        batteryLevel > 20 -> Icons.Default.Battery3Bar
                        else -> Icons.Default.Battery1Bar
                    },
                    null,
                    tint = when {
                        batteryLevel > 50 -> NoorGreen
                        batteryLevel > 20 -> NoorAmber
                        else -> NoorRed
                    },
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    "${batteryLevel.toInt()}%",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        batteryLevel > 50 -> NoorGreen
                        batteryLevel > 20 -> NoorAmber
                        else -> NoorRed
                    }
                )
            }
            Text(
                when {
                    batteryLevel > 80 -> "Excellent"
                    batteryLevel > 50 -> "Bon"
                    batteryLevel > 20 -> "Faible"
                    else -> "Critique"
                },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Slider(
            value = batteryLevel,
            onValueChange = onBatteryChange,
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = when {
                    batteryLevel > 50 -> NoorGreen
                    batteryLevel > 20 -> NoorAmber
                    else -> NoorRed
                },
                activeTrackColor = when {
                    batteryLevel > 50 -> NoorGreen
                    batteryLevel > 20 -> NoorAmber
                    else -> NoorRed
                }
            )
        )
    }
}

@Composable
private fun StatusChips(
    selectedStatus: SensorStatus,
    onStatusSelected: (SensorStatus) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SensorStatus.entries.forEach { status ->
            val isSelected = selectedStatus == status
            Surface(
                modifier = Modifier.clickable { onStatusSelected(status) },
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) status.color.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) status.color
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            null,
                            tint = status.color,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        status.displayName,
                        fontSize = 13.sp,
                        color = if (isSelected) status.color else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButtons(
    isEditMode: Boolean,
    onCancel: () -> Unit,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .clickable { onCancel() },
            shape = RoundedCornerShape(12.dp),
            color = Color.Transparent,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Close, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Annuler", fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }

        Surface(
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .clickable { onAdd() },
            shape = RoundedCornerShape(12.dp),
            color = NoorBlue
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (isEditMode) Icons.Default.Update else Icons.Default.Save,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isEditMode) "Mettre à jour" else "Enregistrer",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun SuccessAnimation() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    null,
                    tint = NoorGreen,
                    modifier = Modifier.size(72.dp)
                )
                Text(
                    "C'est prêt !",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Les modifications ont été enregistrées avec succès.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Mode Clair")
@Composable
fun AddSensorScreenPreview() {
    SansaTheme(darkTheme = false) {
        AddSensorScreen(onBack = {})
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Mode Sombre")
@Composable
fun AddSensorScreenDarkPreview() {
    SansaTheme(darkTheme = true) {
        AddSensorScreen(onBack = {})
    }
}