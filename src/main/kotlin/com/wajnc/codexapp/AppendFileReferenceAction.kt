package com.wajnc.codexapp

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.util.SystemInfo

class AppendFileReferenceAction : AnAction() {
    override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible =
            SystemInfo.isWindows &&
                event.project?.basePath != null &&
                event.getData(CommonDataKeys.VIRTUAL_FILE) != null
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val projectPath = project.basePath ?: return
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val editor = event.getData(CommonDataKeys.EDITOR)
        val selection = editor?.selectionModel

        val reference = try {
            if (editor != null && selection != null && selection.hasSelection()) {
                SelectionReferenceBuilder.buildSelection(
                    projectPath,
                    file.path,
                    editor.document,
                    selection.selectionStart,
                    selection.selectionEnd,
                    pathSeparator = if (SystemInfo.isMac) "/" else "\\",
                )
            } else {
                SelectionReferenceBuilder.buildFile(
                    projectPath,
                    file.path,
                    pathSeparator = if (SystemInfo.isMac) "/" else "\\",
                )
            }
        } catch (exception: IllegalArgumentException) {
            CodexReferenceSender.notifyError(project, exception.message ?: "Cannot build the code reference.")
            return
        }

        CodexReferenceSender.send(project, reference)
    }
}
