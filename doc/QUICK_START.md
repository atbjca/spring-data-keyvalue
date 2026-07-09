# 快速入门 (Quick Start)

## 环境要求

| 项 | 要求 |
|----|------|
| JDK | Java 17（3.5.x 基线要求；本地 sdkman 可用 `17.0.17-amzn`） |
| Maven | 3.9+（项目自带 `mvnw` 3.9.16） |
| 私服 | 内网 Nexus `http://192.168.131.36:8088`（依赖解析与发布） |
| settings | `~/.m2/settings.xml` 提供 `nexus` mirror、`bjca` profile（含 `nexusReleaseUrl` / `nexusSnapshotUrl` 属性，默认激活）及 `snapshots` 发布凭证。命令无需 `-s`，Maven 默认读取 |

## 前置条件（fork 依赖就绪）

本项目运行时依赖三条 fork 制品，构建前需确保它们已在 `~/.m2` 或内网私服可解析：

| 依赖 | 版本 |
|------|------|
| `bjca-footstone-bpring-data-commons` | `3.5.13-nes.patch.1-SNAPSHOT` |
| `bjca-footstone-bpring-context` | `6.2.19-nes.patch.1-SNAPSHOT` |
| `bjca-footstone-bpring-tx` | `6.2.19-nes.patch.1-SNAPSHOT` |

## 三步上手

### 1. 编译

```bash
./mvnw -DskipTests clean package
```

### 2. 运行测试

```bash
./mvnw test
```

### 3. 安装到本地仓库

```bash
./mvnw -DskipTests clean install
```

## 使用 Makefile（推荐）

```bash
make build      # 编译打包（跳过测试）
make test       # 运行测试
make install    # 安装到本地仓库
make deploy     # 发布到 Nexus 私服
make security   # 运行 CVE-2026-41719 SpEL 安全回归测试
```

## 引入依赖

```xml
<dependency>
    <groupId>cn.bjca.footstone.bpring.data</groupId>
    <artifactId>bjca-footstone-bpring-data-keyvalue</artifactId>
    <version>3.5.13-nes.patch.1-SNAPSHOT</version>
</dependency>
```

> Java 包名保持 `org.springframework.data.keyvalue.*` 不变，源代码 `import` 无需修改。

## 更多文档

- [用户手册](USER_MANUAL.md)
- [GAV 映射表](GAV_MAPPING.md)
- [漏洞状态总览](VULNERABILITY_REPORT.md)
- [需求与版本清单](REQUIREMENTS.md)
