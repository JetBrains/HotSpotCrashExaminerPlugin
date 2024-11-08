// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.github.mkartashev.hserr

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader

object HsErrIcons {
    @JvmField
    val TOOLWINDOW = IconLoader.getIcon("/icons/toolwindow.svg", HsErrIcons::class.java)

    @JvmField
    val FILE = IconLoader.getIcon("/icons/file.svg", HsErrIcons::class.java)

    @JvmField
    val SECTION = IconLoader.getIcon("/icons/section.svg", HsErrIcons::class.java)

    @JvmField
    val INTRO = IconLoader.getIcon("/icons/heading.svg", HsErrIcons::class.java)

    @JvmField
    val SUBSECTION = IconLoader.getIcon("/icons/subsection.svg", HsErrIcons::class.java)

    @JvmField
    val THREADS = IconLoader.getIcon("/icons/threads.svg", HsErrIcons::class.java)

    @JvmField
    val REGISTERS = IconLoader.getIcon("/icons/registers.svg", HsErrIcons::class.java)

    @JvmField
    val STACK = IconLoader.getIcon("/icons/stack_top.svg", HsErrIcons::class.java)

    @JvmField
    val EVENT = IconLoader.getIcon("/icons/event.svg", HsErrIcons::class.java)

    @JvmField
    val EXCEPTION = IconLoader.getIcon("/icons/exception.svg", HsErrIcons::class.java)

    @JvmField
    val MEMORY_MAP = AllIcons.Actions.GroupBy

    @JvmField
    val HEAP = IconLoader.getIcon("/icons/heap.svg", HsErrIcons::class.java)
}
