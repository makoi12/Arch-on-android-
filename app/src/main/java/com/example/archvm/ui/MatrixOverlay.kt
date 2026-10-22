package com.example.archvm.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.ArchDarkNavy
import com.example.ui.theme.TermGreen
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun MatrixOverlay(onDismiss: () -> Unit) {
    val charList = remember {
        "0123456789ABCDEFabcdefｦｱｳｴｵｶｷｹｺｻｼｽｾｿﾀﾂﾃﾅﾆﾇﾈﾊﾋﾎﾏﾐﾑﾒﾓﾔﾕﾗﾘﾜ".toList()
    }

    var tick by remember { mutableStateOf(0) }
    val columnDrops = remember { mutableStateListOf<Float>().apply { repeat(28) { add(Random.nextFloat() * 1000f) } } }

    LaunchedEffect(Unit) {
        while (true) {
            delay(45)
            tick++
            for (i in columnDrops.indices) {
                columnDrops[i] = (columnDrops[i] + 28f)
                if (columnDrops[i] > 1800f && Random.nextFloat() > 0.85f) {
                    columnDrops[i] = 0f
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
                .clip(RoundedCornerShape(8.dp)),
            color = Color.Black
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { onDismiss() }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.GREEN
                        textSize = 36f
                        typeface = android.graphics.Typeface.MONOSPACE
                        isAntiAlias = true
                    }
                    val paintHead = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 38f
                        typeface = android.graphics.Typeface.MONOSPACE
                        isAntiAlias = true
                    }

                    val colWidth = size.width / 26f
                    for (col in 0 until 26) {
                        val currentY = columnDrops.getOrElse(col) { 0f }
                        val x = col * colWidth

                        // Draw falling character trail
                        for (trail in 0 until 18) {
                            val y = currentY - (trail * 40f)
                            if (y > 0 && y < size.height) {
                                val char = charList[(tick + col * 7 + trail * 3) % charList.size].toString()
                                if (trail == 0) {
                                    drawContext.canvas.nativeCanvas.drawText(char, x, y, paintHead)
                                } else {
                                    val alpha = (255 * (1f - (trail / 18f))).toInt().coerceIn(20, 255)
                                    paint.alpha = alpha
                                    drawContext.canvas.nativeCanvas.drawText(char, x, y, paint)
                                }
                            }
                        }
                    }
                }

                // Exit HUD Button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x99112211)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                        .testTag("matrix_exit_button")
                ) {
                    Text(
                        "Exit CMatrix (ESC / Tap)",
                        color = TermGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun SlOverlay(onDismiss: () -> Unit) {
    var trainX by remember { mutableStateOf(1000f) }

    val trainArt = """
      ====        ________                ___________ 
  _D _|  |_______/        \__I_I_____===__|_________| 
   |(_)---  |   H\________/ _____ \   |           |   
   /     |  |   H  |  |     |   |     |           |   
  |      |  |   H  |__--------------------|_______|   
  | ________|___H__/__|_____/[][]~\_______|       |   
  |/ |   |_____I_____I_____I_____I_____I__|       |   
__(O)(O)______(O)(O)______(O)(O)______(O)(O)__________
    """.trimIndent()

    LaunchedEffect(Unit) {
        while (trainX > -900f) {
            delay(35)
            trainX -= 25f
        }
        onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .clip(RoundedCornerShape(8.dp)),
            color = ArchDarkNavy
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F141C))
                    .clickable { onDismiss() }
            ) {
                Text(
                    text = trainArt,
                    color = TermGreen,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = trainX.dp)
                )

                Text(
                    text = "🚂 Steam Locomotive in progress... tap anywhere to skip",
                    color = com.example.ui.theme.TermWhite,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp)
                )
            }
        }
    }
}
