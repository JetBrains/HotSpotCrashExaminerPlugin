// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.github.mkartashev.hserr.actions

import com.github.mkartashev.hserr.getHsErrLogCached
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import com.intellij.psi.util.elementType
import com.intellij.ui.JBColor
import com.github.mkartashev.hserr.isHsErr
import com.github.mkartashev.hserr.language.psi.HsErrTypes
import com.github.mkartashev.hserr.miner.artifact.DynamicLibrariesArtifact
import com.github.mkartashev.hserr.miner.artifact.ThreadsArtifact
import com.github.mkartashev.hserr.miner.text.TextRange
import com.github.mkartashev.hserr.parseAsAddress
import com.github.mkartashev.hserr.selectAddrKey
import com.github.mkartashev.hserr.settings.SettingsStore
import java.awt.Color
import java.awt.Font

class FindAddrAction : AnAction() {
    override fun update(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: return

        e.presentation.isEnabled = isHsErr(psiFile) && getAddressOrNull(editor, psiFile) != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: return
        val vf = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        val addr = getAddressOrNull(editor, psiFile) ?: return

        editor.markupModel.removeAllHighlighters()

        highlightNearAddresses(psiFile, editor, addr)
        highlightStructured(project, vf, editor, addr)

        vf.putUserData(selectAddrKey, addr)
    }

    private fun getAddressOrNull(editor: Editor, psiFile: PsiFile): ULong? {
        val selectionModel = editor.selectionModel
        if (selectionModel.hasSelection() && selectionModel.selectedText != null) {
            try {
                return parseAsAddress(selectionModel.selectedText!!)
            } catch (ignored: NumberFormatException) {
            }
        }

        val caretOffset = editor.caretModel.offset
        val wordAtCaret = psiFile.findElementAt(caretOffset)?.text ?: return null
        try {
            return parseAsAddress(wordAtCaret)
        } catch (ignored: NumberFormatException) {
        }

        return null
    }

    private fun highlightStructured(
        project: Project,
        f: VirtualFile,
        editor: Editor,
        addr: ULong
    ) {

        val log = getHsErrLogCached(project, f) ?: return
        val threads = log.getArtifact(ThreadsArtifact::class)
        if (threads != null) {
            val thread = threads.byAddress(addr)
            if (thread != null) {
                highlightRange(thread.location, editor)
            }
            val stack = threads.byStackAddress(addr)
            if (stack != null) {
                highlightRange(stack.location, editor)
            }
        }

        val libs = log.getArtifact(DynamicLibrariesArtifact::class)
        if (libs != null) {
            val lib = libs.byAddress(addr)
            if (lib != null) {
                highlightRange(lib.location, editor)
            }
        }
    }

    private fun baseHighlighterColor() = SettingsStore.getInstance().theState.findAddrColor

    private fun highlighterColorNear() =
        if (JBColor.isBright()) baseHighlighterColor().brighter().brighter()
        else baseHighlighterColor().darker()

    private fun highlighterColorFar() =
        if (JBColor.isBright()) highlighterColorNear().brighter().brighter()
        else highlighterColorNear().darker()

    private fun highlighterColorVeryFar() =
        if (JBColor.isBright()) highlighterColorFar().brighter().brighter()
        else highlighterColorFar().darker()


    private val rangeHighlighterAttributes by lazy {
        TextAttributes(
            null, highlighterColorNear(),
            null,
            null,
            Font.PLAIN
        )
    }

    private fun highlightRange(range: TextRange, editor: Editor) {
        val from = range.start.offset
        val to = range.endInclusive.offset + 1
        if (withinRange(from, to, editor)) {
            addHighlighter(editor, from, to, rangeHighlighterAttributes)
        }
    }

    private fun withinRange(start: Int, end: Int, editor: Editor) =
        start < editor.document.textLength && end <= editor.document.textLength

    private fun highlightNearAddresses(
        psiFile: PsiFile,
        editor: Editor,
        addr: ULong
    ) {
        psiFile.accept(object : PsiRecursiveElementWalkingVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element.elementType == HsErrTypes.NUMBER) {
                    val elementAddr = parseAsAddress(element.text)
                    if (elementAddr != null) {
                        val distance =
                            if (addr >= elementAddr) addr.minus(elementAddr) else elementAddr.minus(addr)
                        if (withinRange(distance)) {
                            val attrs = attributesForDistance(distance)
                            highlightElement(element, editor, attrs)
                        }
                    }
                }
                super.visitElement(element)
            }
        })
    }

    private fun withinRange(distance: ULong) = distance < SettingsStore.getInstance().theState.findAddrRange

    private fun distanceToPercent(distance: ULong): Int =
        distance.toDouble().div(SettingsStore.getInstance().theState.findAddrRange.toDouble())
            .times(100).toInt()


    private fun highlighterColorForDistance(distance: ULong): Color {
        val p = distanceToPercent(distance)
        return when {
            distance == 0UL -> baseHighlighterColor()
            p <= 10 -> highlighterColorNear()
            p <= 50 -> highlighterColorFar()
            else -> highlighterColorVeryFar()
        }
    }

    private fun attributesForDistance(distance: ULong) = TextAttributes(
        null, highlighterColorForDistance(distance),
        null,
        null,
        Font.PLAIN
    )

    private fun highlightElement(
        element: PsiElement,
        editor: Editor,
        attrs: TextAttributes
    ) {
        val from = element.textRange.startOffset
        val to = element.textRange.endOffset
        addHighlighter(editor, from, to, attrs)
    }

    private fun addHighlighter(
        editor: Editor,
        from: Int,
        to: Int,
        attrs: TextAttributes
    ) {
        val h = editor.markupModel.addRangeHighlighter(
            from, to,
            HighlighterLayer.WARNING,
            attrs,
            HighlighterTargetArea.EXACT_RANGE
        )
        h.errorStripeMarkColor = baseHighlighterColor()
    }
}
