package com.example.archvm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Storage
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
fun SystemSpecsDialog(
    onRunDiagnostic: (String) -> Unit,
    onDismiss: () -> Unit
) {
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
                    .padding(14.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Computer, contentDescription = null, tint = ArchCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Arch Linux VM Specs",
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

                Spacer(modifier = Modifier.height(12.dp))

                // Specs cards
                SpecItem(
                    icon = Icons.Default.Computer,
                    title = "Operating System",
                    value = "Arch Linux x86_64 (Rolling Release)",
                    sub = "Kernel 6.11.4-arch1-1-SMP PREEMPT_DYNAMIC"
                )
                Spacer(modifier = Modifier.height(8.dp))
                SpecItem(
                    icon = Icons.Default.Memory,
                    title = "Processor (Virtual CPU)",
                    value = "AMD EPYC 7763 64-Core (8 vCPUs assigned)",
                    sub = "Base Clock @ 2.449 GHz | AVX2, SSE4.2 enabled"
                )
                Spacer(modifier = Modifier.height(8.dp))
                SpecItem(
                    icon = Icons.Default.Storage,
                    title = "Memory & Storage",
                    value = "8.0 GiB RAM (1.84 GiB in use, 6.16 GiB free)",
                    sub = "Virtual NVMe Disk: 64.0 GiB (12.4 GiB system used, 51.6 GiB free)"
                )
                Spacer(modifier = Modifier.height(8.dp))
                SpecItem(
                    icon = Icons.Default.Computer,
                    title = "Software Runtimes",
                    value = "Python 3.12, GCC 14.2, Node 22.10, Rust 1.82",
                    sub = "Package Manager: Pacman 6.1.0 | Shell: GNU bash 5.2.37"
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Quick Diagnostics",
                    color = ArchCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(8.dp))

                val diagnostics = listOf(
                    "neofetch" to "Neofetch Banner",
                    "uname -a" to "Kernel Details",
                    "free -m" to "Memory Breakdown",
                    "df -h" to "Disk Partitions",
                    "cat /proc/cpuinfo" to "CPU Info"
                )

                for ((cmd, label) in diagnostics) {
                    Button(
                        onClick = {
                            onRunDiagnostic(cmd)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ArchSurface),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .testTag("diag_$cmd")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, color = TermBrightWhite, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            Text("$ $cmd", color = ArchCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String, sub: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ArchSurface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = ArchCyan, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(title, color = TermWhite, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text(value, color = TermBrightWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text(sub, color = TermYellow, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}
