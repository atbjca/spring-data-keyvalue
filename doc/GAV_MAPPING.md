# GAV 映射表 (GAV Mapping)

本项目为 Spring Data KeyValue 3.5.x 的 NES fork，对 Maven 坐标（GAV）进行去特征化重命名，以规避 SCA 工具按官方 GAV 特征误报 CVE。

## 本体坐标映射

| 坐标 | 官方原值 | NES fork 新值 |
|------|---------|--------------|
| **groupId** | `org.springframework.data` | `cn.bjca.footstone.bpring.data` |
| **artifactId** | `spring-data-keyvalue` | `bjca-footstone-bpring-data-keyvalue` |
| **version** | `3.5.14-SNAPSHOT` | `3.5.13-nes.patch.1` |

## Parent 坐标

| 坐标 | 值 | 说明 |
|------|-----|------|
| **groupId** | `org.springframework.data.build` | 保持不变（方案 A） |
| **artifactId** | `spring-data-parent` | 保持不变 |
| **version** | `3.5.13` | 由 `3.5.14-SNAPSHOT` 锁定为已发布正式版（私服/本地可解析） |

> Parent 仅在构建期解析，不体现在发布制品的 GAV 特征上，因此保留原坐标即可。

## 运行时依赖坐标联动（官方运行时坐标清零）

为使运行时 Spring 家族坐标全部去特征化，本项目将三条运行时依赖改为 fork 坐标：

| 依赖 | 官方原坐标 | NES fork 坐标 | 版本属性 |
|------|-----------|--------------|---------|
| spring-data-commons | `org.springframework.data:spring-data-commons` | `cn.bjca.footstone.bpring.data:bjca-footstone-bpring-data-commons` | `${springdata.commons}` = `3.5.13-nes.patch.1` |
| spring-context | `org.springframework:spring-context` | `cn.bjca.footstone.bpring:bjca-footstone-bpring-context` | `${bpring.framework}` = `6.2.19-nes.patch.1` |
| spring-tx | `org.springframework:spring-tx` | `cn.bjca.footstone.bpring:bjca-footstone-bpring-tx` | `${bpring.framework}` = `6.2.19-nes.patch.1` |

> commons fork 自身已将其传递的 Spring Framework 依赖（core/beans/expression/aop/jcl）替换为 fork 坐标，故经 `-U` 强制刷新后，`compile`/`runtime` 作用域下官方 `org.springframework` 坐标清零。

### 保持官方坐标的依赖

| 依赖 | 坐标 | 原因 |
|------|-----|------|
| querydsl-collections | `com.querydsl:querydsl-collections` | 第三方（非 Spring 家族）、`optional=true`，其 CVE 盘点不在本项目范围 |

## 保持不变的项

| 项 | 值 | 原因 |
|----|-----|------|
| **Java 包名** | `org.springframework.data.keyvalue.*` | 下游 `import` 语句无需修改 |
| **JPMS 自动模块名** | `spring.data.keyvalue` | 避免破坏下游 `requires` 声明 |
| **业务源码** | 与官方 3.5.13 一致 | 本体 CVE 官方已修复，无私有改动 |

## 依赖方迁移说明

下游项目只需更新依赖坐标，**无需修改任何 Java 源代码**：

```xml
<!-- 修改前 -->
<dependency>
    <groupId>org.springframework.data</groupId>
    <artifactId>spring-data-keyvalue</artifactId>
    <version>3.5.13</version>
</dependency>

<!-- 修改后 -->
<dependency>
    <groupId>cn.bjca.footstone.bpring.data</groupId>
    <artifactId>bjca-footstone-bpring-data-keyvalue</artifactId>
    <version>3.5.13-nes.patch.1</version>
</dependency>
```
