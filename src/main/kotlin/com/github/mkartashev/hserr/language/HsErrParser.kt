// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.github.mkartashev.hserr.language

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.FlexAdapter
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import com.github.mkartashev.hserr.language.psi.*

class HsErrLexerAdapter : FlexAdapter(_HsErrLexer(null))

class HsErrParserDefinition : ParserDefinition {

    override fun createLexer(project: Project?): Lexer {
        return HsErrLexerAdapter()
    }

    override fun createParser(project: Project?): PsiParser {
        return HsErrParser()
    }

    override fun getFileNodeType(): IFileElementType {
        return HsErrParserUtils.FILE
    }

    override fun getCommentTokens(): TokenSet {
        return TokenSet.EMPTY
    }

    override fun getStringLiteralElements(): TokenSet {
        return HsErrParserUtils.STRINGS
    }

    override fun createElement(node: ASTNode?): PsiElement {
        return HsErrTypes.Factory.createElement(node)
    }

    override fun createFile(viewProvider: FileViewProvider): PsiFile {
        return HsErrFile(viewProvider)
    }
}

object HsErrParserUtils {
    val FILE = IFileElementType(HsErrLanguage)
    val STRINGS = TokenSet.create(HsErrTypes.STRING)
}