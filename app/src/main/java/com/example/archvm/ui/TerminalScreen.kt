package com.example.archvm.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.archvm.model.*
import com.example.ui.theme.*

@Composable
fun TerminalScreen(
    viewModel: TerminalViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val session = viewModel.getCurrentSession()
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

    // Auto-scroll to bottom on new terminal lines
    LaunchedEffect(session?.lines?.size) {
        val count = session?.lines?.size ?: 0
        if (count > 0) {
            listState.animateScrollToItem(count - 1)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        containerColor = ArchDarkNavy,
        topBar = {
            TopTerminalBar(
                uiState = uiState,
                onSwitchSession = { viewModel.switchSession(it) },
                onNewSession = { viewModel.createNewSession() },
                onCloseSession = { viewModel.closeSession(it) },
                onOpenPackages = { viewModel.togglePackageBrowser(true) },
                onOpenFiles = { viewModel.toggleFileExplorer(true) },
                onOpenSpecs = { viewModel.toggleSystemSpecs(true) },
                onOpenStarters = { viewModel.toggleStarterScripts(true) },
                onOpenSettings = { viewModel.toggleSettings(true) }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .background(ArchSurface)
            ) {
                // Interactive Command Prompt Input Row
                PromptInputRow(
                    session = session,
                    inputText = uiState.inputText,
                    onInputChange = { viewModel.onInputTextChange(it) },
                    onExecute = { viewModel.executeCurrentInput() },
                    focusRequester = focusRequester,
                    fontSizeSp = uiState.fontSizeSp
                )

                // Quick Keys Toolbar (Tab, Ctrl, Esc, History, etc.)
                TerminalKeybar(
                    onKeyClick = { viewModel.handleQuickKey(it) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ArchTerminalBg)
                .clickable {
                    // Tap on terminal focuses input
                    try { focusRequester.requestFocus() } catch (_: Exception) {}
                }
        ) {
            // Terminal Output Stream
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (session != null) {
                    items(session.lines) { line ->
                        TerminalLineItem(line = line, fontSizeSp = uiState.fontSizeSp)
                    }
                }
            }

            // Vintage CRT scanlines overlay
            if (uiState.crtScanlines) {
                CrtScanlinesOverlay()
            }
        }
    }

    // Modal Overlays
    if (session?.activeMode == ActiveMode.NANO) {
        val targetPath = session.editingFilePath ?: "${session.cwd}/untitled.txt"
        val content = session.editingFileContent ?: ""
        NanoEditorDialog(
            filePath = targetPath,
            initialContent = content,
            onSave = { viewModel.saveNanoFile(it) },
            onExit = { viewModel.exitNano() }
        )
    }

    if (session?.activeMode == ActiveMode.HTOP) {
        HtopOverlay(onQuit = { viewModel.exitInteractiveMode() })
    }

    if (session?.activeMode == ActiveMode.CMATRIX) {
        MatrixOverlay(onDismiss = { viewModel.exitInteractiveMode() })
    }

    if (session?.activeMode == ActiveMode.SL) {
        SlOverlay(onDismiss = { viewModel.exitInteractiveMode() })
    }

    if (uiState.showPackageBrowser) {
        PackageBrowserDialog(
            packages = viewModel.pacman.packages,
            onInstallPackage = { pkg ->
                viewModel.executeCurrentInput()
                viewModel.handleQuickKey("") // triggers haptic
                viewModel.onInputTextChange("pacman -S $pkg")
                viewModel.executeCurrentInput()
            },
            onRemovePackage = { pkg ->
                viewModel.onInputTextChange("pacman -R $pkg")
                viewModel.executeCurrentInput()
            },
            onSystemUpgrade = {
                viewModel.onInputTextChange("pacman -Syu")
                viewModel.executeCurrentInput()
            },
            onDismiss = { viewModel.togglePackageBrowser(false) }
        )
    }

    if (uiState.showFileExplorer) {
        FileExplorerDialog(
            vfs = viewModel.vfs,
            currentDir = session?.cwd ?: "/home/user",
            onOpenFileInNano = { path ->
                viewModel.onInputTextChange("nano $path")
                viewModel.executeCurrentInput()
            },
            onNavigateTerminal = { path ->
                viewModel.onInputTextChange("cd $path")
                viewModel.executeCurrentInput()
            },
            onDismiss = { viewModel.toggleFileExplorer(false) }
        )
    }

    if (uiState.showSystemSpecs) {
        SystemSpecsDialog(
            onRunDiagnostic = { cmd ->
                viewModel.onInputTextChange(cmd)
                viewModel.executeCurrentInput()
            },
            onDismiss = { viewModel.toggleSystemSpecs(false) }
        )
    }

    if (uiState.showStarterScripts) {
        StarterScriptsDialog(
            onRunCommand = { cmd ->
                viewModel.runStarterScript(cmd)
            },
            onDismiss = { viewModel.toggleStarterScripts(false) }
        )
    }

    if (uiState.showSettings) {
        SettingsDialog(
            fontSize = uiState.fontSizeSp,
            crtEnabled = uiState.crtScanlines,
            activeTheme = uiState.activeThemeName,
            onFontSizeChange = { viewModel.setFontSize(it) },
            onToggleCrt = { viewModel.toggleCrt() },
            onSelectTheme = { viewModel.setTheme(it) },
            onResetTerminal = {
                session?.let { viewModel.closeSession(it.id) }
                viewModel.createNewSession()
            },
            onDismiss = { viewModel.toggleSettings(false) }
        )
    }
}

@Composable
private fun TopTerminalBar(
    uiState: UiState,
    onSwitchSession: (String) -> Unit,
    onNewSession: () -> Unit,
    onCloseSession: (String) -> Unit,
    onOpenPackages: () -> Unit,
    onOpenFiles: () -> Unit,
    onOpenSpecs: () -> Unit,
    onOpenStarters: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ArchDarkNavy)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        // App title & actions row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "ArchVM",
                    color = ArchCyan,
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(ArchSurfaceHighlight)
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text("x86_64", color = TermGreen, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }

            // Quick Tools Bar
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onOpenPackages,
                    modifier = Modifier.size(36.dp).testTag("top_btn_packages")
                ) {
                    Icon(Icons.Default.Download, contentDescription = "Pacman Packages", tint = ArchCyan, modifier = Modifier.size(19.dp))
                }
                IconButton(
                    onClick = onOpenFiles,
                    modifier = Modifier.size(36.dp).testTag("top_btn_files")
                ) {
                    Icon(Icons.Default.Folder, contentDescription = "VFS File Manager", tint = TermYellow, modifier = Modifier.size(19.dp))
                }
                IconButton(
                    onClick = onOpenStarters,
                    modifier = Modifier.size(36.dp).testTag("top_btn_starters")
                ) {
                    Icon(Icons.Default.Code, contentDescription = "Starter Scripts", tint = TermGreen, modifier = Modifier.size(19.dp))
                }
                IconButton(
                    onClick = onOpenSpecs,
                    modifier = Modifier.size(36.dp).testTag("top_btn_specs")
                ) {
                    Icon(Icons.Default.Computer, contentDescription = "System Specs", tint = TermBlue, modifier = Modifier.size(19.dp))
                }
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(36.dp).testTag("top_btn_settings")
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TermWhite, modifier = Modifier.size(19.dp))
                }
            }
        }

        // Multi-Session Tabs Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (sess in uiState.sessions) {
                val isActive = sess.id == uiState.currentSessionId
                val bgColor = if (isActive) ArchSurfaceHighlight else ArchSurface
                val textColor = if (isActive) ArchCyanBright else TermWhite

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(bgColor)
                        .clickable { onSwitchSession(sess.id) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = sess.title,
                        color = textColor,
                        fontSize = 11.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close tab",
                        tint = TermWhite.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(12.dp)
                            .clickable { onCloseSession(sess.id) }
                    )
                }
            }

            // New Tab button
            IconButton(
                onClick = onNewSession,
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(ArchSurface)
                    .testTag("btn_new_tab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Tab", tint = ArchCyan, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun PromptInputRow(
    session: TerminalSession?,
    inputText: String,
    onInputChange: (String) -> Unit,
    onExecute: () -> Unit,
    focusRequester: FocusRequester,
    fontSizeSp: Float
) {
    val promptPrefix = when (session?.activeMode) {
        ActiveMode.PYTHON_REPL -> ">>> "
        ActiveMode.NODE_REPL -> "> "
        else -> {
            val user = session?.user ?: "user"
            val symbol = if (user == "root") "#" else "$"
            val displayCwd = if (session?.cwd?.startsWith("/home/user") == true) {
                "~" + session.cwd.removePrefix("/home/user")
            } else session?.cwd ?: "/"
            "[$user@arch $displayCwd]$symbol "
        }
    }

    val prefixColor = when (session?.activeMode) {
        ActiveMode.PYTHON_REPL -> TermYellow
        ActiveMode.NODE_REPL -> TermGreen
        else -> if (session?.user == "root") TermRed else ArchCyan
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ArchSurface)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = promptPrefix,
            color = prefixColor,
            fontFamily = FontFamily.Monospace,
            fontSize = fontSizeSp.sp,
            fontWeight = FontWeight.Bold
        )

        BasicTextField(
            value = inputText,
            onValueChange = onInputChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .testTag("terminal_input_field"),
            textStyle = TextStyle(
                color = TermBrightWhite,
                fontFamily = FontFamily.Monospace,
                fontSize = fontSizeSp.sp
            ),
            cursorBrush = SolidColor(ArchCyanBright),
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Send,
                autoCorrectEnabled = false
            ),
            keyboardActions = KeyboardActions(
                onSend = { onExecute() }
            )
        )

        if (inputText.isNotEmpty()) {
            IconButton(
                onClick = { onInputChange("") },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TermWhite.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
            }
        }

        IconButton(
            onClick = onExecute,
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(ArchCyan)
                .testTag("terminal_send_button")
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Run", tint = Color.Black, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun TerminalLineItem(line: TerminalLine, fontSizeSp: Float) {
    val annotated = remember(line) {
        buildAnnotatedString {
            for (segment in line.segments) {
                withStyle(
                    SpanStyle(
                        color = segment.color,
                        fontWeight = if (segment.isBold) FontWeight.Bold else FontWeight.Normal
                    )
                ) {
                    append(segment.text)
                }
            }
        }
    }

    Text(
        text = annotated,
        fontFamily = FontFamily.Monospace,
        fontSize = fontSizeSp.sp,
        lineHeight = (fontSizeSp + 4).sp
    )
}

@Composable
private fun CrtScanlinesOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val lineSpacing = 4f
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = Color.Black.copy(alpha = 0.18f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.5f
            )
            y += lineSpacing
        }
    }
}
