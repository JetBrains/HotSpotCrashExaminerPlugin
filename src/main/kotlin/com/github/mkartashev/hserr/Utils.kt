// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.github.mkartashev.hserr

import com.github.mkartashev.hserr.language.HsErrLanguage
import com.github.mkartashev.hserr.miner.HsErrLog
import com.github.mkartashev.hserr.toolwindow.HsErrToolWindowFactory
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import java.io.IOException

inline fun <reified T> getService(): T = application.getService(T::class.java)

val application: Application get() = ApplicationManager.getApplication()

fun String.startsWithAny(patterns: List<String>): Boolean {
    val rc = patterns.any {
        this.startsWith(it)
    }
    return rc
}

const val KiB = 1024UL
fun ULong.toHumanReadableSize(): String {
    return when {
        this >= KiB * KiB * KiB -> String.format("%.2f GiB", this.toDouble().div((KiB * KiB * KiB).toDouble()))
        this >= KiB * KiB -> String.format("%.2f MiB", this.toDouble().div((KiB * KiB).toDouble()))
        this >= KiB -> String.format("%.2f KiB", this.toDouble().div((KiB).toDouble()))
        else -> this.toString() + "B"
    }
}

fun parseAsAddress(s: String): ULong? {
    try {
        return if (s.startsWith("0x") || s.startsWith("00")) {
            s.substring(2).toULong(16)
        } else {
            s.toULong()
        }
    } catch (ignored: NumberFormatException) {
    }
    // May still be a hexadecimal number, but without the 0x prefix
    return s.toULongOrNull(16)
}

fun getHsErrLogCached(project: Project, virtualFile: VirtualFile): HsErrLog? {
    val fileDocumentManager = FileDocumentManager.getInstance()
    val document = fileDocumentManager.getDocument(virtualFile)

    var log = virtualFile.getUserData(hsErrLogKey)
    if (log == null && document != null) {
        val psiFile = com.intellij.psi.PsiDocumentManager.getInstance(project).getPsiFile(document)
        val fileText = psiFile?.text
        if (fileText != null) {
            try {
                log = HsErrLog(fileText)
                virtualFile.putUserData(hsErrLogKey, log)
            } catch (ignored: IOException) {
            }
        }
    }

    return log
}

fun resetHsErrLogCache(project: Project, virtualFile: VirtualFile) {
    virtualFile.putUserData(hsErrLogKey, null)
}

fun isHsErr(psiFile: PsiFile) = psiFile.fileType is LanguageFileType
        && (psiFile.fileType as LanguageFileType).language == HsErrLanguage

val selectAddrKey = Key.create<ULong>("HsErrSelectAddr")
val hsErrLogKey = Key.create<HsErrLog>("HsErrLogInstance")
val toolWindowKey = Key.create<HsErrToolWindowFactory.HsErrToolWindow>("HsErrToolWindowInstance")
