package tn.esprit.sansa.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import tn.esprit.sansa.ui.theme.*
import tn.esprit.sansa.ui.viewmodels.AuthState
import tn.esprit.sansa.ui.viewmodels.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onBackPressed: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    
    var observedLoading by remember { mutableStateOf(false) }
    
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current

    // Validation helpers
    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun getPasswordStrength(password: String): Int {
        var strength = 0
        if (password.length >= 8) strength++
        if (password.any { it.isUpperCase() }) strength++
        if (password.any { it.isDigit() }) strength++
        if (password.any { !it.isLetterOrDigit() }) strength++
        return strength
    }

    fun getPasswordStrengthLabel(strength: Int): String {
        return when (strength) {
            0, 1 -> "Faible"
            2 -> "Moyen"
            3, 4 -> "Fort"
            else -> ""
        }
    }

    fun getPasswordStrengthColor(strength: Int): Color {
        return when (strength) {
            0, 1 -> Color.Red
            2 -> Color(0xFFFF9800)
            3, 4 -> Color(0xFF4CAF50)
            else -> Color.Gray
        }
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Loading) {
            observedLoading = true
        }
        if (authState is AuthState.Authenticated && observedLoading) {
            onRegisterSuccess()
        } else if (authState is AuthState.Error) {
            Toast.makeText(context, (authState as AuthState.Error).message, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Decorative background elements
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(400.dp)
                .offset(x = (-100).dp, y = 100.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(NoorBlue.copy(alpha = 0.1f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(200.dp)
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackPressed,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(NoorBlue.copy(alpha = 0.05f))
                ) {
                    Icon(Icons.Default.ArrowBackIos, contentDescription = "Retour", tint = NoorBlue, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(16.dp))
            
            Text(
                "Créer un compte",
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black,
                modifier = Modifier.align(Alignment.Start),
                letterSpacing = (-1).sp
            )
            Text(
                "Rejoignez l'aventure NoorCity",
                fontSize = 15.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp).align(Alignment.Start)
            )

            Spacer(Modifier.height(48.dp))

            // Form
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Nom complet", color = Color.Gray.copy(alpha = 0.6f)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = NoorBlue.copy(alpha = 0.7f), modifier = Modifier.size(20.dp)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NoorBlue,
                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                        focusedContainerColor = NoorBlue.copy(alpha = 0.02f),
                        unfocusedContainerColor = NoorBlue.copy(alpha = 0.02f)
                    )
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { 
                        email = it
                        emailError = if (it.isNotBlank() && !isValidEmail(it)) {
                            "Format d'email invalide"
                        } else null
                    },
                    placeholder = { Text("Adresse email", color = Color.Gray.copy(alpha = 0.6f)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true,
                    isError = emailError != null,
                    leadingIcon = { Icon(Icons.Default.Email, null, tint = if (emailError != null) Color.Red else NoorBlue.copy(alpha = 0.7f), modifier = Modifier.size(20.dp)) },
                    supportingText = emailError?.let { { Text(it, color = Color.Red, fontSize = 12.sp) } },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NoorBlue,
                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                        focusedContainerColor = NoorBlue.copy(alpha = 0.02f),
                        unfocusedContainerColor = NoorBlue.copy(alpha = 0.02f),
                        errorBorderColor = Color.Red
                    )
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { 
                        password = it
                        passwordError = when {
                            it.isBlank() -> null
                            it.length < 8 -> "Minimum 8 caractères"
                            !it.any { c -> c.isUpperCase() } -> "Au moins une majuscule"
                            !it.any { c -> c.isDigit() } -> "Au moins un chiffre"
                            else -> null
                        }
                    },
                    placeholder = { Text("Mot de passe", color = Color.Gray.copy(alpha = 0.6f)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true,
                    isError = passwordError != null,
                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = if (passwordError != null) Color.Red else NoorBlue.copy(alpha = 0.7f), modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                null,
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    supportingText = passwordError?.let { { Text(it, color = Color.Red, fontSize = 12.sp) } },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NoorBlue,
                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                        focusedContainerColor = NoorBlue.copy(alpha = 0.02f),
                        unfocusedContainerColor = NoorBlue.copy(alpha = 0.02f),
                        errorBorderColor = Color.Red
                    )
                )

                // Password strength indicator
                if (password.isNotBlank()) {
                    val strength = getPasswordStrength(password)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Force:", fontSize = 12.sp, color = Color.Gray)
                        LinearProgressIndicator(
                            progress = strength / 4f,
                            modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = getPasswordStrengthColor(strength),
                            trackColor = Color.LightGray.copy(alpha = 0.3f)
                        )
                        Text(
                            getPasswordStrengthLabel(strength),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = getPasswordStrengthColor(strength)
                        )
                    }
                }

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    placeholder = { Text("Confirmer le mot de passe", color = Color.Gray.copy(alpha = 0.6f)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.VerifiedUser, null, tint = NoorBlue.copy(alpha = 0.7f), modifier = Modifier.size(20.dp)) },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NoorBlue,
                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                        focusedContainerColor = NoorBlue.copy(alpha = 0.02f),
                        unfocusedContainerColor = NoorBlue.copy(alpha = 0.02f)
                    )
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (password == confirmPassword) {
                            viewModel.registerCitizen(name, email, password)
                        } else {
                            Toast.makeText(context, "Mots de passe différents", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NoorBlue),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    enabled = authState !is AuthState.Loading && 
                              name.isNotBlank() && 
                              email.isNotBlank() && 
                              password.length >= 8 &&
                              emailError == null &&
                              passwordError == null &&
                              password == confirmPassword
                ) {
                    if (authState is AuthState.Loading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Text("S'inscrire maintenant", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = "En s'inscrivant, vous acceptez nos Conditions d'utilisation",
                fontSize = 11.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }
    }
}
