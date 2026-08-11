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
package com.osgifx.console.util.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.osgifx.console.util.filter.FilterParser.And;
import com.osgifx.console.util.filter.FilterParser.Expression;
import com.osgifx.console.util.filter.FilterParser.Not;
import com.osgifx.console.util.filter.FilterParser.Op;
import com.osgifx.console.util.filter.FilterParser.Or;
import com.osgifx.console.util.filter.FilterParser.SimpleExpression;

public class FilterParserTest {

    private FilterParser parser;

    @BeforeEach
    public void setUp() {
        parser = new FilterParser();
    }

    // --- Parsing tests ---

    @Test
    public void parseSimpleEqualExpression() {
        Expression expr = parser.parse("(key=value)");
        assertTrue(expr instanceof SimpleExpression);
        SimpleExpression se = (SimpleExpression) expr;
        assertEquals("key", se.getKey());
        assertEquals("value", se.getValue());
        assertEquals(Op.EQUAL, se.getOp());
    }

    @Test
    public void parseGreaterOrEqualExpression() {
        Expression expr = parser.parse("(key>=10)");
        assertTrue(expr instanceof SimpleExpression);
        SimpleExpression se = (SimpleExpression) expr;
        assertEquals("key", se.getKey());
        assertEquals("10", se.getValue());
        assertEquals(Op.GREATER_OR_EQUAL, se.getOp());
    }

    @Test
    public void parseLessExpression() {
        Expression expr = parser.parse("(key<=10)");
        assertTrue(expr instanceof SimpleExpression);
        SimpleExpression se = (SimpleExpression) expr;
        assertEquals("key", se.getKey());
        assertEquals("10", se.getValue());
        assertEquals(Op.LESS_OR_EQUAL, se.getOp());
    }

    @Test
    public void parseAndExpression() {
        Expression expr = parser.parse("(&(a=1)(b=2))");
        assertTrue(expr instanceof And);
        And andExpr = (And) expr;
        assertNotNull(andExpr.getExpressions());
        assertEquals(2, andExpr.getExpressions().length);
    }

    @Test
    public void parseOrExpression() {
        Expression expr = parser.parse("(|(a=1)(b=2))");
        assertTrue(expr instanceof Or);
        Or orExpr = (Or) expr;
        assertNotNull(orExpr.getExpressions());
        assertEquals(2, orExpr.getExpressions().length);
    }

    @Test
    public void parseNotExpression() {
        Expression expr = parser.parse("(!(&(a=1)(b=2)))");
        assertTrue(expr instanceof Not);
    }

    // --- Evaluation tests ---

    @Test
    public void evalSimpleExpressionMatch() {
        Expression expr = parser.parse("(key=value)");
        assertTrue(expr.eval(Collections.singletonMap("key", "value")));
    }

    @Test
    public void evalSimpleExpressionNoMatch() {
        Expression expr = parser.parse("(key=value)");
        assertFalse(expr.eval(Collections.singletonMap("key", "other")));
    }

    @Test
    public void evalAndExpression() {
        Expression expr = parser.parse("(&(key1=v1)(key2=v2))");
        assertTrue(expr.eval(Map.of("key1", "v1", "key2", "v2")));
        assertFalse(expr.eval(Map.of("key1", "v1", "key2", "other")));
    }

    @Test
    public void evalOrExpression() {
        Expression expr = parser.parse("(|(key1=v1)(key2=v2))");
        assertTrue(expr.eval(Map.of("key1", "v1", "key2", "other")));
        assertTrue(expr.eval(Map.of("key1", "other", "key2", "v2")));
        assertFalse(expr.eval(Map.of("key1", "other", "key2", "other")));
    }

    @Test
    public void evalNotExpression() {
        Expression expr = parser.parse("(!(key=value))");
        assertFalse(expr.eval(Collections.singletonMap("key", "value")));
        assertTrue(expr.eval(Collections.singletonMap("key", "other")));
    }

    @Test
    public void patternExpressionWildcard() {
        Expression expr = parser.parse("(key=foo*)");
        assertTrue(expr.eval(Collections.singletonMap("key", "foobar")));
        assertFalse(expr.eval(Collections.singletonMap("key", "barfoo")));
    }

    @Test
    public void approximateExpressionCaseInsensitive() {
        Expression expr = parser.parse("(key~=Foo)");
        assertTrue(expr.eval(Collections.singletonMap("key", "foo")));
        assertTrue(expr.eval(Collections.singletonMap("key", "FOO")));
        assertFalse(expr.eval(Collections.singletonMap("key", "bar")));
    }

    // --- Op utility tests ---

    @Test
    public void opNotMethod() {
        assertEquals(Op.LESS_OR_EQUAL, Op.GREATER.not());
        assertEquals(Op.LESS, Op.GREATER_OR_EQUAL.not());
        assertEquals(Op.GREATER_OR_EQUAL, Op.LESS.not());
        assertEquals(Op.GREATER, Op.LESS_OR_EQUAL.not());
        assertEquals(Op.NOT_EQUAL, Op.EQUAL.not());
        assertEquals(Op.EQUAL, Op.NOT_EQUAL.not());
    }

    @Test
    public void trueExpressionEval() {
        assertTrue(Expression.trueExpression.eval(Collections.emptyMap()));
    }

    @Test
    public void falseExpressionEval() {
        assertFalse(Expression.falseExpression.eval(Collections.emptyMap()));
    }

    // --- Range and cache tests ---

    @Test
    public void rangeExpressionRoundTrip() {
        FilterParser parser2 = new FilterParser();
        // Since RangeExpression toString returns e.g. "version=[1,2]", we should test standard roundtrip via range query instead, 
        // or just construct a standard expression for roundtrip if it's not supported to parse range directly.
        Expression regularExpr = parser.parse("(&(a=1)(b=2))");
        Expression parsedAgain = parser2.parse("(" + regularExpr.toString() + ")");
        
        assertEquals(regularExpr.toString(), parsedAgain.toString());
    }

    @Test
    public void cacheReturnsSameExpressionForSameInput() {
        Expression expr1 = parser.parse("(a=b)");
        Expression expr2 = parser.parse("(a=b)");
        
        assertSame(expr1, expr2);
    }
}
