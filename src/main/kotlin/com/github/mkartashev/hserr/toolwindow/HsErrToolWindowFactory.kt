// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.github.mkartashev.hserr.toolwindow

import com.github.mkartashev.hserr.*
import com.github.mkartashev.hserr.actions.HsErrToolWindowActionGroup
import com.github.mkartashev.hserr.file.isTooLarge
import com.github.mkartashev.hserr.miner.HsErrLog
import com.github.mkartashev.hserr.miner.artifact.*
import com.github.mkartashev.hserr.miner.text.TextRange
import com.github.mkartashev.hserr.settings.SettingsStore
import com.intellij.icons.AllIcons
import com.intellij.navigation.extractVcsOriginCanonicalPath
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.markup.*
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.psi.PsiManager
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.nio.file.Files
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class HsErrToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val hsErrToolWindow = HsErrToolWindow(project, toolWindow)
        val selectedEditor = FileEditorManager.getInstance(project).selectedTextEditor
        if (selectedEditor != null) {
            val document = selectedEditor.document
            val file = FileDocumentManager.getInstance().getFile(document)
            if (file != null) hsErrToolWindow.maybeUpdateUI(file, selectedEditor)
        }
        val stats = ContentFactory.getInstance().createContent(hsErrToolWindow.getPanel(), null, false)
        stats.putUserData(toolWindowKey, hsErrToolWindow)
        toolWindow.contentManager.addContent(stats)

        toolWindow.setAdditionalGearActions(HsErrToolWindowActionGroup())
        val refreshAction = com.intellij.openapi.actionSystem.ActionManager.getInstance().getAction("HsErrRefreshAction")
        val settingsAction = com.intellij.openapi.actionSystem.ActionManager.getInstance().getAction("HsErrSettingsAction")
        toolWindow.setTitleActions(listOf(refreshAction, settingsAction))
        toolWindow.stripeTitle = "HsErr"

        project.messageBus.connect().subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            hsErrToolWindow
        )
    }

    override fun shouldBeAvailable(project: Project) = true

    class HsErrToolWindow(
        private val project: Project,
        private val toolWindow: ToolWindow
    ) : FileEditorManagerListener, DumbAware {
        private val highlightersKey: Key<MutableList<RangeHighlighter>> = Key.create("hserrlog.poi.highlighters")

        private val summaryText: JBTextArea = makeMultiLineTextArea(7, 30)
        private val analysisText: JBTextArea = makeMultiLineTextArea(5, 30)

        private val crashReasonLabel = JBLabel()
        private val signalLabel = JBLabel()
        private val ieLabel = JBLabel()

        private val jreVersionLabel = JBLabel()
        private val jvmVersionLabel = JBLabel()
        private val buildDateLabel = JBLabel()

        private val uptimeLabel = JBLabel()
        private val heapLabel = JBLabel()
        private val heapFreeLabel = JBLabel()
        private val threadLabel = JBLabel()
        private val threadCountLabel = JBLabel()
        private val stacksSizeLabel = JBLabel()

        private val osLabel = JBLabel()
        private val hardwareLabel = JBLabel()
        private val cpuCountLabel = JBLabel()
        private val physMemLabel = JBLabel()
        private val swapLabel = JBLabel()
        private val loadFactorLabel = JBLabel()

        fun getPanel(): JPanel {
            val reasonPanel = createCrashReasonPanel()
            val versionPanel = createVersionPanel()
            val osPanel = createOsHwPanel()
            val jvmPanel = createJVMPanel()
            val analysisPanel = createAnalysisPanel()

            val mainPanel = FormBuilder.createFormBuilder().run {
                addComponent(reasonPanel)
                addComponent(versionPanel)
                addComponent(jvmPanel)
                addComponent(osPanel)
                addComponent(analysisPanel)
                addComponentFillVertically(JPanel(), 0)
                panel
            }

            val scrollPane = JBScrollPane(
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
            ).apply {
                border = JBEmptyBorder(0, 10, 0, 10)
                setViewportView(mainPanel)
            }

            val panel = JBPanel<JBPanel<*>>().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                border = null
                add(scrollPane)
            }
            return panel
        }

        private fun createCrashReasonPanel(): JBPanel<JBPanel<*>> {
            val content = FormBuilder.createFormBuilder().run {
                addLabeledComponent("Reason:", crashReasonLabel)
                addLabeledComponent("Signal:", signalLabel)
                addLabeledComponent("Internal exceptions:", ieLabel)
                panel
            }

            val panel = JBPanel<JBPanel<*>>().apply {
                layout = BorderLayout()
                border = IdeBorderFactory.createTitledBorder(HsErrBundle.message("reasonPanelTitle"))
                add(content, BorderLayout.WEST)
            }
            return panel
        }

        private fun createVersionPanel(): JBPanel<JBPanel<*>> {
            val content = FormBuilder.createFormBuilder().run {
                addLabeledComponent("JRE version:", jreVersionLabel)
                addLabeledComponent("JVM version:", jvmVersionLabel)
                addLabeledComponent("Built on:", buildDateLabel)
                panel
            }

            val panel = JBPanel<JBPanel<*>>().apply {
                layout = BorderLayout()
                border = IdeBorderFactory.createTitledBorder(HsErrBundle.message("versionPanelTitle"))
                add(content, BorderLayout.WEST)
            }
            return panel
        }

        private fun createJVMPanel(): JBPanel<JBPanel<*>> {
            val content = FormBuilder.createFormBuilder().run {
                addLabeledComponent("Uptime:", uptimeLabel)
                addLabeledComponent("Java heap total:", heapLabel)
                addLabeledComponent("Java heap free:", heapFreeLabel)
                addLabeledComponent("Thread:", threadLabel)
                addLabeledComponent("Thread count:", threadCountLabel)
                addLabeledComponent("Total stacks size:", stacksSizeLabel)
                panel
            }

            val panel = JBPanel<JBPanel<*>>().apply {
                layout = BorderLayout()
                border = IdeBorderFactory.createTitledBorder(HsErrBundle.message("jvmPanelTitle"))
                add(content, BorderLayout.WEST)
            }

            return panel
        }

        private fun createAnalysisPanel(): JBPanel<JBPanel<*>> {
            val content = FormBuilder.createFormBuilder().run {
                addComponent(summaryText)
                addComponent(analysisText)
                panel
            }

            val panel = JBPanel<JBPanel<*>>().apply {
                layout = BorderLayout()
                border = IdeBorderFactory.createTitledBorder(HsErrBundle.message("analysisTitle"))
                add(content, BorderLayout.WEST)
            }

            return panel
        }

        private fun createOsHwPanel(): JBPanel<JBPanel<*>> {
            val content = FormBuilder.createFormBuilder().run {
                addLabeledComponent("OS:", osLabel)
                addLabeledComponent("Hardware:", hardwareLabel)
                addLabeledComponent("CPU count:", cpuCountLabel)
                addLabeledComponent("Physical memory:", physMemLabel)
                addLabeledComponent("Swap:", swapLabel)
                addLabeledComponent("Load factor:", loadFactorLabel)
                panel
            }

            val osPanel = JBPanel<JBPanel<*>>().apply {
                layout = BorderLayout()
                border = IdeBorderFactory.createTitledBorder(HsErrBundle.message("osAndHwPanelTitle"))
                add(content, BorderLayout.WEST)
            }
            return osPanel
        }

        override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
            resetHsErrLogCache(file)
        }

        override fun selectionChanged(event: FileEditorManagerEvent) {
            val newFile = event.newFile ?: return
            val editor = if (event.newEditor is TextEditor) (event.newEditor as TextEditor).editor else null
            maybeUpdateUI(newFile, editor)
        }

        override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
            val editor = if (source.selectedEditor is TextEditor) (source.selectedEditor as TextEditor).editor else null
            maybeUpdateUI(file, editor)
        }

        fun maybeUpdateUI(newFile: VirtualFile, editor: Editor?) {
            val psiFile = PsiManager.getInstance(toolWindow.project).findFile(newFile) ?: return
            if (isHsErr(psiFile)) {
                if (!isTooLarge(Files.size(newFile.toNioPath()))) {
                    updateUIForFile(newFile, editor)
                } else {
                    resetUIWithComment(HsErrBundle.message("fileIsTooLargeText"))
                }
            } else {
                resetUIWithComment(HsErrBundle.message("wrongFileLang"))
            }
        }

        private fun resetUIWithComment(comment: String) {
            updateLabelsFor(null, null)
            summaryText.text = comment
            analysisText.text = null
        }

        private fun updateUIForFile(newFile: VirtualFile, editor: Editor?) {
            val log = getHsErrLogCached(project, newFile)
            updateLabelsFor(editor, log)
            if (log != null) {
                val summaryArtifact = log.getArtifact(ExecutiveSummaryArtifact::class)
                if (summaryArtifact != null) {
                    summaryText.text = summaryArtifact.comment
                    val text = StringUtil.stripHtml(
                        summaryArtifact.reasonHtml
                            .replace("<p>", "\n")
                            .replace("<br>", "\n"), true
                    )
                    analysisText.text = text
                }
            } else {
                resetUIWithComment("")
            }
        }

        private fun resetHsErrLogCache(virtualFile: VirtualFile) {
            virtualFile.putUserData(hsErrLogKey, null)
        }

        private fun updateLabelsFor(editor: Editor?, log: HsErrLog?) {
            updateCrashReasonLabelsFor(log, editor)
            updateVersionLabelsFor(log, editor)
            updateJVMLabelsFor(log, editor)
            updateOsHwLabelsFor(log, editor)
        }

        private fun updateVersionLabelsFor(log: HsErrLog?, editor: Editor?) {
            jreVersionLabel.toolTipText = "Version of the JRE"
            jvmVersionLabel.toolTipText = "Version of the JVM"
            buildDateLabel.toolTipText = "JVM build date"
            val versionArtifact = log?.getArtifact(VersionArtifact::class)
            if (editor != null && versionArtifact != null) {
                updateLabelWithInfo(jreVersionLabel, versionArtifact.jreVersionFull, versionArtifact.locations, editor)
                updateLabelWithInfo(jvmVersionLabel, versionArtifact.jvmVersionFull, versionArtifact.locations, editor)
                if (versionArtifact.jvmVersionBuild != versionArtifact.jreVersionBuild) {
                    jvmVersionLabel.icon = warningIcon()
                    jvmVersionLabel.toolTipText = versionArtifact.comment
                }
                if (versionArtifact.buildDate != Artifact.DEFAULT_DATE) {
                    updateLabelWithInfo(
                        buildDateLabel,
                        versionArtifact.buildDate.toString(), versionArtifact.locations, editor
                    )
                } else {
                    emptyLabel(buildDateLabel)
                }
            } else {
                emptyLabel(jreVersionLabel)
                emptyLabel(jvmVersionLabel)
                emptyLabel(buildDateLabel)
            }
        }

        private fun updateCrashReasonLabelsFor(log: HsErrLog?, editor: Editor?) {
            crashReasonLabel.toolTipText = "The ultimate reason of the crash (signal, internal error, etc)"
            val headerArtifact = log?.getArtifact(HeaderArtifact::class)
            if (editor != null && headerArtifact != null) {
                val reasonText =
                    if (headerArtifact.reason.startsWith("EXCEPTION_") || headerArtifact.reason.startsWith("SIG")) "signal" else headerArtifact.reason
                updateLabelWithInfo(crashReasonLabel, reasonText, headerArtifact.locations, editor)
            } else {
                emptyLabel(crashReasonLabel)
            }

            val signalInfoArtifact = log?.getArtifact(SignalInfoArtifact::class)
            if (editor != null && signalInfoArtifact != null) {
                updateLabelWithInfo(signalLabel, signalInfoArtifact.signalName, signalInfoArtifact.locations, editor)
                signalLabel.toolTipText = signalInfoArtifact.comment
            } else {
                emptyLabel(signalLabel)
                signalLabel.toolTipText = "The name of the unexpected signal received by the JVM"
            }

            ieLabel.toolTipText = "Internal exceptions thrown recently before the crash"
            val exceptionsArtifact = log?.getArtifact(InternalExceptionsArtifact::class)
            if (editor != null && exceptionsArtifact != null) {
                val textBuilder = mutableListOf<String>()
                if (exceptionsArtifact.oomeCount > 0) {
                    textBuilder.add("${exceptionsArtifact.oomeCount} OOME(s)")
                }
                if (exceptionsArtifact.linkageErrorsCount > 0) {
                    textBuilder.add("${exceptionsArtifact.linkageErrorsCount} LinkageError(s)")
                }
                if (exceptionsArtifact.stackOverflowCount > 0) {
                    textBuilder.add("${exceptionsArtifact.stackOverflowCount} StackOverflowError(s)")
                }
                val text = textBuilder.joinToString(", ")
                if (text.isNotEmpty()) {
                    updateLabelWithInfo(ieLabel, text, exceptionsArtifact.locations, editor)
                    ieLabel.icon = if (exceptionsArtifact.hasMemoryErrors) warningIcon() else null
                } else {
                    emptyLabel(ieLabel)
                }
            } else {
                emptyLabel(ieLabel)
            }
        }

        private fun updateJVMLabelsFor(log: HsErrLog?, editor: Editor?) {
            uptimeLabel.toolTipText = "How long did the JVM ran"
            heapLabel.toolTipText = "The Java heap size"
            heapFreeLabel.toolTipText = "The amount of free space in the Java heap"
            threadLabel.toolTipText = "The name of the thread that crashed"
            threadCountLabel.toolTipText = "The total number of threads in the JVM (both Java and native)"
            stacksSizeLabel.toolTipText = "The sum of all thread's stacks sizes"

            val uptimeArtifact = log?.getArtifact(UptimeArtifact::class)
            if (uptimeArtifact != null && editor != null) {
                val uptime = uptimeArtifact.uptime.toSeconds().toDuration(DurationUnit.SECONDS)
                updateLabelWithInfo(uptimeLabel, uptime.toString(), uptimeArtifact.locations, editor)
                if (uptimeArtifact.uptime.toMinutes() < 1) {
                    uptimeLabel.icon = warningIcon()
                    uptimeLabel.toolTipText = "The JVM crashed too early after the start"
                }
            } else {
                emptyLabel(uptimeLabel)
            }

            val heapArtifact = log?.getArtifact(JavaHeapArtifact::class)
            if (heapArtifact != null && editor != null) {
                updateLabelWithInfo(heapLabel, heapArtifact.size.toHumanReadableSize(), heapArtifact.locations, editor)
                updateLabelWithInfo(heapFreeLabel, "${heapArtifact.freePercentage}%", heapArtifact.locations, editor)
                if (heapArtifact.freePercentage < SettingsStore.getInstance().theState.minFreeHeapPercent) {
                    heapFreeLabel.icon = warningIcon()
                }
            } else {
                emptyLabel(heapLabel)
                emptyLabel(heapFreeLabel)
            }

            val threadsArtifact = log?.getArtifact(ThreadsArtifact::class)
            if (threadsArtifact != null && editor != null) {
                updateLabelWithInfo(
                    threadCountLabel,
                    threadsArtifact.threads.size.toString(),
                    threadsArtifact.locations,
                    editor
                )
                threadCountLabel.toolTipText = threadsArtifact.comment

                if (threadsArtifact.threads.size > SettingsStore.getInstance().theState.maxThreads) {
                    threadCountLabel.icon = warningIcon()
                    threadCountLabel.toolTipText = "The number of threads looks excessive"
                }
                updateLabelWithInfo(
                    stacksSizeLabel,
                    threadsArtifact.stacksSize.toHumanReadableSize(),
                    threadsArtifact.locations,
                    editor
                )
                if (threadsArtifact.stacksSize > SettingsStore.getInstance().theState.maxStacksSize) {
                    stacksSizeLabel.icon = warningIcon()
                    stacksSizeLabel.toolTipText = "The cumulative size of the stacks looks excessive"
                }
            } else {
                emptyLabel(threadCountLabel)
                emptyLabel(stacksSizeLabel)
            }

            val threadArtifact = log?.getArtifact(ThreadInfoArtifact::class)
            if (threadArtifact != null && editor != null) {
                updateLabelWithInfo(threadLabel, "\"${threadArtifact.name}\"", threadArtifact.locations, editor)
            } else {
                emptyLabel(threadLabel)
            }
        }

        private fun updateOsHwLabelsFor(log: HsErrLog?, editor: Editor?) {
            osLabel.toolTipText = "The Operating System name"
            val osArtifact = log?.getArtifact(OSArtifact::class)
            if (editor != null && osArtifact != null) {
                updateLabelWithInfo(osLabel, osArtifact.comment, osArtifact.locations, editor)
            } else {
                emptyLabel(osLabel)
            }

            hardwareLabel.toolTipText = "The host system architecture"
            val archArtifact = log?.getArtifact(ArchitectureArtifact::class)
            if (editor != null && archArtifact != null) {
                updateLabelWithInfo(hardwareLabel, archArtifact.comment, archArtifact.locations, editor)
            } else {
                emptyLabel(hardwareLabel)
            }

            cpuCountLabel.toolTipText = "The number of CPUs on the host system, including virtual ones"
            val sysArtifact = log?.getArtifact(SystemArtifact::class)
            if (editor != null && sysArtifact != null) {
                updateLabelWithInfo(cpuCountLabel, sysArtifact.cpuCount.toString(), sysArtifact.locations, editor)
            } else {
                emptyLabel(cpuCountLabel)
            }

            physMemLabel.toolTipText = "The amount of physical memory on the host system (used/total)"
            if (editor != null && sysArtifact != null && sysArtifact.physMemoryTotal > 0u) {
                val used = (sysArtifact.physMemoryTotal - sysArtifact.physMemoryFree)
                updateLabelWithInfo(
                    physMemLabel,
                    "${used.toHumanReadableSize()} / ${sysArtifact.physMemoryTotal.toHumanReadableSize()}",
                    sysArtifact.locations,
                    editor
                )
                val percentFree =
                    sysArtifact.physMemoryFree.toDouble().div(sysArtifact.physMemoryTotal.toDouble()).times(100).toInt()
                if (percentFree < SettingsStore.getInstance().theState.minFreeMemPercent) {
                    physMemLabel.icon = warningIcon()
                }
            } else {
                emptyLabel(physMemLabel)
            }

            swapLabel.toolTipText = "The amount of swap memory on the host system (used/total)"
            if (editor != null && sysArtifact != null && sysArtifact.swapTotal > 0u) {
                val used = sysArtifact.swapTotal - sysArtifact.swapFree
                updateLabelWithInfo(
                    swapLabel,
                    "${used.toHumanReadableSize()} / ${sysArtifact.swapTotal.toHumanReadableSize()}",
                    sysArtifact.locations,
                    editor
                )
            } else {
                emptyLabel(swapLabel)
            }

            loadFactorLabel.toolTipText = "How busy the host system had been in the past minute"
            if (editor != null && sysArtifact != null) {
                val loadAvg = if (sysArtifact.loadAverage.size > 1) sysArtifact.loadAverage[0] else null
                if (loadAvg != null && sysArtifact.cpuCount > 0) {
                    val loadAvgText = loadAvg.toDouble().div(sysArtifact.cpuCount).times(100).toInt().toString() + "%"
                    updateLabelWithInfo(loadFactorLabel, loadAvgText, sysArtifact.locations, editor)
                    if (sysArtifact.isOverloaded()) {
                        loadFactorLabel.icon = warningIcon()
                        loadFactorLabel.toolTipText = "The system was overloaded before the JVM crash"
                    }
                } else {
                    emptyLabel(loadFactorLabel)
                }
            } else {
                emptyLabel(loadFactorLabel)
            }
        }

        private fun removeHighlighters(editor: Editor) {
            val highlighters = editor.getUserData(highlightersKey)
            highlighters?.forEach {
                editor.markupModel.removeHighlighter(it)
                it.dispose()
            }
            editor.putUserData(highlightersKey, null)
        }

        private fun addHighlighter(editor: Editor, h: RangeHighlighter) {
            val highlighters = editor.getUserData(highlightersKey) ?: mutableListOf()
            highlighters.add(h)
            editor.putUserData(highlightersKey, highlighters)
        }

        private fun updateLabelWithInfo(
            label: JBLabel,
            text: String,
            locations: Set<TextRange>,
            editor: Editor
        ) {
            label.icon = null
            label.text = text
            addLinkTargetToLabel(label, locations, editor)
        }

        private fun addLinkTargetToLabel(
            label: JBLabel,
            locations: Set<TextRange>,
            editor: Editor
        ) {
            if (locations.isNotEmpty()) {
                val start = locations.first().start.offset
                val end = locations.first().endInclusive.moveToNextChar().offset
                if (end > start && end > 0) {
                    label.foreground = JBUI.CurrentTheme.Link.Foreground.ENABLED
                    label.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    label.addMouseListener(object : MouseAdapter() {
                        override fun mouseClicked(e: MouseEvent?) {
                            if (editor.isDisposed) return
                            if (!withinRange(start, end, editor)) return

                            removeHighlighters(editor)
                            editor.caretModel.moveToOffset(start)
                            editor.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
                            val attrs = TextAttributes(
                                null, null,
                                getHighlighterColor(),
                                EffectType.BOLD_DOTTED_LINE,
                                Font.PLAIN
                            )

                            for (loc in locations) {
                                val from = loc.start.offset
                                val to = loc.endInclusive.moveToNextChar().offset
                                if (withinRange(from, to, editor)) {
                                    val h = editor.markupModel.addRangeHighlighter(
                                        from, to,
                                        HighlighterLayer.SELECTION,
                                        attrs,
                                        HighlighterTargetArea.EXACT_RANGE
                                    )
                                    h.errorStripeMarkColor = getHighlighterColor()
                                    addHighlighter(editor, h)
                                }
                            }
                        }
                    })
                }
            }
        }

        private fun getHighlighterColor(): JBColor? = JBColor.ORANGE

        private fun withinRange(start: Int, end: Int, editor: Editor) =
            start < editor.document.textLength && end <= editor.document.textLength

        private fun makeMultiLineTextArea(rows: Int, cols: Int) =
            JBTextArea(rows, cols).apply {
                isEditable = false
                wrapStyleWord = true
                lineWrap = true
                font = JBUI.Fonts.label()
                background = null
                border = JBUI.Borders.emptyBottom(10)
            }

        private fun emptyLabel(label: JBLabel) {
            label.text = null
            label.icon = absentIcon()
            label.toolTipText = null
        }

        private fun absentIcon() = AllIcons.Actions.Cancel
        private fun warningIcon() = AllIcons.Debugger.Db_obsolete
    }
}
