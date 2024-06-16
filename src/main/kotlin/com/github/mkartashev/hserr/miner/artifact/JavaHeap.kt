// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.github.mkartashev.hserr.miner.artifact

import com.github.mkartashev.hserr.miner.HsErrLog
import com.github.mkartashev.hserr.miner.artifact.ArtifactExtractor.Companion.parseMemorySize
import com.github.mkartashev.hserr.miner.artifact.ArtifactExtractor.Companion.toHumanReadableSize
import com.github.mkartashev.hserr.miner.text.TextRange

class JavaHeapArtifact(
    log: HsErrLog,
    textRanges: Set<TextRange>,
    val address: ULong,
    val size: ULong,
    val used: ULong
) : Artifact(log) {
    override val title: String = "Java Heap"

    override val locations by lazy {
        textRanges.toSortedSet(compareBy { it.start })
    }

    val freePercentage = ((size - used).toDouble() * 100.0).div(size.toDouble()).toInt()

    override val comment by lazy {
        "maximum size ${toHumanReadableSize(size)} with ${freePercentage}% free space left"
    }
}

internal object JavaHeapExtractor : ArtifactExtractor<JavaHeapArtifact> {
    override fun extract(log: HsErrLog): JavaHeapArtifact {
        val heapSelection = log.start
            .moveToLineStartsWithString("Heap address: ").selectUpToEOL()
        if (heapSelection.isEmpty()) fail("Couldn't find Heap address information")

        // Heap address: 0x0000000080000000, size: 2048 MB, Compressed Oops mode: 32-bit
        val heapLine = heapSelection.toString()
        val addressStr = heapLine.substringAfter("Heap address: ", "").substringBefore(',')
        val address = ArtifactExtractor.parseAddr(addressStr) ?: fail("Heap address is not a number")

        val sizeStr = heapLine.substringAfter("size: ", "").substringBefore(',')
        val size = parseMemorySize(sizeStr) ?: fail("Couldn't parse heap size")

        val heapSelection2 = log.start.moveToLineStartsWithString("Heap:").selectUpToFirstEmptyLine()
        var used = Artifact.DEFAULT_ULONG
        if (!heapSelection2.isEmpty()) {
            // garbage-first heap   total 1328128K, used 388956K [0x0000000080000000, 0x0000000100000000)
            val line = heapSelection2.toString().substringAfter("garbage-first heap", "").substringBefore('[')
            used = parseMemorySize(line.substringAfter("used ").trim()) ?: fail("Couldn't parse heap used size")
        }

        val locations = mutableSetOf(heapSelection)
        if (!heapSelection2.isEmpty()) locations.add(heapSelection2)

        return JavaHeapArtifact(log, locations, address, size, used)
    }
}
