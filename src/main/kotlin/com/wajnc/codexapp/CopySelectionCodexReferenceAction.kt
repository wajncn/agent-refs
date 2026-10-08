package com.wajnc.codexapp

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.ui.codeFloatingToolbar.CodeFloatingToolbar

class CopySelectionCodexReferenceAction : AnAction() {
    override fun update(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR)
        event.presentation.isEnabledAndVisible =
            event.project?.basePath != null &&
                event.getData(CommonDataKeys.VIRTUAL_FILE) != null &&
                editor?.selectionModel?.hasSelection() == true
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        if (!editor.selectionModel.hasSelection()) return
        if (CodexReferenceCopier.copy(project, file, editor)) {
            CodeFloatingToolbar.getToolbar(editor)?.scheduleHide()
        }
    }
}
