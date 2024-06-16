// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.github.mkartashev.hserr.language.psi

import com.github.mkartashev.hserr.HsErrIcons
import com.github.mkartashev.hserr.language.HsErrFileType
import com.github.mkartashev.hserr.language.HsErrLanguage
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider

class HsErrFile(provider: FileViewProvider) : PsiFileBase(provider, HsErrLanguage) {
    override fun getFileType(): FileType {
        return HsErrFileType
    }

    override fun toString() = "HSCLFile"

    val icon = HsErrIcons.FILE
}