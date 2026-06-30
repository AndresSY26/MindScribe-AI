package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.app.Activity
import com.example.R
import com.example.data.DiaryEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JournalListScreen(
    viewModel: DiaryViewModel,
    onAddEntryClick: (String, String) -> Unit, // passes date YYYY-MM-DD, prefilled title
    onEntryClick: (DiaryEntry) -> Unit,
    onReportsClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    val entries by viewModel.entries.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val todayEvents by viewModel.todayEvents.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val isSupabaseSyncEnabled by viewModel.isSupabaseSyncEnabled.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val registeredUsername by viewModel.registeredUsername.collectAsState()

    var showCalendarSection by remember { mutableStateOf(false) }
    var calendarYear by remember { mutableStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)) }
    var calendarMonth by remember { mutableStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)) }
    var showEmotionCalendar by remember { mutableStateOf(false) }

    val greeting = remember {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Buenos días"
            in 12..18 -> "Buenas tardes"
            else -> "Buenas noches"
        }
    }
    
    val displayName = if (isLoggedIn && registeredUsername.isNotBlank()) {
        registeredUsername.substringBefore("@").replaceFirstChar { it.uppercase() }
    } else {
        ""
    }
    
    val subtitleText = if (displayName.isNotEmpty()) {
        "$greeting, $displayName ✨"
    } else {
        "Diario de bienestar inteligente"
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    var hasRequestedPermission by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val hasCalendarPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_CALENDAR
                ) == PackageManager.PERMISSION_GRANTED

                if (hasCalendarPermission) {
                    viewModel.loadTodayEvents(context)
                } else if (!hasRequestedPermission) {
                    hasRequestedPermission = true
                    val activity = context as? Activity
                    if (activity != null) {
                        ActivityCompat.requestPermissions(
                            activity,
                            arrayOf(Manifest.permission.READ_CALENDAR),
                            101
                        )
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

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
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MindScribe AI",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Cloud Sync indicator click starts sync
                    if (isSupabaseSyncEnabled) {
                        IconButton(
                            onClick = { viewModel.syncWithSupabase() },
                            modifier = Modifier.testTag("sync_button")
                        ) {
                            Icon(
                                imageVector = when (syncState) {
                                    "syncing" -> Icons.Default.CloudSync
                                    "success" -> Icons.Default.CloudDone
                                    else -> Icons.Default.CloudQueue
                                },
                                contentDescription = "Sincronización en la nube",
                                tint = when (syncState) {
                                    "syncing" -> MaterialTheme.colorScheme.secondary
                                    "success" -> Color(0xFF4CAF50)
                                    "error" -> Color(0xFFF44336)
                                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                }
                            )
                        }
                    }

                    IconButton(
                        onClick = { showEmotionCalendar = true },
                        modifier = Modifier.testTag("open_calendar_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Calendario de Emociones",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = onReportsClick,
                        modifier = Modifier.testTag("reports_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "Reportes mensuales",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Ajustes",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAddEntryClick(todayDateStr, "") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .testTag("add_entry_button")
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Escribir hoy"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { 
                    Text(
                        text = "Buscar palabras clave o etiquetas...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    ) 
                },
                leadingIcon = { 
                    Icon(
                        imageVector = Icons.Default.Search, 
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    ) 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_field"),
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color(0xFF16221E),
                    unfocusedContainerColor = Color(0xFF121A18),
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Expandable Centralized Tasks & Calendar View Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF223530).copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                    .testTag("calendar_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF14221E).copy(alpha = 0.7f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .clickable { showCalendarSection = !showCalendarSection }
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Tareas y Eventos de Hoy",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.3.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape)
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = todayEvents.size.toString(),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Icon(
                                imageVector = if (showCalendarSection) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    AnimatedVisibility(visible = showCalendarSection) {
                        Column {
                            Spacer(modifier = Modifier.height(14.dp))
                            if (todayEvents.isEmpty()) {
                                Text(
                                    text = "No hay tareas pendientes ni eventos sincronizados en tus calendarios para hoy.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                                )
                            } else {
                                todayEvents.forEach { event ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { onAddEntryClick(todayDateStr, event.title) }
                                            .padding(vertical = 8.dp, horizontal = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EventNote,
                                            contentDescription = "Crear entrada para este evento",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1.getFloat())) {
                                            Text(
                                                text = event.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${event.getFormattedTime()} (${event.calendarName}) — Toca para escribir",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Emotion Trends Calendar trigger banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showEmotionCalendar = true }
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF203B35), // Sophisticated emerald border
                                Color(0xFF33204E)  // Sophisticated indigo/violet border
                            )
                        ),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .testTag("open_calendar_banner"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF0F1E1B).copy(alpha = 0.8f),
                                    Color(0xFF160F24).copy(alpha = 0.8f)
                                )
                            )
                        )
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Calendario de Emociones",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.2.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Toca para ver tus tendencias de ánimo",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Abrir calendario",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (showEmotionCalendar) {
                Dialog(
                    onDismissRequest = { showEmotionCalendar = false }
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .testTag("emotion_calendar_dialog")
                            .border(1.dp, Color(0xFF223530).copy(alpha = 0.6f), RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF111A18)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Dialog Title / Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Calendario de Emociones",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                
                                IconButton(
                                    onClick = { showEmotionCalendar = false },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cerrar",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Month Navigation Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val monthNames = listOf(
                                    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                                    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
                                )
                                
                                IconButton(
                                    onClick = {
                                        if (calendarMonth == 0) {
                                            calendarMonth = 11
                                            calendarYear -= 1
                                        } else {
                                            calendarMonth -= 1
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Mes anterior",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Text(
                                    text = "${monthNames[calendarMonth]} $calendarYear",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                IconButton(
                                    onClick = {
                                        if (calendarMonth == 11) {
                                            calendarMonth = 0
                                            calendarYear += 1
                                        } else {
                                            calendarMonth += 1
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = "Mes siguiente",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Weekday Labels Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                val weekdays = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
                                weekdays.forEach { dayLabel ->
                                    Text(
                                        text = dayLabel,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Grid computation
                            val cal = java.util.Calendar.getInstance().apply {
                                set(java.util.Calendar.YEAR, calendarYear)
                                set(java.util.Calendar.MONTH, calendarMonth)
                                set(java.util.Calendar.DAY_OF_MONTH, 1)
                            }
                            val firstDayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK)
                            val offset = (firstDayOfWeek - java.util.Calendar.MONDAY + 7) % 7
                            val totalDays = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)

                            val totalCells = offset + totalDays
                            val rowsCount = (totalCells + 6) / 7

                            Column(modifier = Modifier.fillMaxWidth()) {
                                for (weekIndex in 0 until rowsCount) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        for (dayIndex in 0..6) {
                                            val cellIndex = weekIndex * 7 + dayIndex
                                            if (cellIndex < offset || cellIndex >= totalCells) {
                                                Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                                            } else {
                                                val dayNum = cellIndex - offset + 1
                                                val dateKey = String.format(java.util.Locale.US, "%04d-%02d-%02d", calendarYear, calendarMonth + 1, dayNum)
                                                val dayEntry = entries.find { it.date == dateKey }

                                                val isToday = dateKey == todayDateStr

                                                val moodColor = when (dayEntry?.mood) {
                                                    "Excelente" -> Color(0xFFB388FF)
                                                    "Bueno" -> Color(0xFF4DB6AC)
                                                    "Neutral" -> Color(0xFF78909C)
                                                    "Triste" -> Color(0xFF9FA8DA)
                                                    "Estresado" -> Color(0xFFBA68C8)
                                                    else -> null
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .aspectRatio(1f)
                                                        .padding(2.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(
                                                            color = if (moodColor != null) moodColor.copy(alpha = 0.15f)
                                                                    else if (isToday) MaterialTheme.colorScheme.primaryContainer
                                                                    else Color.Transparent
                                                        )
                                                        .border(
                                                            width = if (isToday) 1.5.dp else 0.dp,
                                                            color = if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                        .clickable {
                                                            showEmotionCalendar = false
                                                            if (dayEntry != null) {
                                                                onEntryClick(dayEntry)
                                                            } else {
                                                                onAddEntryClick(dateKey, "")
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.Center
                                                    ) {
                                                        Text(
                                                            text = dayNum.toString(),
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = if (isToday || moodColor != null) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (moodColor != null) moodColor
                                                                    else if (isToday) MaterialTheme.colorScheme.primary
                                                                    else MaterialTheme.colorScheme.onSurface
                                                        )
                                                        
                                                        if (moodColor != null) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .padding(top = 2.dp)
                                                                    .size(6.dp)
                                                                    .background(moodColor, CircleShape)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Mood Legend
                            Text(
                                text = "Código de Colores de Ánimo:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val legendItems = listOf(
                                    Triple("Excelente", Color(0xFFB388FF), "🌟"),
                                    Triple("Bueno", Color(0xFF4DB6AC), "😊"),
                                    Triple("Neutral", Color(0xFF78909C), "😐"),
                                    Triple("Triste", Color(0xFF9FA8DA), "😢"),
                                    Triple("Estresado", Color(0xFFBA68C8), "😫")
                                )
                                legendItems.forEach { (moodName, color, emoji) ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(color, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "$emoji $moodName",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = color
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Journal Entries List
            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.getFloat()),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(Color(0xFF14221E), CircleShape)
                                .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No se encontraron entradas" else "Tu espacio de bienestar está en blanco",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.2.sp
                            ),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Intenta con otras palabras clave o etiquetas." else "Toca el botón '+' abajo para iniciar tu primera entrada diaria y recibir análisis de ánimo inteligente.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 16.dp),
                            lineHeight = 22.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1.getFloat()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(entries) { entry ->
                        JournalCard(
                            entry = entry,
                            onClick = { onEntryClick(entry) },
                            onDelete = { viewModel.deleteEntry(entry) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp)) // padding for FAB
                    }
                }
            }
        }
    }
}

private fun Int.getFloat(): Float = this.toFloat()

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JournalCard(
    entry: DiaryEntry,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val moodEmoji = when (entry.mood) {
        "Excelente" -> "🌟"
        "Bueno" -> "😊"
        "Neutral" -> "😐"
        "Triste" -> "😢"
        "Estresado" -> "😫"
        else -> "📝"
    }

    val moodColor = when (entry.mood) {
        "Excelente" -> Color(0xFFB388FF) // Violet orchid
        "Bueno" -> Color(0xFF4DB6AC)     // Soft teal
        "Neutral" -> Color(0xFF78909C)   // Slate grey
        "Triste" -> Color(0xFF9FA8DA)    // Dusty indigo
        "Estresado" -> Color(0xFFBA68C8) // Muted magenta/grape
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, Color(0xFF223530).copy(alpha = 0.6f), RoundedCornerShape(18.dp))
            .testTag("journal_card_${entry.date}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF131D1B).copy(alpha = 0.75f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Elegant left mood vertical accent strip
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(90.dp)
                    .clip(CircleShape)
                    .background(moodColor)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Header: Date & Mood Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDisplayDate(entry.date),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(moodColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .border(1.dp, moodColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(text = "$moodEmoji ", fontSize = 12.sp)
                        Text(
                            text = entry.mood,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = moodColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Title
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Snippet
                Text(
                    text = entry.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                if (entry.aiSentiment.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // High-end blockquote insight style
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F1715), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF1D2F2A).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(38.dp)
                                .clip(CircleShape)
                                .background(moodColor)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Insight de Ánimo IA",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = moodColor
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = entry.aiSentiment,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Serif,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tags flow list & Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tags
                    if (entry.tags.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.weight(1.toFloat()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            entry.tags.split(",").forEach { tag ->
                                val trimmed = tag.trim()
                                if (trimmed.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "#$trimmed",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1.toFloat()))
                    }

                    // Delete Button
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .testTag("delete_button_${entry.date}")
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Borrar entrada",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

fun formatDisplayDate(dateStr: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = parser.parse(dateStr)
        val formatter = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "ES"))
        formatter.format(date ?: Date()).replaceFirstChar { it.uppercase() }
    } catch (e: Exception) {
        dateStr
    }
}
