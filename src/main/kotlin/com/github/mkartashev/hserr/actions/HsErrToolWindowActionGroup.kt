// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.github.mkartashev.hserr.actions

import com.github.mkartashev.hserr.resetHsErrLogCache
import com.github.mkartashev.hserr.settings.SettingsConfigurable
import com.github.mkartashev.hserr.toolWindowKey
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.wm.ToolWindowManager

class HsErrToolWindowActionGroup: ActionGroup() {
    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        val settingsAction = ActionManager.getInstance().getAction("HsErrSettingsAction")
        return arrayOf(settingsAction)
    }
}

class HsErrToolWindowSettingsAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ShowSettingsUtil.getInstance().showSettingsDialog(project, SettingsConfigurable::class.java)
    }
}

class HsErrToolWindowRefreshAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("HsErrToolWindow") ?: return
        val contents = toolWindow.contentManager.contents[0]
        val hsErrToolWindow = contents.getUserData(toolWindowKey) ?: return

        val selectedEditor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
        val document = selectedEditor.document
        val file = FileDocumentManager.getInstance().getFile(document) ?: return
        resetHsErrLogCache(project, file)
        hsErrToolWindow.maybeUpdateUI(file, selectedEditor)
    }
}