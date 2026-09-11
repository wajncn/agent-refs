package com.wajnc.codexapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.awt.Component
import java.awt.Container
import javax.swing.JButton
import javax.swing.JTextArea
import javax.swing.SwingUtilities

class CodexAppSettingsConfigurableTest {
    @Test
    fun `uses the plugin name as the settings display name`() {
        assertEquals("Codex App Launcher", CodexAppSettingsConfigurable().displayName)
    }

    @Test
    fun `migrates the legacy default mac command`() {
        val settings = CodexAppSettings()
        val state = CodexAppSettings.SettingsState().apply {
            macCommand = """
                codexapp() {
                    local path="${'$'}{1:-.}"
                    local resolved_path
                    resolved_path="${'$'}(cd "${'$'}path" && pwd -P)" || return 1
                    local encoded_path
                    encoded_path="${'$'}(/usr/bin/osascript -l JavaScript -e "function run(argv) { return encodeURIComponent(argv[0]).replace(/[!'()*]/g, function(c) { return '%' + c.charCodeAt(0).toString(16).toUpperCase(); }); }" "${'$'}resolved_path")" || return 1
                    /usr/bin/open "codex://threads/new?path=${'$'}encoded_path"
                }

                codexapp "${'$'}CODEX_IDEA_PROJECT_PATH"
            """.trimIndent()
        }

        settings.loadState(state)

        assertEquals(CodexAppSettings.DEFAULT_MAC_COMMAND, settings.macCommand)
    }

    @Test
    fun `restore defaults restores both built-in commands`() {
        SwingUtilities.invokeAndWait {
            val component = CodexAppSettingsConfigurable().createComponent()
            val textAreas = component.descendants().filterIsInstance<JTextArea>()
            val restoreButton = component.descendants().filterIsInstance<JButton>()
                .singleOrNull { it.text == "Restore Defaults" }

            assertEquals(2, textAreas.size)
            assertNotNull(restoreButton)

            textAreas.forEach { it.text = "custom command" }
            restoreButton!!.doClick()

            assertEquals(
                setOf(CodexAppSettings.DEFAULT_WINDOWS_COMMAND, CodexAppSettings.DEFAULT_MAC_COMMAND),
                textAreas.map { it.text }.toSet(),
            )
        }
    }

    private fun Component.descendants(): List<Component> = buildList {
        add(this@descendants)
        if (this@descendants is Container) {
            this@descendants.components.forEach { addAll(it.descendants()) }
        }
    }
}
