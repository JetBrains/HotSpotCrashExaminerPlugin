// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.github.mkartashev.hserr.miner.artifact

import com.github.mkartashev.hserr.miner.HsErrLog
import com.github.mkartashev.hserr.miner.text.TextRange

class EnvVarsArtifact(
    log: HsErrLog,
    textRange: TextRange,
    val variables: Map<String, String>
) : Artifact(log) {

    val isLibraryPathOverridden = variables.keys.contains("LD_LIBRARY_PATH")
            || variables.keys.contains("DYLD_LIBRARY_PATH")
            || variables.keys.contains("DYLD_FALLBACK_LIBRARY_PATH")

    override val title: String = "Environment variables"

    override val locations = setOf(textRange)

    override val comment = if (isLibraryPathOverridden) "The normal order of shared library loading is overridden"
                           else ""
}

internal object EnvVarsExtractor : ArtifactExtractor<EnvVarsArtifact> {
    override fun extract(log: HsErrLog): EnvVarsArtifact {
        val start = log.start.moveToLineStartsWithString("Environment Variables:")
        val sel = start.selectUpToFirstEmptyLine()
        val variables = if (!sel.isEmpty()) {
            val lines = sel.toString().lines()
            lines.stream()
                .skip(1)
                .filter { it.contains("=") }
                .map {
                    it.split("=", limit = 2).let { p ->
                        p[0] to p[1]
                    }
                }
                .toList()
                .toMap()
        } else emptyMap()

        return EnvVarsArtifact(log, sel, variables)
    }
}
