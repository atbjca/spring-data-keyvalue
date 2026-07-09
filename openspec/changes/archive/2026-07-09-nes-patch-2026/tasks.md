# Tasks — nes-patch-2026 (spring-data-keyvalue 3.5)

> 遵循「变更前确认」原则。**核心特征：本体 CVE 官方已修复，本次零业务代码改动。**
> 图例：`[ ]` 未开始 `[~]` 进行中 `[x]` 完成
>
> **重要（AI Rules）**：进入实施阶段（阶段 1 起）前，须先向用户阐述变更影响并获得明确同意；破坏性/大范围变更前提示确认 Git 分支与备份。本 change 为非破坏性（源码零改动），但仍在实施前二次确认。

## 阶段 0：准备与确认

- [x] 0.1 确认 git 分支状态干净、已备份（当前分支 `3.5.x-bjca-patch`，工作区仅 `.claude/`、`openspec/` 未跟踪）
- [x] 0.2 用 sdkman 选定并验证 JDK 17（`17.0.17-amzn`），`./mvnw -v` 可运行（Maven 3.9.16）
- [x] 0.3 **前置验证**：本地 `.m2` 已确认存在 `spring-data-parent-3.5.13.pom`，parent 可解析
- [x] 0.4 **前置验证**：确认三条 fork 依赖制品在 `~/.m2` 就绪 —— commons `3.5.13-nes.patch.1-SNAPSHOT`、context/tx `6.2.19-nes.patch.1-SNAPSHOT`（均已核实存在，无前置构建阻塞）

> 决策已定（见 design 第 7 节）：4 能力；本体 CVE 官方已修复零源码改动；GAV `cn.bjca.footstone.bpring.data` / `bjca-footstone-bpring-data-keyvalue` / `3.5.13-nes.patch.1-SNAPSHOT`；parent 方案 A（锁 `3.5.13`）；commons 换 fork 坐标；context/tx 联动换 framework fork 坐标。

## 阶段 1：本体 CVE 验证（零源码改动）

### 1A CVE-2026-41719 — SpelPropertyComparator SpEL 排序注入
- [x] 1A.1 核对 `SpelPropertyComparator.java:120-121` `buildExpressionForPath()` 仅返回 `#arg1?.<path>`（无构造器/方法拼接）
- [x] 1A.2 核对 `:36-37` 类级常量 `NULLS_FIRST`/`NULLS_LAST` + `:131` `compare()` 用 Java Comparator 比较
- [x] 1A.3 核对 `:134-143` `getValue()` 用 `SimpleEvaluationContext.forReadOnlyDataBinding()`（非默认 `StandardEvaluationContext`）
- [x] 1A.4 【推荐·可选】在 `SpelPropertyComparatorUnitTests`（或新增 `SpelPropertyComparatorSecurityUnitTests`）增加注入防护断言：构造恶意 `path`，断言受限上下文下不发生方法调用/类型引用。**仅新增测试、不改被测类** → 新增 `SpelPropertyComparatorSecurityUnitTests`（恶意 `path=getClass().getName()` 断言抛 `EvaluationException`；合法路径回归）2/2 绿
- [x] 1A.5 核对现有 `SpelPropertyComparatorUnitTests` 全绿（合法路径、嵌套 `a.b.c`、asc/desc、nullsFirst/nullsLast，语义与官方一致）→ 11/11 绿

### 1B 整体验证
- [x] 1B.1 `./mvnw clean test` 全绿，无回归 → 374 run / 0 fail / 0 error / 13 skip，BUILD SUCCESS

## 阶段 2：GAV 去特征化

- [x] 2.1 `pom.xml`：groupId → `cn.bjca.footstone.bpring.data`
- [x] 2.2 `pom.xml`：artifactId → `bjca-footstone-bpring-data-keyvalue`
- [x] 2.3 `pom.xml`：version → `3.5.13-nes.patch.1-SNAPSHOT`
- [x] 2.4 parent 方案 A：`spring-data-parent` 版本由 `3.5.14-SNAPSHOT` 改为 `3.5.13`（已发布正式版），`<relativePath/>` 置空
- [x] 2.5 依赖坐标联动：
  - [x] 2.5.1 spring-data-commons → `cn.bjca.footstone.bpring.data:bjca-footstone-bpring-data-commons:3.5.13-nes.patch.1-SNAPSHOT`（`${springdata.commons}` 版本属性联动）
  - [x] 2.5.2 spring-context → `cn.bjca.footstone.bpring:bjca-footstone-bpring-context:6.2.19-nes.patch.1-SNAPSHOT`（显式版本 `${bpring.framework}`，脱离 parent BOM 托管）
  - [x] 2.5.3 spring-tx → `cn.bjca.footstone.bpring:bjca-footstone-bpring-tx:6.2.19-nes.patch.1-SNAPSHOT`
  - [x] 2.5.4 querydsl-collections 保持上游官方坐标
- [x] 2.6 确认 `java-module-name` 仍为 `spring.data.keyvalue`（不变）
- [x] 2.7 验证 `./mvnw clean install` 解析成功（project.id = `cn.bjca.footstone.bpring.data:bjca-footstone-bpring-data-keyvalue:jar:3.5.13-nes.patch.1-SNAPSHOT`）
- [x] 2.8 验证依赖树：运行时 Spring 家族坐标全部为 fork（commons `3.5.13-nes.patch.1`、context/tx 及传递 core/beans/expression/aop/jcl `6.2.19-nes.patch.1`），官方运行时坐标清零；包名/模块名不变 → `-U` 强更后 compile/runtime 官方 `org.springframework` 坐标清零（commons fork 已修复其传递依赖为 fork 坐标）

## 阶段 3：私服配置

- [x] 3.1 `pom.xml` 增加 `<distributionManagement>`（releases/snapshots + `${nexusReleaseUrl/nexusSnapshotUrl}`）
- [x] 3.2 移除 `pom.xml` 中外网 `repo.spring.io` 的 `<repositories>`（改由私服 maven-public 聚合上游）
- [x] 3.3 确认私服属性/凭证/mirror 由 `~/.m2/settings.xml` 的 `bjca` profile 提供；项目内另置 `settings.xml` 供参考
- [x] 3.4 `clean deploy` 真实发布 —— 用户已手动执行 `make clean deploy` 成功，制品发布至 Nexus 私服

## 阶段 4：CVE 文档体系

- [x] 4.1 `doc/CVE/CVE-2026-41719.md`（本体，✅已修复，修复方=官方 upstream @3.5.12，含现状核对 + 数据流 + 验证证据 + 参考链接）
- [x] 4.2 `doc/VULNERABILITY_REPORT.md`：6 态归一化总览 + 依赖坐标说明 + 统计 + 索引；明确「官方基线已修复，本 fork 仅验证」，措辞不照抄 2.7 的 backport 表述
- [x] 4.3 校验 CVE 文档链接与状态一致（VULNERABILITY_REPORT ↔ CVE-2026-41719 状态均 ✅已修复；测试数 2+11 与源码核对一致）

## 阶段 5：交付文档

- [x] 5.1 `doc/GAV_MAPPING.md`：GAV 映射表（含 parent、三条 fork 依赖坐标）
- [x] 5.2 `doc/QUICK_START.md`：快速入门（新 GAV 坐标、默认 settings、Java 17、fork 依赖前置条件）
- [x] 5.3 `doc/USER_MANUAL.md`：用户手册（fork 定位、GAV 变化、CVE-2026-41719 官方已修复、与官方差异、包名/模块名不变）
- [x] 5.4 `doc/REQUIREMENTS.md`：需求与版本清单
- [x] 5.5 `Makefile`：build / test / install / deploy / security 快捷命令（用 `mvnw` + `~/.m2/settings.xml`）

## 阶段 6：收尾

- [x] 6.1 `openspec validate nes-patch-2026 --strict` 通过（"Change 'nes-patch-2026' is valid"）
- [x] 6.2 全量构建 + 测试最终验证（`make install` BUILD SUCCESS；`make test` 374 run / 0 fail / 0 error / 13 skip；`make security` 13 run 全绿；deploy 按惯例留手动）
- [x] 6.3 更新 `VULNERABILITY_REPORT` 最终状态（CVE-2026-41719 = ✅已修复 / 官方 upstream @3.5.12，本基线 3.5.13 包含）
- [x] 6.4 归档 change 到 `openspec/changes/archive/`（用户确认后执行，已归档为 `2026-07-09-nes-patch-2026`）
