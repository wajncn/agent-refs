package com.wajnc.codexapp

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.awt.Component
import java.awt.Container
import javax.swing.JButton
import javax.swing.JTextArea
import javax.swing.JTextField

class CodexAppSettingsConfigurableTest : BasePlatformTestCase() {
    private lateinit var originalAgents: List<CodexAppSettings.AgentState>

    override fun setUp() {
        super.setUp()
        originalAgents = CodexAppSettings.getInstance().agents
    }

    override fun tearDown() {
        try {
            CodexAppSettings.getInstance().agents = originalAgents
        } finally {
            super.tearDown()
        }
    }

    fun testUsesThePluginNameAsTheSettingsDisplayName() {
        assertEquals("Agent Refs", CodexAppSettingsConfigurable().displayName)
    }

    fun testMigratesTheLegacySingleAgentSettingsIntoTheCodexCard() {
        val settings = CodexAppSettings()
        val state = CodexAppSettings.SettingsState().apply {
            windowsCommand = "custom windows command"
            macCommand = AgentDefaults.LEGACY_CODEX_MAC_COMMAND
        }

        settings.loadState(state)

        assertEquals(listOf("Codex", "ZCode"), settings.agents.map { it.name })
        assertEquals("custom windows command", settings.agents[0].windowsCommand)
        assertEquals(AgentDefaults.CODEX_MAC_COMMAND, settings.agents[0].macCommand)
        assertEquals(AgentDefaults.ZCODE_WINDOWS_COMMAND, settings.agents[1].windowsCommand)
    }

    fun testKeepsTheBuiltInCommandsOfAnUnchangedLegacyInstallation() {
        val settings = CodexAppSettings()
        val state = CodexAppSettings.SettingsState().apply {
            windowsCommand = AgentDefaults.CODEX_WINDOWS_COMMAND
            macCommand = AgentDefaults.CODEX_MAC_COMMAND
        }

        settings.loadState(state)

        assertEquals(AgentDefaults.CODEX_WINDOWS_COMMAND, settings.agents[0].windowsCommand)
        assertEquals(AgentDefaults.CODEX_MAC_COMMAND, settings.agents[0].macCommand)
    }

    fun testKeepsAnEmptyAgentListEmpty() {
        val settings = CodexAppSettings()

        settings.loadState(CodexAppSettings.SettingsState())

        assertTrue(settings.agents.isEmpty())
    }

    fun testRestoreDefaultsRestoresTheBuiltInAgents() {
        CodexAppSettings.getInstance().agents = listOf(agent("Custom", "w", "m"))

        val component = CodexAppSettingsConfigurable().createComponent()
        component.textAreas().forEach { it.text = "custom command" }

        component.buttons().single { it.text == "Restore Defaults" }.doClick()

        assertEquals(listOf("Codex", "ZCode"), component.nameFields().map { it.text })
        assertEquals(
            setOf(
                AgentDefaults.CODEX_WINDOWS_COMMAND,
                AgentDefaults.CODEX_MAC_COMMAND,
                AgentDefaults.ZCODE_WINDOWS_COMMAND,
                AgentDefaults.ZCODE_MAC_COMMAND,
            ),
            component.textAreas().map { it.text }.toSet(),
        )
    }

    fun testAddAndRemoveAgentCards() {
        CodexAppSettings.getInstance().agents = AgentDefaults.defaultAgents()

        val component = CodexAppSettingsConfigurable().createComponent()
        assertEquals(4, component.textAreas().size)

        component.buttons().single { it.text == "Add Agent" }.doClick()
        assertEquals(6, component.textAreas().size)
        assertEquals(3, component.nameFields().size)

        component.buttons().first { it.text == "Remove" }.doClick()
        assertEquals(4, component.textAreas().size)
    }

    fun testApplyAndResetKeepEveryAgentInSync() {
        CodexAppSettings.getInstance().agents = AgentDefaults.defaultAgents()

        val configurable = CodexAppSettingsConfigurable()
        val component = configurable.createComponent()
        assertFalse(configurable.isModified)

        component.nameFields()[1].text = "Zed Code"
        component.textAreas()[2].text = "custom windows command"
        component.textAreas()[3].text = "custom mac command"
        assertTrue(configurable.isModified)

        configurable.apply()

        val agents = CodexAppSettings.getInstance().agents
        assertEquals("Zed Code", agents[1].name)
        assertEquals("custom windows command", agents[1].windowsCommand)
        assertEquals("custom mac command", agents[1].macCommand)
        assertEquals(AgentDefaults.CODEX_WINDOWS_COMMAND, agents[0].windowsCommand)

        component.textAreas()[0].text = "stale command"
        configurable.reset()

        assertEquals(AgentDefaults.CODEX_WINDOWS_COMMAND, component.textAreas()[0].text)
        assertFalse(configurable.isModified)
    }

    private fun agent(name: String, windowsCommand: String, macCommand: String) =
        CodexAppSettings.AgentState(name, windowsCommand, macCommand)

    private fun Component.nameFields(): List<JTextField> =
        descendants().filterIsInstance<JTextField>()

    private fun Component.textAreas(): List<JTextArea> =
        descendants().filterIsInstance<JTextArea>()

    private fun Component.buttons(): List<JButton> =
        descendants().filterIsInstance<JButton>()

    private fun Component.descendants(): List<Component> = buildList {
        add(this@descendants)
        if (this@descendants is Container) {
            this@descendants.components.forEach { addAll(it.descendants()) }
        }
    }
}
