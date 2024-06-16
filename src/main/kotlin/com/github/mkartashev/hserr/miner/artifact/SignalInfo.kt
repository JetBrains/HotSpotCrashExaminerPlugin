// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.github.mkartashev.hserr.miner.artifact

import com.github.mkartashev.hserr.miner.HsErrLog
import com.github.mkartashev.hserr.miner.text.TextRange

class SignalInfoArtifact(
    log: HsErrLog,
    textRanges: Set<TextRange>,
    val signalName: String,
    val signalCode: String,
    val addr: String
) : Artifact(log) {

    override val title: String = "Signal"

    private val isUnixStyle = signalName.startsWith("SIG")

    override val locations by lazy {
        textRanges.toSortedSet(compareBy { it.start })
    }

    override val comment: String
        get() = "${signalName}${if (signalCode.isNotEmpty()) " ($signalCode)" else ""} " +
                when (signalName) {
                    "EXCEPTION_ACCESS_VIOLATION" -> ", a memory related error"
                    "SIGABRT" -> "abort() was called from the native code, likely because of libc built-in heap checks detected a problem like double-free"
                    "SIGILL" -> {
                        "illegal instruction at PC " +
                                when (signalCode) {
                                    "ILL_ILLOPC" -> "(illegal opcode)"
                                    "ILL_ILLOPN" -> "(illegal operand)"
                                    "ILL_ILLADR" -> "(illegal addressing mode)"
                                    "ILL_ILLTRP" -> "(illegal trap)"
                                    "ILL_PRVOPC" -> "(privileged opcode)"
                                    "ILL_PRVREG" -> "(privileged register)"
                                    "ILL_COPROC" -> "(coprocessor error)"
                                    "ILL_BADSTK" -> "(internal stack error"
                                    "" -> "(no si_code, which is irregular)"
                                    else -> "(unknown si_code)"
                                }
                    }

                    "SIGFPE" -> {
                        "floating-point error by an instruction at $addr: " +
                                when (signalCode) {
                                    "FPE_INTDIV" -> "integer divide by zero"
                                    "FPE_INTOVF" -> "integer overflow"
                                    "FPE_FLTDIV" -> "floating-point divide by zero"
                                    "FPE_FLTOVF" -> "floating-point overflow"
                                    "FPE_FLTUND" -> "floating-point underflow"
                                    "FPE_FLTRES" -> "floating-point inexact result"
                                    "FPE_FLTINV" -> "floating-point invalid operation"
                                    "FPE_FLTSUB" -> "subscript out of range"
                                    "" -> "(no si_code, which is irregular)"
                                    else -> "(unknown si_code)"
                                }
                    }

                    "SIGSEGV" -> {
                        "segmentation violation when accessing $addr " +
                                when (signalCode) {
                                    "SEGV_MAPERR" -> "(address not mapped)"
                                    "SEGV_ACCERR" -> "(invalid permissions)"
                                    "SEGV_BNDERR" -> "(failed address bound check)"
                                    "SEGV_PKUERR" -> "(access was denied by memory protection keys)"
                                    "" -> "(no si_code, which is irregular)"
                                    else -> "(unknown si_code)"
                                }
                    }

                    "SIGBUS" -> {
                        "bus error when accessing $addr " +
                                when (signalCode) {
                                    "BUS_ADRALN" -> "(invalid address alignment)"
                                    "BUS_ADRERR" -> "(nonexistent physical address, for example mmap'ed file from network has disappeared)"
                                    "BUS_OBJERR" -> "(object-specific hardware error)"
                                    "" -> "(no si_code, which is irregular)"
                                    else -> "(unknown si_code)"
                                }
                    }

                    else -> ", address $addr."
                }

}

internal object SignalInfoExtractor : ArtifactExtractor<SignalInfoArtifact> {
    override fun extract(log: HsErrLog): SignalInfoArtifact {
        val line1Start = log.start.moveToLineWith("pc=")
        val line1String = line1Start.selectUpToEOL().toString()
        var sel1: TextRange? = null
        if (line1String.startsWith("#")) {
            sel1 = line1Start.moveToNextWord().selectCurrentWord()
        }

        var sel2: TextRange? = null
        var signalName = sel1.toString()
        var signalCode = ""
        var addr = ""
        val line2Start = log.start.moveToLineStartsWithString("siginfo: ")
        if (line2Start.isValid()) {
            sel2 = line2Start.selectUpToEOL()
            // Examples:
            // siginfo: EXCEPTION_ACCESS_VIOLATION (0xc0000005), reading address 0x000001d474dcbfc0
            // siginfo: si_signo: 4 (SIGILL), si_code: 2 (ILL_ILLTRP), si_addr: 0x000000019064ac70
            val litmusTestWord = line2Start.moveToNextWord().selectCurrentWord()
            if (litmusTestWord.toString().contains("si_signo")) {
                // Unix-style info
                val tokens = line2Start.selectUpToEOL().toString().split(" ")
                for ((i, t) in tokens.withIndex()) {
                    if (t.contains("si_signo")) {
                        signalName = tokens.getOrElse(i + 2) { "" }.trim('(', ')', ',')
                    } else if (t.contains("si_code")) {
                        signalCode = tokens.getOrElse(i + 2) { "" }.trim('(', ')', ',')
                    } else if (t.contains("si_addr")) {
                        addr = tokens.getOrElse(i + 1) { "" }
                    }
                }
            } else {
                // Assuming Windows-style info
                signalName = line2Start.moveToNextWord().selectCurrentWord().toString()
                val tokens = line2Start.selectUpToEOL().toString().split(" ")
                if (tokens.isNotEmpty()) addr = tokens.last()
            }
        }

        val sel = mutableSetOf<TextRange>()
        if (sel1 != null) sel.add(sel1)
        if (sel2 != null) sel.add(sel2)
        if (sel.isEmpty()) fail("Couldn't locate signal info anywhere")

        return SignalInfoArtifact(log, sel, signalName, signalCode, addr)
    }
}
