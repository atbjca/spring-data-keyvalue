/*
 * Copyright 2014-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.keyvalue.core;

import static org.assertj.core.api.Assertions.*;

import java.util.Comparator;

import org.junit.jupiter.api.Test;

import org.springframework.expression.EvaluationException;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * 针对 {@link SpelPropertyComparator} 的安全回归测试（CVE-2026-41719 —— SpEL 排序注入）。
 *
 * <p>
 * 背景：历史漏洞版本将排序属性路径 {@code path} 直接拼入含构造器/方法调用的 SpEL 表达式，并在默认的
 * {@code StandardEvaluationContext} 下求值，导致攻击者可通过构造恶意 {@code path} 触发任意方法调用或类型引用
 * （SpEL 表达式注入，CWE-917）。官方已于 3.5.12 修复，本基线源码已包含该修复：
 * <ol>
 * <li>表达式简化为仅属性导航 {@code #arg1?.<path>}；</li>
 * <li>比较逻辑回归 Java 侧常量比较器；</li>
 * <li>求值改用 {@link org.springframework.expression.spel.support.SimpleEvaluationContext#forReadOnlyDataBinding()
 * 只读数据绑定上下文}，禁用方法调用与类型引用。</li>
 * </ol>
 *
 * <p>
 * 本测试类仅用于「固化」官方修复效果，不修改被测类。断言：当恶意 {@code path} 试图触发方法调用时，受限求值上下文
 * 将拒绝执行（抛出 {@link EvaluationException}），注入无法落地。
 *
 * @author BJCA NES Fork
 */
class SpelPropertyComparatorSecurityUnitTests {

	private static final SpelExpressionParser PARSER = new SpelExpressionParser();

	private static final Payload LEFT = new Payload("a");
	private static final Payload RIGHT = new Payload("b");

	@Test // CVE-2026-41719
	void blocksMethodInvocationInjectionViaMaliciousPath() {

		// 恶意排序属性名：试图在排序路径中调用方法（getClass().getName()）。
		// 修复前会在默认上下文中被当作可执行 SpEL 求值；修复后受限上下文禁用方法调用。
		Comparator<Payload> comparator = new SpelPropertyComparator<>("getClass().getName()", PARSER);

		// 受限上下文（forReadOnlyDataBinding）拒绝方法调用，注入被阻断，抛出 EvaluationException。
		assertThatExceptionOfType(EvaluationException.class)
				.isThrownBy(() -> comparator.compare(LEFT, RIGHT));
	}

	@Test // CVE-2026-41719
	void legitimatePropertyPathStillComparesCorrectly() {

		// 回归：合法属性路径的排序语义不受安全修复影响，行为与官方一致。
		Comparator<Payload> comparator = new SpelPropertyComparator<>("value", PARSER);
		assertThat(comparator.compare(LEFT, RIGHT)).isEqualTo("a".compareTo("b"));
	}

	/**
	 * 测试用简单载荷类型（仅暴露一个可读属性 {@code value}）。
	 */
	@SuppressWarnings("WeakerAccess")
	public static class Payload {

		private String value;

		public Payload() {}

		Payload(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}
}
