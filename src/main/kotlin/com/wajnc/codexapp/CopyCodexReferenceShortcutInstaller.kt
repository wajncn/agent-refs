package com.wajnc.codexapp

import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.keymap.KeymapManager

/**
 * Binds the copy action directly to the editor component. Local shortcuts are resolved before the
 * keymap, so the action wins over the platform Copy Path/Reference action, which the default keymap
 * binds to the same keystroke.
 */
class CopyCodexReferenceShortcutInstaller : EditorFactoryListener {
    private val action = CopyCodexReferenceAction()

    override fun editorCreated(event: EditorFactoryEvent) {
        action.registerCustomShortcutSet(shortcuts(), event.editor.contentComponent)
    }

    override fun editorReleased(event: EditorFactoryEvent) {
        action.unregisterCustomShortcutSet(event.editor.contentComponent)
    }

    private fun shortcuts(): CustomShortcutSet {
        val fromKeymap = KeymapManager.getInstance()
            ?.activeKeymap
            ?.getShortcuts(ACTION_ID)
            ?.filterIsInstance<KeyboardShortcut>()
            .orEmpty()
        return if (fromKeymap.isEmpty()) {
            CustomShortcutSet.fromString(DEFAULT_SHORTCUT)
        } else {
            CustomShortcutSet(*fromKeymap.toTypedArray())
        }
    }

    private companion object {
        private const val ACTION_ID = "com.wajnc.codexapp.CopyCodexReference"
        private const val DEFAULT_SHORTCUT = "control shift C"
    }
}