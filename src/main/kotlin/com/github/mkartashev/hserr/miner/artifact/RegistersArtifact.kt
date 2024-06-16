// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.github.mkartashev.hserr.miner.artifact

import com.github.mkartashev.hserr.miner.HsErrLog
import com.github.mkartashev.hserr.miner.text.TextRange

class RegistersArtifact(
    log: HsErrLog,
    mappings: TextRange,
    registers: TextRange,
    val pcLocations: Set<TextRange>,
    architecture: ArchitectureArtifact.Family,
    private val values: Map<String, ULong>
) : Artifact(log) {

    override val title: String = "Registers"

    override val locations = if (mappings.isEmpty()) setOf(registers) else setOf(mappings, registers)

    private val synonyms = mapOf("X29" to "FP", "X30" to "LR", "X31" to "SP",
                                 "PC" to "RIP", "SP" to "RSP", "FP" to "RBP")

    fun valueOf(name: String): ULong? {
        val uname = name.uppercase()
        if (values.contains(uname)) {
            return values[uname]
        } else if (synonyms.contains(uname)) {
            val synonym = synonyms[uname]
            return values[synonym]
        }
        return null
    }

    fun values() = values
}

internal object RegistersExtractor : ArtifactExtractor<RegistersArtifact> {
    override fun extract(log: HsErrLog): RegistersArtifact {
        val start = log.start.moveToLineStartsWithString("Register to memory mapping:")
        val block1 = start.moveToNextLine().moveToNextLine().selectUpToFirstEmptyLine()
        val block2 = log.start.moveToLineStartsWithString("Registers:").moveToNextLine()
            .selectLinesWhile { it.contains("=0x") }

        if (block2.isEmpty()) fail("Couldn't locate registers section")

        val regs = block2.toString()
        val architecture: ArchitectureArtifact.Family =
            if (regs.contains("RIP=")) ArchitectureArtifact.Family.INTEL
            else if (regs.contains("x0=")) ArchitectureArtifact.Family.ARM
            else ArchitectureArtifact.Family.UNKNOWN

        val pcTextRanges = mutableSetOf<TextRange>()
        if (architecture == ArchitectureArtifact.Family.INTEL) {
            val pcSel1 = block1.start.moveToString("RIP=").selectCurrentWord()
            if (!pcSel1.isEmpty() && pcSel1.endInclusive in block1) pcTextRanges.add(pcSel1)

            val pcSel2 = block2.start.moveToString("RIP=").selectCurrentWord()
            if (!pcSel2.isEmpty() && pcSel2.endInclusive in block2) pcTextRanges.add(pcSel2)

        } else if (architecture == ArchitectureArtifact.Family.ARM) {
            val pcSel = block2.start.moveToString("pc=").selectCurrentWord()
            if (!pcSel.isEmpty() && pcSel.endInclusive in block2) pcTextRanges.add(pcSel)
        }

        val lines = block2.toString().lines()
        val registers: MutableMap<String, ULong> = mutableMapOf()
        for (line in lines) {
            if (line.contains("=")) {
                // R9 =0x00007fff6b9c26d7, EFLAGS=0x0000000000010246, ERR=0x0000000000000000
                //  x4=0x0000000000000050  x5=0x00000003df9c1610  x6=0x0000000000000005  x7=0x0000000000000000
                var l = line
                while (l.contains(" =")) l = l.replace(" =", "=")
                val parts = l.split(",", " ")
                for (part in parts) {
                    val name = part.substringBefore('=').trim()
                    val strValue = part.substringAfter('=').trim().trim(',')
                    if (name.isNotBlank() && strValue.startsWith("0x")) {
                        val value = strValue.substring(2).toULongOrNull(16)
                        if (value != null) registers[name.uppercase()] = value
                    }
                }
            }
        }

        return RegistersArtifact(log, block1, block2, pcTextRanges.toSet(), architecture, registers)
    }
}
