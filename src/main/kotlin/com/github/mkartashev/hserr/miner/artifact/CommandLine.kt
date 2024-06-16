// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.github.mkartashev.hserr.miner.artifact

import com.github.mkartashev.hserr.miner.HsErrLog
import com.github.mkartashev.hserr.miner.text.TextRange

class CommandLineArtifact(log: HsErrLog, textRange: TextRange) : Artifact(log) {
    override val title: String = "JVM command line arguments"

    override val locations = setOf(textRange)

    override val comment = textRange.toString()
}

internal object CommandLineExtractor : ArtifactExtractor<CommandLineArtifact> {
    override fun extract(log: HsErrLog): CommandLineArtifact {
        val summary = log.getArtifact(SummaryArtifact::class) ?: fail("No Summary section found")
        val cmdLine = summary.location.start.moveToString("Command Line:", false)
            .selectUpToEOL()

        if (!summary.location.contains(cmdLine.endInclusive)) fail("Found outside of the summary section")

        return CommandLineArtifact(log, cmdLine)
    }
}