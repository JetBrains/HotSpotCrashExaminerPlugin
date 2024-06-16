// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.
package com.github.mkartashev.hserr.java

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.elementType
import com.github.mkartashev.hserr.language.psi.HsErrTokenType
import com.github.mkartashev.hserr.parseAsAddress
import javax.lang.model.SourceVersion

class MyGotoDeclarationHandler : GotoDeclarationHandler {
    private var isJavaPluginAvailable = PluginManager.isPluginInstalled(PluginId.getId("com.intellij.java"))

    override fun getGotoDeclarationTargets(
        sourceElement: PsiElement?,
        offset: Int,
        editor: Editor
    ): Array<PsiElement>? {
        val project = editor.project
        if (sourceElement?.elementType is HsErrTokenType && project != null) {
            val text = sourceElement.text
            val addr = parseAsAddress(text)
            if (addr != null) {
                return null
//                return getLocationByAddr(project, sourceElement.containingFile, addr)
            } else {
                val name = text
                    .replace('/', '.')// java/lang/Object
                    .substringBefore('(')
                    .substringBefore("\$\$") // ...Class$$Lambda...
                    .substringBefore('$')
                if (isJavaPluginAvailable && SourceVersion.isName(name)) {
                    return getSymbolByName(project, name)
                }
            }
        }
        return null
    }
//
//    private fun getLocationByAddr(project: Project, file: PsiFile, addr: ULong): Array<PsiElement> {
//        val log = getHsErrLogCached(project, file.virtualFile)
//        if (log != null) {
//            val elements = mutableListOf<PsiElement>()
//            val threads = log.getArtifact(ThreadsArtifact::class)
//            if (threads != null) {
//                var thread = threads.byAddress(addr)
//                if (thread == null) thread = threads.byStackAddress(addr)
//                if (thread != null) {
//                    val threadElement = file.findElementAt(thread.location.start.offset + 1)
//                    if (threadElement != null) elements.add(threadElement)
//                }
//            }
//
//            val libs = log.getArtifact(DynamicLibrariesArtifact::class)
//            if (libs != null) {
//                val lib = libs.byAddress(addr)
//                if (lib != null) {
//                    val libElement = file.findElementAt(lib.location.start.offset + 1)
//                    if (libElement != null) elements.add(libElement)
//                }
//            }
//            return elements.toTypedArray()
//        }
//
//        return emptyArray()
//    }

    private fun getSymbolByName(project: Project, name: String): Array<PsiElement>? {
        val scope = GlobalSearchScope.allScope(project)
        val javaPsiFacade = JavaPsiFacade.getInstance(project)

        val className = name.substringBeforeLast('.')
        val methodName = name.substringAfterLast('.')
        if (methodName.isNotBlank() && methodName[0] == methodName[0].uppercaseChar()) {
            // Likely a class name
            val psiClass = javaPsiFacade.findClass(name, scope) ?: return null
            return arrayOf(psiClass)
        } else {
            val methods = mutableListOf<PsiElement>()
            val psiClasses = javaPsiFacade.findClasses(className, scope)
            for (psiClass in psiClasses) {
                for (method in psiClass.allMethods) {
                    if (method.name == methodName) {
                        methods.add(method)
                    }
                }
                if (methods.isEmpty()) methods.add(psiClass)
            }
            return methods.toTypedArray()
        }
    }
}