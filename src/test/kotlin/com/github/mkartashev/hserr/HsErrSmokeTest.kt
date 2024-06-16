// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.github.mkartashev.hserr

import com.github.mkartashev.hserr.language.HsErrFileType
import com.github.mkartashev.hserr.language.psi.HsErrFile
import com.github.mkartashev.hserr.language.psi.HsErrIntro
import com.github.mkartashev.hserr.language.psi.HsErrSection
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.PsiErrorElementUtil


@TestDataPath("\$CONTENT_ROOT/src/test/testData")
class HsErrSmokeTest : BasePlatformTestCase() {

    fun testIntro() {
        val psiFile = myFixture.configureByText(HsErrFileType, "# A fatal error has been detected by the Java Runtime Environment:")
        val hsErrFile = assertInstanceOf(psiFile, HsErrFile::class.java)

        assertFalse(PsiErrorElementUtil.hasErrors(project, hsErrFile.virtualFile))
        assertInstanceOf(hsErrFile.firstChild, HsErrIntro::class.java)
    }

    fun testLinux() {
        val psiFile = myFixture.configureByFile("linux/hs_err_pid123.log")
        val hsErrFile = assertInstanceOf(psiFile, HsErrFile::class.java)

        assertFalse(PsiErrorElementUtil.hasErrors(project, hsErrFile.virtualFile))
        assertInstanceOf(hsErrFile.firstChild, HsErrIntro::class.java)
        val sections = hsErrFile.findChildrenByClass(HsErrSection::class.java)
        val section1 = sections[0]
        val section2 = sections[1]
        val section3 = sections[2]
        val section4 = sections[3]
        assertEquals("---------------  S U M M A R Y ------------", section1?.name)
        assertEquals("---------------  T H R E A D  ---------------", section2?.name)
        assertEquals("---------------  P R O C E S S  ---------------", section3?.name)
        assertEquals("Heap", section4?.name)
    }

    override fun getTestDataPath() = "src/test/testData"
}
