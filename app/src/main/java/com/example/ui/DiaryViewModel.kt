package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DiaryDatabase
import com.example.data.DiaryEntry
import com.example.data.DiaryRepository
import com.example.service.CalendarEvent
import com.example.service.CalendarService
import com.example.service.GeminiService
import com.example.service.ReminderScheduler
import com.example.service.SupabaseService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DiaryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DiaryRepository
    
    // Search query StateFlow
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Reactive list of diary entries (filtered by search query if present)
    val entries: StateFlow<List<DiaryEntry>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.allEntries
            } else {
                repository.searchEntries(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Biometric Authentication State
    private val _isBiometricAuthenticated = MutableStateFlow(false)
    val isBiometricAuthenticated = _isBiometricAuthenticated.asStateFlow()

    // Today's Calendar events (Pending tasks centralized view)
    private val _todayEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val todayEvents = _todayEvents.asStateFlow()

    // Sync State
    private val _syncState = MutableStateFlow<String>("idle") // "idle", "syncing", "success", "error"
    val syncState = _syncState.asStateFlow()

    // AI Analysis State
    private val _aiAnalysisState = MutableStateFlow<String>("idle") // "idle", "loading", "success", "error"
    val aiAnalysisState = _aiAnalysisState.asStateFlow()

    // --- Settings State (SharedPreferences) ---
    val isRegistered = MutableStateFlow(false)
    val registeredUsername = MutableStateFlow("")
    val registeredPassword = MutableStateFlow("")
    val registeredUserId = MutableStateFlow("")
    val isLoggedIn = MutableStateFlow(false)

    val isBiometricEnabled = MutableStateFlow(false)
    val isReminderEnabled = MutableStateFlow(false)
    val reminderHour = MutableStateFlow(21) // default 9 PM
    val reminderMinute = MutableStateFlow(0)
    val supabaseUrl = MutableStateFlow("")
    val supabaseAnonKey = MutableStateFlow("")
    val isSupabaseSyncEnabled = MutableStateFlow(false)

    init {
        val database = DiaryDatabase.getDatabase(application)
        repository = DiaryRepository(database.diaryDao())
        
        loadSettings()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setBiometricAuthenticated(auth: Boolean) {
        _isBiometricAuthenticated.value = auth
    }

    /**
     * Load today's calendar events (Centralized view of tasks/meetings).
     */
    fun loadTodayEvents(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val events = CalendarService.getEventsForToday(context)
            _todayEvents.value = events
        }
    }

    // --- SharedPreferences Settings Management ---

    fun loadSettings() {
        val prefs = getApplication<Application>().getSharedPreferences("diary_settings", Context.MODE_PRIVATE)
        isRegistered.value = prefs.getBoolean("is_registered", false)
        registeredUsername.value = prefs.getString("registered_username", "") ?: ""
        registeredPassword.value = prefs.getString("registered_password", "") ?: ""
        registeredUserId.value = prefs.getString("registered_user_id", "") ?: ""
        isLoggedIn.value = prefs.getBoolean("is_logged_in", false)

        isBiometricEnabled.value = prefs.getBoolean("biometric_enabled", false)
        isReminderEnabled.value = prefs.getBoolean("reminder_enabled", false)
        reminderHour.value = prefs.getInt("reminder_hour", 21)
        reminderMinute.value = prefs.getInt("reminder_minute", 0)
        val defaultUrl = com.example.BuildConfig.SUPABASE_URL
        val defaultKey = com.example.BuildConfig.SUPABASE_ANON_KEY
        
        var loadedUrl = prefs.getString("supabase_url", "") ?: ""
        var loadedKey = prefs.getString("supabase_anon_key", "") ?: ""
        
        if (loadedUrl.isBlank()) {
            loadedUrl = defaultUrl
            prefs.edit().putString("supabase_url", defaultUrl).apply()
        }
        if (loadedKey.isBlank()) {
            loadedKey = defaultKey
            prefs.edit().putString("supabase_anon_key", defaultKey).apply()
        }
        
        supabaseUrl.value = loadedUrl
        supabaseAnonKey.value = loadedKey
        isSupabaseSyncEnabled.value = prefs.getBoolean("supabase_sync_enabled", false)

        // If biometric is disabled, default authenticated state is true
        if (!isBiometricEnabled.value) {
            _isBiometricAuthenticated.value = true
        }
    }

    fun registerUser(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val url = supabaseUrl.value
            val anonKey = supabaseAnonKey.value
            
            // If the user has not configured Supabase, we can't check duplicates on Supabase, so let them know or fallback.
            if (url.isBlank() || anonKey.isBlank()) {
                onResult(false, "El administrador aún no ha configurado las credenciales de Supabase del proyecto.")
                return@launch
            }

            val result = withContext(Dispatchers.IO) {
                SupabaseService.registerUser(url, anonKey, email, password)
            }

            if (result.success) {
                val returnedUserId = result.userId ?: ""
                val prefs = getApplication<Application>().getSharedPreferences("diary_settings", Context.MODE_PRIVATE)
                prefs.edit()
                    .putBoolean("is_registered", true)
                    .putString("registered_username", email)
                    .putString("registered_password", password)
                    .putString("registered_user_id", returnedUserId)
                    .putBoolean("is_logged_in", true)
                    .apply()
                
                isRegistered.value = true
                registeredUsername.value = email
                registeredPassword.value = password
                registeredUserId.value = returnedUserId
                isLoggedIn.value = true
                onResult(true, null)
            } else {
                onResult(false, result.errorMsg)
            }
        }
    }

    fun loginUser(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            // First check if credentials match locally (for offline support)
            if (isRegistered.value && registeredUsername.value == email && registeredPassword.value == password) {
                val prefs = getApplication<Application>().getSharedPreferences("diary_settings", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("is_logged_in", true).apply()
                isLoggedIn.value = true
                onResult(true, null)
                return@launch
            }

            val url = supabaseUrl.value
            val anonKey = supabaseAnonKey.value
            
            if (url.isBlank() || anonKey.isBlank()) {
                onResult(false, "El administrador aún no ha configurado las credenciales de Supabase del proyecto.")
                return@launch
            }

            val result = withContext(Dispatchers.IO) {
                SupabaseService.loginUser(url, anonKey, email, password)
            }

            if (result.success) {
                val returnedUserId = result.userId ?: ""
                val prefs = getApplication<Application>().getSharedPreferences("diary_settings", Context.MODE_PRIVATE)
                prefs.edit()
                    .putBoolean("is_registered", true)
                    .putString("registered_username", email)
                    .putString("registered_password", password)
                    .putString("registered_user_id", returnedUserId)
                    .putBoolean("is_logged_in", true)
                    .apply()
                
                isRegistered.value = true
                registeredUsername.value = email
                registeredPassword.value = password
                registeredUserId.value = returnedUserId
                isLoggedIn.value = true
                onResult(true, null)
            } else {
                onResult(false, result.errorMsg ?: "Credenciales inválidas o sin conexión.")
            }
        }
    }

    fun logoutUser() {
        val prefs = getApplication<Application>().getSharedPreferences("diary_settings", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_logged_in", false).apply()
        isLoggedIn.value = false
        _isBiometricAuthenticated.value = false
    }

    fun saveBiometricEnabled(enabled: Boolean) {
        isBiometricEnabled.value = enabled
        val prefs = getApplication<Application>().getSharedPreferences("diary_settings", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("biometric_enabled", enabled).apply()
        
        if (!enabled) {
            _isBiometricAuthenticated.value = true
        }
    }

    fun saveReminderSettings(enabled: Boolean, hour: Int, minute: Int) {
        isReminderEnabled.value = enabled
        reminderHour.value = hour
        reminderMinute.value = minute

        val context = getApplication<Application>()
        val prefs = context.getSharedPreferences("diary_settings", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("reminder_enabled", enabled)
            .putInt("reminder_hour", hour)
            .putInt("reminder_minute", minute)
            .apply()

        if (enabled) {
            ReminderScheduler.scheduleDailyReminder(context, hour, minute)
        } else {
            ReminderScheduler.cancelReminder(context)
        }
    }

    fun saveSupabaseSettings(url: String, anonKey: String, enabled: Boolean) {
        supabaseUrl.value = url
        supabaseAnonKey.value = anonKey
        isSupabaseSyncEnabled.value = enabled

        val prefs = getApplication<Application>().getSharedPreferences("diary_settings", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("supabase_url", url)
            .putString("supabase_anon_key", anonKey)
            .putBoolean("supabase_sync_enabled", enabled)
            .apply()
    }

    // --- Database Operations ---

    fun insertEntry(entry: DiaryEntry, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val updatedEntry = entry.copy(updatedAt = System.currentTimeMillis())
            repository.insertEntry(updatedEntry)
            
            // Auto sync if Supabase sync is enabled
            if (isSupabaseSyncEnabled.value) {
                syncWithSupabase()
            }
            onComplete()
        }
    }

    suspend fun getEntryForDate(date: String): DiaryEntry? {
        return repository.getEntryByDate(date)
    }

    fun deleteEntry(entry: DiaryEntry) {
        viewModelScope.launch {
            repository.deleteEntry(entry)
        }
    }

    // --- AI Sentiment & Advice Generation ---

    fun analyzeAndSaveEntry(
        date: String,
        title: String,
        content: String,
        manualMood: String,
        tags: String,
        onResult: (DiaryEntry) -> Unit
    ) {
        _aiAnalysisState.value = "loading"
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                GeminiService.analyzeEntry(content)
            }

            // If the user specified a custom mood, keep it or merge with AI sentiment.
            // We'll prioritize the AI analysis sentiment, but let the user adjust it.
            val finalMood = result.sentiment.ifEmpty { manualMood }

            // Fetch existing entry to preserve ID if we are updating
            val existing = repository.getEntryByDate(date)
            val newEntry = DiaryEntry(
                id = existing?.id ?: 0,
                date = date,
                title = title.ifEmpty { "Entrada del $date" },
                content = content,
                mood = finalMood,
                tags = tags,
                aiSentiment = result.summary,
                aiAdvice = result.advice,
                isSynced = false,
                updatedAt = System.currentTimeMillis()
            )

            repository.insertEntry(newEntry)
            _aiAnalysisState.value = "success"

            // Auto sync if Supabase is enabled
            if (isSupabaseSyncEnabled.value) {
                syncWithSupabase()
            }

            onResult(newEntry)
        }
    }

    // --- Cloud Sync ---

    fun syncWithSupabase() {
        if (!isSupabaseSyncEnabled.value) return
        
        _syncState.value = "syncing"
        viewModelScope.launch(Dispatchers.IO) {
            val url = supabaseUrl.value
            val anonKey = supabaseAnonKey.value
            val userId = registeredUserId.value.ifBlank { "00000000-0000-0000-0000-000000000000" }
            
            val (successes, failures) = repository.syncUnsyncedEntries(url, anonKey, userId)
            
            _syncState.value = if (failures == 0 && successes >= 0) {
                "success"
            } else {
                "error"
            }
        }
    }
}
