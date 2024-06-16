// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.github.mkartashev.hserr.miner.artifact

import com.github.mkartashev.hserr.miner.HsErrLog
import com.github.mkartashev.hserr.miner.text.TextRange

class SummaryArtifact(log: HsErrLog, textRange: TextRange): Artifact(log) {
    override val title: String = "Section: SUMMARY"

    override val locations = setOf(textRange)

    val hostInfo: TextRange by lazy {
        textRange.start.moveToLineStartsWithString("Host: ").selectUpToEOL()
    }

}

internal object SummaryExtractor: ArtifactExtractor<SummaryArtifact> {
    override fun extract(log: HsErrLog): SummaryArtifact {
        val summarySelection = log.start
            .moveToLineWith("---------------  S U M M A R Y").moveToNextLine()
            .selectUpToLineWith("---------------")
        if (summarySelection.isEmpty()) fail("Couldn't find SUMMARY markers")
        return SummaryArtifact(log, summarySelection)
    }
}