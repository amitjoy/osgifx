package in.bytehue.osgifx.console.agent.provider;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogService;

import in.bytehue.osgifx.console.agent.dto.XLogEntryDTO;
import in.bytehue.osgifx.console.supervisor.Supervisor;

public class OSGiLogListener implements LogListener {

    private final Supervisor supervisor;

    public OSGiLogListener(final Supervisor supervisor) {
        this.supervisor = supervisor;
    }

    @Override
    public void logged(final LogEntry entry) {
        if (supervisor != null) {
            supervisor.logged(toDTO(entry));
        }
    }

    private XLogEntryDTO toDTO(final LogEntry entry) {
        final XLogEntryDTO dto = new XLogEntryDTO();

        dto.bundle    = XBundleAdmin.toDTO(entry.getBundle());
        dto.message   = entry.getMessage();
        dto.level     = toLevel(entry.getLevel());
        dto.exception = toExceptionString(entry.getException());
        dto.loggedAt  = entry.getTime();

        return dto;
    }

    private String toExceptionString(final Throwable exception) {
        if (exception == null) {
            return null;
        }
        final StringWriter sw = new StringWriter();
        exception.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private String toLevel(final int level) {
        switch (level) {
            case LogService.LOG_WARNING:
                return "WARNING";
            case LogService.LOG_DEBUG:
                return "DEBUG";
            case LogService.LOG_ERROR:
                return "ERROR";
            case LogService.LOG_INFO:
            default:
                return "INFO";
        }
    }

}
