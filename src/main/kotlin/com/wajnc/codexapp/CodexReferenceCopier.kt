package com.wajnc.codexapp

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VirtualFile
import java.awt.datatransfer.StringSelection

internal const val NOTIFICATION_GROUP = "Agent Refs"

internal object CodexReferenceCopier {
    fun copy(project: Project, file: VirtualFile, editor: Editor?): Boolean {
        val reference = buildReference(project, file, editor) ?: return false
        try {
            CopyPasteManager.getInstance().setContents(StringSelection(reference))
        } catch (exception: RuntimeException) {
            notifyError(project, exception.message ?: "Cannot access the system clipboard.")
            return false
        }
        notifyCopied(project)
        return true
    }

    fun buildReference(project: Project, file: VirtualFile, editor: Editor?): String? {
        val projectPath = project.basePath ?: return null
        val selection = editor?.selectionModel
        val pathSeparator = if (SystemInfo.isMac) "/" else "\\"
        return try {
            if (editor != null && selection != null && selection.hasSelection()) {
                SelectionReferenceBuilder.buildSelection(
                    projectPath,
                    file.path,
                    editor.document,
                    selection.selectionStart,
                    selection.selectionEnd,
                    pathSeparator = pathSeparator,
                )
            } else {
                SelectionReferenceBuilder.buildFile(projectPath, file.path, pathSeparator = pathSeparator)
            }
        } catch (exception: IllegalArgumentException) {
            notifyError(project, exception.message ?: "Cannot build the code reference.")
            null
        }
    }

    private fun notifyCopied(project: Project) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP)
            .createNotification(
                "Code Reference Copied",
                "Paste it into your agent's input.",
                NotificationType.INFORMATION,
            )
            .notify(project)
    }

    private fun notifyError(project: Project, message: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP)
            .createNotification("Cannot copy the code reference", message, NotificationType.ERROR)
            .notify(project)
    }
}
