package com.fyp.healthcare

import com.fyp.healthcare.i18n.Text
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fyp.healthcare.i18n.LanguageChip
import com.fyp.healthcare.ui.theme.GlossyButton
import com.fyp.healthcare.ui.theme.glossyFieldColors
import com.fyp.healthcare.ui.theme.glossyFieldWell
import com.fyp.healthcare.ui.theme.glossySurface
import com.fyp.healthcare.ui.theme.themed
import kotlinx.coroutines.launch

private val BrandBlue = Color(0xFF2A6DE1)
private val ErrorRed = Color(0xFFD32F2F)
private val TextDark: Color @Composable get() = themed(Color(0xFF1B1D23), Color(0xFFE8E9EC))
private val LabelGray: Color @Composable get() = themed(Color(0xFF5F6673), Color(0xFF9BA1AC))

/** Which half of the username + password flow [UsernameAuthScreen] opens on. */
enum class AuthMode { SignUp, SignIn }

/**
 * Username + password sign-up / sign-in - the alternative to [SignInScreen]'s Google button.
 * Both halves live in one screen: [initialMode] picks the starting view and the link at the
 * bottom flips between them without leaving the route.
 *
 * No email address is involved. Firebase only accepts addresses for password accounts, so
 * the username is mapped to a private synthetic one - see UserManager's `syntheticEmail`.
 */
@Composable
fun UsernameAuthScreen(
    userManager: UserManager,
    initialMode: AuthMode,
    onSignedIn: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var mode by remember { mutableStateOf(initialMode) }
    // State objects so the fields can read their own value: typing then repaints one field
    // instead of recomposing the whole sign-in screen.
    val username = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val confirm = remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val signingUp = mode == AuthMode.SignUp

    fun submit() {
        if (loading) return
        val u = username.value
        val p = password.value
        val c = confirm.value
        error = when {
            u.isBlank() -> "Choose a username."
            !isValidUsername(u) ->
                "$MIN_USERNAME_LENGTH-$MAX_USERNAME_LENGTH letters, numbers, dot, underscore or dash."
            p.length < MIN_PASSWORD_LENGTH ->
                "Password must be at least $MIN_PASSWORD_LENGTH characters."
            signingUp && p != c -> "The two passwords don't match."
            else -> null
        }
        if (error != null) return

        loading = true
        scope.launch {
            val result = if (signingUp) {
                userManager.signUpWithUsername(u, p)
            } else {
                userManager.signInWithUsername(u, p)
            }
            when (result) {
                is SignInResult.Success -> {
                    // Same hand-off as the Google path: rebuild this account's local state
                    // from the cloud before navigating, or routing misroutes.
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandBlue)
            .imePadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, enabled = !loading) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                )
            }
            Spacer(Modifier.weight(1f))
            // The sign-up/sign-in half of Get Started keeps the same top-right switcher.
            LanguageChip(onColor = true)
        }

        Spacer(Modifier.height(16.dp))

        Text(
            if (signingUp) "Create your account" else "Welcome back",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (signingUp) {
                "Pick a username and password.\nNo email needed · they're only used to sign in to CareApp."
            } else {
                "Sign in with the username and password you chose."
            },
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .glossySurface(RoundedCornerShape(24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FieldLabelRow("Username", LabelGray)
            AuthField(
                value = username,
                onValueChange = {
                    // Anything outside the allowed set never reaches the field, so a typed
                    // space or emoji can't produce a username Firebase would reject.
                    username.value = it.filter { c ->
                        c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' || c in "._-"
                    }.take(MAX_USERNAME_LENGTH)
                    error = null
                },
                placeholder = "e.g. auntieMay",
                enabled = !loading,
                imeAction = ImeAction.Next,
            )

            Spacer(Modifier.height(14.dp))

            FieldLabelRow("Password", LabelGray)
            AuthField(
                value = password,
                onValueChange = {
                    password.value = it
                    error = null
                },
                placeholder = "$MIN_PASSWORD_LENGTH+ characters",
                enabled = !loading,
                isPassword = !showPassword,
                trailing = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        val glyph = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                        Icon(
                            glyph,
                            contentDescription = if (showPassword) "Hide password" else "Show password",
                            tint = LabelGray,
                        )
                    }
                },
                imeAction = if (signingUp) ImeAction.Next else ImeAction.Done,
                onImeSubmit = { submit() },
            )

            if (signingUp) {
                Spacer(Modifier.height(14.dp))

                FieldLabelRow("Re-enter password", LabelGray)
                AuthField(
                    value = confirm,
                    onValueChange = {
                        confirm.value = it
                        error = null
                    },
                    placeholder = "Type it again",
                    enabled = !loading,
                    isPassword = !showPassword,
                    imeAction = ImeAction.Done,
                    onImeSubmit = { submit() },
                )
            }

            error?.let {
                Spacer(Modifier.height(10.dp))
                FieldLabelRow(it, ErrorRed)
            }

            Spacer(Modifier.height(18.dp))

            GlossyButton(
                onClick = { submit() },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
                color = BrandBlue,
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                    )
                } else {
                    Text(
                        if (signingUp) "Sign up" else "Sign in",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            TextButton(
                onClick = {
                    mode = if (signingUp) AuthMode.SignIn else AuthMode.SignUp
                    error = null
                },
                enabled = !loading,
            ) {
                Text(
                    if (signingUp) "Already have an account? Sign in" else "New here? Sign up",
                    fontSize = 13.sp,
                    color = LabelGray,
                )
            }

            // There is no reset email to send (no mailbox behind a username), so the way out
            // of a forgotten password has to be said here, where someone is looking for it.
            if (!signingUp) {
                Spacer(Modifier.height(2.dp))
                Text(
                    "Forgotten it? Use Continue with Google if you linked one - " +
                        "CareApp can't email a reset link to a username account.",
                    fontSize = 12.sp,
                    color = LabelGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // A username account has no mailbox, so there is no way to email a reset link -
        // worth saying before someone commits to a password they'll forget.
        if (signingUp) {
            Spacer(Modifier.height(16.dp))
            Text(
                "A username account has no email to send a reset link to,\nso keep your password safe.",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "By continuing you agree to CareApp's Terms & Privacy Policy",
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(8.dp))
    }
}

/** Small, chunky field label - the text fields use placeholders instead of floating labels. */
@Composable
private fun FieldLabelRow(text: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth().padding(start = 2.dp, bottom = 4.dp)) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}

/**
 * One well + text field pair. The label sits outside the well because
 * [glossyFieldWell] clips its child, which would cut off a floating label.
 */
@Composable
private fun AuthField(
    value: State<String>,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean,
    isPassword: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
    onImeSubmit: () -> Unit = {},
    trailing: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value.value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().glossyFieldWell(RoundedCornerShape(14.dp)),
        placeholder = { Text(placeholder, color = Color(0xFFA6ACB8), fontSize = 16.sp) },
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp, color = TextDark),
        singleLine = true,
        enabled = enabled,
        visualTransformation = if (isPassword) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text,
            imeAction = imeAction,
        ),
        keyboardActions = KeyboardActions(onDone = { onImeSubmit() }),
        trailingIcon = trailing,
        shape = RoundedCornerShape(14.dp),
        colors = glossyFieldColors(accent = BrandBlue),
    )
}
