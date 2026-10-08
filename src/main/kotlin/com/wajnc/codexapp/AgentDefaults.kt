package com.wajnc.codexapp

/** Built-in launch commands for the agents that ship with the plugin. */
internal object AgentDefaults {
    val CODEX_WINDOWS_COMMAND = """
        function codexapp {
            param([string] ${'$'}Path = ".")
            ${'$'}resolvedPath = (Resolve-Path -LiteralPath ${'$'}Path -ErrorAction Stop).Path
            Start-Process "codex://threads/new?path=${'$'}([uri]::EscapeDataString(${'$'}resolvedPath))"
        }

        codexapp -Path ${'$'}Path
    """.trimIndent()

    val CODEX_MAC_COMMAND = "codex app \"${'$'}CODEX_IDEA_PROJECT_PATH\""

    val ZCODE_WINDOWS_COMMAND = """
        function zcodeapp {
            param([string] ${'$'}Path = ".")
            ${'$'}resolvedPath = (Resolve-Path -LiteralPath ${'$'}Path -ErrorAction Stop).Path
            Start-Process "zcode://workspace/open?path=${'$'}([uri]::EscapeDataString(${'$'}resolvedPath))"
        }

        zcodeapp -Path ${'$'}Path
    """.trimIndent()

    val ZCODE_MAC_COMMAND = """
        resolved_path="${'$'}(cd "${'$'}CODEX_IDEA_PROJECT_PATH" && pwd -P)" || exit 1
        encoded_path="${'$'}(/usr/bin/osascript -l JavaScript -e 'function run(argv) { return encodeURIComponent(argv[0]) }' "${'$'}resolved_path")" || exit 1
        /usr/bin/open "zcode://workspace/open?path=${'$'}encoded_path"
    """.trimIndent()

    /** The macOS command shipped before the multi-agent settings existed. */
    val LEGACY_CODEX_MAC_COMMAND = """
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

    fun defaultAgents(): List<CodexAppSettings.AgentState> = listOf(
        CodexAppSettings.AgentState(
            name = "Codex",
            windowsCommand = CODEX_WINDOWS_COMMAND,
            macCommand = CODEX_MAC_COMMAND,
        ),
        CodexAppSettings.AgentState(
            name = "ZCode",
            windowsCommand = ZCODE_WINDOWS_COMMAND,
            macCommand = ZCODE_MAC_COMMAND,
        ),
    )
}
