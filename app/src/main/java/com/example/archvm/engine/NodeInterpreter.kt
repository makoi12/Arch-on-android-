package com.example.archvm.engine

import com.example.archvm.model.LineType
import com.example.archvm.model.TerminalLine
import com.example.ui.theme.TermBrightWhite
import com.example.ui.theme.TermGreen
import com.example.ui.theme.TermYellow

class NodeInterpreter {
    private val jsVars = mutableMapOf<String, Any>()

    fun evalLine(line: String): List<TerminalLine> {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("//")) return emptyList()

        if (trimmed == ".exit") {
            return listOf(TerminalLine.simple("Exited Node.js REPL", TermYellow))
        }

        // console.log(...)
        val logMatch = Regex("""^console\.log\s*\((.*)\);?$""").find(trimmed)
        if (logMatch != null) {
            val arg = logMatch.groupValues[1].trim()
            val evaluated = evalArg(arg)
            return listOf(TerminalLine.simple(evaluated, TermBrightWhite))
        }

        // Variable assignment: const/let/var x = ... or x = ...
        val varMatch = Regex("""^(?:const|let|var)?\s*([a-zA-Z_$][a-zA-Z0-9_$]*)\s*=\s*(.+);?$""").find(trimmed)
        if (varMatch != null) {
            val name = varMatch.groupValues[1]
            val valueExpr = varMatch.groupValues[2]
            val value = evalArg(valueExpr)
            jsVars[name] = value
            return emptyList()
        }

        return listOf(TerminalLine.simple(evalArg(trimmed), TermBrightWhite))
    }

    fun executeScript(content: String): List<TerminalLine> {
        val output = mutableListOf<TerminalLine>()
        for (rawLine in content.lines()) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("//")) continue
            val lines = evalLine(line)
            output.addAll(lines)
        }
        return output
    }

    private fun evalArg(expr: String): String {
        val trimmed = expr.trim().removeSuffix(";")
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) return trimmed.substring(1, trimmed.length - 1)
        if (trimmed.startsWith("'") && trimmed.endsWith("'")) return trimmed.substring(1, trimmed.length - 1)
        if (trimmed.startsWith("`") && trimmed.endsWith("`")) return trimmed.substring(1, trimmed.length - 1)

        if (jsVars.containsKey(trimmed)) return jsVars[trimmed].toString()

        if (trimmed.startsWith("JSON.stringify")) {
            return "{\n  \"os\": \"Arch Linux\",\n  \"kernel\": \"6.11.4\",\n  \"arch\": \"x86_64\",\n  \"status\": \"running\"\n}"
        }

        // Simple arithmetic
        if (trimmed.contains("+")) {
            val parts = trimmed.split("+")
            val sum = parts.sumOf { it.trim().toDoubleOrNull() ?: 0.0 }
            if (sum % 1.0 == 0.0) return sum.toLong().toString()
            return sum.toString()
        }
        return trimmed
    }
}
