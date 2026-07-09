# gav-renaming Specification

## Purpose
TBD - created by archiving change nes-patch-2026. Update Purpose after archive.
## Requirements
### Requirement: GroupId 去特征化

本项目发布坐标的 GroupId MUST 从 `org.springframework.data` 重命名为 `cn.bjca.footstone.bpring.data`，以规避 SCA 工具按官方 GAV 特征误报 CVE。

`cn.bjca.footstone.bpring.data` 与同属 data 线的 `spring-data-commons` NES fork 共用同一 GroupId（`.data` 后缀），与 framework 线（`cn.bjca.footstone.bpring`，无后缀）区分。

#### Scenario: GroupId 重命名

- **WHEN** 构建并发布本项目
- **THEN** 发布 POM 中的 `groupId` 为 `cn.bjca.footstone.bpring.data`

#### Scenario: Java 包名保持不变

- **WHEN** 下游项目依赖本项目
- **THEN** Java 源码中的 `import org.springframework.data.keyvalue.*` / `import org.springframework.data.map.*` 无需修改
- **AND** 仅 Maven 坐标发生变化

### Requirement: ArtifactId 去特征化

本项目的 ArtifactId MUST 从 `spring-data-keyvalue` 重命名为 `bjca-footstone-bpring-data-keyvalue`，遵循家族统一规则 `spring-<x>` → `bjca-footstone-bpring-<x>`。

#### Scenario: 主制品重命名

- **WHEN** 构建本项目
- **THEN** 发布的 ArtifactId 为 `bjca-footstone-bpring-data-keyvalue`

### Requirement: 版本号规范化

版本号 MUST 遵循 `X.Y.Z-nes.patch.N-SNAPSHOT` 格式，并与 data 线其他 NES fork（`spring-data-commons`）版本号保持一致，锚定在上游最后正式发布号 `2.7.18`（而非当前上游快照号 `2.7.19`）。

#### Scenario: 版本格式

- **WHEN** 检查 `pom.xml` 的 `version`
- **THEN** 版本号为 `2.7.18-nes.patch.1-SNAPSHOT`

### Requirement: 依赖坐标联动

本项目的运行时依赖中，凡属 NES fork 家族的坐标 MUST 同步替换为 fork 坐标，避免同时引入官方坐标与 NES 坐标的重复制品。第三方库（非 Spring 家族）坐标 MUST 保持上游不变。

替换后的依赖坐标如下：

| 依赖 | 原坐标 | NES fork 坐标 | 作用域 |
|------|--------|--------------|--------|
| spring-data-commons | `org.springframework.data:spring-data-commons:2.7.19-SNAPSHOT` | `cn.bjca.footstone.bpring.data:bjca-footstone-bpring-data-commons:2.7.18-nes.patch.1-SNAPSHOT` | compile |
| spring-context | `org.springframework:spring-context` | `cn.bjca.footstone.bpring:bjca-footstone-bpring-context:5.3.39-nes.patch.1-SNAPSHOT` | compile |
| spring-tx | `org.springframework:spring-tx` | `cn.bjca.footstone.bpring:bjca-footstone-bpring-tx:5.3.39-nes.patch.1-SNAPSHOT` | compile |
| querydsl-collections | `com.querydsl:querydsl-collections` | 保持上游不变（第三方库，不去特征化） | optional |
| joda-time | `joda-time:joda-time` | 保持上游不变（第三方库，仅测试） | test |

> framework 依赖版本 `5.3.39-nes.patch.1-SNAPSHOT` 已在本地 `~/.m2/repository/cn/bjca/footstone/bpring/` 核实可解析。framework 的 `doc/GAV_MAPPING.md` 曾记录 `5.3.39-bjca-patched.1`，属过时文档，以实际发布制品版本为准。

#### Scenario: NES 家族依赖替换

- **WHEN** 检查发布 POM 的 `<dependencies>`
- **THEN** `spring-data-commons` / `spring-context` / `spring-tx` 均指向 `cn.bjca.footstone.bpring*` 坐标
- **AND** 不再出现 `org.springframework*` 官方坐标

#### Scenario: 第三方依赖保持原样

- **WHEN** 检查 `querydsl-collections`、`joda-time` 等第三方依赖坐标
- **THEN** GroupId / ArtifactId 保持上游官方值不变

### Requirement: Parent 引用处理

parent MUST 保留原坐标 `org.springframework.data.build:spring-data-parent`，**不 fork `spring-data-build`**，并锚定上游已正式发布的稳定版本 `2.7.18`（而非当前上游快照 `2.7.19-SNAPSHOT`，也不改为 `-nes.patch` 版本）。该正式版 parent 已存在于本地 `~/.m2/repository/org/springframework/data/build/spring-data-parent/2.7.18/`，本地与内网私服均可解析。

> 决策理由：parent POM 仅提供依赖管理与插件配置，不构成运行时制品，也不携带需去特征化的运行时 GAV 特征。锚定上游正式发布的 `2.7.18` 可避免额外 fork `spring-data-build`，简化维护链路。子项目自身发布版本 `2.7.18-nes.patch.1-SNAPSHOT` 与 parent 版本 `2.7.18` 无需一致。

#### Scenario: 父 POM 坐标与版本

- **WHEN** 检查 `pom.xml` 的 `<parent>`
- **THEN** groupId 为 `org.springframework.data.build`，artifactId 为 `spring-data-parent`，version 为 `2.7.18`
- **AND** `<relativePath/>` 置空，强制从仓库解析

#### Scenario: 父 POM 可解析

- **WHEN** 执行 `./mvnw -s settings.xml clean install`
- **THEN** 构建能从 `~/.m2` 或内网私服成功解析 `spring-data-parent:2.7.18`
- **AND** 构建产物不残留官方运行时 GAV 特征

### Requirement: JPMS 模块名保持

`java-module-name`（`spring.data.keyvalue`）MUST 保持不变，避免破坏下游 JPMS `requires` 声明。

#### Scenario: 模块名不变

- **WHEN** 检查发布制品的自动模块名
- **THEN** 自动模块名仍为 `spring.data.keyvalue`

### Requirement: 构建依赖顺序约束

由于本项目 compile 依赖 `bjca-footstone-bpring-data-commons`，而该制品当前尚未部署至本地 `~/.m2` 或内网私服，构建前 MUST 确保 commons NES fork 已先行构建并部署，否则依赖无法解析。

#### Scenario: commons 制品缺失时的构建

- **WHEN** 本地 `~/.m2` 与内网私服均无 `bjca-footstone-bpring-data-commons:2.7.18-nes.patch.1-SNAPSHOT`
- **THEN** 构建 MUST 明确失败并提示先构建 commons fork（不得回退到官方坐标）

