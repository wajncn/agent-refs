package com.wajnc.codexapp

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

internal class CodexEditorTarget(val project: Project, val file: VirtualFile, val editor: Editor?)

internal fun resolveCodexEditorTarget(event: AnActionEvent): CodexEditorTarget? {
    val editor = event.getData(CommonDataKeys.EDITOR)
    val project = event.project ?: editor?.project ?: return null
    if (project.basePath == null) return null
    val file = editorFile(editor) ?: event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return null
    return CodexEditorTarget(project, file, editor)
}

private fun editorFile(editor: Editor?): VirtualFile? =
    editor?.document?.let { FileDocumentManager.getInstance().getFile(it) }
