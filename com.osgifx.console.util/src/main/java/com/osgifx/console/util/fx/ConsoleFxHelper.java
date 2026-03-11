/*******************************************************************************
 * Copyright 2021-2026 Amit Kumar Mondal
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

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.CharMatcher;

public final class ConsoleFxHelper {

    // Built once at class-load time; CharMatcher is immutable and thread-safe
    private static final CharMatcher SLASH_MATCHER     = CharMatcher.is('/');
    private static final CharMatcher WILDCARD_MATCHER  = CharMatcher.is('*');
    private static final CharMatcher VALID_TOPIC_CHARS = CharMatcher.inRange('a', 'z').or(CharMatcher.inRange('A', 'Z'))
            .or(CharMatcher.inRange('0', '9')).or(CharMatcher.is('_')).or(SLASH_MATCHER).or(CharMatcher.is('-'))
            .or(WILDCARD_MATCHER);

    private ConsoleFxHelper() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    /**
     * This is required as the agent can be disconnected and all invoked method
     * executions will return null thereafter
     */
    public static <T> List<T> makeNullSafe(final Collection<T> source) {
        if (source == null) {
            return List.of();
        }
        if (source instanceof List) {
            return (List<T>) source;
        }
        return List.copyOf(source);
    }

    public static boolean validateTopic(final String topic) {
        if (StringUtils.isBlank(topic)) {
            return false;
        }
        if (!VALID_TOPIC_CHARS.matchesAllOf(topic)) {
            return false;
        }
        if (topic.contains("/")) {
            // topic cannot begin with /
            if (SLASH_MATCHER.indexIn(topic) == 0) {
                return false;
            }
            // topic cannot end with / and must not have consecutive two /
            final var lastIndex = SLASH_MATCHER.lastIndexIn(topic);
            if (lastIndex == topic.charAt(topic.length() - 1) || topic.contains("//")) {
                return false;
            }
        }
        if (topic.contains("*")) {
            // if there are more than 1 *, the topic is invalid
            if (WILDCARD_MATCHER.countIn(topic) > 1) {
                return false;
            }
            final var index = WILDCARD_MATCHER.indexIn(topic);
            // if the * is not at the end, the topic is invalid and the character before *
            // must always be a / unless the topic has only a single * (length = 1)
            if (index != topic.length() - 1 || topic.length() > 1 && topic.charAt(index - 1) != '/') {
                return false;
            }
        }
        return true;
    }

}
