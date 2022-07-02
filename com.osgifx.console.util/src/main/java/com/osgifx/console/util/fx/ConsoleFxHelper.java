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

    public static boolean validateTopic(final String topic) {
        if (topic.isBlank()) {
            return false;
        }
        final var alphabetLowerCaseMatcher = CharMatcher.inRange('a', 'z');
        final var alphabetUpperCaseMatcher = CharMatcher.inRange('A', 'Z');
        final var digitMatcher             = CharMatcher.inRange('0', '9');
        final var underscoreMatcher        = CharMatcher.is('_');
        final var slashMatcher             = CharMatcher.is('/');
        final var hyphenMatcher            = CharMatcher.is('-');
        final var wildcardMatcher          = CharMatcher.is('*');

        // @formatter:off
        final var valid = alphabetLowerCaseMatcher.or(alphabetUpperCaseMatcher)
                                                  .or(digitMatcher)
                                                  .or(underscoreMatcher)
                                                  .or(slashMatcher)
                                                  .or(hyphenMatcher)
                                                  .or(wildcardMatcher)
                                                  .matchesAllOf(topic);
        // @formatter:on
        if (!valid) {
            return false;
        }
        if (topic.contains("/")) {
            // topic cannot begin with /
            final var firstIndex = slashMatcher.indexIn(topic);
            if (firstIndex == 0) {
                return false;
            }
            // topic cannot end with / and must not have consecutive two /
            final var lastIndex = slashMatcher.lastIndexIn(topic);
            if (lastIndex == topic.charAt(topic.length() - 1) || topic.contains("//")) {
                return false;
            }
        }
        if (topic.contains("*")) {
            // check all indices of * as it can only be at the end
            final var count = wildcardMatcher.countIn(topic);
            // if there are more than 1 *, the topic is invalid
            if (count > 1) {
                return false;
            }
            final var index = wildcardMatcher.indexIn(topic);
            // if the * is not at the end, the topic is invalid and the character before *
            // must always be a / unless the topic has only a single * (length = 1)
            if (index != topic.length() - 1 || topic.length() > 1 && topic.charAt(index - 1) != '/') {
                return false;
            }
        }
        return true;
    }

}
