// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.github.mkartashev.hserr.miner.artifact

import com.github.mkartashev.hserr.miner.HsErrLog
import com.github.mkartashev.hserr.miner.text.TextRange

class InstructionsArtifact(
    log: HsErrLog,
    textRange: TextRange,
    val instructionsFromPC: String
) : Artifact(log) {

    override val title: String = "Instructions around crash site"

    override val locations = setOf(textRange)

    override val comment = "Instructions (starting from pc):\n" +
                instructionsFromPC
}

internal object InstructionsExtractor : ArtifactExtractor<InstructionsArtifact> {
    override fun extract(log: HsErrLog): InstructionsArtifact {
        val instructionsSelection = log.start.moveToLineStartsWithString("Instructions: (pc=").selectUpToFirstEmptyLine()

        if (instructionsSelection.isEmpty()) fail("Couldn't find the instructions section")
        val pcSelection = instructionsSelection.start.moveToString("(pc=", false).selectCurrentHexNumber()
        val pc = pcSelection.toString()

        if (pc.isBlank()) fail("Couldn't extract PC from the instructions section")

        val codeStart = instructionsSelection.start.moveToLineStartsWithString("$pc:")
        val code = TextRange.of(codeStart, instructionsSelection.endInclusive).toString()

        // Now strip address prefixes:
        val instructionsFromPC = code.lines().joinToString("\n")
                { it.substringAfter(":   ") }.trimEnd()

        return InstructionsArtifact(log, instructionsSelection, instructionsFromPC)
    }
}
