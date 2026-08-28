package com.fyp.healthcare

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Circular account avatar: shows the Google account's profile photo once it downloads,
 * otherwise the name initials on a solid circle.
 *
 * The photo comes from FirebaseUser.photoUrl (a public lh3.googleusercontent.com URL that
 * Google Sign-In fills in). It's fetched once and cached on disk, so later screens paint
 * it without hitting the network again. No image-loading library — it's a single small JPEG.
 */
@Composable
fun AccountAvatar(
    photoUrl: String?,
    initials: String,
    size: Dp,
    background: Color,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, photoUrl) {
        value = photoUrl?.takeIf { it.isNotBlank() }?.let { loadAvatar(context, it) }
    }

    Box(
        modifier = modifier.size(size).clip(CircleShape).background(background),
        contentAlignment = Alignment.Center,
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = "Profile picture",
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        } else {
            Text(
                initials,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value / 3f).sp,
            )
        }
    }
}

private suspend fun loadAvatar(context: Context, url: String): ImageBitmap? =
    withContext(Dispatchers.IO) {
        runCatching {
            val cacheFile = File(context.cacheDir, "avatar_${url.hashCode()}.jpg")
            if (!cacheFile.exists() || cacheFile.length() == 0L) {
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8_000
                    readTimeout = 8_000
                    instanceFollowRedirects = true
                }
                conn.inputStream.use { input ->
                    cacheFile.outputStream().use { output -> input.copyTo(output) }
                }
            }
            BitmapFactory.decodeFile(cacheFile.absolutePath)?.asImageBitmap()
        }.getOrNull()
    }
