/*******************************************************************************
 * Copyright 2021-2024 Amit Kumar Mondal
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
package com.osgifx.console.ui.bundles.obr.bnd;

import java.util.Collection;

public class TypedAttribute {

    public final String value;
    public final String type;

    public TypedAttribute(final String type, final String value) {
        this.type  = "String".equals(type) ? null : type;
        this.value = value;
    }

    public static TypedAttribute getTypedAttribute(final Object value) {
        if (value instanceof Collection) {
            final Collection<?> c = (Collection<?>) value;
            if (c.isEmpty()) {
                return null;
            }

            final var sb      = new StringBuilder();
            var       del     = "";
            String    subType = null;

            for (final Object v : c) {
                if (subType == null) {
                    subType = getType(v);
                }
                sb.append(del).append(escape(v.toString()));
                del = ",";
            }
            if (subType == null) {
                subType = "String";
            }
            return new TypedAttribute("List<" + subType + ">", sb.toString());
        }

        if (value.getClass().isArray()) {
            final var array = (Object[]) value;
            if (array.length == 0) {
                return null;
            }
            final var sb      = new StringBuilder();
            var       del     = "";
            String    subType = null;

            for (final Object v : array) {
                if (subType == null) {
                    subType = getType(v);
                }
                sb.append(del).append(escape(v.toString()));
                del = ",";
            }
            if (subType == null) {
                subType = "String";
            }

            return new TypedAttribute("List<" + subType + ">", sb.toString());
        }

        return new TypedAttribute(getType(value), value.toString());
    }

    private static Object escape(final String v) {
        final var sb = new StringBuilder();
        for (var i = 0; i < v.length(); i++) {
            final var c = v.charAt(i);
            switch (c) {
                case '\\':
                    sb.append("\\\\");
                    break;
                case ',':
                    sb.append("\\,");
                default:
                    sb.append(c);
                    break;
            }
        }
        return sb.toString();
    }

    private static String getType(final Object value) {
        if (value instanceof Long || value instanceof Integer || value instanceof Short || value instanceof Byte) {
            return "Long";
        }
        if (value instanceof Double || value instanceof Float) {
            return "Double";
        }
        if (value instanceof Version || value instanceof org.osgi.framework.Version) {
            return "Version";
        }
        return "String";
    }
}
