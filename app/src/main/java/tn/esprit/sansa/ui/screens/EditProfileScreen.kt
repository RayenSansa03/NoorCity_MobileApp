// EditProfileScreen.kt
package tn.esprit.sansa.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import tn.esprit.sansa.data.repositories.FirebaseZonesRepository
import tn.esprit.sansa.ui.screens.models.*
import tn.esprit.sansa.ui.theme.NoorBlue
import tn.esprit.sansa.ui.theme.NoorRed
import tn.esprit.sansa.ui.viewmodels.AuthState
import tn.esprit.sansa.ui.viewmodels.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable

fun EditProfileScreen(
    onBack: () -> Unit,
    onChangePassword: () -> Unit,
    onSetSecurityQuestions: () -> Unit, // New parameter
    onSuccess: () -> Unit, // Navigate back or show success message then back
    viewModel: AuthViewModel = viewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val authState by viewModel.authState.collectAsState()
    
    // Initial values populated from currentUser
    var name by remember(currentUser) { mutableStateOf(currentUser?.name ?: "") }
    var email by remember(currentUser) { mutableStateOf(currentUser?.email ?: "") }
    var phone by remember(currentUser) { mutableStateOf(currentUser?.phoneNumber ?: "") }
    var selectedZone by remember(currentUser) { mutableStateOf(currentUser?.workingZone ?: "") }
    var specialty by remember(currentUser) { mutableStateOf(currentUser?.specialty ?: "") }
    
    val zonesRepository = remember { FirebaseZonesRepository() }
    val zones by zonesRepository.getZones().collectAsState(initial = emptyList())
    var expanded by remember { mutableStateOf(false) }
    
    // Derived state
    val isTechnician = currentUser?.role == UserRole.TECHNICIAN
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Modifier le profil") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // --- Fields ---
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nom complet") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Outlined.Person, null) }
            )
            
            Spacer(Modifier.height(16.dp))
            
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Outlined.Email, null) }
            )
            
            Spacer(Modifier.height(16.dp))
            
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Téléphone") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Outlined.Phone, null) }
            )
            
            Spacer(Modifier.height(16.dp))
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedZone,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Ma Zone") },
                    leadingIcon = { Icon(Icons.Outlined.LocationOn, null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    zones.forEach { zone ->
                        DropdownMenuItem(
                            text = { Text(zone.name) },
                            onClick = {
                                selectedZone = zone.name
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            if (isTechnician) {
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = specialty,
                    onValueChange = { specialty = it },
                    label = { Text("Spécialité") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Outlined.Badge, null) }
                )
            }

            Spacer(Modifier.height(32.dp))

            // --- Security Section ---
            OutlinedButton(
                onClick = onSetSecurityQuestions,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Outlined.Security, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Configurer questions de sécurité")
            }

            Spacer(Modifier.height(16.dp))

            // --- Password Change Button ---
            OutlinedButton(
                onClick = onChangePassword,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Outlined.LockReset, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Modifier le mot de passe")
            }
            
            Spacer(Modifier.height(32.dp))
            
            // --- Save Button ---
            Button(
                onClick = {
                    val updates = mutableMapOf<String, Any>(
                        "name" to name,
                        "email" to email,
                        "phoneNumber" to phone,
                        "workingZone" to selectedZone
                    )
                    if (isTechnician) {
                        updates["specialty"] = specialty
                    }
                    // Trigger update via ViewModel
                    viewModel.updateUserProfile(updates)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NoorBlue)
            ) {
                if (authState is AuthState.Loading) {
                    CircularProgressIndicator(color = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Enregistrer les modifications", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            // Error handling display
             if (authState is AuthState.Error) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = (authState as AuthState.Error).message,
                    color = NoorRed,
                    fontSize = 14.sp
                )
            }
        }
    }
}
