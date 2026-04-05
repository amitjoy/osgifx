package com.osgifx.console.ui.graph.rsa;

import java.util.Objects;

public final class RsaVertex {

    private final String uuid;

    public RsaVertex(final String uuid) {
        this.uuid = uuid;
    }

    public String uuid() {
        return uuid;
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
        return uuid;
    }
}
