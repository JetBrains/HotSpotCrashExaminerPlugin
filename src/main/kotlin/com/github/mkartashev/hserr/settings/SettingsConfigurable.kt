// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.github.mkartashev.hserr.settings

import com.github.mkartashev.hserr.HsErrBundle
import com.intellij.openapi.options.BaseConfigurable
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.Graphics
import javax.swing.JButton
import javax.swing.JColorChooser
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.DocumentEvent
import javax.swing.text.PlainDocument
import javax.swing.text.SimpleAttributeSet

class SettingsConfigurable : BaseConfigurable() {
    private val store = SettingsStore.getInstance()
    private var myState: SettingsStore.State = store.theState.clone()
    private var patternsDoc = PlainDocument()

    override fun createComponent(): JComponent? {
        val patternsEditor = patternsEditorPanel()
        val selectAddr = selectAddrPanel()
        val limitsPanel = createLimitsPanel()
        return FormBuilder().run {
            addComponent(patternsEditor)
            addComponent(selectAddr)
            addComponent(limitsPanel)
            addComponentFillVertically(JPanel(), 0)
            panel
        }
    }

    private fun selectAddrPanel(): JPanel {
        val rangeText = JBTextField(myState.findAddrRange.toString()).apply {
            emptyText.text = SettingsStore.getInstance().theState.findAddrRange.toString()
            document.addDocumentListener(object : DocumentAdapter() {
                override fun textChanged(e: DocumentEvent) {
                    val newVal = e.document.getText(0, e.document.length).toULongOrNull()
                    if (newVal != null && newVal > 0u) {
                        myState.findAddrRange = newVal
                        this@apply.foreground = JBUI.CurrentTheme.Label.foreground()
                    } else {
                        this@apply.foreground = JBColor.RED
                    }
                }
            })
        }

        val initialColor = myState.findAddrColor
        val colorButton = object : JButton() {
            override fun paint(g: Graphics) {
                val color = g.color
                g.color = background
                g.fillRect(0, 0, width, height)
                g.color = color
            }

            override fun getPreferredSize() = JBUI.size(14)
            override fun getMinimumSize() = preferredSize
            override fun getMaximumSize() = preferredSize
        }.apply {
            size = JBUI.size(12)
            background = initialColor
            if (SystemInfo.isMac) {
                putClientProperty("JButton.buttonType", "square")
            }
            addActionListener {
                val color = JColorChooser.showDialog(this, "Select color", initialColor)
                if (color != null) {
                    background = color
                    myState.findAddrColorRGB = color.rgb
                }
            }
        }

        return FormBuilder().run {
            panel.border = IdeBorderFactory.createTitledBorder(HsErrBundle.message("findAddrTitle"))
            addLabeledComponent(HsErrBundle.message("findAddrRangeTitle"), rangeText)
            addLabeledComponent(HsErrBundle.message("findAddrColor"), colorButton)
            panel
        }
    }

    private fun toMb(value: ULong) : ULong = value / (1024u * 1024u)

    private fun patternsEditorPanel(): JPanel {
        val patternsEditor = JBTextArea(patternsDoc, myState.patternsToFold, 3, 40).apply {
            border = IdeBorderFactory.createRoundedBorder()
            emptyText.text = "PROCESS, Dynamic libraries, Heap"
            document.addDocumentListener(object : DocumentAdapter() {
                override fun textChanged(e: DocumentEvent) {
                    val doc = e.document
                    myState.patternsToFold = doc.getText(0, doc.length)
                }
            })
        }

        val panel = FormBuilder().run {
            panel.border = IdeBorderFactory.createTitledBorder(HsErrBundle.message("patternsEditorTitle"))
            addComponent(patternsEditor)
            addComponent(
                JBLabel(
                    HsErrBundle.message("patternsEditorTooltip"),
                    UIUtil.ComponentStyle.SMALL,
                    UIUtil.FontColor.BRIGHTER
                )
            )
            panel
        }
        return panel
    }

    private fun createLimitsPanel(): JPanel {
        val threadsField = JBTextField(myState.maxThreads.toString()).apply {
            emptyText.text = SettingsStore.getInstance().theState.maxThreads.toString()
            document.addDocumentListener(object : DocumentAdapter() {
                override fun textChanged(e: DocumentEvent) {
                    val newVal = e.document.getText(0, e.document.length).toIntOrNull()
                    if (newVal != null && newVal > 0) {
                        myState.maxThreads = newVal
                        this@apply.foreground = JBUI.CurrentTheme.Label.foreground()
                    } else {
                        this@apply.foreground = JBColor.RED
                    }
                }
            })
        }

        val stacksField = JBTextField(toMb(myState.maxStacksSize).toString()).apply {
            emptyText.text = (toMb(SettingsStore.getInstance().theState.maxStacksSize)).toString()
            document.addDocumentListener(object : DocumentAdapter() {
                override fun textChanged(e: DocumentEvent) {
                    val newVal = e.document.getText(0, e.document.length).toULongOrNull()
                    if (newVal != null && newVal > 0u) {
                        myState.maxStacksSize = newVal * 1024U * 1024U
                        this@apply.foreground = JBUI.CurrentTheme.Label.foreground()
                    } else {
                        this@apply.foreground = JBColor.RED
                    }
                }
            })
        }

        val memField = JBTextField(myState.minFreeMemPercent.toString()).apply {
            emptyText.text = SettingsStore.getInstance().theState.minFreeMemPercent.toString()
            document.addDocumentListener(object : DocumentAdapter() {
                override fun textChanged(e: DocumentEvent) {
                    val newVal = e.document.getText(0, e.document.length).toIntOrNull()
                    if (newVal != null && newVal >= 0 && newVal <= 100) {
                        myState.minFreeMemPercent = newVal
                        this@apply.foreground = JBUI.CurrentTheme.Label.foreground()
                    } else {
                        this@apply.foreground = JBColor.RED
                    }
                }
            })
        }

        val heapField = JBTextField(myState.minFreeHeapPercent.toString()).apply {
            emptyText.text = SettingsStore.getInstance().theState.minFreeHeapPercent.toString()
            document.addDocumentListener(object : DocumentAdapter() {
                override fun textChanged(e: DocumentEvent) {
                    val newVal = e.document.getText(0, e.document.length).toIntOrNull()
                    if (newVal != null && newVal >= 0 && newVal <= 100) {
                        myState.minFreeHeapPercent = newVal
                        this@apply.foreground = JBUI.CurrentTheme.Label.foreground()
                    } else {
                        this@apply.foreground = JBColor.RED
                    }
                }
            })
        }

        return FormBuilder().run {
            panel.border = IdeBorderFactory.createTitledBorder(HsErrBundle.message("limitsTitle"))
            addLabeledComponent(HsErrBundle.message("threadNumberTitle"), threadsField)
            addLabeledComponent(HsErrBundle.message("stacksSizeTitle"), stacksField)
            addLabeledComponent(HsErrBundle.message("freeMemTitle"), memField)
            addLabeledComponent(HsErrBundle.message("freeHeapTitle"), heapField)
            panel
        }
    }


    override fun apply() {
        SettingsStore.getInstance().loadState(myState)
    }

    override fun isModified(): Boolean {
        val originalState = SettingsStore.getInstance()
        return originalState.theState != myState
    }

    override fun reset() {
        myState = store.theState.clone()
        patternsDoc.replace(0, patternsDoc.length, myState.patternsToFold, SimpleAttributeSet.EMPTY)
    }

    override fun getDisplayName(): String = HsErrBundle.message("pluginName")

    override fun getHelpTopic(): Nothing? = null
}
