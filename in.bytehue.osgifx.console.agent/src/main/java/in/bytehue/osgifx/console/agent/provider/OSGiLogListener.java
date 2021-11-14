package in.bytehue.osgifx.console.agent.provider;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;

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

        dto.bundle     = XBundleAdmin.toDTO(entry.getBundle());
        dto.message    = entry.getMessage();
        dto.level      = entry.getLogLevel().name();
        dto.exception  = toExceptionString(entry.getException());
        dto.loggedAt   = entry.getTime();
        dto.threadInfo = entry.getThreadInfo();
        dto.logger     = entry.getLoggerName();

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

}
