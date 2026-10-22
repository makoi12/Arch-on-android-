package com.example.archvm.engine

import com.example.archvm.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ShellEngine(
    val vfs: VFS,
    val pacman: PacmanManager,
    val python: PythonInterpreter,
    val node: NodeInterpreter,
    val cCompiler: CCompiler,
    val httpFetcher: HttpFetcher
) {
    val bootTimeMillis = System.currentTimeMillis()

    suspend fun execute(
        commandLine: String,
        session: TerminalSession,
        emit: suspend (TerminalLine) -> Unit
    ) {
        val trimmed = commandLine.trim()
        if (trimmed.isEmpty()) return

        // Record in history
        if (session.history.isEmpty() || session.history.last() != trimmed) {
            session.history.add(trimmed)
        }

        // Chained commands: && or ;
        if (trimmed.contains("&&")) {
            val parts = trimmed.split("&&")
            for (part in parts) {
                executeSingleCommand(part.trim(), session, emit)
                if (session.lastExitCode != 0) break
            }
            return
        }
        if (trimmed.contains(";") && !trimmed.contains("echo") && !trimmed.contains("print")) {
            val parts = trimmed.split(";")
            for (part in parts) {
                executeSingleCommand(part.trim(), session, emit)
            }
            return
        }

        executeSingleCommand(trimmed, session, emit)
    }

    private suspend fun executeSingleCommand(
        cmd: String,
        session: TerminalSession,
        emit: suspend (TerminalLine) -> Unit
    ) {
        // Handle output redirection: > or >>
        var targetFile: String? = null
        var append = false
        var cleanCmd = cmd

        if (cmd.contains(">>")) {
            val idx = cmd.indexOf(">>")
            cleanCmd = cmd.substring(0, idx).trim()
            targetFile = cmd.substring(idx + 2).trim()
            append = true
        } else if (cmd.contains(">") && !cmd.contains("=>")) {
            val idx = cmd.indexOf(">")
            cleanCmd = cmd.substring(0, idx).trim()
            targetFile = cmd.substring(idx + 1).trim()
            append = false
        }

        // Handle pipe: cmd1 | cmd2
        if (cleanCmd.contains("|") && !cleanCmd.contains("||")) {
            handlePipedCommand(cleanCmd, session, emit)
            return
        }

        val collectedOutput = mutableListOf<TerminalLine>()
        val collector: suspend (TerminalLine) -> Unit = { line ->
            if (targetFile != null) {
                collectedOutput.add(line)
            } else {
                emit(line)
            }
        }

        val tokens = tokenize(expandEnvVars(cleanCmd, session.env))
        if (tokens.isEmpty()) return

        val binary = tokens[0]
        val args = tokens.drop(1)

        runBinary(binary, args, session, collector)

        if (targetFile != null) {
            val textContent = collectedOutput.joinToString("\n") { it.rawText }
            val existing = if (append) vfs.getFile(targetFile, session.cwd)?.content.orEmpty() + "\n" else ""
            vfs.createFile(targetFile, existing + textContent, session.cwd)
        }
    }

    private suspend fun runBinary(
        binary: String,
        args: List<String>,
        session: TerminalSession,
        emit: suspend (TerminalLine) -> Unit
    ) {
        when (binary) {
            "help" -> showHelp(emit)
            "man" -> showMan(args.firstOrNull(), emit)
            "clear" -> {
                session.lines.clear()
            }
            "pwd" -> emit(TerminalLine.simple(session.cwd, TermBrightWhite))
            "whoami" -> emit(TerminalLine.simple(session.user, TermGreen))
            "id" -> emit(TerminalLine.simple("uid=1000(${session.user}) gid=1000(${session.user}) groups=1000(${session.user}),998(wheel),108(vboxsf)"))
            "date" -> {
                val format = SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US)
                emit(TerminalLine.simple(format.format(Date()), TermBrightWhite))
            }
            "uptime" -> {
                val upSec = (System.currentTimeMillis() - bootTimeMillis) / 1000
                val hours = upSec / 3600
                val mins = (upSec % 3600) / 60
                val secs = upSec % 60
                emit(TerminalLine.simple(" ${SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())} up ${hours}h ${mins}m ${secs}s, 1 user, load average: 0.14, 0.08, 0.03", TermBrightWhite))
            }
            "uname" -> {
                if (args.contains("-a") || args.contains("-r")) {
                    emit(TerminalLine.simple("Linux ${session.host} 6.11.4-arch1-1-SMP PREEMPT_DYNAMIC #1 SMP Sat Oct 12 14:02:15 UTC 2026 x86_64 GNU/Linux", TermBrightWhite))
                } else {
                    emit(TerminalLine.simple("Linux", TermBrightWhite))
                }
            }
            "neofetch", "fastfetch" -> renderNeofetch(session, emit)
            "ls", "ll" -> handleLs(binary, args, session, emit)
            "cd" -> handleCd(args.firstOrNull(), session, emit)
            "cat" -> handleCat(args, session, emit)
            "mkdir" -> handleMkdir(args, session, emit)
            "rm" -> handleRm(args, session, emit)
            "touch" -> handleTouch(args, session, emit)
            "cp" -> handleCp(args, session, emit)
            "mv" -> handleMv(args, session, emit)
            "echo" -> emit(TerminalLine.simple(args.joinToString(" "), TermBrightWhite))
            "grep" -> handleGrep(args, session, emit)
            "head" -> handleHead(args, session, emit)
            "tail" -> handleTail(args, session, emit)
            "wc" -> handleWc(args, session, emit)
            "tree" -> handleTree(args.firstOrNull() ?: session.cwd, session, emit)
            "which" -> {
                val target = args.firstOrNull() ?: ""
                val found = vfs.getFile("/usr/bin/$target") != null || vfs.getFile("/bin/$target") != null
                if (found) emit(TerminalLine.simple("/usr/bin/$target", TermGreen))
                else emit(TerminalLine.error("which: no $target in (${session.env["PATH"]})"))
            }
            "free" -> renderFree(args, emit)
            "df" -> renderDf(args, emit)
            "ps" -> renderPs(args, emit)
            "kill" -> {
                val pid = args.firstOrNull() ?: ""
                emit(TerminalLine.simple("Process $pid terminated.", TermYellow))
            }
            "history" -> {
                session.history.forEachIndexed { i, h ->
                    emit(TerminalLine.simple("  ${i + 1}  $h"))
                }
            }
            "export" -> handleExport(args, session, emit)
            "env" -> {
                session.env.forEach { (k, v) ->
                    emit(TerminalLine.simple("$k=$v"))
                }
            }
            "su" -> {
                session.user = "root"
                session.title = "root@arch: ${session.cwd}"
                emit(TerminalLine.system("Switched to root session. Type 'su user' to exit."))
            }
            "sudo" -> {
                if (args.isEmpty()) {
                    emit(TerminalLine.error("usage: sudo command"))
                } else {
                    runBinary(args[0], args.drop(1), session, emit)
                }
            }
            "pacman" -> handlePacman(args, session, emit)
            "python", "python3" -> handlePython(args, session, emit)
            "node" -> handleNode(args, session, emit)
            "gcc", "clang" -> handleGcc(args, session, emit)
            "rustc", "cargo" -> handleRust(binary, args, session, emit)
            "git" -> handleGit(args, session, emit)
            "curl" -> handleCurl(args, emit)
            "wget" -> handleWget(args, session, emit)
            "nano", "vim" -> handleNano(args.firstOrNull(), session, emit)
            "htop", "top" -> {
                session.activeMode = ActiveMode.HTOP
            }
            "cmatrix" -> {
                session.activeMode = ActiveMode.CMATRIX
            }
            "sl" -> {
                session.activeMode = ActiveMode.SL
            }
            "cowsay" -> {
                val msg = if (args.isNotEmpty()) args.joinToString(" ") else "Arch Linux rocks on Android!"
                renderCowsay(msg, emit)
            }
            "fortune" -> renderFortune(emit)
            "figlet" -> {
                val msg = if (args.isNotEmpty()) args.joinToString(" ") else "ARCH"
                renderFiglet(msg, emit)
            }
            else -> {
                // Check if it's an executable file in cwd (e.g. ./a.out or ./script.sh)
                if (binary.startsWith("./") || binary.startsWith("/")) {
                    val filePath = if (binary.startsWith("./")) binary.removePrefix("./") else binary
                    val file = vfs.getFile(filePath, session.cwd)
                    if (file != null) {
                        if (file.content.startsWith("#!ELF-x86_64")) {
                            val output = cCompiler.executeBinary(file.name, session.cwd)
                            output.forEach { emit(it) }
                            return
                        } else if (file.name.endsWith(".sh") || file.content.startsWith("#!/bin/bash") || file.content.startsWith("#!/bin/sh")) {
                            executeScriptFile(file.content, session, emit)
                            return
                        }
                    }
                }
                session.lastExitCode = 127
                emit(TerminalLine.error("bash: $binary: command not found (hint: try 'pacman -S $binary' or 'help')"))
            }
        }
    }

    private suspend fun handlePipedCommand(
        cmd: String,
        session: TerminalSession,
        emit: suspend (TerminalLine) -> Unit
    ) {
        val parts = cmd.split("|").map { it.trim() }
        var currentLines = mutableListOf<TerminalLine>()

        for (i in parts.indices) {
            val part = parts[i]
            val nextLines = mutableListOf<TerminalLine>()
            val tokens = tokenize(part)
            if (tokens.isEmpty()) continue
            val b = tokens[0]
            val a = tokens.drop(1)

            if (i == 0) {
                runBinary(b, a, session) { nextLines.add(it) }
            } else {
                if (b == "grep") {
                    val query = a.firstOrNull() ?: ""
                    for (l in currentLines) {
                        if (l.rawText.contains(query, ignoreCase = true)) {
                            nextLines.add(l)
                        }
                    }
                } else if (b == "head") {
                    val count = a.getOrNull(1)?.toIntOrNull() ?: 10
                    nextLines.addAll(currentLines.take(count))
                } else if (b == "tail") {
                    val count = a.getOrNull(1)?.toIntOrNull() ?: 10
                    nextLines.addAll(currentLines.takeLast(count))
                } else if (b == "wc") {
                    nextLines.add(TerminalLine.simple("  ${currentLines.size} lines in stream", TermBrightWhite))
                } else {
                    runBinary(b, a, session) { nextLines.add(it) }
                }
            }
            currentLines = nextLines
        }
        currentLines.forEach { emit(it) }
    }

    private suspend fun handleLs(bin: String, args: List<String>, session: TerminalSession, emit: suspend (TerminalLine) -> Unit) {
        val showAll = args.any { it.contains("a") }
        val showLong = args.any { it.contains("l") } || bin == "ll"
        val targetPath = args.lastOrNull { !it.startsWith("-") } ?: session.cwd

        val nodes = vfs.list(targetPath, session.cwd)
        if (nodes == null) {
            emit(TerminalLine.error("ls: cannot access '$targetPath': No such file or directory"))
            return
        }

        val filtered = if (showAll) nodes else nodes.filter { !it.name.startsWith(".") }

        if (showLong) {
            emit(TerminalLine.simple("total ${filtered.size * 4}K", TermWhite))
            val sdf = SimpleDateFormat("MMM dd HH:mm", Locale.US)
            for (node in filtered) {
                val isDir = node is DirNode
                val typeChar = if (isDir) "d" else "-"
                val color = if (isDir) ArchCyan else if ((node as? FileNode)?.isExecutable == true) TermGreen else TermBrightWhite
                val sizeStr = if (isDir) "4096" else node.size.toString()
                val dateStr = sdf.format(Date(node.modifiedTime))

                emit(TerminalLine(
                    segments = listOf(
                        ColoredSegment("$typeChar${node.permissions} ", ArchBorder),
                        ColoredSegment("1 ${node.owner} ${node.group} ", TermWhite),
                        ColoredSegment("${sizeStr.padStart(6)} ", TermYellow),
                        ColoredSegment("$dateStr ", TermWhite),
                        ColoredSegment(node.name, color, isBold = isDir)
                    )
                ))
            }
        } else {
            // Horizontal columns
            val segments = mutableListOf<ColoredSegment>()
            for (node in filtered) {
                val isDir = node is DirNode
                val isExec = (node as? FileNode)?.isExecutable == true
                val color = if (isDir) ArchCyan else if (isExec) TermGreen else TermBrightWhite
                segments.add(ColoredSegment("${node.name}  ", color, isBold = isDir))
            }
            emit(TerminalLine(segments = segments))
        }
    }

    private suspend fun handleCd(path: String?, session: TerminalSession, emit: suspend (TerminalLine) -> Unit) {
        val target = path ?: "/home/user"
        val norm = vfs.normalize(target, session.cwd)
        val dir = vfs.getDir(norm)
        if (dir != null) {
            session.cwd = norm
            val display = if (norm.startsWith("/home/user")) "~" + norm.removePrefix("/home/user") else norm
            session.title = "${session.user}@arch: $display"
        } else {
            emit(TerminalLine.error("bash: cd: $target: No such file or directory"))
        }
    }

    private suspend fun handleCat(args: List<String>, session: TerminalSession, emit: suspend (TerminalLine) -> Unit) {
        if (args.isEmpty()) {
            emit(TerminalLine.error("cat: missing operand"))
            return
        }
        for (arg in args) {
            val file = vfs.getFile(arg, session.cwd)
            if (file != null) {
                file.content.lines().forEach { line ->
                    emit(TerminalLine.simple(line, TermBrightWhite))
                }
            } else {
                emit(TerminalLine.error("cat: $arg: No such file or directory"))
            }
        }
    }

    private suspend fun handleMkdir(args: List<String>, session: TerminalSession, emit: suspend (TerminalLine) -> Unit) {
        val path = args.lastOrNull { !it.startsWith("-") }
        if (path == null) {
            emit(TerminalLine.error("mkdir: missing operand"))
            return
        }
        if (!vfs.createDir(path, session.cwd)) {
            emit(TerminalLine.error("mkdir: cannot create directory '$path': File exists or invalid path"))
        }
    }

    private suspend fun handleRm(args: List<String>, session: TerminalSession, emit: suspend (TerminalLine) -> Unit) {
        val recursive = args.any { it.contains("r") || it.contains("f") }
        val targets = args.filter { !it.startsWith("-") }
        for (target in targets) {
            if (!vfs.remove(target, session.cwd, recursive = recursive)) {
                emit(TerminalLine.error("rm: cannot remove '$target': No such file or directory or is a directory"))
            }
        }
    }

    private suspend fun handleTouch(args: List<String>, session: TerminalSession, emit: suspend (TerminalLine) -> Unit) {
        for (arg in args.filter { !it.startsWith("-") }) {
            val existing = vfs.getFile(arg, session.cwd)
            if (existing != null) {
                existing.modifiedTime = System.currentTimeMillis()
            } else {
                vfs.createFile(arg, "", session.cwd)
            }
        }
    }

    private suspend fun handleCp(args: List<String>, session: TerminalSession, emit: suspend (TerminalLine) -> Unit) {
        if (args.size < 2) {
            emit(TerminalLine.error("cp: missing destination file operand"))
            return
        }
        if (!vfs.copy(args[0], args[1], session.cwd)) {
            emit(TerminalLine.error("cp: cannot copy '${args[0]}' to '${args[1]}'"))
        }
    }

    private suspend fun handleMv(args: List<String>, session: TerminalSession, emit: suspend (TerminalLine) -> Unit) {
        if (args.size < 2) {
            emit(TerminalLine.error("mv: missing destination file operand"))
            return
        }
        if (!vfs.move(args[0], args[1], session.cwd)) {
            emit(TerminalLine.error("mv: cannot move '${args[0]}' to '${args[1]}'"))
        }
    }

    private suspend fun handleGrep(args: List<String>, session: TerminalSession, emit: suspend (TerminalLine) -> Unit) {
        val query = args.firstOrNull { !it.startsWith("-") } ?: return
        val target = args.lastOrNull { it != query && !it.startsWith("-") } ?: return
        val file = vfs.getFile(target, session.cwd)
        if (file == null) {
            emit(TerminalLine.error("grep: $target: No such file or directory"))
            return
        }
        for (line in file.content.lines()) {
            if (line.contains(query, ignoreCase = true)) {
                emit(TerminalLine.simple(line, TermYellow))
            }
        }
    }

    private suspend fun handleHead(args: List<String>, session: TerminalSession, emit: suspend (TerminalLine) -> Unit) {
        val target = args.lastOrNull { !it.startsWith("-") } ?: return
        val file = vfs.getFile(target, session.cwd) ?: run {
            emit(TerminalLine.error("head: cannot open '$target' for reading"))
            return
        }
        file.content.lines().take(10).forEach { emit(TerminalLine.simple(it)) }
    }

    private suspend fun handleTail(args: List<String>, session: TerminalSession, emit: suspend (TerminalLine) -> Unit) {
        val target = args.lastOrNull { !it.startsWith("-") } ?: return
        val file = vfs.getFile(target, session.cwd) ?: run {
            emit(TerminalLine.error("tail: cannot open '$target' for reading"))
            return
        }
        file.content.lines().takeLast(10).forEach { emit(TerminalLine.simple(it)) }
    }

    private suspend fun handleWc(args: List<String>, session: TerminalSession, emit: suspend (TerminalLine) -> Unit) {
        val target = args.lastOrNull { !it.startsWith("-") } ?: return
        val file = vfs.getFile(target, session.cwd) ?: run {
            emit(TerminalLine.error("wc: $target: No such file or directory"))
            return
        }
        val lines = file.content.lines().size
        val words = file.content.split(Regex("\\s+")).filter { it.isNotEmpty() }.size
        val bytes = file.size
        emit(TerminalLine.simple("  $lines  $words  $bytes $target", TermBrightWhite))
    }

    private suspend fun handleTree(path: String, session: TerminalSession, emit: suspend (TerminalLine) -> Unit) {
        val startDir = vfs.getDir(path, session.cwd) ?: run {
            emit(TerminalLine.error("tree: $path: No such directory"))
            return
        }
        emit(TerminalLine.simple(path, ArchCyan, isBold = true))
        var fileCount = 0
        var dirCount = 0

        suspend fun printTree(dir: DirNode, prefix: String) {
            val children = dir.children.values.sortedBy { it.name }
            for (i in children.indices) {
                val isLast = i == children.size - 1
                val branch = if (isLast) "└── " else "├── "
                val nextPrefix = prefix + if (isLast) "    " else "│   "
                val node = children[i]
                if (node is DirNode) {
                    dirCount++
                    emit(TerminalLine(segments = listOf(
                        ColoredSegment(prefix + branch, ArchBorder),
                        ColoredSegment(node.name, ArchCyan, isBold = true)
                    )))
                    printTree(node, nextPrefix)
                } else {
                    fileCount++
                    val isExec = (node as? FileNode)?.isExecutable == true
                    emit(TerminalLine(segments = listOf(
                        ColoredSegment(prefix + branch, ArchBorder),
                        ColoredSegment(node.name, if (isExec) TermGreen else TermBrightWhite)
                    )))
                }
            }
        }
        printTree(startDir, "")
        emit(TerminalLine.simple("\n$dirCount directories, $fileCount files", TermWhite))
    }

    private fun handleExport(args: List<String>, session: TerminalSession, emit: suspend (TerminalLine) -> Unit) {
        for (arg in args) {
            if (arg.contains("=")) {
                val parts = arg.split("=", limit = 2)
                session.env[parts[0]] = parts[1]
            }
        }
    }

    private suspend fun handlePacman(args: List<String>, session: TerminalSession, emit: suspend (TerminalLine) -> Unit) {
        if (args.isEmpty()) {
            emit(TerminalLine.error("error: no operation specified (use -h for help)"))
            return
        }

        when {
            args.contains("-Syu") || (args.contains("-S") && args.contains("-u")) -> {
                pacman.syncAndUpgrade(emit)
            }
            args.contains("-S") -> {
                val pkgName = args.lastOrNull { !it.startsWith("-") }
                if (pkgName == null) {
                    emit(TerminalLine.error("error: no targets specified (use -h for help)"))
                } else {
                    pacman.install(pkgName, emit)
                }
            }
            args.contains("-Ss") -> {
                val q = args.lastOrNull { !it.startsWith("-") } ?: ""
                pacman.search(q).forEach { emit(it) }
            }
            args.contains("-Q") -> {
                pacman.listInstalled().forEach { emit(it) }
            }
            args.contains("-R") -> {
                val pkgName = args.lastOrNull { !it.startsWith("-") }
                if (pkgName == null) {
                    emit(TerminalLine.error("error: no targets specified"))
                } else {
                    pacman.remove(pkgName, emit)
                }
            }
            args.contains("-Si") -> {
                val pkgName = args.lastOrNull { !it.startsWith("-") } ?: ""
                pacman.info(pkgName).forEach { emit(it) }
            }
            else -> {
                emit(TerminalLine.simple("Pacman v6.1.0 - Arch Linux package manager\nUsage: pacman {-S|-R|-Q|-Ss|-Si|-Syu} [options] [package(s)]", ArchCyan))
            }
        }
    }

    private suspend fun handlePython(args: List<String>, session: TerminalSession, emit: suspend (TerminalLine) -> Unit) {
        val script = args.firstOrNull { !it.startsWith("-") }
        if (script != null) {
            val file = vfs.getFile(script, session.cwd) ?: run {
                emit(TerminalLine.error("python3: can't open file '$script': [Errno 2] No such file or directory"))
                return
            }
            val res = python.executeScript(file.content)
            res.forEach { emit(it) }
        } else {
            session.activeMode = ActiveMode.PYTHON_REPL
            emit(TerminalLine.simple("Python 3.12.7 (main, Oct 2026, 08:30:00) [GCC 14.2.1 20240910] on linux\nType \"help\", \"copyright\", \"credits\" or \"license\" for more information.", TermBrightWhite))
        }
    }

    private suspend fun handleNode(args: List<String>, session: TerminalSession, emit: suspend (TerminalLine) -> Unit) {
        val script = args.firstOrNull { !it.startsWith("-") }
        if (script != null) {
            val file = vfs.getFile(script, session.cwd) ?: run {
                emit(TerminalLine.error("node: cannot find module '$script'"))
                return
            }
            val res = node.executeScript(file.content)
            res.forEach { emit(it) }
        } else {
            session.activeMode = ActiveMode.NODE_REPL
            emit(TerminalLine.simple("Welcome to Node.js v22.10.0.\nType \".help\" for more information.", TermBrightWhite))
        }
    }

    private suspend fun handleGcc(args: List<String>, session: TerminalSession, emit: suspend (TerminalLine) -> Unit) {
        if (args.isEmpty()) {
            emit(TerminalLine.error("gcc: fatal error: no input files\ncompilation terminated."))
            return
        }
        val sourceFile = args.firstOrNull { it.endsWith(".c") || it.endsWith(".cpp") }
        if (sourceFile == null) {
            emit(TerminalLine.error("gcc: error: no C/C++ source file found in arguments"))
            return
        }

        val outIdx = args.indexOf("-o")
        val outName = if (outIdx != -1 && outIdx + 1 < args.size) args[outIdx + 1] else "a.out"

        val result = cCompiler.compile(sourceFile, outName, session.cwd)
        result.outputLines.forEach { emit(it) }
        if (result.success) {
            emit(TerminalLine.success("==> Binary generated: $outName (run with ./$outName)"))
        }
    }

    private suspend fun handleRust(bin: String, args: List<String>, session: TerminalSession, emit: suspend (TerminalLine) -> Unit) {
        val file = args.firstOrNull { it.endsWith(".rs") }
        if (file == null && bin == "cargo" && args.contains("run")) {
            val fibFile = vfs.getFile("/home/user/projects/fib.rs")
            if (fibFile != null) {
                emit(TerminalLine.simple("   Compiling archvm-rust v0.1.0 (/home/user/projects)", TermGreen))
                emit(TerminalLine.simple("    Finished `dev` profile [unoptimized + debuginfo] in 0.42s", TermGreen, isBold = true))
                emit(TerminalLine.simple("     Running `target/debug/archvm-rust`", TermBrightWhite))
                emit(TerminalLine.simple("🦀 Rust 1.82 on Arch Linux!\nFibonacci: 0 1 1 2 3 5 8 13 21 34 55 89", TermBrightWhite))
                return
            }
        }
        if (file != null) {
            val f = vfs.getFile(file, session.cwd)
            if (f != null) {
                emit(TerminalLine.success("==> Compiling Rust file $file..."))
                delay(200)
                emit(TerminalLine.simple("🦀 Rust 1.82 on Arch Linux!\nFibonacci: 0 1 1 2 3 5 8 13 21 34 55 89", TermBrightWhite))
            } else {
                emit(TerminalLine.error("error[E0463]: can't find crate for `$file`"))
            }
        } else {
            emit(TerminalLine.simple("rustc 1.82.0 (f6e511eec 2026-10-15)\nUsage: rustc [OPTIONS] INPUT", TermBrightWhite))
        }
    }

    private suspend fun handleGit(args: List<String>, session: TerminalSession, emit: suspend (TerminalLine) -> Unit) {
        if (args.isEmpty()) {
            emit(TerminalLine.simple("git version 2.47.0\nusage: git [--version] [--help] <command> [<args>]", TermBrightWhite))
            return
        }
        when (args[0]) {
            "status" -> {
                emit(TerminalLine.simple("On branch main\nYour branch is up to date with 'origin/main'.\n\nChanges not staged for commit:\n  (use \"git add <file>...\" to update what will be committed)\n\tmodified:   projects/hello.py\n\nno changes added to commit (use \"git add\")", TermBrightWhite))
            }
            "init" -> {
                vfs.createDir("${session.cwd}/.git")
                emit(TerminalLine.success("Initialized empty Git repository in ${session.cwd}/.git/"))
            }
            "clone" -> {
                val url = args.getOrNull(1) ?: "https://github.com/archlinux/archvm"
                emit(TerminalLine.simple("Cloning into '${url.substringAfterLast("/").removeSuffix(".git")}'..."))
                delay(300)
                emit(TerminalLine.simple("remote: Enumerating objects: 48, done.\nremote: Counting objects: 100% (48/48), done.\nremote: Compressing objects: 100% (32/32), done.\nReceiving objects: 100% (48/48), 12.42 KiB | 2.48 MiB/s, done."))
                emit(TerminalLine.success("Successfully cloned repository."))
            }
            "commit" -> {
                emit(TerminalLine.simple("[main 4a8f9c2] Updated ArchVM configurations\n 1 file changed, 8 insertions(+)", TermBrightWhite))
            }
            "log" -> {
                emit(TerminalLine.simple("4a8f9c2 (HEAD -> main, origin/main) Add starter Python & C demos", TermYellow))
                emit(TerminalLine.simple("1d3b7e4 Initial Arch Linux container VM commit", TermYellow))
            }
            else -> emit(TerminalLine.simple("git: '${args[0]}' executed successfully.", TermGreen))
        }
    }

    private suspend fun handleCurl(args: List<String>, emit: suspend (TerminalLine) -> Unit) {
        val url = args.lastOrNull { !it.startsWith("-") }
        if (url == null) {
            emit(TerminalLine.error("curl: try 'curl --help' for more information"))
            return
        }
        val includeHeaders = args.contains("-I") || args.contains("-i")
        val result = httpFetcher.fetchUrl(url, includeHeaders)
        result.onSuccess { text ->
            text.lines().forEach { emit(TerminalLine.simple(it, TermBrightWhite)) }
        }.onFailure { err ->
            emit(TerminalLine.error("curl: (6) Could not resolve host: $url (${err.message})"))
        }
    }

    private suspend fun handleWget(args: List<String>, session: TerminalSession, emit: suspend (TerminalLine) -> Unit) {
        val url = args.lastOrNull { !it.startsWith("-") } ?: return
        emit(TerminalLine.simple("--${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}--  $url"))
        emit(TerminalLine.simple("Resolving host... connected."))
        emit(TerminalLine.simple("HTTP request sent, awaiting response... 200 OK"))
        val result = httpFetcher.fetchUrl(url)
        result.onSuccess { data ->
            val fileName = url.substringAfterLast("/").ifEmpty { "index.html" }
            vfs.createFile(fileName, data, session.cwd)
            emit(TerminalLine.success("'$fileName' saved [${data.length}/${data.length}]"))
        }.onFailure {
            emit(TerminalLine.error("wget: unable to resolve host address '$url'"))
        }
    }

    private fun handleNano(file: String?, session: TerminalSession, emit: suspend (TerminalLine) -> Unit) {
        if (file == null) {
            session.editingFilePath = "${session.cwd}/new_file.txt"
            session.editingFileContent = ""
        } else {
            val resolved = vfs.normalize(file, session.cwd)
            session.editingFilePath = resolved
            val existing = vfs.getFile(resolved)
            session.editingFileContent = existing?.content ?: ""
        }
        session.activeMode = ActiveMode.NANO
    }

    private suspend fun executeScriptFile(content: String, session: TerminalSession, emit: suspend (TerminalLine) -> Unit) {
        for (line in content.lines()) {
            val t = line.trim()
            if (t.isNotEmpty() && !t.startsWith("#")) {
                executeSingleCommand(t, session, emit)
            }
        }
    }

    private suspend fun renderNeofetch(session: TerminalSession, emit: suspend (TerminalLine) -> Unit) {
        val upSec = (System.currentTimeMillis() - bootTimeMillis) / 1000
        val mins = upSec / 60
        val installedCount = pacman.packages.count { it.isInstalled }

        val infoList = listOf(
            listOf(ColoredSegment("${session.user}", TermGreen, isBold = true), ColoredSegment("@", TermWhite), ColoredSegment("${session.host}", ArchCyan, isBold = true)),
            listOf(ColoredSegment("-----------------", ArchBorder)),
            listOf(ColoredSegment("OS: ", ArchCyan, isBold = true), ColoredSegment("Arch Linux x86_64", TermBrightWhite)),
            listOf(ColoredSegment("Host: ", ArchCyan, isBold = true), ColoredSegment("ArchVM Android Container (QEMU-VFS)", TermBrightWhite)),
            listOf(ColoredSegment("Kernel: ", ArchCyan, isBold = true), ColoredSegment("6.11.4-arch1-1-SMP PREEMPT", TermBrightWhite)),
            listOf(ColoredSegment("Uptime: ", ArchCyan, isBold = true), ColoredSegment("$mins mins", TermBrightWhite)),
            listOf(ColoredSegment("Packages: ", ArchCyan, isBold = true), ColoredSegment("$installedCount (pacman)", TermBrightWhite)),
            listOf(ColoredSegment("Shell: ", ArchCyan, isBold = true), ColoredSegment("bash 5.2.37", TermBrightWhite)),
            listOf(ColoredSegment("Terminal: ", ArchCyan, isBold = true), ColoredSegment("archvm-term", TermBrightWhite)),
            listOf(ColoredSegment("CPU: ", ArchCyan, isBold = true), ColoredSegment("AMD EPYC 7763 (8) @ 2.45GHz", TermBrightWhite)),
            listOf(ColoredSegment("Memory: ", ArchCyan, isBold = true), ColoredSegment("1840MiB / 8000MiB", TermBrightWhite)),
            emptyList(),
            // 8-color test swatch
            listOf(
                ColoredSegment("███", TermBlack),
                ColoredSegment("███", TermRed),
                ColoredSegment("███", TermGreen),
                ColoredSegment("███", TermYellow),
                ColoredSegment("███", TermBlue),
                ColoredSegment("███", TermMagenta),
                ColoredSegment("███", TermCyan),
                ColoredSegment("███", TermWhite)
            )
        )

        val asciiArt = listOf(
            "       /\\         ",
            "      /  \\        ",
            "     /\\   \\       ",
            "    /      \\      ",
            "   /   ,,   \\     ",
            "  /   |  |  -\\    ",
            " /_-''    ''-_\\   "
        )

        val maxLines = maxOf(asciiArt.size, infoList.size)
        for (i in 0 until maxLines) {
            val artPart = if (i < asciiArt.size) asciiArt[i] else "                  "
            val infoPart = if (i < infoList.size) infoList[i] else emptyList()

            val combined = mutableListOf<ColoredSegment>()
            combined.add(ColoredSegment(artPart, ArchCyanBright, isBold = true))
            combined.addAll(infoPart)
            emit(TerminalLine(segments = combined, type = LineType.ASCII))
        }
    }

    private suspend fun renderFree(args: List<String>, emit: suspend (TerminalLine) -> Unit) {
        emit(TerminalLine.simple("               total        used        free      shared  buff/cache   available", TermBrightWhite, isBold = true))
        emit(TerminalLine.simple("Mem:         8000000     1842100     4657900      124000     1500000     5980000"))
        emit(TerminalLine.simple("Swap:        4096000           0     4096000"))
    }

    private suspend fun renderDf(args: List<String>, emit: suspend (TerminalLine) -> Unit) {
        emit(TerminalLine.simple("Filesystem      Size  Used Avail Use% Mounted on", TermBrightWhite, isBold = true))
        emit(TerminalLine.simple("/dev/vda1        64G   12G   49G  20% /"))
        emit(TerminalLine.simple("devtmpfs        3.9G     0  3.9G   0% /dev"))
        emit(TerminalLine.simple("tmpfs           4.0G  4.0K  4.0G   1% /tmp"))
        emit(TerminalLine.simple("/dev/vda2       512M   84M  428M  17% /boot"))
    }

    private suspend fun renderPs(args: List<String>, emit: suspend (TerminalLine) -> Unit) {
        emit(TerminalLine.simple("USER       PID %CPU %MEM    VSZ   RSS TTY      STAT START   TIME COMMAND", TermBrightWhite, isBold = true))
        emit(TerminalLine.simple("root         1  0.0  0.1 168240 12540 ?        Ss   09:00   0:01 /sbin/init"))
        emit(TerminalLine.simple("root         2  0.0  0.0      0     0 ?        S    09:00   0:00 [kthreadd]"))
        emit(TerminalLine.simple("root       210  0.0  0.2  24500  4210 ?        Ss   09:01   0:00 /usr/lib/systemd/systemd-journald"))
        emit(TerminalLine.simple("user      1042  0.2  0.4  14800  7820 pts/0    Ss   09:10   0:00 /bin/bash"))
        emit(TerminalLine.simple("user      1205  0.4  0.8 128400 15200 pts/0    S    09:12   0:00 python3"))
        emit(TerminalLine.simple("user      1340  0.1  0.2   9840  3240 pts/0    R+   09:15   0:00 ps aux"))
    }

    private suspend fun renderCowsay(msg: String, emit: suspend (TerminalLine) -> Unit) {
        val border = "-".repeat(msg.length + 2)
        emit(TerminalLine.simple(" $border", TermBrightWhite))
        emit(TerminalLine.simple("< $msg >", TermGreen, isBold = true))
        emit(TerminalLine.simple(" $border", TermBrightWhite))
        emit(TerminalLine.simple("        \\   ^__^"))
        emit(TerminalLine.simple("         \\  (oo)\\_______"))
        emit(TerminalLine.simple("            (__)\\       )\\/\\"))
        emit(TerminalLine.simple("                ||----w |"))
        emit(TerminalLine.simple("                ||     ||"))
    }

    private suspend fun renderFortune(emit: suspend (TerminalLine) -> Unit) {
        val quotes = listOf(
            "\"I use Arch, btw.\" — Every Linux enthusiast",
            "\"Simplicity is the ultimate sophistication.\" — Leonardo da Vinci",
            "\"There are 10 types of people: those who understand binary, and those who don't.\"",
            "\"Arch Linux: Keep It Simple, Stupid (KISS).\"",
            "\"Any fool can write code that a computer can understand. Good programmers write code that humans can understand.\" — Martin Fowler"
        )
        emit(TerminalLine.simple(quotes.random(), TermYellow))
    }

    private suspend fun renderFiglet(text: String, emit: suspend (TerminalLine) -> Unit) {
        emit(TerminalLine.simple("  _  _    ____   ____ _   _ ", ArchCyan, isBold = true))
        emit(TerminalLine.simple(" / \\| |  |  _ \\ / ___| | | |", ArchCyan, isBold = true))
        emit(TerminalLine.simple("/ _ \\ |  | |_) | |   | |_| |", ArchCyan, isBold = true))
        emit(TerminalLine.simple("/ ___ \\ |__|  _ <| |___|  _  |", ArchCyan, isBold = true))
        emit(TerminalLine.simple("/_/   \\_\\_____|_| \\_\\\\____|_| |_|", ArchCyan, isBold = true))
    }

    private suspend fun showHelp(emit: suspend (TerminalLine) -> Unit) {
        emit(TerminalLine.simple("==================================================", ArchCyan))
        emit(TerminalLine.simple("   ArchVM - Arch Linux Terminal Commands Cheatsheet", TermBrightWhite, isBold = true))
        emit(TerminalLine.simple("==================================================", ArchCyan))
        emit(TerminalLine.simple("Package Manager (Pacman):", TermGreen, isBold = true))
        emit(TerminalLine.simple("  pacman -S <pkg>     : Install developer package (e.g. pacman -S python)"))
        emit(TerminalLine.simple("  pacman -Syu         : Full system rolling upgrade & mirror sync"))
        emit(TerminalLine.simple("  pacman -Ss <query>  : Search packages in core/extra repositories"))
        emit(TerminalLine.simple("  pacman -Q           : List all installed packages"))
        emit(TerminalLine.simple("  pacman -R <pkg>     : Uninstall a package"))
        emit(TerminalLine.simple("Development & Code Execution:", TermGreen, isBold = true))
        emit(TerminalLine.simple("  python [file.py]    : Python 3 interactive REPL or script runner"))
        emit(TerminalLine.simple("  gcc <file.c> -o app : Compile C code and execute with ./app"))
        emit(TerminalLine.simple("  node [file.js]      : Run Node.js JavaScript environment"))
        emit(TerminalLine.simple("  cargo run / rustc   : Rust compiler demo"))
        emit(TerminalLine.simple("  nano <file>         : Full screen terminal text editor"))
        emit(TerminalLine.simple("System & Monitoring:", TermGreen, isBold = true))
        emit(TerminalLine.simple("  neofetch            : Arch Linux specs & ASCII logo"))
        emit(TerminalLine.simple("  htop                : Interactive animated process manager"))
        emit(TerminalLine.simple("  cmatrix             : Matrix hacker falling green code"))
        emit(TerminalLine.simple("  curl <url>          : Live web request (e.g. curl icanhazip.com)"))
        emit(TerminalLine.simple("  cowsay / fortune    : Fun geek terminal toys"))
        emit(TerminalLine.simple("Navigation & Files:", TermGreen, isBold = true))
        emit(TerminalLine.simple("  ls -la, cd, pwd, cat, mkdir, rm -rf, touch, cp, mv, grep, tree"))
        emit(TerminalLine.simple("Quick Keys Toolbar:", TermYellow, isBold = true))
        emit(TerminalLine.simple("  Use the TAB button for bash auto-completion, and ▲/▼ for history!"))
    }

    private suspend fun showMan(cmd: String?, emit: suspend (TerminalLine) -> Unit) {
        if (cmd == null) {
            emit(TerminalLine.error("What manual page do you want? (e.g. 'man pacman' or 'man python')"))
            return
        }
        when (cmd) {
            "pacman" -> {
                emit(TerminalLine.simple("PACMAN(8)                 Arch Linux Manual                 PACMAN(8)", TermBrightWhite, isBold = true))
                emit(TerminalLine.simple("NAME\n       pacman - package manager utility\n\nSYNOPSIS\n       pacman <operation> [options] [targets]"))
                emit(TerminalLine.simple("OPERATIONS\n       -S : Synchronize packages\n       -Q : Query the package database\n       -R : Remove package(s)\n       -U : Upgrade packages"))
            }
            "python" -> {
                emit(TerminalLine.simple("PYTHON(1)                 Linux User's Manual                 PYTHON(1)", TermBrightWhite, isBold = true))
                emit(TerminalLine.simple("NAME\n       python3 - an interpreted, interactive, object-oriented programming language"))
            }
            else -> emit(TerminalLine.simple("No manual entry for $cmd. Try 'help'.", TermYellow))
        }
    }

    private fun tokenize(cmd: String): List<String> {
        val list = mutableListOf<String>()
        var inQuote = false
        var quoteChar = ' '
        val sb = StringBuilder()
        for (c in cmd) {
            if ((c == '"' || c == '\'') && !inQuote) {
                inQuote = true
                quoteChar = c
            } else if (c == quoteChar && inQuote) {
                inQuote = false
            } else if (c.isWhitespace() && !inQuote) {
                if (sb.isNotEmpty()) {
                    list.add(sb.toString())
                    sb.clear()
                }
            } else {
                sb.append(c)
            }
        }
        if (sb.isNotEmpty()) list.add(sb.toString())
        return list
    }

    private fun expandEnvVars(cmd: String, env: Map<String, String>): String {
        var result = cmd
        env.forEach { (k, v) ->
            result = result.replace("$$k", v).replace("\${$k}", v)
        }
        return result
    }
}
