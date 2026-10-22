package com.example.archvm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*

@Composable
fun NanoEditorDialog(
    filePath: String,
    initialContent: String,
    onSave: (String) -> Unit,
    onExit: () -> Unit
) {
    var content by remember { mutableStateOf(initialContent) }
    var isModified by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onExit,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(ArchDarkNavy),
            color = ArchDarkNavy
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ArchTerminalBg)
            ) {
                // GNU Nano Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ArchCyan)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GNU nano 8.2",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = filePath.substringAfterLast("/"),
                        color = Color.Black,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (isModified) "Modified" else "",
                        color = TermRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // File Path info
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ArchSurface)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "File: $filePath",
                        color = TermWhite,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Editor Text Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    BasicTextField(
                        value = content,
                        onValueChange = {
                            content = it
                            isModified = true
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("nano_content_input"),
                        textStyle = TextStyle(
                            color = TermBrightWhite,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        ),
                        cursorBrush = SolidColor(ArchCyanBright)
                    )
                }

                // Bottom Nano Command Shortcuts Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ArchDarkNavy)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Button(
                        onClick = {
                            onSave(content)
                            isModified = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ArchSurfaceHighlight),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("nano_save_button")
                    ) {
                        Text(
                            text = "^O WriteOut (Save)",
                            color = TermGreen,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onExit,
                        colors = ButtonDefaults.buttonColors(containerColor = ArchSurfaceHighlight),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("nano_exit_button")
                    ) {
                        Text(
                            text = "^X Exit",
                            color = TermRed,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
