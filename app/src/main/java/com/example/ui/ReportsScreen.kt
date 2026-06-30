package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.core.content.FileProvider
import com.example.data.DiaryEntry
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class DashboardFilter {
    SEMANA, MES, ANIO, HISTORICO
}

@Composable
fun FilterSelector(
    selectedFilter: DashboardFilter,
    onFilterSelected: (DashboardFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = listOf(
        Pair(DashboardFilter.SEMANA, "Semana"),
        Pair(DashboardFilter.MES, "Mes"),
        Pair(DashboardFilter.ANIO, "Año"),
        Pair(DashboardFilter.HISTORICO, "Todo")
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFF131D1B).copy(alpha = 0.6f),
                shape = RoundedCornerShape(24.dp)
            )
            .border(
                width = 1.dp,
                color = Color(0xFF223530).copy(alpha = 0.4f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        filters.forEach { (filter, label) ->
            val isSelected = filter == selectedFilter
            val backgroundColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                Color.Transparent
            }
            val contentColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(backgroundColor, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onFilterSelected(filter) }
                    .padding(vertical = 10.dp)
                    .testTag("filter_button_${filter.name.lowercase()}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
fun ReportsScreen(
    viewModel: DiaryViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val entries by viewModel.entries.collectAsState()
    val currentMonthYear = SimpleDateFormat("MMMM yyyy", Locale("es", "ES")).format(Date())

    var selectedFilter by remember { mutableStateOf(DashboardFilter.MES) }

    // Filter entries dynamically based on chosen range
    val filteredEntries = remember(entries, selectedFilter) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = Calendar.getInstance()
        
        entries.filter { entry ->
            try {
                val entryDateObj = sdf.parse(entry.date) ?: return@filter false
                val entryCal = Calendar.getInstance().apply { time = entryDateObj }
                
                when (selectedFilter) {
                    DashboardFilter.SEMANA -> {
                        val limit = Calendar.getInstance().apply { 
                            time = today.time
                            add(Calendar.DAY_OF_YEAR, -7) 
                        }
                        entryCal.after(limit) || entry.date == sdf.format(today.time)
                    }
                    DashboardFilter.MES -> {
                        val limit = Calendar.getInstance().apply { 
                            time = today.time
                            add(Calendar.DAY_OF_YEAR, -30) 
                        }
                        entryCal.after(limit) || entry.date == sdf.format(today.time)
                    }
                    DashboardFilter.ANIO -> {
                        val limit = Calendar.getInstance().apply { 
                            time = today.time
                            add(Calendar.DAY_OF_YEAR, -365) 
                        }
                        entryCal.after(limit) || entry.date == sdf.format(today.time)
                    }
                    DashboardFilter.HISTORICO -> true
                }
            } catch (e: Exception) {
                true
            }
        }
    }

    // Calculate Mood statistics based on filtered list
    val totalCount = filteredEntries.size
    val moodCounts = filteredEntries.groupBy { it.mood }.mapValues { it.value.size }

    val excelenteCount = moodCounts["Excelente"] ?: 0
    val buenoCount = moodCounts["Bueno"] ?: 0
    val neutralCount = moodCounts["Neutral"] ?: 0
    val tristeCount = moodCounts["Triste"] ?: 0
    val estresadoCount = moodCounts["Estresado"] ?: 0

    val filterTitle = when (selectedFilter) {
        DashboardFilter.SEMANA -> "Resumen de los últimos 7 días"
        DashboardFilter.MES -> "Resumen de los últimos 30 días"
        DashboardFilter.ANIO -> "Resumen de este último año"
        DashboardFilter.HISTORICO -> "Resumen histórico total"
    }

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
                verticalAlignment = Alignment.CenterVertically
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
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Dashboard de Bienestar",
                    style = MaterialTheme.typography.titleLarge.copy(
                        letterSpacing = 0.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
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
            FilterSelector(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it },
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF223530).copy(alpha = 0.6f), RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF131D1B).copy(alpha = 0.75f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Distribución de Ánimo",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = filterTitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (totalCount == 0) {
                        Text(
                            text = "No tienes suficientes registros en tu diario en este periodo para generar estadísticas. ¡Escribe algunas entradas para ver tu análisis!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    } else {
                        // Stat Rows
                        StatBar("Excelente 🌟", excelenteCount, totalCount, Color(0xFFB388FF))
                        Spacer(modifier = Modifier.height(10.dp))
                        StatBar("Bueno 😊", buenoCount, totalCount, Color(0xFF4DB6AC))
                        Spacer(modifier = Modifier.height(10.dp))
                        StatBar("Neutral 😐", neutralCount, totalCount, Color(0xFF78909C))
                        Spacer(modifier = Modifier.height(10.dp))
                        StatBar("Triste 😢", tristeCount, totalCount, Color(0xFF9FA8DA))
                        Spacer(modifier = Modifier.height(10.dp))
                        StatBar("Estresado 😫", estresadoCount, totalCount, Color(0xFFBA68C8))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            MoodEvolutionChart(entries = filteredEntries)

            Spacer(modifier = Modifier.height(24.dp))

            // PDF Export Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF223530).copy(alpha = 0.6f), RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF131D1B).copy(alpha = 0.75f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Exportar Reporte PDF",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Genera un documento PDF oficial y pulido con los sentimientos diarios, estadísticas mensuales del estado de ánimo y los valiosos consejos motivacionales generados por la IA.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (filteredEntries.isNotEmpty()) {
                                exportPdf(context, filteredEntries, filterTitle)
                            } else {
                                Toast.makeText(context, "No hay entradas para exportar", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("export_pdf_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        enabled = filteredEntries.isNotEmpty()
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Exportar y Compartir PDF", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // AI Insight Tip Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .padding(bottom = 32.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Consejo de Bienestar Emocional",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Exportar tus reportes mensuales te ayuda a realizar un seguimiento retrospectivo de tus patrones de ánimo para discutir con terapeutas, consejeros o simplemente para tu crecimiento personal.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatBar(
    moodLabel: String,
    count: Int,
    total: Int,
    color: Color
) {
    val percentage = if (total > 0) count.toFloat() / total.toFloat() else 0f
    val percentageText = "${(percentage * 100).toInt()}%"

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = moodLabel,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$count días ($percentageText)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        // Progress bar track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(5.dp))
        ) {
            // Fill
            Box(
                modifier = Modifier
                    .fillMaxWidth(percentage)
                    .height(10.dp)
                    .background(color, RoundedCornerShape(5.dp))
            )
        }
    }
}

/**
 * Native PDF generation and share on Android using standard PdfDocument.
 */
fun exportPdf(context: Context, entries: List<DiaryEntry>, monthYearStr: String) {
    val pdfDocument = PdfDocument()
    
    // Page Info (A4 size: 595 x 842 points)
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    var page = pdfDocument.startPage(pageInfo)
    var canvas = page.canvas

    val paint = Paint()
    val titlePaint = Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 18f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val subtitlePaint = Paint().apply {
        color = android.graphics.Color.GRAY
        textSize = 12f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
    }

    val headerPaint = Paint().apply {
        color = android.graphics.Color.DKGRAY
        textSize = 14f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val bodyPaint = Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 11f
    }

    val boldBodyPaint = Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 11f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    var y = 50f

    // Draw header
    canvas.drawText("REPORTE DE BIENESTAR EMOCIONAL - IA", 50f, y, titlePaint)
    y += 22f
    canvas.drawText("$monthYearStr | Generado por MindScribe AI", 50f, y, subtitlePaint)
    y += 12f
    canvas.drawLine(50f, y, 545f, y, Paint().apply { strokeWidth = 2f; color = android.graphics.Color.BLACK })
    y += 25f

    // Helper functions for page flow and word wrap
    fun checkPageOverflow(estimatedHeight: Float) {
        if (y + estimatedHeight > 780f) {
            pdfDocument.finishPage(page)
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            y = 50f
            canvas.drawText("Reporte de Bienestar Emocional (Continuación)", 50f, y, subtitlePaint)
            y += 15f
            canvas.drawLine(50f, y, 545f, y, Paint().apply { strokeWidth = 1f; color = android.graphics.Color.LTGRAY })
            y += 25f
        }
    }

    fun drawWrappedText(text: String, x: Float, maxWidth: Float, textPaint: Paint, lineHeight: Float) {
        val words = text.split(" ")
        var line = StringBuilder()
        for (word in words) {
            val testLine = if (line.isEmpty()) word else "${line} $word"
            if (textPaint.measureText(testLine) > maxWidth) {
                checkPageOverflow(lineHeight)
                canvas.drawText(line.toString(), x, y, textPaint)
                y += lineHeight
                line = StringBuilder(word)
            } else {
                line = StringBuilder(testLine)
            }
        }
        if (line.isNotEmpty()) {
            checkPageOverflow(lineHeight)
            canvas.drawText(line.toString(), x, y, textPaint)
            y += lineHeight
        }
    }

    // Mood distribution statistics
    canvas.drawText("Distribución del Estado de Ánimo en este período:", 50f, y, headerPaint)
    y += 18f

    val total = entries.size
    val moodCounts = entries.groupBy { it.mood }.mapValues { it.value.size }
    val moods = listOf("Excelente", "Bueno", "Neutral", "Triste", "Estresado")

    moods.forEach { mood ->
        val count = moodCounts[mood] ?: 0
        val percentage = if (total > 0) (count * 100) / total else 0
        canvas.drawText("- $mood: $count días ($percentage%)", 70f, y, bodyPaint)
        y += 16f
    }

    y += 12f
    canvas.drawLine(50f, y, 545f, y, Paint().apply { strokeWidth = 1f; color = android.graphics.Color.LTGRAY })
    y += 25f

    canvas.drawText("Historial de Entradas y Análisis IA Detallado:", 50f, y, headerPaint)
    y += 22f

    // Sort entries chronologically for report
    val chronologicalEntries = entries.sortedBy { it.date }

    chronologicalEntries.forEachIndexed { index, entry ->
        // Check if header needs a new page
        checkPageOverflow(40f)

        canvas.drawText("${entry.date} - ${entry.title.ifBlank { "Entrada sin título" }}", 50f, y, boldBodyPaint)
        canvas.drawText("Ánimo: ${entry.mood}", 430f, y, boldBodyPaint)
        y += 16f

        // Content
        if (entry.content.isNotEmpty()) {
            drawWrappedText("Escrito: \"${entry.content}\"", 70f, 475f, bodyPaint, 14f)
            y += 4f
        }

        // Sentiment
        if (entry.aiSentiment.isNotEmpty()) {
            val italicPaint = Paint(subtitlePaint).apply { textSize = 11f }
            drawWrappedText("Análisis IA: ${entry.aiSentiment}", 70f, 475f, italicPaint, 13f)
            y += 4f
        }

        // Advice
        if (entry.aiAdvice.isNotEmpty()) {
            val advicePaint = Paint(bodyPaint).apply { 
                color = android.graphics.Color.parseColor("#1565C0") // A lovely blue tone for IA Advice
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                textSize = 11f
            }
            drawWrappedText("Consejo IA: ${entry.aiAdvice}", 70f, 475f, advicePaint, 13f)
            y += 4f
        }

        // Draw separator
        checkPageOverflow(15f)
        canvas.drawLine(50f, y, 545f, y, Paint().apply { strokeWidth = 0.5f; color = android.graphics.Color.LTGRAY })
        y += 18f
    }

    pdfDocument.finishPage(page)

    // Save PDF
    val cachePath = File(context.cacheDir, "reportes")
    cachePath.mkdirs()
    val file = File(cachePath, "reporte_bienestar_${System.currentTimeMillis()}.pdf")
    
    try {
        val fileOutputStream = FileOutputStream(file)
        pdfDocument.writeTo(fileOutputStream)
        fileOutputStream.close()
        pdfDocument.close()

        // Share the generated file via Share Intent
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Reporte de Bienestar Emocional - MindScribe AI")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(intent, "Compartir Reporte PDF"))

    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error al exportar PDF: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun MoodEvolutionChart(
    entries: List<DiaryEntry>,
    modifier: Modifier = Modifier
) {
    if (entries.isEmpty()) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Agrega más entradas al diario para ver la evolución de tu estado de ánimo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        return
    }

    // Sort entries chronologically
    val sortedEntries = remember(entries) {
        entries.sortedBy { it.date }
    }

    // Interactive state
    var selectedIndex by remember(sortedEntries) {
        mutableStateOf<Int?>(if (sortedEntries.isNotEmpty()) sortedEntries.lastIndex else null)
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val surfaceColor = MaterialTheme.colorScheme.surface
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Evolución de Ánimo Temporal (IA)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Toca o arrastra sobre el gráfico para analizar un día específico",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Canvas for chart drawing
                Box(
                    modifier = modifier
                        .fillMaxWidth()
                        .height(220.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(sortedEntries) {
                                detectTapGestures { offset ->
                                    val width = size.width
                                    val leftPaddingPx = 130f
                                    val rightPaddingPx = 30f
                                    val availableWidth = width - leftPaddingPx - rightPaddingPx
                                    val pointsCount = sortedEntries.size
                                    
                                    if (pointsCount > 1 && availableWidth > 0f) {
                                        val stepX = availableWidth / (pointsCount - 1)
                                        var bestIndex = 0
                                        var minDiff = Float.MAX_VALUE
                                        for (i in 0 until pointsCount) {
                                            val x = leftPaddingPx + i * stepX
                                            val diff = kotlin.math.abs(offset.x - x)
                                            if (diff < minDiff) {
                                                minDiff = diff
                                                bestIndex = i
                                            }
                                        }
                                        selectedIndex = bestIndex
                                    } else if (pointsCount == 1) {
                                        selectedIndex = 0
                                    }
                                }
                            }
                            .pointerInput(sortedEntries) {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    val offset = change.position
                                    val width = size.width
                                    val leftPaddingPx = 130f
                                    val rightPaddingPx = 30f
                                    val availableWidth = width - leftPaddingPx - rightPaddingPx
                                    val pointsCount = sortedEntries.size
                                    
                                    if (pointsCount > 1 && availableWidth > 0f) {
                                        val stepX = availableWidth / (pointsCount - 1)
                                        var bestIndex = 0
                                        var minDiff = Float.MAX_VALUE
                                        for (i in 0 until pointsCount) {
                                            val x = leftPaddingPx + i * stepX
                                            val diff = kotlin.math.abs(offset.x - x)
                                            if (diff < minDiff) {
                                                minDiff = diff
                                                bestIndex = i
                                            }
                                        }
                                        selectedIndex = bestIndex
                                    } else if (pointsCount == 1) {
                                        selectedIndex = 0
                                    }
                                }
                            }
                    ) {
                        val width = size.width.toFloat()
                        val height = size.height.toFloat()

                        // Define paddings inside canvas in pixels
                        val leftPaddingPx = 130f
                        val rightPaddingPx = 30f
                        val topPaddingPx = 20f
                        val bottomPaddingPx = 50f

                        val availableWidth = width - leftPaddingPx - rightPaddingPx
                        val availableHeight = height - topPaddingPx - bottomPaddingPx

                        // Mood mappings
                        val moodLevels = listOf(
                            Pair(5f, "Excelente 🌟"),
                            Pair(4f, "Bueno 😊"),
                            Pair(3f, "Neutral 😐"),
                            Pair(2f, "Triste 😢"),
                            Pair(1f, "Estresado 😫")
                        )

                        // Draw horizontal grid lines and labels
                        moodLevels.forEach { (level, label) ->
                            val y = topPaddingPx + (5f - level) * (availableHeight / 4f)
                            
                            // Grid line
                            drawLine(
                                color = gridColor,
                                start = Offset(leftPaddingPx, y),
                                end = Offset(width - rightPaddingPx, y),
                                strokeWidth = 1f
                            )

                            // Labels
                            drawContext.canvas.nativeCanvas.drawText(
                                label,
                                10f,
                                y + 10f,
                                Paint().apply {
                                    color = onSurfaceColor.copy(alpha = 0.8f).value.toInt()
                                    textSize = 28f
                                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                                }
                            )
                        }

                        if (sortedEntries.isNotEmpty()) {
                            val pointsCount = sortedEntries.size
                            val stepX = if (pointsCount > 1) availableWidth / (pointsCount - 1) else availableWidth

                            // Calculate points coordinates
                            val points = sortedEntries.mapIndexed { index, entry ->
                                val moodVal = when (entry.mood) {
                                    "Excelente" -> 5f
                                    "Bueno" -> 4f
                                    "Neutral" -> 3f
                                    "Triste" -> 2f
                                    "Estresado" -> 1f
                                    else -> 3f
                                }
                                val x = if (pointsCount > 1) leftPaddingPx + index * stepX else leftPaddingPx + availableWidth / 2f
                                val y = topPaddingPx + (5f - moodVal) * (availableHeight / 4f)
                                Offset(x, y)
                            }

                            // 1. Draw Area Gradient under the curve
                            if (points.size > 1) {
                                val areaPath = Path().apply {
                                    moveTo(points.first().x, topPaddingPx + availableHeight)
                                    points.forEach { point ->
                                        lineTo(point.x, point.y)
                                    }
                                    lineTo(points.last().x, topPaddingPx + availableHeight)
                                    close()
                                }
                                drawPath(
                                    path = areaPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            primaryColor.copy(alpha = 0.35f),
                                            primaryColor.copy(alpha = 0.0f)
                                        ),
                                        startY = topPaddingPx,
                                        endY = topPaddingPx + availableHeight
                                    )
                                )
                            }

                            // 2. Draw vertical selection helper line if selected
                            selectedIndex?.let { idx ->
                                if (idx in points.indices) {
                                    val selectedPt = points[idx]
                                    drawLine(
                                        color = secondaryColor.copy(alpha = 0.4f),
                                        start = Offset(selectedPt.x, topPaddingPx),
                                        end = Offset(selectedPt.x, topPaddingPx + availableHeight),
                                        strokeWidth = 2f,
                                        cap = StrokeCap.Round
                                    )
                                }
                            }

                            // 3. Draw the Line Trend
                            if (points.size > 1) {
                                val strokePath = Path().apply {
                                    moveTo(points.first().x, points.first().y)
                                    for (i in 1 until points.size) {
                                        lineTo(points[i].x, points[i].y)
                                    }
                                }
                                drawPath(
                                    path = strokePath,
                                    color = primaryColor,
                                    style = Stroke(width = 8f, cap = StrokeCap.Round)
                                )
                            }

                            // 4. Draw Date Labels at bottom (only show a few to avoid crowding)
                            val labelInterval = when {
                                pointsCount <= 5 -> 1
                                pointsCount <= 10 -> 2
                                else -> pointsCount / 4
                            }

                            sortedEntries.forEachIndexed { index, entry ->
                                if (index % labelInterval == 0 || index == pointsCount - 1) {
                                    val pt = points[index]
                                    // Parse date string (YYYY-MM-DD) to format it shorter, e.g., "DD/MM" or "DD"
                                    val shortDate = try {
                                        val parts = entry.date.split("-")
                                        if (parts.size >= 3) "${parts[2]}/${parts[1]}" else entry.date
                                    } catch (e: Exception) {
                                        entry.date
                                    }

                                    drawContext.canvas.nativeCanvas.drawText(
                                        shortDate,
                                        pt.x - 30f,
                                        topPaddingPx + availableHeight + 40f,
                                        Paint().apply {
                                            color = onSurfaceColor.copy(alpha = 0.5f).value.toInt()
                                            textSize = 24f
                                        }
                                    )
                                }
                            }

                            // 5. Draw interactive / highlighted points
                            points.forEachIndexed { index, pt ->
                                val isSelected = index == selectedIndex
                                val radius = if (isSelected) 14f else 8f
                                val color = if (isSelected) secondaryColor else primaryColor

                                // Point glow/shadow if selected
                                if (isSelected) {
                                    drawCircle(
                                        color = secondaryColor.copy(alpha = 0.3f),
                                        radius = radius + 10f,
                                        center = pt
                                    )
                                }

                                drawCircle(
                                    color = color,
                                    radius = radius,
                                    center = pt
                                )

                                drawCircle(
                                    color = surfaceColor,
                                    radius = radius / 2f,
                                    center = pt
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selected Day Details (Dynamic Tooltip)
        selectedIndex?.let { idx ->
            if (idx in sortedEntries.indices) {
                val entry = sortedEntries[idx]
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = try {
                                    val sdfIn = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                    val sdfOut = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "ES"))
                                    val dateObj = sdfIn.parse(entry.date)
                                    sdfOut.format(dateObj ?: Date()).replaceFirstChar { it.uppercase() }
                                } catch (e: Exception) {
                                    entry.date
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )

                            // Mood Tag
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = when (entry.mood) {
                                            "Excelente" -> Color(0xFFB388FF).copy(alpha = 0.15f)
                                            "Bueno" -> Color(0xFF4DB6AC).copy(alpha = 0.15f)
                                            "Neutral" -> Color(0xFF78909C).copy(alpha = 0.15f)
                                            "Triste" -> Color(0xFF9FA8DA).copy(alpha = 0.15f)
                                            "Estresado" -> Color(0xFFBA68C8).copy(alpha = 0.15f)
                                            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = entry.mood,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = when (entry.mood) {
                                        "Excelente" -> Color(0xFFB388FF)
                                        "Bueno" -> Color(0xFF4DB6AC)
                                        "Neutral" -> Color(0xFF78909C)
                                        "Triste" -> Color(0xFF9FA8DA)
                                        "Estresado" -> Color(0xFFBA68C8)
                                        else -> MaterialTheme.colorScheme.primary
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = entry.title.ifBlank { "Entrada sin título" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (entry.content.length > 120) entry.content.take(120) + "..." else entry.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )

                        if (entry.aiSentiment.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Análisis de Sentimiento IA",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = entry.aiSentiment,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
