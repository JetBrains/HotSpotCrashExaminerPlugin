// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.github.mkartashev.hserr.miner.artifact

import com.github.mkartashev.hserr.miner.HsErrLog
import com.github.mkartashev.hserr.miner.artifact.ArtifactExtractor.Companion.toHumanReadableSize
import com.github.mkartashev.hserr.miner.text.TextRange
import kotlin.text.StringBuilder

class ExecutiveSummaryArtifact(log: HsErrLog, summary: String) : Artifact(log) {

    override val title: String = "Executive summary"

    override val locations = setOf(TextRange.of(log.start, log.end))

    override val comment = summary

    val reasonHtml: String by lazy {
        val arch = log.getArtifact(ArchitectureArtifact::class)
        val os = log.getArtifact(OSArtifact::class)
        val header = log.getArtifact(HeaderArtifact::class)
        val signal = log.getArtifact(SignalInfoArtifact::class)
        val uptime = log.getArtifact(UptimeArtifact::class)
        val thread = log.getArtifact(ThreadInfoArtifact::class)
        val system = log.getArtifact(SystemArtifact::class)
        val threads = log.getArtifact(ThreadsArtifact::class)
        val envVars = log.getArtifact(EnvVarsArtifact::class)
        val addr = log.getArtifact(CrashAddressArtifact::class)

        val sb = StringBuilder()
        sb.append("<html>")
        when (signal?.signalName) {
            "SIGABRT" -> {
                sb.append("A crash by <code>SIGABRT</code> likely means that <code>abort()<abort> was called.")
                if (os?.isLinux() == true || os?.isMacOS() == true) {
                    sb.append(
                        " <code>SIGABRT</code> can also be caused by the C library discovering" +
                                " a problem with the native heap (like double-free or buffer overrun), so" +
                                " this could mean a native heap problem"
                    )
                }
            }

            "SIGSEGV" -> {
                sb.append("<p>A crash by <code>SIGSEGV</code> usually means a \"bad\" pointer")
                when (signal.signalCode) {
                    "SEGV_MAPERR" -> sb.append(
                        "; sub-code suggests that the address was not mapped," +
                                " i.e. this memory is not occupied neither by a file nor by a dynamic object"
                    )

                    "SEGV_ACCERR" -> sb.append(
                        "; sub-code suggests invalid permissions," +
                                " i.e. this memory was protected by the system or the program itself"
                    )
                }
                sb.append(".</p>")
            }

            "SIGBUS" -> {
                sb.append(
                    "<p>A crash by <code>SIGBUS</code> means a bus error when accessing address;" +
                            " could be a bad pointer " +
                            if (arch?.family == ArchitectureArtifact.Family.ARM) "(alignment issues on aarch64)," else "" +
                                    " protected memory with <code>mprotect</code> (logical error?)," +
                                    " we’re on a network drive and there was a network outage," +
                                    " malfunctioning device driver or an underlying hardware error"
                )
                when (signal.signalCode) {
                    "BUS_ADRALN" -> {
                        sb.append("; sub-code suggests invalid address alignment")
                        if (os?.isMacOS() == true) {
                            sb.append(". On MacOS, it may also mean a violation of some OS-specific memory protection.")
                        }
                    }

                    "BUS_ADRERR" -> sb.append(
                        "; sub-code suggests nonexistent physical address," +
                                " for example <code>mmap</code>'ed file from network has disappeared"
                    )
                }
                sb.append(".</p>")
            }

            "SIGILL" -> {
                sb.append(
                    "<p>A crash by <code>SIGILL</code> means an illegal instruction at PC. Could be caused" +
                            "by incorrect code generation or, more likely, by a jump/call to a wrong address.</p>"
                )
            }

            "SIGFPE" -> {
                sb.append("<p>A crash by <code>SIGFPE</code> means floating-point error by an instruction at address; ")
                when (signal.signalCode) {
                    "FPE_INTDIV" -> sb.append("sub-code indicates integer divide by zero")
                    "FPE_FLTDIV" -> sb.append("sub-code indicates floating-point divide by zero")
                    "FPE_FLTINV" -> sb.append(
                        "sub-code indicates an invalid floating-point operation" +
                                " (such as square root of a negative value)"
                    )

                    else -> sb.append(
                        "FPE_INTDIV - integer divide by zero, FPE_FLTDIV - floating-point divide by zero" +
                                "FPE_FLTINV - floating-point invalid operation (like <code>sqrt(-1)</code>)"
                    )
                }
                sb.append(".</p>")
            }
        }

        if (arch?.family == ArchitectureArtifact.Family.INTEL && addr?.isMalformedOnX64 == true) {
            sb.append(
                "<p>The crash address is malformed and will likely look like <code>0x0</code> in the log." +
                        "Consult the PC register for the correct value. This is likely the result of an indirect call" +
                        "to a wrong address.</p>"
            )
        }

        val deadByOOME = header?.reason == "Out of Memory Error"
        if (header?.reason == "Internal Error") {
            sb.append(
                "<p>JVM crashed because of internal consistency check failure" +
                        " (<code>${header.assertion}</code>)."
            )
            if (header.sourceLocation.length > 4) {
                sb.append("Check <code>${header.sourceLocation}</code> to find out more.")
            }
            sb.append("</p>")
        } else if (deadByOOME) {
            sb.append(
                "<p>JVM crashed because it ran out of memory, not necessarily the Java heap." +
                        " The exact type and amount of memory can be found at the top of the log file."
            )
            if (header != null && header.sourceLocation.length > 4) {
                sb.append(
                    "<p>You can also check this source code location " +
                            "<code>${header.sourceLocation}</code> to find out more details."
                )
            }
            sb.append("</p>")
        }

        if (threads != null && threads.threads.size > 300) {
            sb.append(
                "<p>There seems to be an unusually high number of threads (${threads.threads.size}) in the JVM. " +
                        "Their stacks occupy ${toHumanReadableSize(threads.stacksSize)} that is not counted towards the Java heap.</p>"
            )
        }

        if (system != null && system.freePercentage < 2) {
            sb.append("<p>The host system is also nearly out of free physical memory (${system.freePercentage}% left)")
            if (os?.isWindows() == false && system.swapTotal <= 0u) sb.append("Also, there is no swap allocated.")
            sb.append("</p>")
        }

        if (thread != null) {
            if (thread.isCompilerThread) {
                sb.append(
                    """<p>JVM crashed compiling some Java bytecode into the native code;
                        | this may be a bug in the JVM itself, or a result of memory
                        |  corruption caused either by the program itself
                        |   or by a hardware malfunction.</p>""".trimMargin()
                )
                if (thread.methodBeingCompiled.length > 3) {
                    sb.append(
                        """<p>The method that was being compiled: <code>${thread.methodBeingCompiled}</code>.
                            | If there are many similar reports with the same method name,
                            |  it can be excluded from compilation with
                            |   <code>-XX:CompileCommand=exclude,class/name,method_name</code>.</p>""".trimMargin()
                    )
                }
            } else if (thread.name in setOf(
                    "VM Thread",
                    "Signal Dispatcher",
                    "Attach Listener",
                    "Finalizer",
                    "Reference Handler",
                    "Sweeper thread",
                    "Common-Cleaner"
                )
            ) {
                sb.append(
                    "<p>JVM crashed in one of its utility threads (<code>${thread.name}</code>)." +
                            " This may be caused either by a bug in the JVM or a memory corruption.</p>"
                )
            } else if (thread.name.contains("AWT")) {
                sb.append(
                    "<p>JVM crashed in an AWT-related thread. The thread's stack needs to be inspected" +
                            " to find further clues.</p>"
                )
            } else if (thread.name.contains("GCTaskThread") || thread.name.contains("GC Thread")) {
                sb.append(
                    """<p>The name of the thread that crashed (<code>${thread.name}<code>)
                        | implies a problem in the garbage collector. This may be caused
                        |  either by a bug in the JVM or a memory corruption
                        |  (which may have been caused by faulty memory chip or overclocking).</p>""".trimMargin()
                )
            }
        }

        if (envVars?.isLibraryPathOverridden == true) {
            sb.append(
                """
                <p>The dynamic library path has been overridden with an environment variable.
                This can lead to serious problems, including crashes. Check the dynamic libraries
                section carefully to see if any of the libraries are loaded from the location
                pointed to by that environment variable.</p>
            """.trimIndent()
            )
        }

        if (uptime != null && uptime.uptime.toSeconds() <= 5) {
            sb.append(
                """<p>The JVM uptime suggests that it crashed very early after the start.
                    | This may point towards startup issues like incorrect
                    | options specified, etc.</p>""".trimMargin()
            )
        }

        sb.append("</html>")
        sb.toString()
    }

    val reason: String by lazy {
        reasonHtml.replace(Regex("<.*?>"), "")
    }

}

internal object ExecutiveSummaryExtractor : ArtifactExtractor<ExecutiveSummaryArtifact> {
    override fun extract(log: HsErrLog): ExecutiveSummaryArtifact {
        val s = StringBuilder()

        val arch = log.getArtifact(ArchitectureArtifact::class)
        val os = log.getArtifact(OSArtifact::class)
        val header = log.getArtifact(HeaderArtifact::class)
        val signal = log.getArtifact(SignalInfoArtifact::class)
        val version = log.getArtifact(VersionArtifact::class)
        val uptime = log.getArtifact(UptimeArtifact::class)
        val thread = log.getArtifact(ThreadInfoArtifact::class)
        val system = log.getArtifact(SystemArtifact::class)
        val exceptions = log.getArtifact(InternalExceptionsArtifact::class)
        val threads = log.getArtifact(ThreadsArtifact::class)
        val heap = log.getArtifact(JavaHeapArtifact::class)
        val memory = log.getArtifact(ProcessMemoryArtifact::class)

        if (version != null) {
            s.append("JVM (${version.jvmVersionBuild})")
        } else {
            s.append("JVM (version unknown)")
        }

        s.append(" crashed")

        val osString = os?.name ?: ""
        val archString = arch?.family.toString()

        if (osString.isNotEmpty() || archString.isNotEmpty()) {
            s.append(" running on $osString")
            if (archString.isNotEmpty()) s.append(" (${archString})")
        }

        if (uptime != null) {
            if (uptime.uptime.toSeconds() <= 5) {
                s.append(" almost immediately after the start")
            } else {
                s.append(" after ${uptime.uptimeAsString} from the start")
            }
        }

        if (header != null || signal != null || thread != null) {
            describeCrash(s, header, thread, signal, exceptions)
        }

        if (threads != null) {
            describeThreads(s, threads, thread)
        }

        if (heap != null) {
            describeHeap(s, heap)
        }

        if (memory != null) {
            describeMemory(s, memory, system)
        }

        if (system != null) {
            describeSystem(s, system)
        }

        s.append(".")
        return ExecutiveSummaryArtifact(log, s.toString())
    }

    private fun describeCrash(
        s: StringBuilder,
        header: HeaderArtifact?,
        thread: ThreadInfoArtifact?,
        signal: SignalInfoArtifact?,
        exceptions: InternalExceptionsArtifact?
    ) {
        s.append(". It crashed")

        if (thread != null) {
            s.append(" in ")

            val funName = thread.stack.topFrame().function
            if (funName.isNotBlank()) s.append("\"${funName}\" ")

            if (thread.isCompilerThread) {
                s.append("in a compiler thread while compiling \"${thread.methodBeingCompiled}\"")
            } else if (thread.isNative) {
                s.append("in a native thread, i.e. a thread that doesn't execute any Java code")
            } else {
                s.append("in a Java thread")
            }
        }

        if (signal != null) {
            s.append(" because of ${signal.comment}")
        } else if (header != null && header.reason.isNotEmpty()) {
            s.append(" because of ${header.reason}")
            if (header.sourceLocation.isNotEmpty()) {
                s.append(" (in '${header.sourceLocation}')")
            }
        }

        if (exceptions != null && exceptions.hasMemoryErrors) {
            s.append("; memory errors were reported before the crash")
        }
    }

    private fun describeThreads(s: StringBuilder, threads: ThreadsArtifact, thread: ThreadInfoArtifact?) {
        s.append(". JVM was executing ${threads.threads.size} threads")
        if (threads.stacksSize > 1024UL * 1024UL) {
            s.append(" with cumulative stack size of ${toHumanReadableSize(threads.stacksSize)}")
        }

        if (thread != null) {
            s.append(". The crash occurred in a thread ${thread.comment}")
        }
    }

    private fun describeSystem(s: StringBuilder, system: SystemArtifact) {
        s.append(". The host system ")
        if (system.isVirtual) s.append("was running in virtual")
        else s.append("was running on hardware")
        s.append(" with ${system.cpuCount} CPUs")
        if (system.loadAverage.isNotEmpty() && system.cpuCount > 0) {
            val load = (system.loadAverage[0] * 100.0 / system.cpuCount).toInt()
            s.append(" (${load}% load in the last minute)")
        }

        if (system.physMemoryTotal > 0u) {
            s.append(" and ${system.freePercentage}% free physical memory")
        }
    }

    private fun describeHeap(s: StringBuilder, heap: JavaHeapArtifact) {
        s.append(". Java heap had ${heap.freePercentage}% of ${toHumanReadableSize(heap.size)} free space")
    }

    private fun describeMemory(s: StringBuilder, memory: ProcessMemoryArtifact, system: SystemArtifact?) {
        s.append(". The Java process RSS was ${toHumanReadableSize(memory.rss)}")
        if (system != null && system.physMemoryTotal > 0UL) {
            val percent = (memory.rss.toDouble() * 100 / system.physMemoryTotal.toDouble()).toInt()
            if (percent > 0) {
                s.append(" or ${percent}% of system's total physical memory")
            }
        }
    }
}