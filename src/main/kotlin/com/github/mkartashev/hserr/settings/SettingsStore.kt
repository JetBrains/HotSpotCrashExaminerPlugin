// Copyright 2024 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.github.mkartashev.hserr.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.Tag
import com.github.mkartashev.hserr.getService
import com.intellij.ui.JBColor
import java.awt.Color

@State(
    name = "SettingsStore",
    storages = [Storage(value = "hserr_crash_examiner_plugin.xml", roamingType = RoamingType.DEFAULT)]
)
class SettingsStore : PersistentStateComponent<SettingsStore.State>, Cloneable {

    companion object {
        const val CURRENT_SETTINGS_VERSION = "1"

        fun getInstance() = getService<SettingsStore>()
    }

    data class State(
        @Tag("settingsVersion")
        var version: String,
        @Tag("patternsToFold")
        var patternsToFold: String,
        @Tag("findAddrRange")
        var findAddrRange: ULong,
        @Tag("findAddrColorRGB")
        var findAddrColorRGB: Int,
        @Tag("maxThreads")
        var maxThreads: Int,
        @Tag("maxStacksSize")
        var maxStacksSize: ULong,
        @Tag("minFreeMemPercent")
        var minFreeMemPercent: Int,
        @Tag("minFreeHeapPercent")
        var minFreeHeapPercent: Int
    ) : Cloneable {
        constructor() : this(
            CURRENT_SETTINGS_VERSION,
            "Heap Regions",
            4UL * 1024UL,
            JBColor.ORANGE.rgb,
            300,
            (300UL * 2UL * 1024UL * 1024UL),
            2,
            5
        )

        public override fun clone(): State {
            return State(
                version,
                patternsToFold,
                findAddrRange,
                findAddrColorRGB,
                maxThreads,
                maxStacksSize,
                minFreeMemPercent,
                minFreeHeapPercent
            )
        }

        val findAddrColor: Color
                get() = Color(findAddrColorRGB)
    }

    private val cleanState = State()
    var theState = cleanState.clone()

    override fun getState(): State {
        return theState
    }

    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, theState)
    }

    override fun initializeComponent() {
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as SettingsStore

        return theState == other.theState
    }

    override fun hashCode(): Int {
        return theState.hashCode()
    }
}