/*******************************************************************************
 * Copyright 2021-2022 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.util.converter;

import java.util.List;
import java.util.stream.Stream;

import org.osgi.util.converter.Converter;
import org.osgi.util.converter.ConverterBuilder;
import org.osgi.util.converter.Converters;
import org.osgi.util.converter.Rule;
import org.osgi.util.converter.TypeReference;

import com.google.common.primitives.Floats;
import com.osgifx.console.agent.dto.XAttributeDefType;

public final class ValueConverter {

	private final Converter converter;

	public ValueConverter() {
		final var c  = Converters.standardConverter();
		final var cb = c.newConverterBuilder();

		initRules(cb);
		converter = cb.build();
	}

	private void initRules(final ConverterBuilder cb) {
		cb.rule(new Rule<String, String[]>(v -> v.split(",")) {
		});
		cb.rule(new Rule<String, List<String>>(v -> Stream.of(v.split(",")).toList()) {
		});
		cb.rule(new Rule<String, int[]>(v -> Stream.of(v.split(",")).mapToInt(Integer::parseInt).toArray()) {
		});
		cb.rule(new Rule<String, List<Integer>>(v -> Stream.of(v.split(",")).map(Integer::parseInt).toList()) {
		});
		cb.rule(new Rule<String, boolean[]>(v -> {
			final var split = v.split(",");
			final var array = new boolean[split.length];
			for (var i = 0; i < split.length; i++) {
				final var value = Boolean.parseBoolean(split[i]);
				array[i] = value;
			}
			return array;
		}) {
		});
		cb.rule(new Rule<String, List<Boolean>>(v -> Stream.of(v.split(",")).map(Boolean::parseBoolean).toList()) {
		});
		cb.rule(new Rule<String, double[]>(v -> Stream.of(v.split(",")).mapToDouble(Double::parseDouble).toArray()) {
		});
		cb.rule(new Rule<String, List<Double>>(v -> Stream.of(v.split(",")).map(Double::parseDouble).toList()) {
		});
		cb.rule(new Rule<String, float[]>(v -> {
			final var elements = Stream.of(v.split(",")).map(Double::parseDouble).toList();
			return Floats.toArray(elements);
		}) {
		});
		cb.rule(new Rule<String, List<Float>>(v -> Stream.of(v.split(",")).map(Float::parseFloat).toList()) {
		});
		cb.rule(new Rule<String, char[]>(v -> {
			final var split = v.split(",");
			final var array = new char[split.length];
			for (var i = 0; i < split.length; i++) {
				final var value = split[i].charAt(0);
				array[i] = value;
			}
			return array;
		}) {
		});
		cb.rule(new Rule<String, List<Character>>(v -> Stream.of(v.split(",")).map(e -> e.charAt(0)).toList()) {
		});
		cb.rule(new Rule<String, long[]>(v -> Stream.of(v.split(",")).mapToLong(Long::parseLong).toArray()) {
		});
		cb.rule(new Rule<String, List<Long>>(v -> Stream.of(v.split(",")).map(Long::parseLong).toList()) {
		});
	}

	public Object convert(final Object value, final XAttributeDefType target) {
		return switch (target) {
		case STRING_ARRAY -> converter.convert(value).to(String[].class);
		case STRING_LIST -> converter.convert(value).to(new TypeReference<List<String>>() {
		});
		case INTEGER_ARRAY -> converter.convert(value).to(int[].class);
		case INTEGER_LIST -> converter.convert(value).to(new TypeReference<List<Integer>>() {
		});
		case BOOLEAN_ARRAY -> converter.convert(value).to(boolean[].class);
		case BOOLEAN_LIST -> converter.convert(value).to(new TypeReference<List<Boolean>>() {
		});
		case DOUBLE_ARRAY -> converter.convert(value).to(double[].class);
		case DOUBLE_LIST -> converter.convert(value).to(new TypeReference<List<Double>>() {
		});
		case FLOAT_ARRAY -> converter.convert(value).to(float[].class);
		case FLOAT_LIST -> converter.convert(value).to(new TypeReference<List<Float>>() {
		});
		case CHAR_ARRAY -> converter.convert(value).to(char[].class);
		case CHAR_LIST -> converter.convert(value).to(new TypeReference<List<Character>>() {
		});
		case LONG_ARRAY -> converter.convert(value).to(long[].class);
		case LONG_LIST -> converter.convert(value).to(new TypeReference<List<Long>>() {
		});
		default -> converter.convert(value).to(XAttributeDefType.clazz(target));
		};
	}

	public <T> T convert(final Object value, final Class<T> clazz) {
		return converter.convert(value).to(clazz);
	}

	public <T> T convert(final Object value, final TypeReference<T> ref) {
		return converter.convert(value).to(ref);
	}

}
