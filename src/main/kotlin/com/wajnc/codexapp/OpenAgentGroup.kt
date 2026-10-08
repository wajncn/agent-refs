package com.wajnc.codexapp

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

/** Toolbar dropdown that lists every configured agent. */
class OpenAgentGroup : ActionGroup("Open Agent", true) {
    override fun getChildren(event: AnActionEvent?): Array<AnAction> =
        CodexAppSettings.getInstance().agents
            .filter { it.name.isNotBlank() }
            .map { OpenAgentAction(it) as AnAction }
            .toTypedArray()

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabled =
            AgentLauncher.isSupportedPlatform() &&
                event.project?.basePath != null &&
                CodexAppSettings.getInstance().agents.any { it.name.isNotBlank() }
    }
}
