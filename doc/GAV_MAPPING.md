# GAV 映射表 — Spring Data KeyValue NES Fork

本 fork 将官方 Maven 坐标去特征化，以规避 SCA 工具按官方 GAV 特征误报 CVE。**Java 包名与 JPMS 模块名保持不变**，下游 `import` / `requires` 无需修改。

## 本项目主坐标

| 坐标 | 原值（上游） | NES fork 值 |
|------|-------------|------------|
| groupId | `org.springframework.data` | `cn.bjca.footstone.bpring.data` |
| artifactId | `spring-data-keyvalue` | `bjca-footstone-bpring-data-keyvalue` |
| version | `2.7.19-SNAPSHOT` | `2.7.18-nes.patch.1` |
| Automatic-Module-Name | `spring.data.keyvalue` | `spring.data.keyvalue`（不变） |

## Parent

| 坐标 | 值 | 说明 |
|------|-----|------|
| groupId | `org.springframework.data.build` | 保留官方（不 fork `spring-data-build`） |
| artifactId | `spring-data-parent` | 保留官方 |
| version | `2.7.18` | 锚定上游正式版（原 `2.7.19-SNAPSHOT`）；`<relativePath/>` 置空 |

> parent 仅构建期解析、不进发布制品的运行时 GAV 特征，无需去特征化。

## 依赖坐标映射

| 依赖 | 原坐标 | NES fork 坐标 | 作用域 |
|------|--------|--------------|--------|
| spring-data-commons | `org.springframework.data:spring-data-commons` | `cn.bjca.footstone.bpring.data:bjca-footstone-bpring-data-commons:2.7.18-nes.patch.1` | compile |
| spring-context | `org.springframework:spring-context` | `cn.bjca.footstone.bpring:bjca-footstone-bpring-context:5.3.39-nes.patch.1` | compile |
| spring-tx | `org.springframework:spring-tx` | `cn.bjca.footstone.bpring:bjca-footstone-bpring-tx:5.3.39-nes.patch.1` | compile |
| querydsl-collections | `com.querydsl:querydsl-collections:5.0.0` | 保持上游不变（第三方，不去特征化） | optional |
| joda-time | `joda-time:joda-time` | 保持上游不变（第三方，仅测试） | test |

> 传递依赖 `spring-core`/`spring-beans`/`spring-expression`/`spring-aop`/`spring-jcl` 由 commons fork 与 context/tx fork 统一带入 fork 坐标 `cn.bjca.footstone.bpring:bjca-footstone-bpring-*:5.3.39-nes.patch.1`，classpath 中不再残留官方运行时坐标。

## 版本命名规则

RELEASE 使用 `X.Y.Z-nes.patch.N`：
- `X.Y.Z` 锚定上游最后正式发布号（本项目 `2.7.18`）
- `nes.patch.N` 为 NES 补丁序号
- data 线各 fork（keyvalue / commons）版本号保持一致
- 下一开发版本的 `-SNAPSHOT` 提升必须通过独立变更完成。
