# Codex App Launcher

IntelliJ IDEA plugin that opens the current project in Codex App and appends selected code references to its current input.

## Usage

1. Install the ZIP from `build/distributions` through **Settings | Plugins | Install Plugin from Disk**.
2. Click the Codex icon in the right side of the main toolbar.
3. On Windows or macOS, select code with the mouse and click the Codex icon in the floating code toolbar. The plugin appends a reference such as `@project\src\Example.java#L10-12` (Windows) or `@project/src/Example.java#L10-12` (macOS) to the current Codex App input without sending it.
4. On Windows or macOS, right-click in the editor and select **Send to Codex App**. If code is selected, the selected line range is appended; otherwise, the current file reference is appended without line numbers.
5. Edit the Windows PowerShell or macOS shell command under **Settings | Tools | Codex App Launcher** when needed.
6. Use **Restore Defaults** to restore both built-in commands; click **Apply** or **OK** to save them.

The Windows default command opens:

```text
codex://threads/new?path=<encoded project path>
```

The plugin resolves the current IDEA project root to its real filesystem path before launching the platform command:

- Windows executes the configured command with `powershell.exe` and exposes the path as `$Path`.
- macOS executes `codex app "$CODEX_IDEA_PROJECT_PATH"` with `/bin/zsh`; the environment variable contains the absolute real project path.
- macOS uses `/usr/bin/osascript` and System Events to focus the Codex App input and paste the reference. Grant IntelliJ IDEA (or the launched `osascript`) permission under **System Settings | Privacy & Security | Accessibility** when prompted.
- Other operating systems are currently disabled.

The plugin explicitly supports dynamic loading, so installation and updates do not require restarting IntelliJ IDEA.

## Build

```powershell
.\gradlew.bat clean test buildPlugin
```

## 社区

本项目认可并链接 LINUX DO 社区。

LinuxDo 的链接是：[https://linux.do/](https://linux.do/)
