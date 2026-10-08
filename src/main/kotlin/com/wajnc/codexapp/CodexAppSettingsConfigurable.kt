package com.wajnc.codexapp

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.border.TitledBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class CodexAppSettingsConfigurable : Configurable {
    private var cardsPanel: JPanel? = null
    private val cards = ArrayList<AgentCard>()

    private class AgentCard(
        val nameField: JBTextField,
        val windowsCommandField: JBTextArea,
        val macCommandField: JBTextArea,
        val panel: JPanel,
    )

    override fun getDisplayName(): String = "Agent Refs"

    override fun createComponent(): JComponent {
        val cardsPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
        }
        this.cardsPanel = cardsPanel
        val buttons = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
            border = JBUI.Borders.emptyTop(8)
            add(JButton("Add Agent").apply {
                addActionListener {
                    cards.add(createCard(CodexAppSettings.AgentState()))
                    refreshCards()
                }
            })
            add(Box.createHorizontalStrut(8))
            add(JButton("Restore Defaults").apply {
                addActionListener { showAgents(AgentDefaults.defaultAgents()) }
            })
        }
        reset()
        return JPanel(BorderLayout()).apply {
            add(JBScrollPane(cardsPanel), BorderLayout.CENTER)
            add(buttons, BorderLayout.SOUTH)
        }
    }

    override fun isModified(): Boolean {
        val settings = CodexAppSettings.getInstance().agents
        val current = cardAgents()
        return current.size != settings.size || current.zip(settings).any { (left, right) ->
            left.name != right.name ||
                left.windowsCommand != right.windowsCommand ||
                left.macCommand != right.macCommand
        }
    }

    override fun apply() {
        val agents = cardAgents()
        agents.forEachIndexed { index, agent ->
            if (agent.name.isBlank()) {
                throw ConfigurationException("Agent ${index + 1} needs a name.")
            }
            if (agent.windowsCommand.isBlank()) {
                throw ConfigurationException("Agent ${index + 1} needs a Windows PowerShell command.")
            }
            if (agent.macCommand.isBlank()) {
                throw ConfigurationException("Agent ${index + 1} needs a macOS shell command.")
            }
        }
        CodexAppSettings.getInstance().agents = agents
    }

    override fun reset() {
        showAgents(CodexAppSettings.getInstance().agents)
    }

    override fun disposeUIResources() {
        cards.clear()
        cardsPanel = null
    }

    private fun showAgents(agents: List<CodexAppSettings.AgentState>) {
        cards.clear()
        agents.forEach { cards.add(createCard(it)) }
        refreshCards()
    }

    private fun createCard(agent: CodexAppSettings.AgentState): AgentCard {
        val nameField = JBTextField(agent.name)
        val windowsCommandField = JBTextArea(agent.windowsCommand, 8, 60).apply { lineWrap = false }
        val macCommandField = JBTextArea(agent.macCommand, 8, 60).apply { lineWrap = false }
        val panel = JPanel(BorderLayout()).apply {
            border = IdeBorderFactory.createTitledBorder(agent.name)
        }
        val card = AgentCard(nameField, windowsCommandField, macCommandField, panel)
        nameField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(event: DocumentEvent) = updateTitle()

            override fun removeUpdate(event: DocumentEvent) = updateTitle()

            override fun changedUpdate(event: DocumentEvent) = updateTitle()

            private fun updateTitle() {
                (panel.border as? TitledBorder)?.title = nameField.text
                panel.repaint()
            }
        })
        panel.add(
            FormBuilder.createFormBuilder()
                .addLabeledComponent(JBLabel("Name:"), nameField, 1, false)
                .addLabeledComponent(
                    JBLabel("Windows PowerShell command:"),
                    JBScrollPane(windowsCommandField),
                    1,
                    false,
                )
                .addLabeledComponent(JBLabel("macOS shell command:"), JBScrollPane(macCommandField), 1, false)
                .addComponent(JButton("Remove").apply {
                    addActionListener {
                        cards.remove(card)
                        refreshCards()
                    }
                })
                .panel,
            BorderLayout.CENTER,
        )
        return card
    }

    private fun refreshCards() {
        val panel = cardsPanel ?: return
        panel.removeAll()
        cards.forEachIndexed { index, card ->
            if (index > 0) panel.add(Box.createVerticalStrut(8))
            panel.add(card.panel)
        }
        panel.revalidate()
        panel.repaint()
    }

    private fun cardAgents(): List<CodexAppSettings.AgentState> = cards.map { card ->
        CodexAppSettings.AgentState(
            name = card.nameField.text.trim(),
            windowsCommand = card.windowsCommandField.text.trim(),
            macCommand = card.macCommandField.text.trim(),
        )
    }
}
