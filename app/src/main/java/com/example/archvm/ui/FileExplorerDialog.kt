package com.example.archvm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.archvm.model.DirNode
import com.example.archvm.model.FileNode
import com.example.archvm.model.VFS
import com.example.ui.theme.*

@Composable
fun FileExplorerDialog(
    vfs: VFS,
    currentDir: String,
    onOpenFileInNano: (String) -> Unit,
    onNavigateTerminal: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var workingPath by remember { mutableStateOf(currentDir) }
    var refreshKey by remember { mutableStateOf(0) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }
    var isCreatingFolder by remember { mutableStateOf(false) }

    val nodes = remember(workingPath, refreshKey) {
        vfs.list(workingPath) ?: emptyList()
    }

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
                    .padding(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Folder, contentDescription = "VFS", tint = ArchCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Linux File System (VFS)",
                            color = TermBrightWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("file_explorer_close")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TermWhite)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Path navigation bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ArchSurface)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (workingPath != "/") {
                                val parent = workingPath.substringBeforeLast("/").ifEmpty { "/" }
                                workingPath = parent
                            }
                        },
                        enabled = workingPath != "/",
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("btn_parent_dir")
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Up", tint = if (workingPath != "/") ArchCyan else ArchBorder)
                    }

                    IconButton(
                        onClick = { workingPath = "/home/user" },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("btn_home_dir")
                    ) {
                        Icon(Icons.Default.Home, contentDescription = "Home", tint = TermGreen)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = workingPath,
                        color = ArchCyanBright,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )

                    // cd here in terminal
                    TextButton(
                        onClick = {
                            onNavigateTerminal(workingPath)
                            onDismiss()
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("cd here", color = TermGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action Bar (New File / New Dir)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            isCreatingFolder = false
                            showNewFileDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ArchSurfaceHighlight),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("btn_new_file")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "New File", tint = ArchCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ New File", color = TermBrightWhite, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }

                    Button(
                        onClick = {
                            isCreatingFolder = true
                            showNewFileDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ArchSurfaceHighlight),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("btn_new_folder")
                    ) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder", tint = TermGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ New Folder", color = TermBrightWhite, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // File/Dir List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (nodes.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("(Empty directory)", color = ArchBorder, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                            }
                        }
                    }

                    items(nodes) { node ->
                        val isDir = node is DirNode
                        val fileNode = node as? FileNode
                        val icon = when {
                            isDir -> Icons.Default.Folder
                            node.name.endsWith(".py") -> Icons.Default.Code
                            node.name.endsWith(".c") || node.name.endsWith(".cpp") -> Icons.Default.Terminal
                            node.name.endsWith(".sh") -> Icons.Default.PlayArrow
                            fileNode?.isExecutable == true -> Icons.Default.PlayCircle
                            else -> Icons.Default.Description
                        }

                        val iconColor = when {
                            isDir -> ArchCyan
                            node.name.endsWith(".py") -> TermYellow
                            node.name.endsWith(".c") -> TermBlue
                            node.name.endsWith(".sh") || fileNode?.isExecutable == true -> TermGreen
                            else -> TermWhite
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val fullPath = if (workingPath == "/") "/${node.name}" else "$workingPath/${node.name}"
                                    if (isDir) {
                                        workingPath = fullPath
                                    } else {
                                        onOpenFileInNano(fullPath)
                                        onDismiss()
                                    }
                                }
                                .testTag("file_item_${node.name}"),
                            colors = CardDefaults.cardColors(containerColor = ArchSurface),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = node.name,
                                            color = if (isDir) ArchCyanBright else TermBrightWhite,
                                            fontWeight = if (isDir) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "${node.permissions}  ${if (isDir) "dir" else "${node.size}B"}",
                                            color = TermWhite,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                if (!isDir) {
                                    IconButton(
                                        onClick = {
                                            val fullPath = if (workingPath == "/") "/${node.name}" else "$workingPath/${node.name}"
                                            onOpenFileInNano(fullPath)
                                            onDismiss()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit in nano", tint = ArchCyan, modifier = Modifier.size(16.dp))
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        val fullPath = if (workingPath == "/") "/${node.name}" else "$workingPath/${node.name}"
                                        vfs.remove(fullPath, recursive = true)
                                        refreshKey++
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TermRed, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewFileDialog) {
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            title = {
                Text(
                    text = if (isCreatingFolder) "Create New Directory" else "Create New File",
                    color = TermBrightWhite,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp
                )
            },
            text = {
                OutlinedTextField(
                    value = newFileName,
                    onValueChange = { newFileName = it },
                    placeholder = { Text(if (isCreatingFolder) "my_folder" else "script.py", color = ArchBorder) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TermBrightWhite,
                        unfocusedTextColor = TermWhite,
                        focusedBorderColor = ArchCyan,
                        unfocusedBorderColor = ArchBorder
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newFileName.trim()
                        if (name.isNotEmpty()) {
                            val path = if (workingPath == "/") "/$name" else "$workingPath/$name"
                            if (isCreatingFolder) {
                                vfs.createDir(path)
                            } else {
                                vfs.createFile(path, "")
                            }
                            newFileName = ""
                            showNewFileDialog = false
                            refreshKey++
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ArchCyan)
                ) {
                    Text("Create", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFileDialog = false }) {
                    Text("Cancel", color = TermWhite)
                }
            },
            containerColor = ArchSurface
        )
    }
}
