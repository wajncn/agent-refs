package com.wajnc.codexapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Base64

class CodexPowerShellCommandTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `passes project path through environment without interpolating it into the script`() {
        val projectPath = "C:\\work path\\project's folder"
        val command = "Write-Output ${'$'}path"

        val processBuilder = CodexCommandRunner.windowsProcessBuilder(command, projectPath)
        val decodedScript = String(
            Base64.getDecoder().decode(processBuilder.command().last()),
            Charsets.UTF_16LE,
        )

        assertEquals(projectPath, processBuilder.environment()[CodexCommandRunner.PROJECT_PATH_ENV])
        assertEquals(command, decodedScript.lineSequence().last())
        assertTrue(decodedScript.contains("${'$'}env:${CodexCommandRunner.PROJECT_PATH_ENV}"))
        assertFalse(decodedScript.contains(projectPath))
    }

    @Test
    fun `powershell receives the exact project path`() {
        val projectPath = "C:\\work path\\project's folder"
        val process = CodexCommandRunner.windowsProcessBuilder("Write-Output ${'$'}Path", projectPath)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }.trim()

        assertEquals(0, process.waitFor())
        assertEquals(projectPath, output)
    }

    @Test
    fun `default codex command opens the encoded real project path`() {
        val projectPath = temporaryFolder.newFolder("work path", "project's folder").canonicalPath
        val output = runDefaultWindowsCommand(AgentDefaults.CODEX_WINDOWS_COMMAND, projectPath)

        assertEquals("codex://threads/new?path=${encoded(projectPath)}", output)
    }

    @Test
    fun `default zcode command opens the encoded real project path`() {
        val projectPath = temporaryFolder.newFolder("work path", "project's folder").canonicalPath
        val output = runDefaultWindowsCommand(AgentDefaults.ZCODE_WINDOWS_COMMAND, projectPath)

        assertEquals("zcode://workspace/open?path=${encoded(projectPath)}", output)
    }

    @Test
    fun `mac command receives project path through environment`() {
        val projectPath = "/Users/example/work project"

        val processBuilder = CodexCommandRunner.macProcessBuilder("echo ok", projectPath)

        assertEquals(listOf("/bin/zsh", "-lc", "echo ok"), processBuilder.command())
        assertEquals(projectPath, processBuilder.environment()[CodexCommandRunner.PROJECT_PATH_ENV])
    }

    @Test
    fun `resolves project path to an absolute real path`() {
        val resolvedPath = CodexCommandRunner.resolveProjectPath(".")

        assertTrue(java.nio.file.Path.of(resolvedPath).isAbsolute)
        assertEquals(java.nio.file.Path.of(".").toRealPath().toString(), resolvedPath)
    }

    @Test
    fun `default mac commands launch the agents with the absolute project path`() {
        assertEquals("codex app \"${'$'}CODEX_IDEA_PROJECT_PATH\"", AgentDefaults.CODEX_MAC_COMMAND)
        assertTrue(AgentDefaults.ZCODE_MAC_COMMAND.contains("zcode://workspace/open?path="))
        assertTrue(AgentDefaults.ZCODE_MAC_COMMAND.contains("${'$'}CODEX_IDEA_PROJECT_PATH"))
    }

    private fun runDefaultWindowsCommand(command: String, projectPath: String): String {
        val script = """
            function Start-Process { param([string] ${'$'}FilePath) Write-Output ${'$'}FilePath }
            $command
        """.trimIndent()
        val process = CodexCommandRunner.windowsProcessBuilder(script, projectPath)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
        assertEquals(0, process.waitFor())
        return output
    }

    private fun encoded(projectPath: String): String = URLEncoder
        .encode(projectPath, StandardCharsets.UTF_8)
        .replace("+", "%20")
        .replace("%27", "'")
}
