package com.fyp.healthcare

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fyp.healthcare.ui.theme.HealthCareTheme

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* if denied, reminders are scheduled but stay silent */ }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Edge-to-edge with light (white) status-bar icons on every API level, since the top
        // of every screen sits on the brand-blue header / backdrop.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (savedInstanceState == null) {
            // re-arm every medication reminder in case the app was force-stopped
            // or a dose time slipped past while it was closed
            Thread { ReminderScheduler.syncAll(applicationContext) }.start()
        }
        setContent {
            HealthCareTheme {
                val medManager = remember { MedicationManager(applicationContext) }
                val userManager = remember { UserManager(applicationContext) }
                val healthData = remember { HealthDataManager(applicationContext) }
                val activityData = remember { ActivityDataManager(applicationContext) }
                val navController = rememberNavController()

                val startDestination =
                    if (userManager.getLoggedInUser() != null) "home" else "login"

                val switchTab: (String) -> Unit = { route ->
                    navController.navigate(route) {
                        popUpTo("home") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }

                // Brand blue — every screen header hardcodes this same value.
                val brandBlue = Color(0xFF2A6DE1)

                Box(modifier = Modifier.fillMaxSize()) {

                NavHost(navController = navController, startDestination = startDestination) {

                    // ===== Auth =====
                    composable("login") {
                        LoginScreen(
                            userManager = userManager,
                            onLoginSuccess = {
                                navController.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                }
                            },
                            onRegisterClick = { navController.navigate("register") }
                        )
                    }
                    composable("register") {
                        RegisterScreen(
                            userManager = userManager,
                            onRegisterSuccess = {
                                navController.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                }
                            },
                            onSignInClick = { navController.popBackStack() }
                        )
                    }

                    // ===== Main tabs =====
                    composable("home") {
                        TabScaffold("home", switchTab) {
                            HomeScreen(
                                userManager = userManager,
                                healthData = healthData,
                                onLogoutClick = {
                                    userManager.clearSession()
                                    navController.navigate("login") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                },
                                onNavigate = { navController.navigate(it) }
                            )
                        }
                    }
                    composable("health") {
                        TabScaffold("health", switchTab) {
                            HealthTrendsScreen(onBackClick = { navController.popBackStack() })
                        }
                    }

                    // "reminders" (bottom nav) and "medication" (Home quick action) both show the list
                    val medicationList: @Composable () -> Unit = {
                        TabScaffold("reminders", switchTab) {
                            MedicationScreen(
                                medManager = medManager,
                                onBackClick = { navController.popBackStack() },
                                onAddClick = { navController.navigate("add_medication") },
                                onEditClick = { id -> navController.navigate("edit_medication/$id") }
                            )
                        }
                    }
                    composable("reminders") { medicationList() }
                    composable("medication") { medicationList() }

                    composable("add_medication") {
                        AddMedicationScreen(
                            medManager = medManager,
                            editId = null,
                            onBackClick = { navController.popBackStack() },
                            onSaved = { id ->
                                navController.navigate("medication_added/$id") {
                                    popUpTo("add_medication") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(
                        "edit_medication/{medId}",
                        arguments = listOf(navArgument("medId") { type = NavType.LongType })
                    ) { entry ->
                        AddMedicationScreen(
                            medManager = medManager,
                            editId = entry.arguments?.getLong("medId"),
                            onBackClick = { navController.popBackStack() },
                            onSaved = { navController.popBackStack() }
                        )
                    }

                    composable(
                        "medication_added/{medId}",
                        arguments = listOf(navArgument("medId") { type = NavType.LongType })
                    ) { entry ->
                        MedicationAddedScreen(
                            medManager = medManager,
                            medId = entry.arguments?.getLong("medId") ?: -1L,
                            onAddAnother = {
                                navController.navigate("add_medication") {
                                    popUpTo("medication_added/{medId}") { inclusive = true }
                                }
                            },
                            onBackToReminders = { navController.popBackStack() }
                        )
                    }
                    composable("activity") {
                        TabScaffold("activity", switchTab) {
                            ActivityScreen(
                                activity = activityData,
                                onBackClick = { navController.popBackStack() },
                            )
                        }
                    }
                    composable("profile") { TabScaffold("profile", switchTab) { BlankScreen("Profile") } }

                    // ===== Record Data flow =====
                    composable("record_data") {
                        RecordDataScreen(
                            healthData = healthData,
                            onBackClick = { navController.popBackStack() },
                            onSaved = {
                                navController.navigate("data_recorded") {
                                    popUpTo("record_data") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("data_recorded") {
                        DataRecordedScreen(
                            healthData = healthData,
                            onBackToHome = { navController.popBackStack() }
                        )
                    }

                    // ===== Other quick actions (blank for now) =====
                    composable("view_trends") {
                        TabScaffold("health", switchTab) {
                            HealthTrendsScreen(onBackClick = { navController.popBackStack() })
                        }
                    }
                    composable("emergency") { BlankScreen("Emergency") }
                }

                    // Opaque backdrop behind the transparent, edge-to-edge status bar so the top
                    // of every screen is the brand colour instead of a blank/undrawn strip.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsTopHeight(WindowInsets.statusBars)
                            .align(Alignment.TopCenter)
                            .background(brandBlue)
                    )
                }
            }
        }
    }
}