// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.github.mkartashev.hserr.language

import com.intellij.psi.tree.IElementType

class HsErrElementType (debugName: String?) : IElementType(debugName!!, HsErrLanguage) {
    override fun toString(): String {
        return "HsErr: " + super.toString()
    }
}