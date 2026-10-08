package com.wajnc.codexapp

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class CopyCodexReferenceAction : AnAction() {
    override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible = resolveCodexEditorTarget(event) != null
    }

    override fun actionPerformed(event: AnActionEvent) {
        val target = resolveCodexEditorTarget(event) ?: return
        CodexReferenceCopier.copy(target.project, target.file, target.editor)
    }
}
