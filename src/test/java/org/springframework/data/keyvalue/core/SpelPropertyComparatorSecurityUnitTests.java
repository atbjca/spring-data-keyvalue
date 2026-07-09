/*
 * Copyright 2014-2023 the original author or authors.
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * 针对 {@link SpelPropertyComparator} 的安全回归测试（CVE-2026-41719）。
 * <p>
 * 该 CVE 属 SpEL 表达式注入（CWE-917）：修复前 {@code buildExpressionForPath()} 将不可信的排序属性名
 * 直接拼入含 {@code new ...Comparator(...)} 的 SpEL 字符串，并在默认 {@code StandardEvaluationContext}
 * 下求值——该上下文允许构造器 / 方法调用 / {@code T()} 类型引用，导致攻击者可借恶意「属性名」执行任意 SpEL。
 * <p>
 * 本用例构造恶意排序属性名，令表达式在求值时触发一次<strong>可观测且无害</strong>的副作用
 * （写入一个自定义系统属性）。断言逻辑：
 * <ul>
 *     <li>修复前（漏洞版）——副作用会被执行，系统属性被写入 → 断言失败（红）；</li>
 *     <li>修复后——表达式仅做只读属性导航（{@code SimpleEvaluationContext.forReadOnlyDataBinding()}），
 *         方法调用 / 类型引用被禁用，副作用绝不会发生 → 系统属性保持为空（绿）。</li>
 * </ul>
 * 说明：此处刻意选用「写系统属性」这类无害副作用作为「代码被执行」的证据，绝不使用
 * {@code Runtime.exec} 等真实危险载荷。参照本地 {@code spring-data-keyvalue-3.5}（{@code 3.5.14}）官方修复。
 *
 * @author BJCA Footstone NES
 */
class SpelPropertyComparatorSecurityUnitTests {

	private static final SpelExpressionParser PARSER = new SpelExpressionParser();

	/** 用于探测「表达式是否被当作可执行代码求值」的系统属性键。 */
	private static final String PROBE_KEY = "nesInjectionProbe";

	/** 触发副作用的探测值。 */
	private static final String PROBE_VALUE = "pwned";

	private static final SomeType ONE = new SomeType("one", 1, 1);
	private static final SomeType TWO = new SomeType("two", 2, 2);

	@BeforeEach
	@AfterEach
	void clearProbe() {
		// 每个用例前后都清理探测系统属性，避免用例间互相污染
		System.clearProperty(PROBE_KEY);
	}

	@Test // CVE-2026-41719
	void shouldNotEvaluateMaliciousSortPropertyAsExecutableSpel() {

		// 恶意排序「属性名」：闭合掉正常属性导航后，借 T(System) 类型引用调用方法写入系统属性。
		// 注意 buildExpressionForPath() 会把 '.' 全部替换为 '?.'（安全导航），故此处点号被替换后
		// 仍是合法 SpEL（如 T(System)?.getProperties()?.put(...)），不影响副作用触发能力。
		String maliciousPath = "stringProperty == T(System).getProperties().put('" + PROBE_KEY + "','" + PROBE_VALUE
				+ "')";

		Comparator<SomeType> comparator = new SpelPropertyComparator<>(maliciousPath, PARSER);

		// 修复后：只读上下文会拒绝 T()/方法调用，compare 可能抛异常，这是「安全失败」，忽略之。
		// 关键断言是：无论 compare 结果如何，恶意副作用绝不能发生。
		try {
			comparator.compare(ONE, TWO);
		} catch (RuntimeException expectedAfterFix) {
			// 只读上下文拒绝方法/类型引用而抛出属预期，不做处理
		}

		assertThat(System.getProperty(PROBE_KEY))
				.as("恶意排序属性名不得被当作可执行 SpEL 求值（CVE-2026-41719）")
				.isNull();
	}

	@SuppressWarnings("WeakerAccess")
	public static class SomeType {

		String stringProperty;
		Integer integerProperty;
		int primitiveProperty;

		public SomeType() {}

		SomeType(String stringProperty, Integer integerProperty, int primitiveProperty) {
			this.stringProperty = stringProperty;
			this.integerProperty = integerProperty;
			this.primitiveProperty = primitiveProperty;
		}

		public String getStringProperty() {
			return stringProperty;
		}

		public Integer getIntegerProperty() {
			return integerProperty;
		}

		public int getPrimitiveProperty() {
			return primitiveProperty;
		}
	}
}
