package com.example.archvm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import kotlinx.coroutines.delay

data class HtopProcess(
    val pid: Int,
    val user: String,
    val pri: Int,
    val ni: Int,
    val virt: String,
    val res: String,
    val shr: String,
    val state: String,
    var cpu: Double,
    val mem: Double,
    val time: String,
    val command: String
)

@Composable
fun HtopOverlay(onQuit: () -> Unit) {
    var cpuLoads by remember {
        mutableStateOf(listOf(18.2, 42.5, 12.0, 28.4, 8.5, 33.1, 15.6, 22.0))
    }

    var processes by remember {
        mutableStateOf(
            listOf(
                HtopProcess(1, "root", 20, 0, "168M", "12M", "8M", "S", 0.0, 0.2, "0:01.42", "/sbin/init"),
                HtopProcess(2, "root", 20, 0, "0", "0", "0", "S", 0.0, 0.0, "0:00.08", "[kthreadd]"),
                HtopProcess(184, "root", 20, 0, "24M", "4M", "3M", "S", 0.2, 0.1, "0:00.35", "/usr/lib/systemd-journald"),
                HtopProcess(940, "user", 20, 0, "14M", "7M", "5M", "S", 0.1, 0.1, "0:00.22", "bash"),
                HtopProcess(1120, "user", 20, 0, "128M", "18M", "12M", "R", 12.4, 0.4, "0:02.15", "python3 ~/projects/hello.py"),
                HtopProcess(1250, "user", 20, 0, "82M", "14M", "9M", "S", 4.2, 0.3, "0:01.02", "node ~/projects/app.js"),
                HtopProcess(1310, "user", 20, 0, "18M", "6M", "4M", "R", 1.8, 0.1, "0:00.41", "htop")
            )
        )
    }

    // Dynamic timer update
    LaunchedEffect(Unit) {
        while (true) {
            delay(1500)
            cpuLoads = cpuLoads.map {
                (it + ((-10..10).random() * 0.8)).coerceIn(5.0, 85.0)
            }
            processes = processes.map {
                if (it.pid == 1120 || it.pid == 1310) {
                    it.copy(cpu = (it.cpu + ((-5..5).random() * 0.5)).coerceIn(0.5, 45.0))
                } else it
            }
        }
    }

    Dialog(
        onDismissRequest = onQuit,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
                .clip(RoundedCornerShape(8.dp)),
            color = ArchDarkNavy
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0C1017))
                    .padding(8.dp)
            ) {
                // Top Header / Gauges
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Left: CPU 1-4
                    Column(modifier = Modifier.weight(1f)) {
                        for (i in 0 until 4) {
                            HtopMeterRow(
                                label = "${i + 1}",
                                percent = cpuLoads[i],
                                barText = "${"%.1f".format(cpuLoads[i])}%"
                            )
                        }
                        HtopMeterRow(
                            label = "Mem",
                            percent = 23.0,
                            barText = "1.84G/7.81G"
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Right: CPU 5-8 & Swap
                    Column(modifier = Modifier.weight(1f)) {
                        for (i in 4 until 8) {
                            HtopMeterRow(
                                label = "${i + 1}",
                                percent = cpuLoads[i],
                                barText = "${"%.1f".format(cpuLoads[i])}%"
                            )
                        }
                        HtopMeterRow(
                            label = "Swp",
                            percent = 0.0,
                            barText = "0K/4.00G"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Tasks & Uptime Info
                Text(
                    text = "Tasks: 38, 54 thr; 2 running  |  Load average: 0.28 0.15 0.08  |  Uptime: 01:42:10",
                    color = TermWhite,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Process Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ArchCyan)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(" PID", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(36.dp))
                    Text("USER", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(42.dp))
                    Text("PRI", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(28.dp))
                    Text("VIRT", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(42.dp))
                    Text("S", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(18.dp))
                    Text("CPU%", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(42.dp))
                    Text("MEM%", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(42.dp))
                    Text("COMMAND", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                }

                // Process Rows
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(processes) { p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 1.dp)
                        ) {
                            Text(p.pid.toString().padStart(4), color = ArchCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(36.dp))
                            Text(p.user, color = TermWhite, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(42.dp))
                            Text(p.pri.toString().padStart(3), color = TermWhite, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(28.dp))
                            Text(p.virt.padStart(5), color = TermYellow, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(42.dp))
                            Text(p.state, color = if (p.state == "R") TermGreen else TermWhite, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(18.dp))
                            Text("${"%.1f".format(p.cpu)}%".padStart(5), color = if (p.cpu > 5.0) TermGreen else TermWhite, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(42.dp))
                            Text("${"%.1f".format(p.mem)}%".padStart(5), color = TermWhite, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(42.dp))
                            Text(p.command, color = TermBrightWhite, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                        }
                    }
                }

                // Bottom Function Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ArchSurfaceHighlight)
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("F1:Help", color = TermGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text("F3:Search", color = TermYellow, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text("F6:Sort", color = ArchCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Button(
                        onClick = onQuit,
                        colors = ButtonDefaults.buttonColors(containerColor = TermRed),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier
                            .height(28.dp)
                            .testTag("htop_quit_button")
                    ) {
                        Text("F10:Quit", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun HtopMeterRow(label: String, percent: Double, barText: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label.padStart(3),
            color = ArchCyan,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(28.dp)
        )
        Text("[", color = ArchBorder, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(11.dp)
                .background(ArchSurface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((percent / 100.0).toFloat().coerceIn(0f, 1f))
                    .background(if (percent > 60) TermRed else if (percent > 30) TermYellow else TermGreen)
            )
        }
        Text("]", color = ArchBorder, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        Text(
            text = " $barText",
            color = TermWhite,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(72.dp)
        )
    }
}
