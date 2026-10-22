package com.example.archvm.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.archvm.engine.*
import com.example.archvm.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val sessions: List<TerminalSession> = emptyList(),
    val currentSessionId: String = "",
    val inputText: String = "",
    val historyIndex: Int = -1,
    val fontSizeSp: Float = 13f,
    val crtScanlines: Boolean = false,
    val isExecuting: Boolean = false,
    val showPackageBrowser: Boolean = false,
    val showFileExplorer: Boolean = false,
    val showSystemSpecs: Boolean = false,
    val showStarterScripts: Boolean = false,
    val showSettings: Boolean = false,
    val activeThemeName: String = "Arch Linux"
)

class TerminalViewModel(application: Application) : AndroidViewModel(application) {
    val vfs = VFS()
    val pacman = PacmanManager(vfs)
    val python = PythonInterpreter()
    val node = NodeInterpreter()
    val cCompiler = CCompiler(vfs)
    val httpFetcher = HttpFetcher()
    val shellEngine = ShellEngine(vfs, pacman, python, node, cCompiler, httpFetcher)

    private val prefs = application.getSharedPreferences("archvm_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vm?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    init {
        restoreState()
        if (_uiState.value.sessions.isEmpty()) {
            createNewSession("user", "/home/user")
        }
    }

    private fun restoreState() {
        val savedFiles = prefs.getString("vfs_files", null)
        if (!savedFiles.isNullOrEmpty()) {
            vfs.restoreCustomFiles(savedFiles)
        }
        val savedFont = prefs.getFloat("font_size", 13f)
        val savedCrt = prefs.getBoolean("crt_scanlines", false)
        val savedTheme = prefs.getString("theme", "Arch Linux") ?: "Arch Linux"
        _uiState.update {
            it.copy(fontSizeSp = savedFont, crtScanlines = savedCrt, activeThemeName = savedTheme)
        }
    }

    fun saveState() {
        prefs.edit()
            .putString("vfs_files", vfs.serializeCustomFiles())
            .putFloat("font_size", _uiState.value.fontSizeSp)
            .putBoolean("crt_scanlines", _uiState.value.crtScanlines)
            .putString("theme", _uiState.value.activeThemeName)
            .apply()
    }

    fun getCurrentSession(): TerminalSession? {
        val currentId = _uiState.value.currentSessionId
        return _uiState.value.sessions.find { it.id == currentId }
    }

    fun createNewSession(user: String = "user", cwd: String = "/home/user") {
        val id = "session_${System.currentTimeMillis()}"
        val displayCwd = if (cwd.startsWith("/home/user")) "~" + cwd.removePrefix("/home/user") else cwd
        val newSession = TerminalSession(
            id = id,
            title = "$user@arch: $displayCwd",
            user = user,
            cwd = cwd
        )

        // Initial welcome lines
        newSession.lines.add(
            TerminalLine.simple("Arch Linux 6.11.4-arch1-1 (tty1)", ArchCyan, isBold = true)
        )
        newSession.lines.add(
            TerminalLine.simple("archlinux login: $user (automatic login)", TermWhite)
        )
        newSession.lines.add(
            TerminalLine.simple("Last login: ${java.util.Date()} on pts/0", TermWhite)
        )
        newSession.lines.add(
            TerminalLine.simple("Type 'help' or 'neofetch' to explore your Arch VM environment.", TermGreen, isBold = true)
        )

        _uiState.update { state ->
            val updated = state.sessions + newSession
            state.copy(
                sessions = updated,
                currentSessionId = id,
                inputText = "",
                historyIndex = -1
            )
        }
    }

    fun switchSession(sessionId: String) {
        _uiState.update { it.copy(currentSessionId = sessionId, historyIndex = -1) }
    }

    fun closeSession(sessionId: String) {
        val currentSessions = _uiState.value.sessions
        if (currentSessions.size <= 1) {
            // Reset existing session instead of leaving zero sessions
            val session = currentSessions.firstOrNull() ?: return
            session.lines.clear()
            session.cwd = "/home/user"
            session.user = "user"
            session.lines.add(TerminalLine.system("Terminal session reset."))
            _uiState.update { it.copy(inputText = "") }
            return
        }

        val remaining = currentSessions.filter { it.id != sessionId }
        val nextActive = if (_uiState.value.currentSessionId == sessionId) {
            remaining.last().id
        } else {
            _uiState.value.currentSessionId
        }
        _uiState.update { it.copy(sessions = remaining, currentSessionId = nextActive) }
    }

    fun onInputTextChange(newText: String) {
        _uiState.update { it.copy(inputText = newText) }
    }

    fun triggerHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(12)
            }
        } catch (_: Exception) {}
    }

    fun executeCurrentInput() {
        triggerHaptic()
        val session = getCurrentSession() ?: return
        val rawInput = _uiState.value.inputText
        _uiState.update { it.copy(inputText = "", historyIndex = -1, isExecuting = true) }

        viewModelScope.launch {
            when (session.activeMode) {
                ActiveMode.PYTHON_REPL -> {
                    if (rawInput.trim() == "exit()" || rawInput.trim() == "quit()") {
                        session.activeMode = ActiveMode.SHELL
                        session.lines.add(TerminalLine.simple(">>> $rawInput", TermYellow))
                        session.lines.add(TerminalLine.system("Exited Python REPL."))
                    } else {
                        session.lines.add(TerminalLine.simple(">>> $rawInput", TermBrightWhite))
                        val results = python.evalLine(rawInput)
                        session.lines.addAll(results)
                    }
                }
                ActiveMode.NODE_REPL -> {
                    if (rawInput.trim() == ".exit") {
                        session.activeMode = ActiveMode.SHELL
                        session.lines.add(TerminalLine.simple("> $rawInput", TermYellow))
                        session.lines.add(TerminalLine.system("Exited Node.js REPL."))
                    } else {
                        session.lines.add(TerminalLine.simple("> $rawInput", TermBrightWhite))
                        val results = node.evalLine(rawInput)
                        session.lines.addAll(results)
                    }
                }
                else -> {
                    // Standard Shell
                    session.lines.add(
                        TerminalLine.promptLine(session.user, session.host, session.cwd, rawInput)
                    )
                    shellEngine.execute(rawInput, session) { outputLine ->
                        session.lines.add(outputLine)
                    }
                }
            }
            _uiState.update { it.copy(isExecuting = false) }
            saveState()
        }
    }

    fun handleQuickKey(key: String) {
        triggerHaptic()
        val session = getCurrentSession() ?: return
        val currentText = _uiState.value.inputText

        when (key) {
            "TAB" -> performTabCompletion(session, currentText)
            "▲" -> navigateHistory(session, -1)
            "▼" -> navigateHistory(session, 1)
            "CTRL+C" -> {
                session.activeMode = ActiveMode.SHELL
                session.lines.add(TerminalLine.simple("^C", TermRed))
                _uiState.update { it.copy(inputText = "", isExecuting = false) }
            }
            "CTRL+L" -> {
                session.lines.clear()
            }
            "CTRL+D" -> {
                if (session.activeMode != ActiveMode.SHELL) {
                    session.activeMode = ActiveMode.SHELL
                    session.lines.add(TerminalLine.system("Exited interactive mode."))
                } else if (session.user == "root") {
                    session.user = "user"
                    session.title = "user@arch: ${session.cwd}"
                    session.lines.add(TerminalLine.system("Logged out from root."))
                }
            }
            "ESC" -> {
                if (session.activeMode != ActiveMode.SHELL) {
                    session.activeMode = ActiveMode.SHELL
                }
            }
            else -> {
                _uiState.update { it.copy(inputText = currentText + key) }
            }
        }
    }

    private fun performTabCompletion(session: TerminalSession, currentInput: String) {
        val trimmed = currentInput.trimStart()
        if (trimmed.isEmpty()) return

        val tokens = trimmed.split(" ")
        if (tokens.size == 1) {
            // Auto complete command name
            val prefix = tokens[0]
            val allCommands = listOf(
                "pacman", "python", "python3", "gcc", "clang", "node", "cargo", "rustc",
                "neofetch", "fastfetch", "htop", "nano", "vim", "curl", "wget", "git",
                "cmatrix", "cowsay", "fortune", "sl", "figlet", "tree", "clear", "history",
                "uname", "whoami", "date", "uptime", "free", "df", "ps", "kill", "which",
                "ls", "cd", "pwd", "cat", "mkdir", "rm", "cp", "mv", "touch", "echo", "grep"
            )
            val matches = allCommands.filter { it.startsWith(prefix) }
            if (matches.size == 1) {
                _uiState.update { it.copy(inputText = matches[0] + " ") }
            } else if (matches.size > 1) {
                session.lines.add(TerminalLine.promptLine(session.user, session.host, session.cwd, currentInput))
                session.lines.add(TerminalLine.simple(matches.joinToString("  "), ArchCyan))
            }
        } else {
            // Auto complete file path in current dir
            val lastToken = tokens.last()
            val nodes = vfs.list(session.cwd) ?: emptyList()
            val matches = nodes.map { it.name }.filter { it.startsWith(lastToken) }
            if (matches.size == 1) {
                val completed = tokens.dropLast(1).joinToString(" ") + " " + matches[0] + (if (vfs.getDir("${session.cwd}/${matches[0]}") != null) "/" else " ")
                _uiState.update { it.copy(inputText = completed.trimStart()) }
            } else if (matches.size > 1) {
                session.lines.add(TerminalLine.promptLine(session.user, session.host, session.cwd, currentInput))
                session.lines.add(TerminalLine.simple(matches.joinToString("  "), TermYellow))
            }
        }
    }

    private fun navigateHistory(session: TerminalSession, direction: Int) {
        if (session.history.isEmpty()) return
        val currentIdx = _uiState.value.historyIndex
        val newIdx = when {
            currentIdx == -1 && direction < 0 -> session.history.size - 1
            currentIdx != -1 -> (currentIdx + direction).coerceIn(0, session.history.size - 1)
            else -> -1
        }

        if (newIdx in session.history.indices) {
            _uiState.update {
                it.copy(
                    inputText = session.history[newIdx],
                    historyIndex = newIdx
                )
            }
        }
    }

    fun saveNanoFile(content: String) {
        val session = getCurrentSession() ?: return
        val path = session.editingFilePath ?: "${session.cwd}/untitled.txt"
        vfs.createFile(path, content, session.cwd)
        session.lines.add(TerminalLine.success("Wrote ${content.lines().size} lines to $path"))
        session.activeMode = ActiveMode.SHELL
        session.editingFilePath = null
        session.editingFileContent = null
        saveState()
    }

    fun exitNano() {
        val session = getCurrentSession() ?: return
        session.activeMode = ActiveMode.SHELL
        session.editingFilePath = null
        session.editingFileContent = null
    }

    fun exitInteractiveMode() {
        val session = getCurrentSession() ?: return
        session.activeMode = ActiveMode.SHELL
    }

    fun setFontSize(delta: Float) {
        _uiState.update {
            val updated = (it.fontSizeSp + delta).coerceIn(10f, 22f)
            it.copy(fontSizeSp = updated)
        }
        saveState()
    }

    fun toggleCrt() {
        _uiState.update { it.copy(crtScanlines = !it.crtScanlines) }
        saveState()
    }

    fun setTheme(name: String) {
        _uiState.update { it.copy(activeThemeName = name) }
        saveState()
    }

    fun togglePackageBrowser(show: Boolean) {
        _uiState.update { it.copy(showPackageBrowser = show) }
    }

    fun toggleFileExplorer(show: Boolean) {
        _uiState.update { it.copy(showFileExplorer = show) }
    }

    fun toggleSystemSpecs(show: Boolean) {
        _uiState.update { it.copy(showSystemSpecs = show) }
    }

    fun toggleStarterScripts(show: Boolean) {
        _uiState.update { it.copy(showStarterScripts = show) }
    }

    fun toggleSettings(show: Boolean) {
        _uiState.update { it.copy(showSettings = show) }
    }

    fun runStarterScript(command: String) {
        _uiState.update { it.copy(showStarterScripts = false, inputText = command) }
        executeCurrentInput()
    }
}
