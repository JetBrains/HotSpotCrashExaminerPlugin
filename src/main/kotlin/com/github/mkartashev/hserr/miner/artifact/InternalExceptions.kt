// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.github.mkartashev.hserr.miner.artifact

import com.github.mkartashev.hserr.miner.HsErrLog
import com.github.mkartashev.hserr.miner.text.TextRange

class InternalExceptionsArtifact(
    log: HsErrLog,
    textRange: Set<TextRange>,
    val latest: List<String>,
    val oomeCount: Int,
    val stackOverflowCount: Int,
    val linkageErrorsCount: Int
) : Artifact(log) {

    override val title: String = "Internal exceptions"

    override val locations by lazy {
        textRange.toSortedSet(compareBy { it.start })
    }

    val hasMemoryErrors = oomeCount > 0 || stackOverflowCount > 0 || latest.any { it.contains(".OutOfMemoryError") }

    override val comment = "the latest exceptions thrown" + if (hasMemoryErrors) "; includes memory errors (NB!)" else ""
}

internal object InternalExceptionsExtractor : ArtifactExtractor<InternalExceptionsArtifact> {
    override fun extract(log: HsErrLog): InternalExceptionsArtifact {
        val countsStart = log.start.moveToLineStartsWithString("OutOfMemory and StackOverflow Exception counts:")
        var countsEnd = countsStart.moveToNextLine()
        var oomeCount = 0
        var stackOverflowCount = 0
        var linkageErrorsCount = 0
        while (countsEnd.isValid() && !countsEnd.isAtEnd()) {
            val line = countsEnd.selectUpToEOL().toString()
            if (line.isBlank()) break
            val count = line.substringAfter('=').toIntOrNull() ?: 0
            if (line.contains("OutOfMemoryError")) {
               oomeCount += count
            } else if (line.contains("LinkageErrors")) {
                linkageErrorsCount += count
            } else if (line.contains("StackOverflowErrors")) {
                stackOverflowCount += count
            }
            countsEnd = countsEnd.moveToNextLine()
        }
        val locations = mutableSetOf(countsStart.selectUpTo(countsEnd))

        val eventsListSelection = log.start
            .moveToLineStartsWithString("Internal exceptions")
            .selectUpToFirstEmptyLine()
        if (!eventsListSelection.isEmpty()) locations.add(eventsListSelection)

        val lines = eventsListSelection.toString().lines().filter { it.startsWith("Event:") && it.contains("Exception <a '") }
        val latest = lines.map {
            it.substringAfter("<a '").substringBefore('\'').replace('/', '.')
        }

        return InternalExceptionsArtifact(log, locations, latest, oomeCount, stackOverflowCount, linkageErrorsCount)
    }
}
