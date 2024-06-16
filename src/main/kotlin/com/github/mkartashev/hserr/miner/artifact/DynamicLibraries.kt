// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.github.mkartashev.hserr.miner.artifact

import com.github.mkartashev.hserr.miner.HsErrLog
import com.github.mkartashev.hserr.miner.text.TextRange

class DynamicLibrariesArtifact(
    log: HsErrLog,
    textRange: TextRange,
    osKind: String,
    val map: List<MemoryMapEntry>
) : Artifact(log) {

    override val title: String = "Dynamic Libraries"

    override val locations = setOf(textRange)

    fun byAddress(addr: ULong): MemoryMapEntry? {
        return map.firstOrNull { addr >= it.start && addr < it.end }
    }

    data class MemoryMapEntry(
        val location: TextRange,
        val file: String,
        val start: ULong,
        val end: ULong,
        val perm: String
    )
}

internal object DynamicLibrariesExtractor : ArtifactExtractor<DynamicLibrariesArtifact> {
    override fun extract(log: HsErrLog): DynamicLibrariesArtifact {
        val sel = log.start.moveToLineStartsWithString("Dynamic libraries:").selectUpToFirstEmptyLine()
        val libraries = mutableListOf<DynamicLibrariesArtifact.MemoryMapEntry>()
        val osArtifact = log.getArtifact(OSArtifact::class)
        val osKind = osArtifact?.kind ?: Artifact.DEFAULT_STRING
        if (osArtifact != null) {
            if (osArtifact.isMacOS()) {
                // 0x0000000198469000 	/usr/lib/libxml2.2.dylib
                var line = sel.start.moveToNextLine().selectUpToEOL()
                var lineText = line.toString()
                while (lineText.startsWith("0x")) {
                    val addrString = lineText.substringBefore(' ')
                    val addr = ArtifactExtractor.parseAddr(addrString)
                    if (addr != null) {
                        val file = lineText.substringAfter(' ')
                        val entry = DynamicLibrariesArtifact.MemoryMapEntry(
                            line,
                            file,
                            addr,
                            Artifact.DEFAULT_ULONG,
                            Artifact.DEFAULT_STRING
                        )
                        libraries.add(entry)
                    }
                    line = line.endInclusive.moveToNextChar(2).selectUpToEOL()
                    lineText = line.toString()
                }
            } else if (osArtifact.isWindows()) {
                // 0x00007ff8c1bf0000 - 0x00007ff8c1cf0000 	C:\Windows\System32\ucrtbase.dll
                var line = sel.start.moveToNextLine().selectUpToEOL()
                var lineText = line.toString()
                while (lineText.startsWith("0x") && lineText.contains('-')) {
                    val startString = lineText.substringBefore(' ')
                    val s = lineText.substringAfter('-').trim()
                    val endString = s.substringBefore(' ')
                    val file = s.substringAfter(' ').trim()
                    val start = ArtifactExtractor.parseAddr(startString)
                    val end = ArtifactExtractor.parseAddr(endString)
                    if (start != null && end != null) {
                        val entry = DynamicLibrariesArtifact.MemoryMapEntry(
                            line,
                            file,
                            start,
                            end,
                            Artifact.DEFAULT_STRING
                        )
                        libraries.add(entry)
                    }
                    line = line.endInclusive.moveToNextChar(2).selectUpToEOL()
                    lineText = line.toString()
                }
            } else if (osArtifact.isLinux()) {
                // 100020000-100040000 rw-p 00000000 00:00 0
                // 556dc0802000-556dc0803000 rw-p 00002000 103:05 14692230                  /home/user/bin/idea-T/jbr/bin/java
                var line = sel.start.moveToNextLine().selectUpToEOL()
                var lineText = line.toString()
                while (lineText.contains('-')) {
                    val startString = lineText.substringBefore('-')
                    val s = lineText.substringAfter('-').trim()
                    val endString = s.substringBefore(' ')
                    val perm = s.substringAfter(' ', "").substringBefore(' ')
                    val file = s.substringAfter(' ', "")
                        .substringAfter(' ', "")
                        .substringAfter(' ', "")
                        .substringAfter(' ', "")
                        .substringAfter(' ', "").trim()
                    val start = ArtifactExtractor.parseAddr(startString)
                    val end = ArtifactExtractor.parseAddr(endString)
                    if (start != null && end != null) {
                        val entry = DynamicLibrariesArtifact.MemoryMapEntry(
                            line,
                            file,
                            start,
                            end,
                            perm
                        )
                        libraries.add(entry)
                    }
                    line = line.endInclusive.moveToNextChar(2).selectUpToEOL()
                    lineText = line.toString()
                }
            }
        }

        return DynamicLibrariesArtifact(log, sel, osKind, libraries)
    }
}
