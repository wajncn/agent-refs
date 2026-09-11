package com.wajnc.codexapp

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo

class OpenCodexAppAction : AnAction() {
    override fun update(event: AnActionEvent) {
        event.presentation.isEnabled =
            (SystemInfo.isWindows || SystemInfo.isMac) &&
                event.project?.basePath != null
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val basePath = project.basePath ?: return

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val projectPath = CodexCommandRunner.resolveProjectPath(basePath)
                val settings = CodexAppSettings.getInstance()
                val processBuilder = when {
                    SystemInfo.isWindows -> CodexCommandRunner.windowsProcessBuilder(settings.windowsCommand, projectPath)
                    SystemInfo.isMac -> CodexCommandRunner.macProcessBuilder(settings.macCommand, projectPath)
                    else -> return@executeOnPooledThread
                }
                val process = processBuilder
                    .redirectErrorStream(true)
                    .start()
                val output = process.inputStream.bufferedReader().use { it.readText() }
                if (process.waitFor() != 0) {
                    notifyError(project, output.ifBlank { "PowerShell exited with an error." })
                }
            } catch (exception: Exception) {
                LOG.warn("Failed to open Codex App", exception)
                notifyError(project, exception.message ?: exception.javaClass.simpleName)
            }
        }
    }

    private fun notifyError(project: Project, message: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Codex App")
            .createNotification("Cannot open Codex App", message, NotificationType.ERROR)
            .notify(project)
    }

    companion object {
        private val LOG = Logger.getInstance(OpenCodexAppAction::class.java)
    }
}
