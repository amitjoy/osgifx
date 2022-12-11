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
package com.osgifx.console.ui.bundles.obr.bnd;

import java.util.regex.Pattern;

public class Version implements Comparable<Version> {

    private static final String TOKEN       = "[-\\w]+";
    private static final String HIGHESTCHAR = "\uFFFF";
    final int                   major;
    final int                   minor;
    final int                   micro;
    final String                qualifier;
    final boolean               snapshot;

    public static final String  VERSION_STRING = "(\\d{1,10})(\\.(\\d{1,10})(\\.(\\d{1,10})(\\.(" + TOKEN + "))?)?)?";
    public static final Pattern VERSION        = Pattern.compile(VERSION_STRING);
    public static final Version LOWEST         = new Version();
    public static final Version HIGHEST        = new Version(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE,
                                                             HIGHESTCHAR);

    public static final Version emptyVersion = LOWEST;
    public static final Version ONE          = new Version(1, 0, 0);
    public static final Pattern SNAPSHOT_P   = Pattern.compile("(.*-)?SNAPSHOT$");

    public Version() {
        this(0);
    }

    public Version(final int major, final int minor, final int micro, final String qualifier) {
        this.major     = major;
        this.minor     = minor;
        this.micro     = micro;
        this.qualifier = qualifier;
        snapshot       = isSnapshot(qualifier);
    }

    public Version(final int major, final int minor, final int micro) {
        this(major, minor, micro, null);
    }

    public Version(final int major, final int minor) {
        this(major, minor, 0, null);
    }

    public Version(final int major) {
        this(major, 0, 0, null);
    }

    public Version(String version) {
        version = version.trim();
        final var m = VERSION.matcher(version);
        if (!m.matches()) {
            throw new IllegalArgumentException("Invalid syntax for version: " + version);
        }

        major = Integer.parseInt(m.group(1));
        if (m.group(3) != null) {
            minor = Integer.parseInt(m.group(3));
        } else {
            minor = 0;
        }

        if (m.group(5) != null) {
            micro = Integer.parseInt(m.group(5));
        } else {
            micro = 0;
        }

        qualifier = m.group(7);
        snapshot  = isSnapshot(qualifier);
    }

    private boolean isSnapshot(final String qualifier) {
        return qualifier != null && qualifier != HIGHESTCHAR && SNAPSHOT_P.matcher(qualifier).matches();
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getMicro() {
        return micro;
    }

    public String getQualifier() {
        return qualifier;
    }

    @Override
    public int compareTo(final Version other) {
        if (other == this) {
            return 0;
        }

        final var o   = other;
        var       cmp = major - o.major;
        if (cmp != 0) {
            return cmp;
        }

        cmp = minor - o.minor;
        if (cmp != 0) {
            return cmp;
        }

        cmp = micro - o.micro;
        if (cmp != 0) {
            return cmp;
        }

        if (qualifier != null) {
            cmp = 1;
        }
        if (o.qualifier != null) {
            cmp += 2;
        }

        switch (cmp) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return -1;
        }
        return qualifier.compareTo(o.qualifier);
    }

    @Override
    public String toString() {
        final var sb = new StringBuilder();
        sb.append(major).append('.').append(minor).append('.').append(micro);
        if (qualifier != null) {
            sb.append('.').append(qualifier);
        }
        return sb.toString();
    }

    String toMavenString() {
        final var sb = new StringBuilder();
        sb.append(major).append('.').append(minor).append('.').append(micro);
        if (qualifier != null) {
            sb.append('-').append(qualifier);
        }
        return sb.toString();
    }

    public String toStringWithoutQualifier() {
        final var sb = new StringBuilder();
        sb.append(major).append('.').append(minor).append('.').append(micro);
        return sb.toString();
    }

    @Override
    public boolean equals(final Object ot) {
        if (!(ot instanceof Version)) {
            return false;
        }
        return compareTo((Version) ot) == 0;
    }

    @Override
    public int hashCode() {
        return major * 97 ^ minor * 13 ^ micro + (qualifier == null ? 97 : qualifier.hashCode());
    }

    public int get(final int i) {
        return switch (i) {
            case 0 -> major;
            case 1 -> minor;
            case 2 -> micro;
            default -> throw new IllegalArgumentException("Version can only get 0 (major), 1 (minor), or 2 (micro)");
        };
    }

    public static Version parseVersion(final String version) {
        if (version == null) {
            return LOWEST;
        }

        return valueOf(version);
    }

    public static Version valueOf(String version) {
        version = version.trim();
        if (version.isEmpty()) {
            return LOWEST;
        }

        return new Version(version);
    }

    public Version getWithoutQualifier() {
        if (qualifier == null) {
            return this;
        }
        return new Version(major, minor, micro);
    }

    public static boolean isVersion(final String version) {
        return version != null && VERSION.matcher(version).matches();
    }

    public boolean isSnapshot() {
        return snapshot;
    }

    public Version bumpMajor() {
        return new Version(major + 1);
    }

    public Version bumpMinor() {
        return new Version(major, minor + 1);
    }
}
