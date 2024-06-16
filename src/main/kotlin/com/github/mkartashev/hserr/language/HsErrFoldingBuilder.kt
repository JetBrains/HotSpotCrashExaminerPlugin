// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.github.mkartashev.hserr.language

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.FoldingGroup
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.util.containers.toArray
import com.github.mkartashev.hserr.language.psi.*
import com.github.mkartashev.hserr.settings.SettingsStore
import com.github.mkartashev.hserr.startsWithAny

class HsErrFoldingBuilder : FoldingBuilderEx(), DumbAware {
    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val descriptors: MutableList<FoldingDescriptor> = mutableListOf()

        root.accept(object : PsiRecursiveElementWalkingVisitor() {
            var group: FoldingGroup? = null

            override fun visitElement(element: PsiElement) {
                super.visitElement(element)

                if (!element.isValid || element.textRange.length <= 2) {
                    group = null
                    return
                }

                when (element) {
                    is HsErrIntro, is HsErrSection -> {
                        group = FoldingGroup.newGroup("hserr section")
                        descriptors.add(
                            FoldingDescriptor(
                                element.node,
                                element.textRange.shrinkBy(1),
                                group
                            )
                        )
                    }
                    is HsErrSubsection -> {
                        descriptors.add(
                            FoldingDescriptor(
                                element.node,
                                element.textRange.shrinkBy(1),
                                group
                            )
                        )
                    }
                    else -> {
                        group = null
                    }
                }
            }
        })

        return descriptors.toArray(FoldingDescriptor.EMPTY)
    }

    override fun getPlaceholderText(node: ASTNode): String? {
        return when (node.psi) {
            is HsErrIntro -> "# INTRO"
            is HsErrSection -> (node.psi as HsErrSection).presentation.presentableText
            is HsErrSubsection -> (node.psi as HsErrSubsection).presentation.presentableText
            else -> null
        }
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean {
        return when (node.psi) {
            is HsErrSection -> {
                val name = (node.psi as HsErrSection).presentation.presentableText
                name?.startsWithAny(collapsedPatterns())  ?: false
            }
            is HsErrSubsection -> {
                val name = (node.psi as HsErrSubsection).presentation.presentableText
                name?.startsWithAny(collapsedPatterns())  ?: false
            }
            else -> false
        }
    }

    private fun collapsedPatterns(): List<String> {
        val patternsString = SettingsStore.getInstance().theState.patternsToFold
        return patternsString.split(',').map { it.trim() }
    }
}

private fun TextRange.shrinkBy(i: Int): TextRange {
    return TextRange(this.startOffset, this.endOffset - i)
}
