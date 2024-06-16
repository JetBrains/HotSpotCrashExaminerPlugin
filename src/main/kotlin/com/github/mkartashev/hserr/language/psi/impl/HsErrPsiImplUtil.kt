// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.github.mkartashev.hserr.language.psi.impl

import com.github.mkartashev.hserr.HsErrIcons
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.PsiElement
import com.github.mkartashev.hserr.language.psi.*
import javax.swing.Icon

class HsErrPsiImplUtil {
    companion object {
        @JvmStatic
        fun getName(element: HsErrSection): String? {
            val headerNode = element.node.findChildByType(HsErrTypes.SECTION_HDR)
            return headerNode?.text?.replace("\n", " ")?.trim()
        }

        @JvmStatic
        fun getName(element: HsErrSubsection): String? {
            val subtitleNode = element.node.findChildByType(HsErrTypes.SUBTITLE)
            return subtitleNode?.text?.replace("\n", " ")?.trim()
        }

        @JvmStatic
        fun getPresentation(element: HsErrSection): ItemPresentation {
            return object : ItemPresentation {
                override fun getPresentableText(): String? {
                    val name = element.name
                    return beautifyName(name)
                }

                override fun getIcon(unused: Boolean): Icon {
                    return  when {
                        element.name == null -> HsErrIcons.SECTION
                        element.name!!.startsWith("END") -> HsErrIcons.INTRO
                        element.name!!.contains("event", true) -> HsErrIcons.EVENT
                        element.name!!.contains("exception", true) -> HsErrIcons.EXCEPTION
                        element.name!!.containsAnyIgnoreCase(
                            "gc heap history",
                            "classes unloaded",
                            "classes redefined",
                            "vm operations"
                        ) -> HsErrIcons.EVENT
                        element.name!!.containsAnyIgnoreCase("heap") -> HsErrIcons.HEAP
                        element.name!!.containsAnyIgnoreCase("thread") -> HsErrIcons.THREADS
                        else -> HsErrIcons.SECTION
                    }
                }

            }
        }

        fun beautifyName(name: String?) = if (name != null && name.contains("---"))
            name.filterNot { c -> c == ' ' || c == '-' }
        else name

        @JvmStatic
        fun getPresentation(element: HsErrSubsection): ItemPresentation {
            return object : ItemPresentation {
                override fun getPresentableText(): String? {
                    return element.name
                }

                override fun getIcon(unused: Boolean): Icon {
                    return when {
                        element.name == null -> HsErrIcons.SUBSECTION
                        element.name!!.contains("thread", true) -> HsErrIcons.THREADS
                        element.name!!.contains("register", true) -> HsErrIcons.REGISTERS
                        element.name!!.contains("event", true) -> HsErrIcons.EVENT
                        element.name!!.contains("exception", true) -> HsErrIcons.EXCEPTION
                        element.name!!.containsAnyIgnoreCase(
                            "gc heap history",
                            "classes unloaded",
                            "classes redefined",
                            "vm operations"
                        ) -> HsErrIcons.EVENT

                        element.name!!.contains("Dynamic libraries") -> HsErrIcons.MEMORY_MAP
                        element.name!!.containsAnyIgnoreCase("heap") -> HsErrIcons.HEAP
                        element.name!!.containsAny("GC") -> HsErrIcons.HEAP
                        element.name!!.contains("stack", true) -> HsErrIcons.STACK
                        else -> HsErrIcons.SUBSECTION
                    }
                }
            }
        }

        @JvmStatic
        fun getName(element: HsErrIntro): String {
            return "INTRO"
        }

        @JvmStatic
        fun getPresentation(element: HsErrIntro): ItemPresentation {
            return object : ItemPresentation {
                override fun getPresentableText(): String {
                    return "INTRO"
                }

                override fun getIcon(unused: Boolean): Icon {
                    return HsErrIcons.INTRO
                }

            }
        }

        @JvmStatic
        fun setName(element: HsErrToken, name: String): PsiElement {
            // ignore this request
            return element
        }

        @JvmStatic
        fun getNameIdentifier(element: HsErrToken): PsiElement {
            return element
        }
    }
}

private fun String.containsAnyIgnoreCase(vararg strings: String): Boolean {
    for (s in strings) {
        if (this.contains(s, true)) return true
    }
    return false
}

private fun String.containsAny(vararg strings: String): Boolean {
    for (s in strings) {
        if (this.contains(s, false)) return true
    }
    return false
}
