package com.wajnc.codexapp

import java.nio.file.Path
import java.util.Base64

internal object CodexCommandRunner {
    const val PROJECT_PATH_ENV = "CODEX_IDEA_PROJECT_PATH"

    fun resolveProjectPath(projectPath: String): String = Path.of(projectPath).toRealPath().toString()

    fun windowsScript(command: String): String = "${'$'}Path = ${'$'}env:$PROJECT_PATH_ENV\n$command"

    fun windowsProcessBuilder(command: String, projectPath: String): ProcessBuilder {
        return windowsPowerShellProcessBuilder(windowsScript(command)).apply {
            environment()[PROJECT_PATH_ENV] = projectPath
        }
    }

    fun macProcessBuilder(command: String, projectPath: String): ProcessBuilder =
        ProcessBuilder("/bin/zsh", "-lc", command).apply {
            environment()[PROJECT_PATH_ENV] = projectPath
        }

    private fun windowsPowerShellProcessBuilder(script: String): ProcessBuilder {
        val encodedCommand = Base64.getEncoder()
            .encodeToString(script.toByteArray(Charsets.UTF_16LE))

        return ProcessBuilder(
            "powershell.exe",
            "-NoProfile",
            "-NoLogo",
            "-NonInteractive",
            "-WindowStyle",
            "Hidden",
            "-OutputFormat",
            "Text",
            "-EncodedCommand",
            encodedCommand,
        )
    }
}
