package com.wajnc.codexapp

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo

internal const val AGENT_NOTIFICATION_GROUP = "Agent Refs"

/** Runs the configured launch command of an agent for the current project. */
internal object AgentLauncher {
    fun isSupportedPlatform(): Boolean = SystemInfo.isWindows || SystemInfo.isMac

    fun open(project: Project, agent: CodexAppSettings.AgentState) {
        val basePath = project.basePath ?: return
        val command = when {
            SystemInfo.isWindows -> agent.windowsCommand
            SystemInfo.isMac -> agent.macCommand
            else -> return
        }
        val isWindows = SystemInfo.isWindows

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val projectPath = CodexCommandRunner.resolveProjectPath(basePath)
                val processBuilder = if (isWindows) {
                    CodexCommandRunner.windowsProcessBuilder(command, projectPath)
                } else {
                    CodexCommandRunner.macProcessBuilder(command, projectPath)
                }
                val process = processBuilder
                    .redirectErrorStream(true)
                    .start()
                val output = process.inputStream.bufferedReader().use { it.readText() }
                if (process.waitFor() != 0) {
                    notifyError(project, agent.name, output.ifBlank { "The launch command exited with an error." })
                }
            } catch (exception: Exception) {
                LOG.warn("Failed to open ${agent.name}", exception)
                notifyError(project, agent.name, exception.message ?: exception.javaClass.simpleName)
            }
        }
    }

    private fun notifyError(project: Project, agentName: String, message: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(AGENT_NOTIFICATION_GROUP)
            .createNotification("Cannot open $agentName", message, NotificationType.ERROR)
            .notify(project)
    }

    private val LOG = Logger.getInstance(AgentLauncher::class.java)
}
