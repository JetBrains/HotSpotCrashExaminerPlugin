// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.github.mkartashev.hserr.language

import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.elementType
import com.github.mkartashev.hserr.miner.artifact.*
import com.github.mkartashev.hserr.*
import com.github.mkartashev.hserr.language.psi.HsErrTypes
import com.github.mkartashev.hserr.settings.SettingsStore

class HsErrDocumentationProvider : DocumentationProvider {
    override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? {
        // Ctrl+hover = shows the definition
        return if (element == null) null else generateDoc(element, true)
    }

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        return if (element == null) null else generateDoc(element, false)
    }

    override fun getCustomDocumentationElement(
        editor: Editor,
        file: PsiFile,
        contextElement: PsiElement?,
        targetOffset: Int
    ): PsiElement? {
        return if (contextElement?.elementType == HsErrTypes.TOKEN) contextElement else null
    }

    private fun generateDoc(element: PsiElement, define: Boolean): String? {
        if (element.elementType == HsErrTypes.TOKEN) {
            val n = element.firstChild
            if (n != null) {
                if (n.elementType == HsErrTypes.NUMBER)
                    return generateDocForNumber(n, define)
                else if (n.elementType == HsErrTypes.WORD && !define) {
                    val word = n.text
                    if (word.length > 3 && word[0].isDigit()) {
                        if (word.endsWith('k', true) || word.endsWith("kb", true)) {
                            return generateDocForSize(word)
                        }
                    }
                } else if (n.elementType == HsErrTypes.REGISTER) {
                    return generateDocForRegister(n, define)
                } else if (n.elementType == HsErrTypes.SIGNAL) {
                    return generateDocForSignal(n.text)
                } else if (n.elementType == HsErrTypes.KEYWORD) {
                    return generateDocForKeyword(n.text)
                }
            }
        }

        return null
    }

    private fun generateDocForKeyword(text: String): String? {
        return when (text) {
            "safepoint"
                -> "<p>A moment when all threads running Java code have reached a well-known point in their code, " +
                    "where they don't modify the heap. Threads running JNI code are not paused because they access " +
                    "Java objects through JNI.</p>"
            "VMThread"
                -> "<p>A special thread where VM executes all its operations such as GC," +
                    " class redefinition, printing thread's list, etc.</p>"
            "WatcherThread"
                -> "<p>A dedicated VM thread used for simulating timer interrupts.</p>"
            "GCTaskThread", "ConcurrentGCThread"
                -> "<p>Threads running various tasks related to garbage collection" +
                    " such as scanning and marking.</p>"
            "JavaThread"
                -> "<p>A thread created from the Java code or attached to JVM with <code>AttachCurrentThread()</code>. " +
                    "Only a <code>JavaThread</code> can execute Java code and perform the <code>GetEnv()</code> JNI call.</p>"
            "LD_LIBRARY_PATH", "DYLD_LIBRARY_PATH"
                -> "<p>An environment variable that alters the normal dynamic libraries lookup and is usually reserved for " +
                    "debugging or temporary workarounds.</p>"
            "LD_PRELOAD", "DYLD_INSERT_LIBRARIES", "LDR_PRELOAD", "LDR_PRELOAD64"
                -> "<p>An environment variable that makes the dynamic loader insert libraries into the loaded program. " +
                    "This alters the order of initialization and changes symbol's lookup and is usually reserved for " +
                    "debugging or temporary workarounds.</p>"
            "C1" -> "<p>C1 is a HotSpot bytecode compiler that generates native code from the Java bytecode.</p>" +
                    "<p>C1 works faster than C2, but generates less optimized native code. " +
                    "It also can produce code that collects profiling information as that code runs in order to give " +
                    "the C2 compiler more data for best optimizations.</p>"
            "C2" -> "<p>C2 is a HotSpot bytecode compiler that generates native code from the Java bytecode.</p>" +
                    "<p>C2 generates highly optimized code and needs accurate profiling information for that, " +
                    "it is usually invoked for a method only after that method was called already thousands of times.</p>"
            "nmethod" -> "<p>Non-Interpreter Method, a piece of bytecode compiled into native code.</p>" +
                    "<p>This code is stored in the code cache, which size is controlled with <code>-XX:ReservedCodeCacheSize</code>.</p>"
            "daemon" -> "<p>A <i>daemon</i> thread does not block JVM from exiting when " +
                    "the last non-daemon thread was terminated.</p>"
            "Halt" -> null
            "SafepointALot" -> null
            "Cleanup" -> null
            "ForceSafepoint" -> "<p>A VM operation that forces a safepoint.</p>"
            "ICBufferFull" -> "<p>A VM operation that forces a safepoint due to inline cache buffers being full.</p>"
            "VM_ClearICs" -> "<p>A VM operation that clears inline code caches.</p>"
            "CleanClassLoaderDataMetaspaces" -> "<p>A VM operation that marks metadata seen on the stack " +
                    "so that unreferenced entries can be deleted.</p>"
            "DeoptimizeFrame" -> "<p>A VM operation that deoptimizes a compiled frame.</p>"
            "DeoptimizeAll" -> null
            "ZombieAll" -> null
            "PrintThreads" -> "<p>A VM operation that prints out additional information supplied by the application.</p>"
            "PrintMetadata" -> "<p>A VM operation that prints out metaspace statistics.</p>"
            "FindDeadlocks" -> "<p>A VM operation that prints finds deadlocks involving raw monitors, " +
                    "object monitors and concurrent locks.</p>"
            "Exit" -> "<p>A VM operation that initiates the termination of the VM.</p>"
            "ThreadDump" -> "<p>A VM operation that prints out threads info, usually invoked from <code>jstack</code>.</p>"
            "PrintCompileQueue" -> "<p>A VM operation that prints out what threads currently compile what methods.</p>"
            "PrintClassHierarchy" -> "<p>A VM operation that prints out class hierarchy.</p>"
            "HandshakeAllThreads" -> "<p>A VM operation that pauses all Java threads in order to perform certain operations.</p>"
            else -> null
        }
    }

    private fun generateDocForSignal(name: String): String? {
        return when (name) {
            "EXCEPTION_ACCESS_VIOLATION" -> "Indicates a memory related error."
            "SIGABRT" -> "Indicates that abort() was called from the native code, likely because of libc built-in heap checks detected a problem like double-free."
            "SIGILL" -> "Indicates an illegal instruction at PC."
            "SIGFPE" -> "Indicates a floating-point error by an instruction at PC."
            "SIGSEGV" -> "Indicates a segmentation violation when accessing the address pointed to by <code>si_addr</code>."
            "SIGBUS" -> "Indicates a bus error when accessing the address pointed to by <code>si_addr</code>."
            else -> null
        }
    }

    private fun generateDocForRegister(reg: PsiElement, define: Boolean): String {
        val name = reg.text
        val doc = StringBuilder()
        if (!define) {
            // https://gist.github.com/justinian/385c70347db8aca7ba93e87db90fc9a6
            // https://github.com/Siguza/ios-resources/blob/master/bits/arm64.md#calling-convention
            val descr = when (name.uppercase()) {
                "PC", "RIP" -> "instruction pointer"
                "RDI" -> "first argument"
                "RSI" -> "second argument"
                "RDX", "X2" -> "third argument"
                "RCX", "X3" -> "4th argument"
                "R8", "X4" -> "5th argument"
                "R9", "X5" -> "6th argument"
                "R10", "R11" -> "scratch"
                "EFLAGS", "CPSR" -> "status"
                "CSGSFS" -> "segments (cs, gs, fs)"
                "ERR" -> "exception vector number"
                "TRAPNO" -> "error code for exception"
                "X6" -> "7th argument"
                "X7" -> "8th argument"
                "X8" -> "points to the return value if >128 bits, otherwise scratch"
                "X18" -> "platform register"
                "XMM0" -> "128bit floating-point, return value"
                "XMM1", "XMM2", "XMM3", "XMM4", "XMM5", "XMM6", "XMM7" -> "128bit floating-point"
                "RAX" -> "return value"
                "SP", "RSP" -> "stack pointer"
                "RBP", "X29", "FP" -> "frame pointer, callee-saved"
                "RBX", "R12", "R13", "R14", "R15" -> "callee-saved"
                "X30", "LR" -> "return address"
                "X0" -> "first argument, return value"
                "X1" -> "second argument, return value"
                "X9", "X10", "X11", "X12", "X13", "X14", "X15", "X16", "X17" -> "scratch"
                "X19", "X20", "X21", "X22", "X23", "X24", "X25", "X26", "X27", "X28" -> "callee-saved"
                else -> "unknown"
            }
            doc.append("<p>Register, <b>$descr</b>.</p>")
        }

        val log = getHsErrLogCached(reg.project, reg.containingFile.virtualFile)
        if (log != null) {
            val registersArtifact = log.getArtifact(RegistersArtifact::class)
            if (registersArtifact != null) {
                val value = registersArtifact.valueOf(name)
                if (value != null) {
                    doc.append("<p>Value: <code>0x${value.toString(16)}</code></p>")
                    describeAddress(reg, value, doc, false)
                }
            }
        }
        return doc.toString()
    }

    private fun generateDocForSize(word: String): String? {
        if (word.endsWith('k', true)) {
            return word.substring(0, word.length - 1).toULongOrNull()?.times(1024u)?.toHumanReadableSize()
        } else if (word.endsWith("kb", true)) {
            return word.substring(0, word.length - 2).toULongOrNull()?.times(1024u)?.toHumanReadableSize()
        } else if (word.endsWith('m', true)) {
            return word.substring(0, word.length - 1).toULongOrNull()?.times(1024u*1024u)?.toHumanReadableSize()
        } else if (word.endsWith("mb", true)) {
            return word.substring(0, word.length - 2).toULongOrNull()?.times(1024u*1024u)?.toHumanReadableSize()
        }
        return null
    }


    private fun generateDocForNumber(n: PsiElement, define: Boolean): String? {
        val doc = StringBuilder()
        val addr = parseAsAddress(n.text) ?: return null

        if (!define && n.text.toULongOrNull() != null && addr > 999u) {
            // Possibly not an address after all
            val followingText = n.parent.skipWhitespace()?.text
            if (followingText != null) {
                if (followingText.equals("k", true) || followingText.equals("kb", true)) {
                    return addr.times(1024u).toHumanReadableSize()
                } else if (followingText.equals("m", true) || followingText.equals("mb", true)) {
                    return addr.times(1024u * 1024u).toHumanReadableSize()
                }
            }

            doc.append("<p>${addr.toHumanReadableSize()}</p>")
        }

        describeAddress(n, addr, doc, true)
        return if (doc.isBlank()) null else doc.toString()
    }

    private fun describeAddress(n: PsiElement, addr: ULong, doc: StringBuilder, checkRegister: Boolean) {
        val selectAddr = n.containingFile.virtualFile.getUserData(selectAddrKey)
        if (selectAddr != null) {
            if (selectAddr != addr) {
                val sign = if (selectAddr > addr) '-' else '+'
                val delta = if (selectAddr > addr) selectAddr - addr else addr - selectAddr
                doc.append("<code>== 0x${selectAddr.toString(16)} $sign 0x${delta.toString(16)}</code></p>")
            } else {
                doc.append("<p>== selected address <code>0x${addr.toString(16)}</code></p>")
            }
        }

        val log = getHsErrLogCached(n.project, n.containingFile.virtualFile)
        if (log != null) {
            val range = SettingsStore.getInstance().theState.findAddrRange

            val heapArtifact = log.getArtifact(JavaHeapArtifact::class)
            if (heapArtifact != null && addr >= heapArtifact.address && addr < heapArtifact.address + heapArtifact.size) {
                val distance = addr - heapArtifact.address
                if (distance == 0UL) {
                    doc.append("<p>== Java heap start address</p>")
                } else {
                    doc.append("<p>Points into Java heap (<code>0x${heapArtifact.address.toString(16)}" +
                            " + 0x${distance.toString(16)}</code>)</p>")
                }
            }

            val registersArtifact = log.getArtifact(RegistersArtifact::class)
            if (registersArtifact != null && checkRegister) {
                val regs = registersArtifact.values().filter {
                    val distance = distanceBetween(addr, it.value)
                    distance <= range
                }
                for (reg in regs) {
                    val sign = if (reg.value > addr) '-' else '+'
                    val delta = if (reg.value > addr) reg.value - addr else addr - reg.value
                    if (delta == 0UL) {
                        doc.append("<p><code>== ${'$'}${reg.key}</p>")
                    } else {
                        doc.append("<p><code>== ${'$'}${reg.key} $sign 0x${delta.toString(16)}</code></p>")
                    }
                }
            }

            val threads = log.getArtifact(ThreadsArtifact::class)
            if (threads != null) {
                var thread = threads.byAddress(addr)
                if (thread != null) {
                    if (thread.name.isBlank()) {
                        doc.append("<p>Points to an <i>unnamed</i> <i>${thread.kind}</i>.<p>")
                    } else {
                        doc.append("<p>Points to a <i>${thread.kind}</i> \"${thread.name}\".<p>")
                    }
                }
                thread = threads.byStackAddress(addr)
                if (thread != null) {
                    if (thread.name.isBlank()) {
                        doc.append("<p>Belongs to the stack of an <i>unnamed</i> <i>${thread.kind}</i>.<p>")
                    } else {
                        doc.append("<p>Belongs to the stack of a <i>${thread.kind}</i> \"${thread.name}\".<p>")
                    }
                }
            }

            val libs = log.getArtifact(DynamicLibrariesArtifact::class)
            if (libs != null) {
                val lib = libs.byAddress(addr)
                if (lib != null && addr >= lib.start) {
                    val delta = addr - lib.start
                    doc.append("<p>Belongs to ${if (lib.file.isBlank()) "an <i>unnamed</i> memory segment" else "\"" + lib.file + "\""} ")
                    doc.append("(<code>0x${lib.start.toString(16)} + 0x${delta.toString(16)}</code>)")
                    if (lib.perm.isNotBlank() && lib.perm != Artifact.DEFAULT_STRING) doc.append(" with permissions '<code>${lib.perm}</code>'")
                    doc.append("</p>")
                }
            }
        }
    }

    private fun distanceBetween(addr1: ULong, addr2: ULong) =
        if (addr1 >= addr2) addr1.minus(addr2) else addr2.minus(addr1)
}

fun PsiElement.skipWhitespace(): PsiElement? {
    var element: PsiElement? = this.nextSibling
    while (element != null && element.elementType == HsErrTypes.WHITE_SPACE) {
        element = element.nextSibling
    }
    return element
}