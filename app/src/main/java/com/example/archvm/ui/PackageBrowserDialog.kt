package com.example.archvm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
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
import com.example.archvm.model.ArchPackage
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun PackageBrowserDialog(
    packages: List<ArchPackage>,
    onInstallPackage: (String) -> Unit,
    onRemovePackage: (String) -> Unit,
    onSystemUpgrade: () -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Programming", "System", "Utilities", "Shell & CLI", "Fun & ASCII")

    val filtered = packages.filter { pkg ->
        val matchesSearch = pkg.name.contains(searchQuery, ignoreCase = true) ||
                pkg.description.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == "All" || pkg.category == selectedCategory
        matchesSearch && matchesCategory
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
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Pacman Package Manager",
                                color = ArchCyan,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            "Arch Linux Official Repositories (core / extra)",
                            color = TermWhite,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("package_browser_close")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TermWhite)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search & Upgrade Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("package_search_input"),
                        placeholder = {
                            Text("Search packages (e.g. python, gcc, rust)...", fontSize = 12.sp, color = ArchBorder)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = ArchCyan)
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ArchCyan,
                            unfocusedBorderColor = ArchBorder,
                            focusedTextColor = TermBrightWhite,
                            unfocusedTextColor = TermWhite,
                            focusedContainerColor = ArchSurface,
                            unfocusedContainerColor = ArchSurface
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            onSystemUpgrade()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ArchCyan),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                        modifier = Modifier.testTag("pacman_syu_button")
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = "Sync", tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("pacman -Syu", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Categories
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        val isSelected = cat == selectedCategory
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ArchSurfaceHighlight,
                                selectedLabelColor = ArchCyanBright,
                                containerColor = ArchSurface,
                                labelColor = TermWhite
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) ArchCyan else ArchBorder
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Packages List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered) { pkg ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pkg_card_${pkg.name}"),
                            colors = CardDefaults.cardColors(containerColor = ArchSurface),
                            shape = RoundedCornerShape(8.dp),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(ArchBorder))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = pkg.name,
                                            color = TermBrightWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = pkg.version,
                                            color = TermWhite,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (pkg.repo == "core") Color(0xFF1E3A2F) else ArchSurfaceHighlight)
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = pkg.repo,
                                                color = if (pkg.repo == "core") TermGreen else ArchCyan,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = pkg.description,
                                        color = TermWhite,
                                        fontSize = 11.sp,
                                        maxLines = 2
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Size: ${pkg.sizeMb} MiB  |  Binaries: ${pkg.binaries.joinToString(", ")}",
                                        color = ArchCyan,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                if (pkg.isInstalled) {
                                    Button(
                                        onClick = {
                                            onRemovePackage(pkg.name)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ArchSurfaceHighlight),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("pkg_btn_${pkg.name}")
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = "Installed", tint = TermGreen, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Installed", color = TermGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            onInstallPackage(pkg.name)
                                            onDismiss()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ArchCyan),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("pkg_btn_${pkg.name}")
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = "Install", tint = Color.Black, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Install", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
