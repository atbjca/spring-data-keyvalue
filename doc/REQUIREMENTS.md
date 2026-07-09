# 需求与版本清单 (Requirements)

## 1. 项目版本

| 项 | 值 |
|----|-----|
| 基线版本 | Spring Data KeyValue 3.5.13（官方正式版） |
| 制品版本 | 3.5.13-nes.patch.1-SNAPSHOT |
| Parent | `org.springframework.data.build:spring-data-parent:3.5.13` |
| 构建工具 | Maven（`mvnw` 3.9.16） |
| 编译目标 | Java 17（3.5.x 基线要求） |
| Git 分支 | `3.5.x-bjca-patch` |

## 2. 改造需求清单

| 编号 | 需求 | 状态 |
|:---:|------|:---:|
| R1 | 验证本体 CVE-2026-41719（SpelPropertyComparator SpEL 排序注入）官方修复在本基线生效 | ✅ 完成 |
| R2 | 新增 SpEL 注入安全回归测试（不改被测类） | ✅ 完成 |
| R3 | GAV 去特征化（groupId/artifactId/version） | ✅ 完成 |
| R4 | Parent 版本锁定 3.5.13（私服/本地可解析） | ✅ 完成 |
| R5 | 运行时依赖坐标联动 fork（commons / context / tx，官方运行时坐标清零） | ✅ 完成 |
| R6 | 私服发布配置（distributionManagement）+ 移除外网仓库声明 | ✅ 完成 |
| R7 | CVE 文档体系（VULNERABILITY_REPORT + 每 CVE 独立文档） | ✅ 完成 |
| R8 | 交付文档（用户手册、快速入门、GAV 映射、需求清单） | ✅ 完成 |
| R9 | Makefile 构建快捷命令（含 security 回归） | ✅ 完成 |

## 3. 约束

| 约束 | 说明 |
|------|------|
| 零业务代码改动 | 本体 CVE 官方已修复，本 fork 不改任何业务源码（仅新增测试） |
| 包名不变 | `org.springframework.data.keyvalue.*` 保持，下游 `import` 无需修改 |
| JPMS 模块名不变 | `spring.data.keyvalue` |
| API 兼容 | 与官方 3.5.13 完全一致 |
| 私服隔离 | 依赖解析与发布走内网 Nexus，不直连外网 |

## 4. 依赖概览

| 依赖 | 坐标 | 作用域 | 说明 |
|------|-----|:---:|------|
| spring-data-commons | `cn.bjca.footstone.bpring.data:bjca-footstone-bpring-data-commons:3.5.13-nes.patch.1-SNAPSHOT` | compile | fork 坐标 |
| spring-context | `cn.bjca.footstone.bpring:bjca-footstone-bpring-context:6.2.19-nes.patch.1-SNAPSHOT` | compile | fork 坐标（显式版本，脱离 parent BOM 托管） |
| spring-tx | `cn.bjca.footstone.bpring:bjca-footstone-bpring-tx:6.2.19-nes.patch.1-SNAPSHOT` | compile | fork 坐标 |
| querydsl-collections | `com.querydsl:querydsl-collections:${querydsl}` | compile (optional) | 第三方，保持官方坐标 |

> SpEL（`spring-expression`）经 context/tx 的 fork 传递依赖引入，CVE-2026-41719 修复所用 `SimpleEvaluationContext.forReadOnlyDataBinding()` 即由其提供。传递依赖 CVE 盘点不在本项目范围（已由 spring-framework / spring-data-commons / spring-boot NES GAV 处理覆盖）。

## 5. 测试覆盖（回归验证）

| CVE | 测试 | 结果 |
|------|------|:---:|
| CVE-2026-41719 | `SpelPropertyComparatorSecurityUnitTests`（新增，2 个） | ✅ |
| CVE-2026-41719 | `SpelPropertyComparatorUnitTests`（现有，11 个） | ✅ |
| 全量回归 | `clean test` 374 个（Failures 0 / Errors 0 / Skipped 13） | ✅ |
