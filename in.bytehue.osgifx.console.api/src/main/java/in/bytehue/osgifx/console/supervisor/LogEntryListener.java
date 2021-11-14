package in.bytehue.osgifx.console.supervisor;

import in.bytehue.osgifx.console.agent.dto.XLogEntryDTO;

@FunctionalInterface
public interface LogEntryListener {
    void logged(XLogEntryDTO logEntry);
}
