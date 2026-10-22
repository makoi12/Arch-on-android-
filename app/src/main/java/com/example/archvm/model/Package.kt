package com.example.archvm.model

data class ArchPackage(
    val name: String,
    val version: String,
    val repo: String,
    val description: String,
    val sizeMb: Double,
    val dependencies: List<String> = emptyList(),
    var isInstalled: Boolean = false,
    val binaries: List<String> = emptyList(),
    val category: String = "Utilities",
    val url: String = "https://archlinux.org/packages/$repo/x86_64/$name/"
)

object ArchPackageRegistry {
    fun getInitialCatalog(): MutableList<ArchPackage> = mutableListOf(
        ArchPackage(
            name = "bash",
            version = "5.2.37-1",
            repo = "core",
            description = "The GNU Bourne Again shell (standard Linux command language)",
            sizeMb = 9.2,
            isInstalled = true,
            binaries = listOf("bash", "sh"),
            category = "Shell & CLI"
        ),
        ArchPackage(
            name = "coreutils",
            version = "9.5-1",
            repo = "core",
            description = "The basic file, shell and text manipulation utilities of the GNU operating system",
            sizeMb = 18.4,
            isInstalled = true,
            binaries = listOf("ls", "cat", "mkdir", "rm", "cp", "mv", "touch", "echo", "pwd", "date", "uname", "whoami"),
            category = "System"
        ),
        ArchPackage(
            name = "pacman",
            version = "6.1.0-3",
            repo = "core",
            description = "A library-based package manager with dependency support",
            sizeMb = 4.8,
            isInstalled = true,
            binaries = listOf("pacman"),
            category = "System"
        ),
        ArchPackage(
            name = "python",
            version = "3.12.7-1",
            repo = "extra",
            description = "Next generation of the python high-level scripting language with interactive REPL",
            sizeMb = 68.4,
            isInstalled = true,
            binaries = listOf("python", "python3", "pip"),
            category = "Programming"
        ),
        ArchPackage(
            name = "gcc",
            version = "14.2.1-3",
            repo = "extra",
            description = "The GNU Compiler Collection - C and C++ frontends and execution",
            sizeMb = 185.2,
            isInstalled = true,
            binaries = listOf("gcc", "g++", "cpp"),
            category = "Programming"
        ),
        ArchPackage(
            name = "nodejs",
            version = "22.10.0-1",
            repo = "extra",
            description = "Event-driven I/O server-side JavaScript environment based on V8 engine",
            sizeMb = 34.1,
            isInstalled = true,
            binaries = listOf("node", "npm", "npx"),
            category = "Programming"
        ),
        ArchPackage(
            name = "rust",
            version = "1.82.0-1",
            repo = "extra",
            description = "Empowering everyone to build reliable and efficient software with Cargo build system",
            sizeMb = 92.5,
            isInstalled = true,
            binaries = listOf("rustc", "cargo"),
            category = "Programming"
        ),
        ArchPackage(
            name = "git",
            version = "2.47.0-1",
            repo = "extra",
            description = "Fast, scalable, distributed revision control system with commit graph",
            sizeMb = 32.8,
            isInstalled = true,
            binaries = listOf("git"),
            category = "Programming"
        ),
        ArchPackage(
            name = "neofetch",
            version = "7.1.0-2",
            repo = "extra",
            description = "CLI system information tool with iconic Arch Linux ASCII art banner",
            sizeMb = 0.4,
            isInstalled = true,
            binaries = listOf("neofetch"),
            category = "Utilities"
        ),
        ArchPackage(
            name = "fastfetch",
            version = "2.28.0-1",
            repo = "extra",
            description = "Fast, highly customizable system information tool written in C",
            sizeMb = 1.8,
            isInstalled = true,
            binaries = listOf("fastfetch"),
            category = "Utilities"
        ),
        ArchPackage(
            name = "htop",
            version = "3.3.0-1",
            repo = "extra",
            description = "Interactive process viewer and system resource monitor with CPU gauges",
            sizeMb = 1.2,
            isInstalled = true,
            binaries = listOf("htop", "top"),
            category = "System"
        ),
        ArchPackage(
            name = "nano",
            version = "8.2-1",
            repo = "core",
            description = "Pico editor clone with enhanced syntax highlighting and shortcut bar",
            sizeMb = 2.5,
            isInstalled = true,
            binaries = listOf("nano"),
            category = "Utilities"
        ),
        ArchPackage(
            name = "vim",
            version = "9.1.0784-1",
            repo = "extra",
            description = "Vi Improved - powerful modal command-line text editor",
            sizeMb = 16.4,
            isInstalled = false,
            binaries = listOf("vim", "vi"),
            category = "Utilities"
        ),
        ArchPackage(
            name = "curl",
            version = "8.10.1-1",
            repo = "core",
            description = "Command line tool for transferring data with URLs using HTTP/HTTPS",
            sizeMb = 2.1,
            isInstalled = true,
            binaries = listOf("curl"),
            category = "Utilities"
        ),
        ArchPackage(
            name = "wget",
            version = "1.25.0-1",
            repo = "extra",
            description = "Network utility to retrieve files from the Web using HTTP and FTP",
            sizeMb = 3.6,
            isInstalled = true,
            binaries = listOf("wget"),
            category = "Utilities"
        ),
        ArchPackage(
            name = "tree",
            version = "2.1.3-1",
            repo = "extra",
            description = "A recursive directory indentation listing program",
            sizeMb = 0.3,
            isInstalled = true,
            binaries = listOf("tree"),
            category = "Utilities"
        ),
        ArchPackage(
            name = "cmatrix",
            version = "2.0-2",
            repo = "extra",
            description = "Simulates the glowing cascading digital rain from The Matrix",
            sizeMb = 0.2,
            isInstalled = true,
            binaries = listOf("cmatrix"),
            category = "Fun & ASCII"
        ),
        ArchPackage(
            name = "cowsay",
            version = "3.7.0-2",
            repo = "extra",
            description = "Configurable talking cow ASCII art speech bubble generator",
            sizeMb = 0.1,
            isInstalled = true,
            binaries = listOf("cowsay"),
            category = "Fun & ASCII"
        ),
        ArchPackage(
            name = "fortune",
            version = "1.99.4-7",
            repo = "extra",
            description = "Displays a random, hopefully interesting, adage and geek quotes",
            sizeMb = 2.3,
            isInstalled = true,
            binaries = listOf("fortune"),
            category = "Fun & ASCII"
        ),
        ArchPackage(
            name = "sl",
            version = "5.05-1",
            repo = "extra",
            description = "Steam Locomotive running through your terminal to cure mistyped ls",
            sizeMb = 0.1,
            isInstalled = true,
            binaries = listOf("sl"),
            category = "Fun & ASCII"
        ),
        ArchPackage(
            name = "figlet",
            version = "2.2.5-3",
            repo = "extra",
            description = "Frank, Ian & Glenn's Letters - prints large banners from text",
            sizeMb = 0.8,
            isInstalled = false,
            binaries = listOf("figlet"),
            category = "Fun & ASCII"
        ),
        ArchPackage(
            name = "clang",
            version = "19.1.2-1",
            repo = "extra",
            description = "C language family frontend for LLVM compiler infrastructure",
            sizeMb = 142.0,
            isInstalled = false,
            binaries = listOf("clang", "clang++"),
            category = "Programming"
        ),
        ArchPackage(
            name = "make",
            version = "4.4.1-2",
            repo = "core",
            description = "GNU utility to maintain and build groups of programs",
            sizeMb = 1.5,
            isInstalled = true,
            binaries = listOf("make"),
            category = "Programming"
        ),
        ArchPackage(
            name = "zsh",
            version = "5.9-4",
            repo = "extra",
            description = "Advanced programmable command interpreter and power user shell",
            sizeMb = 9.8,
            isInstalled = false,
            binaries = listOf("zsh"),
            category = "Shell & CLI"
        )
    )
}
