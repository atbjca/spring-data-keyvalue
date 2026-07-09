# build-documentation Specification

## Purpose
TBD - created by archiving change nes-patch-2026. Update Purpose after archive.
## Requirements
### Requirement: 交付文档完整性

MUST 提供面向使用者与运维的交付文档，至少包含用户手册与快速入门。

#### Scenario: 快速入门

- **WHEN** 查看 `doc/QUICK_START.md`
- **THEN** MUST 包含最小化的依赖引入、构建、测试步骤，使用新 GAV 坐标（`cn.bjca.footstone.bpring.data:bjca-footstone-bpring-data-keyvalue`）
- **AND** MUST 说明构建前需先行部署 commons fork 的前置条件

#### Scenario: 用户手册

- **WHEN** 查看 `doc/USER_MANUAL.md`
- **THEN** MUST 说明本 fork 的定位、GAV 变化、安全修复内容（`CVE-2026-41719` backport）、与官方版本的差异
- **AND** MUST 说明 Java 包名与 JPMS 模块名（`spring.data.keyvalue`）保持不变，下游 `import` / `requires` 无需修改

#### Scenario: 需求与版本清单

- **WHEN** 查看 `doc/REQUIREMENTS.md`
- **THEN** MUST 列出目标版本、依赖版本清单、CVE 修复范围

#### Scenario: GAV 映射表

- **WHEN** 查看 `doc/GAV_MAPPING.md`
- **THEN** MUST 提供原始 GAV 与 NES GAV 的对照表（含 parent、三条兄弟 fork 依赖）

### Requirement: 构建快捷命令

MUST 提供 `Makefile` 封装常用 Maven 构建命令，基于 Maven + Java 8（`8.0.482-kona`）。

#### Scenario: Makefile 目标

- **WHEN** 查看 `Makefile`
- **THEN** MUST 至少包含 `build`、`test`、`install`、`deploy` 目标
- **AND** 各目标 MUST 使用项目 `settings.xml` 与 `mvnw`

