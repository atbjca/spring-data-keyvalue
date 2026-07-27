# 需求与版本清单 — Spring Data KeyValue NES Fork

## 一、需求背景

Spring Data KeyValue 官方 `2.7.x` 版本线已 EOL，不再提供开源安全补丁。企业内部需在此版本线上：

1. 修复本体安全漏洞（CVE-2026-41719 SpEL 排序注入）；
2. 去特征化 Maven 坐标，规避 SCA 工具对 EOL 版本线的 CVE 误报；
3. 通过内网私服（Nexus）分发；
4. 保持 API / 包名 / 模块名不变，下游零改动升级。

## 二、能力清单（对应 openspec change `nes-patch-2026`）

| 能力 | 说明 | 状态 |
|------|------|:---:|
| cve-remediation | 本体 CVE-2026-41719 backport 修复（核心价值） | ✅ |
| cve-assessment | 全量依赖攻击面盘点与 CVE 适用性 6 态归一化 | ✅ |
| cve-documentation | `VULNERABILITY_REPORT.md` + 独立 CVE 文档 | ✅ |
| gav-renaming | GAV 去特征化 + 依赖坐标联动 + parent 处理 | ✅ |
| nexus-config | 私服 `distributionManagement` 配置 | ✅ |
| build-documentation | 交付文档（用户手册/快速入门/GAV 映射/需求/Makefile） | ✅ |

## 三、版本清单

| 组件 | 版本 |
|------|------|
| 本项目 | `bjca-footstone-bpring-data-keyvalue:2.7.18-nes.patch.1` |
| Parent | `org.springframework.data.build:spring-data-parent:2.7.18` |
| spring-data-commons (fork) | `2.7.18-nes.patch.1` |
| Spring Framework (fork) | `5.3.39-nes.patch.1` |
| Java | 8（`8.0.482-kona`） |
| Maven | 3.6.3 |
| querydsl-collections | 5.0.0（第三方，保持上游） |
| joda-time | 上游（test 作用域） |

## 四、约束与非目标

**约束**：
- 必须 Java 8 兼容（不引入 JDK 9+ API）。
- 构建离线优先，走内网私服；缺 commons fork 制品时明确失败，不回退官方坐标。
- 遵循 TDD（测试先行）与变更前确认。

**非目标**：
- 不 fork `spring-data-build`（parent 用上游正式版 2.7.18）。
- 不引入官方 3.5 后期的 `PathSortAccessor` 等无关架构。
- 不去特征化第三方库（querydsl / joda-time / hateoas 等）。

## 五、验收标准

| 项 | 标准 | 结果 |
|----|------|:---:|
| 本体修复 | CVE-2026-41719 注入用例红→绿；合法排序回归全绿 | ✅ 285/285 |
| 去特征化 | 发布制品运行时无官方 Spring 坐标；包名/模块名不变 | ✅ |
| 依赖收敛 | classpath 无官方/fork 双份 Spring 制品 | ✅ |
| 文档 | 用户手册 + 快速入门 + CVE 报告齐备 | ✅ |
| 构建 | `clean install` BUILD SUCCESS | ✅ |
