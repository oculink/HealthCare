package com.fyp.healthcare

import com.fyp.healthcare.i18n.Text
import com.fyp.healthcare.i18n.LanguageChip
import com.fyp.healthcare.i18n.tr
import com.fyp.healthcare.ui.theme.GlossyButton
import com.fyp.healthcare.ui.theme.glossySurface
import com.fyp.healthcare.ui.theme.themed
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val BrandBlue = Color(0xFF2A6DE1)
private val TextDark: Color @Composable get() = themed(Color(0xFF1B1D23), Color(0xFFE8E9EC))
private val LabelGray: Color @Composable get() = themed(Color(0xFF5F6673), Color(0xFF9BA1AC))
private val SeparatorGray: Color @Composable get() = themed(Color(0xFFE3E5E9), Color(0xFF33363E))

@Composable
fun SignInScreen(
    userManager: UserManager,
    onSignUp: () -> Unit,
    onSignIn: () -> Unit,
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
                    // rebuild this account's state from the cloud before navigating - see
                    // UserManager.finishSignIn().
                    userManager.finishSignIn(context)
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandBlue),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))

            Image(
                painter = painterResource(R.drawable.ic_app_logo),
                contentDescription = "CareApp",
                modifier = Modifier.size(96.dp).clip(RoundedCornerShape(28.dp)),
            )
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
                    "Continue with Google, or use a username and password",
                    fontSize = 13.sp,
                    color = LabelGray,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))

                OutlinedButton(
                    onClick = { startGoogleSignIn() },
                    enabled = !loading,
                    // heightIn, not height: at a large system font the label must be able to
                    // grow the button instead of being clipped by it.
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFDADCE0)),
                ) {
                    if (loading) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp), color = BrandBlue)
                    } else {
                        GoogleGlyph()
                        Spacer(Modifier.size(12.dp))
                        // Naming the account this device used last is the guard against picking a
                        // different Google account by habit and landing in someone else's
                        // CareApp account. Still just a label - the picker decides.
                        Text(
                            GoogleHint.last(context)?.let { "${tr("Continue as")} $it" }
                                ?: tr("Continue with Google"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                error?.let {
                    Spacer(Modifier.height(14.dp))
                    Text(it, color = Color(0xFFD32F2F), fontSize = 12.sp, textAlign = TextAlign.Center)
                }

                Spacer(Modifier.height(18.dp))

                // Line between the Google option and the email/password options.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f).height(1.dp).background(SeparatorGray))
                    Text(" or ", fontSize = 12.sp, color = LabelGray)
                    Box(Modifier.weight(1f).height(1.dp).background(SeparatorGray))
                }

                Spacer(Modifier.height(18.dp))

                GlossyButton(
                    onClick = onSignUp,
                    modifier = Modifier.fillMaxWidth(),
                    color = BrandBlue,
                ) {
                    Text("Sign up", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onSignIn,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFDADCE0)),
                ) {
                    Text("Sign in", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextDark)
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

            // Language lives here as well as in Profile: someone who can't read the English
            // sign-in card should be able to fix that before creating an account.
            LanguageChip(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 8.dp, end = 20.dp),
                onColor = true,
            )
        }
    }

/** A tiny four-colour "G" so we don't ship Google's trademarked asset. */
@Composable
private fun GoogleGlyph() {
    Row {
        Text("G", color = Color(0xFF4285F4), fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
