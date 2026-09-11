package com.wajnc.codexapp

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.APP)
@State(name = "CodexAppSettings", storages = [Storage("codexApp.xml")])
class CodexAppSettings : PersistentStateComponent<CodexAppSettings.SettingsState> {
    class SettingsState {
        var command: String? = null
        var windowsCommand: String = DEFAULT_WINDOWS_COMMAND
        var macCommand: String = DEFAULT_MAC_COMMAND
    }

    private var settingsState = SettingsState()

    var windowsCommand: String
        get() = settingsState.windowsCommand
        set(value) {
            settingsState.windowsCommand = value
        }

    var macCommand: String
        get() = settingsState.macCommand
        set(value) {
            settingsState.macCommand = value
        }

    override fun getState(): SettingsState = settingsState

    override fun loadState(state: SettingsState) {
        if (!state.command.isNullOrBlank() && state.windowsCommand == DEFAULT_WINDOWS_COMMAND) {
            state.windowsCommand = state.command!!
        }
        if (state.macCommand == LEGACY_DEFAULT_MAC_COMMAND) {
            state.macCommand = DEFAULT_MAC_COMMAND
        }
        state.command = null
        settingsState = state
    }

    companion object {
        val DEFAULT_WINDOWS_COMMAND = """
            function codexapp {
                param([string] ${'$'}Path = ".")
                ${'$'}resolvedPath = (Resolve-Path -LiteralPath ${'$'}Path -ErrorAction Stop).Path
                Start-Process "codex://threads/new?path=${'$'}([uri]::EscapeDataString(${'$'}resolvedPath))"
            }

            codexapp -Path ${'$'}Path
        """.trimIndent()

        val DEFAULT_MAC_COMMAND = "codex app \"${'$'}CODEX_IDEA_PROJECT_PATH\""

        private val LEGACY_DEFAULT_MAC_COMMAND = """
            codexapp() {
                local path="${'$'}{1:-.}"
                local resolved_path
                resolved_path="${'$'}(cd "${'$'}path" && pwd -P)" || return 1
                local encoded_path
                encoded_path="${'$'}(/usr/bin/osascript -l JavaScript -e "function run(argv) { return encodeURIComponent(argv[0]).replace(/[!'()*]/g, function(c) { return '%' + c.charCodeAt(0).toString(16).toUpperCase(); }); }" "${'$'}resolved_path")" || return 1
                /usr/bin/open "codex://threads/new?path=${'$'}encoded_path"
            }

            codexapp "${'$'}CODEX_IDEA_PROJECT_PATH"
        """.trimIndent()

        fun getInstance(): CodexAppSettings =
            ApplicationManager.getApplication().getService(CodexAppSettings::class.java)
    }
}
