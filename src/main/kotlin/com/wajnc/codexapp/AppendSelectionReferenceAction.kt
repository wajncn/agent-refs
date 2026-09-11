package com.wajnc.codexapp

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import java.awt.datatransfer.StringSelection

class AppendSelectionReferenceAction : AnAction() {
    override fun update(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR)
        event.presentation.isEnabledAndVisible =
            SystemInfo.isWindows &&
                event.project?.basePath != null &&
                event.getData(CommonDataKeys.VIRTUAL_FILE) != null &&
                editor?.selectionModel?.hasSelection() == true
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val projectPath = project.basePath ?: return
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val selection = editor.selectionModel
        if (!selection.hasSelection()) return

        val reference = try {
            SelectionReferenceBuilder.buildSelection(
                projectPath,
                file.path,
                editor.document,
                selection.selectionStart,
                selection.selectionEnd,
                pathSeparator = if (SystemInfo.isMac) "/" else "\\",
            )
        } catch (exception: IllegalArgumentException) {
            CodexReferenceSender.notifyError(project, exception.message ?: "Cannot build the selected code reference.")
            return
        }

        CodexReferenceSender.send(project, reference)
    }
}

internal object CodexReferenceSender {
    private val LOG = Logger.getInstance(CodexReferenceSender::class.java)
    private val WINDOWS_RUNNER = PersistentPowerShellRunner()

    fun send(project: Project, reference: String) {
        copyToClipboard(reference)
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val projectPath = CodexCommandRunner.resolveProjectPath(project.basePath ?: return@executeOnPooledThread)
                val settings = CodexAppSettings.getInstance()
                if (SystemInfo.isWindows) {
                    val output = WINDOWS_RUNNER.execute(
                        CodexCommandRunner.windowsPersistentAppendScript(
                            reference,
                            settings.windowsCommand,
                            projectPath,
                        ),
                    )
                    if (output.errorMessage != null) {
                        notifyError(project, output.errorMessage)
                    }
                    return@executeOnPooledThread
                }
                val processBuilder = when {
                    SystemInfo.isMac -> CodexCommandRunner.macAppendProcessBuilder(
                        reference,
                        settings.macCommand,
                        projectPath,
                    )
                    else -> return@executeOnPooledThread
                }
                val process = processBuilder
                    .redirectErrorStream(true)
                    .start()
                val output = process.inputStream.bufferedReader().use { it.readText() }
                if (process.waitFor() != 0) {
                    notifyError(project, output.ifBlank { "Codex App command exited with an error." })
                }
            } catch (exception: Exception) {
                LOG.warn("Failed to append the reference to Codex App", exception)
                notifyError(project, exception.message ?: exception.javaClass.simpleName)
            }
        }
    }

    private fun copyToClipboard(reference: String) {
        try {
            CopyPasteManager.getInstance().setContents(StringSelection(reference))
        } catch (exception: RuntimeException) {
            LOG.warn("Failed to copy the reference to the clipboard", exception)
        }
    }

    private class PersistentPowerShellRunner {
        private val marker = "__CODEX_IDEA_DONE__"
        private val errorMarker = "__CODEX_IDEA_ERROR__"
        private val lock = Any()
        private var process: Process? = null
        private var input: java.io.BufferedWriter? = null
        private var output: java.io.BufferedReader? = null

        fun execute(script: String): Result = synchronized(lock) {
            ensureProcess()
            val writer = input ?: error("PowerShell input is unavailable")
            val reader = output ?: error("PowerShell output is unavailable")
            val encodedScript = java.util.Base64.getEncoder().encodeToString(script.toByteArray(Charsets.UTF_8))
            writer.write(encodedScript)
            writer.newLine()
            writer.flush()

            val lines = mutableListOf<String>()
            var hasError = false
            while (true) {
                val line = reader.readLine() ?: throw IllegalStateException("Persistent PowerShell exited unexpectedly")
                if (line == errorMarker) {
                    hasError = true
                    continue
                }
                if (line == marker) break
                lines += line
            }
            Result(if (hasError) lines.joinToString("\n").ifBlank { "PowerShell command failed." } else null)
        }

        private fun ensureProcess(): Process {
            val existing = process
            if (existing != null && existing.isAlive) return existing
            input?.close()
            output?.close()
            val started = ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-NoLogo",
                "-NonInteractive",
                "-WindowStyle",
                "Hidden",
                "-Command",
                "while (${ '$' }line = [Console]::In.ReadLine()) { try { Invoke-Expression ([Text.Encoding]::UTF8.GetString([Convert]::FromBase64String(${ '$' }line))); Write-Output '$marker' } catch { Write-Output '$errorMarker'; Write-Output ${ '$' }_.Exception.Message; Write-Output '$marker' } }",
            ).redirectErrorStream(true).start()
            process = started
            input = started.outputStream.bufferedWriter()
            output = started.inputStream.bufferedReader()
            return started
        }

        data class Result(val errorMessage: String?)
    }

    fun notifyError(project: Project, message: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Codex App")
            .createNotification("Cannot add reference to Codex App", message, NotificationType.ERROR)
            .notify(project)
    }
}
