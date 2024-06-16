// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.github.mkartashev.hserr.miner.text

import java.util.*

open class Text(val contents: String) {
    // Maps offset within 'contents' of the first character in a line to the line number
    private val offset2LineMap = TreeMap<Int, Int>()

    init {
        var line = 0
        var isNewLine = true
        for ((i, c) in contents.withIndex()) {
            if (isNewLine) offset2LineMap[i] = line++
            isNewLine = (c == '\n')
        }
    }

    val start by lazy {
        if (contents.isEmpty()) pastEnd
        else Cursor(0)
    }

    val end by lazy {
        if (contents.isEmpty()) pastEnd
        else Cursor(contents.length - 1)
    }

    val pastEnd by lazy { Cursor(-1) }

    val emptyRange = TextRange(pastEnd, pastEnd)

    fun charAt(cursor: Cursor) = if (cursor.isValid()) contents[cursor.offset] else Char.MIN_VALUE

    fun lineAsTokens(l: Cursor): List<TextRange> {
        if (l.isAtEnd() || !l.isValid()) return emptyList()

        val result = mutableListOf<TextRange>()
        var cur = l.moveToNonWhitespace()
        while (cur.isValid() && !cur.isAtEnd() && cur.line == l.line) {
            val next = cur.moveToWordEnd()
            val s = TextRange.of(cur, next)
            if (!s.isEmpty()) result.add(s) else break
            cur = next.moveToNextChar().moveToNonWhitespace()
        }

        return result
    }

    fun cursorAt(offset: Int): Cursor {
        return if (offset < 0 || offset >= contents.length) pastEnd else Cursor(offset)
    }

    inner class Cursor(val offset: Int) : Comparable<Cursor> {
        private val coords: Coordinates by lazy { computeCoordinates() }

        init {
            require(offset == -1 || offset < contents.length)
        }

        val line: Int
            get() = coords.line
        val col: Int
            get() = coords.col

        val text: Text
            get() = this@Text

        private fun computeCoordinates(): Coordinates {
            if (offset < 0) return Coordinates.INVALID

            val (lineStart, lineNum) = offset2LineMap.floorEntry(offset) ?: return Coordinates.INVALID
            return Coordinates(lineNum, offset - lineStart)
        }

        override fun toString(): String {
            return "[${coords.line}, ${coords.col}]"
        }

        override fun compareTo(other: Cursor) = offset.compareTo(other.offset)

        fun isValid() = offset != -1

        fun isAtEnd() = offset + 1 == contents.length

        fun moveToNextChar(count: Int = 1): Cursor {
            require(count > 0)
            if (!isValid()) return this
            return cursorAt(offset + count)
        }

        fun moveToPrevChar(count: Int = 1): Cursor {
            if (!isValid()) return this
            return cursorAt(offset - count)
        }

        fun moveToNextLine(): Cursor {
            return moveToLineEnd(false).moveToNextChar()
        }

        fun moveToPrevLine(): Cursor {
            if (!isValid()) return this
            if (offset == 0) return this
            val thisLineStartOffset = contents.lastIndexOf('\n', offset - 1)
            return if (thisLineStartOffset == -1) start else cursorAt(thisLineStartOffset).moveToLineStart()
        }

        fun moveToLineStart(): Cursor {
            if (!isValid()) return this
            if (offset == 0 || contents[offset - 1] == '\n') return this
            val thisLineStartOffset = contents.lastIndexOf('\n', offset - 1)
            return if (thisLineStartOffset == -1) start else cursorAt(thisLineStartOffset + 1)
        }

        fun moveToLineEnd(excludeLineBreak: Boolean = true): Cursor {
            if (!isValid()) return this
            val atLineEnd = contents[offset] == '\n'
            if (atLineEnd) return this
            val thisLineEndOffset = contents.indexOf('\n', offset + 1)
            return if (thisLineEndOffset == -1) end
            else cursorAt(thisLineEndOffset - (if (excludeLineBreak) 1 else 0))
        }

        fun moveToNextWord(): Cursor =
            moveToWordEnd().moveToNextChar().moveToNonWhitespace()

        fun moveToWordEnd(): Cursor {
            if (!isValid()) return this
            var i = offset
            while (i < contents.length && !contents[i].isWhitespace()) i++
            return if (i >= contents.length) end else cursorAt(i - 1)
        }

        fun moveToNonWhitespace(): Cursor {
            if (!isValid()) return this
            var i = offset
            while (i < contents.length && contents[i].isWhitespace()) i++
            return if (i >= contents.length) end else cursorAt(i)
        }

        fun moveToString(what: String, stopAtBeginning: Boolean = true): Cursor {
            return if (!isValid()) pastEnd
            else {
                val i = contents.indexOf(what, offset)
                if (i < 0) pastEnd else cursorAt(i + if (stopAtBeginning) 0 else what.length)
            }
        }

        fun moveToLastStringInLine(what: String, stopAtBeginning: Boolean = true): Cursor {
            return if (!isValid()) pastEnd
            else {
                val line = contents.substring(offset).substringBefore('\n')
                val i = line.lastIndexOf(what) + offset
                if (i < offset) pastEnd else cursorAt(i + if (stopAtBeginning) 0 else what.length)
            }
        }

        fun moveToLineWith(what: String): Cursor {
            return moveToString(what).moveToLineStart()
        }

        fun moveToLineStartsWithString(what: String): Cursor {
            return if (contents.startsWith(what)) start
                   else moveToString("\n" + what).moveToNextChar()
        }

        fun selectUpToEOL(): TextRange {
            return TextRange.of(this, moveToLineEnd())
        }

        fun selectCurrentWord(): TextRange {
            return TextRange.of(this, moveToWordEnd())
        }

        fun selectQuotedString(includeQuotes: Boolean = true): TextRange {
            val first = this
            var last = first.moveToNextChar()
            while (last.isValid() && !last.isAtEnd()) {
                val c = charAt(last)
                if (c == '\'') {
                    last = last.moveToNextChar()
                } else if (c == '"' || c == '\n') break
                last = last.moveToNextChar()
            }

            var result = TextRange.of(first, last)
            if (!includeQuotes) {
                if (charAt(first) == '"') result = result.trimLeft(1)
                if (charAt(last) == '"') result = result.trimRight(1)
            }
            return result
        }

        fun selectCurrentHexNumber(): TextRange {
            var lastDigit = this
            if (charAt(lastDigit) == '0') lastDigit = lastDigit.moveToNextChar()
            if (charAt(lastDigit).equals('x', true)) lastDigit = lastDigit.moveToNextChar()
            var char = charAt(lastDigit)
            while (char.isHexDigit()){
                lastDigit = lastDigit.moveToNextChar()
                char = charAt(lastDigit)
            }

            return TextRange.of(this, lastDigit.moveToPrevChar())
        }

        fun selectUpToLineWith(what: String, includeThatLine: Boolean = false): TextRange {
            val lastLine = moveToNextLine().moveToLineWith(what)
            val end = lastLine.moveToPrevLine().moveToLineEnd()
            return selectUpTo(if (includeThatLine) lastLine.moveToLineEnd() else end)
        }

        fun selectUpToFirstEmptyLine(): TextRange {
            val lastLine = moveToLineStartsWithString("\n").moveToPrevChar()
            return selectUpTo(if (lastLine.isValid()) lastLine else end)
        }


        fun selectLinesWhile(p: (String) -> Boolean): TextRange {
            var lastGoodLine = this
            var currentLine = this
            while (currentLine.isValid() && !currentLine.isAtEnd()) {
                val str = currentLine.selectUpToEOL()
                if (!p(str.toString())) break
                lastGoodLine = currentLine
                currentLine = currentLine.moveToNextLine()
            }

            lastGoodLine = lastGoodLine.moveToLineEnd()
            return if (lastGoodLine != this) selectUpTo(lastGoodLine) else TextRange.of(pastEnd, pastEnd)
        }

        fun selectUpTo(endInclusive: Cursor): TextRange {
            return TextRange.of(this, if (endInclusive.isValid()) endInclusive else end)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Cursor

            return offset == other.offset
        }

        override fun hashCode(): Int {
            return offset
        }
    }
}

fun Char.isHexDigit(): Boolean {
    return isDigit() || equals('a', true) || equals('b', true)
            || equals('c', true) || equals('d', true) || equals('e', true)
            || equals('f', true)
}

data class Coordinates(val line: Int, val col: Int) {
    companion object {
        val INVALID = Coordinates(-1, -1)
    }
}
