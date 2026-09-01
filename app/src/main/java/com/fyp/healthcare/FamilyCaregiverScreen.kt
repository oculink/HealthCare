package com.fyp.healthcare

import com.fyp.healthcare.ui.theme.appBackground
import com.fyp.healthcare.ui.theme.glossyTopBar
import com.fyp.healthcare.ui.theme.glossySurface
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.fyp.healthcare.ui.theme.themed
import kotlinx.coroutines.launch

private val BrandBlue = Color(0xFF2A6DE1)
private val AlertRed = Color(0xFFD32F2F)
private val CardWhite: Color @Composable get() = themed(Color(0xFFFFFFFF), Color(0xFF1C1D22))
private val ScreenBackground: Color @Composable get() = themed(Color(0xFFEFF1F6), Color(0xFF121316))
private val FieldBackground: Color @Composable get() = themed(Color(0xFFEDEFF4), Color(0xFF262730))
private val TextDark: Color @Composable get() = themed(Color(0xFF1B1D23), Color(0xFFE8E9EC))
private val LabelGray: Color @Composable get() = themed(Color(0xFF5F6673), Color(0xFF9BA1AC))

@Composable
fun FamilyCaregiverScreen(
    onBack: () -> Unit,
    onUnlinked: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val caretakerView = Session.isCaretakerMode

    var loading by remember { mutableStateOf(true) }
    var working by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf<String?>(null) }
    var caretakers by remember { mutableStateOf<List<FamilyLink.CaretakerInfo>>(emptyList()) }
    var errorText by remember { mutableStateOf<String?>(null) }

    var confirmReset by remember { mutableStateOf(false) }
    var confirmRevoke by remember { mutableStateOf<FamilyLink.CaretakerInfo?>(null) }
    var confirmUnlink by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!caretakerView) {
            runCatching { FamilyLink.myCode() }
                .onSuccess { code = it }
                .onFailure { errorText = "Couldn't load your code. Check your connection." }
            caretakers = runCatching { FamilyLink.linkedCaretakers() }.getOrDefault(emptyList())
        }
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize().appBackground()) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .glossyTopBar(BrandBlue)
                .statusBarsPadding()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                "Family Caregiver",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(48.dp))
        }

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandBlue)
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (caretakerView) {
                CaretakerLinkedCard()
                TextButton(
                    onClick = { confirmUnlink = true },
                    enabled = !working,
                ) {
                    Icon(Icons.Filled.LinkOff, contentDescription = null, tint = AlertRed, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Unlink from this patient", color = AlertRed, fontWeight = FontWeight.Bold)
                }
            } else {
                CodeCard(
                    code = code,
                    error = errorText,
                    onCopy = { code?.let { copyToClipboard(context, it) } },
                    onShare = { code?.let { shareCode(context, it) } },
                )
                TextButton(onClick = { confirmReset = true }, enabled = !working && code != null) {
                    Icon(Icons.Filled.Autorenew, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Reset code", color = BrandBlue, fontWeight = FontWeight.Bold)
                }

                Text("Linked Caregivers", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LabelGray)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glossySurface(RoundedCornerShape(20.dp), CardWhite)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (caretakers.isEmpty()) {
                        Text("No caregivers linked yet.", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                        Text(
                            "Share your code with a family member so they can help manage your health.",
                            fontSize = 12.sp,
                            color = LabelGray,
                        )
                    } else {
                        caretakers.forEach { c ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AccountAvatar(
                                    photoUrl = c.photoUrl,
                                    initials = profileInitials(c.name),
                                    size = 40.dp,
                                    background = BrandBlue,
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(c.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextDark, modifier = Modifier.weight(1f))
                                TextButton(onClick = { confirmRevoke = c }, enabled = !working) {
                                    Icon(Icons.Filled.PersonRemove, contentDescription = "Remove", tint = AlertRed, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Remove", color = AlertRed, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                Text(
                    "Anyone with this code can view and manage your health data, medication and " +
                        "emergency profile. Reset the code if you no longer want it shared.",
                    fontSize = 11.sp,
                    color = LabelGray,
                )
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset your code?") },
            text = { Text("A new code is generated. Caregivers already linked stay linked, but the old code stops working for new links.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    working = true
                    scope.launch {
                        runCatching { FamilyLink.resetCode() }
                            .onSuccess { code = it }
                            .onFailure { errorText = "Couldn't reset. Try again." }
                        working = false
                    }
                }) { Text("Reset", color = BrandBlue, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
        )
    }

    confirmRevoke?.let { c ->
        AlertDialog(
            onDismissRequest = { confirmRevoke = null },
            title = { Text("Remove ${c.name}?") },
            text = { Text("They will lose access to your account immediately.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmRevoke = null
                    working = true
                    scope.launch {
                        runCatching { FamilyLink.revokeCaretaker(c.uid) }
                        caretakers = runCatching { FamilyLink.linkedCaretakers() }.getOrDefault(caretakers)
                        working = false
                    }
                }) { Text("Remove", color = AlertRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmRevoke = null }) { Text("Cancel") } },
        )
    }

    if (confirmUnlink) {
        AlertDialog(
            onDismissRequest = { confirmUnlink = false },
            title = { Text("Unlink from this patient?") },
            text = { Text("You'll stop managing their account and return to your own.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmUnlink = false
                    working = true
                    scope.launch {
                        runCatching { FamilyLink.unlink() }
                        working = false
                        onUnlinked()
                    }
                }) { Text("Unlink", color = AlertRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmUnlink = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun CodeCard(
    code: String?,
    error: String?,
    onCopy: () -> Unit,
    onShare: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glossySurface(RoundedCornerShape(20.dp), CardWhite)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Your Family Code", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LabelGray)
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(FieldBackground)
                .padding(vertical = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                code ?: (error?.let { "—" } ?: "…"),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 6.sp,
                color = TextDark,
            )
        }
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, fontSize = 12.sp, color = AlertRed, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ActionButton(Icons.Filled.ContentCopy, "Copy", Modifier.weight(1f), enabled = code != null, onClick = onCopy)
            ActionButton(Icons.Filled.Share, "Share", Modifier.weight(1f), enabled = code != null, onClick = onShare)
        }
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(BrandBlue.copy(alpha = if (enabled) 0.12f else 0.05f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
    }
}

@Composable
private fun CaretakerLinkedCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glossySurface(RoundedCornerShape(20.dp), CardWhite)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AccountAvatar(
            photoUrl = Session.controlledPatientPhoto,
            initials = profileInitials(Session.controlledPatientName.ifBlank { "P" }),
            size = 52.dp,
            background = BrandBlue,
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Managing", fontSize = 12.sp, color = LabelGray)
            Text(
                Session.controlledPatientName.ifBlank { "the patient" },
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
            )
            Text("Their health data saves to their account", fontSize = 12.sp, color = LabelGray)
        }
    }
}

private fun copyToClipboard(context: Context, code: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    cm.setPrimaryClip(ClipData.newPlainText("CareApp family code", code))
    Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
}

private fun shareCode(context: Context, code: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(
            Intent.EXTRA_TEXT,
            "Link to my CareApp health monitoring. Open CareApp → sign in → \"I'm a Caregiver\" " +
                "→ enter this code:\n\n$code",
        )
    }
    context.startActivity(Intent.createChooser(send, "Share family code"))
}
