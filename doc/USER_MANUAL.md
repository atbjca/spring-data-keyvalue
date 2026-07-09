# 用户手册 (User Manual)

## 1. 项目定位

本项目是 **Spring Data KeyValue 3.5.x 的 NES（Never-Ending Support）fork**，由 BJCA 维护分支管理。其核心目标：

1. **安全基线对齐**：本库自身携带的本体 CVE-2026-41719（SpEL 排序注入），官方已在 3.5.12 修复，本基线 3.5.13 已包含；本 fork 验证修复生效并文档留痕（**不改动业务代码**）。
2. **GAV 去特征化**：重命名 Maven 坐标，规避 SCA 工具误报。
3. **私服隔离**：依赖解析与制品发布统一走内网 Nexus，运行时 Spring 家族坐标全部替换为 fork 坐标。

本项目与官方 3.5.13 功能对齐，仅在坐标与发布渠道上有差异；**源码不含任何本 fork 私有改动**。

## 2. 与官方版本的差异

| 维度 | 官方 3.5.13 | 本 NES fork |
|------|------------|-------------|
| GAV | `org.springframework.data:spring-data-keyvalue:3.5.13` | `cn.bjca.footstone.bpring.data:bjca-footstone-bpring-data-keyvalue:3.5.13-nes.patch.1-SNAPSHOT` |
| CVE-2026-41719 | ✅ 已修复（3.5.12） | ✅ 继承官方修复 |
| 运行时依赖坐标 | 官方 Spring 家族坐标 | 全部换 fork 坐标（commons / context / tx，官方运行时坐标清零） |
| Java 包名 | `org.springframework.data.keyvalue.*` | 不变 |
| JPMS 模块名 | `spring.data.keyvalue` | 不变 |
| 业务源码 | — | 完全一致（零私有改动） |

详见 [GAV 映射表](GAV_MAPPING.md) 与 [漏洞状态总览](VULNERABILITY_REPORT.md)。

> **与 spring-data-keyvalue-2.7 fork 的区别**：2.7.x 属 EOL、官方无补丁，2.7 fork 需自行 backport CVE-2026-41719；而 3.5.x 是活跃线、官方已在 3.5.12 修复，本 fork 直接继承官方修复，不做 backport。实际上 2.7 fork 的修复正是从 3.5.12 官方"标准答案"反向移植而来。

## 3. 安全修复说明

本体 SpEL 注入漏洞由**官方 upstream 在 3.5.12 修复**，本基线 3.5.13 已包含。本 fork 运行回归测试确认防护生效，对正常业务无影响。

### 3.1 CVE-2026-41719 — SpelPropertyComparator SpEL 排序注入

`SpelPropertyComparator` 对 KeyValue 查询结果按属性排序时，将排序属性名 `path` 拼进 SpEL 表达式求值。历史漏洞版本将 `path` 拼入含方法调用的表达式并在默认 `StandardEvaluationContext` 求值，当排序属性名可由不可信输入间接控制时，攻击者可构造恶意 `path` 触发任意方法调用（CWE-917）。

官方三道防线（本基线均已就位）：

1. **比较回归 Java 侧**：排序大小由 `Comparator.nullsFirst/nullsLast(naturalOrder())` 决定，SpEL 只负责取属性值（`SpelPropertyComparator.java:36-37`, `:131`）。
2. **表达式收窄**：`buildExpressionForPath()` 仅生成 `#arg1?.<path>` 纯属性导航，无构造器/方法拼接（`:120-122`）。
3. **受限求值上下文**：改用 `SimpleEvaluationContext.forReadOnlyDataBinding()`，禁用方法调用、类型引用 `T(...)`、构造器，恶意 `path` 触发方法调用即抛 `EvaluationException`（`:134-143`）。

正常属性排序（合法 `path`，含嵌套 `a.b.c`）语义与官方完全一致，不受影响。详见 [CVE-2026-41719](CVE/CVE-2026-41719.md)。

## 4. 构建与发布

参见 [快速入门](QUICK_START.md)。常用命令：

```bash
make build      # 编译打包
make test       # 运行测试
make install    # 安装到本地仓库
make deploy     # 发布到 Nexus 私服
make security   # CVE-2026-41719 SpEL 安全回归
```

## 5. 私服配置

`pom.xml` 的 `<distributionManagement>` 使用属性占位：

```xml
<distributionManagement>
    <repository>
        <id>releases</id>
        <url>${nexusReleaseUrl}</url>
    </repository>
    <snapshotRepository>
        <id>snapshots</id>
        <url>${nexusSnapshotUrl}</url>
    </snapshotRepository>
</distributionManagement>
```

`${nexusReleaseUrl}` / `${nexusSnapshotUrl}` 及凭证 `releases` / `snapshots` 由 `~/.m2/settings.xml` 的 `bjca` profile（默认激活）提供。外网 `repo.spring.io` 仓库声明已移除，依赖统一由内网私服 maven-public 聚合上游解析。
