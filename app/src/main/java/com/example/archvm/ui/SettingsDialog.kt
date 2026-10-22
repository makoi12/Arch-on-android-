package com.example.archvm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*

@Composable
fun SettingsDialog(
    fontSize: Float,
    crtEnabled: Boolean,
    activeTheme: String,
    onFontSizeChange: (Float) -> Unit,
    onToggleCrt: () -> Unit,
    onSelectTheme: (String) -> Unit,
    onResetTerminal: () -> Unit,
    onDismiss: () -> Unit
) {
    val themes = listOf("Arch Linux", "Dracula", "Matrix Green", "Monokai", "Nord")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .clip(RoundedCornerShape(12.dp)),
            color = ArchDarkNavy
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ArchDarkNavy)
                    .padding(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = ArchCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Terminal Settings",
                            color = TermBrightWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TermWhite)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Font Size control
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ArchSurface),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Terminal Font Size: ${fontSize.toInt()}sp", color = TermBrightWhite, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { onFontSizeChange(-1f) },
                                colors = ButtonDefaults.buttonColors(containerColor = ArchSurfaceHighlight),
                                modifier = Modifier.testTag("font_dec")
                            ) {
                                Text("- Smaller", color = TermBrightWhite, fontFamily = FontFamily.Monospace)
                            }

                            Text(
                                "Preview: $ ls -la",
                                color = ArchCyan,
                                fontSize = fontSize.sp,
                                fontFamily = FontFamily.Monospace
                            )

                            Button(
                                onClick = { onFontSizeChange(1f) },
                                colors = ButtonDefaults.buttonColors(containerColor = ArchSurfaceHighlight),
                                modifier = Modifier.testTag("font_inc")
                            ) {
                                Text("+ Larger", color = TermBrightWhite, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // CRT Scanlines Toggle
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ArchSurface),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Retro CRT Scanlines", color = TermBrightWhite, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                            Text("Vintage monitor phosphor line effect", color = TermWhite, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                        Switch(
                            checked = crtEnabled,
                            onCheckedChange = { onToggleCrt() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = ArchCyan,
                                uncheckedThumbColor = TermWhite,
                                uncheckedTrackColor = ArchSurfaceHighlight
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Color Themes
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ArchSurface),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Terminal Palette", color = TermBrightWhite, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(8.dp))
                        for (theme in themes) {
                            val isSelected = theme == activeTheme
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) ArchSurfaceHighlight else Color.Transparent)
                                    .clickable { onSelectTheme(theme) }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = theme,
                                    color = if (isSelected) ArchCyanBright else TermWhite,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (isSelected) {
                                    Text("Active", color = TermGreen, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Reset VM
                Button(
                    onClick = {
                        onResetTerminal()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TermRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("reset_terminal_button")
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear History & Reset Session", color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
