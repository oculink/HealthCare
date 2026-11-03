package com.fyp.healthcare.i18n

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text as MaterialText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Translate an English UI string into the current language.
 *
 * English is the key: the source string itself is what gets looked up, so wrapping a literal
 * is a one-word change and anything missing from [MalayStrings] falls back to the English it
 * already was. Strings that are built at runtime (names, emails, formatted dates) aren't
 * English keys, miss the map, and pass through untouched - which is exactly what we want.
 */
fun tr(text: String): String =
    if (Language.current == AppLanguage.ENGLISH) text
    else MalayStrings.lookup(text)

/** Same as [tr] for text that sits on a coloured/transparent background. */
fun trOrNull(text: String?): String? = text?.let(::tr)

/**
 * Drop-in replacement for `androidx.compose.material3.Text` that translates before drawing.
 *
 * Every screen imports this instead of the Material one, so all the hardcoded English already
 * in the app becomes bilingual without rewriting each label by hand.
 */
@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current,
) {
    // Reading Language.current here is what subscribes the text to language changes, so the
    // whole UI re-renders the moment the switcher fires.
    MaterialText(
        text = tr(text),
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout ?: {},
        style = style,
    )
}

/**
 * Annotated strings only get translated when they carry no styling - restyling one means
 * remapping every span offset, and the screens that build styled text (highlighted words,
 * coloured numbers) already use their own literal pieces.
 */
@Composable
fun Text(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current,
) {
    val plain = text.spanStyles.isEmpty() && text.paragraphStyles.isEmpty()
    MaterialText(
        text = if (plain) AnnotatedString(tr(text.text)) else text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout ?: {},
        style = style,
    )
}

/**
 * The top-right language switcher: "EN 🌐" or "BM 🌐".
 *
 * Tapping it lists both languages; picking one saves locally and re-renders the app. It's
 * deliberately tiny so it can sit in a header without stealing room from the page title.
 */
@Composable
fun LanguageChip(
    modifier: Modifier = Modifier,
    /** True when the chip sits on the blue header and should paint in white. */
    onColor: Boolean = false,
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val language = Language.current
    val content = if (onColor) Color.White else Color(0xFF1B1D23)
    val chipBackground =
        if (onColor) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.72f)

    Row(
        modifier = modifier
            .background(chipBackground, RoundedCornerShape(50))
            .clickable { expanded = true }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = language.shortLabel,
            color = content,
            fontSize = 12.sp,
            fontWeight = Bold,
        )
        Spacer(Modifier.size(6.dp))
        Icon(
            imageVector = Icons.Filled.Language,
            contentDescription = tr("Change language"),
            tint = content,
            modifier = Modifier.size(16.dp),
        )
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
    ) {
        AppLanguage.entries.forEach { option ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = option.displayName,
                        fontSize = 14.sp,
                        fontWeight = if (option == language) Bold else FontWeight.Normal,
                    )
                },
                leadingIcon = {
                    Text(
                        text = option.shortLabel,
                        fontSize = 12.sp,
                        fontWeight = Bold,
                        color = Color(0xFF5F6673),
                    )
                },
                trailingIcon = {
                    if (option == language) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color(0xFF2A6DE1),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                },
                onClick = {
                    Language.set(context, option)
                    expanded = false
                },
            )
        }
    }
}
