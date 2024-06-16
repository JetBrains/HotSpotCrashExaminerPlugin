// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.github.mkartashev.hserr.miner.artifact

import com.github.mkartashev.hserr.miner.HsErrLog
import com.github.mkartashev.hserr.miner.artifact.ArtifactExtractor.Companion.parseMemorySize
import com.github.mkartashev.hserr.miner.artifact.ArtifactExtractor.Companion.toHumanReadableSize
import com.github.mkartashev.hserr.miner.text.TextRange

class ProcessMemoryArtifact(
    log: HsErrLog,
    textRanges: Set<TextRange>,
    val rss: ULong
) : Artifact(log) {
    override val title: String = "Process Memory"

    override val locations by lazy {
        textRanges.toSortedSet(compareBy { it.start })
    }

    override val comment by lazy {
        "RSS ${toHumanReadableSize(rss)}"
    }
}

internal object ProcessMemoryExtractor : ArtifactExtractor<ProcessMemoryArtifact> {
    override fun extract(log: HsErrLog): ProcessMemoryArtifact {
        val memUsageSelection = log.start
            .moveToLineStartsWithString("Process memory usage:").selectUpToFirstEmptyLine()
        if (memUsageSelection.isEmpty()) fail("Couldn't find process memory usage information")

        val rssString = memUsageSelection.start.moveToLineWith("Resident Set Size:")
            .moveToLastStringInLine("Resident Set Size:", false)
            .moveToNextWord().selectCurrentWord().toString()

        val rss = parseMemorySize(rssString) ?: fail("Couldn't the RSS value")

        val locations = mutableSetOf(memUsageSelection)

        val procMemorySelection = log.start
            .moveToLineStartsWithString("Process Memory:")
            .selectUpToFirstEmptyLine()
        if (!procMemorySelection.isEmpty()) locations.add(procMemorySelection)
        return ProcessMemoryArtifact(log, locations, rss)
    }
}
