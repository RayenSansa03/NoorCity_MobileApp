// HomeScreen.kt – VERSION MODERNE CORRIGÉE ET COMPLÈTE (2025 Style)
package tn.esprit.sansa.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import tn.esprit.sansa.ui.theme.SansaTheme
import tn.esprit.sansa.ui.viewmodels.SettingsViewModel
import tn.esprit.sansa.ui.viewmodels.AdminDashboardViewModel
import tn.esprit.sansa.ui.utils.Sansa
import androidx.lifecycle.viewmodel.compose.viewModel
import tn.esprit.sansa.ui.theme.*
import tn.esprit.sansa.ui.screens.models.UserRole
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.shape.CircleShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenModern(
    settingsViewModel: SettingsViewModel = viewModel(),
    adminDashboardViewModel: AdminDashboardViewModel = viewModel(),
    role: UserRole? = UserRole.CITIZEN,
    userName: String? = null,
    onNavigate: (String) -> Unit = {}
) {
    val stats by adminDashboardViewModel.stats.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Custom Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp, 64.dp, 24.dp, 32.dp)
                ) {
                    Text(
                        text = "Bienvenue, ${userName ?: (role?.name ?: "Utilisateur")}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "NoorCity",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Dashboard Stats
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (role == UserRole.ADMIN) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        ModernStatusCard(
                            count = stats.totalStreetlights, 
                            label = "Lampadaires", 
                            color = NoorBlue, 
                            loading = stats.isLoading
                        )
                        ModernStatusCard(
                            count = stats.malfunctioningLights, 
                            label = "Pannes", 
                            color = NoorRed, 
                            loading = stats.isLoading
                        )
                        ModernStatusCard(
                            count = stats.activeReclamations, 
                            label = "Réclamations", 
                            color = NoorAmber, 
                            loading = stats.isLoading
                        )
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }

                Text(
                    text = "Accès Rapide",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                if (role == UserRole.ADMIN || role == UserRole.TECHNICIAN) {
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.padding(bottom = 20.dp)) {
                         ModernMenuCard(title = "Techniciens", icon = Icons.Default.Engineering, onClick = { onNavigate("technicians") })
                         ModernMenuCard(title = "Réclamations", icon = Icons.Default.ReportProblem, onClick = { onNavigate("reclamations") })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.padding(bottom = 20.dp)) {
                         ModernMenuCard(title = "Lampadaires", icon = Icons.Default.LightMode, onClick = { onNavigate("streetlights") })
                         ModernMenuCard(title = "Caméras", icon = Icons.Default.Videocam, onClick = { onNavigate("cameras") })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.padding(bottom = 20.dp)) {
                         ModernMenuCard(title = "Événements", icon = Icons.Default.Event, onClick = { onNavigate("cultural_events") })
                         ModernMenuCard(title = "Programmes", icon = Icons.Default.AutoMode, onClick = { onNavigate("lighting_programs") })
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.padding(bottom = 20.dp)) {
                        ModernMenuCard(title = "Problème ?", icon = Icons.Default.ReportProblem, onClick = { onNavigate("add_reclamation") })
                        ModernMenuCard(title = "Ma Ville", icon = Icons.Default.Map, onClick = { onNavigate("zones") })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.padding(bottom = 20.dp)) {
                        ModernMenuCard(title = "Événements", icon = Icons.Default.Event, onClick = { onNavigate("cultural_events") })
                        ModernMenuCard(title = "Éclairage", icon = Icons.Default.LightMode, onClick = { onNavigate("lighting_programs") })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.padding(bottom = 20.dp)) {
                        ModernMenuCard(title = "Map", icon = Icons.Default.Public, onClick = { onNavigate("streetlights") })
                        ModernMenuCard(title = "Historique", icon = Icons.Default.History, onClick = { onNavigate("history") })
                    }
                }
                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        // Floating Chat Button for Admin/Tech
        if (role == UserRole.ADMIN || role == UserRole.TECHNICIAN) {
            FloatingActionButton(
                onClick = { onNavigate("users_list") },
                containerColor = NoorIndigo,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 32.dp, end = 24.dp)
                    .size(64.dp)
                    .shadow(12.dp, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = "Chat d'équipe",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun RowScope.ModernStatusCard(
    count: Int,
    label: String,
    color: Color,
    loading: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val elevation by animateDpAsState(if (isPressed) 16.dp else 4.dp)

    Card(
        modifier = Modifier
            .weight(1f)
            .aspectRatio(1f),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(0.2f), Color.Transparent),
                        radius = 600f
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (loading) "..." else "$count",
                    fontSize = if (loading) 24.sp else 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RowScope.ModernMenuCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    badgeCount: Int = 0,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateDpAsState(if (isPressed) (-4).dp else 0.dp)

    Card(
        modifier = Modifier
            .weight(1f)
            .aspectRatio(1.1f)
            .offset(y = scale)
            .shadow(if (isPressed) 8.dp else 4.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        interactionSource = interactionSource,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NoorBlue,
                    modifier = Modifier.size(36.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}