package com.wajnc.codexapp

import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class OpenAgentGroupTest : BasePlatformTestCase() {
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

    fun testOffersOneActionPerConfiguredAgent() {
        CodexAppSettings.getInstance().agents = listOf(agent("Codex"), agent("ZCode"))

        val children = OpenAgentGroup().getChildren(null)

        assertEquals(listOf("Codex", "ZCode"), children.map { it.templatePresentation.text })
    }

    fun testSkipsAgentsWithoutAName() {
        CodexAppSettings.getInstance().agents = listOf(agent("Codex"), agent(""))

        val children = OpenAgentGroup().getChildren(null)

        assertEquals(listOf("Codex"), children.map { it.templatePresentation.text })
    }

    fun testIsDisabledWithoutAgents() {
        CodexAppSettings.getInstance().agents = emptyList()
        val group = OpenAgentGroup()
        val event = eventFor(group)

        group.update(event)

        assertFalse(event.presentation.isEnabled)
    }

    fun testIsEnabledWithAgentsOnASupportedPlatform() {
        if (!AgentLauncher.isSupportedPlatform()) return
        CodexAppSettings.getInstance().agents = listOf(agent("Codex"))
        val group = OpenAgentGroup()
        val event = eventFor(group)

        group.update(event)

        assertTrue(event.presentation.isEnabled)
    }

    private fun agent(name: String) = CodexAppSettings.AgentState(name, "windows command", "mac command")

    private fun eventFor(action: AnAction): AnActionEvent {
        val context = SimpleDataContext.builder()
            .add(CommonDataKeys.PROJECT, project)
            .build()
        return AnActionEvent.createFromAnAction(action, null, ActionPlaces.UNKNOWN, context)
    }
}
