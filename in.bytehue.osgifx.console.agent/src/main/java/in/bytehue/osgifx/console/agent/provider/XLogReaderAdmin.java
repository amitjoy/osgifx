package in.bytehue.osgifx.console.agent.provider;

import static java.util.Objects.requireNonNull;

import org.osgi.service.log.LogReaderService;

public final class XLogReaderAdmin {

    private XLogReaderAdmin() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static void register(final Object service, final OSGiLogListener logListener) {
        requireNonNull(service);
        requireNonNull(logListener);
        if (service instanceof LogReaderService) {
            ((LogReaderService) service).addLogListener(logListener);
        }
    }

    public static void unregister(final Object service, final OSGiLogListener logListener) {
        requireNonNull(service);
        requireNonNull(logListener);
        if (service instanceof LogReaderService) {
            ((LogReaderService) service).removeLogListener(logListener);
        }
    }

}
