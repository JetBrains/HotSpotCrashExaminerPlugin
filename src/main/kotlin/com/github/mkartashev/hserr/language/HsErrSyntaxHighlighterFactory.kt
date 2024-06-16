// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.github.mkartashev.hserr.language

import com.github.mkartashev.hserr.language.psi.HsErrTypes
import com.intellij.lexer.FlexAdapter
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType

class HsErrSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter {
        return HsErrSyntaxHighlighter
    }
}

object HsErrSyntaxHighlighter : SyntaxHighlighter {
    private val STRING = TextAttributesKey.createTextAttributesKey("HSERR_STRING", DefaultLanguageHighlighterColors.STRING)
    private val NUMBER = TextAttributesKey.createTextAttributesKey("HSERR_NUMBER", DefaultLanguageHighlighterColors.NUMBER)
    private val WORD = TextAttributesKey.createTextAttributesKey("HSERR_WORD", DefaultLanguageHighlighterColors.LINE_COMMENT)
    private val PUNCT = TextAttributesKey.createTextAttributesKey("HSERR_PUNCT", DefaultLanguageHighlighterColors.COMMA)
    private val SECTION_HDR = TextAttributesKey.createTextAttributesKey("HSERR_SECTION_HDR", DefaultLanguageHighlighterColors.CLASS_NAME)
    private val SUBTITLE = TextAttributesKey.createTextAttributesKey("HSERR_SUBTITLE", DefaultLanguageHighlighterColors.INSTANCE_FIELD)
    private val SIGNAL = TextAttributesKey.createTextAttributesKey("HSERR_SIGNAL", DefaultLanguageHighlighterColors.GLOBAL_VARIABLE)
    private val URL = TextAttributesKey.createTextAttributesKey("HSERR_URL", DefaultLanguageHighlighterColors.DOC_COMMENT_TAG)
    private val KEYWORD = TextAttributesKey.createTextAttributesKey("HSERR_KEYWORD", DefaultLanguageHighlighterColors.IDENTIFIER)
    private val IDENTIFIER = TextAttributesKey.createTextAttributesKey("HSERR_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER)
    private val REGISTER = TextAttributesKey.createTextAttributesKey("HSERR_REGISTER", DefaultLanguageHighlighterColors.GLOBAL_VARIABLE)

    override fun getHighlightingLexer(): Lexer {
        return FlexAdapter(_HsErrLexer(null))
    }

    private val typeToTextAttr: Map<IElementType, Array<TextAttributesKey>> = mapOf(
        HsErrTypes.STRING to arrayOf(STRING),
        HsErrTypes.NUMBER to arrayOf(NUMBER),
        HsErrTypes.WORD to arrayOf(WORD),
        HsErrTypes.SECTION_HDR to arrayOf(SECTION_HDR),
        HsErrTypes.SUBTITLE to arrayOf(SUBTITLE),
        HsErrTypes.KEYWORD to arrayOf(KEYWORD),
        HsErrTypes.SIGNAL to arrayOf(SIGNAL),
        HsErrTypes.URL to arrayOf(URL),
        HsErrTypes.IDENTIFIER to arrayOf(IDENTIFIER),
        HsErrTypes.REGISTER to arrayOf(REGISTER),
        HsErrTypes.PUNCT to arrayOf(PUNCT))

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> {
        return typeToTextAttr[tokenType] ?: return TextAttributesKey.EMPTY_ARRAY
    }
}