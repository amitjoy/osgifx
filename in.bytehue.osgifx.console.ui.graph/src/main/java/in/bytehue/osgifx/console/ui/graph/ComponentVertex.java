package in.bytehue.osgifx.console.ui.graph;

import java.util.Objects;

public final class ComponentVertex {

    public String name;

    public ComponentVertex(final String name) {
        this.name = name;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final ComponentVertex other = (ComponentVertex) obj;
        return Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name;
    }

}
