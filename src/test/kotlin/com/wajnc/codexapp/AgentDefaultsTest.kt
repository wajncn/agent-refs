package com.wajnc.codexapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentDefaultsTest {
    @Test
    fun `ships codex and zcode with their deep links`() {
        val agents = AgentDefaults.defaultAgents()

        assertEquals(listOf("Codex", "ZCode"), agents.map { it.name })
        assertTrue(agents[0].windowsCommand.contains("codex://threads/new?path="))
        assertTrue(agents[1].windowsCommand.contains("zcode://workspace/open?path="))
        assertTrue(agents[0].macCommand.contains("${'$'}CODEX_IDEA_PROJECT_PATH"))
        assertTrue(agents[1].macCommand.contains("${'$'}CODEX_IDEA_PROJECT_PATH"))
    }

    @Test
    fun `windows commands read the resolved project path from the Path variable`() {
        AgentDefaults.defaultAgents().forEach { agent ->
            assertTrue(agent.windowsCommand.contains("${'$'}Path"))
            assertFalse(agent.windowsCommand.contains("CODEX_IDEA_PROJECT_PATH"))
        }
    }

    @Test
    fun `every call returns independent agents`() {
        AgentDefaults.defaultAgents()[0].name = "Changed"

        assertEquals("Codex", AgentDefaults.defaultAgents()[0].name)
    }

    @Test
    fun `the legacy mac command differs from the current default`() {
        assertNotEquals(AgentDefaults.CODEX_MAC_COMMAND, AgentDefaults.LEGACY_CODEX_MAC_COMMAND)
    }
}
