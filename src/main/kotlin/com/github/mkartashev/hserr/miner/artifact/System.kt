// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.github.mkartashev.hserr.miner.artifact

import com.github.mkartashev.hserr.miner.HsErrLog
import com.github.mkartashev.hserr.miner.artifact.ArtifactExtractor.Companion.parseMemorySize
import com.github.mkartashev.hserr.miner.artifact.ArtifactExtractor.Companion.toHumanReadableSize
import com.github.mkartashev.hserr.miner.text.TextRange

class SystemArtifact(
    log: HsErrLog,
    ranges: Set<TextRange>,
    val uname: String,
    val cpuCount: Int,
    val isVirtual: Boolean,
    val physMemoryTotal: ULong,
    val physMemoryFree: ULong,
    val swapTotal: ULong,
    val swapFree: ULong,
    val loadAverage: List<Float>
) : Artifact(log) {
    override val title: String = "Host system info"

    override val locations by lazy { ranges.toSortedSet(compareBy { it.start }) }

    fun isOverloaded(): Boolean {
        if (loadAverage.size >= 3) {
            val averageCPUQueueSizeForTheLast5Minutes = loadAverage[1]
            if (averageCPUQueueSizeForTheLast5Minutes > cpuCount * 2) return true
        }
        return false
    }

    override val comment: String
        get() {
            return "has $cpuCount CPU(s), ${if (isVirtual) "is running in virtual, " else ""}" +
                    "${toHumanReadableSize(physMemoryFree)} out of ${toHumanReadableSize(physMemoryTotal)} free physical memory" +
                    (if (loadAverage.isNotEmpty()) " with load average $loadAverage (in 1, 5, and 15 past minutes)" else "") +
                    (if (uname.isNotEmpty()) ". Full uname: '$uname'" else "") +
                    "."
        }

    val freePercentage: Int
        get() = if (physMemoryTotal > 0u) physMemoryFree.toDouble().div(physMemoryTotal.toDouble()).times(100).toInt()
        else 0

    val swapFreePercentage: Int
        get() = if (swapTotal > 0u) swapFree.toDouble().div(swapTotal.toDouble()).times(100).toInt()
        else 0
}

internal object SystemExtractor : ArtifactExtractor<SystemArtifact> {
    override fun extract(log: HsErrLog): SystemArtifact {
        val locations = mutableSetOf<TextRange>()
        val systemSelection = log.start
            .moveToLineWith("---------------  S Y S T E M").moveToNextLine()
            .selectUpToLineWith("END.")
        if (systemSelection.isEmpty()) fail("Couldn't find SYSTEM marker")

        val unameSelection = systemSelection.start.moveToLineStartsWithString("OS:")
            .moveToNextLine().selectUpToEOL()
        val uname = unameSelection.toString()
        if (!unameSelection.isEmpty()) locations.add(unameSelection)

        val cpuCountSelection = systemSelection.start.moveToString("CPU: total", false)
            .moveToNextWord().selectCurrentWord()
        val cpuCount = cpuCountSelection.toString().toIntOrNull() ?: 0
        if (!cpuCountSelection.isEmpty()) locations.add(cpuCountSelection)

        val loadAverageSelection = systemSelection.start.moveToString("load average: ", false)
            .selectUpToEOL()
        val loadAverage =
            if (loadAverageSelection.isEmpty()) emptyList() else loadAverageSelection.toString().split(" ")
                .map { (it.replace(',', '.').toFloatOrNull() ?: -1f) }
        if (!loadAverageSelection.isEmpty()) locations.add(loadAverageSelection)


        //     Memory: 16k page, physical 33554432k(903568k free), swap 2097152k(888640k free)
        val memorySelection = systemSelection.start.moveToLineStartsWithString("Memory: ")
            .moveToString("physical ", false).selectUpToEOL()
        val memoryTokens = memorySelection.toString().split(" ", "(", ")").filterNot { it.isBlank() }
        if (!memorySelection.isEmpty()) locations.add(memorySelection)

        val physMemoryTotalStr = memoryTokens.getOrElse(0) { "0" }
        val physMemoryFreeStr = memoryTokens.getOrElse(1) { "0" }

        val swapSelection = systemSelection.start.moveToLineStartsWithString("Memory: ")
            .moveToString(", swap ", false).selectUpToEOL()
        val swapTokens = swapSelection.toString().split(" ", "(", ")").filterNot { it.isBlank() }
        val swapTotalStr = swapTokens.getOrElse(0) { "-1" }
        val swapFreeStr = swapTokens.getOrElse(1) { "-1" }
        if (!swapSelection.isEmpty()) locations.add(swapSelection)

        val physMemoryTotal = parseMemorySize(physMemoryTotalStr) ?: Artifact.DEFAULT_ULONG
        val physMemoryFree = parseMemorySize(physMemoryFreeStr) ?: Artifact.DEFAULT_ULONG
        val swapTotal = parseMemorySize(swapTotalStr) ?: Artifact.DEFAULT_ULONG
        val swapFree = parseMemorySize(swapFreeStr) ?: Artifact.DEFAULT_ULONG

        val system = systemSelection.toString()
        val isVirtual = system.contains("Xen hardware-assisted virtualization detected")
                || system.contains("Xen optimized paravirtualization detected")
                || system.contains("KVM virtualization detected")
                || system.contains("VMWare virtualization detected")
                || system.contains("Hyper-V virtualization detected")

        return SystemArtifact(
            log,
            locations,
            uname,
            cpuCount,
            isVirtual,
            physMemoryTotal,
            physMemoryFree,
            swapTotal,
            swapFree,
            loadAverage
        )
    }
}
