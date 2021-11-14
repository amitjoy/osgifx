package in.bytehue.osgifx.console.supervisor;

import in.bytehue.osgifx.console.agent.dto.XEventDTO;

@FunctionalInterface
public interface EventListener {
    void onEvent(XEventDTO event);
}
