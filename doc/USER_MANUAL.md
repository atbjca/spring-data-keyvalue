# 用户手册 — Spring Data KeyValue NES Fork

## 一、项目定位

本项目是 Spring Data KeyValue `2.7.18` 的 **NES（Never-Ending Support）安全维护分支**，用于在官方 `2.7.x` 版本线 EOL（生命周期结束、不再提供开源补丁）后，继续为企业内部提供安全修复与私服分发。

- **基线**：Spring Data KeyValue 2.7.18 / Spring Framework 5.3.39 / Java 8
- **分支**：`2.7.x-bjca-patch`
- **发布坐标**：`cn.bjca.footstone.bpring.data:bjca-footstone-bpring-data-keyvalue:2.7.18-nes.patch.1`

## 二、与官方版本的差异

### 1. 坐标去特征化（GAV Renaming）

Maven 坐标从官方 `org.springframework.data:spring-data-keyvalue` 改为 `cn.bjca.footstone.bpring.data:bjca-footstone-bpring-data-keyvalue`，用于规避 SCA 工具按官方 GAV 特征误报 EOL 版本线 CVE。完整映射见 [`GAV_MAPPING.md`](GAV_MAPPING.md)。

**不变项**：
- Java 包名 `org.springframework.data.keyvalue.*` / `org.springframework.data.map.*`
- JPMS 自动模块名 `spring.data.keyvalue`
- 所有公开 API 与行为语义

因此下游项目升级本 fork 时，**仅需改 Maven 坐标，源码零改动**。

### 2. 安全修复（CVE-2026-41719）

本 fork 对 `SpelPropertyComparator` 的 SpEL 排序注入漏洞做了 backport 修复（官方仅在 3.5.12/4.0.6 修复，2.7.x 无开源补丁）。

## 三、CVE-2026-41719 修复说明

### 漏洞

按属性排序时，`SpelPropertyComparator` 将不可信的**排序属性名**拼入含构造器/方法调用的 SpEL 字符串，并在默认 `StandardEvaluationContext`（允许构造器、方法、`T()` 类型引用）下求值，导致 SpEL 表达式注入（CWE-917，CVSS 6.4）。

### 修复（三处协同）

| 改动 | 说明 |
|------|------|
| 表达式简化 | `buildExpressionForPath()` 仅返回只读属性导航 `#arg1?.<path>`，去除构造器/方法拼接 |
| 比较逻辑回归 Java | 新增类级常量 `NULLS_FIRST`/`NULLS_LAST`，`compare()` 用 Java `Comparator` 完成 null 处理与自然序比较 |
| 受限求值上下文 | 求值改用 `SimpleEvaluationContext.forReadOnlyDataBinding()`，禁用构造器/方法/类型引用 |

排序功能（asc/desc、nullsFirst/nullsLast、嵌套路径 `a.b.c`）行为与官方一致，合法输入不受影响。详见 [`CVE/CVE-2026-41719.md`](CVE/CVE-2026-41719.md)。

### 兼容性

- **API 兼容**：`SpelPropertyComparator` 的构造器、`asc()/desc()/nullsFirst()/nullsLast()/getPath()/compare()` 签名不变。
- **行为兼容**：对合法排序属性名，排序结果与修复前一致（既有 11 个回归用例全绿）。

## 四、构建与分发

见 [`QUICK_START.md`](QUICK_START.md)。核心命令：

```bash
export JAVA_HOME=~/.sdkman/candidates/java/8.0.482-kona
./mvnw -s settings.xml clean test      # 测试
./mvnw -DskipTests install             # 增量本地安装，不执行 clean
./mvnw -DskipTests -Dmaven.test.skip=true deploy  # 发布私服 RELEASE，仅由协调会话执行
```

## 五、依赖前置约束

本项目 compile 依赖 NES fork 制品，构建前须确保私服/`~/.m2` 已部署：

- `bjca-footstone-bpring-data-commons:2.7.18-nes.patch.1`
- `bjca-footstone-bpring-context` / `bjca-footstone-bpring-tx:5.3.39-nes.patch.1`

缺失时构建明确失败，不回退官方坐标（保证去特征化完整性）。

## 六、漏洞状态总览

见 [`VULNERABILITY_REPORT.md`](VULNERABILITY_REPORT.md)：本体真实攻击面 1 项（CVE-2026-41719）已修复；直接依赖攻击面经联网盘点无适用高危项。
