package tn.esprit.sansa.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import tn.esprit.sansa.ui.screens.*
import tn.esprit.sansa.ui.utils.Sansa
import tn.esprit.sansa.ui.viewmodels.*
import tn.esprit.sansa.ui.screens.models.UserRole
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import tn.esprit.sansa.ui.theme.NoorBlue

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Streetlights     : Screen("streetlights", "Lampadaires", Icons.Default.LightMode)
    object Cameras          : Screen("cameras", "Caméras", Icons.Default.Videocam)
    object Technicians      : Screen("technicians", "Techniciens", Icons.Default.Engineering)
    object Citizens         : Screen("citizens", "Citoyens", Icons.Default.People)
    object Reclamations     : Screen("reclamations", "Réclamations", Icons.Default.ReportProblem)

    object Zones            : Screen("zones", "Zones", Icons.Default.Map)
    object Interventions    : Screen("interventions", "Interventions", Icons.Default.Construction)
    object CulturalEvents   : Screen("cultural_events", "Év. culturels", Icons.Default.Event)
    object LightingPrograms : Screen("lighting_programs", "Programmes", Icons.Default.AutoMode)
    object Sensors          : Screen("sensors", "Capteurs", Icons.Default.Sensors)

    object AddStreetlight     : Screen("add_streetlight", "", Icons.Default.AddCircle)
    object AddCamera          : Screen("add_camera", "", Icons.Default.AddCircle)
    object AddTechnician      : Screen("add_technician", "", Icons.Default.AddCircle)
    object AddCitizen         : Screen("add_citizen", "", Icons.Default.AddCircle)
    object AddReclamation     : Screen("add_reclamation", "", Icons.Default.AddCircle)
    object AddIntervention    : Screen("add_intervention", "", Icons.Default.AddCircle)
    object AddCulturalEvent   : Screen("add_cultural_event", "", Icons.Default.AddCircle)
    object AddLightingProgram : Screen("add_lighting_program", "", Icons.Default.AddCircle)
    object AddZone            : Screen("add_zone", "", Icons.Default.AddCircle)
    object AddSensor          : Screen("add_sensor", "", Icons.Default.AddCircle)

    object EditStreetlight    : Screen("edit_streetlight/{streetlightId}", "Modifier Lampadaire", Icons.Default.Edit)
    object EditCamera         : Screen("edit_camera/{cameraId}", "Modifier Caméra", Icons.Default.Edit)
    object EditZone           : Screen("edit_zone/{zoneId}", "Modifier Zone", Icons.Default.Edit)
    object EditSensor         : Screen("edit_sensor/{sensorId}", "Modifier Capteur", Icons.Default.Edit)
    object EditIntervention    : Screen("edit_intervention/{interventionId}", "Modifier Intervention", Icons.Default.Edit)
    object EditLightingProgram : Screen("edit_lighting_program/{programId}", "Modifier Programme", Icons.Default.Edit)

    object Home    : Screen("home", "Accueil", Icons.Default.Home)
    object History : Screen("history", "Historique", Icons.Default.History)
    object Settings: Screen("settings", "Paramètres", Icons.Default.Settings)
    object Chat : Screen("chat/{targetId}/{targetName}", "Chat", Icons.Default.Chat)
    object UsersList : Screen("users_list", "Contacts", Icons.Default.Contacts)
    object ProfileTechnician : Screen("profile_technician", "Profil Technicien", Icons.Default.Person)
    object ProfileCitizen : Screen("profile_citizen", "Profil Citoyen", Icons.Default.Person)

    object EditProfile      : Screen("edit_profile", "Modifier Profil", Icons.Default.Edit)
    object ChangePassword   : Screen("change_password", "Changer MDP", Icons.Default.LockReset)
    object SetSecurityQuestions : Screen("set_security_questions", "Questions de sécurité", Icons.Default.Quiz)

    // Auth Screens
    object Login             : Screen("login", "Connexion", Icons.Default.Login)
    object Register          : Screen("register", "Inscription", Icons.Default.PersonAdd)
    object ForgotPassword   : Screen("forgot_password", "Récupération", Icons.Default.LockReset)
    object AdminTechMgmt    : Screen("admin_tech_mgmt", "Gestion Tech", Icons.Default.AdminPanelSettings)
    object TechWelcome      : Screen("tech_welcome", "Bienvenue Technicien", Icons.Default.Dashboard)
    object Onboarding       : Screen("onboarding", "Bienvenue", Icons.Default.Star)
}

private val NoorIndigo = Color(0xFF4F46E5)

@Composable
fun ModernBottomBar(
    navController: NavHostController,
    onShowSecondarySheet: () -> Unit,
    role: UserRole?,
    modifier: Modifier = Modifier
) {
    val strings = Sansa.strings
    
    val primaryItems = when (role) {
        UserRole.ADMIN -> listOf(
            Screen.Streetlights to "Lampadaires",
            Screen.Cameras to "Caméras",
            Screen.Technicians to strings.technicians,
            Screen.UsersList to "Chat"
        )
        UserRole.TECHNICIAN -> listOf(
            Screen.Streetlights to "Lampes",
            Screen.LightingPrograms to "Programmes",
            Screen.Interventions to "Interventions",
            Screen.UsersList to "Chat"
        )
        else -> listOf(
            Screen.Streetlights to "Lampes",
            Screen.LightingPrograms to "Programmes",
            Screen.CulturalEvents to "Événements",
            Screen.Zones to "Zones",
            Screen.AddReclamation to "Signaler"
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(
        modifier = modifier
            .height(84.dp)
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
            .border(
                width = 0.6.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ),
        containerColor = Color.Transparent,
        tonalElevation = 0.dp
    ) {
        primaryItems.forEach { (screen, localizedTitle) ->
            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = localizedTitle,
                        modifier = Modifier.size(if (selected) 30.dp else 26.dp),
                        tint = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f)
                    )
                },
                label = null,
                alwaysShowLabel = false,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    indicatorColor = Color.Transparent
                )
            )
        }

        NavigationBarItem(
            selected = false,
            onClick = onShowSecondarySheet,
            icon = {
                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = "Autres fonctionnalités",
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            label = null,
            alwaysShowLabel = false,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                indicatorColor = Color.Transparent
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecondaryFeaturesBottomSheet(
    onDismiss: () -> Unit,
    navController: NavHostController,
    role: UserRole?
) {
    val strings = Sansa.strings
    
    val secondaryItems = when (role) {
        UserRole.ADMIN -> listOf(
            Screen.Citizens to strings.citizens,
            Screen.Reclamations to strings.reclamations,
            Screen.Zones to "Zones",
            Screen.Interventions to "Interventions",
            Screen.CulturalEvents to "Événements",
            Screen.LightingPrograms to "Programmes",
            Screen.Sensors to "Capteurs",
            Screen.Settings to strings.settings,
            Screen.AdminTechMgmt to "Administration"
        )
        UserRole.TECHNICIAN -> listOf(
            Screen.CulturalEvents to "Événements",
            Screen.Zones to "Zones",
            Screen.Sensors to "Capteurs",
            Screen.ProfileTechnician to "Mon Profil",
            Screen.Settings to strings.settings
        )
        else -> listOf(
            Screen.ProfileCitizen to "Mon Profil",
            Screen.Settings to strings.settings,
            Screen.History to "Historique"
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Plus d'options",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(secondaryItems) { (screen, localizedTitle) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                onDismiss()
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                            .padding(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(NoorIndigo.copy(alpha = 0.1f), RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(screen.icon, contentDescription = localizedTitle, tint = NoorIndigo, modifier = Modifier.size(30.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = localizedTitle,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun AppNavigationWithModernBar(
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel,
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = viewModel()
) {
    val sensorsViewModel: SensorsViewModel = viewModel()
    val camerasViewModel: CamerasViewModel = viewModel()
    val culturalEventsViewModel: CulturalEventsViewModel = viewModel()
    val chatViewModel: ChatViewModel = viewModel()
    var showSecondarySheet by remember { mutableStateOf(false) }
    
    val adminDashboardViewModel: AdminDashboardViewModel = viewModel()
    
    val authState by authViewModel.authState.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    if (showSecondarySheet) {
        SecondaryFeaturesBottomSheet(
            onDismiss = { showSecondarySheet = false },
            navController = navController,
            role = currentUser?.role
        )
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            val authScreens = listOf(
                Screen.Login.route,
                Screen.Register.route,
                Screen.ForgotPassword.route,
                Screen.Onboarding.route
            )
            if (authState is AuthState.Authenticated && 
                currentRoute !in authScreens
            ) {
                ModernBottomBar(
                    navController = navController,
                    onShowSecondarySheet = { showSecondarySheet = true },
                    role = currentUser?.role
                )
            }
        }
    ) { innerPadding ->
        val context = LocalContext.current
        
        // Empêcher l'affichage du NavHost tant que la session n'est pas vérifiée (Idle)
        if (authState is AuthState.Idle) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NoorBlue)
            }
            return@Scaffold
        }

        // L'utilisateur souhaite voir l'onboarding à chaque lancement
        val startDest = Screen.Onboarding.route

        NavHost(
            navController = navController,
            startDestination = startDest,
            modifier = Modifier.padding(innerPadding)
        ) {
            // New Onboarding Route (General Slides)
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onLoginClick = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            // Auth Flow
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        if (authState is AuthState.Authenticated) {
                            val user = (authState as AuthState.Authenticated).user
                            if (user.role == UserRole.TECHNICIAN && user.isFirstLogin) {
                                navController.navigate(Screen.TechWelcome.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            } else {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            }
                        }
                    },
                    onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                    onNavigateToForgotPassword = { navController.navigate(Screen.ForgotPassword.route) },
                    viewModel = authViewModel,
                    allowAutoLogin = false
                )
            }
            composable(Screen.Register.route) {
                RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Register.route) { inclusive = true }
                        }
                    },
                    onBackPressed = { navController.popBackStack() },
                    viewModel = authViewModel
                )
            }
            composable(Screen.ForgotPassword.route) {
                ForgotPasswordScreen(onBackPressed = { navController.popBackStack() })
            }
            composable(Screen.AdminTechMgmt.route) {
                AdminTechManagementScreen(
                    onBackPressed = { navController.popBackStack() },
                    viewModel = authViewModel
                )
            }
            // Welcome for Technician (New)
            composable(Screen.TechWelcome.route) {
                TechnicianWelcomeScreen(
                    onNavigate = { route -> navController.navigate(route) },
                    onOnboardingComplete = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.TechWelcome.route) { inclusive = true }
                        }
                    },
                    authViewModel = authViewModel
                )
            }

            composable(Screen.Streetlights.route) {
                StreetlightsMapScreen(
                    onNavigateToAddStreetlight = {
                        navController.navigate(Screen.AddStreetlight.route)
                    },
                    onNavigateToEditStreetlight = { id ->
                        navController.navigate("edit_streetlight/$id")
                    },
                    role = currentUser?.role
                )
            }

            composable(Screen.Cameras.route) {
                CamerasScreen(
                    onNavigateToAddCamera = {
                        navController.navigate(Screen.AddCamera.route)
                    },
                    onNavigateToEditCamera = { id ->
                        navController.navigate("edit_camera/$id")
                    },
                    viewModel = camerasViewModel,
                    role = currentUser?.role
                )
            }

            composable(Screen.Technicians.route) {
                TechniciansScreen(
                    onNavigateToProfile = { navController.navigate(Screen.ProfileTechnician.route) },
                    viewModel = authViewModel
                )
            }
            composable(Screen.Citizens.route) {
                CitizensScreen(
                    onNavigateToProfile = { navController.navigate(Screen.ProfileCitizen.route) }
                )
            }
            composable(Screen.Reclamations.route) { 
                ReclamationsScreen(currentUser = currentUser) 
            }

            composable(Screen.Zones.route) { 
                ZonesScreen(
                    role = currentUser?.role,
                    onNavigateToAddZone = { navController.navigate(Screen.AddZone.route) },
                    onNavigateToEditZone = { id -> navController.navigate("edit_zone/$id") }
                ) 
            }
            composable(Screen.Interventions.route) { 
                InterventionsScreen(
                    onNavigateToAddIntervention = { navController.navigate(Screen.AddIntervention.route) },
                    onNavigateToEditIntervention = { id -> navController.navigate("edit_intervention/$id") }
                ) 
            }
            composable(Screen.CulturalEvents.route) { 
                CulturalEventsScreen(
                    role = currentUser?.role,
                    viewModel = culturalEventsViewModel,
                    onNavigateToAddEvent = {
                        navController.navigate(Screen.AddCulturalEvent.route)
                    }
                ) 
            }

            composable(Screen.LightingPrograms.route) {
                LightingProgramsScreen(
                    role = currentUser?.role,
                    onNavigateToAddProgram = {
                        navController.navigate(Screen.AddLightingProgram.route)
                    },
                    onNavigateToEditProgram = { id ->
                        navController.navigate("edit_lighting_program/$id")
                    }
                )
            }

            composable(Screen.Sensors.route) {
                SensorsScreen(
                    role = currentUser?.role,
                    onNavigateToAddSensor = {
                        navController.navigate(Screen.AddSensor.route)
                    },
                    onNavigateToEditSensor = { id ->
                        navController.navigate("edit_sensor/$id")
                    },
                    viewModel = sensorsViewModel
                )
            }

            composable(Screen.AddCamera.route) {
                AddCameraScreen(
                    onAddSuccess = { navController.popBackStack() },
                    modifier = Modifier.fillMaxSize(),
                    viewModel = camerasViewModel
                )
            }

            composable(
                route = Screen.EditCamera.route,
                arguments = listOf(androidx.navigation.navArgument("cameraId") { type = androidx.navigation.NavType.StringType })
            ) { backStackEntry ->
                val cameraId = backStackEntry.arguments?.getString("cameraId")
                AddCameraScreen(
                    editingCameraId = cameraId,
                    onAddSuccess = { navController.popBackStack() },
                    modifier = Modifier.fillMaxSize(),
                    viewModel = camerasViewModel
                )
            }

            composable(
                route = Screen.EditStreetlight.route,
                arguments = listOf(androidx.navigation.navArgument("streetlightId") { type = androidx.navigation.NavType.StringType })
            ) { backStackEntry ->
                val streetlightId = backStackEntry.arguments?.getString("streetlightId")
                AddStreetlightScreen(
                    editingStreetlightId = streetlightId,
                    onAddSuccess = { navController.popBackStack() },
                    onBackPressed = { navController.popBackStack() }
                )
            }

            composable(Screen.AddLightingProgram.route) {
                AddLightingProgramScreen(
                    onAddSuccess = { navController.popBackStack() },
                    modifier = Modifier.fillMaxSize(),
                    culturalEventsViewModel = culturalEventsViewModel // Shared instance
                )
            }

            composable(
                route = Screen.EditLightingProgram.route,
                arguments = listOf(androidx.navigation.navArgument("programId") { type = androidx.navigation.NavType.StringType })
            ) { backStackEntry ->
                val programId = backStackEntry.arguments?.getString("programId")
                AddLightingProgramScreen(
                    editingProgramId = programId,
                    onAddSuccess = { navController.popBackStack() },
                    modifier = Modifier.fillMaxSize(),
                    culturalEventsViewModel = culturalEventsViewModel
                )
            }

            composable(Screen.AddStreetlight.route) {
                AddStreetlightScreen(
                    onAddSuccess = { navController.popBackStack() },
                    onBackPressed = { navController.popBackStack() },
                    modifier = Modifier.fillMaxSize()
                )
            }
            composable(Screen.AddTechnician.route) {
                Text("Ajout technicien - À implémenter", modifier = Modifier.fillMaxSize().padding(16.dp))
            }
            composable(Screen.AddCitizen.route) {
                Text("Ajout citoyen - À implémenter", modifier = Modifier.fillMaxSize().padding(16.dp))
            }
            composable(Screen.AddReclamation.route) {
                AddReclamationScreen(
                    onAddSuccess = { navController.popBackStack() },
                    onBackPressed = { navController.popBackStack() },
                    currentUser = currentUser
                )
            }
            composable(Screen.AddIntervention.route) {
                AddInterventionScreen(
                    onAddSuccess = { navController.popBackStack() },
                    onBackPressed = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.EditIntervention.route,
                arguments = listOf(androidx.navigation.navArgument("interventionId") { type = androidx.navigation.NavType.StringType })
            ) { backStackEntry ->
                val interventionId = backStackEntry.arguments?.getString("interventionId")
                AddInterventionScreen(
                    editingInterventionId = interventionId,
                    onAddSuccess = { navController.popBackStack() },
                    onBackPressed = { navController.popBackStack() }
                )
            }
            composable(Screen.AddCulturalEvent.route) {
                AddCulturalEventScreen(
                    viewModel = culturalEventsViewModel,
                    onAddSuccess = { navController.popBackStack() },
                    onBackPressed = { navController.popBackStack() },
                    modifier = Modifier.fillMaxSize()
                )
            }
            composable(Screen.AddZone.route) {
                AddZoneScreen(
                    onAddSuccess = { navController.popBackStack() },
                    onBackPressed = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.EditZone.route,
                arguments = listOf(androidx.navigation.navArgument("zoneId") { type = androidx.navigation.NavType.StringType })
            ) { backStackEntry ->
                val zoneId = backStackEntry.arguments?.getString("zoneId")
                AddZoneScreen(
                    editingZoneId = zoneId,
                    onAddSuccess = { navController.popBackStack() },
                    onBackPressed = { navController.popBackStack() }
                )
            }
            composable(Screen.AddSensor.route) {
                AddSensorScreen(
                    onBack = { navController.popBackStack() },
                    viewModel = sensorsViewModel
                )
            }

            composable(
                route = Screen.EditSensor.route,
                arguments = listOf(androidx.navigation.navArgument("sensorId") { type = androidx.navigation.NavType.StringType })
            ) { backStackEntry ->
                val sensorId = backStackEntry.arguments?.getString("sensorId")
                AddSensorScreen(
                    editingSensorId = sensorId,
                    onBack = { navController.popBackStack() },
                    viewModel = sensorsViewModel
                )
            }

            composable(Screen.UsersList.route) {
                UsersListScreen(
                    currentUserId = currentUser?.uid ?: "",
                    viewModel = chatViewModel,
                    onUserSelected = { id, name ->
                        navController.navigate("chat/$id/$name")
                    },
                    onBackPressed = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.Chat.route,
                arguments = listOf(
                    androidx.navigation.navArgument("targetId") { type = androidx.navigation.NavType.StringType },
                    androidx.navigation.navArgument("targetName") { type = androidx.navigation.NavType.StringType }
                )
            ) { backStackEntry ->
                val targetId = backStackEntry.arguments?.getString("targetId") ?: ""
                val targetName = backStackEntry.arguments?.getString("targetName") ?: ""
                
                ChatScreen(
                    currentUserId = currentUser?.uid ?: "",
                    currentUserName = currentUser?.name ?: "Inconnu",
                    targetUserId = targetId,
                    targetUserName = targetName,
                    viewModel = chatViewModel,
                    onBackPressed = { navController.popBackStack() }
                )
            }

            composable(Screen.Home.route) { 
                // Redirect if technician and first login
                LaunchedEffect(currentUser) {
                    if (currentUser?.role == UserRole.TECHNICIAN && currentUser?.isFirstLogin == true) {
                        navController.navigate(Screen.TechWelcome.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                }

                HomeScreenModern(
                    settingsViewModel = settingsViewModel,
                    adminDashboardViewModel = adminDashboardViewModel,
                    role = currentUser?.role,
                    userName = currentUser?.name,
                    onNavigate = { route -> navController.navigate(route) }
                ) 
            }
            
            composable(Screen.History.route) { HistoryScreen() }
            
            composable(Screen.ProfileTechnician.route) {
                ProfileScreen(
                    account = currentUser,
                    onLogOut = { 
                        authViewModel.logout()
                        navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                    },
                    onDeleteAccount = {
                        authViewModel.deleteAccount()
                        navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                    },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onEditProfile = { navController.navigate(Screen.EditProfile.route) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.ProfileCitizen.route) {
                ProfileScreen(
                    account = currentUser,
                    onLogOut = { 
                        authViewModel.logout()
                        navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                    },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onEditProfile = { navController.navigate(Screen.EditProfile.route) },
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable(Screen.EditProfile.route) {
                EditProfileScreen(
                    onBack = { navController.popBackStack() },
                    onChangePassword = { navController.navigate(Screen.ChangePassword.route) },
                    onSetSecurityQuestions = { navController.navigate(Screen.SetSecurityQuestions.route) },
                    onSuccess = { navController.popBackStack() },
                    viewModel = authViewModel
                )
            }
            
            composable(Screen.ChangePassword.route) {
                ChangePasswordScreen(
                    onBack = { navController.popBackStack() },
                    onSuccess = { navController.popBackStack() },
                    viewModel = authViewModel
                )
            }
            composable(Screen.SetSecurityQuestions.route) {
                SetSecurityQuestionsScreen(
                    onBack = { navController.popBackStack() },
                    onSuccess = { navController.popBackStack() },
                    viewModel = authViewModel
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    currentUser = currentUser,
                    onBack = { navController.popBackStack() },
                    onLogout = { 
                        authViewModel.logout()
                        navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                    },
                    settingsViewModel = settingsViewModel,
                    onProfileClick = {
                        val profileRoute = if (currentUser?.role == UserRole.CITIZEN) Screen.ProfileCitizen.route else Screen.ProfileTechnician.route
                        navController.navigate(profileRoute)
                    }
                )
            }
        }
    }
}