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
package com.osgifx.console.ui.gogo;

import java.util.ArrayList;
import java.util.List;

public final class GogoStatementSplitter {

    private GogoStatementSplitter() {
    }

    public static List<GogoStatement> split(final String script) {
        if (script == null || script.trim().isEmpty()) {
            return List.of();
        }

        final var statements = new ArrayList<GogoStatement>();
        final var length     = script.length();

        var i             = 0;
        var stmtStart     = 0;
        var braceDepth    = 0;
        var parenDepth    = 0;
        var inSingleQuote = false;
        var inDoubleQuote = false;

        while (i < length) {
            final var ch = script.charAt(i);

            // Handle escape sequences inside quotes
            if ((inSingleQuote || inDoubleQuote) && ch == '\\' && i + 1 < length) {
                i += 2;
                continue;
            }

            // Toggle single quote
            if (ch == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                i++;
                continue;
            }

            // Toggle double quote
            if (ch == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                i++;
                continue;
            }

            // Skip content inside quotes
            if (inSingleQuote || inDoubleQuote) {
                i++;
                continue;
            }

            // Track braces and parentheses
            if (ch == '{') {
                braceDepth++;
            } else if (ch == '}') {
                braceDepth = Math.max(0, braceDepth - 1);
            } else if (ch == '(') {
                parenDepth++;
            } else if (ch == ')') {
                parenDepth = Math.max(0, parenDepth - 1);
            }

            // Statement boundary: newline or semicolon at top level
            final var isNewline   = ch == '\n';
            final var isSemicolon = ch == ';';

            if ((isNewline || isSemicolon) && braceDepth == 0 && parenDepth == 0) {
                final var text = script.substring(stmtStart, i).trim();
                if (!text.isEmpty() && !text.startsWith("#")) {
                    statements.add(new GogoStatement(stmtStart, i, text));
                }
                stmtStart = i + 1;
            }

            i++;
        }

        // Handle trailing statement without newline/semicolon
        if (stmtStart < length) {
            final var text = script.substring(stmtStart).trim();
            if (!text.isEmpty() && !text.startsWith("#")) {
                statements.add(new GogoStatement(stmtStart, length, text));
            }
        }

        return List.copyOf(statements);
    }

}
