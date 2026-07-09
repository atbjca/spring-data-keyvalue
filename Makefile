.PHONY: clean build test install deploy security help

# =============================================================================
# Spring Data KeyValue 3.5.x BJCA/NES 维护分支 — 构建快捷命令（Maven）
# =============================================================================
# 本项目为 Maven 工程，基线 3.5.13，编译目标 Java 17。
# 私服依赖解析与发布配置（mirror、bjca profile 属性、snapshots 凭证）全部由
# Maven 默认用户级配置 ~/.m2/settings.xml 提供，且 bjca profile 默认激活，
# 因此不显式传 -s。如需覆盖，命令行传 SETTINGS='-s <路径>' 即可。

SHELL := /bin/bash
MVNW  := ./mvnw
SETTINGS ?=

help:
	@echo ""
	@echo "可用命令:"
	@echo "  make clean    - 清理构建产物（target）"
	@echo "  make build    - 编译打包（跳过测试）"
	@echo "  make test     - 运行测试"
	@echo "  make install  - 安装到本地 Maven 仓库（~/.m2），跳过测试"
	@echo "  make deploy   - 发布到 Nexus 私服，跳过测试"
	@echo "  make security - 运行本体 SpEL 安全回归测试（CVE-2026-41719）"
	@echo ""

# 清理构建产物
clean:
	$(MVNW) $(SETTINGS) clean

# 编译打包：跳过测试，用于日常编译验证
build:
	$(MVNW) $(SETTINGS) -DskipTests clean package

# 运行测试
test:
	$(MVNW) $(SETTINGS) test

# 安装到本地仓库（跳过测试）
install:
	$(MVNW) $(SETTINGS) -DskipTests clean install

# 发布到 Nexus 私服（跳过测试）
deploy:
	$(MVNW) $(SETTINGS) -DskipTests clean deploy

# 本体安全回归：验证 CVE-2026-41719（SpEL 排序注入）官方修复生效
security:
	$(MVNW) $(SETTINGS) test -Dtest='SpelPropertyComparatorSecurityUnitTests,SpelPropertyComparatorUnitTests'
