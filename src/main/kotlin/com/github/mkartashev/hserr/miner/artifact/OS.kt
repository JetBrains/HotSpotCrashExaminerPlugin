// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.github.mkartashev.hserr.miner.artifact

import com.github.mkartashev.hserr.miner.HsErrLog
import com.github.mkartashev.hserr.miner.text.TextRange

private const val osKindWindows = "windows"
private const val osKindMacOS = "macOS"
private const val osKindLinux = "linux"

class OSArtifact(
    log: HsErrLog,
    textRanges: Set<TextRange>,
    val kind: String,
    val name: String
) : Artifact(log) {

    override val title: String = "Host OS"

    override val locations by lazy {
        textRanges.toSortedSet(compareBy { it.start })
    }

    override val comment: String
        get() = "$name ($kind)"

    fun isMacOS() = kind == osKindMacOS
    fun isWindows() = kind == osKindWindows
    fun isLinux() = kind == osKindLinux
}

internal object OSExtractor : ArtifactExtractor<OSArtifact> {
    override fun extract(log: HsErrLog): OSArtifact {
        val summary = log.getArtifact(SummaryArtifact::class) ?: fail("No Summary section found")
        val hostLine = summary.location.start.moveToLineStartsWithString("Host: ").selectUpToEOL()

        var osTextRange1: TextRange? = null
        var osKind = ""
        if (!hostLine.isEmpty()) {
            val hostString = hostLine.toString()
            if (hostString.contains("Windows")) {
                val start = hostLine.start.moveToString("Windows")
                val end = start.moveToNextWord().moveToWordEnd()
                osTextRange1 = start.selectUpTo(end)
                osKind = osKindWindows
            } else if (hostString.contains(osKindMacOS)) {
                val start = hostLine.start.moveToString(osKindMacOS)
                val end = start.moveToNextWord().moveToWordEnd()
                osTextRange1 = start.selectUpTo(end)
                osKind = osKindMacOS
            } else if (hostString.contains("Darwin")) {
                val start = hostLine.start.moveToString("Darwin")
                val end = start.moveToNextWord().moveToWordEnd()
                osTextRange1 = start.selectUpTo(end)
                osKind = osKindMacOS
            } else {
                // Assuming various kinds of Linuxes
                osTextRange1 = hostLine.start.moveToLastStringInLine(", ", false).selectUpToEOL()
                osKind = osKindLinux
            }
        }

        var osName = osTextRange1.toString()
        var osTextRange2: TextRange? = null
        val systemLoc = log.start.moveToLineWith("---------------  S Y S T E M").moveToNextLine()
        osTextRange2 = systemLoc.moveToLineStartsWithString("OS:")
            .selectUpToLineWith("OS uptime")

        // Double-check the kind because it is hard to detect Linux correctly in all cases
        // in the first check (the "Host:" line)
        val osLine = osTextRange2.toString()
        if (osLine.contains("uname:")) {
            // Linux or macOS
            val s = osLine.substringAfter("uname:")
            if (s.contains("Darwin")) {
                osKind = osKindMacOS
                if (osName.isEmpty()) {
                    osName = s.substringBefore(';')
                }
            } else if (s.contains("Linux")) {
                osKind = osKindLinux
                val descriptionSel = osTextRange2.start.moveToLineStartsWithString("DISTRIB_DESCRIPTION=")
                    .selectUpToEOL()
                if (!descriptionSel.isEmpty()) {
                    osName = descriptionSel.toString().substringAfter('=').trim('"')
                }
            } else if (osKind == osKindWindows) {
                fail("Ambiguous OS kind in the Host and system sections")
            }
        } else {
            if (osLine.contains("Windows")) {
                osKind = osKindWindows
            }
        }

        if (osTextRange2.isEmpty()) osTextRange2 = null

        if (osTextRange1 == null) fail("Can't identify OS from 'Host:' line")

        val locations = mutableSetOf(osTextRange1)
        if (osTextRange2 != null) locations.add(osTextRange2)
        return OSArtifact(log, locations, osKind, osName)
    }
}
