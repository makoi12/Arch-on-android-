package com.example.archvm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
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

data class StarterItem(
    val title: String,
    val runtime: String,
    val command: String,
    val description: String,
    val tagColor: Color
)

@Composable
fun StarterScriptsDialog(
    onRunCommand: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val starters = listOf(
        StarterItem(
            title = "Python 3 Prime Sieve & Worker Threads",
            runtime = "python3",
            command = "python ~/projects/hello.py",
            description = "Runs Python math logic, list comprehensions, and worker simulation.",
            tagColor = TermYellow
        ),
        StarterItem(
            title = "GCC 14.2 C Program Compilation",
            runtime = "gcc",
            command = "gcc ~/projects/main.c -o demo && ./demo",
            description = "Compiles main.c with GCC into an x86_64 binary and runs it.",
            tagColor = TermBlue
        ),
        StarterItem(
            title = "Node.js JavaScript Environment",
            runtime = "node",
            command = "node ~/projects/app.js",
            description = "Executes server-side JavaScript with JSON serialization and arrays.",
            tagColor = TermGreen
        ),
        StarterItem(
            title = "Rust Cargo Fibonacci Calculator",
            runtime = "rust",
            command = "cargo run",
            description = "Builds and runs Rust project with Fibonacci sequence stream.",
            tagColor = Color(0xFFE57373)
        ),
        StarterItem(
            title = "ArchVM System Health Shell Script",
            runtime = "bash",
            command = "bash ~/projects/script.sh",
            description = "Runs date, uname, free, df commands in an automated bash script.",
            tagColor = ArchCyan
        ),
        StarterItem(
            title = "Neofetch System Information",
            runtime = "system",
            command = "neofetch",
            description = "Displays the iconic Arch Linux ASCII art logo and container specs.",
            tagColor = ArchCyanBright
        ),
        StarterItem(
            title = "Interactive Htop Process Viewer",
            runtime = "system",
            command = "htop",
            description = "Live real-time 8-core CPU meters and process task manager.",
            tagColor = TermGreen
        ),
        StarterItem(
            title = "Live Web Query (cURL)",
            runtime = "network",
            command = "curl icanhazip.com",
            description = "Makes real HTTP network requests from inside the Arch VM.",
            tagColor = TermYellow
        ),
        StarterItem(
            title = "The Matrix Digital Rain",
            runtime = "fun",
            command = "cmatrix",
            description = "Classic green cascading digital cyber rain.",
            tagColor = TermGreen
        ),
        StarterItem(
            title = "Cowsay Fortune Geek Quotes",
            runtime = "fun",
            command = "fortune | cowsay",
            description = "Pipes random developer wisdom into speaking ASCII cow.",
            tagColor = TermMagenta
        ),
        StarterItem(
            title = "Steam Locomotive Train",
            runtime = "fun",
            command = "sl",
            description = "Runs the classic ASCII steam train animation across your terminal.",
            tagColor = TermBrightWhite
        )
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .clip(RoundedCornerShape(12.dp)),
            color = ArchDarkNavy
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ArchDarkNavy)
                    .padding(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Starter Scripts & Distributions",
                            color = ArchCyan,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "One-tap interactive developer execution demos",
                            color = TermWhite,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TermWhite)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(starters) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = ArchSurface),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.title,
                                        color = TermBrightWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(ArchSurfaceHighlight)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = item.runtime,
                                            color = item.tagColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.description,
                                    color = TermWhite,
                                    fontSize = 11.sp
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(ArchTerminalBg)
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$ ${item.command}",
                                        color = ArchCyanBright,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Button(
                                        onClick = { onRunCommand(item.command) },
                                        colors = ButtonDefaults.buttonColors(containerColor = ArchCyan),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.height(28.dp).testTag("run_${item.runtime}")
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Run", tint = Color.Black, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Run", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
