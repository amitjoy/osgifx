package in.bytehue.osgifx.console.util.fx;

import java.util.Collections;
import java.util.List;

public final class ConsoleFXHelper {

    private ConsoleFXHelper() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    /**
     * This is required as the agent can be disconnected and all invoked method
     * executions will return null thereafter
     */
    public static <T> List<T> makeNullSafe(final List<T> source) {
        if (source == null) {
            return Collections.emptyList();
        }
        return source;
    }

}
