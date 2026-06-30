package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.AuthScreen
import com.example.ui.DiaryViewModel
import com.example.ui.JournalEntryScreen
import com.example.ui.JournalListScreen
import com.example.ui.ReportsScreen
import com.example.ui.SettingsScreen
import com.example.ui.RegistrationScreen
import com.example.ui.LoginScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : FragmentActivity() {
    
    private lateinit var diaryViewModel: DiaryViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            diaryViewModel = viewModel()
            
            MyApplicationTheme {
                val isRegistered by diaryViewModel.isRegistered.collectAsState()
                val isLoggedIn by diaryViewModel.isLoggedIn.collectAsState()
                val isBiometricEnabled by diaryViewModel.isBiometricEnabled.collectAsState()
                val isBiometricAuthenticated by diaryViewModel.isBiometricAuthenticated.collectAsState()
                
                var authModeLogin by remember(isRegistered) { mutableStateOf(isRegistered) }

                if (!isLoggedIn) {
                    if (authModeLogin) {
                        LoginScreen(
                            viewModel = diaryViewModel,
                            onLoginSuccess = {
                                // On login success, goes inside
                            },
                            onNavigateToRegister = {
                                authModeLogin = false
                            }
                        )
                    } else {
                        RegistrationScreen(
                            viewModel = diaryViewModel,
                            onRegistrationSuccess = {
                                // On registration success, user is automatically logged in and enters
                            },
                            onNavigateToLogin = {
                                authModeLogin = true
                            }
                        )
                    }
                } else if (isBiometricEnabled && !isBiometricAuthenticated) {
                    AuthScreen(
                        viewModel = diaryViewModel,
                        onAuthSuccess = {
                            // On success, state changes and recomposes to standard navigation below
                        }
                    )
                } else {
                    val navController = rememberNavController()
                    
                    NavHost(
                        navController = navController,
                        startDestination = "list",
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable("list") {
                            JournalListScreen(
                                viewModel = diaryViewModel,
                                onAddEntryClick = { date, prefillTitle ->
                                    val encodedTitle = Uri.encode(prefillTitle)
                                    navController.navigate("entry/$date?id=-1&title=$encodedTitle")
                                },
                                onEntryClick = { entry ->
                                    navController.navigate("entry/${entry.date}?id=${entry.id}")
                                },
                                onReportsClick = {
                                    navController.navigate("reports")
                                },
                                onSettingsClick = {
                                    navController.navigate("settings")
                                }
                            )
                        }
                        
                        composable(
                            route = "entry/{date}?id={id}&title={title}",
                            arguments = listOf(
                                navArgument("date") { type = NavType.StringType },
                                navArgument("id") {
                                    type = NavType.IntType
                                    defaultValue = -1
                                },
                                navArgument("title") {
                                    type = NavType.StringType
                                    defaultValue = ""
                                }
                            )
                        ) { backStackEntry ->
                            val date = backStackEntry.arguments?.getString("date") ?: ""
                            val id = backStackEntry.arguments?.getInt("id") ?: -1
                            val prefilledTitle = backStackEntry.arguments?.getString("title") ?: ""
                            val entries by diaryViewModel.entries.collectAsState()
                            val existing = if (id != -1) entries.find { it.id == id } else null
                            
                            JournalEntryScreen(
                                viewModel = diaryViewModel,
                                dateStr = date,
                                existingEntry = existing,
                                prefilledTitle = prefilledTitle,
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        
                        composable("reports") {
                            ReportsScreen(
                                viewModel = diaryViewModel,
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        
                        composable("settings") {
                            SettingsScreen(
                                viewModel = diaryViewModel,
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload settings (e.g. if modified on other screens)
        if (::diaryViewModel.isInitialized) {
            diaryViewModel.loadSettings()
        }
    }

    override fun onStop() {
        super.onStop()
        // Automatically lock the app when backgrounded or stopped
        if (::diaryViewModel.isInitialized) {
            diaryViewModel.setBiometricAuthenticated(false)
        }
    }
}
