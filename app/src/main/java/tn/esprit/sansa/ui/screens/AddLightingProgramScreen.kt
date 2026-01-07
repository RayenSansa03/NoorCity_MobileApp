// AddLightingProgramScreen.kt — Version moderne avec filtrage par zone et wizard
package tn.esprit.sansa.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import tn.esprit.sansa.ui.screens.models.*
import tn.esprit.sansa.ui.theme.*
import tn.esprit.sansa.ui.viewmodels.LightingProgramsViewModel
import tn.esprit.sansa.ui.viewmodels.CulturalEventsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLightingProgramScreen(
    editingProgramId: String? = null,
    modifier: Modifier = Modifier,
    viewModel: LightingProgramsViewModel = viewModel(),
    culturalEventsViewModel: CulturalEventsViewModel = viewModel(),
    onAddSuccess: () -> Unit = {},
    onBackPressed: () -> Unit = {}
) {
    val isEditMode = editingProgramId != null
    var initialProgram by remember { mutableStateOf<LightingProgram?>(null) }
    
    var currentStep by remember { mutableStateOf(0) }
    var selectedEvent by remember { mutableStateOf<CulturalEvent?>(null) }
    var selectedTechnician by remember { mutableStateOf<Technician?>(null) }
    var selectedStreetlights by remember { mutableStateOf(setOf<String>()) }
    var selectedAmbience by remember { mutableStateOf(LightingAmbience.NORMAL) }
    var timelinePoints by remember { mutableStateOf(listOf<TimelinePoint>()) }

    // Fetch data
    val allStreetlights by viewModel.streetlights.collectAsState()
    val rawEvents by culturalEventsViewModel.events.collectAsState()

    // Load existing data if in edit mode
    LaunchedEffect(editingProgramId, allStreetlights, rawEvents) {
        if (isEditMode) {
            val program = viewModel.getProgramById(editingProgramId!!)
            if (program != null && initialProgram == null) {
                initialProgram = program
                selectedEvent = rawEvents.find { it.id == program.eventId }
                selectedTechnician = culturalEventsViewModel.getAllTechnicians().find { it.id == program.technicianId }
                selectedStreetlights = program.associatedStreetlights.toSet()
                selectedAmbience = program.ambience
                timelinePoints = program.timeline
            }
        }
    }
    
    // Filter events
    val availableEvents = remember(rawEvents) {
        rawEvents.filter { it.status == EventStatus.UPCOMING || it.status == EventStatus.ACTIVE }
    }
    
    // Filter technicians (All available, as requested)
    // Filter technicians (All available, as requested)
    val availableTechnicians = remember(selectedEvent) {
        culturalEventsViewModel.getAllTechnicians()
    }
    
    // Filter streetlights (All available, as requested)
    val filteredStreetlights = allStreetlights
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Modifier le Programme" else "Nouveau Programme d'Éclairage", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NoorBlue,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationButtons(
                currentStep = currentStep,
                canGoNext = when(currentStep) {
                    0 -> selectedEvent != null
                    1 -> selectedTechnician != null
                    2 -> selectedStreetlights.isNotEmpty()
                    3 -> true // Ambience always has a default
                    4 -> true // Timeline is auto-generated
                    else -> false
                },
                onNext = { currentStep++ },
                onPrevious = { if (currentStep > 0) currentStep-- },
                onFinish = {
                    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    val now = sdf.format(Date())
                    
                    val program = if (isEditMode) {
                        initialProgram!!.copy(
                            name = "Noor: ${selectedEvent!!.name}",
                            timeline = timelinePoints,
                            ambience = selectedAmbience,
                            associatedStreetlights = selectedStreetlights.toList(),
                            lastModified = now,
                            description = "Programme intelligent pour ${selectedEvent!!.name}",
                            eventId = selectedEvent?.id,
                            technicianId = selectedTechnician?.id
                        )
                    } else {
                        LightingProgram(
                            id = "",
                            name = "Noor: ${selectedEvent!!.name}",
                            timeline = timelinePoints,
                            ambience = selectedAmbience,
                            associatedStreetlights = selectedStreetlights.toList(),
                            status = ProgramStatus.PENDING,
                            createdDate = now,
                            lastModified = now,
                            priority = 5,
                            description = "Programme intelligent pour ${selectedEvent!!.name}",
                            eventId = selectedEvent?.id,
                            technicianId = selectedTechnician?.id,
                            technicianStatus = TechnicianAssignmentStatus.WAITING
                        )
                    }
                    
                    if (isEditMode) {
                        viewModel.updateProgram(program) { onAddSuccess() }
                    } else {
                        viewModel.addProgram(program) { onAddSuccess() }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Stepper
            ProgramCreationStepper(
                currentStep = currentStep,
                selectedEvent = selectedEvent,
                selectedTechnician = selectedTechnician,
                selectedStreetlightsCount = selectedStreetlights.size
            )
            
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            
            // Content based on step
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    slideInHorizontally { it } + fadeIn() togetherWith
                    slideOutHorizontally { -it } + fadeOut()
                },
                label = "step_transition"
            ) { step ->
                when(step) {
                    0 -> EventSelectionStep(
                        events = availableEvents,
                        selectedEvent = selectedEvent,
                        onEventSelected = { 
                            selectedEvent = it
                            selectedTechnician = null
                            selectedStreetlights = emptySet()
                        }
                    )
                    1 -> TechnicianSelectionStep(
                        technicians = availableTechnicians,
                        selectedTechnician = selectedTechnician,
                        onTechnicianSelected = { selectedTechnician = it },
                        eventZone = selectedEvent?.zones?.firstOrNull() ?: ""
                    )
                    2 -> StreetlightSelectionStep(
                        streetlights = filteredStreetlights,
                        selectedStreetlights = selectedStreetlights,
                        onToggleStreetlight = { id ->
                            selectedStreetlights = if (id in selectedStreetlights) {
                                selectedStreetlights - id
                            } else {
                                selectedStreetlights + id
                            }
                        },
                        eventZone = selectedEvent?.zones?.firstOrNull() ?: ""
                    )
                    3 -> AmbienceSelectionStep(
                        selectedAmbience = selectedAmbience,
                        onAmbienceSelected = { selectedAmbience = it }
                    )
                    4 -> TimelineConfigurationStep(
                        timelinePoints = timelinePoints,
                        ambience = selectedAmbience,
                        onPointsChanged = { timelinePoints = it }
                    )
                }
            }
        }
    }
}

// Stepper Component
@Composable
fun ProgramCreationStepper(
    currentStep: Int,
    selectedEvent: CulturalEvent?,
    selectedTechnician: Technician?,
    selectedStreetlightsCount: Int
) {
    val steps = listOf(
        StepInfo("Événement", Icons.Default.Event, selectedEvent != null),
        StepInfo("Technicien", Icons.Default.Person, selectedTechnician != null),
        StepInfo("Lampadaires", Icons.Default.Lightbulb, selectedStreetlightsCount > 0),
        StepInfo("Ambiance", Icons.Default.AutoAwesome, true),
        StepInfo("Timeline", Icons.Default.Timeline, true)
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, step ->
            StepItem(
                stepNumber = index + 1,
                stepInfo = step,
                isActive = index == currentStep,
                isCompleted = index < currentStep || step.isCompleted,
                modifier = Modifier.weight(1f)
            )
            
            if (index < steps.size - 1) {
                Divider(
                    modifier = Modifier
                        .width(24.dp)
                        .padding(horizontal = 4.dp),
                    color = if (index < currentStep) NoorBlue else MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
}

data class StepInfo(val name: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val isCompleted: Boolean)

@Composable
fun StepItem(
    stepNumber: Int,
    stepInfo: StepInfo,
    isActive: Boolean,
    isCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isCompleted -> NoorGreen
                        isActive -> NoorBlue
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    imageVector = stepInfo.icon,
                    contentDescription = null,
                    tint = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stepInfo.name,
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) NoorBlue else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Navigation Buttons
@Composable
fun NavigationButtons(
    currentStep: Int,
    canGoNext: Boolean,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onFinish: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (currentStep > 0) {
                OutlinedButton(
                    onClick = onPrevious,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Précédent")
                }
                
                Spacer(Modifier.width(12.dp))
            }
            
            Button(
                onClick = if (currentStep == 4) onFinish else onNext,
                enabled = canGoNext,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentStep == 4) NoorGreen else NoorBlue
                )
            ) {
                Text(if (currentStep == 4) "Confirmer le Programme" else "Suivant")
                Spacer(Modifier.width(8.dp))
                Icon(
                    if (currentStep == 4) Icons.Default.RocketLaunch else Icons.Default.ArrowForward,
                    null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// Event Selection Step
@Composable
fun EventSelectionStep(
    events: List<CulturalEvent>,
    selectedEvent: CulturalEvent?,
    onEventSelected: (CulturalEvent) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Sélectionnez l'événement culturel",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Le programme d'éclairage sera créé pour cet événement",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
        }
        
        items(events) { event ->
            EventSelectionCard(
                event = event,
                isSelected = selectedEvent?.id == event.id,
                onClick = { onEventSelected(event) }
            )
        }
        
        if (events.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Event,
                    message = "Aucun événement disponible",
                    description = "Créez d'abord un événement culturel"
                )
            }
        }
    }
}

@Composable
fun EventSelectionCard(
    event: CulturalEvent,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                NoorBlue.copy(alpha = 0.1f)
            else 
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) 
            androidx.compose.foundation.BorderStroke(2.dp, NoorBlue)
        else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(event.ambianceType.color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = null,
                    tint = event.ambianceType.color,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Zone: ${event.zones.firstOrNull() ?: "Non définie"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = SimpleDateFormat("dd MMM yyyy à HH:mm", Locale.getDefault())
                            .format(event.dateTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Sélectionné",
                    tint = NoorBlue,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

// Technician Selection Step
@Composable
fun TechnicianSelectionStep(
    technicians: List<Technician>,
    selectedTechnician: Technician?,
    onTechnicianSelected: (Technician) -> Unit,
    eventZone: String
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Sélectionnez le technicien",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Tous les techniciens",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
        }
        
        items(technicians) { technician ->
            TechnicianSelectionCard(
                technician = technician,
                isSelected = selectedTechnician?.id == technician.id,
                onClick = { onTechnicianSelected(technician) }
            )
        }
        
        if (technicians.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Person,
                    message = "Aucun technicien trouvé",
                    description = "Aucun technicien n'est enregistré dans le système"
                )
            }
        }
    }
}

@Composable
fun TechnicianSelectionCard(
    technician: Technician,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                NoorGreen.copy(alpha = 0.1f)
            else 
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) 
            androidx.compose.foundation.BorderStroke(2.dp, NoorGreen)
        else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(technician.specialty.color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = technician.name.first().toString(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = technician.specialty.color
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = technician.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = technician.specialty.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = technician.specialty.color
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Zones: ${technician.zoneIds.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = technician.status.color.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = technician.status.displayName,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = technician.status.color
                    )
                }
                
                if (isSelected) {
                    Spacer(Modifier.height(8.dp))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Sélectionné",
                        tint = NoorGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

// Streetlight Selection Step
@Composable
fun StreetlightSelectionStep(
    streetlights: List<Streetlight>,
    selectedStreetlights: Set<String>,
    onToggleStreetlight: (String) -> Unit,
    eventZone: String
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Sélectionnez les lampadaires",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${streetlights.size} lampadaires au total",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${selectedStreetlights.size} sélectionné(s)",
                style = MaterialTheme.typography.bodySmall,
                color = NoorBlue,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(8.dp))
        }
        
        if (streetlights.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            streetlights.forEach { onToggleStreetlight(it.id) }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Tout sélectionner")
                    }
                    OutlinedButton(
                        onClick = {
                            selectedStreetlights.forEach { onToggleStreetlight(it) }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Tout désélectionner")
                    }
                }
            }
        }
        
        items(streetlights) { streetlight ->
            StreetlightSelectionCard(
                streetlight = streetlight,
                isSelected = streetlight.id in selectedStreetlights,
                onToggle = { onToggleStreetlight(streetlight.id) }
            )
        }
        
        if (streetlights.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Lightbulb,
                    message = "Aucun lampadaire disponible",
                    description = "Aucun lampadaire n'est assigné à la zone $eventZone"
                )
            }
        }
    }
}

@Composable
fun StreetlightSelectionCard(
    streetlight: Streetlight,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                NoorAmber.copy(alpha = 0.1f)
            else 
                MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected) 
            androidx.compose.foundation.BorderStroke(2.dp, NoorAmber)
        else null
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = NoorAmber)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = if (isSelected) NoorAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Lampadaire #${streetlight.id.take(8)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = streetlight.address.ifBlank { "Zone: ${streetlight.zoneId}" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(streetlight.status.color)
            )
        }
    }
}

// Empty State Card
@Composable
fun EmptyStateCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String,
    description: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

// --- New Steps ---

@Composable
fun AmbienceSelectionStep(
    selectedAmbience: LightingAmbience,
    onAmbienceSelected: (LightingAmbience) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Choisissez l'ambiance lumineuse",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Des préréglages intelligents pour chaque situation",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
        }

        items(LightingAmbience.values()) { ambience ->
            AmbienceCard(
                ambience = ambience,
                isSelected = selectedAmbience == ambience,
                onClick = { onAmbienceSelected(ambience) }
            )
        }
    }
}

@Composable
fun AmbienceCard(
    ambience: LightingAmbience,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) ambience.color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, ambience.color) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ambience.color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(ambience.icon, null, tint = ambience.color, modifier = Modifier.size(28.dp))
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(ambience.displayName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    ambience.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            if (isSelected) {
                Icon(Icons.Default.CheckCircle, null, tint = ambience.color)
            }
        }
    }
}

@Composable
fun TimelineConfigurationStep(
    timelinePoints: List<TimelinePoint>,
    ambience: LightingAmbience,
    onPointsChanged: (List<TimelinePoint>) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            text = "Timeline d'intensité",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Ajustez la courbe de luminosité pour la soirée",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        // Visual Graph Placeholder (using simpler horizontal sliders for now)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Ajustement par tranche horaire", fontWeight = FontWeight.Bold, color = ambience.color)
                Spacer(Modifier.height(16.dp))
                
                timelinePoints.forEachIndexed { index, point ->
                    TimelineSlider(
                        point = point,
                        color = ambience.color,
                        onIntensityChanged = { newIntensity ->
                            val newList = timelinePoints.toMutableList()
                            newList[index] = point.copy(intensity = newIntensity)
                            onPointsChanged(newList)
                        }
                    )
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Energy savings hint
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NoorGreen.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Eco, null, tint = NoorGreen)
            Spacer(Modifier.width(12.dp))
            Text(
                "Économie d'énergie estimée : 35%",
                fontWeight = FontWeight.Bold,
                color = NoorGreen,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun TimelineSlider(
    point: TimelinePoint,
    color: Color,
    onIntensityChanged: (Int) -> Unit
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${point.hour}h${point.minute.toString().padStart(2, '0')}", fontWeight = FontWeight.Medium)
            Text("${point.intensity}%", fontWeight = FontWeight.Bold, color = color)
        }
        Slider(
            value = point.intensity.toFloat(),
            onValueChange = { onIntensityChanged(it.toInt()) },
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = color.copy(alpha = 0.2f)
            )
        )
    }
}