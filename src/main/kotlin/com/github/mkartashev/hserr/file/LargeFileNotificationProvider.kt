// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.github.mkartashev.hserr.file

import com.github.mkartashev.hserr.HsErrBundle
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import javax.swing.JComponent
import java.util.function.Function

class LargeFileNotificationProvider : EditorNotificationProvider {
    override fun collectNotificationData(project: Project, file: VirtualFile): Function<in FileEditor, out JComponent?> {
        return Function {
            if (!isTooLarge(file.length)) {
                return@Function null
            }

            val panel = EditorNotificationPanel()
            return@Function panel.text(HsErrBundle.message("fileIsTooLargeText"))
        }
    }
}

fun isTooLarge(len: Long) = len > 10_000_000