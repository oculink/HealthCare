package com.fyp.healthcare

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Build
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fyp.healthcare.ui.theme.AppTheme
import com.fyp.healthcare.ui.theme.HealthCareTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// main

/** Pre-Home routes that a late cloud-sync is allowed to bounce the user off of. */
private val ENTRY_ROUTES = setOf("role_select", "onboarding", "caretaker_link")

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* if denied, reminders are scheduled but stay silent */ }
    private val activityRecognitionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            StepTracker.start(applicationContext)
            SleepTracker.register(applicationContext)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppTheme.init(applicationContext)
        Session.init(applicationContext)
        // existing users (installed before the role screen) who already finished onboarding
        // are patients — don't send them back through role selection
        if (Session.role.isBlank() && ProfileManager(applicationContext).isOnboarded()) {
            Session.setRole(applicationContext, "patient")
        }
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
        // Step counter for Home's "Today's Summary" — patient's own phone only (StepTracker
        // no-ops in caretaker mode). ACTIVITY_RECOGNITION is a runtime permission from API 29.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            activityRecognitionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        if (savedInstanceState == null) {
            lifecycleScope.launch(Dispatchers.IO) {
                // make sure the local session (role + caretaker link) matches the cloud
                runCatching { FamilyLink.restoreSession(applicationContext) }
                // pull the current target account's data down from Firestore into the local
                // cache, so a linked caretaker's edits (or the patient's own, from another
                // device) show up on open
                runCatching { CloudHydrator.hydrate(applicationContext) }
                // re-arm every medication + appointment reminder in case the app was force-stopped
                ReminderScheduler.syncAll(applicationContext)
                AppointmentReminderScheduler.syncAll(applicationContext)
                // push any data saved before cloud sync / while offline (runs once per account)
                Cloud.pushBacklog(applicationContext)
                // fold this account's existing readings into the community aggregate (once)
                Cloud.seedCommunityBacklog(applicationContext)
            }
        }
        setContent {
            HealthCareTheme {
                // Rebuilt when caretaker mode switches, so they read/write the right account's
                // store (own prefs in self mode, `*__<patientUid>` prefs in caretaker mode).
                val dataScope = Session.controlledPatientUid
                val medManager = remember(dataScope) { MedicationManager(applicationContext) }
                val apptManager = remember(dataScope) { AppointmentManager(applicationContext) }
                val userManager = remember { UserManager(applicationContext) }
                val healthData = remember(dataScope) { HealthDataManager(applicationContext) }
                val activityData = remember(dataScope) { ActivityDataManager(applicationContext) }
                val profileManager = remember(dataScope) { ProfileManager(applicationContext) }
                val navController = rememberNavController()

                // draft carried between the two Add-Medication steps (details -> schedule)
                var medDraft by remember { mutableStateOf<Medication?>(null) }
                var medDraftIsEdit by remember { mutableStateOf(false) }

                // where to land once authenticated:
                //  - no role chosen yet        -> role select
                //  - caretaker, not yet linked -> enter a family code
                //  - caretaker, linked         -> patient's Home
                //  - patient, not onboarded    -> onboarding
                //  - otherwise                 -> Home
                val afterAuth = {
                    when {
                        Session.role == "caretaker" && !Session.isCaretakerMode -> "caretaker_link"
                        Session.role == "caretaker" -> "home"
                        Session.role == "patient" ->
                            if (profileManager.isOnboarded()) "home" else "onboarding"
                        // role not known on this device yet: an already-onboarded account is a
                        // returning patient — don't send them back through role selection
                        profileManager.isOnboarded() -> "home"
                        else -> "role_select"
                    }
                }
                val startDestination = if (userManager.isSignedIn()) afterAuth() else "signin"

                // If the post-sign-in cloud sync lands AFTER we first routed — e.g. a fresh
                // device, where the profile wasn't in local storage yet — re-route now that it
                // is, so an already-set-up account isn't stuck on role select / onboarding.
                LaunchedEffect(Session.dataVersion, Session.role, Session.controlledPatientUid) {
                    if (!userManager.isSignedIn()) return@LaunchedEffect
                    val current = navController.currentBackStackEntry?.destination?.route
                    if (current == null || current !in ENTRY_ROUTES) return@LaunchedEffect
                    val dest = afterAuth()
                    if (dest != current) {
                        navController.navigate(dest) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                }

                val clearBackStackTo = { route: String ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
                val refreshCache = {
                    lifecycleScope.launch(Dispatchers.IO) {
                        runCatching { CloudHydrator.hydrate(applicationContext) }
                    }
                }

                // Bottom-nav tab switch. Pop back to Home and remember each tab's
                // state, but never *restore* Home itself. Quick actions (Record Data,
                // Health Report, Profile) are plain-navigated on top of Home, so their
                // routes get saved under Home's key when popped — restoring that
                // sub-stack would immediately re-push the screen the user just left,
                // making the Home tab button look like it does nothing.
                val switchTab: (String) -> Unit = { route ->
                    navController.navigate(route) {
                        popUpTo("home") { saveState = true }
                        launchSingleTop = true
                        restoreState = route != "home"
                    }
                }

                // Brand blue — every screen header hardcodes this same value.
                val brandBlue = Color(0xFF2A6DE1)

                Box(modifier = Modifier.fillMaxSize()) {

                NavHost(navController = navController, startDestination = startDestination) {

                    // ===== Auth =====
                    composable("signin") {
                        SignInScreen(
                            userManager = userManager,
                            onSignedIn = {
                                // SignInScreen already restored the session + hydrated this
                                // account's caches (and bumped dataVersion) before calling us.
                                Thread {
                                    Cloud.pushBacklog(applicationContext)
                                    Cloud.seedCommunityBacklog(applicationContext)
                                }.start()
                                navController.navigate(afterAuth()) {
                                    popUpTo("signin") { inclusive = true }
                                }
                            },
                        )
                    }
                    composable("onboarding") {
                        EditProfileScreen(
                            userManager = userManager,
                            profileManager = profileManager,
                            firstRun = true,
                            onBackClick = {
                                // "Go back" from onboarding = return to role selection so
                                // the user can switch between Patient and Caregiver. The role
                                // isn't committed yet, so drop it (locally + cloud) — otherwise
                                // a later sign-in would restore it and skip this screen.
                                Session.setRole(applicationContext, "")
                                Cloud.selfDoc?.set(mapOf("role" to ""), SetOptions.merge())
                                navController.navigate("role_select") {
                                    popUpTo("onboarding") { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onSaved = {
                                // profile is done -> now the "patient" role is real: persist it
                                Cloud.selfDoc?.set(mapOf("role" to "patient"), SetOptions.merge())
                                navController.navigate("home") {
                                    popUpTo("onboarding") { inclusive = true }
                                }
                            },
                        )
                    }

                    // ===== Role + Family Caregiver linking =====
                    composable("role_select") {
                        RoleSelectScreen(
                            onPatient = {
                                Session.setRole(applicationContext, "patient")
                                val onboarded = profileManager.isOnboarded()
                                // only commit the role to the cloud once it's real — here that
                                // means the profile is already done; otherwise onboarding's
                                // onSaved writes it. Backing out before then leaves no trace.
                                if (onboarded) {
                                    Cloud.selfDoc?.set(mapOf("role" to "patient"), SetOptions.merge())
                                }
                                navController.navigate(if (onboarded) "home" else "onboarding") {
                                    popUpTo("role_select") { inclusive = true }
                                }
                            },
                            onCaretaker = {
                                // role committed to the cloud only on a successful link
                                // (FamilyLink.link writes it); backing out leaves no trace.
                                Session.setRole(applicationContext, "caretaker")
                                navController.navigate("caretaker_link") {
                                    popUpTo("role_select") { inclusive = true }
                                }
                            },
                            onSignOut = {
                                PatientMonitor.stop()
                                StepTracker.stop()
                                SleepTracker.unregister(applicationContext)
                                userManager.signOut(applicationContext)
                                Session.clear(applicationContext)
                                clearBackStackTo("signin")
                            },
                        )
                    }
                    composable("caretaker_link") {
                        CaretakerLinkScreen(
                            onLinked = { info ->
                                Session.enterCaretakerMode(
                                    applicationContext, info.uid, info.name, info.photoUrl
                                )
                                refreshCache()
                                // this account is now a caregiver — stop tracking THIS phone's
                                // own steps / sleep; show the patient's synced figures instead
                                StepTracker.stop()
                                SleepTracker.unregister(applicationContext)
                                PatientMonitor.start(applicationContext)
                                clearBackStackTo("home")
                            },
                            onBack = {
                                Session.setRole(applicationContext, "")
                                Cloud.selfDoc?.set(mapOf("role" to ""), SetOptions.merge())
                                navController.navigate("role_select") {
                                    popUpTo("caretaker_link") { inclusive = true }
                                }
                            },
                        )
                    }
                    composable("family_caregiver") {
                        FamilyCaregiverScreen(
                            onBack = { navController.popBackStack() },
                            onUnlinked = {
                                PatientMonitor.stop()
                                Session.exitCaretakerMode(applicationContext)
                                clearBackStackTo("caretaker_link")
                            },
                        )
                    }

                    // ===== Main tabs =====
                    composable("home") {
                        TabScaffold("home", switchTab) {
                            HomeScreen(
                                userManager = userManager,
                                profileManager = profileManager,
                                healthData = healthData,
                                activityData = activityData,
                                onNavigate = { navController.navigate(it) },
                            )
                        }
                    }
                    composable("health") {
                        TabScaffold("health", switchTab) {
                            HealthTrendsScreen(
                                healthData = healthData,
                                onBackClick = { navController.popBackStack() },
                            )
                        }
                    }

                    // Medication reminders live on the "reminders" bottom-nav tab
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

                    composable("add_medication") { entry ->
                        val pName by entry.savedStateHandle
                            .getStateFlow<String?>("med_pick_name", null).collectAsState()
                        val pDesc by entry.savedStateHandle
                            .getStateFlow<String?>("med_pick_desc", null).collectAsState()
                        val pRoute by entry.savedStateHandle
                            .getStateFlow<String?>("med_pick_route", null).collectAsState()
                        val pCustom by entry.savedStateHandle
                            .getStateFlow("med_pick_custom", false).collectAsState()
                        AddMedicationScreen(
                            medManager = medManager,
                            editId = null,
                            onChooseMedication = { navController.navigate("medication_catalog") },
                            pickedName = pName,
                            pickedDescription = pDesc,
                            pickedRoute = pRoute,
                            pickedCustom = pCustom,
                            onPickConsumed = {
                                entry.savedStateHandle["med_pick_name"] = null
                                entry.savedStateHandle["med_pick_desc"] = null
                                entry.savedStateHandle["med_pick_route"] = null
                                entry.savedStateHandle["med_pick_custom"] = false
                            },
                            onBackClick = { navController.popBackStack() },
                            onNext = { draft ->
                                medDraft = draft
                                medDraftIsEdit = false
                                navController.navigate("medication_schedule")
                            },
                        )
                    }

                    composable(
                        "edit_medication/{medId}",
                        arguments = listOf(navArgument("medId") { type = NavType.LongType })
                    ) { entry ->
                        val pName by entry.savedStateHandle
                            .getStateFlow<String?>("med_pick_name", null).collectAsState()
                        val pDesc by entry.savedStateHandle
                            .getStateFlow<String?>("med_pick_desc", null).collectAsState()
                        val pRoute by entry.savedStateHandle
                            .getStateFlow<String?>("med_pick_route", null).collectAsState()
                        val pCustom by entry.savedStateHandle
                            .getStateFlow("med_pick_custom", false).collectAsState()
                        AddMedicationScreen(
                            medManager = medManager,
                            editId = entry.arguments?.getLong("medId"),
                            onChooseMedication = { navController.navigate("medication_catalog") },
                            pickedName = pName,
                            pickedDescription = pDesc,
                            pickedRoute = pRoute,
                            pickedCustom = pCustom,
                            onPickConsumed = {
                                entry.savedStateHandle["med_pick_name"] = null
                                entry.savedStateHandle["med_pick_desc"] = null
                                entry.savedStateHandle["med_pick_route"] = null
                                entry.savedStateHandle["med_pick_custom"] = false
                            },
                            onBackClick = { navController.popBackStack() },
                            onNext = { draft ->
                                medDraft = draft
                                medDraftIsEdit = true
                                navController.navigate("medication_schedule")
                            },
                        )
                    }

                    composable("medication_schedule") {
                        val draft = medDraft
                        if (draft == null) {
                            LaunchedEffect(Unit) { navController.popBackStack() }
                        } else {
                            MedicationScheduleScreen(
                                draft = draft,
                                isEdit = medDraftIsEdit,
                                onBackClick = { navController.popBackStack() },
                                onSave = { finalMed ->
                                    val saved = medManager.upsert(finalMed)
                                    ReminderScheduler.scheduleNext(applicationContext, saved)
                                    // NOTE: don't clear medDraft here — nulling it recomposes this
                                    // destination (still briefly on the stack) into its null-guard,
                                    // which would pop the confirmation screen we're navigating to.
                                    navController.navigate("medication_added/${saved.id}") {
                                        popUpTo("reminders") { inclusive = false }
                                    }
                                },
                            )
                        }
                    }

                    composable("medication_catalog") {
                        MedicationPickerScreen(
                            onPickEntry = { medName, medDesc, medRoute ->
                                navController.previousBackStackEntry?.savedStateHandle?.let {
                                    it["med_pick_name"] = medName
                                    it["med_pick_desc"] = medDesc
                                    it["med_pick_route"] = medRoute
                                    it["med_pick_custom"] = false
                                }
                                navController.popBackStack()
                            },
                            onEnterManually = {
                                navController.previousBackStackEntry?.savedStateHandle
                                    ?.set("med_pick_custom", true)
                                navController.popBackStack()
                            },
                            onBackClick = { navController.popBackStack() },
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
                    composable("profile") {
                        TabScaffold("profile", switchTab) {
                            ProfileScreen(
                                userManager = userManager,
                                profileManager = profileManager,
                                activity = activityData,
                                onEditProfile = { navController.navigate("edit_profile") },
                                onOpenFamilyCaregiver = { navController.navigate("family_caregiver") },
                                onSignOut = {
                                    userManager.signOut(applicationContext)
                                    Session.clear(applicationContext)
                                    clearBackStackTo("signin")
                                },
                                onBackClick = { navController.popBackStack() },
                            )
                        }
                    }
                    composable("edit_profile") {
                        EditProfileScreen(
                            userManager = userManager,
                            profileManager = profileManager,
                            firstRun = false,
                            onBackClick = { navController.popBackStack() },
                            onSaved = { navController.popBackStack() },
                        )
                    }

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

                    // ===== Other quick actions =====
                    composable("clinics") {
                        NearbyClinicsScreen(onBackClick = { navController.popBackStack() })
                    }
                    composable("health_report") {
                        HealthReportScreen(
                            userManager = userManager,
                            profileManager = profileManager,
                            healthData = healthData,
                            medManager = medManager,
                            onBackClick = { navController.popBackStack() },
                        )
                    }
                    composable("emergency") {
                        EmergencyProfileScreen(
                            userManager = userManager,
                            profileManager = profileManager,
                            medManager = medManager,
                            onBackClick = { navController.popBackStack() },
                            onEditProfile = { navController.navigate("edit_profile") },
                        )
                    }

                    // ===== Appointments (manual log + local reminders + caregiver sync) =====
                    composable("appointments") {
                        AppointmentsScreen(
                            apptManager = apptManager,
                            onBackClick = { navController.popBackStack() },
                            onAddClick = { navController.navigate("add_appointment") },
                            onEditClick = { id -> navController.navigate("edit_appointment/$id") },
                        )
                    }
                    composable("add_appointment") {
                        AddAppointmentScreen(
                            apptManager = apptManager,
                            editId = null,
                            onBackClick = { navController.popBackStack() },
                            onSaved = { navController.popBackStack() },
                        )
                    }
                    composable(
                        "edit_appointment/{apptId}",
                        arguments = listOf(navArgument("apptId") { type = NavType.LongType })
                    ) { entry ->
                        AddAppointmentScreen(
                            apptManager = apptManager,
                            editId = entry.arguments?.getLong("apptId"),
                            onBackClick = { navController.popBackStack() },
                            onSaved = { navController.popBackStack() },
                        )
                    }
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

    override fun onResume() {
        super.onResume()
        // refresh the local cache from Firestore whenever we come back to the foreground
        if (Firebase.auth.currentUser != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                runCatching { CloudHydrator.hydrate(applicationContext) }
            }
            // in caretaker mode, keep the patient's data streaming in live while foregrounded
            PatientMonitor.start(applicationContext)
            // sample the phone step counter while foregrounded (self mode only)
            StepTracker.start(applicationContext)
            // (re)subscribe to the phone Sleep API — self mode only; the subscription itself
            // outlives the app being closed so the morning sleep segment still lands
            SleepTracker.register(applicationContext)
        }
    }

    override fun onStop() {
        super.onStop()
        // drop the live listeners while backgrounded — CloudHydrator + PatientMonitor
        // both refill / re-attach on the next resume; the step counter keeps counting in
        // hardware and we resample on the next open
        PatientMonitor.stop()
        StepTracker.stop()
    }
}