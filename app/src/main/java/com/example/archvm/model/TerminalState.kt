package com.example.archvm.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

enum class LineType {
    COMMAND,
    OUTPUT,
    ERROR,
    SYSTEM,
    SUCCESS,
    ASCII,
    PROMPT
}

enum class ActiveMode {
    SHELL,
    PYTHON_REPL,
    NODE_REPL,
    HTOP,
    NANO,
    CMATRIX,
    SL
}

data class ColoredSegment(
    val text: String,
    val color: Color = TermWhite,
    val isBold: Boolean = false
)

data class TerminalLine(
    val segments: List<ColoredSegment>,
    val type: LineType = LineType.OUTPUT,
    val timestamp: Long = System.currentTimeMillis()
) {
    val rawText: String
        get() = segments.joinToString("") { it.text }

    companion object {
        fun simple(text: String, color: Color = TermWhite, isBold: Boolean = false, type: LineType = LineType.OUTPUT): TerminalLine {
            return TerminalLine(
                segments = listOf(ColoredSegment(text = text, color = color, isBold = isBold)),
                type = type
            )
        }

        fun error(text: String): TerminalLine {
            return TerminalLine(
                segments = listOf(ColoredSegment(text = text, color = TermRed, isBold = true)),
                type = LineType.ERROR
            )
        }

        fun success(text: String): TerminalLine {
            return TerminalLine(
                segments = listOf(ColoredSegment(text = text, color = TermGreen, isBold = false)),
                type = LineType.SUCCESS
            )
        }

        fun system(text: String): TerminalLine {
            return TerminalLine(
                segments = listOf(ColoredSegment(text = text, color = ArchCyan, isBold = true)),
                type = LineType.SYSTEM
            )
        }

        fun promptLine(user: String, host: String, cwd: String, command: String): TerminalLine {
            val displayCwd = if (cwd.startsWith("/home/user")) {
                "~" + cwd.removePrefix("/home/user")
            } else cwd
            val promptSymbol = if (user == "root") "#" else "$"
            val userColor = if (user == "root") TermRed else TermGreen

            return TerminalLine(
                segments = listOf(
                    ColoredSegment("[", ArchBorder),
                    ColoredSegment(user, userColor, isBold = true),
                    ColoredSegment("@", TermWhite),
                    ColoredSegment(host, ArchCyan, isBold = true),
                    ColoredSegment(" ", TermWhite),
                    ColoredSegment(displayCwd, TermBlue, isBold = true),
                    ColoredSegment("]$promptSymbol ", TermPrompt, isBold = true),
                    ColoredSegment(command, TermBrightWhite, isBold = false)
                ),
                type = LineType.COMMAND
            )
        }
    }
}

data class TerminalSession(
    val id: String,
    var title: String = "arch: ~",
    var user: String = "user",
    var host: String = "archlinux",
    var cwd: String = "/home/user",
    val env: MutableMap<String, String> = mutableMapOf(
        "USER" to "user",
        "HOME" to "/home/user",
        "SHELL" to "/bin/bash",
        "TERM" to "xterm-256color",
        "PATH" to "/usr/local/bin:/usr/bin:/bin",
        "ARCH" to "x86_64",
        "LANG" to "en_US.UTF-8"
    ),
    val history: MutableList<String> = mutableListOf(),
    val lines: MutableList<TerminalLine> = mutableListOf(),
    var activeMode: ActiveMode = ActiveMode.SHELL,
    var editingFilePath: String? = null,
    var editingFileContent: String? = null,
    var lastExitCode: Int = 0
)
