// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.github.mkartashev.hserr.miner.text

data class TextRange(
    override val start: Text.Cursor,
    override val endInclusive: Text.Cursor
) : ClosedRange<Text.Cursor> {

    init {
        require(start.text == endInclusive.text)
        require(isEmpty() || start <= endInclusive)
    }

    companion object {
        fun of(start: Text.Cursor, endInclusive: Text.Cursor): TextRange {
            require(start.text == endInclusive.text)

            return if (start.isValid() && endInclusive.isValid() && start <= endInclusive) TextRange(start, endInclusive)
            else start.text.emptyRange
        }
    }

    override fun toString(): String {
        return if (isEmpty()) ""
               else start.text.contents.substring(start.offset, endInclusive.offset + 1)
    }

    override fun contains(value: Text.Cursor): Boolean {
        return if (isEmpty()) false else super.contains(value)
    }

    override fun isEmpty() = !start.isValid() || !endInclusive.isValid()

    fun trimRight(amount: Int) = of(start, endInclusive.moveToPrevChar(amount))
    fun trimLeft(amount: Int) = of(start.moveToNextChar(amount), endInclusive)
}