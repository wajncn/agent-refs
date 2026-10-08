package com.wajnc.codexapp

import com.intellij.openapi.editor.Document
import java.nio.file.Path

internal object SelectionReferenceBuilder {
    fun buildSelection(
        projectPath: String,
        filePath: String,
        document: Document,
        selectionStart: Int,
        selectionEnd: Int,
        pathSeparator: String = "\\",
    ): String {
        val startLine = document.getLineNumber(selectionStart) + 1
        val inclusiveEndOffset = (selectionEnd - 1).coerceAtLeast(selectionStart)
        val endLine = document.getLineNumber(inclusiveEndOffset) + 1
        return build(projectPath, filePath, startLine, endLine, pathSeparator)
    }

    fun build(
        projectPath: String,
        filePath: String,
        startLine: Int,
        endLine: Int,
        pathSeparator: String = "\\",
    ): String = buildFile(projectPath, filePath, pathSeparator) +
        if (startLine == endLine) "#L$startLine" else "#L$startLine-$endLine"

    fun buildFile(projectPath: String, filePath: String, pathSeparator: String = "\\"): String {
        val projectRoot = Path.of(projectPath).toAbsolutePath().normalize()
        val selectedFile = Path.of(filePath).toAbsolutePath().normalize()
        require(selectedFile.startsWith(projectRoot)) { "The selected file is outside the current project." }

        val relativePath = projectRoot.relativize(selectedFile)
            .joinToString(pathSeparator) { it.toString() }

        return "@$relativePath"
    }
}
