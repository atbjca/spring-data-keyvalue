## Why

`spring-data-keyvalue` 3.5.x 是官方仍在维护的活跃版本线。BJCA 维护分支 `3.5.x-bjca-patch` 需要与已完成的 `spring-data-commons-3.5`、`spring-data-keyvalue-2.7`、`spring-data-commons-2.7`、`spring-kafka-2.9`、`spring-framework-5.3`、`spring-boot-2.7` 等 NES（Never-Ending Support）fork 对齐，建立统一的**安全维护 + 私服隔离 + 文档化**流程。

与 **2.7 fork（EOL、漏洞宿主本体、需自己 backport）本质不同**：本项目本体 CVE（CVE-2026-41719 SpEL 排序注入）**官方已在 3.5.12 修复**，当前 `3.5.14-SNAPSHOT` 源码中的 `SpelPropertyComparator` 已是修复版——它正是 `spring-data-keyvalue-2.7` fork backport 时逐行比对的「标准答案」。因此本次**不做任何业务代码修改**（避免画蛇添足），只需：验证官方修复生效、去特征化 GAV、配置私服、补齐文档。具体解决三个问题：

1. **GAV 去特征化**：重命名 Maven 坐标，规避 SCA 工具按官方 GAV 特征误报 CVE。（**必做**）
2. **私服隔离**：统一依赖下载与构件发布渠道，通过内网 Nexus 私服隔离外网。
3. **文档化**：建立完整的漏洞报告体系与交付文档，如实记录本体 CVE 由官方 upstream 修复的事实与验证证据。

## What Changes

- **本体 CVE 验证（零源码改动）**：CVE-2026-41719 官方已在 3.5.12 修复，本次仅核对/运行回归测试确认防护生效，状态记 `✅已修复`，修复方标注为**官方 upstream**（非本 fork backport）。
  - CVE-2026-41719：`SpelPropertyComparator` 已将排序表达式简化为只读属性导航 `#arg1?.<path>`（`SpelPropertyComparator.java:120-121`），比较逻辑回归 Java 常量比较器 `NULLS_FIRST`/`NULLS_LAST`（`:36-37`），求值改用 `SimpleEvaluationContext.forReadOnlyDataBinding()`（`:138`）——注入无法落地。
- **GAV 去特征化**：
  - GroupId: `org.springframework.data` → `cn.bjca.footstone.bpring.data`
  - ArtifactId: `spring-data-keyvalue` → `bjca-footstone-bpring-data-keyvalue`
  - Version: `3.5.14-SNAPSHOT` → `3.5.13-nes.patch.1-SNAPSHOT`
  - Parent: 方案 A 保留 `org.springframework.data.build:spring-data-parent`，版本 `3.5.14-SNAPSHOT` → `3.5.13`（已发布正式版，本地 `.m2` 已确认存在 `spring-data-parent-3.5.13.pom`）
  - `java-module-name`（`spring.data.keyvalue`）保持不变。
- **依赖坐标联动**（三条 fork 制品均已在 `~/.m2` 就绪，无前置阻塞）：
  - spring-data-commons → `cn.bjca.footstone.bpring.data:bjca-footstone-bpring-data-commons:3.5.13-nes.patch.1-SNAPSHOT`
  - spring-context → `cn.bjca.footstone.bpring:bjca-footstone-bpring-context:6.2.19-nes.patch.1-SNAPSHOT`
  - spring-tx → `cn.bjca.footstone.bpring:bjca-footstone-bpring-tx:6.2.19-nes.patch.1-SNAPSHOT`
  - querydsl-collections：保持上游官方坐标（第三方、`optional`、内存查询辅助，不去特征化）
- **私服配置**：`pom.xml` 增加 `<distributionManagement>`（`${nexusReleaseUrl}` / `${nexusSnapshotUrl}`），凭证/属性/mirror 由 `~/.m2/settings.xml` 的 `bjca` profile 提供。
- **CVE 文档体系**（参考 `spring-boot-2.7/doc/`）：
  - `doc/VULNERABILITY_REPORT.md`：漏洞状态总览，6 态归一化（✅已修复 / ⬜免疫 / ❌不适用 / 🔧修复中 / ⚠️已缓解 / ⏸️暂缓）。
  - `doc/CVE/CVE-xxxx.md`：每个 CVE 一个独立文档。
- **交付文档**：`doc/USER_MANUAL.md`、`doc/QUICK_START.md`、`doc/REQUIREMENTS.md`、`doc/GAV_MAPPING.md`。
- **构建快捷方式**：`Makefile`（build / test / install / deploy / security），基于 Maven（`mvnw`）+ Java 17。

## Capabilities

### New Capabilities

- `cve-documentation`：漏洞报告与 CVE 独立文档体系，含「验证官方修复生效」的回归测试场景。
- `gav-renaming`：GAV 去特征化重命名逻辑（含依赖坐标联动、parent 方案 A）。
- `nexus-config`：Nexus 私服配置与发布管理。
- `build-documentation`：构建、发布与交付文档。

> 说明：不设 `cve-assessment`（本项目运行时依赖极薄，改以官方公告人工核实）与 `cve-remediation`（本体官方已修复，零源码改动）——这是与 2.7 fork 6 能力方案的关键差异，对齐 `spring-data-commons-3.5` 的 4 能力方案。

### Modified Capabilities

- （无——本项目此前无 openspec 受管能力）

## Impact

- **源码**：**无业务代码改动**（本体 CVE 官方基线已修复）。仅可能补充一条 SpEL 注入防护的安全回归测试以固化验证（可选，尊重「零源码改动」原则不改被测类）。
- **测试**：核对现有 `SpelPropertyComparatorUnitTests` 全绿；如缺注入防护断言则补充边界用例。
- **构建配置**：`pom.xml`（groupId / artifactId / version / parent / dependencies / distributionManagement）、项目内 `settings.xml`。
- **新增文件**：`Makefile`、`settings.xml`、`doc/VULNERABILITY_REPORT.md`、`doc/CVE/*.md`、`doc/USER_MANUAL.md`、`doc/QUICK_START.md`、`doc/REQUIREMENTS.md`、`doc/GAV_MAPPING.md`。
- **下游影响**：依赖方需更新 GAV 坐标（Java `import` 与 JPMS `requires` 无需改动，包名 `org.springframework.data.keyvalue.*` 与模块名 `spring.data.keyvalue` 均不变）。
- **风险点**：
  - Parent 采用方案 A（保留原坐标，锁定已发布正式版 `3.5.13`）；本地 `.m2` 已确认存在 `spring-data-parent-3.5.13.pom`，阶段 0 再干跑验证私服解析。
  - 依赖坐标联动：commons/context/tx 三条 fork 制品当前均在 `~/.m2` 可解析（commons `3.5.13-nes.patch.1`、context/tx `6.2.19-nes.patch.1`），**无前置构建阻塞**；缺失时应显式失败不回退官方坐标。
  - 本次零源码改动，风险主要在构建坐标与发布链路；通过 `clean install` / `clean deploy` 干跑验证。
