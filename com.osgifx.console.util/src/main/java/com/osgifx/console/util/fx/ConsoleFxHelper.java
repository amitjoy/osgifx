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
package com.osgifx.console.util.fx;

import java.util.Collection;
import java.util.List;

import com.google.common.base.CharMatcher;

public final class ConsoleFxHelper {

	private ConsoleFxHelper() {
		throw new IllegalAccessError("Cannot be instantiated");
	}

	/**
	 * This is required as the agent can be disconnected and all invoked method
	 * executions will return null thereafter
	 */
	public static <T> Collection<T> makeNullSafe(final Collection<T> source) {
		if (source == null) {
			return List.of();
		}
		return source;
	}

	public static void validateTopic(final String topic) {
		final var chars  = topic.toCharArray();
		final var length = chars.length;
		if (length == 0) {
			return;
		}
		for (var i = 0; i < length; i++) {
			final var ch = chars[i];
			if (ch == '/') {
				// cannot start or end with a '/' but anywhere else is okay
				// cannot have "//" as that implies empty token
				if (i == 0 || i == length - 1 || chars[i - 1] == '/') {
					throw new IllegalArgumentException("Invalid topic: " + topic);
				}
				continue;
			}
			if ('A' <= ch && ch <= 'Z' || 'a' <= ch && ch <= 'z') {
				continue;
			}
			if ('0' <= ch && ch <= '9' || ch == '_' || ch == '-') {
				continue;
			}
			if (ch == '*') {
				continue;
			}
			throw new IllegalArgumentException("Invalid topic: " + topic);
		}
		if (topic.contains("*")) {
			// check all indices of * as it can only be at the end
			final var charMatcher = CharMatcher.is('*');
			final var count       = charMatcher.countIn(topic);
			// if there are more than 1 *, it is invalid
			if (count > 1) {
				throw new IllegalArgumentException("Invalid topic: " + topic);
			}
			final var index = charMatcher.indexIn(topic);
			// if the * is not at the end, it is invalid and the character before * must
			// always be a / unless the topic has only a single * (length = 1)
			if (index != topic.length() - 1 || topic.length() > 1 && topic.charAt(index - 1) != '/') {
				throw new IllegalArgumentException("Invalid topic: " + topic);
			}
		}
	}

}
