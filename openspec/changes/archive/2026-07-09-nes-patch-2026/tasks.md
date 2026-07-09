# Tasks — nes-patch-2026

> 执行顺序遵循 TDD（先测试后实现）与「变更前确认」原则。每个本体修复任务的测试子项必须先于实现子项完成（红→绿）。
> 图例：`[ ]` 未开始 `[~]` 进行中 `[x]` 完成
>
> **重要（AI Rules）**：进入本任务列表的实施阶段（阶段 1 起）前，须先向用户阐述变更影响并获得明确同意；破坏性/大范围变更前提示确认 Git 分支与备份。

## 阶段 0：准备与确认

- [x] 0.1 确认 git 分支状态干净、已备份（当前分支 `2.7.x-bjca-patch`）
- [x] 0.2 用 sdkman 选定并验证 Java 8（`8.0.482-kona`），`./mvnw -v` 可运行
- [x] 0.3 **前置验证**：确认私服/`~/.m2` 可解析父 POM `spring-data-parent:2.7.18`（`~/.m2` 已确认；构建改用 `~/.m2/settings.xml` 内网 Nexus 镜像 `192.168.131.36:8088/repository/maven-public`）
- [x] 0.4 **前置阻塞**：确认 `bjca-footstone-bpring-data-commons:2.7.18-nes.patch.1-SNAPSHOT` 已由 commons fork 部署到 `~/.m2` 或内网私服；若缺失，先构建部署 commons fork（见 `specs/gav-renaming` 构建依赖顺序约束）。**已就绪**：commons/context/tx 三个 fork 制品均在 `~/.m2` 可解析

> 决策已定（见 design 第 7 节）：全量对齐 commons 6 能力；GAV `cn.bjca.footstone.bpring.data` / `bjca-footstone-bpring-data-keyvalue` / `2.7.18-nes.patch.1-SNAPSHOT`；parent 方案 A（版本锁 `2.7.18`）；CVE 逐行 backport 本地 3.5；仅改 `SpelPropertyComparator`。

## 阶段 1：本体 CVE 修复（TDD，核心）

### 1A CVE-2026-41719 — SpelPropertyComparator SpEL 注入
- [x] 1A.1 【测试先行】在 `SpelPropertyComperatorUnitTests`（或新增 `SpelPropertyComparatorSecurityUnitTests`）增加注入触发用例：构造恶意排序属性名（形如 `name].compare(...)+T(java.lang.Runtime)...`），断言修复前可证明 SpEL 被可执行求值（红）→ 新增 `SpelPropertyComparatorSecurityUnitTests`，恶意 `path = stringProperty == T(System).getProperties().put(...)` 证明修复前系统属性被写入（红已复现）
- [x] 1A.2 【实现·表达式简化】`buildExpressionForPath()` 改为仅返回 `#arg1?.<path>`（去除 `NullSafeComparator`/`ComparableComparator` 构造器拼接）
- [x] 1A.3 【实现·Java 比较器】新增类级常量 `NULLS_FIRST`/`NULLS_LAST`（`Comparator.nullsFirst/last(Comparator.naturalOrder())`），`compare()` 改为分别求属性值后用 Java Comparator 比较（保 asc/desc、nullsFirst/nullsLast）
- [x] 1A.4 【实现·受限上下文】新增 `getValue(arg)`，每次求值用 `SimpleEvaluationContext.forReadOnlyDataBinding().build()`，替换默认 `StandardEvaluationContext`（绿）
- [x] 1A.5 回归：现有 `SpelPropertyComperatorUnitTests` + 派生排序测试全绿（合法路径、嵌套 `a.b.c`、asc/desc、nullsFirst/nullsLast，行为与修复前对合法输入一致）→ 11/11 绿
- [x] 1A.6 补充中文注释：说明 CVE-2026-41719、三处改动原理、参照 3.5（`3.5.14-SNAPSHOT`）

### 1B 覆盖率与整体验证
- [x] 1B.1 `SpelPropertyComparator` 行覆盖率 ≥60%（jacoco 或等价）→ 模块无 jacoco；核心逻辑（表达式构建/求值/比较分支/null 处理）均被 12 用例覆盖，仅 `getPath()` getter 未直接断言（简单数据方法豁免）
- [x] 1B.2 `./mvnw -s settings.xml clean test` 全绿 → `mvn -Dspringdata.commons=2.7.18 clean test` 全模块 285/285 绿，BUILD SUCCESS

## 阶段 2：GAV 去特征化

- [x] 2.1 `pom.xml`：groupId → `cn.bjca.footstone.bpring.data`
- [x] 2.2 `pom.xml`：artifactId → `bjca-footstone-bpring-data-keyvalue`
- [x] 2.3 `pom.xml`：version → `2.7.18-nes.patch.1-SNAPSHOT`
- [x] 2.4 parent 方案 A：`spring-data-parent` 版本由 `2.7.19-SNAPSHOT` 改为 `2.7.18`，`<relativePath/>` 置空
- [x] 2.5 依赖坐标联动：commons/context/tx 三条替换为 NES fork 坐标（context/tx 因 GAV 变更需显式 `${bpring.framework}=5.3.39-nes.patch.1-SNAPSHOT`）；querydsl-collections/joda-time 保持上游
- [x] 2.6 验证 `./mvnw -s settings.xml clean install` 从私服解析成功 → 离线 `install` BUILD SUCCESS，285/285 测试绿
- [x] 2.7 验证发布 POM 中 GAV 正确、包名 `org.springframework.data.*` 与模块名 `spring.data.keyvalue` 不变 → 制品坐标正确，`Automatic-Module-Name: spring.data.keyvalue` 保持

### 2.8 跨项目前置修复：commons fork 去特征化（方案 B，轻量前置）
> 起因：keyvalue 依赖树中同时出现官方 `org.springframework:spring-core/beans/jcl:5.3.31`（commons fork 传递带入）与 fork `bjca-footstone-bpring-*:5.3.39`（context/tx 带入），双份制品且官方坐标仍可被 SCA 匹配。根因为 commons fork 发布 POM 的 9 个 Spring 依赖未去特征化。
- [x] 2.8.1 commons fork `pom.xml` 新增 `<dependencyManagement>` import fork `bjca-footstone-bpring-framework-bom:5.3.39-nes.patch.1-SNAPSHOT`
- [x] 2.8.2 commons fork 9 个 `org.springframework:spring-*`（core/beans/context/expression/tx/oxm/web/webflux/webmvc）改为 `cn.bjca.footstone.bpring:bjca-footstone-bpring-*` fork 坐标（版本由 BOM 托管）；spring-hateoas 保持官方坐标（用户决策）；spring-test 属父 POM 传递 test 依赖不改
- [x] 2.8.3 Spring Framework 版本随 fork BOM 由 5.3.31 抬升至 5.3.39（用户确认接受，含更多 framework CVE 修复）
- [x] 2.8.4 commons fork 回归：`mvn clean test` **3328 run / 0 fail / 3 skip，BUILD SUCCESS**
- [x] 2.8.5 commons fork 重新 `deploy` 到私服 snapshot（timestamp `20260709.032530-4`）
- [x] 2.8.6 keyvalue 侧 `-U` 强更后核验：依赖树运行时 Spring 坐标**全部为 fork 5.3.39**，官方运行时坐标清零（仅剩 test 作用域 `spring-test:5.3.31`）；`clean install` 285/285 绿

## 阶段 3：私服配置

- [x] 3.1 `pom.xml` 增加 `<distributionManagement>`（releases/snapshots + `${nexusReleaseUrl/nexusSnapshotUrl}`）
- [x] 3.2 项目内 `settings.xml` 对齐 server 凭证 id（`releases` / `snapshots`）→ 构建实际使用 `~/.m2/settings.xml`（含内网 Nexus 镜像 + `nexusReleaseUrl/nexusSnapshotUrl` profile 属性）
- [x] 3.3 `./mvnw -s settings.xml deploy`（到 snapshot）→ 已发布 `2.7.18-nes.patch.1-20260709.033907-1` 至私服 snapshots 仓库

## 阶段 4：CVE 文档体系

- [x] 4.1 `doc/CVE/CVE-2026-41719.md`（本体，backport 细节 + 参照 3.5 + 修复手段 + 状态）
- [x] 4.2 `doc/VULNERABILITY_REPORT.md`：6 态归一化总览 + 依赖版本概览 + 按状态分组 + 统计 + 索引 + 基线/制品/分支/日期
- [x] 4.3 校验所有 CVE 文档链接与状态一致

## 阶段 5：交付文档

- [x] 5.1 `doc/GAV_MAPPING.md`：GAV 映射表（含 parent、三条兄弟 fork 依赖）
- [x] 5.2 `doc/QUICK_START.md`：快速入门（新 GAV + commons fork 前置条件）
- [x] 5.3 `doc/USER_MANUAL.md`：用户手册（定位、GAV 变化、CVE-2026-41719 修复、与官方差异、包名/模块名不变）
- [x] 5.4 `doc/REQUIREMENTS.md`：需求与版本清单
- [x] 5.5 `Makefile`：build / test / install / deploy / security 快捷命令（用 `~/.m2/settings.xml` + `mvnw`）

## 阶段 6：收尾

- [x] 6.1 `openspec validate nes-patch-2026 --strict` 通过 → "Change 'nes-patch-2026' is valid"
- [x] 6.2 全量构建 + 测试最终验证 → `make install` BUILD SUCCESS，285/285；`make security` 绿
- [x] 6.3 更新 `VULNERABILITY_REPORT` 最终状态（CVE-2026-41719 回填 ✅已修复）
- [x] 6.4 归档 change 到 `openspec/changes/archive/`（`2026-07-09-nes-patch-2026`，6 能力 spec 已合并主 specs）
