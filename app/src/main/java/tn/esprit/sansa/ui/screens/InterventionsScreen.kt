// InterventionsScreen.kt – Version Connectée (Décembre 2025)
package tn.esprit.sansa.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import tn.esprit.sansa.ui.theme.SansaTheme
import tn.esprit.sansa.ui.theme.*
import tn.esprit.sansa.ui.components.*
import tn.esprit.sansa.ui.screens.models.*
import tn.esprit.sansa.ui.viewmodels.InterventionsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterventionsScreen(
    modifier: Modifier = Modifier,
    onNavigateToAddIntervention: () -> Unit = {},
    onNavigateToEditIntervention: (String) -> Unit = {},
    viewModel: InterventionsViewModel = viewModel()
) {
    InterventionsMainScreen(
        modifier = modifier,
        onNavigateToAddIntervention = onNavigateToAddIntervention,
        onNavigateToEditIntervention = onNavigateToEditIntervention,
        viewModel = viewModel
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InterventionsMainScreen(
    modifier: Modifier = Modifier,
    onNavigateToAddIntervention: () -> Unit,
    onNavigateToEditIntervention: (String) -> Unit,
    viewModel: InterventionsViewModel
) {
    val interventionsList by viewModel.interventions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showTutorial by rememberSaveable { mutableStateOf(true) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf<InterventionStatus?>(null) }
    var selectedType by remember { mutableStateOf<InterventionType?>(null) }

    val filteredInterventions = remember(interventionsList, searchQuery, selectedStatus, selectedType) {
        interventionsList.filter { intervention ->
            val matchesSearch = searchQuery.isEmpty() ||
                    intervention.id.contains(searchQuery, ignoreCase = true) ||
                    intervention.description.contains(searchQuery, ignoreCase = true) ||
                    intervention.location.contains(searchQuery, ignoreCase = true) ||
                    intervention.technicianName.contains(searchQuery, ignoreCase = true)
            val matchesStatus = selectedStatus == null || intervention.status == selectedStatus
            val matchesType = selectedType == null || intervention.type == selectedType
            matchesSearch && matchesStatus && matchesType
        }.sortedByDescending { it.date }
    }

    val stats = remember(interventionsList) {
        mapOf(
            "Total" to interventionsList.size,
            "Planifiées" to interventionsList.count { it.status == InterventionStatus.SCHEDULED },
            "En cours" to interventionsList.count { it.status == InterventionStatus.IN_PROGRESS }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { InterventionsTopBarModern(stats = stats, onRefresh = { viewModel.refresh() }) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddIntervention,
                containerColor = NoorBlue,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nouvelle intervention")
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

            item { InterventionSearchBar(query = searchQuery, onQueryChange = { searchQuery = it }) }

            item {
                Text("Filtrer par statut", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                InterventionStatusFilters(
                    selectedStatus = selectedStatus,
                    onStatusSelected = { selectedStatus = if (selectedStatus == it) null else it }
                )
            }

            item {
                Text("Filtrer par type", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                InterventionTypeFilters(
                    selectedType = selectedType,
                    onTypeSelected = { selectedType = if (selectedType == it) null else it }
                )
            }

            if (isLoading) {
                items(5) { CardSkeleton() }
            } else if (filteredInterventions.isEmpty()) {
                item {
                    EmptyState(
                        modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
                        icon = Icons.Default.EventBusy,
                        title = "Aucune intervention",
                        description = "Tout semble opérationnel pour le moment.",
                        actionLabel = "Planifier des travaux",
                        onActionClick = onNavigateToAddIntervention,
                        iconColor = NoorRed
                    )
                }
            } else {
                itemsIndexed(
                    items = filteredInterventions,
                    key = { _, intervention -> intervention.id }
                ) { index, intervention ->
                    StaggeredItem(index = index) {
                        Box {
                            SwipeActionsContainer(
                                item = intervention,
                                onDelete = { viewModel.deleteIntervention(intervention.id) },
                                onEdit = { i -> onNavigateToEditIntervention(i.id) }
                            ) { item ->
                                InterventionCard(intervention = item)
                            }

                            if (index == 0 && showTutorial && interventionsList.isNotEmpty()) {
                                CoachMarkTooltip(
                                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp).offset(x = 16.dp, y = 32.dp),
                                    text = "Glissez vers la gauche pour supprimer",
                                    onDismiss = { showTutorial = false }
                                )
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(100.dp)) }
        }
    }
}

@Composable
private fun InterventionsTopBarModern(stats: Map<String, Int>, onRefresh: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(brush = Brush.verticalGradient(colors = listOf(NoorBlue, NoorBlue.copy(alpha = 0.7f))))
            .padding(horizontal = 20.dp, vertical = 28.dp)
            .statusBarsPadding()
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Service de Maintenance", color = Color.White.copy(0.85f), fontSize = 14.sp)
                    Text("Gestion Technique", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualiser", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                stats.forEach { (label, value) ->
                    QuickStatCardCompact(value = value.toString(), label = label, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun QuickStatCardCompact(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = value, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text(text = label, color = Color.White.copy(0.9f), fontSize = 11.sp, maxLines = 1)
        }
    }
}

@Composable
private fun InterventionSearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Rechercher une intervention...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NoorBlue,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            cursorColor = NoorBlue
        )
    )
}

@Composable
private fun InterventionStatusFilters(selectedStatus: InterventionStatus?, onStatusSelected: (InterventionStatus) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
        InterventionStatus.entries.forEach { status ->
            FilterChip(
                selected = selectedStatus == status,
                onClick = { onStatusSelected(status) },
                label = { Text(status.displayName) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = status.color,
                    selectedLabelColor = Color.White,
                    labelColor = status.color
                )
            )
        }
    }
}

@Composable
private fun InterventionTypeFilters(selectedType: InterventionType?, onTypeSelected: (InterventionType) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
        InterventionType.entries.forEach { type ->
            FilterChip(
                selected = selectedType == type,
                onClick = { onTypeSelected(type) },
                label = { Text(type.displayName) },
                leadingIcon = { Icon(type.icon, null, modifier = Modifier.size(16.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NoorBlue,
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White
                )
            )
        }
    }
}

@Composable
private fun InterventionCard(intervention: Intervention) {
    var expanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f)

    Card(
        modifier = Modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.clickable { expanded = !expanded }.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier.size(50.dp).clip(RoundedCornerShape(12.dp)).background(intervention.type.color.copy(0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(intervention.type.icon, null, tint = intervention.type.color)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Engineering, null, modifier = Modifier.size(16.dp), tint = NoorBlue)
                        Text(intervention.technicianName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Badge(containerColor = intervention.status.color) {
                            Text(intervention.status.displayName, color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp))
                        }
                        Text(intervention.priority, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
            }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ModernStatItem(value = intervention.location.split(",").last().trim(), label = "Ville", modifier = Modifier.weight(1f))
                ModernStatItem(value = "${intervention.estimatedDuration}m", label = "Durée", modifier = Modifier.weight(1f))
                ModernStatItem(value = SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(intervention.date)), label = "Date", modifier = Modifier.weight(1f))
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    InfoRow(Icons.Default.LocationOn, "Localisation", intervention.location)
                    InfoRow(Icons.Default.Lightbulb, "ID Lampadaire", intervention.streetlightId)
                    InfoRow(Icons.Default.Description, "Description", intervention.description)
                    if (intervention.notes.isNotBlank()) InfoRow(Icons.Default.Note, "Notes", intervention.notes)
                    InfoRow(Icons.Default.SupervisorAccount, "Assigné par", intervention.assignedBy)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(icon, null, modifier = Modifier.size(16.dp), tint = NoorBlue)
        Spacer(Modifier.width(8.dp))
        Text("$label: ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(value, fontSize = 12.sp, maxLines = 2)
    }
}