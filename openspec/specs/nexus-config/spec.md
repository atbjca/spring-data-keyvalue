# nexus-config Specification

## Purpose
TBD - created by archiving change nes-patch-2026. Update Purpose after archive.
## Requirements
### Requirement: 发布管理配置

`pom.xml` MUST 通过 `<distributionManagement>` 将制品发布指向内网 Nexus 私服，URL 由外部属性提供，不硬编码。

#### Scenario: distributionManagement 存在
- **WHEN** 检查 `pom.xml`
- **THEN** 存在 `<distributionManagement>`，含 `releases`（`${nexusReleaseUrl}`）与 `snapshots`（`${nexusSnapshotUrl}`）两个仓库

#### Scenario: 凭证与属性外部化
- **WHEN** 构建发布制品
- **THEN** 私服 URL 属性、server 凭证、mirror 由 `~/.m2/settings.xml` 的 `bjca` profile 提供
- **AND** `pom.xml` 不含明文凭证

### Requirement: 外网仓库隔离

本项目 MUST 移除指向外网（`repo.spring.io`）的 `<repositories>`，改由内网私服 maven-public 聚合上游，实现外网隔离。

#### Scenario: 无外网仓库声明
- **WHEN** 检查 `pom.xml`
- **THEN** 不含指向 `https://repo.spring.io/*` 的 `<repository>` 声明

#### Scenario: 依赖从私服解析
- **WHEN** 执行 `./mvnw clean install`
- **THEN** 全部依赖（含 parent、fork 依赖、上游第三方）从内网私服/本地仓库解析成功

