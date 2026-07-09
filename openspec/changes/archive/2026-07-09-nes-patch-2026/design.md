# Design — nes-patch-2026

> 本文档记录 spring-data-keyvalue-2.7 NES fork 改造的技术设计与关键决策。
> 遵循 TDD：本体 CVE 修复先写触发用例，再改业务代码。

## 1. 背景与约束

| 项 | 值 |
|----|-----|
| 基线版本 | 上游 `2.7.19-SNAPSHOT`；改造后锚定已发布正式版 `2.7.18` 为基线与 parent 版本 |
| 目标版本 | `2.7.18-nes.patch.1-SNAPSHOT` |
| 构建工具 | Maven（`mvnw`，本项目自带 wrapper） |
| 私服 | 内网 Nexus（release / snapshot / 聚合 maven-public） |
| Git 分支 | `2.7.x-bjca-patch` |
| Java | **硬约束：产物必须兼容 Java 8**。工具链 `8.0.482-kona`（sdkman）。修复/测试代码只用 Java 8 API 与语法（禁 var、List.of、Map.of 等 Java 9+） |

**核心约束**：官方对 `CVE-2026-41719` 仅在 3.5.12 / 4.0.6 修复，**2.7.x 属 EOL，无开源补丁**。本 fork 即承担 NES 角色，将官方补丁 backport 到 2.7.x。

**与下游 NES fork 的本质区别**：`spring-boot` / `spring-kafka` 等下游项目对本 CVE 标 `⬜免疫`（不携带漏洞代码）；本项目是**漏洞宿主本体**，攻击面真实存在于自身 `org.springframework.data.keyvalue.core.SpelPropertyComparator`，必须实修。这是本 change 区别于兄弟 fork 的核心价值。

## 2. 本体 CVE 修复方案（核心）

### 2.1 攻击面：SpEL 排序注入（CVE-2026-41719）

| 项 | 值 |
|----|-----|
| 编号 | CVE-2026-41719 |
| 类型 | SpEL 表达式注入（CWE-917） |
| CVSS | 6.4 MEDIUM（`AV:N/AC:H/PR:L/UI:N/S:U/C:H/I:L/A:L`） |
| 受影响 | spring-data-keyvalue 2.7.0 – 2.7.19 |
| 官方修复 | 3.5.12 / 4.0.6 |
| backport 参照 | 本地 `spring-data-keyvalue-3.5`（`3.5.14-SNAPSHOT`，已含官方修复），逐行比对 |

**数据流**：

```
   不可信排序输入（Sort 属性名 path）
        │
        ▼
   SpelPropertyComparator(path, parser)
        │
        ▼  buildExpressionForPath()  ← 【注入点】path 直接拼入 SpEL 字符串
   "new ...NullSafeComparator(new ...ComparableComparator(), <nullsFirst>)
        .compare(#arg1?.<path>, #arg2?.<path>)"
        │
        ▼  parser.parseRaw(...)  (SpelPropertyComparator.java:105)
        │
        ▼  compare() → expression.getValue(Integer.class)
           使用默认 StandardEvaluationContext（可调用构造器 / 方法 / 类型引用）
        │
        ▼  恶意 path 如  name].compare(...) + T(java.lang.Runtime)...
           被作为可执行 SpEL 求值 → 任意方法/类型调用
```

根因两处（`SpelPropertyComparator.java`）：
1. **`buildExpressionForPath()`（:116-122）**：把 `path` 直接 `String.format` 拼进含 `new ...Comparator(...)` 的 SpEL 字符串——属性路径与可执行构造器混在同一表达式。
2. **`compare()`（:130-137）+ `getExpression()`（:104-105）**：用 `SpelExpressionParser.parseRaw()` 解析，并在**默认求值上下文**（`StandardEvaluationContext`）中 `getValue`——该上下文允许构造器、方法调用、`T()` 类型引用。

### 2.2 官方修复设计（3.5 已验证，逐行 backport）

三处协同改动，将「表达式承载比较逻辑」改为「表达式只做属性导航、比较逻辑回归 Java」：

**① 表达式简化——只保留属性导航**

```java
// 修复前（2.7 漏洞版）
protected String buildExpressionForPath() {
    String rawExpression = String.format(
        "new org.springframework.util.comparator.NullSafeComparator(new org.springframework.util.comparator.ComparableComparator(), %s).compare(#arg1?.%s,#arg2?.%s)",
        Boolean.toString(this.nullsFirst), path.replace(".", "?."), path.replace(".", "?."));
    return rawExpression;
}

// 修复后（3.5 参照）—— 表达式仅剩属性导航，无任何可执行构造器/方法
protected String buildExpressionForPath() {
    return String.format("#arg1?.%s", path.replace(".", "?."));
}
```

**② 比较逻辑移回 Java 侧常量比较器**

```java
// 3.5 参照：类级常量，替代原先拼进 SpEL 的 NullSafeComparator/ComparableComparator
private static final Comparator<?> NULLS_FIRST = Comparator.nullsFirst(Comparator.naturalOrder());
private static final Comparator<?> NULLS_LAST  = Comparator.nullsLast(Comparator.naturalOrder());
```

`compare()` 改为：分别用简化表达式对 `arg1` / `arg2` 求出属性值，再用上面的 Java `Comparator` 比较（含 asc/desc、nullsFirst/nullsLast 语义）。

**③ 受限求值上下文——切断注入危害**

```java
// 3.5 参照：每次求值用只读数据绑定上下文，禁用构造器/方法/类型引用
private @Nullable Object getValue(@Nullable T arg) {
    SpelExpression expressionToUse = getExpression();
    SimpleEvaluationContext ctx = SimpleEvaluationContext.forReadOnlyDataBinding().build();
    ctx.setVariable("arg1", arg);
    expressionToUse.setEvaluationContext(ctx);
    return expressionToUse.getValue();
}
```

即便攻击者构造恶意 `path`，`SimpleEvaluationContext.forReadOnlyDataBinding()` 也只允许属性读取，不解析构造器 / 方法 / `T()`，注入无法落地。**双重防御**（表达式无害化 + 上下文受限）与官方一致。

### 2.3 backport 范围最小化

| 类 | 2.7 vs 3.5 差异 | 是否改动 |
|----|----------------|:---:|
| `SpelPropertyComparator` | 实质安全改动（上述三处） | ✅ **改** |
| `SpelSortAccessor` | 仅 license / 断言文案 / javadoc 差异，无实质安全改动 | ❌ 不改（除非回归需要） |
| `SpelQueryEngine` | 无实质安全改动 | ❌ 不改 |
| `PathSortAccessor` | 3.5 后期新增的无关架构 | ❌ 不引入 |

**性能重构**（常量化比较器、去反射实例化）随①②自然带入，属安全必需改动的副产物，不额外扩大改动面。

### 2.4 状态映射（初判，实现后回填）

| CVE | 攻击面存在 | 计划状态 |
|-----|:---:|:---:|
| CVE-2026-41719 | ✅ SpelPropertyComparator | 🔧修复中 → ✅已修复 |

## 3. GAV 去特征化设计

| 坐标 | 原值 | 新值 |
|------|------|------|
| groupId | `org.springframework.data` | `cn.bjca.footstone.bpring.data` |
| artifactId | `spring-data-keyvalue` | `bjca-footstone-bpring-data-keyvalue` |
| version | `2.7.19-SNAPSHOT` | `2.7.18-nes.patch.1-SNAPSHOT` |
| java-module-name | `spring.data.keyvalue` | 保持不变（JPMS 模块名，改动影响下游 `requires`） |

依赖坐标联动（详见 `specs/gav-renaming`）：

| 依赖 | 新坐标 |
|------|--------|
| spring-data-commons | `cn.bjca.footstone.bpring.data:bjca-footstone-bpring-data-commons:2.7.18-nes.patch.1-SNAPSHOT` |
| spring-context | `cn.bjca.footstone.bpring:bjca-footstone-bpring-context:5.3.39-nes.patch.1-SNAPSHOT` |
| spring-tx | `cn.bjca.footstone.bpring:bjca-footstone-bpring-tx:5.3.39-nes.patch.1-SNAPSHOT` |
| querydsl-collections / joda-time | 保持上游（第三方，不去特征化） |

**Parent 处理 —— 方案 A（保留原 parent 坐标，版本锁 2.7.18 正式版）**：

```
✅ 方案 A：保留 org.springframework.data.build:spring-data-parent，版本 2.7.18
   方案 B：同步 fork spring-data-build 去特征化 ──（未采用）工作量翻倍
   方案 C：扁平化去 parent                      ──（未采用）
```

```xml
<parent>
  <groupId>org.springframework.data.build</groupId>
  <artifactId>spring-data-parent</artifactId>
  <version>2.7.18</version>   <!-- 原为 2.7.19-SNAPSHOT，改为已发布正式版 -->
  <relativePath/>
</parent>
```

理由：parent 只在构建期解析、不进发布制品 GAV 特征（SCA 扫的是制品自身坐标，已去特征化）；引用已发布 `2.7.18` 正式版彻底消除 SNAPSHOT 父 POM 私服解析不到的风险；`~/.m2` 已确认可解析。

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

`${nexusReleaseUrl}` / `${nexusSnapshotUrl}` 由 `~/.m2/settings.xml` profile 提供；项目内 `settings.xml` 对齐 server 凭证 id = `releases` / `snapshots`。

## 5. 变更影响分析（Impact Analysis）

| 受影响对象 | 变更 | 风险 | 缓解 |
|-----------|------|------|------|
| `SpelPropertyComparator` | 三处安全改动（表达式简化 / Java 比较器 / 受限上下文） | 破坏正常按属性排序语义 | 逐行参照 3.5 已验证实现；现有 `SpelPropertyComperatorUnitTests` 全绿 + 新增注入用例 |
| `pom.xml` GAV | 坐标重命名 | 下游解析失败 | GAV_MAPPING 文档 + 保留包名/模块名 |
| parent 引用 | 版本 `2.7.19-SNAPSHOT` → `2.7.18` | 私服解析父 POM | 方案 A，锁正式版（`~/.m2` 已确认可解析） |
| 依赖坐标 | commons/context/tx 换 NES 坐标 | 兄弟 fork 制品缺失导致解析失败 | 构建前先部署 commons fork；缺失时显式失败不回退官方坐标 |
| 下游依赖方 | 需换 GAV | 构建中断 | 文档说明 + 版本对照表 |

**构建依赖顺序**（关键风险）：本项目 compile 依赖 `bjca-footstone-bpring-data-commons`，该制品当前不在 `~/.m2`。**必须先构建并部署 commons fork**，否则本项目无法解析依赖。

## 6. 测试策略（TDD + 覆盖率 ≥60%）

- **红 → 绿**：先提交能证明注入可执行的失败用例（恶意 `path` 触发方法调用），再提交修复使其变绿（注入被阻断）。
- **回归**：`SpelPropertyComperatorUnitTests` 及派生排序测试（合法属性路径、嵌套 `a.b.c`、asc/desc、nullsFirst/nullsLast）全量通过，行为与修复前对合法输入一致。
- **覆盖率**：`SpelPropertyComparator` 行覆盖 ≥60%（jacoco 或等价）。
- **构建验证**：`./mvnw -s settings.xml clean install` 从私服解析依赖成功。

## 7. 决策记录（已拍板，2026-07-08）

1. **变更范围**：✅ 全量对齐 commons（6 能力：cve-assessment / cve-remediation / cve-documentation / gav-renaming / nexus-config / build-documentation）。
2. **GAV 命名**：✅ `cn.bjca.footstone.bpring.data` / `bjca-footstone-bpring-data-keyvalue` / `2.7.18-nes.patch.1-SNAPSHOT`。
3. **Parent 处理**：✅ 方案 A（保留原坐标），版本 `2.7.19-SNAPSHOT` → 已发布正式版 `2.7.18`（不 fork `spring-data-build`）。
4. **CVE 修复手段**：✅ 逐行 backport 本地 3.5（`3.5.14-SNAPSHOT`）已验证实现——表达式简化为 `#arg1?.<path>` + `SimpleEvaluationContext.forReadOnlyDataBinding` + Java 侧常量比较器，**不预设、不臆测**（已核实源码，避免知识幻觉）。
5. **修复范围**：✅ 仅 `SpelPropertyComparator`；不改无实质差异的 `SpelSortAccessor` / `SpelQueryEngine`，不引入 `PathSortAccessor`。
6. **CVE 排查方式**：✅ 不使用 SCA 工具，改以官方公告 + NVD/厂商联网检索逐条人工核实。本项目运行时依赖极薄，核心真实攻击面为自身 SpEL 代码。
7. **工具链**：✅ Maven + Java 8（`8.0.482-kona`）。
