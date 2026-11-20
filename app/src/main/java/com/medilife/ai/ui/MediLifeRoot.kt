package com.medilife.ai.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.medilife.ai.MainActivity
import com.medilife.ai.ui.components.GradientBackground
import com.medilife.ai.ui.components.IconBubble
import com.medilife.ai.ui.components.SpacerH
import com.medilife.ai.ui.screens.AppointmentsScreen
import com.medilife.ai.ui.screens.BodyExplorerScreen
import com.medilife.ai.ui.screens.ChatScreen
import com.medilife.ai.ui.screens.EditProfileScreen
import com.medilife.ai.ui.screens.EmergencyScreen
import com.medilife.ai.ui.screens.HealthScreen
import com.medilife.ai.ui.screens.HomeScreen
import com.medilife.ai.ui.screens.InteractionCheckerScreen
import com.medilife.ai.ui.screens.MedicalHistoryScreen
import com.medilife.ai.ui.screens.MedicineDetailScreen
import com.medilife.ai.ui.screens.MedicineEditScreen
import com.medilife.ai.ui.screens.MedicinesScreen
import com.medilife.ai.ui.screens.OnboardingScreen
import com.medilife.ai.ui.screens.ProfileScreen
import com.medilife.ai.ui.screens.ReportDetailScreen
import com.medilife.ai.ui.screens.ReportsScreen
import com.medilife.ai.ui.screens.SettingsScreen
import com.medilife.ai.ui.screens.SymptomJournalScreen
import com.medilife.ai.ui.screens.TipsScreen
import com.medilife.ai.ui.screens.TrendsScreen
import com.medilife.ai.ui.theme.MediLifeTheme
import com.medilife.ai.ui.theme.Palette

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "Home", Icons.Rounded.Home),
    Tab("medicines", "Medicines", Icons.Rounded.Medication),
    Tab("health", "Health", Icons.Rounded.MonitorHeart),
    Tab("ai", "Medi AI", Icons.Rounded.AutoAwesome),
    Tab("profile", "Profile", Icons.Rounded.Person)
)

@Composable
fun MediLifeRoot(vm: MediViewModel, activity: MainActivity) {
    val settings by vm.settings.collectAsState()
    val locked by activity.locked.collectAsState()
    val s = settings
    MediLifeTheme(themeMode = s?.themeMode ?: "DARK", reduceMotion = s?.reduceMotion ?: false) {
        GradientBackground {
            when {
                s == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                locked && s.biometricLock -> LockScreen { activity.authenticate() }
                !s.onboardingDone -> OnboardingScreen(vm)
                else -> MainShell(vm)
            }
        }
    }
}

@Composable
private fun LockScreen(onUnlock: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
        IconBubble(Icons.Rounded.Fingerprint, Palette.primaryGradient, 84.dp)
        SpacerH(20.dp)
        Text("MediLife AI is locked", style = MaterialTheme.typography.headlineSmall)
        SpacerH(8.dp)
        Text("Unlock with your fingerprint, face or screen lock.", style = MaterialTheme.typography.bodyMedium)
        SpacerH(24.dp)
        Button(onClick = onUnlock) { Text("Unlock") }
    }
}

@Composable
private fun MainShell(vm: MediViewModel) {
    val nav = rememberNavController()
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.messages.collect { snack.showSnackbar(it) } }
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBar = tabs.any { it.route == route }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            if (showBar) NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f), tonalElevation = 0.dp) {
                tabs.forEach { t ->
                    NavigationBarItem(
                        selected = route == t.route,
                        onClick = {
                            nav.navigate(t.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true; restoreState = true
                            }
                        },
                        icon = { Icon(t.icon, t.label, modifier = Modifier.size(24.dp)) },
                        label = { Text(t.label, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                    )
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad)) { Graph(nav, vm) }
    }
}

@Composable
private fun Graph(nav: NavHostController, vm: MediViewModel) {
    val back: () -> Unit = { nav.popBackStack() }
    NavHost(
        nav, startDestination = "home",
        enterTransition = { fadeIn(tween(260)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(320), initialOffset = { it / 8 }) },
        exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { fadeIn(tween(260)) },
        popExitTransition = { fadeOut(tween(180)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(300), targetOffset = { it / 8 }) }
    ) {
        composable("home") { HomeScreen(vm) { nav.navigate(it) } }
        composable("medicines") { MedicinesScreen(vm) { nav.navigate(it) } }
        composable("health") { HealthScreen(vm) { nav.navigate(it) } }
        composable("ai") { ChatScreen(vm) }
        composable("profile") { ProfileScreen(vm) { nav.navigate(it) } }
        composable("medicine/{id}", listOf(navArgument("id") { type = NavType.LongType })) {
            MedicineDetailScreen(vm, it.arguments?.getLong("id") ?: 0L, back) { r -> nav.navigate(r) }
        }
        composable("medicine_edit/{id}", listOf(navArgument("id") { type = NavType.LongType })) {
            MedicineEditScreen(vm, it.arguments?.getLong("id") ?: 0L, back)
        }
        composable("interactions") { InteractionCheckerScreen(vm, back) }
        composable("reports") { ReportsScreen(vm, back) { r -> nav.navigate(r) } }
        composable("report/{id}", listOf(navArgument("id") { type = NavType.LongType })) {
            ReportDetailScreen(vm, it.arguments?.getLong("id") ?: 0L, back)
        }
        composable("symptoms") { SymptomJournalScreen(vm, back) }
        composable("trends") { TrendsScreen(vm, back) }
        composable("appointments") { AppointmentsScreen(vm, back) }
        composable("emergency") { EmergencyScreen(vm, back) }
        composable("settings") { SettingsScreen(vm, back) }
        composable("history") { MedicalHistoryScreen(vm, back) }
        composable("profile_edit") { EditProfileScreen(vm, back) }
        composable("tips") { TipsScreen(vm, back) }
        composable("body") { BodyExplorerScreen(vm, back) }
    }
}
