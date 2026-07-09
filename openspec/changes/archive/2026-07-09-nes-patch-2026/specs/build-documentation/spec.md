## ADDED Requirements

### Requirement: 构建快捷方式

本项目 MUST 提供 `Makefile`，封装常用构建、测试、安装、发布与安全检查命令，基于 `mvnw` + JDK 17。

#### Scenario: 标准目标可用
- **WHEN** 查看 `Makefile`
- **THEN** 提供 `build` / `test` / `install` / `deploy` / `security` 目标
- **AND** 命令使用项目自带 `mvnw` 与 `~/.m2/settings.xml`

### Requirement: 交付文档完整性

本项目 MUST 提供用户手册与快速入门文档，覆盖 fork 定位、GAV 变化、与官方差异。

#### Scenario: 用户手册存在
- **WHEN** 查看 `doc/USER_MANUAL.md`
- **THEN** 说明本 fork 定位、GAV 坐标变化、CVE-2026-41719 官方已修复、与官方差异、包名与模块名保持不变

#### Scenario: 快速入门存在
- **WHEN** 查看 `doc/QUICK_START.md`
- **THEN** 给出新 GAV 依赖坐标、Java 17 要求、fork 依赖前置条件、默认 settings 使用方式

#### Scenario: GAV 映射与需求清单存在
- **WHEN** 查看 `doc/GAV_MAPPING.md` 与 `doc/REQUIREMENTS.md`
- **THEN** 前者含原→新坐标对照（含 parent 与三条 fork 依赖），后者含需求与版本清单
