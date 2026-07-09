# Design — nes-patch-2026 (spring-data-keyvalue 3.5)

> 本文档记录 spring-data-keyvalue-3.5 NES fork 改造的技术设计与关键决策。
> **核心特征：本体 CVE 官方已修复，本次零业务代码改动。** 遵循「变更前确认」与「避免知识幻觉」原则，所有结论均经本地源码/制品核实。

## 1. 背景与约束

| 项 | 值 |
|----|-----|
| 基线版本 | 上游 `3.5.14-SNAPSHOT`；改造后锚定已发布正式版 `3.5.13` 为 parent 版本 |
| 目标版本 | `3.5.13-nes.patch.1-SNAPSHOT` |
| 构建工具 | Maven（`mvnw`，本项目自带 wrapper）；**非 Gradle**（`~/dev` 下的 gradle 与本项目无关） |
| 私服 | 内网 Nexus（release / snapshot / 聚合 maven-public），由 `~/.m2/settings.xml` 的 `bjca` profile 提供 |
| Git 分支 | `3.5.x-bjca-patch`（工作区仅 `.claude/`、`openspec/` 未跟踪，干净） |
| Java | **JDK 17**（parent `spring-data-parent:3.5.13` 的 `source.level=17`）。工具链选 `17.0.17-amzn`（sdkman）。与 2.7 fork 的 Java 8 完全不同 |
| Spring Framework | 6.2.x（parent BOM 托管 `6.2.19`；fork 坐标 `6.2.19-nes.patch.1`） |

**核心约束**：本体 CVE-2026-41719 官方已在 3.5.12 修复，当前 `3.5.14-SNAPSHOT` 源码已包含该修复。本 fork 承担 NES 角色，但对本体 CVE **无需 backport**——只验证、不改码。

**与 2.7 fork 的本质区别**：

| 维度 | spring-data-keyvalue-2.7 | spring-data-keyvalue-3.5（本项目） |
|------|--------------------------|-----------------------------------|
| 版本线状态 | EOL，无开源补丁 | 官方活跃维护 |
| 本体 SpEL 源码 | **漏洞版**，需 backport | **已是官方修复版**，零改动 |
| 本项目角色 | 漏洞宿主本体，backport 修复方 | 官方修复的「标准答案」，仅验证 |
| Java 基线 | 8（`8.0.482-kona`） | 17（`17.0.17-amzn`） |
| Framework | 5.3.x | 6.2.x |
| 能力数 | 6（含 cve-assessment / cve-remediation） | 4（砍掉上述两个） |

## 2. 本体 CVE 验证方案（核心，零源码改动）

### 2.1 攻击面：SpEL 排序注入（CVE-2026-41719）

| 项 | 值 |
|----|-----|
| 编号 | CVE-2026-41719 |
| 类型 | SpEL 表达式注入（CWE-917） |
| CVSS | 6.4 MEDIUM（`AV:N/AC:H/PR:L/UI:N/S:U/C:H/I:L/A:L`） |
| 受影响 | spring-data-keyvalue 2.7.0 – 2.7.19、3.0.x – 3.5.11 |
| 官方修复 | 3.5.12 / 4.0.6 |
| 本项目基线 | **3.5.14-SNAPSHOT，已含官方 3.5.12 修复，不受影响** |

**历史数据流（修复前，2.7 漏洞版）**：

```
   不可信排序输入（Sort 属性名 path）
        │
        ▼  buildExpressionForPath()  ← 【旧注入点】path 直接拼入含构造器的 SpEL
   "new NullSafeComparator(new ComparableComparator(), <nullsFirst>)
        .compare(#arg1?.<path>, #arg2?.<path>)"
        │
        ▼  parser.parseRaw(...) + 默认 StandardEvaluationContext
        │  （允许构造器 / 方法 / T() 类型引用）
        ▼  恶意 path 被作为可执行 SpEL 求值 → 任意方法/类型调用
```

### 2.2 官方修复现状核对（3.5 源码已就位）

三处协同防护，将「表达式承载比较逻辑」改为「表达式只做属性导航、比较逻辑回归 Java、上下文受限」——经本地 `SpelPropertyComparator.java` 逐行核实**已全部就位**：

**① 表达式简化——只保留属性导航（`SpelPropertyComparator.java:120-121`）**

```java
protected String buildExpressionForPath() {
    return String.format("#arg1?.%s", path.replace(".", "?."));  // 仅属性导航，无构造器/方法
}
```

**② 比较逻辑移回 Java 侧常量比较器（`:36-37`、`:131`）**

```java
private static final Comparator<?> NULLS_FIRST = Comparator.nullsFirst(Comparator.naturalOrder());
private static final Comparator<?> NULLS_LAST  = Comparator.nullsLast(Comparator.naturalOrder());
// compare(): 分别求属性值后用 Java Comparator 比较（保 asc/desc、nullsFirst/nullsLast）
```

**③ 受限求值上下文——切断注入危害（`:134-143`）**

```java
private @Nullable Object getValue(@Nullable T arg) {
    SpelExpression expressionToUse = getExpression();
    SimpleEvaluationContext ctx = SimpleEvaluationContext.forReadOnlyDataBinding().build();
    ctx.setVariable("arg1", arg);
    expressionToUse.setEvaluationContext(ctx);
    return expressionToUse.getValue();
}
```

即便攻击者构造恶意 `path`，`SimpleEvaluationContext.forReadOnlyDataBinding()` 只允许属性读取，不解析构造器/方法/`T()`，注入无法落地。**双重防御**（表达式无害化 + 上下文受限）已与官方一致。

### 2.3 状态映射（已确定）

| CVE | 攻击面 | 官方修复 | 本基线 | 状态 |
|-----|:---:|:---:|:---:|:---:|
| CVE-2026-41719 | SpelPropertyComparator | 3.5.12 | 3.5.14-SNAPSHOT 已含 | ✅已修复（官方 upstream） |

### 2.4 验证方式（尊重「零源码改动」）

- **核对**：现有 `SpelPropertyComparatorUnitTests` 全绿，确认合法排序语义（嵌套 `a.b.c`、asc/desc、nullsFirst/nullsLast）未受影响。
- **可选安全回归**：新增一条注入防护断言用例（构造恶意 `path`，断言 `SimpleEvaluationContext` 下不发生方法调用/类型引用），固化官方修复。**仅新增测试、不改被测类**，符合「零源码改动」与「新增变更含测试」双重要求。用户已确认本体定位为「✅已修复（官方原生）」，故此测试为推荐项而非强制。

## 3. GAV 去特征化设计

| 坐标 | 原值 | 新值 |
|------|------|------|
| groupId | `org.springframework.data` | `cn.bjca.footstone.bpring.data` |
| artifactId | `spring-data-keyvalue` | `bjca-footstone-bpring-data-keyvalue` |
| version | `3.5.14-SNAPSHOT` | `3.5.13-nes.patch.1-SNAPSHOT` |
| java-module-name | `spring.data.keyvalue` | 保持不变（JPMS 模块名，改动影响下游 `requires`） |

**依赖坐标联动**（三条 fork 制品均已在 `~/.m2` 就绪）：

| 依赖 | 原坐标 | 新坐标 |
|------|--------|--------|
| spring-data-commons | `org.springframework.data:spring-data-commons` | `cn.bjca.footstone.bpring.data:bjca-footstone-bpring-data-commons:3.5.13-nes.patch.1-SNAPSHOT` |
| spring-context | `org.springframework:spring-context`（BOM 托管） | `cn.bjca.footstone.bpring:bjca-footstone-bpring-context:6.2.19-nes.patch.1-SNAPSHOT` |
| spring-tx | `org.springframework:spring-tx`（BOM 托管） | `cn.bjca.footstone.bpring:bjca-footstone-bpring-tx:6.2.19-nes.patch.1-SNAPSHOT` |
| querydsl-collections | `com.querydsl:querydsl-collections`（`optional`） | 保持上游（第三方，不去特征化） |

> 联动 context/tx 后，传递引入的 `spring-core`/`-beans`/`-expression`/`-aop`/`-jcl` 亦为 fork `6.2.19-nes.patch.1` 坐标，官方运行时坐标清零。此策略比 `spring-data-commons-3.5`（保留官方 spring 坐标）更彻底，与用户「联动换 framework fork 坐标」的决策一致。

**Parent 处理 —— 方案 A（保留原坐标，版本锁 3.5.13 正式版）**：

```xml
<parent>
  <groupId>org.springframework.data.build</groupId>
  <artifactId>spring-data-parent</artifactId>
  <version>3.5.13</version>   <!-- 原为 3.5.14-SNAPSHOT，改为已发布正式版 -->
  <relativePath/>
</parent>
```

```
✅ 方案 A：保留 spring-data-parent，版本锁 3.5.13 正式版
   方案 B：同步 fork spring-data-build ──（未采用）工作量翻倍
   方案 C：扁平化去 parent            ──（未采用）
```

理由：parent 只在构建期解析、不进发布制品 GAV 特征（SCA 扫的是制品自身坐标，已去特征化）；引用已发布 `3.5.13` 正式版彻底消除 SNAPSHOT 父 POM 私服解析不到的风险；`~/.m2` 已确认存在 `spring-data-parent-3.5.13.pom`。

**包名不改**：`org.springframework.data.keyvalue.*` / `org.springframework.data.map.*` 保持，下游 `import` 与 JPMS `requires` 无需改动。

## 4. 私服配置设计

`pom.xml` 增加：

```xml
<distributionManagement>
  <repository>
    <id>releases</id>
    <name>Nexus Release Repository</name>
    <url>${nexusReleaseUrl}</url>
  </repository>
  <snapshotRepository>
    <id>snapshots</id>
    <name>Nexus Snapshot Repository</name>
    <url>${nexusSnapshotUrl}</url>
  </snapshotRepository>
</distributionManagement>
```

`${nexusReleaseUrl}` / `${nexusSnapshotUrl}` 与 server 凭证由 `~/.m2/settings.xml` 的 `bjca` profile（默认激活）+ mirror 提供；同时移除 `pom.xml` 中指向外网 `repo.spring.io` 的 `<repositories>`（改由私服 maven-public 聚合），实现外网隔离。项目内另置一份 `settings.xml` 供无全局 settings 场景参考。

## 5. 变更影响分析（Impact Analysis）

| 受影响对象 | 变更 | 风险 | 缓解 |
|-----------|------|------|------|
| `SpelPropertyComparator` 等源码 | **无改动** | — | 官方基线已修复，仅核对测试 |
| `pom.xml` GAV | 坐标重命名 | 下游解析失败 | GAV_MAPPING 文档 + 保留包名/模块名 |
| parent 引用 | `3.5.14-SNAPSHOT` → `3.5.13` | 私服解析父 POM | 方案 A，锁正式版（`~/.m2` 已确认可解析） |
| 依赖坐标 | commons/context/tx 换 fork 坐标 | 兄弟 fork 制品缺失导致解析失败 | 三制品均已在 `~/.m2`；缺失时显式失败不回退 |
| `<repositories>` | 移除外网 `repo.spring.io` | 构建拉不到依赖 | 私服 maven-public 聚合上游 |
| 下游依赖方 | 需换 GAV | 构建中断 | 文档说明 + 版本对照表 |

**构建依赖顺序**：本项目 compile 依赖 commons/context/tx 三条 fork，均已在 `~/.m2` 可解析，**无前置构建阻塞**（区别于 2.7 曾需先部署 commons fork）。

## 6. 测试策略

- **核对**：`SpelPropertyComparatorUnitTests` 及派生排序测试全量通过，合法输入行为与官方一致。
- **可选安全回归**：补充注入防护断言（见 2.4），仅新增测试文件、不改被测类。
- **构建验证**：`./mvnw clean install` 从私服/`~/.m2` 解析全部 fork 依赖成功，全模块测试无回归。
- 本次零业务代码改动，不涉及新增业务逻辑，覆盖率要求由「核对既有官方测试通过」满足。

## 7. 决策记录（已拍板，2026-07-09）

1. **变更范围**：✅ 4 能力（cve-documentation / gav-renaming / nexus-config / build-documentation），对齐 `spring-data-commons-3.5`；不设 cve-assessment / cve-remediation。
2. **本体 CVE 定位**：✅ CVE-2026-41719 标 `✅已修复（官方 upstream @3.5.12）`，零源码改动（已核实 `SpelPropertyComparator` 就是 2.7 backport 的标准答案）。
3. **GAV 命名**：✅ `cn.bjca.footstone.bpring.data` / `bjca-footstone-bpring-data-keyvalue` / `3.5.13-nes.patch.1-SNAPSHOT`。
4. **Parent 处理**：✅ 方案 A（保留原坐标），版本锁已发布正式版 `3.5.13`（不 fork `spring-data-build`）。
5. **commons 依赖**：✅ 换 fork 坐标 `bjca-footstone-bpring-data-commons:3.5.13-nes.patch.1-SNAPSHOT`。
6. **framework 依赖**：✅ context/tx 联动换 fork 坐标 `bjca-footstone-bpring-*:6.2.19-nes.patch.1-SNAPSHOT`（比 commons-3.5 更彻底）。
7. **工具链**：✅ Maven + JDK 17（`17.0.17-amzn`）。
8. **CVE 排查方式**：✅ 不使用 SCA 工具，改以官方公告 + NVD/厂商联网检索逐条人工核实（本项目运行时依赖极薄）。
