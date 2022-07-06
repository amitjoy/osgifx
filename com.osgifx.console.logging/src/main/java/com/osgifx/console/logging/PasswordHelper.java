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
package com.osgifx.console.logging;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

import java.util.regex.Pattern;

public final class PasswordHelper {

    private static final Pattern PASSWORD_PATTERN     =                                                                 //
            Pattern.compile("([\"\']?)(password)([\"\']?)( *[=:] *)([\"\']?)[^\'\"}, ]*([\"\'}, ]?)", CASE_INSENSITIVE);
    private static final String  PASSWORD_REPLACEMENT = "$1$2$3$4$5*****$6";

    private PasswordHelper() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    /**
     * Masks the specified string in the region between start and end index
     *
     * @param strText  the string to be masked
     * @param start    the start index for masking
     * @param end      the end index for masking
     * @param maskChar the masking character
     *
     * @return the masked output
     *
     * @throws IllegalStateException if the start index is greater than end index
     */
    public static String mask(final String strText, int start, int end, final char maskChar) {
        if (strText == null || "".equals(strText)) {
            return "";
        }
        if (start < 0) {
            start = 0;
        }
        if (end > strText.length()) {
            end = strText.length();
        }
        if (start > end) {
            throw new IllegalArgumentException("End index cannot be greater than start index");
        }
        final var maskLength = end - start;
        if (maskLength == 0) {
            return strText;
        }
        final var sbMaskString = new StringBuilder(maskLength);
        for (var i = 0; i < maskLength; i++) {
            sbMaskString.append(maskChar);
        }
        return strText.substring(0, start) + sbMaskString.toString() + strText.substring(start + maskLength);
    }

    /**
     * Masks the specified string
     *
     * @param message the message
     * @return message with potentially filtered-out confidential data
     */
    public static String mask(final String message) {
        if (message == null) {
            return "";
        }
        return PASSWORD_PATTERN.matcher(message).replaceAll(PASSWORD_REPLACEMENT);
    }

}
