package com.example.archvm.engine

import com.example.archvm.model.ColoredSegment
import com.example.archvm.model.LineType
import com.example.archvm.model.TerminalLine
import com.example.ui.theme.TermBrightWhite
import com.example.ui.theme.TermGreen
import com.example.ui.theme.TermRed
import com.example.ui.theme.TermYellow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.*

class PythonInterpreter {
    private val globalVars = mutableMapOf<String, Any>(
        "pi" to Math.PI,
        "e" to Math.E,
        "__name__" to "__main__"
    )

    fun evalLine(line: String): List<TerminalLine> {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("#")) return emptyList()

        if (trimmed == "quit()" || trimmed == "exit()") {
            return listOf(TerminalLine.simple("Use Ctrl+D or type 'exit()' to return to shell.", TermYellow))
        }

        if (trimmed == "help()" || trimmed == "help") {
            return listOf(
                TerminalLine.simple("ArchVM Python 3.12 Interactive Environment", TermGreen, isBold = true),
                TerminalLine.simple("Available built-ins: print(), len(), type(), range(), abs(), round(), math functions."),
                TerminalLine.simple("Assign variables: x = 42, text = 'Arch'")
            )
        }

        try {
            // Check for assignment: var = value
            val assignMatch = Regex("""^([a-zA-Z_][a-zA-Z0-9_]*)\s*=\s*(.+)$""").find(trimmed)
            if (assignMatch != null) {
                val varName = assignMatch.groupValues[1]
                val expression = assignMatch.groupValues[2]
                val value = evaluateExpression(expression)
                globalVars[varName] = value
                return emptyList()
            }

            // Check for print statement: print(...)
            val printMatch = Regex("""^print\s*\((.*)\)$""", RegexOption.DOT_MATCHES_ALL).find(trimmed)
            if (printMatch != null) {
                val inside = printMatch.groupValues[1]
                val output = parsePrintArgs(inside)
                return listOf(TerminalLine.simple(output, TermBrightWhite))
            }

            // Direct expression evaluation (e.g. 5 * 10 or x)
            val result = evaluateExpression(trimmed)
            return listOf(TerminalLine.simple(result.toString(), TermBrightWhite))
        } catch (e: Exception) {
            return listOf(TerminalLine.error("Traceback (most recent call last):\n  File \"<stdin>\", line 1\nSyntaxError: ${e.message ?: "invalid syntax"}"))
        }
    }

    fun executeScript(scriptContent: String): List<TerminalLine> {
        val outputLines = mutableListOf<TerminalLine>()
        val lines = scriptContent.lines()
        var inLoop = false
        var loopVar = ""
        var loopRange = 0..0
        val loopBody = mutableListOf<String>()

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) continue

            // Simple for loop: for x in range(N):
            val forMatch = Regex("""^for\s+([a-zA-Z_][a-zA-Z0-9_]*)\s+in\s+range\((?:(\d+)\s*,\s*)?(\d+)\):""").find(line)
            if (forMatch != null) {
                inLoop = true
                loopVar = forMatch.groupValues[1]
                val start = if (forMatch.groupValues[2].isNotEmpty()) forMatch.groupValues[2].toInt() else 0
                val end = forMatch.groupValues[3].toInt()
                loopRange = start until end
                loopBody.clear()
                continue
            }

            if (inLoop) {
                if (rawLine.startsWith("    ") || rawLine.startsWith("\t")) {
                    loopBody.add(rawLine.trim())
                    continue
                } else {
                    // Execute accumulated loop
                    for (i in loopRange) {
                        globalVars[loopVar] = i
                        for (bodyLine in loopBody) {
                            val res = evalLine(bodyLine)
                            outputLines.addAll(res)
                        }
                    }
                    inLoop = false
                }
            }

            val result = evalLine(line)
            outputLines.addAll(result)
        }

        if (inLoop) {
            for (i in loopRange) {
                globalVars[loopVar] = i
                for (bodyLine in loopBody) {
                    val res = evalLine(bodyLine)
                    outputLines.addAll(res)
                }
            }
        }

        return outputLines
    }

    private fun parsePrintArgs(argsStr: String): String {
        var str = argsStr.trim()
        if (str.isEmpty()) return ""

        // Handle formatted string f"..." or f'...'
        if ((str.startsWith("f\"") && str.endsWith("\"")) || (str.startsWith("f'") && str.endsWith("'"))) {
            var raw = str.substring(2, str.length - 1)
            val regex = Regex("""\{([^}]+)\}""")
            return regex.replace(raw) { match ->
                val expr = match.groupValues[1].trim()
                evaluateExpression(expr).toString()
            }
        }

        // Handle regular string literal "..." or '...'
        if ((str.startsWith("\"") && str.endsWith("\"")) || (str.startsWith("'") && str.endsWith("'"))) {
            return str.substring(1, str.length - 1)
                .replace("\\n", "\n")
                .replace("\\t", "    ")
        }

        // Handle comma-separated print arguments
        val parts = splitArgs(str)
        return parts.joinToString(" ") { part ->
            evaluateExpression(part).toString()
        }
    }

    private fun splitArgs(text: String): List<String> {
        val result = mutableListOf<String>()
        var depth = 0
        var inQuote = false
        var current = StringBuilder()
        for (ch in text) {
            if (ch == '"' || ch == '\'') inQuote = !inQuote
            if (!inQuote) {
                if (ch == '(' || ch == '[' || ch == '{') depth++
                if (ch == ')' || ch == ']' || ch == '}') depth--
                if (ch == ',' && depth == 0) {
                    result.add(current.toString().trim())
                    current = StringBuilder()
                    continue
                }
            }
            current.append(ch)
        }
        if (current.isNotEmpty()) result.add(current.toString().trim())
        return result
    }

    private fun evaluateExpression(expr: String): Any {
        val trimmed = expr.trim()
        if (trimmed == "True") return true
        if (trimmed == "False") return false
        if (trimmed == "None") return "None"
        if (trimmed == "sys.platform") return "linux (archlinux)"

        // String literal
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            return trimmed.substring(1, trimmed.length - 1)
        }

        // Integer or Double
        trimmed.toLongOrNull()?.let { return it }
        trimmed.toDoubleOrNull()?.let { return it }

        // Variable lookup
        if (globalVars.containsKey(trimmed)) {
            return globalVars[trimmed]!!
        }

        // Math functions (e.g. math.sqrt(16) or math.isqrt(x))
        if (trimmed.startsWith("math.sqrt(") && trimmed.endsWith(")")) {
            val inner = trimmed.removePrefix("math.sqrt(").removeSuffix(")")
            val v = (evaluateExpression(inner) as? Number)?.toDouble() ?: 0.0
            return sqrt(v)
        }
        if (trimmed.startsWith("len(") && trimmed.endsWith(")")) {
            val inner = trimmed.removePrefix("len(").removeSuffix(")")
            val v = evaluateExpression(inner)
            return when (v) {
                is String -> v.length
                is List<*> -> v.size
                else -> 0
            }
        }

        // Basic arithmetic binary operations: + - * / % **
        if (trimmed.contains("**")) {
            val parts = trimmed.split("**", limit = 2)
            val base = (evaluateExpression(parts[0]) as Number).toDouble()
            val exp = (evaluateExpression(parts[1]) as Number).toDouble()
            return base.pow(exp).toLong()
        }
        if (trimmed.contains("+")) {
            val parts = trimmed.split("+", limit = 2)
            val left = evaluateExpression(parts[0])
            val right = evaluateExpression(parts[1])
            if (left is String || right is String) {
                return left.toString() + right.toString()
            }
            if (left is Number && right is Number) {
                return left.toDouble() + right.toDouble()
            }
        }
        if (trimmed.contains("-") && !trimmed.startsWith("-")) {
            val parts = trimmed.split("-", limit = 2)
            val left = (evaluateExpression(parts[0]) as Number).toDouble()
            val right = (evaluateExpression(parts[1]) as Number).toDouble()
            return left - right
        }
        if (trimmed.contains("*")) {
            val parts = trimmed.split("*", limit = 2)
            val left = (evaluateExpression(parts[0]) as Number).toDouble()
            val right = (evaluateExpression(parts[1]) as Number).toDouble()
            return left * right
        }
        if (trimmed.contains("/")) {
            val parts = trimmed.split("/", limit = 2)
            val left = (evaluateExpression(parts[0]) as Number).toDouble()
            val right = (evaluateExpression(parts[1]) as Number).toDouble()
            return if (right != 0.0) left / right else "ZeroDivisionError"
        }

        return trimmed
    }
}
