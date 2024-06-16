// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.github.mkartashev.hserr.miner.artifact

import com.github.mkartashev.hserr.miner.HsErrLog
import com.github.mkartashev.hserr.miner.text.TextRange

class HeaderArtifact(
    log: HsErrLog,
    textRange: TextRange,
    val pid: Long,
    val reason: String,
    val sourceLocation: String,
    val assertion: String
) : Artifact(log) {
    override val title: String = "Section: HEADER"

    override val locations = setOf(textRange)

}

internal object HeaderExtractor : ArtifactExtractor<HeaderArtifact> {
    override fun extract(log: HsErrLog): HeaderArtifact {
        var headerLine = log.start
        while (log.charAt(headerLine) == '#') headerLine = headerLine.moveToNextLine()

        if (headerLine == log.start) fail("Not one line with # in the beginning of the crash log")

        val header = log.start.selectUpTo(headerLine.moveToPrevLine().moveToLineEnd())
        val pidString = header.start.moveToString("pid=").moveToNextChar(4)
            .selectCurrentWord().toString().trimEnd(',')
        val pid = ArtifactExtractor.parseLong(pidString)

        // #  Internal Error (relocInfo_x86.cpp:155), pid=13063, tid=13109
        // #  guarantee(which == Assembler::imm_operand) failed: must be immediate operand
        // or
        // #  Out of Memory Error (./src/hotspot/os/windows/os_windows.cpp:3521), pid=1284, tid=11844
        val pidLineStart = log.start.moveToLineWith("pid=")
        val pidLine = pidLineStart.selectUpToEOL().toString()
        val reason = pidLine.substringBefore('(', "").trimEnd().trimStart('#', ' ')
        val sourceLocation =
            if (reason.startsWith("EXCEPTION_") || reason.startsWith("SIG")) "" else
                pidLine.substringAfter('(', "").substringBefore(')')
        val assertion = pidLineStart.moveToNextLine().selectUpToEOL().toString().trimStart('#', ' ')

        return HeaderArtifact(log, header, pid, reason, sourceLocation, assertion)
    }
}