// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.github.mkartashev.hserr.language.psi

import com.github.mkartashev.hserr.language.HsErrLanguage
import com.intellij.psi.tree.IElementType

class HsErrTokenType(debugName: String): IElementType(debugName, HsErrLanguage) {
    override fun toString(): String {
        return "HsErrTokenType." + super.toString()
    }
}
