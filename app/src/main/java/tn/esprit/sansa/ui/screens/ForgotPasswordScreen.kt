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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import tn.esprit.sansa.ui.theme.*
import tn.esprit.sansa.ui.viewmodels.AuthViewModel
import tn.esprit.sansa.ui.viewmodels.AuthState

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ForgotPasswordScreen(
    onBackPressed: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    val authState by viewModel.authState.collectAsState()
    val recoveryUser by viewModel.recoveryUser.collectAsState()
    
    val context = LocalContext.current
    
    // Étape actuelle du wizard
    var step by remember { mutableStateOf(RecoveryStep.EMAIL) }
    
    // Réponses du quiz
    var answers by remember { mutableStateOf(mutableMapOf<String, String>()) }
    
    // Nouveaux identifiants
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(NoorBlue, NoorIndigo)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (step == RecoveryStep.EMAIL) onBackPressed()
                        else {
                            step = when(step) {
                                RecoveryStep.QUIZ -> RecoveryStep.EMAIL
                                RecoveryStep.RESET -> RecoveryStep.QUIZ
                                else -> RecoveryStep.EMAIL
                            }
                            if (step == RecoveryStep.EMAIL) viewModel.resetRecovery()
                        }
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Retour", tint = Color.White)
                }
            }

            Spacer(Modifier.height(32.dp))
            
            Text(
                "Récupération",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                modifier = Modifier.align(Alignment.Start)
            )
            Text(
                when(step) {
                    RecoveryStep.EMAIL -> "Identifiez votre compte"
                    RecoveryStep.QUIZ -> "Défi d'identité"
                    RecoveryStep.RESET -> "Nouveau départ"
                    RecoveryStep.SUCCESS -> "Accès rétabli"
                },
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(Modifier.height(48.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                shape = RoundedCornerShape(32.dp),
                color = Color.White.copy(alpha = 0.98f),
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedContent(
                        targetState = step,
                        transitionSpec = {
                            fadeIn() with fadeOut()
                        }
                    ) { targetStep ->
                        when (targetStep) {
                            RecoveryStep.EMAIL -> EmailStep(
                                email = email,
                                onEmailChange = { email = it },
                                isLoading = authState is AuthState.Loading,
                                onNext = {
                                    viewModel.findUserForRecovery(email)
                                }
                            )
                            RecoveryStep.QUIZ -> QuizStep(
                                user = recoveryUser!!,
                                answers = answers,
                                onAnswerChange = { q, a -> 
                                    val newMap = answers.toMutableMap()
                                    newMap[q] = a
                                    answers = newMap
                                },
                                onNext = {
                                    // Vérifier les 3 réponses
                                    val allCorrect = recoveryUser!!.securityQuestions.all { sq ->
                                        val userAnswer = answers[sq.question]?.trim() ?: ""
                                        userAnswer.equals(sq.answer.trim(), ignoreCase = true)
                                    }
                                    
                                    if (allCorrect) {
                                        step = RecoveryStep.RESET
                                    } else {
                                        Toast.makeText(context, "Une ou plusieurs réponses sont incorrectes.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            )
                            RecoveryStep.RESET -> NewPasswordStep(
                                password = newPassword,
                                onPasswordChange = { newPassword = it },
                                confirm = confirmPassword,
                                onConfirmChange = { confirmPassword = it },
                                isLoading = authState is AuthState.Loading,
                                onNext = {
                                    if (newPassword == confirmPassword) {
                                        viewModel.resetPasswordWithRecovery(recoveryUser!!.uid, newPassword) {
                                            step = RecoveryStep.SUCCESS
                                        }
                                    } else {
                                        Toast.makeText(context, "Les mots de passe ne correspondent pas.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            RecoveryStep.SUCCESS -> SuccessStep(
                                onFinish = onBackPressed
                            )
                        }
                    }
                }
            }
        }
    }

    // Gérer la redirection après recherche utilisateur
    LaunchedEffect(recoveryUser) {
        if (recoveryUser != null && step == RecoveryStep.EMAIL) {
            if (recoveryUser!!.securityQuestions.size == 3) {
                step = RecoveryStep.QUIZ
            } else {
                Toast.makeText(context, "Ce compte n'a pas configuré de questions de sécurité.", Toast.LENGTH_LONG).show()
                viewModel.resetRecovery()
            }
        }
    }

    // Afficher les erreurs du ViewModel
    LaunchedEffect(authState) {
        if (authState is AuthState.Error) {
            Toast.makeText(context, (authState as AuthState.Error).message, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }
}

enum class RecoveryStep { EMAIL, QUIZ, RESET, SUCCESS }

@Composable
fun EmailStep(
    email: String,
    onEmailChange: (String) -> Unit,
    isLoading: Boolean,
    onNext: () -> Unit
) {
    Column {
        Text(
            "Entrez votre adresse email pour commencer le défi d'identité.",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, null, tint = NoorBlue) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NoorBlue,
                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
            )
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NoorBlue),
            enabled = email.isNotBlank() && !isLoading
        ) {
            if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            else Text("Continuer", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun QuizStep(
    user: tn.esprit.sansa.ui.screens.models.UserAccount,
    answers: Map<String, String>,
    onAnswerChange: (String, String) -> Unit,
    onNext: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Bonjour ${user.name.split(" ").first()} !",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Text(
            "Répondez à vos 3 questions de sécurité.",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        user.securityQuestions.forEach { sq ->
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Text(sq.question, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NoorIndigo)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = answers[sq.question] ?: "",
                    onValueChange = { onAnswerChange(sq.question, it) },
                    placeholder = { Text("Votre réponse...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NoorBlue),
            enabled = answers.size == 3 && answers.values.all { it.isNotBlank() }
        ) {
            Text("Vérifier les réponses", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun NewPasswordStep(
    password: String,
    onPasswordChange: (String) -> Unit,
    confirm: String,
    onConfirmChange: (String) -> Unit,
    isLoading: Boolean,
    onNext: () -> Unit
) {
    Column {
        Text(
            "Identité confirmée ! Choisissez un nouveau mot de passe.",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Nouveau mot de passe") },
            leadingIcon = { Icon(Icons.Default.Lock, null, tint = NoorBlue) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = confirm,
            onValueChange = onConfirmChange,
            label = { Text("Confirmer le mot de passe") },
            leadingIcon = { Icon(Icons.Default.LockReset, null, tint = NoorBlue) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NoorGreen),
            enabled = password.length >= 6 && !isLoading
        ) {
            if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            else Text("Réinitialiser le mot de passe", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SuccessStep(onFinish: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = NoorGreen
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "Mot de passe mis à jour !",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = NoorBlue
        )
        Text(
            "Vous pouvez maintenant vous connecter avec votre nouveau mot de passe.",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NoorBlue)
        ) {
            Text("Se connecter maintenant")
        }
    }
}
