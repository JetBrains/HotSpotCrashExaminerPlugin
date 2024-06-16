// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.github.mkartashev.hserr.language

import com.intellij.ide.navigationToolbar.StructureAwareNavBarModelExtension
import com.intellij.lang.Language
import com.github.mkartashev.hserr.language.psi.*
import com.github.mkartashev.hserr.HsErrIcons
import javax.swing.Icon

class HsErrStructureAwareNavBar: StructureAwareNavBarModelExtension() {

    override val language: Language
        get() = HsErrLanguage

    override fun getPresentableText(o: Any?): String? {
        return when (o) {
            is HsErrFile ->  null
            is HsErrSection -> o.presentation.presentableText
            is HsErrSubsection -> o.presentation.presentableText
            is HsErrIntro -> o.presentation.presentableText
            else -> null
        }
    }

    override fun getIcon(o: Any?): Icon? {
        return when (o) {
            is HsErrFile -> HsErrIcons.FILE
            is HsErrSection -> o.presentation.getIcon(false)
            is HsErrIntro -> o.presentation.getIcon(false)
            is HsErrSubsection -> o.presentation.getIcon(false)
            else -> null
        }
    }
}