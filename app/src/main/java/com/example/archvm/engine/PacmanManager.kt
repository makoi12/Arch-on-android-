package com.example.archvm.engine

import com.example.archvm.model.ArchPackage
import com.example.archvm.model.ArchPackageRegistry
import com.example.archvm.model.ColoredSegment
import com.example.archvm.model.LineType
import com.example.archvm.model.TerminalLine
import com.example.archvm.model.VFS
import com.example.ui.theme.*
import kotlinx.coroutines.delay

class PacmanManager(private val vfs: VFS) {
    val packages: MutableList<ArchPackage> = ArchPackageRegistry.getInitialCatalog()

    fun findPackage(name: String): ArchPackage? {
        return packages.find { it.name.equals(name, ignoreCase = true) }
    }

    suspend fun install(pkgName: String, emit: suspend (TerminalLine) -> Unit): Boolean {
        val pkg = findPackage(pkgName)
        if (pkg == null) {
            emit(TerminalLine.error("error: target not found: $pkgName"))
            return false
        }

        if (pkg.isInstalled) {
            emit(TerminalLine.simple("warning: ${pkg.name}-${pkg.version} is up to date -- reinstalling", TermYellow))
        }

        emit(TerminalLine.simple("resolving dependencies...", TermWhite))
        delay(200)
        emit(TerminalLine.simple("looking for conflicting packages...", TermWhite))
        delay(150)
        emit(TerminalLine(
            segments = listOf(
                ColoredSegment("Packages (1) ", TermBrightWhite, isBold = true),
                ColoredSegment("${pkg.name}-${pkg.version} ", ArchCyan),
                ColoredSegment("(${pkg.sizeMb} MiB)", TermWhite)
            )
        ))
        emit(TerminalLine.simple("Total Download Size:    ${pkg.sizeMb} MiB"))
        emit(TerminalLine.simple("Total Installed Size:   ${"%.2f".format(pkg.sizeMb * 3.2)} MiB"))
        delay(250)

        emit(TerminalLine.simple(":: Proceed with installation? [Y/n] Y", TermBrightWhite, isBold = true))
        delay(200)
        emit(TerminalLine(
            segments = listOf(
                ColoredSegment(":: Retrieving packages...\n", TermWhite),
                ColoredSegment(" ${pkg.name}-${pkg.version}  ", TermBrightWhite),
                ColoredSegment("[####################################] 100% ", TermGreen, isBold = true),
                ColoredSegment("${pkg.sizeMb} MiB", TermWhite)
            )
        ))
        delay(350)
        emit(TerminalLine.simple("(1/1) checking keys in keyring... [####################################] 100%"))
        delay(150)
        emit(TerminalLine.simple("(1/1) checking package integrity... [####################################] 100%"))
        delay(150)
        emit(TerminalLine.simple("(1/1) loading package files... [####################################] 100%"))
        delay(150)
        emit(TerminalLine.simple("(1/1) checking for file conflicts... [####################################] 100%"))
        delay(150)
        emit(TerminalLine.simple(":: Processing package changes..."))
        delay(250)
        emit(TerminalLine(
            segments = listOf(
                ColoredSegment("(1/1) installing ${pkg.name}... ", TermWhite),
                ColoredSegment("[####################################] 100%", TermGreen, isBold = true)
            )
        ))
        delay(250)
        emit(TerminalLine.simple(":: Running post-transaction hooks..."))
        emit(TerminalLine.simple("(1/2) Arming ConditionNeedsUpdate..."))
        emit(TerminalLine.simple("(2/2) Updating man-db and desktop file MIME database..."))

        pkg.isInstalled = true
        // Populate binary into /usr/bin/
        for (bin in pkg.binaries) {
            vfs.createFile("/usr/bin/$bin", "#!/bin/bash\n# Arch Linux binary stub for $bin\n", isExecutable = true)
        }

        vfs.createFile(
            "/var/log/pacman.log",
            vfs.getFile("/var/log/pacman.log")?.content.orEmpty() +
                "\n[${System.currentTimeMillis()}] [ALPM] installed ${pkg.name} (${pkg.version})"
        )

        emit(TerminalLine.success("==> Installation of ${pkg.name} completed successfully."))
        return true
    }

    suspend fun syncAndUpgrade(emit: suspend (TerminalLine) -> Unit) {
        emit(TerminalLine.simple(":: Synchronizing package databases...", TermBrightWhite, isBold = true))
        delay(250)
        emit(TerminalLine(
            segments = listOf(
                ColoredSegment(" core  ", ArchCyan, isBold = true),
                ColoredSegment("[####################################] 100% ", TermGreen),
                ColoredSegment("148.2 KiB", TermWhite)
            )
        ))
        delay(200)
        emit(TerminalLine(
            segments = listOf(
                ColoredSegment(" extra ", ArchCyan, isBold = true),
                ColoredSegment("[####################################] 100% ", TermGreen),
                ColoredSegment("8.4 MiB", TermWhite)
            )
        ))
        delay(200)
        emit(TerminalLine(
            segments = listOf(
                ColoredSegment(" multilib ", ArchCyan, isBold = true),
                ColoredSegment("[####################################] 100% ", TermGreen),
                ColoredSegment("172.0 KiB", TermWhite)
            )
        ))
        delay(300)
        emit(TerminalLine.simple(":: Starting full system rolling upgrade...", TermBrightWhite, isBold = true))
        delay(350)
        emit(TerminalLine.simple("resolving dependencies...", TermWhite))
        delay(200)
        emit(TerminalLine.simple("looking for conflicting packages...", TermWhite))
        delay(200)
        emit(TerminalLine.success(":: Total installed packages: ${packages.count { it.isInstalled }}"))
        emit(TerminalLine.success(":: There is nothing to do -- Arch Linux VM is rolling and up to date."))
    }

    suspend fun remove(pkgName: String, emit: suspend (TerminalLine) -> Unit): Boolean {
        val pkg = findPackage(pkgName)
        if (pkg == null || !pkg.isInstalled) {
            emit(TerminalLine.error("error: target not found: $pkgName"))
            return false
        }

        if (pkg.name == "pacman" || pkg.name == "bash" || pkg.name == "coreutils") {
            emit(TerminalLine.error("error: failed to prepare transaction (could not satisfy dependencies)\n:: $pkgName: removing break critical system dependencies"))
            return false
        }

        emit(TerminalLine.simple("checking dependencies...", TermWhite))
        delay(200)
        emit(TerminalLine.simple(":: Do you want to remove these packages? [Y/n] Y", TermBrightWhite))
        delay(250)
        emit(TerminalLine.simple("(1/1) removing ${pkg.name}... [####################################] 100%"))
        delay(200)
        emit(TerminalLine.simple(":: Running post-transaction hooks..."))

        pkg.isInstalled = false
        for (bin in pkg.binaries) {
            vfs.remove("/usr/bin/$bin")
        }
        emit(TerminalLine.success("Package ${pkg.name} removed."))
        return true
    }

    fun search(query: String): List<TerminalLine> {
        val results = packages.filter {
            it.name.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true)
        }
        if (results.isEmpty()) {
            return listOf(TerminalLine.simple("No matching packages found for '$query'.", TermYellow))
        }

        val lines = mutableListOf<TerminalLine>()
        for (pkg in results) {
            val installedBadge = if (pkg.isInstalled) " [installed]" else ""
            lines.add(
                TerminalLine(
                    segments = listOf(
                        ColoredSegment("${pkg.repo}/", TermMagenta, isBold = true),
                        ColoredSegment(pkg.name, ArchCyan, isBold = true),
                        ColoredSegment(" ${pkg.version}", TermWhite),
                        ColoredSegment(installedBadge, TermGreen, isBold = true)
                    )
                )
            )
            lines.add(TerminalLine.simple("    ${pkg.description}", TermWhite))
        }
        return lines
    }

    fun listInstalled(): List<TerminalLine> {
        val installed = packages.filter { it.isInstalled }
        val lines = mutableListOf<TerminalLine>()
        for (pkg in installed) {
            lines.add(
                TerminalLine(
                    segments = listOf(
                        ColoredSegment(pkg.name, ArchCyan, isBold = true),
                        ColoredSegment(" ${pkg.version}", TermWhite),
                        ColoredSegment(" (${pkg.category})", TermYellow)
                    )
                )
            )
        }
        lines.add(TerminalLine.simple("Total: ${installed.size} packages installed", TermGreen))
        return lines
    }

    fun info(pkgName: String): List<TerminalLine> {
        val pkg = findPackage(pkgName) ?: return listOf(TerminalLine.error("error: package '$pkgName' was not found"))
        val installedStatus = if (pkg.isInstalled) "Yes [${pkg.version}]" else "No"
        return listOf(
            TerminalLine.simple("Repository      : ${pkg.repo}", ArchCyan),
            TerminalLine.simple("Name            : ${pkg.name}", TermBrightWhite, isBold = true),
            TerminalLine.simple("Version         : ${pkg.version}"),
            TerminalLine.simple("Description     : ${pkg.description}"),
            TerminalLine.simple("Architecture    : x86_64"),
            TerminalLine.simple("URL             : ${pkg.url}", TermBlue),
            TerminalLine.simple("Licenses        : GPL-3.0 / MIT / Apache-2.0"),
            TerminalLine.simple("Category        : ${pkg.category}"),
            TerminalLine.simple("Installed Size  : ${"%.2f".format(pkg.sizeMb * 3.2)} MiB"),
            TerminalLine.simple("Installed       : $installedStatus", if (pkg.isInstalled) TermGreen else TermYellow),
            TerminalLine.simple("Binaries        : ${pkg.binaries.joinToString(", ")}", TermYellow)
        )
    }
}
