## Why

`spring-data-keyvalue` 2.7.x 已随 Spring Data 2021.2.x 版本线进入开源生命周期终点（EOL），官方不再为该版本线发布安全补丁。BJCA 维护分支 `2.7.x-bjca-patch` 需要建立一套标准的 **NES（Never-Ending Support）安全维护流程**，与 `spring-framework-5.3`、`spring-kafka-2.9`、`spring-boot-2.7`、`spring-data-commons-2.7` 已完成或进行中的 NES fork 对齐，具体解决四个问题：

1. **本体安全修复**：本项目自身即 SpEL 注入类漏洞（CVE-2026-41719）的宿主。经源码审计确认，`SpelPropertyComparator.buildExpressionForPath()` 将排序属性路径 `path` 直接拼入 SpEL 表达式后交由 `parser.parseRaw()` 解析执行（`SpelPropertyComparator.java:116-122` / `:105`），攻击面真实存在于 `org.springframework.data.keyvalue.core` 包。本项目必须像 `spring-data-commons` 一样 backport 官方补丁或实施缓解（而非像 `spring-boot` 等下游项目那样标"免疫"）。
2. **私服隔离**：统一依赖下载与构件发布渠道，通过内网 Nexus 私服隔离外网。
3. **GAV 去特征化**：重命名 Maven 坐标，规避 SCA 工具按官方 GAV 特征误报 CVE（详见 `specs/gav-renaming`）。
4. **文档化**：建立完整的漏洞报告体系与交付文档。

## What Changes

- **本体 CVE 修复（TDD）**：
  - CVE-2026-41719：SpEL 排序注入。`SpelPropertyComparator` / `SpelQueryEngine` / `SpelSortAccessor` 构成的排序链路将属性路径拼入 SpEL 并解析执行。**官方修复的具体手段（属性路径合法性校验 / 改用非 SpEL 比较器 / 限制表达式能力）待 `cve-assessment` 阶段核实定案后写入 `design.md`，本提案不预设结论以避免知识幻觉。**
  - 其余经 SCA 扫描命中的 CVE：逐条完成适用性分析并归一化状态。
- **CVE 全量排查**：使用 `~/dev/dependency-check` 对本项目全部依赖做一次 SCA 扫描，将命中项纳入批次评估。本项目运行时依赖极薄（`spring-data-commons` / `spring-context` / `spring-tx` 均为 NES fork 兄弟项目，状态"继承"自各自 fork），核心真实攻击面为自身 SpEL 代码。
- **GAV 去特征化**（已在 `specs/gav-renaming` 定稿）：
  - GroupId: `org.springframework.data` → `cn.bjca.footstone.bpring.data`
  - ArtifactId: `spring-data-keyvalue` → `bjca-footstone-bpring-data-keyvalue`
  - Version: `2.7.19-SNAPSHOT` → `2.7.18-nes.patch.1-SNAPSHOT`
  - 依赖坐标联动：commons / context / tx 三条 fork 坐标同步替换。
  - Parent `spring-data-parent` 保留原坐标不 fork，锚定上游正式版 `2.7.18`（本地 `~/.m2` 已可解析）。
- **私服配置**：`pom.xml` 增加 `<distributionManagement>`，`settings.xml` 对齐内网 Nexus。
- **CVE 文档体系**（参考 `spring-boot-2.7/doc/`）：
  - `doc/VULNERABILITY_REPORT.md`：漏洞状态总览，6 态归一化（✅已修复 / ⬜免疫 / ❌不适用 / 🔧修复中 / ⚠️已缓解 / ⏸️暂缓）。
  - `doc/CVE/CVE-xxxx.md`：每个 CVE 一个独立文档。
- **交付文档**：`doc/USER_MANUAL.md`、`doc/QUICK_START.md`。
- **构建快捷方式**：`Makefile`（build / test / install / deploy），基于 Maven + Java 8（`8.0.482-kona`）。

## Capabilities

### New Capabilities

- `cve-assessment`：SCA 扫描 + CVE 适用性分析 + 优先级矩阵。
- `cve-remediation`：本体 SpEL CVE 的 backport 修复与缓解（TDD 驱动）——**本项目区别于下游 NES fork 的核心价值**。
- `cve-documentation`：漏洞报告与 CVE 独立文档体系。
- `gav-renaming`：GAV 去特征化重命名逻辑（**已定稿**）。
- `nexus-config`：Nexus 私服配置与发布管理。
- `build-documentation`：构建、发布与交付文档。

### Modified Capabilities

- （无——本项目此前无 openspec 受管能力）

## Impact

- **源码（本体修复）**：`src/main/java/org/springframework/data/keyvalue/core/SpelPropertyComparator.java`、`SpelQueryEngine.java`、`SpelSortAccessor.java`、`SpelCriteriaAccessor.java`。
- **测试（TDD 先行）**：`SpelPropertyComperatorUnitTests`（现有）+ 新增 SpEL 注入触发用例；核心业务逻辑测试覆盖度须 ≥ 60%。
- **构建配置**：`pom.xml`（groupId / artifactId / version / parent / dependencies / distributionManagement / java-module-name）、`settings.xml`。
- **新增文件**：`Makefile`、`doc/VULNERABILITY_REPORT.md`、`doc/CVE/*.md`、`doc/USER_MANUAL.md`、`doc/QUICK_START.md`。
- **下游影响**：依赖方需更新 GAV 坐标（Java `import` 与 JPMS `requires` 无需改动，包名与模块名不变）。
- **风险点**：
  - **构建依赖顺序**：本项目依赖 `bjca-footstone-bpring-data-commons`，该制品当前尚未部署至 `~/.m2` 或内网私服，构建前须先行构建并部署 commons fork（见 `specs/gav-renaming` 构建依赖顺序约束）。
  - **Parent 解析**：`spring-data-parent` 保留原坐标锚定正式版 `2.7.18`（不 fork），已确认本地 `~/.m2` 可解析；内网私服可解析性待构建阶段验证。
  - **本体修复语义**：改动 SpEL 排序行为需确保不破坏正常按属性排序语义（现有测试全绿 + 新增边界与注入用例）。
