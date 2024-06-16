// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.github.mkartashev.hserr.miner.artifact

import com.github.mkartashev.hserr.miner.HsErrLog
import com.github.mkartashev.hserr.miner.text.TextRange
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class UptimeArtifact(
    log: HsErrLog,
    textRange: TextRange,
    uptimeInSeconds: Int
) : Artifact(log) {
    override val title: String = "JVM uptime"

    override val locations = setOf(textRange)

    override val comment: String by lazy {
        uptimeAsString +
                if (uptime.toSeconds() <= 5) {
                    ", JVM crashed right after start"
                } else if (uptime.toMinutes() <= 1) {
                    ", JVM crashed very early after start"
                } else {
                    ""
                }
    }

    val uptime: java.time.Duration by lazy {
        java.time.Duration.ofSeconds(uptimeInSeconds.toLong())
    }

    val uptimeAsString = uptime.toSeconds().toDuration(DurationUnit.SECONDS).toString()
}

internal object UptimeExtractor : ArtifactExtractor<UptimeArtifact> {
    override fun extract(log: HsErrLog): UptimeArtifact {
        val summary = log.getArtifact(SummaryArtifact::class) ?: fail("No Summary section found")
        val uptimeStartLoc = summary.location.start.moveToString("elapsed time:").moveToNextWord().moveToNextWord()
        val uptimeEndLoc = uptimeStartLoc.moveToWordEnd()

        if (!uptimeEndLoc.isValid()) fail("failed to find 'elapsed time' in the summary")

        val unitsStartLoc = uptimeEndLoc.moveToNextWord()
        val unitsTextRange = TextRange.of(unitsStartLoc, unitsStartLoc.moveToWordEnd())
        if (!unitsTextRange.toString().contains("seconds")) {
            fail("failed to find the word 'seconds' following JVM uptime")
        }

        if (!summary.location.contains(uptimeEndLoc)) {
            fail("elapsed time found outside of the summary")
        }

        val uptimeTextRange = TextRange.of(uptimeStartLoc, uptimeEndLoc)
        val uptime =
            uptimeTextRange.toString().trim().toDoubleOrNull() ?: fail("failed parsing uptime '$uptimeTextRange'")

        return UptimeArtifact(log, uptimeTextRange, uptime.toInt())
    }
}
