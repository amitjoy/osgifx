package com.osgifx.console.ui.graph.rsa;

import java.util.Objects;

public final class RsaVertex {

    private final String uuid;
    private final String label;

    public RsaVertex(final String uuid, final String label) {
        this.uuid  = uuid;
        this.label = label;
    }

    public String uuid() {
        return uuid;
    }

    public String label() {
        return label;
    }

    public String toDotID() {
        return uuid.replace("-", ""); // DOT IDs usually don't like hyphens
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final RsaVertex other = (RsaVertex) obj;
        return Objects.equals(uuid, other.uuid);
    }

    @Override
    public String toString() {
        return label;
    }

    /**
     * Shortens a UUID to first 8 characters for display
     */
    public static String shortenUUID(final String uuid) {
        if (uuid == null || uuid.length() <= 8) {
            return uuid;
        }
        return uuid.substring(0, 8) + "...";
    }
}
