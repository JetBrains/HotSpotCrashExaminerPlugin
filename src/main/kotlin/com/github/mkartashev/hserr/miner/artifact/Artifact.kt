// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.github.mkartashev.hserr.miner.artifact

import com.github.mkartashev.hserr.miner.HsErrLog
import com.github.mkartashev.hserr.miner.text.TextRange
import java.lang.RuntimeException
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.reflect.KClass

abstract class Artifact(val log: HsErrLog) {
    abstract val title: String

    open val locations: Set<TextRange> = emptySet()

    val location: TextRange
        get() = if (locations.isEmpty()) log.emptyRange else locations.first()

    open val comment: String = ""

    companion object {
        const val DEFAULT_STRING = "<unknown>"
        const val DEFAULT_CHAR = '-'
        const val DEFAULT_INT = -1
        const val DEFAULT_LONG = -1L
        const val DEFAULT_ULONG = 0UL
        val DEFAULT_DATE: LocalDate = LocalDate.of(1962, 10, 27)
        val DEFAULT_DATE_TIME: LocalDateTime = LocalDateTime.of(1951, 1, 1, 0, 0)
    }

    override fun toString(): String {
        return "$title: $comment."
    }
}

class ArtifactExtractionException(reason: String): RuntimeException(reason)

interface ArtifactExtractor<out T : Artifact> {
    fun extract(log: HsErrLog): T

    fun fail(reason: String): Nothing = throw ArtifactExtractionException(reason)

    companion object {
        private const val KiB = 1024u

        fun parseInt(s: String): Int =
            try {
                Integer.decode(s)
            } catch (ignore: NumberFormatException) {
                -1
            }

        fun parseLong(s: String): Long =
            try {
                java.lang.Long.decode(s)
            } catch (ignore: NumberFormatException) {
                -1
            }

        fun parseAddr(s: String): ULong? {
            val res = if (s.startsWith("0x") || s.startsWith("00"))
                s.substring(2).toULongOrNull(16)
            else s.toULongOrNull()
            return res
        }

        fun parseMemorySize(s: String): ULong? {
            return if (s.isBlank()) null else {
                val numberStr = s.takeWhile { it.isDigit() }
                val number = numberStr.toULongOrNull() ?: return null
                val suffix = s.substringAfter(numberStr).trim().lowercase(Locale.getDefault())

                when (suffix) {
                    "kb" -> number * KiB
                    "k", "kib" -> number * KiB
                    "mb" -> number * KiB * KiB
                    "m", "mib" -> number * KiB * KiB
                    "gb" -> number * KiB * KiB * KiB
                    "g", "gib" -> number * KiB * KiB * KiB
                    else -> null
                }
            }
        }

        fun toHumanReadableSize(l: ULong): String {
            return when {
                l >= KiB * KiB * KiB -> String.format("%.2f GiB", l.toDouble().div((KiB * KiB * KiB).toDouble()))
                l >= KiB * KiB -> String.format("%.2f MiB", l.toDouble().div((KiB * KiB).toDouble()))
                l >= KiB -> String.format("%.2f KiB", l.toDouble().div((KiB).toDouble()))
                else -> l.toString() + "B"
            }
        }
    }
}

object ArtifactExtractorRegistry {
    private val registry = mutableMapOf<KClass<*>, ArtifactExtractor<*>>()

    init {
        registry[SummaryArtifact::class] = SummaryExtractor
        registry[UptimeArtifact::class] = UptimeExtractor
        registry[CommandLineArtifact::class] = CommandLineExtractor
        registry[ArchitectureArtifact::class] = ArchitectureExtractor
        registry[VersionArtifact::class] = VersionExtractor
        registry[HeaderArtifact::class] = HeaderExtractor
        registry[CrashAddressArtifact::class] = CrashAddressExtractor
        registry[InstructionsArtifact::class] = InstructionsExtractor
        registry[RegistersArtifact::class] = RegistersExtractor
        registry[OSArtifact::class] = OSExtractor
        registry[SystemArtifact::class] = SystemExtractor
        registry[SignalInfoArtifact::class] = SignalInfoExtractor
        registry[ThreadInfoArtifact::class] = ThreadInfoExtractor
        registry[ExecutiveSummaryArtifact::class] = ExecutiveSummaryExtractor
        registry[InternalExceptionsArtifact::class] = InternalExceptionsExtractor
        registry[ThreadsArtifact::class] = ThreadsExtractor
        registry[JavaHeapArtifact::class] = JavaHeapExtractor
        registry[EnvVarsArtifact::class] = EnvVarsExtractor
        registry[DynamicLibrariesArtifact::class] = DynamicLibrariesExtractor
        registry[ProcessMemoryArtifact::class] = ProcessMemoryExtractor
    }

    fun get(cls: KClass<*>): ArtifactExtractor<*> {
        return registry.getValue(cls)
    }

    fun forEachArtifact(f: (KClass<Artifact>) -> Unit) {
        for (artifact in registry.keys) {
            f(artifact as KClass<Artifact>)
        }
    }
}