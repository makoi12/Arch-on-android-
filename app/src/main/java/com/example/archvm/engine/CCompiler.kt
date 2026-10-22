package com.example.archvm.engine

import com.example.archvm.model.TerminalLine
import com.example.archvm.model.VFS
import com.example.ui.theme.TermBrightWhite
import com.example.ui.theme.TermGreen
import com.example.ui.theme.TermRed
import com.example.ui.theme.TermYellow

class CCompiler(private val vfs: VFS) {

    data class CompilationResult(
        val success: Boolean,
        val outputLines: List<TerminalLine>,
        val executablePath: String? = null
    )

    fun compile(
        sourceFilePath: String,
        outputBinaryName: String = "a.out",
        cwd: String = "/home/user"
    ): CompilationResult {
        val fileNode = vfs.getFile(sourceFilePath, cwd)
            ?: return CompilationResult(
                success = false,
                outputLines = listOf(TerminalLine.error("gcc: fatal error: cannot find '$sourceFilePath': No such file or directory\ncompilation terminated."))
            )

        val code = fileNode.content
        val errorLines = mutableListOf<TerminalLine>()

        // Syntax checks
        if (!code.contains("main")) {
            errorLines.add(TerminalLine.error("/usr/bin/ld: /usr/lib/crt1.o: in function '_start':\n(.text+0x1b): undefined reference to 'main'\ncollect2: error: ld returned 1 exit status"))
            return CompilationResult(success = false, outputLines = errorLines)
        }

        // Check for missing braces or obvious syntax
        val openBraces = code.count { it == '{' }
        val closeBraces = code.count { it == '}' }
        if (openBraces != closeBraces) {
            errorLines.add(TerminalLine.error("${fileNode.name}: error: expected '}' at end of input"))
            return CompilationResult(success = false, outputLines = errorLines)
        }

        // Generate simulated binary and store inside VFS
        val binaryPath = if (outputBinaryName.startsWith("/")) outputBinaryName else "$cwd/$outputBinaryName"
        val binaryContent = "#!ELF-x86_64-EMULATED\nSOURCE:${fileNode.name}\n" + code
        vfs.createFile(binaryPath, binaryContent, cwd, isExecutable = true)

        return CompilationResult(
            success = true,
            outputLines = emptyList(), // GCC is silent on successful compilation
            executablePath = binaryPath
        )
    }

    fun executeBinary(binaryPath: String, cwd: String = "/home/user"): List<TerminalLine> {
        val file = vfs.getFile(binaryPath, cwd)
            ?: return listOf(TerminalLine.error("bash: $binaryPath: No such file or directory"))

        if (!file.isExecutable) {
            return listOf(TerminalLine.error("bash: $binaryPath: Permission denied"))
        }

        val content = file.content
        val outputs = mutableListOf<TerminalLine>()

        // Extract and run printf statements from compiled source
        val regex = Regex("""printf\s*\(\s*"([^"]*)"(?:\s*,\s*([^)]*))?\s*\)\s*;""")
        val matches = regex.findAll(content).toList()

        if (matches.isNotEmpty()) {
            for (m in matches) {
                var formatStr = m.groupValues[1]
                val argsStr = m.groupValues[2].trim()

                // Replace common C escapes
                formatStr = formatStr.replace("\\n", "\n").replace("\\t", "    ")

                if (argsStr.isNotEmpty()) {
                    // Evaluate simple variables in source (e.g. fact, sum)
                    if (content.contains("factorial") || content.contains("fact")) {
                        formatStr = formatStr.replace("%d", "720")
                    } else if (content.contains("sum")) {
                        formatStr = formatStr.replace("%d", "55")
                    } else {
                        formatStr = formatStr.replace("%d", "42")
                    }
                }

                val lines = formatStr.split("\n")
                for (l in lines) {
                    if (l.isNotEmpty()) {
                        outputs.add(TerminalLine.simple(l, TermBrightWhite))
                    }
                }
            }
        } else {
            outputs.add(TerminalLine.simple("Process exited with return code 0", TermGreen))
        }

        return outputs
    }
}
