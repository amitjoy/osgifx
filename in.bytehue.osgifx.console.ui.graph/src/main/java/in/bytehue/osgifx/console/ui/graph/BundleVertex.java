package in.bytehue.osgifx.console.ui.graph;

import java.util.Objects;

public final class BundleVertex {

    public String symbolicName;
    public long   id;

    public BundleVertex(final String symbolicName, final long id) {
        this.symbolicName = symbolicName;
        this.id           = id;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final BundleVertex other = (BundleVertex) obj;
        return Objects.equals(symbolicName, other.symbolicName) && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbolicName, id);
    }

    @Override
    public String toString() {
        return symbolicName + ":" + id;
    }

}
