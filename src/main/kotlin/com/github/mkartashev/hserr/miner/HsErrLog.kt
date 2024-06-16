// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.github.mkartashev.hserr.miner

import com.github.mkartashev.hserr.miner.artifact.Artifact
import com.github.mkartashev.hserr.miner.artifact.ArtifactExtractionException
import com.github.mkartashev.hserr.miner.artifact.ArtifactExtractorRegistry
import com.github.mkartashev.hserr.miner.text.Text
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

class HsErrLog(
    text: String,
    private val enableDebug: Boolean = false,
    private val catchAll: Boolean = false
): Text(text) {
    private val artifactByClass : MutableMap<KClass<*>, Artifact?> = mutableMapOf()
    fun <T: Artifact> getArtifact(cls: KClass<T>): T? {
        var result: Artifact? = null
        if (!artifactByClass.containsKey(cls)) {
            // NB: can't use "computeIfAbsent()" because "extract()" may modify
            // the underlying map
            val extractor = ArtifactExtractorRegistry.get(cls)
            try {
                result = extractor.extract(this)
            } catch (e: ArtifactExtractionException) {
                if (enableDebug) {
                    println(e.message)
                }
            } catch (e: Exception) {
                if (!catchAll) throw e
                else println(e.message)
            } finally {
                artifactByClass[cls] = result
            }
        } else {
            result = artifactByClass[cls]
        }

        return  cls.safeCast(result)
    }

    fun allArtifacts(): List<Artifact> {
        val result = mutableListOf<Artifact>()
        ArtifactExtractorRegistry.forEachArtifact {
            val artifact = getArtifact(it)
            if (artifact != null) result.add(artifact)
        }

        return result
    }
}