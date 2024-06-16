// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.github.mkartashev.hserr

import com.github.mkartashev.hserr.language.HsErrFileType
import com.github.mkartashev.hserr.language.psi.HsErrFile
import com.github.mkartashev.hserr.language.psi.HsErrIntro
import com.github.mkartashev.hserr.language.psi.HsErrSection
import com.github.mkartashev.hserr.language.psi.HsErrSubsection
import com.intellij.psi.PsiElement
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.PsiErrorElementUtil
import java.nio.file.Files
import java.nio.file.Path

@TestDataPath("\$CONTENT_ROOT/src/test/testData")
class HsErrPsiTestLinux : BasePlatformTestCase() {
    // Verifies the correctness of the PSI structure of a hs_err file produced on Linux
    fun testLinux() {
        val psiFile = myFixture.configureByFile("linux/hs_err_pid123.log")
        val hsErrFile = assertInstanceOf(psiFile, HsErrFile::class.java)

        assertFalse(PsiErrorElementUtil.hasErrors(project, hsErrFile.virtualFile))
        assertInstanceOf(hsErrFile.firstChild, HsErrIntro::class.java)

        val sections = hsErrFile.findChildrenByClass(HsErrSection::class.java)

        val summary = sectionByName(sections, "---------------  S U M M A R Y ------------")
        assertNotNull(summary)
        summary!!
        verifySummary(summary)

        val thread = sectionByName(sections, "---------------  T H R E A D  ---------------")
        assertNotNull(thread)
        thread!!
        verifyThread(thread)

        val process = sectionByName(sections, "---------------  P R O C E S S  ---------------")
        assertNotNull(process)
        process!!
        verifyProcess(process)

        val heap = sectionByName(sections, "Heap")
        assertNotNull(heap)
        heap!!
        verifyHeap(heap)

        assertNotNull(sectionByName(sections, "Compilation events"))
        assertNotNull(sectionByName(sections, "GC Heap History"))
        assertNotNull(sectionByName(sections, "Compilation events"))
        assertNotNull(sectionByName(sections, "Dll operation events"))
        assertNotNull(sectionByName(sections, "Deoptimization events"))
        assertNotNull(sectionByName(sections, "Classes loaded"))
        assertNotNull(sectionByName(sections, "Classes unloaded"))
        assertNotNull(sectionByName(sections, "Classes redefined"))
        assertNotNull(sectionByName(sections, "ZGC Phase Switch"))
        assertNotNull(sectionByName(sections, "VM Operations"))
        assertNotNull(sectionByName(sections, "Events"))
        assertNotNull(sectionByName(sections, "Dynamic libraries"))

        val vmArgs = sectionByName(sections, "VM Arguments")
        assertNotNull(vmArgs)
        vmArgs!!
        verifyVMArgs(vmArgs)

        assertNotNull(sectionByName(sections, "Logging"))

        val envVars = sectionByName(sections, "Environment Variables")
        assertNotNull(envVars)
        envVars!!
        verifyEnvVars(envVars)

        assertNotNull(sectionByName(sections, "Active Locale"))
        assertNotNull(sectionByName(sections, "Signal Handlers"))
        assertNotNull(sectionByName(sections, "Native Memory Tracking"))

        val system = sectionByName(sections, "---------------  S Y S T E M  ---------------")
        assertNotNull(system)
        system!!
        verifySystem(system)
    }

    // Verifies that the output of jcmd VM.info is parsable
    fun testLinuxVmInfo() {
        val vmInfoFileText = Files.readAllLines(Path.of(getTestDataPath(), "linux/vm_info.log")).joinToString("\n")
        val psiFile = myFixture.configureByText(HsErrFileType, vmInfoFileText)
        val hsErrFile = assertInstanceOf(psiFile, HsErrFile::class.java)

        assertFalse(PsiErrorElementUtil.hasErrors(project, hsErrFile.virtualFile))
        assertInstanceOf(hsErrFile.firstChild, HsErrIntro::class.java)

        val sections = hsErrFile.findChildrenByClass(HsErrSection::class.java)

        val summary = sectionByName(sections, "---------------  S U M M A R Y ------------")
        assertNotNull(summary)
        summary!!
        verifySummary(summary)

        assertNull( sectionByName(sections, "---------------  T H R E A D  ---------------"))
        assertNotNull(sectionByName(sections, "---------------  P R O C E S S  ---------------"))
        assertNotNull(sectionByName(sections, "Heap"))
        assertNotNull(sectionByName(sections, "Compilation events"))
        assertNotNull(sectionByName(sections, "GC Heap History"))
        assertNotNull(sectionByName(sections, "Compilation events"))
        assertNotNull(sectionByName(sections, "Dll operation events"))
        assertNotNull(sectionByName(sections, "Deoptimization events"))
        assertNotNull(sectionByName(sections, "Classes loaded"))
        assertNotNull(sectionByName(sections, "Classes unloaded"))
        assertNotNull(sectionByName(sections, "Classes redefined"))
        assertNotNull(sectionByName(sections, "VM Operations"))
        assertNotNull(sectionByName(sections, "Events"))
        assertNotNull(sectionByName(sections, "Dynamic libraries"))

        assertNotNull(sectionByName(sections, "VM Arguments"))
        assertNotNull(sectionByName(sections, "Logging"))

        val envVars = sectionByName(sections, "Environment Variables")
        assertNotNull(envVars)
        envVars!!
        verifyEnvVars(envVars)

        assertNotNull(sectionByName(sections, "Active Locale"))
        assertNotNull(sectionByName(sections, "Signal Handlers"))
        assertNull(sectionByName(sections, "Native Memory Tracking"))

        val system = sectionByName(sections, "---------------  S Y S T E M  ---------------")
        assertNotNull(system)
        system!!
        verifySystem(system)
    }

    override fun getTestDataPath() = "src/test/testData"

    private fun verifySummary(section: HsErrSection) {
        val subsections = section.subsectionList
        assertNotNull(subsectionByName(subsections, "Command Line"))
        assertNotNull(subsectionByName(subsections, "Host"))
        assertNotNull(subsectionByName(subsections, "Time"))
    }

    private fun verifyThread(section: HsErrSection) {
        val subsections = section.subsectionList
        assertNotNull(subsectionByName(subsections, "Current thread"))
        assertNotNull(subsectionByName(subsections, "Stack"))
        assertNotNull(subsectionByName(subsections, "Native frames"))
        assertNotNull(subsectionByName(subsections, "siginfo"))
        assertNotNull(subsectionByName(subsections, "Registers"))
        assertNotNull(subsectionByName(subsections, "Register to memory mapping"))
        assertNotNull(subsectionByName(subsections, "Top of Stack"))
        assertNotNull(subsectionByName(subsections, "Instructions"))
        assertNotNull(subsectionByName(subsections, "Stack slot to memory mapping"))
    }

    private fun verifyProcess(section: HsErrSection) {
        val subsections = section.subsectionList
        assertNotNull(subsectionByName(subsections, "Threads class SMR info"))
        assertNotNull(subsectionByName(subsections, "Java Threads"))
        assertNotNull(subsectionByName(subsections, "Other Threads"))
        assertNotNull(subsectionByName(subsections, "Threads with active compile tasks"))
        assertNotNull(subsectionByName(subsections, "VM state"))
        assertNotNull(subsectionByName(subsections, "VM Mutex/Monitor currently owned by a thread"))
        assertNotNull(subsectionByName(subsections, "Heap address"))
        assertNotNull(subsectionByName(subsections, "CDS"))
        assertNotNull(subsectionByName(subsections, "Compressed class space mapped at"))
        assertNotNull(subsectionByName(subsections, "Narrow klass base"))
        assertNotNull(subsectionByName(subsections, "GC Precious Log"))
    }

    private fun verifyHeap(section: HsErrSection) {
        val subsections = section.subsectionList
        assertNotNull(subsectionByName(subsections, "Heap Regions"))
        assertNotNull(subsectionByName(subsections, "Card table byte_map"))
        assertNotNull(subsectionByName(subsections, "Marking Bits"))
        assertNotNull(subsectionByName(subsections, "Polling page"))
        assertNotNull(subsectionByName(subsections, "Metaspace"))
        assertNotNull(subsectionByName(subsections, "Usage"))
        assertNotNull(subsectionByName(subsections, "Virtual space"))
        assertNotNull(subsectionByName(subsections, "Chunk freelists"))
        assertNotNull(subsectionByName(subsections, "MaxMetaspaceSize"))
        assertNotNull(subsectionByName(subsections, "CompressedClassSpaceSize"))
        assertNotNull(subsectionByName(subsections, "Initial GC threshold"))
        assertNotNull(subsectionByName(subsections, "Current GC threshold"))
        assertNotNull(subsectionByName(subsections, "CDS"))
        assertNotNull(subsectionByName(subsections, "Internal statistics"))
        assertNotNull(subsectionByName(subsections, "CodeHeap"))
    }

    private fun verifyVMArgs(section: HsErrSection) {
        val subsections = section.subsectionList
        assertNotNull(subsectionByName(subsections, "jvm_args"))
        assertNotNull(subsectionByName(subsections, "java_command"))
        assertNotNull(subsectionByName(subsections, "java_class_path"))
        assertNotNull(subsectionByName(subsections, "Launcher Type"))
    }

    private fun verifySystem(section: HsErrSection) {
        val subsections = section.subsectionList
        assertNotNull(subsectionByName(subsections, "uname"))
        assertNotNull(subsectionByName(subsections, "OS uptime"))
        assertNotNull(subsectionByName(subsections, "libc"))
        assertNotNull(subsectionByName(subsections, "rlimit"))
        assertNotNull(subsectionByName(subsections, "load average"))
        assertNotNull(subsectionByName(subsections, "Process Memory"))
        assertNotNull(subsectionByName(subsections, "CPU"))
        assertNotNull(subsectionByName(subsections, "Memory"))
        assertNotNull(subsectionByName(subsections, "vm_info"))
    }

    private fun verifyEnvVars(section: HsErrSection) {
        assertNotNull(childWithText(section, "PATH"))
        assertNotNull(childWithText(section, "USERNAME"))
        assertNotNull(childWithText(section, "SHELL"))
        assertNotNull(childWithText(section, "DISPLAY"))
        assertNotNull(childWithText(section, "LANG"))
        assertNotNull(childWithText(section, "LC_NUMERIC"))
        assertNotNull(childWithText(section, "LC_TIME"))
        assertNotNull(childWithText(section, "TERM"))
    }

    private fun sectionByName(sections: Array<HsErrSection>, name: String): HsErrSection? {
        return sections.firstOrNull { it.name == name }
    }

    private fun subsectionByName(subsections: List<HsErrSubsection>, name: String): HsErrSubsection? {
        return subsections.firstOrNull { it.name == name }
    }

    private fun childWithText(element: PsiElement, name: String): PsiElement? {
        return element.children.firstOrNull { it.text == name }
    }
}
