package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Mic
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import java.util.Locale
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DiaryEntry

@Composable
fun JournalEntryScreen(
    viewModel: DiaryViewModel,
    dateStr: String,
    existingEntry: DiaryEntry?,
    prefilledTitle: String = "",
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf(existingEntry?.title ?: prefilledTitle) }
    var content by remember { mutableStateOf(existingEntry?.content ?: "") }
    var selectedMood by remember { mutableStateOf(existingEntry?.mood ?: "Neutral") }
    var tags by remember { mutableStateOf(existingEntry?.tags ?: "") }

    var aiSentiment by remember { mutableStateOf(existingEntry?.aiSentiment ?: "") }
    var aiAdvice by remember { mutableStateOf(existingEntry?.aiAdvice ?: "") }

    val aiAnalysisState by viewModel.aiAnalysisState.collectAsState()

    val context = LocalContext.current
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.getOrNull(0) ?: ""
            if (spokenText.isNotEmpty()) {
                val formattedText = spokenText.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                content = if (content.trim().isEmpty()) {
                    formattedText
                } else {
                    "${content.trim()} $formattedText"
                }
            }
        }
    }

    val moodsList = listOf(
        Pair("Excelente", "🌟"),
        Pair("Bueno", "😊"),
        Pair("Neutral", "😐"),
        Pair("Triste", "😢"),
        Pair("Estresado", "😫")
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D1513), // Deep ambient teal-black
                        Color(0xFF070A09)  // Deepest obsidian slate
                    )
                )
            ),
        containerColor = Color.Transparent,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowBack,
                        contentDescription = "Atrás",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                Text(
                    text = if (existingEntry != null) "Editar Entrada" else "Nueva Entrada",
                    style = MaterialTheme.typography.titleLarge.copy(
                        letterSpacing = 0.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                IconButton(
                    onClick = {
                        // Fast save locally
                        val savedEntry = DiaryEntry(
                            id = existingEntry?.id ?: 0,
                            date = dateStr,
                            title = title.ifEmpty { "Entrada del $dateStr" },
                            content = content,
                            mood = selectedMood,
                            tags = tags,
                            aiSentiment = aiSentiment,
                            aiAdvice = aiAdvice,
                            isSynced = false
                        )
                        viewModel.insertEntry(savedEntry) {
                            onBack()
                        }
                    },
                    modifier = Modifier.testTag("save_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Guardar",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = formatDisplayDate(dateStr),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Title TextField
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Título de la entrada (Opcional)", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("title_field"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    unfocusedBorderColor = Color(0xFF223530).copy(alpha = 0.4f),
                    focusedContainerColor = Color(0xFF131D1B).copy(alpha = 0.5f),
                    unfocusedContainerColor = Color(0xFF111715).copy(alpha = 0.5f)
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Header Row for Content and Voice Dictation
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tu Historia de Hoy",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        .clickable {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().language)
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Dicta tu entrada de hoy...")
                            }
                            try {
                                speechLauncher.launch(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "El dictado por voz no está disponible", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("dictate_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Dictar entrada con voz",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Dictar con Voz",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Content Area
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                placeholder = { 
                    Text(
                        text = "¿Cómo te fue hoy? Escribe libremente sobre tus logros, preocupaciones, emociones...",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        style = MaterialTheme.typography.bodyMedium
                    ) 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .testTag("content_field"),
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    unfocusedBorderColor = Color(0xFF223530).copy(alpha = 0.4f),
                    focusedContainerColor = Color(0xFF131D1B).copy(alpha = 0.5f),
                    unfocusedContainerColor = Color(0xFF111715).copy(alpha = 0.5f)
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Word and character count + reading time estimate
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val wordCount = if (content.isBlank()) 0 else content.trim().split("\\s+".toRegex()).size
                val charCount = content.length
                Text(
                    text = "$charCount caracteres • $wordCount palabras",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )

                val readTime = (wordCount / 200).coerceAtLeast(1)
                Text(
                    text = "Lectura: ~$readTime min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Mood Selector
            Text(
                text = "¿Cómo definirías tu estado de ánimo general hoy?",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                moodsList.forEach { (moodName, emoji) ->
                    val isSelected = selectedMood == moodName
                    
                    val moodColor = when (moodName) {
                        "Excelente" -> Color(0xFFB388FF)
                        "Bueno" -> Color(0xFF4DB6AC)
                        "Neutral" -> Color(0xFF78909C)
                        "Triste" -> Color(0xFF9FA8DA)
                        "Estresado" -> Color(0xFFBA68C8)
                        else -> MaterialTheme.colorScheme.primary
                    }

                    val chipBg = if (isSelected) moodColor.copy(alpha = 0.15f) else Color(0xFF131D1B).copy(alpha = 0.6f)
                    val chipBorderColor = if (isSelected) moodColor else Color(0xFF223530).copy(alpha = 0.4f)
                    val chipTextColor = if (isSelected) moodColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(chipBg)
                            .border(1.dp, chipBorderColor, RoundedCornerShape(14.dp))
                            .clickable { selectedMood = moodName }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = emoji, fontSize = 20.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = moodName,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = chipTextColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Tags TextField
            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                placeholder = { Text("Etiquetas (separadas por comas, ej: salud, familia, exito)", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
                leadingIcon = { Icon(Icons.Default.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tags_field"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    unfocusedBorderColor = Color(0xFF223530).copy(alpha = 0.4f),
                    focusedContainerColor = Color(0xFF131D1B).copy(alpha = 0.5f),
                    unfocusedContainerColor = Color(0xFF111715).copy(alpha = 0.5f)
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // AI Sentiment Analysis Trigger Button
            Button(
                onClick = {
                    if (content.trim().isNotEmpty()) {
                        viewModel.analyzeAndSaveEntry(
                            date = dateStr,
                            title = title,
                            content = content,
                            manualMood = selectedMood,
                            tags = tags
                        ) { updatedEntry ->
                            aiSentiment = updatedEntry.aiSentiment
                            aiAdvice = updatedEntry.aiAdvice
                            selectedMood = updatedEntry.mood
                        }
                    }
                },
                enabled = content.trim().isNotEmpty() && aiAnalysisState != "loading",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("analyze_ai_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (aiAnalysisState == "loading") {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("IA Analizando Sentimiento...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Guardar y Analizar con IA", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // AI Advice card
            AnimatedVisibility(visible = aiAdvice.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF223530).copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                        .testTag("ai_result_card")
                        .padding(bottom = 32.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF13201D).copy(alpha = 0.8f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Análisis de Sentimiento IA",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = selectedMood,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "\"$aiSentiment\"",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            fontStyle = FontStyle.Italic,
                            fontFamily = FontFamily.Serif,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Advice Sub-card
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary, // Match primary natural tone
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Consejo Motivacional",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = aiAdvice,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontStyle = FontStyle.Italic,
                                    fontFamily = FontFamily.Serif,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
