# Agent Refs

IntelliJ IDEA plugin that copies selected code as a precise reference such as `project\src\Example.java#L10-12` to the clipboard, ready to paste into a coding agent.

## Usage

1. Install the ZIP from `build/distributions` through **Settings | Plugins | Install Plugin from Disk**.
2. Select code with the mouse and click the copy icon in the floating code toolbar. The plugin copies a reference such as `project\src\Example.java#L10-12` (Windows) or `project/src/Example.java#L10-12` (macOS) to the clipboard. Double-clicking a method or variable behaves exactly like a manual selection.
3. Right-click in the editor and select **Copy as Code Reference**, which sits right above **Copy / Paste Special**, or press **Ctrl+Shift+C**. If code is selected, the copied reference includes the selected line range; otherwise it points at the file without line numbers. A notification confirms the copy and disappears on its own.

A reference is the project folder name, the project-relative path, and the line range:

```text
my-project\src\main\java\com\example\Example.java#L10-12
```

The plugin has no settings and copies references on every platform, including Linux.

The plugin explicitly supports dynamic loading, so installation and updates do not require restarting IntelliJ IDEA.

## Build

```powershell
.\gradlew.bat clean test buildPlugin
```

## 社区

本项目认可并链接 LINUX DO 社区。

LinuxDo 的链接是：[https://linux.do/](https://linux.do/)
