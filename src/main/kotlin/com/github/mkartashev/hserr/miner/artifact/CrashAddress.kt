// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.github.mkartashev.hserr.miner.artifact

import com.github.mkartashev.hserr.miner.HsErrLog
import com.github.mkartashev.hserr.miner.text.TextRange

class CrashAddressArtifact(
    log: HsErrLog,
    textRange: Set<TextRange>,
    val pc: ULong
) : Artifact(log) {

    override val title: String = "Crash address"

    override val locations by lazy {
        textRange.toSortedSet(compareBy { it.start })
    }

    val isMalformedOnX64 = when ((pc shr 32) and 0xffff8000U) {
        0x0UL, 0xffff8000UL -> false
        else -> true
    }

    override val comment = "0x${pc.toString(16)}${if (isMalformedOnX64) " (malformed)" else ""}"
}

internal object CrashAddressExtractor : ArtifactExtractor<CrashAddressArtifact> {
    override fun extract(log: HsErrLog): CrashAddressArtifact {
        val header = log.getArtifact(HeaderArtifact::class) ?: fail("No header section found")

        val headerLoc = header.location
        var pcFromSummarySelection = headerLoc.start.moveToString("pc=", stopAtBeginning = false).selectCurrentWord()
        if (pcFromSummarySelection.toString().endsWith(",")) pcFromSummarySelection =
            pcFromSummarySelection.trimRight(1)
        if (pcFromSummarySelection.endInclusive !in headerLoc) fail("pc= not found in the header")
        val pcString = pcFromSummarySelection.toString()
        if (!pcString.startsWith("0x")) fail("pc value in does not start with 0x")
        val pc = ArtifactExtractor.parseAddr(pcString) ?: fail("Failed to convert $pcString to integer")

        val resultSelections = mutableSetOf(pcFromSummarySelection)

        val instructionsArtifact = log.getArtifact(InstructionsArtifact::class)
        if (instructionsArtifact != null && pc != 0UL) {
            val start = instructionsArtifact.location.start
            val instrPCSelection = start.moveToString(pcString).selectCurrentHexNumber()
            if (!instrPCSelection.isEmpty()) resultSelections.add(instrPCSelection)

            val instrSelection = instrPCSelection.endInclusive.moveToString(pcString).selectCurrentHexNumber()
            if (!instrPCSelection.isEmpty()) resultSelections.add(instrSelection)
        }

        val registersArtifact = log.getArtifact(RegistersArtifact::class)
        if (registersArtifact != null) {
            resultSelections.addAll(registersArtifact.pcLocations)
        }

        return CrashAddressArtifact(log, resultSelections, pc)
    }
}
