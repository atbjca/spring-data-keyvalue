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

import java.util.Comparator;

import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link Comparator} implementation using {@link SpelExpression}.
 *
 * @author Christoph Strobl
 * @author Oliver Gierke
 * @author Mark Paluch
 * @param <T>
 */
public class SpelPropertyComparator<T> implements Comparator<T> {

	// CVE-2026-41719（SpEL 注入 / CWE-917）安全加固——比较逻辑回归 Java 侧常量比较器。
	// 修复前将 NullSafeComparator/ComparableComparator 以「构造器调用」形式拼进 SpEL 字符串，
	// 与不可信属性路径同处一个可执行表达式；此处改由 Java 常量承载 null 处理与自然序比较，
	// SpEL 仅保留只读属性导航。参照本地 spring-data-keyvalue-3.5（3.5.14）官方修复。
	private static final Comparator<?> NULLS_FIRST = Comparator.nullsFirst(Comparator.naturalOrder());
	private static final Comparator<?> NULLS_LAST = Comparator.nullsLast(Comparator.naturalOrder());

	private final String path;
	private final SpelExpressionParser parser;

	private boolean asc = true;
	private boolean nullsFirst = true;
	private @Nullable SpelExpression expression;

	/**
	 * Create new {@link SpelPropertyComparator} for the given property path an {@link SpelExpressionParser}.
	 *
	 * @param path must not be {@literal null} or empty.
	 * @param parser must not be {@literal null}.
	 */
	public SpelPropertyComparator(String path, SpelExpressionParser parser) {

		Assert.hasText(path, "Path must not be null or empty!");
		Assert.notNull(parser, "SpelExpressionParser must not be null!");

		this.path = path;
		this.parser = parser;
	}

	/**
	 * Sort {@literal ascending}.
	 *
	 * @return
	 */
	public SpelPropertyComparator<T> asc() {
		this.asc = true;
		return this;
	}

	/**
	 * Sort {@literal descending}.
	 *
	 * @return
	 */
	public SpelPropertyComparator<T> desc() {
		this.asc = false;
		return this;
	}

	/**
	 * Sort {@literal null} values first.
	 *
	 * @return
	 */
	public SpelPropertyComparator<T> nullsFirst() {
		this.nullsFirst = true;
		return this;
	}

	/**
	 * Sort {@literal null} values last.
	 *
	 * @return
	 */
	public SpelPropertyComparator<T> nullsLast() {
		this.nullsFirst = false;
		return this;
	}

	/**
	 * Parse values to {@link SpelExpression}
	 *
	 * @return
	 */
	protected SpelExpression getExpression() {

		if (this.expression == null) {
			this.expression = parser.parseRaw(buildExpressionForPath());
		}

		return this.expression;
	}

	/**
	 * Create the expression raw value.
	 *
	 * @return
	 */
	protected String buildExpressionForPath() {

		// CVE-2026-41719 安全加固——表达式仅保留只读属性导航（如 #arg1?.a?.b?.c），
		// 不再拼接任何 new ...Comparator(...) 构造器与 compare(...) 方法调用。
		// 即便攻击者传入恶意「属性名」，也只会被解析为属性导航路径，配合下方受限求值上下文彻底切断注入。
		return String.format("#arg1?.%s", path.replace(".", "?."));
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public int compare(T arg1, T arg2) {

		// CVE-2026-41719 安全加固——先分别用「只读属性导航」表达式求出两侧属性值，
		// 再交由 Java 侧常量比较器完成 null 处理与自然序比较（保留 asc/desc、nullsFirst/nullsLast 语义），
		// 比较逻辑不再依赖 SpEL 表达式内的可执行调用。
		Object value1 = getValue(arg1);
		Object value2 = getValue(arg2);

		return ((Comparator<Object>) (nullsFirst ? NULLS_FIRST : NULLS_LAST)).compare(value1, value2) * (asc ? 1 : -1);
	}

	/**
	 * 使用受限的只读求值上下文对单个入参求出目标属性值。
	 * <p>
	 * CVE-2026-41719 安全加固核心：改用 {@link SimpleEvaluationContext#forReadOnlyDataBinding()} 替换默认的
	 * {@code StandardEvaluationContext}。只读数据绑定上下文仅允许属性读取，禁用构造器调用、方法调用与
	 * {@code T()} 类型引用，使恶意 SpEL 无法落地执行。
	 *
	 * @param arg 待求值对象，可为 {@literal null}。
	 * @return 目标属性值，可能为 {@literal null}。
	 */
	private @Nullable Object getValue(@Nullable T arg) {

		SpelExpression expressionToUse = getExpression();

		SimpleEvaluationContext ctx = SimpleEvaluationContext.forReadOnlyDataBinding().build();
		ctx.setVariable("arg1", arg);

		expressionToUse.setEvaluationContext(ctx);

		return expressionToUse.getValue();
	}

	/**
	 * Get dot path to property.
	 *
	 * @return
	 */
	public String getPath() {
		return path;
	}
}
