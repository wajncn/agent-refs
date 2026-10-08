package com.wajnc.codexapp

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.APP)
@State(name = "CodexAppSettings", storages = [Storage("codexApp.xml")])
class CodexAppSettings : PersistentStateComponent<CodexAppSettings.SettingsState> {
    class AgentState(
        var name: String = "",
        var windowsCommand: String = "",
        var macCommand: String = "",
    )

    class SettingsState {
        var agents: MutableList<AgentState> = mutableListOf()

        /** Single-agent settings written by older plugin versions, read only for migration. */
        var command: String? = null
        var windowsCommand: String? = null
        var macCommand: String? = null
    }

    private var settingsState = SettingsState().apply {
        agents = AgentDefaults.defaultAgents().toMutableList()
    }

    var agents: List<AgentState>
        get() = settingsState.agents
        set(value) {
            settingsState.agents = value.toMutableList()
        }

    override fun getState(): SettingsState = settingsState

    override fun loadState(state: SettingsState) {
        if (state.agents.isEmpty()) state.agents.addAll(migrateLegacyAgent(state))
        state.command = null
        state.windowsCommand = null
        state.macCommand = null
        settingsState = state
    }

    private fun migrateLegacyAgent(state: SettingsState): List<AgentState> {
        val legacyWindows = state.windowsCommand?.takeIf { it.isNotBlank() }
            ?: state.command?.takeIf { it.isNotBlank() }
        val legacyMac = state.macCommand?.takeIf { it.isNotBlank() }
        if (legacyWindows == null && legacyMac == null) return emptyList()

        val codexWindows = legacyWindows
            ?.takeIf { it != AgentDefaults.CODEX_WINDOWS_COMMAND }
            ?: AgentDefaults.CODEX_WINDOWS_COMMAND
        val codexMac = legacyMac
            ?.takeIf { it != AgentDefaults.LEGACY_CODEX_MAC_COMMAND }
            ?: AgentDefaults.CODEX_MAC_COMMAND
        return listOf(
            AgentState("Codex", codexWindows, codexMac),
            AgentState("ZCode", AgentDefaults.ZCODE_WINDOWS_COMMAND, AgentDefaults.ZCODE_MAC_COMMAND),
        )
    }

    companion object {
        fun getInstance(): CodexAppSettings =
            ApplicationManager.getApplication().getService(CodexAppSettings::class.java)
    }
}
