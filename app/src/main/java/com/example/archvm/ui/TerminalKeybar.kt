package com.example.archvm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun TerminalKeybar(
    onKeyClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val keys = listOf(
        "TAB", "▲", "▼", "CTRL+C", "CTRL+L", "CTRL+D",
        "ESC", "|", "/", "-", "~", "_", "$", "\\",
        "&", ">", "<", "\"", "'", ";", ":", "*"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(ArchSurface)
            .padding(horizontal = 4.dp, vertical = 5.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (key in keys) {
            val isActionKey = key in listOf("TAB", "CTRL+C", "CTRL+L", "CTRL+D", "ESC", "▲", "▼")
            val bgColor = if (isActionKey) ArchSurfaceHighlight else ArchTerminalBg
            val textColor = when (key) {
                "TAB" -> ArchCyanBright
                "CTRL+C" -> TermRed
                "CTRL+L" -> TermYellow
                "▲", "▼" -> TermGreen
                else -> TermBrightWhite
            }

            Box(
                modifier = Modifier
                    .height(36.dp)
                    .widthIn(min = if (key.length > 2) 54.dp else 36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(bgColor)
                    .clickable { onKeyClick(key) }
                    .testTag("key_$key")
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = key,
                    color = textColor,
                    fontSize = if (key.length > 4) 11.sp else 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
