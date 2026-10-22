package com.example.archvm.model

import org.json.JSONArray
import org.json.JSONObject

sealed class VFSNode(
    open var name: String,
    open var permissions: String = "rw-r--r--",
    open var owner: String = "user",
    open var group: String = "user",
    open var modifiedTime: Long = System.currentTimeMillis()
) {
    abstract val size: Long
}

data class FileNode(
    override var name: String,
    var content: String = "",
    var isExecutable: Boolean = false,
    override var permissions: String = if (isExecutable) "rwxr-xr-x" else "rw-r--r--",
    override var owner: String = "user",
    override var group: String = "user",
    override var modifiedTime: Long = System.currentTimeMillis()
) : VFSNode(name, permissions, owner, group, modifiedTime) {
    override val size: Long
        get() = content.toByteArray().size.toLong()
}

data class DirNode(
    override var name: String,
    val children: MutableMap<String, VFSNode> = mutableMapOf(),
    override var permissions: String = "rwxr-xr-x",
    override var owner: String = "user",
    override var group: String = "user",
    override var modifiedTime: Long = System.currentTimeMillis()
) : VFSNode(name, permissions, owner, group, modifiedTime) {
    override val size: Long
        get() = children.size.toLong() * 4096L
}

class VFS {
    val root: DirNode = DirNode(name = "")

    init {
        initializeDefaultArchSystem()
    }

    private fun initializeDefaultArchSystem() {
        createDir("/bin")
        createDir("/usr")
        createDir("/usr/bin")
        createDir("/etc")
        createDir("/home")
        createDir("/home/user")
        createDir("/home/user/projects")
        createDir("/root")
        createDir("/var")
        createDir("/var/log")
        createDir("/tmp")
        createDir("/proc")

        // Populate binaries in /usr/bin and /bin
        val coreBins = listOf(
            "bash", "sh", "pacman", "ls", "cat", "cd", "pwd", "mkdir", "rm", "cp", "mv",
            "touch", "echo", "grep", "head", "tail", "wc", "clear", "uname", "whoami",
            "date", "uptime", "neofetch", "fastfetch", "htop", "top", "free", "df",
            "ps", "kill", "curl", "wget", "git", "python", "python3", "gcc", "clang",
            "node", "nano", "vim", "cowsay", "cmatrix", "fortune", "sl", "figlet",
            "tree", "history", "help", "man", "chmod", "which", "find", "stat"
        )
        for (bin in coreBins) {
            createFile("/usr/bin/$bin", "#!/bin/bash\n# Arch Linux binary stub for $bin\n", isExecutable = true)
        }

        // Configuration files in /etc
        createFile(
            "/etc/os-release",
            """
            NAME="Arch Linux"
            PRETTY_NAME="Arch Linux"
            ID=arch
            BUILD_ID=rolling
            ANSI_COLOR="38;2;23;147;209"
            HOME_URL="https://archlinux.org/"
            SUPPORT_URL="https://bbs.archlinux.org/"
            BUG_REPORT_URL="https://gitlab.archlinux.org/groups/archlinux"
            LOGO=archlinux-logo
            """.trimIndent()
        )

        createFile("/etc/arch-release", "Arch Linux release\n")
        createFile("/etc/hostname", "archlinux\n")
        createFile(
            "/etc/motd",
            """
            --------------------------------------------------
            * Welcome to Arch Linux Container VM on Android! *
            * Kernel: 6.11.4-arch1-1-SMP PREEMPT_DYNAMIC      *
            * Package Manager: pacman (v6.1.0)               *
            * Type 'help' or 'neofetch' to get started!       *
            --------------------------------------------------
            """.trimIndent()
        )

        createFile(
            "/etc/pacman.conf",
            """
            # /etc/pacman.conf
            [options]
            HoldPkg     = pacman glibc
            Architecture = x86_64
            Color
            CheckSpace
            VerbosePkgLists
            ParallelDownloads = 5
            SigLevel    = Required DatabaseOptional
            LocalFileSigLevel = Optional

            [core]
            Include = /etc/pacman.d/mirrorlist

            [extra]
            Include = /etc/pacman.d/mirrorlist

            [multilib]
            Include = /etc/pacman.d/mirrorlist
            """.trimIndent()
        )

        createFile(
            "/proc/version",
            "Linux version 6.11.4-arch1-1 (gcc 14.2.1 20240910) #1 SMP PREEMPT_DYNAMIC Sat Oct 12 14:02:15 UTC 2026\n"
        )
        createFile(
            "/proc/cpuinfo",
            """
            processor	: 0
            vendor_id	: AuthenticAMD
            cpu family	: 25
            model		: 1
            model name	: AMD EPYC 7763 64-Core Processor (ArchVM Virtualized)
            stepping	: 1
            cpu MHz		: 2449.880
            cache size	: 512 KB
            cpu cores	: 8
            flags		: fpu vme de pse tsc msr pae mce cx8 apic sep mtrr pge mca cmov pat pse36 clflush mmx fxsr sse sse2 ht syscall nx mmxext fxsr_opt pdpe1gb rdtscp lm constant_tsc rep_good nopl xtopology cpuid tsc_known_freq pni pclmulqdq ssse3 fma cx16 pcid sse4_1 sse4_2 x2apic movbe popcnt tsc_deadline_timer aes xsave avx f16c rdrand hypervisor lahf_lm cmp_legacy cr8_legacy abm sse4a misalignsse 3dnowprefetch osvw topoext perfctr_core invpcid avx2 smep erms invpcid_single rdseed adx smap clflushopt clwb sha_ni xsaveopt xsavec xgetbv1 xsaves clzero xsaveerptr arat
            """.trimIndent()
        )
        createFile(
            "/proc/meminfo",
            """
            MemTotal:        8192000 kB
            MemFree:         4892300 kB
            MemAvailable:    5981240 kB
            Buffers:          210400 kB
            Cached:          1879000 kB
            SwapTotal:       4194300 kB
            SwapFree:        4194300 kB
            """.trimIndent()
        )

        // User files in /home/user
        createFile(
            "/home/user/.bashrc",
            """
            # ~/.bashrc: executed by bash for non-login shells.
            alias ls='ls --color=auto'
            alias ll='ls -la'
            alias update='sudo pacman -Syu'
            alias fetch='neofetch'
            alias py='python3'
            export TERM=xterm-256color
            export PATH=/usr/local/bin:/usr/bin:/bin
            """.trimIndent()
        )

        createFile(
            "/home/user/welcome.txt",
            """
            🚀 Welcome to your Arch Linux VM Terminal!

            Key features at your fingertips:
            • pacman -S <pkg>   : Install developer tools (python, gcc, nodejs, rust, git, htop, etc.)
            • pacman -Syu       : Full system sync and rolling upgrade
            • neofetch          : Display Arch Linux system specs and logo
            • htop              : Interactive process & memory monitor
            • python script.py  : Python 3 interpreter and REPL
            • gcc main.c -o app : Compile C code and run ./app
            • nano filename     : In-terminal text editor (with Ctrl+O save, Ctrl+X exit)
            • curl <url>        : Real internet HTTP requests
            • cmatrix           : Hollywood hacker green rain animation

            Check out pre-made starter code in ~/projects/ :
              cd ~/projects
              python hello.py
              gcc main.c -o test && ./test
              node app.js
            """.trimIndent()
        )

        createFile(
            "/home/user/projects/hello.py",
            """
            # ArchVM Python 3 Demo
            import math
            import sys

            print("🐍 Hello from Python 3 on Arch Linux!")
            print(f"Platform: {sys.platform} | Interpreter: ArchVM Python 3.12")
            
            # Simple computation demo
            primes = [x for x in range(2, 30) if all(x % d != 0 for d in range(2, int(math.isqrt(x)) + 1))]
            print(f"Computed primes under 30: {primes}")

            for i in range(1, 4):
                print(f"  [+] Arch Linux Worker Thread #{i} initialized.")
            print("Done! Ready for your Python scripts.")
            """.trimIndent()
        )

        createFile(
            "/home/user/projects/main.c",
            """
            #include <stdio.h>

            int main() {
                printf("⚡ GCC 14.2 on Arch Linux x86_64\n");
                printf("Calculating factorial of 6...\n");
                int fact = 1;
                for (int i = 1; i <= 6; i++) {
                    fact *= i;
                }
                printf("Result: 6! = %d\n", fact);
                printf("C program executed successfully!\n");
                return 0;
            }
            """.trimIndent()
        )

        createFile(
            "/home/user/projects/app.js",
            """
            // Node.js on ArchVM
            console.log("🟢 Node.js v22.10.0 initialized");
            const system = {
              os: "Arch Linux",
              kernel: "6.11.4",
              arch: "x86_64",
              status: "running"
            };
            console.log("System Status:", JSON.stringify(system, null, 2));
            const numbers = [10, 20, 30, 40, 50];
            const sum = numbers.reduce((a, b) => a + b, 0);
            console.log("Average: " + (sum / numbers.length));
            """.trimIndent()
        )

        createFile(
            "/home/user/projects/fib.rs",
            """
            // Rust demo for ArchVM
            fn main() {
                println!("🦀 Rust 1.82 on Arch Linux!");
                let mut a = 0;
                let mut b = 1;
                print!("Fibonacci: ");
                for _ in 0..12 {
                    print!("{} ", a);
                    let temp = a + b;
                    a = b;
                    b = temp;
                }
                println!();
            }
            """.trimIndent()
        )

        createFile(
            "/home/user/projects/script.sh",
            """
            #!/bin/bash
            echo "🐧 ArchVM Maintenance and Health Check"
            echo "Current date: $(date)"
            echo "Kernel: $(uname -a)"
            echo "Current User: $(whoami)"
            echo "Disk Space:"
            df -h
            echo "Memory Info:"
            free -m
            echo "ArchVM is running optimally!"
            """.trimIndent(),
            isExecutable = true
        )

        createFile(
            "/var/log/pacman.log",
            """
            [2026-10-15T09:12:01+0000] [PACMAN] Running 'pacman -Syu'
            [2026-10-15T09:12:02+0000] [PACMAN] synchronizing package lists
            [2026-10-15T09:12:15+0000] [ALPM] transaction started
            [2026-10-15T09:12:20+0000] [ALPM] upgraded linux (6.11.3 -> 6.11.4-arch1-1)
            [2026-10-15T09:12:22+0000] [ALPM] transaction completed
            """.trimIndent()
        )
    }

    fun normalize(path: String, cwd: String): String {
        val trimmed = path.trim()
        val absolutePath = when {
            trimmed.startsWith("~") -> "/home/user" + trimmed.removePrefix("~")
            trimmed.startsWith("/") -> trimmed
            else -> if (cwd == "/") "/$trimmed" else "$cwd/$trimmed"
        }

        val parts = absolutePath.split("/").filter { it.isNotEmpty() && it != "." }
        val stack = mutableListOf<String>()
        for (part in parts) {
            if (part == "..") {
                if (stack.isNotEmpty()) stack.removeAt(stack.size - 1)
            } else {
                stack.add(part)
            }
        }
        return "/" + stack.joinToString("/")
    }

    fun getNode(path: String, cwd: String = "/"): VFSNode? {
        val norm = normalize(path, cwd)
        if (norm == "/") return root
        val parts = norm.split("/").filter { it.isNotEmpty() }
        var current: DirNode = root
        for (i in parts.indices) {
            val part = parts[i]
            val child = current.children[part] ?: return null
            if (i == parts.size - 1) {
                return child
            } else {
                if (child !is DirNode) return null
                current = child
            }
        }
        return null
    }

    fun getFile(path: String, cwd: String = "/"): FileNode? = getNode(path, cwd) as? FileNode
    fun getDir(path: String, cwd: String = "/"): DirNode? = getNode(path, cwd) as? DirNode

    fun createDir(path: String, cwd: String = "/"): Boolean {
        val norm = normalize(path, cwd)
        if (norm == "/") return true
        val parts = norm.split("/").filter { it.isNotEmpty() }
        var current: DirNode = root
        for (part in parts) {
            val child = current.children[part]
            if (child == null) {
                val newDir = DirNode(name = part)
                current.children[part] = newDir
                current = newDir
            } else if (child is DirNode) {
                current = child
            } else {
                return false
            }
        }
        return true
    }

    fun createFile(
        path: String,
        content: String = "",
        cwd: String = "/",
        isExecutable: Boolean = false,
        owner: String = "user"
    ): Boolean {
        val norm = normalize(path, cwd)
        val lastSlash = norm.lastIndexOf('/')
        val dirPath = if (lastSlash <= 0) "/" else norm.substring(0, lastSlash)
        val fileName = norm.substring(lastSlash + 1)
        if (fileName.isEmpty()) return false

        val parentDir = getDir(dirPath) ?: run {
            if (createDir(dirPath)) getDir(dirPath) else null
        } ?: return false

        val existing = parentDir.children[fileName]
        if (existing is DirNode) return false

        parentDir.children[fileName] = FileNode(
            name = fileName,
            content = content,
            isExecutable = isExecutable,
            owner = owner
        )
        return true
    }

    fun remove(path: String, cwd: String = "/", recursive: Boolean = false): Boolean {
        val norm = normalize(path, cwd)
        if (norm == "/" || norm == "/home" || norm == "/etc" || norm == "/bin") return false
        val lastSlash = norm.lastIndexOf('/')
        val dirPath = if (lastSlash <= 0) "/" else norm.substring(0, lastSlash)
        val name = norm.substring(lastSlash + 1)

        val parent = getDir(dirPath) ?: return false
        val child = parent.children[name] ?: return false
        if (child is DirNode && child.children.isNotEmpty() && !recursive) {
            return false
        }
        parent.children.remove(name)
        return true
    }

    fun list(path: String, cwd: String = "/"): List<VFSNode>? {
        val dir = getDir(path, cwd) ?: return null
        return dir.children.values.sortedBy { it.name }
    }

    fun copy(src: String, dst: String, cwd: String = "/"): Boolean {
        val srcNode = getNode(src, cwd) ?: return false
        if (srcNode is FileNode) {
            val dstNorm = normalize(dst, cwd)
            val targetDir = getDir(dstNorm)
            val targetPath = if (targetDir != null) "$dstNorm/${srcNode.name}" else dstNorm
            return createFile(targetPath, srcNode.content, isExecutable = srcNode.isExecutable)
        }
        return false
    }

    fun move(src: String, dst: String, cwd: String = "/"): Boolean {
        if (copy(src, dst, cwd)) {
            return remove(src, cwd, recursive = true)
        }
        return false
    }

    fun serializeCustomFiles(): String {
        val rootObj = JSONObject()
        val userFiles = JSONArray()
        fun traverse(dir: DirNode, currentPath: String) {
            for ((name, node) in dir.children) {
                val fullPath = if (currentPath == "/") "/$name" else "$currentPath/$name"
                if (node is FileNode) {
                    val fileObj = JSONObject().apply {
                        put("path", fullPath)
                        put("content", node.content)
                        put("isExecutable", node.isExecutable)
                    }
                    userFiles.put(fileObj)
                } else if (node is DirNode) {
                    traverse(node, fullPath)
                }
            }
        }
        traverse(root, "")
        rootObj.put("files", userFiles)
        return rootObj.toString()
    }

    fun restoreCustomFiles(json: String) {
        try {
            val rootObj = JSONObject(json)
            val array = rootObj.optJSONArray("files") ?: return
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val path = item.getString("path")
                val content = item.getString("content")
                val isExec = item.optBoolean("isExecutable", false)
                createFile(path, content, isExecutable = isExec)
            }
        } catch (_: Exception) {}
    }
}
