# Agent Refs

IntelliJ IDEA plugin that copies selected code as a precise reference such as `project\src\Example.java#L10-12` to the clipboard, ready to paste into a coding agent. It also opens the current project in the agent app you use (Codex, ZCode, …).

## Usage

1. Install the ZIP from `build/distributions` through **Settings | Plugins | Install Plugin from Disk**.
2. Select code with the mouse and click the copy icon in the floating code toolbar. The plugin copies a reference such as `project\src\Example.java#L10-12` (Windows) or `project/src/Example.java#L10-12` (macOS) to the clipboard. Double-clicking a method or variable behaves exactly like a manual selection.
3. Right-click in the editor and select **Copy as Code Reference**, which sits right above **Copy / Paste Special**, or press **Ctrl+Shift+C**. If code is selected, the copied reference includes the selected line range; otherwise it points at the file without line numbers. A notification confirms the copy and disappears on its own.
4. Click the **Open Agent** dropdown on the right side of the main toolbar and pick the agent that should open the current project. Codex and ZCode are configured out of the box.
5. Add, rename or remove agents under **Settings | Tools | Agent Refs**. Every agent has a name plus a Windows PowerShell and a macOS shell command; **Restore Defaults** restores the Codex and ZCode cards and discards your own agents.

The built-in agents open:

```text
codex://threads/new?path=<encoded project path>
zcode://workspace/open?path=<encoded project path>
```

The plugin resolves the current IDEA project root to its real filesystem path before launching the configured command. The path is injected instead of being pasted into the command text:

- Windows executes the configured command with `powershell.exe` and exposes the path as `$Path`.
- macOS executes the configured command with `/bin/zsh -lc`; the environment variable `CODEX_IDEA_PROJECT_PATH` contains the absolute real project path.
- Other operating systems are currently disabled.

The plugin explicitly supports dynamic loading, so installation and updates do not require restarting IntelliJ IDEA.

## Build

```powershell
.\gradlew.bat clean test buildPlugin
```

## 社区

本项目认可并链接 LINUX DO 社区。

LinuxDo 的链接是：[https://linux.do/](https://linux.do/)
