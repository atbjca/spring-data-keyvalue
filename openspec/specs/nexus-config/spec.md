# nexus-config Specification

## Purpose
TBD - created by archiving change nes-patch-2026. Update Purpose after archive.
## Requirements
### Requirement: Nexus 私服发布配置

`pom.xml` MUST 通过 `<distributionManagement>` 配置内网 Nexus 私服作为构件发布目标，URL 使用属性占位以便环境隔离。

#### Scenario: distributionManagement 配置

- **WHEN** 检查 `pom.xml`
- **THEN** MUST 存在 `<distributionManagement>`，包含 `releases`（`${nexusReleaseUrl}`）与 `snapshots`（`${nexusSnapshotUrl}`）两个仓库

#### Scenario: 属性由 settings 提供

- **WHEN** 执行发布
- **THEN** `${nexusReleaseUrl}` / `${nexusSnapshotUrl}` MUST 由 `~/.m2/settings.xml` 的 profile 属性解析
- **AND** server 凭证 id `releases` / `snapshots` MUST 与 settings 中一致

### Requirement: 私服依赖解析

构建 MUST 优先从内网 Nexus 私服解析依赖，隔离外网；本地 `~/.m2` 已有制品可直接命中。

#### Scenario: 依赖从私服拉取

- **WHEN** 执行 `./mvnw -s settings.xml clean install`
- **THEN** 依赖 MUST 通过内网 Nexus 聚合仓库解析成功
- **AND** MUST NOT 依赖外网直连

#### Scenario: 兄弟 fork 依赖可解析

- **WHEN** 解析 `bjca-footstone-bpring-data-commons:2.7.18-nes.patch.1-SNAPSHOT`
- **THEN** 该制品 MUST 已由 commons fork 先行部署至 `~/.m2` 或内网私服（见 `gav-renaming` 构建依赖顺序约束）
- **AND** 若缺失，构建 MUST 明确失败，MUST NOT 回退官方坐标

#### Scenario: 快照发布

- **WHEN** 执行 `./mvnw -s settings.xml deploy`
- **THEN** `2.7.18-nes.patch.1-SNAPSHOT` 制品 MUST 发布到 snapshots 仓库

