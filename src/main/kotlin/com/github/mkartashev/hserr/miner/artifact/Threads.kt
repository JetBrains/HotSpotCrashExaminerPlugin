// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.github.mkartashev.hserr.miner.artifact

import com.github.mkartashev.hserr.miner.HsErrLog
import com.github.mkartashev.hserr.miner.text.TextRange
import com.github.mkartashev.hserr.miner.artifact.ArtifactExtractor.Companion.toHumanReadableSize

class ThreadsArtifact(
    log: HsErrLog,
    textRanges: Set<TextRange>,
    threadsLocations: List<TextRange>
) : Artifact(log) {

    override val title: String = "Threads"

    override val locations by lazy {
        textRanges.toSortedSet(compareBy { it.start })
    }

    data class ThreadDescr(
        val location: TextRange,
        val kind: String,
        val name: String,
        val address: ULong,
        val stackLow: ULong,
        val stackHigh: ULong
    )

    val threads: List<ThreadDescr> by lazy {
        threadsLocations.map {
            val line = it.toString().trim().substringAfter("=>")

            // Examples:
            //   0x00007fa39801c9e0 VMThread "VM Thread" [stack: 0x00007fa37d8f8000,0x00007fa37d9f8000] [id=11129]
            //   0x00007fa3080adf20 JavaThread "I/O pool 30" [_thread_blocked, id=12184, stack(0x00007fa008a00000,0x00007fa008b00000)]
            // =>0x000000014189c800 JavaThread "AWT-AppKit" daemon [_thread_in_native, id=259, stack(0x000000016f150000,0x000000016f94c000)]
            val address = ArtifactExtractor.parseAddr(line.substringBefore(' ')) ?: DEFAULT_ULONG
            val name = line.substringAfter('"', "").substringBefore('"', "")
            val kind = line.substringAfter(' ', "").substringBefore(' ', "")

            val stackBounds = if (line.contains("[stack:")) line.substringAfter("[stack: ").substringBefore(']', "")
            else line.substringAfter("stack(").substringBefore(')', "")
            var stackLow = 0UL
            var stackHigh = 0UL
            if (stackBounds.contains(',')) {
                stackLow = ArtifactExtractor.parseAddr(stackBounds.substringBefore(',')) ?: DEFAULT_ULONG
                stackHigh = ArtifactExtractor.parseAddr(stackBounds.substringAfter(',')) ?: DEFAULT_ULONG
            }
            ThreadDescr(it, kind, name, address, stackLow, stackHigh)
        }
    }

    val names: List<String> by lazy {
        threads.map { it.name }
    }

    fun byAddress(addr: ULong): ThreadDescr? {
        return threads.firstOrNull { it.address == addr }
    }

    fun byStackAddress(addr: ULong): ThreadDescr? {
        return threads.firstOrNull { it.stackLow <= addr && addr < it.stackHigh }
    }

    val stacksSize: ULong by lazy {
        threads.map { it.stackHigh - it.stackLow }.fold(0UL, ULong::plus)
    }

    override val comment = "${threads.count()} total" +
            if (stacksSize > 1024UL * 1024UL) ", cumulative stacks size ${toHumanReadableSize(stacksSize)}" else ""
}

internal object ThreadsExtractor : ArtifactExtractor<ThreadsArtifact> {
    override fun extract(log: HsErrLog): ThreadsArtifact {
        val javaThreadsSelection = log.start.moveToLineWith("Java Threads:").selectUpToFirstEmptyLine()
        val otherThreadsSelection = log.start.moveToLineWith("Other Threads:").selectUpToFirstEmptyLine()

        val locations = mutableSetOf<TextRange>()
        if (!javaThreadsSelection.isEmpty()) locations.add(javaThreadsSelection)
        if (!otherThreadsSelection.isEmpty()) locations.add(otherThreadsSelection)

        if (locations.isEmpty()) fail("Couldn't find any threads information")

        val threadsLocations = mutableListOf<TextRange>()
        var lineStart = javaThreadsSelection.start.moveToNextLine()
        while (lineStart.isValid() && !lineStart.isAtEnd()) {
            val fullLine = lineStart.selectUpToEOL()
            if (fullLine.toString().isBlank()) break
            threadsLocations.add(fullLine)
            lineStart = lineStart.moveToNextLine()
        }

        lineStart = otherThreadsSelection.start.moveToNextLine()
        while (lineStart.isValid() && !lineStart.isAtEnd()) {
            val fullLine = lineStart.selectUpToEOL()
            if (fullLine.toString().isBlank()) break
            threadsLocations.add(fullLine)
            lineStart = lineStart.moveToNextLine()
        }

        return ThreadsArtifact(log, locations, threadsLocations)
    }
}
