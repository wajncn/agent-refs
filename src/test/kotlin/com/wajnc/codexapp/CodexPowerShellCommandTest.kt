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
    fun `append command passes the exact reference through the environment`() {
        val reference = "broker-trading-svc\\src\\main\\java\\Example.java#L53-54"
        val projectPath = "C:\\work path\\project's folder"
        val openCommand = "Write-Output ${'$'}Path"

        val processBuilder = CodexCommandRunner.windowsAppendProcessBuilder(reference, openCommand, projectPath)
        val decodedScript = String(
            Base64.getDecoder().decode(processBuilder.command().last()),
            Charsets.UTF_16LE,
        )

        assertEquals(reference, processBuilder.environment()[CodexCommandRunner.SELECTION_REFERENCE_ENV])
        assertEquals(projectPath, processBuilder.environment()[CodexCommandRunner.PROJECT_PATH_ENV])
        assertTrue(decodedScript.contains(openCommand))
        assertTrue(decodedScript.contains("Get-CodexAppProcess"))
        assertTrue(decodedScript.contains("Get-CodexProjectButton"))
        assertTrue(decodedScript.contains("Split-Path -Leaf ${'$'}Path"))
        assertTrue(decodedScript.contains("Current.Name.EndsWith"))
        assertTrue(decodedScript.contains("Codex App did not open within 15 seconds."))
        assertTrue(decodedScript.contains("Codex App did not open project '${'$'}projectName' within 15 seconds."))
        assertTrue(decodedScript.contains("ValuePattern"))
        assertTrue(decodedScript.contains("ProseMirror"))
        assertTrue(decodedScript.contains("ProseMirror-trailingBreak"))
        assertTrue(decodedScript.contains("SendKeys(\"^{END}\")"))
        assertTrue(decodedScript.contains("${'$'}env:${CodexCommandRunner.SELECTION_REFERENCE_ENV}"))
        assertFalse(decodedScript.contains(reference))
        assertFalse(decodedScript.contains(projectPath))
    }

    @Test
    fun `persistent append script encodes request data instead of interpolating it`() {
        val reference = "project with spaces\\src\\Example.kt#L1-2"
        val projectPath = "C:\\work path\\project's folder"
        val script = CodexCommandRunner.windowsPersistentAppendScript(reference, "Write-Output ${'$'}Path", projectPath)

        assertTrue(script.contains("FromBase64String"))
        assertFalse(script.contains(reference))
        assertFalse(script.contains(projectPath))
    }

    @Test
    fun `default windows command opens the encoded real project path`() {
        val projectPath = temporaryFolder.newFolder("work path", "project's folder").canonicalPath
        val command = """
            function Start-Process { param([string] ${'$'}FilePath) Write-Output ${'$'}FilePath }
            ${CodexAppSettings.DEFAULT_WINDOWS_COMMAND}
        """.trimIndent()
        val process = CodexCommandRunner.windowsProcessBuilder(command, projectPath)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }.trim()

        assertEquals(output, 0, process.waitFor())
        val encodedPath = URLEncoder.encode(projectPath, StandardCharsets.UTF_8)
            .replace("+", "%20")
            .replace("%27", "'")
        assertEquals("codex://threads/new?path=$encodedPath", output)
    }

    @Test
    fun `mac command receives project path through environment`() {
        val projectPath = "/Users/example/work project"

        val processBuilder = CodexCommandRunner.macProcessBuilder("echo ok", projectPath)

        assertEquals(listOf("/bin/zsh", "-lc", "echo ok"), processBuilder.command())
        assertEquals(projectPath, processBuilder.environment()[CodexCommandRunner.PROJECT_PATH_ENV])
    }

    @Test
    fun `mac append command uses osascript and passes reference through environment`() {
        val reference = "project/src/Example.java#L10-12"
        val projectPath = "/Users/example/work project"
        val openCommand = "codex app \"${'$'}CODEX_IDEA_PROJECT_PATH\""

        val processBuilder = CodexCommandRunner.macAppendProcessBuilder(reference, openCommand, projectPath)
        val script = processBuilder.command().last()

        assertEquals(listOf("/bin/zsh", "-lc", script), processBuilder.command())
        assertEquals(reference, processBuilder.environment()[CodexCommandRunner.SELECTION_REFERENCE_ENV])
        assertEquals(projectPath, processBuilder.environment()[CodexCommandRunner.PROJECT_PATH_ENV])
        assertTrue(script.contains("/usr/bin/osascript"))
        assertTrue(script.contains("AXTextArea"))
        assertTrue(script.contains("AXComboBox"))
        assertTrue(script.contains("随心输入"))
        assertTrue(script.contains("AXRaise"))
        assertTrue(script.contains("key code 125 using {command down}"))
        assertTrue(script.contains("keystroke \"v\" using {command down}"))
        assertTrue(script.contains(openCommand))
        assertFalse(script.contains(reference))
        assertFalse(script.contains(projectPath))
    }

    @Test
    fun `resolves project path to an absolute real path`() {
        val resolvedPath = CodexCommandRunner.resolveProjectPath(".")

        assertTrue(java.nio.file.Path.of(resolvedPath).isAbsolute)
        assertEquals(java.nio.file.Path.of(".").toRealPath().toString(), resolvedPath)
    }

    @Test
    fun `default mac command opens codex app with the absolute project path`() {
        assertEquals(
            "codex app \"${'$'}CODEX_IDEA_PROJECT_PATH\"",
            CodexAppSettings.DEFAULT_MAC_COMMAND,
        )
    }
}
