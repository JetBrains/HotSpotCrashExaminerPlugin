// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.github.mkartashev.hserr.miner.artifact

import com.github.mkartashev.hserr.miner.HsErrLog
import com.github.mkartashev.hserr.miner.text.TextRange

class ArchitectureArtifact(
    log: HsErrLog,
    ranges: Set<TextRange>,
    val family: Family,
    val is64Bit: Boolean
) : Artifact(log) {

    enum class Family {
        UNKNOWN, INTEL, ARM
    }

    override val title: String = "JVM architecture"

    override val locations by lazy { ranges.toSortedSet(compareBy { it.start }) }

    override val comment: String
        get() = "$family ${if (is64Bit) "64-bit" else "32-bit"}"

}

internal object ArchitectureExtractor : ArtifactExtractor<ArchitectureArtifact> {
    override fun extract(log: HsErrLog): ArchitectureArtifact {
        // Something like "bsd-aarch64)" or "windows-amd64)"
        var jvmArchSelection = log.lineAsTokens(log.start.moveToString("# Java VM:")).lastOrNull()
            ?: fail("Couldn't find Java VM info")
        if (jvmArchSelection.toString().endsWith(")")) jvmArchSelection = jvmArchSelection.trimRight(1)
        val dashIndex = jvmArchSelection.toString().indexOf('-')
        if (dashIndex != -1) jvmArchSelection = jvmArchSelection.trimLeft(dashIndex + 1)

        if (jvmArchSelection.isEmpty()) fail("Couldn't determine JVM arch from Java VM info")

        var family = when (jvmArchSelection.toString()) {
            "aarch64" -> ArchitectureArtifact.Family.ARM
            "amd64" -> ArchitectureArtifact.Family.INTEL
            else -> ArchitectureArtifact.Family.UNKNOWN
        }

        val locations = mutableSetOf(jvmArchSelection)
        // Something like "Host: "MacBookPro18,1" arm64" or "Host: Intel(R) Core(TM) i7-5500U CPU @ 2.40GHz"
        val summary = log.getArtifact(SummaryArtifact::class)
        var hostInfoTextRange: TextRange? = null
        if (summary != null) {
            for (token in log.lineAsTokens(summary.hostInfo.start)) {
                val str = token.toString()
                val endsWithComma = str.endsWith(",")
                if (str.contentEquals("arm64", true)) {
                    if (family != ArchitectureArtifact.Family.ARM) family = ArchitectureArtifact.Family.UNKNOWN
                } else if (str.contentEquals("intel", true)) {
                    if (family != ArchitectureArtifact.Family.INTEL) family = ArchitectureArtifact.Family.UNKNOWN
                } else if (str.contentEquals("amd64", true)) {
                    if (family != ArchitectureArtifact.Family.INTEL) family = ArchitectureArtifact.Family.UNKNOWN
                }
                hostInfoTextRange = if (endsWithComma) token.trimRight(1) else token
                if (endsWithComma) break
            }
            if (hostInfoTextRange != null && !hostInfoTextRange.isEmpty()) locations.add(hostInfoTextRange)
        }

        val systemLoc = log.start.moveToLineWith("---------------  S Y S T E M").moveToNextLine()
        val unameLineStart = systemLoc.moveToLineStartsWithString("uname:")
        if (unameLineStart.isValid()) {
            val unameTokens = log.lineAsTokens(unameLineStart)
            if (unameTokens.isNotEmpty()) locations.add(unameTokens.last())
        }

        return ArchitectureArtifact(log, locations, family, true)
    }
}
