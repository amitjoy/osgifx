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

import static com.google.common.base.Verify.verify;

import java.util.Formatter;
import java.util.List;
import java.util.regex.Pattern;

import com.google.common.collect.Lists;

public class VersionRange {

    final Version high;
    final Version low;
    char          start = '[';
    char          end   = ']';

    private static final Pattern RANGE = Pattern.compile(
            "(\\(|\\[)\\s*(" + Version.VERSION_STRING + ")\\s*,\\s*(" + Version.VERSION_STRING + ")\\s*(\\)|\\])");

    public VersionRange(String string) {
        string = string.trim();

        // If a range starts with @ then we make it a
        // a semantic import range

        var auto = 0;
        if (string.startsWith("@")) {
            string = string.substring(1);
            auto   = 1;                  // for consumers
        } else if (string.endsWith("@")) {
            string = string.substring(0, string.length() - 1);
            auto   = 2;                                       // for providers
        } else if (string.startsWith("=")) {
            string = string.substring(1);
            auto   = 3;
        }

        final var m = RANGE.matcher(string);
        if (m.matches()) {
            start = m.group(1).charAt(0);
            final var v1 = m.group(2);
            final var v2 = m.group(10);
            low  = new Version(v1);
            high = new Version(v2);
            end  = m.group(18).charAt(0);
            verify(low.compareTo(high) <= 0, "Low Range is higher than High Range: %s - %s", low, high);
        } else {
            final var v = new Version(string);
            if (auto == 3) {
                start = '[';
                end   = ']';
                low   = v;
                high  = v;
            } else if (auto != 0) {
                low   = v;
                high  = auto == 1 ? v.bumpMajor() : v.bumpMinor();
                start = '[';
                end   = ')';
            } else {
                low = high = v;
            }
        }
    }

    public VersionRange(final boolean b, final Version lower, final Version upper, final boolean c) {
        start = b ? '[' : '(';
        end   = c ? ']' : ')';
        low   = lower;
        high  = unique(upper);
    }

    public VersionRange(final String low, final String higher) {
        this(new Version(low), new Version(higher));
    }

    public VersionRange(final Version low, final Version higher) {
        this.low = low;
        high     = unique(higher);
        start    = '[';
        end      = this.low.equals(high) ? ']' : ')';
    }

    static Version unique(final Version v) {
        if (Version.HIGHEST.equals(v)) {
            return Version.HIGHEST;
        }
        if (Version.LOWEST.equals(v)) {
            return Version.LOWEST;
        }
        return v;
    }

    public boolean isRange() {
        return high != low;
    }

    public boolean includeLow() {
        return start == '[';
    }

    public boolean includeHigh() {
        return end == ']';
    }

    @Override
    public String toString() {
        if (high == Version.HIGHEST) {
            return low.toString();
        }
        final var sb = new StringBuilder();
        sb.append(start);
        sb.append(low);
        sb.append(',');
        sb.append(high);
        sb.append(end);
        return sb.toString();
    }

    public Version getLow() {
        return low;
    }

    public Version getHigh() {
        return high;
    }

    public boolean includes(final Version v) {
        if (!isRange()) {
            return low.compareTo(v) <= 0;
        }
        if (includeLow()) {
            if (v.compareTo(low) < 0) {
                return false;
            }
        } else if (v.compareTo(low) <= 0) {
            return false;
        }
        if (includeHigh()) {
            if (v.compareTo(high) > 0) {
                return false;
            }
        } else if (v.compareTo(high) >= 0) {
            return false;
        }
        return true;
    }

    public Iterable<Version> filter(final Iterable<Version> versions) {
        final List<Version> list = Lists.newArrayList();
        for (final Version v : versions) {
            if (includes(v)) {
                list.add(v);
            }
        }
        return list;
    }

    /**
     * Convert to an OSGi filter expression
     */
    public String toFilter() {
        return toFilter("version");
    }

    /**
     * Convert to an OSGi filter expression
     */
    public String toFilter(final String versionAttribute) {
        try (var f = new Formatter()) {
            if (high == Version.HIGHEST) {
                return "(" + versionAttribute + ">=" + low + ")";
            }
            if (isRange()) {
                f.format("(&");
                if (includeLow()) {
                    f.format("(%s>=%s)", versionAttribute, getLow());
                } else {
                    f.format("(!(%s<=%s))", versionAttribute, getLow());
                }
                if (includeHigh()) {
                    f.format("(%s<=%s)", versionAttribute, getHigh());
                } else {
                    f.format("(!(%s>=%s))", versionAttribute, getHigh());
                }
                f.format(")");
            } else {
                f.format("(%s>=%s)", versionAttribute, getLow());
            }
            return f.toString();
        }
    }

    public static boolean isVersionRange(final String stringRange) {
        return RANGE.matcher(stringRange).matches();
    }

    /**
     * Intersect two version ranges
     */
    public VersionRange intersect(final VersionRange other) {
        Version lower;
        var     start = this.start;

        final var lowc = low.compareTo(other.low);
        if (lowc <= 0) {
            lower = other.low;
            if (lowc != 0 || start == '[') {
                start = other.start;
            }
        } else {
            lower = low;
        }
        Version upper;
        var     end = this.end;

        final var highc = high.compareTo(other.high);
        if (highc >= 0) {
            upper = other.high;
            if (highc != 0 || end == ']') {
                end = other.end;
            }
        } else {
            upper = high;
        }
        return new VersionRange(start == '[', lower, upper, end == ']');
    }

    public static VersionRange parseVersionRange(final String version) {
        if (!isVersionRange(version)) {
            return null;
        }
        return new VersionRange(version);
    }

    public static VersionRange parseOSGiVersionRange(final String version) {
        if (Version.isVersion(version)) {
            return new VersionRange(new Version(version), Version.HIGHEST);
        }
        if (isVersionRange(version)) {
            return new VersionRange(version);
        }
        return null;
    }

    public static boolean isOSGiVersionRange(final String range) {
        return Version.isVersion(range) || isVersionRange(range);
    }

    public boolean isSingleVersion() {
        return high == Version.HIGHEST;
    }

    public static VersionRange likeOSGi(final String version) {
        if (version == null) {
            return new VersionRange(Version.LOWEST, Version.HIGHEST);
        }
        if (Version.isVersion(version)) {
            return new VersionRange(new Version(version), Version.HIGHEST);
        }
        if (isVersionRange(version)) {
            return new VersionRange(version);
        }
        return null;
    }
}
