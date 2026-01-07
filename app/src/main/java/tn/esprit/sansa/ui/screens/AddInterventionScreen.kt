// AddInterventionScreen.kt – Version Premium (Noor Design System)
package tn.esprit.sansa.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import tn.esprit.sansa.ui.theme.SansaTheme
import tn.esprit.sansa.ui.theme.*
import tn.esprit.sansa.ui.components.*
import tn.esprit.sansa.ui.screens.models.*
import tn.esprit.sansa.ui.viewmodels.InterventionsViewModel
import tn.esprit.sansa.ui.viewmodels.StreetlightsViewModel
import tn.esprit.sansa.ui.viewmodels.AuthViewModel
import androidx.compose.ui.zIndex

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddInterventionScreen(
    editingInterventionId: String? = null,
    onAddSuccess: () -> Unit = {},
    onBackPressed: () -> Unit = {},
    viewModel: InterventionsViewModel = viewModel()
) {
    val isEditMode = editingInterventionId != null
    var initialIntervention by remember { mutableStateOf<Intervention?>(null) }
    
    val context = LocalContext.current
    val streetlightsViewModel: StreetlightsViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel()
    val currentUser by authViewModel.currentUser.collectAsState()
    val streetlights by streetlightsViewModel.streetlights.collectAsState()

    var location by remember { mutableStateOf("") }
    var streetlightId by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var technicianName by remember { mutableStateOf(currentUser?.name ?: "") }
    var assignedBy by remember { mutableStateOf(if (currentUser?.role == UserRole.ADMIN) currentUser?.name ?: "Administrateur" else "Service Technique") }
    var type by remember { mutableStateOf(InterventionType.MAINTENANCE) }
    var priority by remember { mutableStateOf(InterventionPriority.MEDIUM) }
    var estimatedDuration by remember { mutableStateOf("60") }
    var notes by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }

    LaunchedEffect(editingInterventionId) {
        if (isEditMode) {
            val intervention = viewModel.getInterventionById(editingInterventionId!!)
            if (intervention != null && initialIntervention == null) {
                initialIntervention = intervention
                location = intervention.location
                streetlightId = intervention.streetlightId
                description = intervention.description
                technicianName = intervention.technicianName
                assignedBy = intervention.assignedBy
                type = intervention.type
                priority = InterventionPriority.entries.find { it.displayName == intervention.priority } ?: InterventionPriority.MEDIUM
                estimatedDuration = intervention.estimatedDuration.toString()
            }
        }
    }

    val descriptionSuggestions = remember(type) {
        when (type) {
            InterventionType.REPAIR -> listOf("Ballast défectueux", "Câblage endommagé", "Changement d'ampoule", "Support de fixation")
            InterventionType.MAINTENANCE -> listOf("Nettoyage luminaire", "Vérification connexions", "Contrôle général", "Lubrification")
            InterventionType.INSPECTION -> listOf("Inspection routine", "Vérification conformité", "Contrôle sécurité", "Audit technique")
            InterventionType.INSTALLATION -> listOf("Nouveau lampadaire", "Système contrôle", "Capteurs intelligents", "Raccordement réseau")
            InterventionType.REPLACEMENT -> listOf("Changement complet", "Changement poteau", "Système éclairage", "Mise à niveau LED")
            InterventionType.EMERGENCY -> listOf("Danger immédiat", "Arbre sur ligne", "Court-circuit", "Poteau renversé")
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AddInterventionTopBar(
                title = if (isEditMode) "Modifier Travaux" else "Nouveaux Travaux",
                onBackPressed = onBackPressed
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item { Spacer(Modifier.height(8.dp)) }

                // Type & Priorité
                item {
                    ModernSectionCard(title = "Type & Priorité", icon = Icons.Default.Category) {
                        Text("Type d'intervention", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(InterventionType.entries) { item ->
                                FilterChip(
                                    selected = type == item,
                                    onClick = { type = item },
                                    label = { Text(item.displayName, fontSize = 11.sp) },
                                    leadingIcon = { Icon(item.icon, null, modifier = Modifier.size(16.dp)) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NoorBlue,
                                        selectedLabelColor = Color.White,
                                        selectedLeadingIconColor = Color.White
                                    )
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Priorité", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            InterventionPriority.entries.forEach { prio ->
                                FilterChip(
                                    modifier = Modifier.weight(1f),
                                    selected = priority == prio,
                                    onClick = { priority = prio },
                                    label = { Text(prio.displayName, fontSize = 11.sp) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = prio.color,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }

                // Localisation
                item {
                    ModernSectionCard(title = "Localisation", icon = Icons.Default.LocationOn) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = streetlightId,
                                onValueChange = { streetlightId = it },
                                label = { Text("ID Lampadaire") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NoorBlue, cursorColor = NoorBlue)
                            )
                            
                            IconButton(
                                onClick = { showScanner = true },
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(NoorBlue.copy(alpha = 0.1f))
                            ) {
                                Icon(Icons.Default.QrCodeScanner, null, tint = NoorBlue)
                            }
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            label = { Text("Adresse / Quartier") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NoorBlue, cursorColor = NoorBlue)
                        )
                    }
                }

                // Détails techniques
                item {
                    ModernSectionCard(title = "Détails techniques", icon = Icons.Default.Build) {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NoorBlue, cursorColor = NoorBlue)
                        )
                        Spacer(Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(descriptionSuggestions) { suggestion ->
                                SuggestionChip(
                                    onClick = { description = suggestion },
                                    label = { Text(suggestion, fontSize = 11.sp) },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = estimatedDuration,
                            onValueChange = { estimatedDuration = it.filter { it.isDigit() } },
                            label = { Text("Durée estimée (min)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NoorBlue, cursorColor = NoorBlue)
                        )
                    }
                }


                // Bouton d'action
                item {
                    Button(
                        onClick = {
                            isSubmitting = true
                            val interventionToSave = if (isEditMode) {
                                initialIntervention!!.copy(
                                    streetlightId = streetlightId,
                                    technicianName = technicianName,
                                    type = type,
                                    location = location,
                                    description = description,
                                    estimatedDuration = estimatedDuration.toIntOrNull() ?: 60,
                                    priority = priority.displayName,
                                    assignedBy = assignedBy
                                )
                            } else {
                                Intervention(
                                    streetlightId = streetlightId,
                                    technicianName = technicianName,
                                    type = type,
                                    location = location,
                                    description = description,
                                    estimatedDuration = estimatedDuration.toIntOrNull() ?: 60,
                                    priority = priority.displayName,
                                    assignedBy = assignedBy,
                                    status = InterventionStatus.SCHEDULED
                                )
                            }
                            
                            val action: (Intervention, (Boolean) -> Unit) -> Unit = if (isEditMode) viewModel::updateIntervention else viewModel::addIntervention
                            
                            action(interventionToSave) { success ->
                                isSubmitting = false
                                if (success) {
                                    Toast.makeText(context, if (isEditMode) "Intervention mise à jour !" else "Intervention planifiée !", Toast.LENGTH_SHORT).show()
                                    onAddSuccess()
                                } else {
                                    Toast.makeText(context, "Erreur d'enregistrement", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .shadow(8.dp, RoundedCornerShape(32.dp)),
                        enabled = !isSubmitting && location.isNotBlank() && description.isNotBlank(),
                        shape = RoundedCornerShape(32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NoorBlue)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(if (isEditMode) Icons.Default.Save else Icons.Default.EventAvailable, null)
                            Spacer(Modifier.width(12.dp))
                            Text(if (isEditMode) "Enregistrer les modifications" else "Planifier l'intervention", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                item { Spacer(Modifier.height(40.dp)) }
            }

            if (showScanner) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.9f))
                        .zIndex(10f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            "Scanner le QR Code du lampadaire",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(280.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .border(2.dp, NoorBlue, RoundedCornerShape(24.dp))
                        ) {
                            QRScannerView(
                                onCodeScanned = { code ->
                                    streetlightId = code
                                    // Auto-fill location if streetlight found
                                    val streetlight = streetlights.find { it.id == code }
                                    if (streetlight != null) {
                                        location = streetlight.address
                                        Toast.makeText(context, "Lampadaire détecté : ${streetlight.address}", Toast.LENGTH_SHORT).show()
                                    }
                                    showScanner = false
                                }
                            )
                        }
                        
                        Spacer(Modifier.height(32.dp))
                        
                        Button(
                            onClick = { showScanner = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Annuler", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddInterventionTopBar(
    title: String = "Nouveaux Travaux",
    onBackPressed: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(brush = Brush.verticalGradient(colors = listOf(NoorBlue, NoorBlue.copy(alpha = 0.7f))))
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBackPressed,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Icon(Icons.Default.ArrowBack, "Retour", tint = Color.White)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Planification", color = Color.White.copy(0.85f), fontSize = 14.sp)
                Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}