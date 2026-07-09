.PHONY: clean build test install deploy security help

# =============================================================================
# Spring Data KeyValue 2.7.x BJCA/NES 维护分支 — 构建快捷命令（Maven）
# =============================================================================
# 本项目为 Maven 工程，编译产物字节码目标为 Java 8。
# 工具链要求 Java 8（sdkman: 8.0.482-kona）：
#   export JAVA_HOME=~/.sdkman/candidates/java/8.0.482-kona
# 私服依赖解析与发布配置（mirror、nexus profile 属性、snapshots 凭证）由用户级
# ~/.m2/settings.xml 提供（含内网 Nexus 镜像 192.168.131.36:8088）。
# 项目内 settings.xml 是 Spring 官方原版（仅含 Spring Artifactory 认证），
# 对内网 Nexus 场景无用；如需显式指定，命令行传 SETTINGS='-s <路径>' 即可。

SHELL := /bin/bash
MVNW  := ./mvnw
# 默认使用用户级 ~/.m2/settings.xml（含内网 Nexus 镜像）
SETTINGS ?= -s $(HOME)/.m2/settings.xml

help:
	@echo ""
	@echo "可用命令:"
	@echo "  make clean     - 清理构建产物（target）"
	@echo "  make build     - 编译打包（跳过测试）"
	@echo "  make test      - 运行测试"
	@echo "  make security  - 仅运行 CVE-2026-41719 安全回归用例"
	@echo "  make install   - 安装到本地 Maven 仓库（~/.m2），跳过测试"
	@echo "  make deploy    - 发布到 Nexus 私服 snapshot，跳过测试"
	@echo ""
	@echo "提示: 需先 export JAVA_HOME=~/.sdkman/candidates/java/8.0.482-kona"
	@echo ""

# 清理构建产物
clean:
	$(MVNW) $(SETTINGS) clean

# 编译打包：跳过测试，用于日常编译验证
build:
	$(MVNW) $(SETTINGS) -DskipTests clean package

# 运行全部测试
test:
	$(MVNW) $(SETTINGS) test

# 仅运行 CVE-2026-41719 SpEL 注入安全回归用例
security:
	$(MVNW) $(SETTINGS) -Dtest=SpelPropertyComparatorSecurityUnitTests test

# 安装到本地仓库（跳过测试）
install:
	$(MVNW) $(SETTINGS) -DskipTests clean install

# 发布到 Nexus 私服 snapshot（跳过测试）
deploy:
	$(MVNW) $(SETTINGS) -DskipTests clean deploy
