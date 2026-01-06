package tn.esprit.sansa.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import tn.esprit.sansa.ui.screens.models.SecurityQuestion
import tn.esprit.sansa.ui.theme.*
import tn.esprit.sansa.ui.viewmodels.AuthState
import tn.esprit.sansa.ui.viewmodels.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetSecurityQuestionsScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: AuthViewModel
) {
    val authState by viewModel.authState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val context = LocalContext.current
    
    // Initialiser avec les questions existantes ou 3 vides
    val initialQuestions = remember(currentUser) {
        val existing = currentUser?.securityQuestions ?: emptyList()
        if (existing.size == 3) existing else listOf(
            SecurityQuestion("", ""),
            SecurityQuestion("", ""),
            SecurityQuestion("", "")
        )
    }

    var questions by remember { mutableStateOf(initialQuestions) }
    
    // Questions déjà sélectionnées pour éviter les doublons
    val selectedQuestionStrings = questions.map { it.question }.filter { it.isNotBlank() }

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
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Retour", tint = Color.White)
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    "Sécurité",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(32.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                color = Color.White.copy(alpha = 0.98f),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        "Configurez vos 3 questions",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        "Elles serviront à récupérer votre compte en cas d'oubli de mot de passe.",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    questions.forEachIndexed { index, questionItem ->
                        SecurityQuestionInput(
                            index = index + 1,
                            selectedQuestion = questionItem.question,
                            answer = questionItem.answer,
                            predefinedQuestions = viewModel.predefinedQuestions,
                            alreadySelected = selectedQuestionStrings,
                            onQuestionChange = { newQ ->
                                val list = questions.toMutableList()
                                list[index] = list[index].copy(question = newQ)
                                questions = list
                            },
                            onAnswerChange = { newA ->
                                val list = questions.toMutableList()
                                list[index] = list[index].copy(answer = newA)
                                questions = list
                            }
                        )
                        if (index < 2) Spacer(Modifier.height(24.dp))
                    }

                    Spacer(Modifier.height(40.dp))

                    val canSave = questions.all { it.question.isNotBlank() && it.answer.trim().length >= 2 }

                    Button(
                        onClick = {
                            viewModel.updateSecurityQuestions(questions) {
                                Toast.makeText(context, "Questions enregistrées !", Toast.LENGTH_SHORT).show()
                                onSuccess()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NoorBlue),
                        enabled = canSave && authState !is AuthState.Loading
                    ) {
                        if (authState is AuthState.Loading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Enregistrer les questions", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityQuestionInput(
    index: Int,
    selectedQuestion: String,
    answer: String,
    predefinedQuestions: List<String>,
    alreadySelected: List<String>,
    onQuestionChange: (String) -> Unit,
    onAnswerChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            "Question $index",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = NoorIndigo,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedQuestion,
                onValueChange = {},
                readOnly = true,
                placeholder = { Text("Choisir une question") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NoorBlue,
                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                )
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color.White)
            ) {
                predefinedQuestions.forEach { question ->
                    val isUsed = question in alreadySelected && question != selectedQuestion
                    DropdownMenuItem(
                        text = { 
                            Text(
                                question,
                                color = if (isUsed) Color.LightGray else Color.Black
                            ) 
                        },
                        onClick = {
                            if (!isUsed) {
                                onQuestionChange(question)
                                expanded = false
                            }
                        },
                        enabled = !isUsed
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = answer,
            onValueChange = onAnswerChange,
            label = { Text("Votre réponse") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NoorBlue,
                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
            )
        )
    }
}
