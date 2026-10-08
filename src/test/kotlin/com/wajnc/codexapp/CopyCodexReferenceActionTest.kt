package com.wajnc.codexapp

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.awt.datatransfer.DataFlavor
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.nio.file.Files
import java.nio.file.Path

class CopyCodexReferenceActionTest : BasePlatformTestCase() {
    fun testCtrlShiftCIsBoundToTheEditorComponent() {
        myFixture.configureByText("Example.txt", "copy me")

        val installedOnEditor = ActionUtil.getActions(myFixture.editor.contentComponent)
            .filterIsInstance<CopyCodexReferenceAction>()
        assertEquals(1, installedOnEditor.size)

        val expectedModifiers = InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK
        val keyStrokes = installedOnEditor.single()
            .shortcutSet
            .shortcuts
            .filterIsInstance<KeyboardShortcut>()
            .map { it.firstKeyStroke }
        assertTrue(
            "Expected Ctrl+Shift+C on the editor component, got $keyStrokes",
            keyStrokes.any {
                it.keyCode == KeyEvent.VK_C && (it.modifiers and expectedModifiers) == expectedModifiers
            },
        )
    }

    fun testSelectedCodeIsCopiedWithItsLineRange() {
        val editor = openProjectEditor("Example.txt", "first\nsecond\n")
        editor.selectionModel.setSelection(
            editor.document.getLineStartOffset(1),
            editor.document.getLineEndOffset(1),
        )

        CopyCodexReferenceAction().actionPerformed(eventFor(editor))

        assertEquals("@Example.txt#L2", copiedText())
    }

    fun testCodeWithoutSelectionFallsBackToTheFileReference() {
        val editor = openProjectEditor("Example.txt", "first\nsecond\n")
        editor.selectionModel.removeSelection()

        CopyCodexReferenceAction().actionPerformed(eventFor(editor))

        assertEquals("@Example.txt", copiedText())
    }

    fun testWordSelectionIsCopiedLikeAManualSelection() {
        val editor = openProjectEditor("Example.txt", "first\nsecond line\n")
        selectWord(editor, "second")

        CopyCodexReferenceAction().actionPerformed(eventFor(editor))

        assertEquals("@Example.txt#L2", copiedText())
    }

    fun testFloatingToolbarOffersTheActionForAWordSelection() {
        val editor = openProjectEditor("Example.txt", "first\nsecond line\n")
        selectWord(editor, "second")
        val action = CopySelectionCodexReferenceAction()

        val event = AnActionEvent.createFromAnAction(action, null, ActionPlaces.UNKNOWN, contextFor(editor))
        action.update(event)

        assertTrue(event.presentation.isEnabledAndVisible)
    }

    private fun selectWord(editor: Editor, word: String) {
        val start = editor.document.text.indexOf(word)
        assertTrue("Cannot find '$word' in the editor", start >= 0)
        editor.selectionModel.setSelection(start, start + word.length)
    }

    private fun openProjectEditor(fileName: String, text: String): Editor {
        val path = Path.of(project.basePath!!, fileName)
        Files.createDirectories(path.parent)
        Files.writeString(path, text)
        val file = LocalFileSystem.getInstance().refreshAndFindFileByPath(path.toString().replace('\\', '/'))
        assertNotNull("Cannot create $fileName in the project", file)
        myFixture.openFileInEditor(file!!)
        return myFixture.editor
    }

    private fun copiedText(): String {
        val copied = CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor) as String?
        assertNotNull("Nothing was copied to the clipboard", copied)
        return copied!!
    }

    private fun eventFor(editor: Editor): AnActionEvent {
        return AnActionEvent.createFromAnAction(CopyCodexReferenceAction(), null, ActionPlaces.UNKNOWN, contextFor(editor))
    }

    private fun contextFor(editor: Editor): DataContext {
        val context = SimpleDataContext.builder()
            .add(CommonDataKeys.PROJECT, project)
            .add(CommonDataKeys.EDITOR, editor)
            .add(CommonDataKeys.VIRTUAL_FILE, FileDocumentManager.getInstance().getFile(editor.document) as VirtualFile)
            .build()
        return context
    }
}
