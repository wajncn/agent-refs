package com.wajnc.codexapp

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.FormBuilder
import javax.swing.JButton
import javax.swing.JComponent

class CodexAppSettingsConfigurable : Configurable {
    private var windowsCommandField: JBTextArea? = null
    private var macCommandField: JBTextArea? = null

    override fun getDisplayName(): String = "Codex App Launcher"

    override fun createComponent(): JComponent {
        val windowsField = JBTextArea(10, 80).apply {
            lineWrap = false
        }
        val macField = JBTextArea(10, 80).apply {
            lineWrap = false
        }
        windowsCommandField = windowsField
        macCommandField = macField
        val restoreDefaultsButton = JButton("Restore Defaults").apply {
            addActionListener {
                windowsField.text = CodexAppSettings.DEFAULT_WINDOWS_COMMAND
                macField.text = CodexAppSettings.DEFAULT_MAC_COMMAND
            }
        }

        return FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Windows PowerShell command:"), JBScrollPane(windowsField), 1, false)
            .addLabeledComponent(JBLabel("macOS shell command:"), JBScrollPane(macField), 1, false)
            .addComponent(restoreDefaultsButton)
            .panel
    }

    override fun isModified(): Boolean =
        windowsCommandField?.text != CodexAppSettings.getInstance().windowsCommand ||
            macCommandField?.text != CodexAppSettings.getInstance().macCommand

    override fun apply() {
        val windowsCommand = windowsCommandField?.text.orEmpty()
        val macCommand = macCommandField?.text.orEmpty()
        if (windowsCommand.isBlank()) {
            throw ConfigurationException("The Windows PowerShell command cannot be empty.")
        }
        if (macCommand.isBlank()) {
            throw ConfigurationException("The macOS shell command cannot be empty.")
        }
        CodexAppSettings.getInstance().windowsCommand = windowsCommand
        CodexAppSettings.getInstance().macCommand = macCommand
    }

    override fun reset() {
        windowsCommandField?.text = CodexAppSettings.getInstance().windowsCommand
        macCommandField?.text = CodexAppSettings.getInstance().macCommand
    }

    override fun disposeUIResources() {
        windowsCommandField = null
        macCommandField = null
    }
}
