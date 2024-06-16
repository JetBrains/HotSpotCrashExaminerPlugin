// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.github.mkartashev.hserr.miner.artifact

import com.github.mkartashev.hserr.miner.HsErrLog
import com.github.mkartashev.hserr.miner.text.TextRange
import com.github.mkartashev.hserr.miner.text.isHexDigit

class ThreadInfoArtifact(
    log: HsErrLog,
    textRange: TextRange,
    val name: String,
    val stack: Stack,
    val isNative: Boolean,
    val isCompilerThread: Boolean,
    val methodBeingCompiled: String
) : Artifact(log) {

    override val title: String = "Thread"

    override val locations = setOf(textRange)

    override val comment by lazy {
        val javaFramesCount = stack.frames.count { it.isJavaCode() }
        val nativeFramesCount = stack.frames.count { it.isNativeCode() }
        val vmFramesCount = stack.frames.count { it.isVMCode() }

        val suffix = if (nativeFramesCount == 0 && vmFramesCount == 0) ", all Java,"
        else if (nativeFramesCount == 0 && javaFramesCount == 0) ", all VM,"
        else if (vmFramesCount == 0 && javaFramesCount == 0) ", all native,"
        else if (javaFramesCount > nativeFramesCount && javaFramesCount > vmFramesCount) " mostly Java"
        else if (nativeFramesCount > javaFramesCount && nativeFramesCount > vmFramesCount) " mostly native"
        else if (vmFramesCount > javaFramesCount && vmFramesCount > nativeFramesCount) " mostly VM"
        else ""

        "named \"${name}\" with ${stack.frames.size}${suffix} frames"
    }

}

class Stack(frameLines: List<String>) {
    class Frame(val number: Int,
                val kind: Char,
                val function: String, val functionOffset: Int,
                val module: String, val moduleOffset: Int,
                val compiledBy: String) {

        fun isVMCode() = kind == 'V' || kind == 'v'
        fun isNativeCode() = kind == 'C'
        fun isJavaCode() = kind == 'j' || kind == 'J' || kind == 'A'
        fun isInterpretedCode() = kind == 'j'
    }

    val frames: List<Frame> by lazy {
        val result = mutableListOf<Frame>()

        var number = 0
        for (line in frameLines) {
            val kind: Char = line.trimStart().first()
            val remainder = line.trimStart().substring(1).trim()
            var module: String = Artifact.DEFAULT_STRING
            var moduleOffset: Int = Artifact.DEFAULT_INT
            var function: String = Artifact.DEFAULT_STRING
            var functionOffset: Int = Artifact.DEFAULT_INT
            var compiledBy: String = Artifact.DEFAULT_STRING

            when (kind) {
                'v' -> {
                    // v  ~StubRoutines::call_stub
                    module = "hotspot"
                    function = remainder.substringBefore(' ').trim()
                    result.add(Frame(number, kind, function, functionOffset, module, moduleOffset, compiledBy))
                }

                'V', 'C' -> {
                    // V  [libjvm.so+0x441ab0]  AccessInternal::PostRuntimeDispatch<G1BarrierSet::AccessBarrier<1335398ul, G1BarrierSet>, (AccessInternal::BarrierType)3, 1335398ul>::oop_access_barrier(oopDesc*, long)+0x0
                    // V  [jvm.dll+0x6a7d2b]  PhaseIterGVN::remove_speculative_types+0x4b  (phaseX.cpp:1684)
                    // C  [libc.so.6+0x9d37e]  __libc_malloc+0x11e
                    // V (00000001`8010c4e0) jvm!JVM_EnqueueOperation+0x13cbd5 | (00000001`80249190) jvm!blob jvm.dll
                    // V  [jvm.dll+0x38baca] --> (00000001`8038ba90)   jvm!PhaseCFG::do_global_code_motion+0x3a   |  (00000001`8038bb00)   jvm!PhaseCFG::estimate_block_frequency
                    if (remainder.contains("-->") || remainder.startsWith('(')) {
                        // Can have two candidate frames
                        if (remainder.startsWith('[')) {
                            val pm = splitModuleNameAndOffset(remainder.substringAfter('[', "").substringBefore(']', ""))
                            module = pm.first
                            moduleOffset = pm.second
                        }
                        val r = if (remainder.startsWith('(')) remainder.substringAfter( ' ').trim()
                        else remainder.substringAfter("-->").trim().substringAfter( ' ').trim()

                        val f1 = r.substringBefore('|').trim()
                        var f2 = r.substringAfter('|', "").trim()
                        if (f2.startsWith('(')) f2 = f2.substringAfter(')', "").trim()
                        f2 = f2.substringBefore(' ')
                        val pf = splitNameAndOffset(f1)
                        function = pf.first.substringAfter('!')
                        if (pf.first.contains('!')) {
                            module = pf.first.substringBefore('!')
                            moduleOffset = Artifact.DEFAULT_INT
                        }
                        functionOffset = pf.second
                        result.add(Frame(number, kind, function, functionOffset, module, moduleOffset, compiledBy))
                        if (f2.isNotEmpty()) {
                            val pf2 = splitNameAndOffset(f2)
                            function = pf2.first.substringAfter('!')
                            if (pf.first.contains('!')) {
                                module = pf2.first.substringBefore('!')
                                moduleOffset = Artifact.DEFAULT_INT
                            }
                            functionOffset = pf2.second
                            result.add(Frame(number, kind, function, functionOffset, module, moduleOffset, compiledBy))
                        }

                    } else if (remainder.contains("] ") || remainder.endsWith("]")) {
                        val s = remainder.substringAfter("] ", Artifact.DEFAULT_STRING).trimStart()
                        val f = if (s.endsWith(")")) s.substringBeforeLast("(").trimEnd()
                                else s
                        val pf = splitNameAndOffset(f)
                        function = pf.first
                        functionOffset = pf.second
                        val m = remainder.substringAfter('[', "").substringBefore(']', "")
                        val pm = splitModuleNameAndOffset(m)
                        module = pm.first
                        moduleOffset = pm.second
                        result.add(Frame(number, kind, function, functionOffset, module, moduleOffset, compiledBy))
                    } else {
                        val f = remainder.substringBefore(' ')
                        module = remainder.substringAfter(' ', "").trim()
                        val p = splitNameAndOffset(f)
                        function = p.first
                        functionOffset = p.second
                        result.add(Frame(number, kind, function, functionOffset, module, moduleOffset, compiledBy))
                    }
                }

                'j' -> {
                    // j  org.cef.browser.CefBrowser_N.N_SetParent(JLjava/awt/Component;)V+0 jcef@11.0.12
                    // j  com.jetbrains.ui.tabs.impl.JBTabsImpl.addNotify()V+1
                    val f = remainder.substringBefore(' ')
                    val pf = splitNameAndOffset(f)
                    function = pf.first
                    functionOffset = pf.second
                    val m = remainder.substringAfterLast(' ', "")
                    val pm = splitModuleNameAndOffset(m)
                    module = pm.first
                    moduleOffset = pm.second
                    result.add(Frame(number, kind, function, functionOffset, module, moduleOffset, compiledBy))
                }

                'J' -> {
                    // J 65922 c2 java.awt.Container.addNotify()V java.desktop@11.0.12 (81 bytes) @ 0x00007f23355fc834 [0x00007f23355fc6c0+0x0000000000000174]
                    // J c2 java.awt.Container.addNotify()V java.desktop@11.0.12 (81 bytes) @ 0x00007f23355fc834 [0x00007f23355fc6c0+0x0000000000000174]
                    // J 49362% c2 java.awt.EventDispatchThread.pumpEventsForFilter(ILjava/awt/Conditional;Ljava/awt/EventFilter;)V java.desktop@11.0.12 (47 bytes) @ 0x00007f2335fa2884 [0x00007f2335fa2700+0x0000000000000184]
                    val r = remainder.substringAfter(' ', Artifact.DEFAULT_STRING).trimStart()
                    val suspectCompiledBy = r.substringBefore(' ')
                    val f = if (suspectCompiledBy.length <= 2) r.substringAfter(' ', Artifact.DEFAULT_STRING).trimStart()
                                    .substringBefore(' ', Artifact.DEFAULT_STRING)
                            else suspectCompiledBy
                    val pf = splitNameAndOffset(f)
                    function = pf.first
                    functionOffset = pf.second
                    val m = if (suspectCompiledBy.length <= 2) remainder.substringAfter(' ', "").trimStart()
                                    .substringAfter(' ', "").trimStart()
                                    .substringAfter(' ', "").trimStart().substringBefore(' ')
                            else remainder.substringAfter(' ', "").trimStart()
                                    .substringAfter(' ', "").trimStart().substringBefore(' ')
                    val pm = splitModuleNameAndOffset(m)
                    module = pm.first
                    moduleOffset = pm.second
                    if (suspectCompiledBy.length == 2) compiledBy = suspectCompiledBy
                    result.add(Frame(number, kind, function, functionOffset, module, moduleOffset, compiledBy))
                }
            }
            number++
        }

        result
    }

    fun frame(n: Int) : Frame {
        return frames.first { it.number == n }
    }

    fun topFrame() = frames.first()
}

internal object ThreadInfoExtractor : ArtifactExtractor<ThreadInfoArtifact> {
    override fun extract(log: HsErrLog): ThreadInfoArtifact {
        val threadSectionStart = log.start.moveToLineStartsWithString("---------------  T H R E A D")
        if (!threadSectionStart.isValid()) fail("Couldn't find THREAD section marker")

        val currentThreadLine = threadSectionStart.moveToLineStartsWithString("Current thread").selectUpToEOL()
        if (currentThreadLine.isEmpty()) fail("Couldn't locate info about the current thread")

        val isThreadNative = currentThreadLine.toString().startsWith("Current thread is native thread")
        var threadName = ""
        if (!isThreadNative) {
            val threadNameSelection =
                currentThreadLine.start.moveToNextWord().moveToNextWord().moveToNextWord().moveToNextWord()
                    .selectQuotedString(false)
            threadName = threadNameSelection.toString()
        }

        val compileTaskLine = threadSectionStart.moveToLineStartsWithString("Current CompileTask:")
        val isCompilerThread = compileTaskLine.isValid()
        var methodBeingCompiled = ""
        if (isCompilerThread) {
            val tokens = log.lineAsTokens(compileTaskLine.moveToNextLine())
            if (tokens.size > 3) {
                val methodSelection = tokens[tokens.lastIndex - 2]
                methodBeingCompiled = methodSelection.toString()
            }
        }

        val stackStart = threadSectionStart.moveToLineStartsWithString("Stack: ")
        var stackEnd = stackStart
        var emptyLinesCount = 0
        while (stackEnd.isValid() && !stackEnd.isAtEnd() && emptyLinesCount < 2) {
            stackEnd = stackEnd.moveToNextLine()
            val currentLine = stackEnd.selectUpToEOL()
            if (currentLine.toString().isBlank()) emptyLinesCount += 1 else emptyLinesCount = 0
        }

        if (!stackEnd.isValid()) stackEnd = log.end

        val stackSelection = stackStart.selectUpTo(stackEnd)
        val frameLines = stackSelection.toString().lines().filter {
            val c = if (it.isEmpty()) ' ' else it[0]
            when (c) {
                'v', 'V', 'j', 'J', 'C', 'A' -> it.length > 2 && it[1].isWhitespace()
                else -> false
            }
        }

        if (frameLines.isEmpty()) fail("No frames in stack")

        val sel = threadSectionStart.selectUpTo(stackEnd)
        return ThreadInfoArtifact(
            log, sel, threadName, Stack(frameLines),
            isThreadNative, isCompilerThread, methodBeingCompiled
        )
    }
}

private fun splitNameAndOffset(f: String): Pair<String, Int> {
    if (f.contains('+')) {
        var offsetStartIdx = f.lastIndexOf("+0x")
        var offsetDigitsIdx = 0
        if (offsetStartIdx == -1) {
            offsetStartIdx = f.lastIndexOf('+')
            offsetDigitsIdx = offsetStartIdx + 1
        } else {
            offsetDigitsIdx += offsetStartIdx + 3
        }

        for (i in offsetDigitsIdx until f.length) {
            val c = f[i]
            if (!c.isHexDigit()) {
                offsetStartIdx = -1
                break
            }
        }
        val name = if (offsetStartIdx > 0) f.substring(0, offsetStartIdx) else f
        val offset = if (offsetStartIdx > 0) {
            ArtifactExtractor.parseInt(f.substring(offsetStartIdx))
        } else Artifact.DEFAULT_INT

        return name to offset
    } else {
        return f to Artifact.DEFAULT_INT
    }
}

private fun splitModuleNameAndOffset(m: String): Pair<String, Int> {
    if (m.isBlank() || !m[0].isJavaIdentifierStart()) {
        return Artifact.DEFAULT_STRING to Artifact.DEFAULT_INT
    } else if (m.contains('@')) {
        val pm = splitNameAndOffset(m)
        return pm.first.substringBefore('@') to pm.second
    } else if (m.contains('+')) {
        val pm = splitNameAndOffset(m)
        return pm.first.substringBefore('+') to pm.second
    }

    return m to Artifact.DEFAULT_INT
}