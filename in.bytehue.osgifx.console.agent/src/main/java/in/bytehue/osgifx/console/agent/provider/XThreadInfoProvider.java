package in.bytehue.osgifx.console.agent.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import in.bytehue.osgifx.console.agent.dto.XThreadDTO;

public final class XThreadInfoProvider {

    private XThreadInfoProvider() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static List<XThreadDTO> get() {
        final Map<Thread, StackTraceElement[]> threads    = Thread.getAllStackTraces();
        final List<Thread>                     threadList = new ArrayList<>(threads.keySet());
        return threadList.stream().map(XThreadInfoProvider::toDTO).collect(Collectors.toList());
    }

    private static XThreadDTO toDTO(final Thread thread) {
        final XThreadDTO dto = new XThreadDTO();

        dto.name          = thread.getName();
        dto.id            = thread.getId();
        dto.priority      = thread.getPriority();
        dto.state         = thread.getState().name();
        dto.isInterrupted = thread.isInterrupted();
        dto.isAlive       = thread.isAlive();
        dto.isDaemon      = thread.isDaemon();

        return dto;
    }

}
