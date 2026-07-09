## ADDED Requirements

### Requirement: GroupId 去特征化

本项目发布坐标的 GroupId MUST 从 `org.springframework.data` 重命名为 `cn.bjca.footstone.bpring.data`，以规避 SCA 工具按官方 GAV 特征误报 CVE。

#### Scenario: GroupId 重命名
- **WHEN** 构建并发布本项目
- **THEN** 发布 POM 中的 `groupId` 为 `cn.bjca.footstone.bpring.data`

#### Scenario: Java 包名保持不变
- **WHEN** 下游项目依赖本项目
- **THEN** Java 源码中的 `import org.springframework.data.keyvalue.*` / `org.springframework.data.map.*` 无需修改
- **AND** 仅 Maven 坐标发生变化

### Requirement: ArtifactId 去特征化

本项目的 ArtifactId MUST 从 `spring-data-keyvalue` 重命名为 `bjca-footstone-bpring-data-keyvalue`。

#### Scenario: 主制品重命名
- **WHEN** 构建本项目
- **THEN** 发布的 ArtifactId 为 `bjca-footstone-bpring-data-keyvalue`

### Requirement: 版本号规范化

版本号 MUST 遵循 `X.Y.Z-nes.patch.N-SNAPSHOT` 格式，锚定已发布正式基线 `3.5.13`。

#### Scenario: 版本格式
- **WHEN** 检查 `pom.xml` 的 `version`
- **THEN** 版本号为 `3.5.13-nes.patch.1-SNAPSHOT`

### Requirement: Parent 引用处理

parent（`org.springframework.data.build:spring-data-parent`）MUST 采用方案 A（保留原坐标），版本从 `3.5.14-SNAPSHOT` 锁定为已发布正式版 `3.5.13`，以保证可从私服/本地仓库解析，且不残留 SNAPSHOT 父 POM 解析风险。

#### Scenario: 父 POM 版本锁定为正式版
- **WHEN** 检查 `pom.xml` 的 `<parent>`
- **THEN** `groupId` 为 `org.springframework.data.build`，`artifactId` 为 `spring-data-parent`，`version` 为 `3.5.13`

#### Scenario: 父 POM 可解析
- **WHEN** 执行 `./mvnw clean install`
- **THEN** 构建能成功解析父 POM `spring-data-parent:3.5.13`
- **AND** 构建产物不残留官方 GAV 特征（本制品坐标已去特征化）

### Requirement: 依赖坐标联动去特征化

本项目对 Spring 家族依赖的坐标 MUST 联动替换为对应 NES fork 坐标，使运行时依赖树不残留官方 Spring 坐标。第三方依赖（querydsl）保持上游官方坐标。

#### Scenario: spring-data-commons 换 fork 坐标
- **WHEN** 检查 `pom.xml` 的 spring-data-commons 依赖
- **THEN** 坐标为 `cn.bjca.footstone.bpring.data:bjca-footstone-bpring-data-commons:3.5.13-nes.patch.1-SNAPSHOT`

#### Scenario: spring-context / spring-tx 换 framework fork 坐标
- **WHEN** 检查 `pom.xml` 的 spring-context / spring-tx 依赖
- **THEN** 坐标为 `cn.bjca.footstone.bpring:bjca-footstone-bpring-context:6.2.19-nes.patch.1-SNAPSHOT` 与 `cn.bjca.footstone.bpring:bjca-footstone-bpring-tx:6.2.19-nes.patch.1-SNAPSHOT`

#### Scenario: 运行时官方坐标清零
- **WHEN** 执行 `./mvnw dependency:tree`
- **THEN** compile/runtime 作用域的 Spring 家族坐标全部为 `cn.bjca.footstone.bpring*` fork 坐标
- **AND** 传递引入的 spring-core/beans/expression/aop/jcl 均为 fork `6.2.19-nes.patch.1` 坐标

#### Scenario: 第三方依赖不去特征化
- **WHEN** 检查 querydsl-collections 依赖
- **THEN** 保持上游官方坐标 `com.querydsl:querydsl-collections`（`optional`）

### Requirement: JPMS 模块名保持

`java-module-name`（`spring.data.keyvalue`）MUST 保持不变，避免破坏下游 JPMS `requires` 声明。

#### Scenario: 模块名不变
- **WHEN** 检查发布制品的自动模块名
- **THEN** `Automatic-Module-Name` 仍为 `spring.data.keyvalue`
