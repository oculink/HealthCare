package com.fyp.healthcare

import com.fyp.healthcare.ui.theme.appBackground
import com.fyp.healthcare.ui.theme.glossyChip
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fyp.healthcare.ui.theme.themed

// Blank page for features that aren't built yet
@Composable
fun BlankScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize().appBackground(),
        contentAlignment = Alignment.Center
    ) {
        Text("$title — coming soon", color = themed(Color(0xFF8A8F98), Color(0xFF8A8F98)), fontSize = 16.sp)
    }
}

private val NavAccent = Color(0xFF2A6DE1)

/**
 * Bottom navigation — a raised glossy bar. The selected tab's icon sits in a glossy
 * accent pill and its label goes bold blue, so the current place is obvious at a
 * glance. Larger icons + labels than the stock Material bar, for older eyes.
 */
@Composable
fun BottomNavBar(currentRoute: String, onNavigate: (String) -> Unit) {
    val items = listOf(
        Triple("home", "Home", Icons.Filled.Home),
        Triple("health", "Health", Icons.Filled.MonitorHeart),
        Triple("reminders", "Reminders", Icons.Filled.Alarm),
        Triple("activity", "Activity", Icons.AutoMirrored.Filled.DirectionsRun),
        Triple("profile", "Profile", Icons.Filled.Person),
    )
    val dark = com.fyp.healthcare.ui.theme.AppTheme.isDark
    val surface = themed(Color(0xFFFFFFFF), Color(0xFF1C1D22))
    val idle = themed(Color(0xFF5F6673), Color(0xFF9BA1AC))
    val topLine = if (dark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.10f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(themed(Color(0xFFFCFCFF), Color(0xFF232429)), surface),
                ),
            )
            .drawWithContent {
                drawContent()
                drawLine(topLine, Offset(0f, 0.5f), Offset(size.width, 0.5f), strokeWidth = 1f)
            }
            .navigationBarsPadding()
            .padding(top = 8.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { (route, label, icon: ImageVector) ->
            val selected = currentRoute == route
            val interaction = remember { MutableInteractionSource() }
            Column(
                modifier = Modifier
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = { if (!selected) onNavigate(route) },
                    )
                    .padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .glossyChip(selected, NavAccent, RoundedCornerShape(50))
                        .padding(horizontal = 22.dp, vertical = 6.dp),
                ) {
                    Icon(
                        icon,
                        contentDescription = label,
                        tint = if (selected) Color.White else idle,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    label,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) NavAccent else idle,
                )
            }
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
