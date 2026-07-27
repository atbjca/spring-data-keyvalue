# 快速入门 — Spring Data KeyValue NES Fork

## 1. 前置条件

| 项 | 要求 |
|----|------|
| JDK | Java 8（工具链 `8.0.482-kona`，sdkman 安装） |
| Maven | 3.6.3（或用项目自带 `./mvnw`） |
| 私服 | 可访问内网 Nexus（`~/.m2/settings.xml` 已配置镜像 `192.168.131.36:8088/repository/maven-public`） |
| **依赖前置** | 私服/`~/.m2` 须已存在以下 NES fork RELEASE 制品：<br>· `cn.bjca.footstone.bpring.data:bjca-footstone-bpring-data-commons:2.7.18-nes.patch.1`<br>· `cn.bjca.footstone.bpring:bjca-footstone-bpring-context:5.3.39-nes.patch.1`<br>· `cn.bjca.footstone.bpring:bjca-footstone-bpring-tx:5.3.39-nes.patch.1` |

> 若 commons fork 制品缺失，构建将**明确失败**（不回退官方坐标）。请先构建并部署 commons fork。

## 2. 引入依赖

```xml
<dependency>
    <groupId>cn.bjca.footstone.bpring.data</groupId>
    <artifactId>bjca-footstone-bpring-data-keyvalue</artifactId>
    <version>2.7.18-nes.patch.1</version>
</dependency>
```

> Java 代码 `import org.springframework.data.keyvalue.*` / `org.springframework.data.map.*` **无需改动**，仅 Maven 坐标变化。

## 3. 构建 / 测试

```bash
# 选定 Java 8 工具链
export JAVA_HOME=~/.sdkman/candidates/java/8.0.482-kona

# 构建 + 测试（走内网私服）
./mvnw -s settings.xml clean test

# 安装到本地仓库
./mvnw -DskipTests install
```

或使用 `Makefile` 快捷命令：

```bash
make test     # 构建并测试
make install  # 安装到本地
make deploy   # 发布到私服 RELEASE（仅由发布协调会话执行）
```

## 4. 验证安全修复

CVE-2026-41719（SpEL 排序注入）已修复。可运行安全回归用例确认：

```bash
./mvnw -s settings.xml -Dtest=SpelPropertyComparatorSecurityUnitTests test
```

用例断言：恶意排序属性名不会被当作可执行 SpEL 求值。详见 [`doc/CVE/CVE-2026-41719.md`](CVE/CVE-2026-41719.md)。
