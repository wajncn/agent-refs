package com.wajnc.codexapp

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

/** Opens the current project in one configured agent. */
class OpenAgentAction(private val agent: CodexAppSettings.AgentState) : AnAction(agent.name) {
    override fun update(event: AnActionEvent) {
        event.presentation.isEnabled = AgentLauncher.isSupportedPlatform() && event.project?.basePath != null
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        AgentLauncher.open(project, agent)
    }
}
