package com.fyp.healthcare

import com.fyp.healthcare.ui.theme.glossySurface
import com.fyp.healthcare.ui.theme.themed
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val BrandBlue = Color(0xFF2A6DE1)
private val BrandBlueDark = Color(0xFF1E50C8)
private val TextDark: Color @Composable get() = themed(Color(0xFF1B1D23), Color(0xFFE8E9EC))
private val LabelGray: Color @Composable get() = themed(Color(0xFF5F6673), Color(0xFF9BA1AC))

@Composable
fun SignInScreen(
    userManager: UserManager,
    onSignedIn: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun startGoogleSignIn() {
        if (loading) return
        loading = true
        error = null
        scope.launch {
            when (val result = userManager.signInWithGoogle(context)) {
                is SignInResult.Success -> {
                    // Local session + data caches were wiped on the previous sign-out, so
                    // rebuild this account's state from the cloud BEFORE navigating — otherwise
                    // afterAuth() misroutes (sees no profile) and screens flash empty / stale.
                    runCatching { FamilyLink.restoreSession(context) }
                    runCatching { CloudHydrator.hydrate(context) }
                    onSignedIn()
                }
                is SignInResult.Cancelled -> loading = false
                is SignInResult.Failed -> {
                    error = result.message
                    loading = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandBlue)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))

        Box(
            modifier = Modifier.size(96.dp).clip(RoundedCornerShape(28.dp)).background(BrandBlueDark),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Favorite, contentDescription = "CareApp", tint = Color.White, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("CareApp", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Monitor health, stay safe",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp,
        )

        Spacer(Modifier.weight(1f))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glossySurface(RoundedCornerShape(24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Get Started", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(Modifier.height(4.dp))
            Text(
                "Sign in with your Google account to continue",
                fontSize = 13.sp,
                color = LabelGray,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))

            OutlinedButton(
                onClick = { startGoogleSignIn() },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFDADCE0)),
            ) {
                if (loading) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp), color = BrandBlue)
                } else {
                    GoogleGlyph()
                    Spacer(Modifier.size(12.dp))
                    Text("Continue with Google", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextDark)
                }
            }

            error?.let {
                Spacer(Modifier.height(14.dp))
                Text(it, color = Color(0xFFD32F2F), fontSize = 12.sp, textAlign = TextAlign.Center)
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "By continuing you agree to CareApp's Terms & Privacy Policy",
                fontSize = 10.sp,
                color = LabelGray,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

/** A tiny four-colour "G" so we don't ship Google's trademarked asset. */
@Composable
private fun GoogleGlyph() {
    Row {
        Text("G", color = Color(0xFF4285F4), fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
