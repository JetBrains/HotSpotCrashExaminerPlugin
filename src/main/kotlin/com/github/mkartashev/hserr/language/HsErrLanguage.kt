// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.github.mkartashev.hserr.language

import com.intellij.lang.Language

object HsErrLanguage: Language("HotSpot Fatal Error Log") {
    private fun readResolve(): Any = HsErrLanguage
}