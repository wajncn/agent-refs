<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Agent Refs Changelog

## [Unreleased]

- 插件更名为 Agent Refs：主工具栏改为 **Open Agent** 下拉菜单，用当前项目打开任意配置的 Agent 应用，默认内置 Codex 与 ZCode。
- 设置页标题与通知分组同步改名为 Agent Refs。
- 设置页改为 Agent 卡片列表，可增删 Agent 并分别配置 Windows PowerShell 与 macOS 命令；旧的单 Agent 命令会在升级时自动迁移为 Codex 卡片。
- 删除 Send to Codex App、Codex CLI queue 与 IDE bridge（`/ide`）等「把引用发送到 Agent」的实现及配套本机 UI 自动化脚本，插件只保留启动器与复制引用。
- 选中代码的浮动工具栏动作与编辑器右键菜单动作改为把代码引用复制到剪贴板，不再发送到 Codex App。
- 编辑器右键菜单中的 Copy as Code Reference 位于 Copy / Paste Special 上方，并改用复制图标。
- 复制完成后关闭浮动工具栏，并在右下角弹出会自动消失的提示。
- 新增 Ctrl+Shift+C 快捷键。
- 修复编辑器内 Ctrl+Shift+C 被 IDEA 自带的 Copy Path/Reference 抢占的问题：光标在编辑器时优先复制代码引用，选中代码带行号范围，并弹出右下角提示。

## [0.0.3] - 2026-09-03

- 将 Send to Codex App 放置在 Paste 菜单项上方。
- Windows 发送复用后台 PowerShell 进程，减少发送延迟。
