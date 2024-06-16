// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.github.mkartashev.hserr.language.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.github.mkartashev.hserr.language.psi.HsErrNamedElement

abstract class HsErrNamedElementImpl(node: ASTNode): ASTWrapperPsiElement(node), HsErrNamedElement
