package com.fyp.healthcare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fyp.healthcare.ui.theme.HealthCareTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HealthCareTheme {
                val userManager = remember { UserManager(applicationContext) }
                val healthData = remember { HealthDataManager(applicationContext) }
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
                    composable("reminders") { TabScaffold("reminders", switchTab) { BlankScreen("Reminders") } }
                    composable("activity") { TabScaffold("activity", switchTab) { BlankScreen("Activity") } }
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
                    composable("medication") { BlankScreen("Medication") }
                    composable("emergency") { BlankScreen("Emergency") }
                }
            }
        }
    }
}