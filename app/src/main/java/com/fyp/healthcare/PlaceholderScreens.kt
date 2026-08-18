package com.fyp.healthcare

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

// Blank page for features that aren't built yet
@Composable
fun BlankScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFFEFF1F6)),
        contentAlignment = Alignment.Center
    ) {
        Text("$title — coming soon", color = Color.Gray, fontSize = 16.sp)
    }
}

// Bottom bar. Emojis are placeholders — swap for asset images later
@Composable
fun BottomNavBar(currentRoute: String, onNavigate: (String) -> Unit) {
    val items = listOf(
        Triple("home", "Home", "🏠"),
        Triple("health", "Health", "❤️"),
        Triple("reminders", "Reminders", "⏰"),
        Triple("activity", "Activity", "🏃"),
        Triple("profile", "Profile", "👤")
    )

    NavigationBar(containerColor = Color.White) {
        items.forEach { (route, label, emoji) ->
            NavigationBarItem(
                selected = currentRoute == route,
                onClick = { onNavigate(route) },
                icon = { Text(emoji, fontSize = 20.sp) },
                label = { Text(label, fontSize = 11.sp) }
            )
        }
    }
}

// Wraps the 5 main tabs so they all share the bottom bar
@Composable
fun TabScaffold(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(bottomBar = { BottomNavBar(currentRoute, onNavigate) }) { innerPadding ->
        Box(Modifier.padding(innerPadding).fillMaxSize()) {
            content()
        }
    }
}