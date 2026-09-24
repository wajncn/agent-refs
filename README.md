# Codex App Launcher

IntelliJ IDEA plugin that opens the current project in Codex App and copies code references such as `project\src\Example.java#L10-12` to the clipboard.

## Usage

1. Install the ZIP from `build/distributions` through **Settings | Plugins | Install Plugin from Disk**.
2. Click the Codex icon in the right side of the main toolbar.
3. Select code with the mouse and click the copy icon in the floating code toolbar. The plugin copies a reference such as `project\src\Example.java#L10-12` (Windows) or `project/src/Example.java#L10-12` (macOS) to the clipboard.
4. Right-click in the editor and select **Copy as Code Reference**, which sits right above **Copy / Paste Special**, or press **Ctrl+Shift+C**. If code is selected, the copied reference includes the selected line range; otherwise it points at the file without line numbers. A notification confirms the copy and disappears on its own.
5. Edit the Windows PowerShell or macOS shell command under **Settings | Tools | Codex App Launcher** when needed.
6. Use **Restore Defaults** to restore both built-in commands; click **Apply** or **OK** to save them.

The Windows default command opens:

```text
codex://threads/new?path=<encoded project path>
```

The plugin resolves the current IDEA project root to its real filesystem path before launching the platform command:

- Windows executes the configured command with `powershell.exe` and exposes the path as `$Path`.
- macOS executes `codex app "$CODEX_IDEA_PROJECT_PATH"` with `/bin/zsh`; the environment variable contains the absolute real project path.
- Other operating systems are currently disabled.

The plugin explicitly supports dynamic loading, so installation and updates do not require restarting IntelliJ IDEA.

## Build

```powershell
.\gradlew.bat clean test buildPlugin
```

## 社区

本项目认可并链接 LINUX DO 社区。

LinuxDo 的链接是：[https://linux.do/](https://linux.do/)
