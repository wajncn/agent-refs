package com.wajnc.codexapp

import java.nio.file.Path
import java.util.Base64

internal object CodexCommandRunner {
    const val PROJECT_PATH_ENV = "CODEX_IDEA_PROJECT_PATH"
    const val SELECTION_REFERENCE_ENV = "CODEX_IDEA_SELECTION_REFERENCE"

    internal fun windowsAppendScript(openCommand: String) = """
        ${'$'}ProgressPreference = "SilentlyContinue"
        Add-Type -AssemblyName UIAutomationClient

        function Get-CodexAppProcess {
            Get-Process -Name "ChatGPT", "Codex" -ErrorAction SilentlyContinue |
                Where-Object { ${'$'}_.MainWindowHandle -ne 0 } |
                Select-Object -First 1
        }

        function Get-CodexProjectButton {
            param(
                [System.Windows.Automation.AutomationElement] ${'$'}Root,
                [string] ${'$'}ProjectName
            )

            ${'$'}buttonCondition = New-Object System.Windows.Automation.PropertyCondition(
                [System.Windows.Automation.AutomationElement]::ControlTypeProperty,
                [System.Windows.Automation.ControlType]::Button
            )
            ${'$'}Root.FindAll(
                [System.Windows.Automation.TreeScope]::Descendants,
                ${'$'}buttonCondition
            ) | Where-Object {
                ${'$'}_.Current.ClassName -like "*token-button-composer*" -and
                ${'$'}_.Current.IsEnabled -and
                -not ${'$'}_.Current.IsOffscreen -and
                -not [string]::IsNullOrEmpty(${'$'}_.Current.Name) -and
                ${'$'}_.Current.Name.EndsWith(${'$'}ProjectName, [System.StringComparison]::OrdinalIgnoreCase)
            } | Select-Object -First 1
        }

        ${'$'}projectName = Split-Path -Leaf ${'$'}Path
        ${'$'}codexProcess = Get-CodexAppProcess
        ${'$'}projectButton = ${'$'}null
        if (${'$'}codexProcess) {
            ${'$'}root = [System.Windows.Automation.AutomationElement]::FromHandle(${'$'}codexProcess.MainWindowHandle)
            ${'$'}projectButton = Get-CodexProjectButton -Root ${'$'}root -ProjectName ${'$'}projectName
        }
        if (-not ${'$'}projectButton) {
            $openCommand
            for (${'$'}attempt = 0; -not ${'$'}projectButton -and ${'$'}attempt -lt 150; ${'$'}attempt++) {
                Start-Sleep -Milliseconds 100
                ${'$'}codexProcess = Get-CodexAppProcess
                if (${'$'}codexProcess) {
                    ${'$'}root = [System.Windows.Automation.AutomationElement]::FromHandle(${'$'}codexProcess.MainWindowHandle)
                    ${'$'}projectButton = Get-CodexProjectButton -Root ${'$'}root -ProjectName ${'$'}projectName
                }
            }
        }
        if (-not ${'$'}codexProcess) {
            throw "Codex App did not open within 15 seconds."
        }
        if (-not ${'$'}projectButton) {
            throw "Codex App did not open project '${'$'}projectName' within 15 seconds."
        }

        ${'$'}shell = New-Object -ComObject WScript.Shell
        if (-not ${'$'}shell.AppActivate(${'$'}codexProcess.Id)) {
            throw "Cannot activate Codex App."
        }

        ${'$'}condition = New-Object System.Windows.Automation.PropertyCondition(
            [System.Windows.Automation.AutomationElement]::ControlTypeProperty,
            [System.Windows.Automation.ControlType]::Edit
        )
        ${'$'}composer = ${'$'}null
        for (${'$'}attempt = 0; -not ${'$'}composer -and ${'$'}attempt -lt 150; ${'$'}attempt++) {
            ${'$'}root = [System.Windows.Automation.AutomationElement]::FromHandle(${'$'}codexProcess.MainWindowHandle)
            ${'$'}composer = ${'$'}root.FindAll(
                [System.Windows.Automation.TreeScope]::Descendants,
                ${'$'}condition
            ) | Where-Object {
                ${'$'}_.Current.ClassName -like "ProseMirror*" -and
                ${'$'}_.Current.IsEnabled -and
                -not ${'$'}_.Current.IsOffscreen
            } | Select-Object -First 1
            if (-not ${'$'}composer) {
                Start-Sleep -Milliseconds 100
            }
        }
        if (-not ${'$'}composer) {
            throw "Cannot find the Codex App input box. Open a chat and try again."
        }

        ${'$'}valuePattern = [System.Windows.Automation.ValuePattern] ${'$'}composer.GetCurrentPattern(
            [System.Windows.Automation.ValuePattern]::Pattern
        )
        ${'$'}descendants = ${'$'}composer.FindAll(
            [System.Windows.Automation.TreeScope]::Descendants,
            [System.Windows.Automation.Condition]::TrueCondition
        )
        ${'$'}value = ${'$'}valuePattern.Current.Value
        ${'$'}isEmpty =
            ${'$'}descendants.Count -eq 2 -and
            ${'$'}descendants.Item(0).Current.ClassName -eq "ProseMirror-trailingBreak" -and
            [string]::IsNullOrEmpty(${'$'}descendants.Item(1).Current.ClassName) -and
            ${'$'}composer.Current.Name -eq ${'$'}descendants.Item(1).Current.Name -and
            ${'$'}value -eq (${'$'}descendants.Item(0).Current.Name + ${'$'}descendants.Item(1).Current.Name)
        ${'$'}currentText = if (${'$'}isEmpty) { "" } else { ${'$'}value }
        ${'$'}separator = if (
            [string]::IsNullOrEmpty(${'$'}currentText) -or
            [char]::IsWhiteSpace(${'$'}currentText[${'$'}currentText.Length - 1])
        ) { "" } else { " " }

        ${'$'}valuePattern.SetValue(${'$'}currentText + ${'$'}separator + ${'$'}env:$SELECTION_REFERENCE_ENV)
        ${'$'}composer.SetFocus()
        Start-Sleep -Milliseconds 10
        ${'$'}shell.SendKeys("^{END}")
    """.trimIndent()

    private fun macAppendScript(openCommand: String) = """
        (${openCommand}) >/dev/null 2>&1 &
        /usr/bin/osascript - "${'$'}$PROJECT_PATH_ENV" "${'$'}$SELECTION_REFERENCE_ENV" <<'APPLESCRIPT'
        on run argv
            set projectPath to item 1 of argv
            set referenceText to item 2 of argv

            tell application "System Events"
                set codexProcess to missing value
                repeat 60 times
                    repeat with processName in {"Codex", "ChatGPT"}
                        set candidateName to contents of processName
                        if exists process candidateName then
                            set codexProcess to process candidateName
                            exit repeat
                        end if
                    end repeat
                    if codexProcess is not missing value then exit repeat
                    delay 0.25
                end repeat

                if codexProcess is missing value then
                    error "Codex App did not open within 15 seconds."
                end if

                set frontmost of codexProcess to true
                set composer to missing value
                repeat 60 times
                    if (count of windows of codexProcess) > 0 then
                        tell window 1 of codexProcess
                            set allElements to entire contents
                            repeat with elementRef in allElements
                                try
                                    if role of elementRef is "AXTextArea" or role of elementRef is "AXTextField" or role of elementRef is "AXComboBox" then
                                        set composer to contents of elementRef
                                        exit repeat
                                    end if
                                end try
                            end repeat

                            if composer is missing value then
                                repeat with elementRef in allElements
                                    try
                                        set elementValue to value of elementRef as text
                                    on error
                                        set elementValue to ""
                                    end try
                                    set elementName to ""
                                    try
                                        set elementName to description of elementRef as text
                                    end try
                                    if elementValue contains "随心输入" or elementName contains "随心输入" or elementValue contains "Ask anything" or elementName contains "Ask anything" then
                                        set composer to contents of elementRef
                                        exit repeat
                                    end if
                                end repeat
                            end if
                        end tell
                    end if
                    if composer is not missing value then exit repeat
                    delay 0.25
                end repeat

                if composer is not missing value then
                    try
                        perform action "AXPress" of composer
                    on error
                        try
                            click composer
                        end try
                    end try
                    delay 0.2
                    key code 125 using {command down}

                    set currentValue to ""
                    try
                        set currentValue to value of composer as text
                    end try
                    set isPlaceholder to currentValue is "随心输入" or currentValue is (linefeed & "随心输入")
                    if currentValue is not "" and not isPlaceholder then
                        set lastCharacter to last character of currentValue
                        if lastCharacter is not " " and lastCharacter is not return and lastCharacter is not linefeed then
                            keystroke " "
                        end if
                    end if
                else
                    tell window 1 of codexProcess to perform action "AXRaise"
                    delay 0.2
                end if

                set the clipboard to referenceText
                keystroke "v" using {command down}
            end tell
        end run
        APPLESCRIPT
    """.trimIndent()

    fun resolveProjectPath(projectPath: String): String = Path.of(projectPath).toRealPath().toString()

    fun windowsScript(command: String): String = "${'$'}Path = ${'$'}env:$PROJECT_PATH_ENV\n$command"

    fun windowsProcessBuilder(command: String, projectPath: String): ProcessBuilder {
        return windowsPowerShellProcessBuilder(windowsScript(command)).apply {
            environment()[PROJECT_PATH_ENV] = projectPath
        }
    }

    fun windowsAppendProcessBuilder(reference: String, openCommand: String, projectPath: String): ProcessBuilder =
        windowsPowerShellProcessBuilder(windowsScript(windowsAppendScript(openCommand))).apply {
            environment()[SELECTION_REFERENCE_ENV] = reference
            environment()[PROJECT_PATH_ENV] = projectPath
        }

    fun windowsPersistentAppendScript(reference: String, openCommand: String, projectPath: String): String {
        val encodedReference = Base64.getEncoder().encodeToString(reference.toByteArray(Charsets.UTF_8))
        val encodedProjectPath = Base64.getEncoder().encodeToString(projectPath.toByteArray(Charsets.UTF_8))
        return """
            ${'$'}env:$PROJECT_PATH_ENV = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String('$encodedProjectPath'))
            ${'$'}env:$SELECTION_REFERENCE_ENV = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String('$encodedReference'))
            ${'$'}Path = ${'$'}env:$PROJECT_PATH_ENV
            ${windowsAppendScript(openCommand)}
        """.trimIndent()
    }

    fun macAppendProcessBuilder(reference: String, openCommand: String, projectPath: String): ProcessBuilder =
        ProcessBuilder("/bin/zsh", "-lc", macAppendScript(openCommand)).apply {
            environment()[SELECTION_REFERENCE_ENV] = reference
            environment()[PROJECT_PATH_ENV] = projectPath
        }

    fun macProcessBuilder(command: String, projectPath: String): ProcessBuilder =
        ProcessBuilder("/bin/zsh", "-lc", command).apply {
            environment()[PROJECT_PATH_ENV] = projectPath
        }

    private fun windowsPowerShellProcessBuilder(script: String): ProcessBuilder {
        val encodedCommand = Base64.getEncoder()
            .encodeToString(script.toByteArray(Charsets.UTF_16LE))

        return ProcessBuilder(
            "powershell.exe",
            "-NoProfile",
            "-NoLogo",
            "-NonInteractive",
            "-WindowStyle",
            "Hidden",
            "-OutputFormat",
            "Text",
            "-EncodedCommand",
            encodedCommand,
        )
    }
}
