// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.github.mkartashev.hserr.language

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.structureView.*
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement
import com.intellij.ide.util.treeView.smartTree.Sorter
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.Editor
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.github.mkartashev.hserr.language.psi.*

class HsErrStructureViewFactory: PsiStructureViewFactory {
    override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder {
        return object: TreeBasedStructureViewBuilder() {
            override fun createStructureViewModel(editor: Editor?): StructureViewModel {
                return HsErrStructureViewModel(editor, psiFile)
            }
        }
    }
}

class HsErrStructureViewModel(editor: Editor?, psiFile: PsiFile)
    : StructureViewModelBase(psiFile, editor, HsErrStructureViewElement(psiFile))
    , StructureViewModel.ElementInfoProvider {

    override fun getSorters(): Array<Sorter> {
        return arrayOf(Sorter.ALPHA_SORTER)
    }
    override fun isAlwaysShowsPlus(element: StructureViewTreeElement?): Boolean {
        return false
    }

    override fun isAlwaysLeaf(element: StructureViewTreeElement?): Boolean {
        return when (element?.value) {
            is HsErrFile, is HsErrSection -> false
            else -> true
        }
    }

    override fun getSuitableClasses(): Array<Class<out PsiElement>> {
        return arrayOf(HsErrIntro::class.java, HsErrSection::class.java, HsErrSubsection::class.java)
    }
}

class HsErrStructureViewElement(private val element: NavigatablePsiElement): StructureViewTreeElement, SortableTreeElement {
    override fun getPresentation(): ItemPresentation {
        return element.presentation ?: PresentationData()
    }

    override fun getChildren(): Array<TreeElement> {
        return when (element) {
            is HsErrFile -> {
                val subsections = PsiTreeUtil.getChildrenOfAnyType(
                    element,
                    HsErrSection::class.java,
                    HsErrIntro::class.java)

                subsections.map { o -> HsErrStructureViewElement(o as ASTWrapperPsiElement) }.toTypedArray()
            }
            is HsErrSection -> {
                val subsections = PsiTreeUtil.getChildrenOfTypeAsList(element, HsErrSubsection::class.java)
                subsections.map { o -> HsErrStructureViewElement(o as ASTWrapperPsiElement) }.toTypedArray()
            }
            else -> TreeElement.EMPTY_ARRAY
        }
    }

    override fun navigate(requestFocus: Boolean) {
        element.navigate(requestFocus)
    }

    override fun canNavigate(): Boolean {
        return element.canNavigate()
    }

    override fun canNavigateToSource(): Boolean {
        return element.canNavigateToSource()
    }

    override fun getValue() = element

    override fun getAlphaSortKey(): String {
        return element.name ?: ""
    }
}
