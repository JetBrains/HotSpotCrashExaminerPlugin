// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.github.mkartashev.hserr.miner.artifact

import com.github.mkartashev.hserr.miner.HsErrLog
import com.github.mkartashev.hserr.miner.text.TextRange
import java.time.LocalDate
import java.time.format.DateTimeParseException

class VersionArtifact(
    log: HsErrLog,
    jreVersionTextRange: TextRange,
    jvmVersionTextRange: TextRange,
    val buildDate: LocalDate,
    val jreVersionFull: String,
    val jreVersionBuild: String,
    val jvmVersionFull: String,
    val jvmVersionBuild: String,
    val builtWith: String
) : Artifact(log) {
    override val title: String = "JVM/JRE versions"

    override val locations by lazy {
        setOf(jreVersionTextRange, jvmVersionTextRange).toSortedSet(compareBy { it.start })
    }

    val jreVersionMajor: Int by lazy {
        jreVersionBuild.takeWhile { it.isDigit() }.toInt()
    }

    private val versionsDiffer: Boolean
        get() = jreVersionBuild != jvmVersionBuild

    override val comment by lazy {
        jvmVersionFull +
                if (versionsDiffer) "; JVM and JRE ($jreVersionFull) versions differ, which normally doesn't happen" else ""
    }
}

internal object VersionExtractor : ArtifactExtractor<VersionArtifact> {
    override fun extract(log: HsErrLog): VersionArtifact {
        val header = log.getArtifact(HeaderArtifact::class) ?: fail("No Summary section found")
        var jreVersionSelection = header.location.start.moveToLineStartsWithString("# JRE version:").selectUpToEOL()
        val jvmVersionSelection = header.location.start.moveToLineStartsWithString("# Java VM:").selectUpToEOL()

        // vm_info: OpenJDK 64-Bit Server VM (17.0.6+10-b888.3) for linux-amd64 JRE (17.0.6+10-b888.3), built on 2024-03-17 by "builduser" with gcc 8.3.1 20190311 (Red Hat 8.3.1-3)
        val vmInfoSel = log.start.moveToLineStartsWithString("vm_info:").selectUpToEOL()

        if (jreVersionSelection.isEmpty() && vmInfoSel.isEmpty()) fail("Couldn't locate JRE version")
        if (jvmVersionSelection.isEmpty() && vmInfoSel.isEmpty()) fail("Couldn't locate Java VM version")

        var jreVersionBuild = ""
        var jreVersionFull = ""
        var jvmVersionBuild = ""
        var jvmVersionFull = ""

        // # JRE version: OpenJDK Runtime Environment JBR-17.0.3+7-469.12-jcef (17.0.3+7) (build 17.0.3+7-b469.12)
        val jreVersionTokens = jreVersionSelection.toString().split(' ')

        if (jreVersionTokens.size >= 8) {
            if (!jreVersionTokens[jreVersionTokens.lastIndex - 1].contains("build")) fail("Couldn't find JRE build number")

            jreVersionBuild = jreVersionTokens.lastOrNull()?.trim('(', ')') ?: ""
            jreVersionFull =
                jreVersionTokens.dropWhile { !it.contains('.') }.firstOrNull()?.trim('(', ')') ?: ""
        }

        // # Java VM: OpenJDK 64-Bit Server VM JBR-17.0.3+7-469.12-jcef (17.0.3+7-b469.12, mixed mode, tiered, compressed oops, compressed class ptrs, g1 gc, bsd-aarch64)
        val jvmVersionTokens = jvmVersionSelection.toString().split(' ')

        if (jvmVersionTokens.size >= 9) {
            jvmVersionFull = jvmVersionTokens.dropWhile { !it.contains('.') }.firstOrNull()?.trim(',') ?: ""
            jvmVersionBuild =
                jvmVersionTokens.dropWhile { !it.startsWith('(') }.dropWhile { !it.contains('.') }.firstOrNull()
                    ?.trim('(', ')', ',') ?: ""
        }

        val vmInfo = vmInfoSel.toString()
        if (jreVersionBuild.isEmpty() || jreVersionFull.isEmpty()) {
            // try getting from "vm_info:" line
            jreVersionBuild = vmInfo.substringAfter("JRE (").substringBefore(')')
            jreVersionFull = jreVersionBuild
            jreVersionSelection = vmInfoSel
        }

        jvmVersionFull = jvmVersionFull.trim { it == '(' || it == ')' || it == ',' }

        if (jvmVersionBuild.isEmpty() || jvmVersionFull.isEmpty()) {
            // try getting from "vm_info:" line
            jvmVersionBuild = vmInfo.substringAfter('(').substringBefore(')')
            jvmVersionFull = jvmVersionBuild
            jreVersionSelection = vmInfoSel
        }

        val vmInfoBuild = vmInfo.substringAfter("built on ", "")
        val buildDateCandidate = vmInfoBuild.substringBefore(' ')
        val buildDateString =
            if (buildDateCandidate.length >= 10) buildDateCandidate.substring(0, 10) else buildDateCandidate
        val buildDate = try {
            LocalDate.parse(buildDateString)
        } catch (ignored: DateTimeParseException) {
            Artifact.DEFAULT_DATE
        }

        val buildWith = if (vmInfoBuild.contains(" with ")) {
            vmInfoBuild.substringAfter(" with ").trim()
        } else {
            Artifact.DEFAULT_STRING
        }

        return VersionArtifact(
            log,
            jreVersionSelection, jvmVersionSelection,
            buildDate,
            jreVersionFull.trim(), jreVersionBuild.trim(),
            jvmVersionFull.trim(), jvmVersionBuild.trim(),
            buildWith
        )
    }
}
