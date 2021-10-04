package in.bytehue.osgifx.console.application.addon;

import javax.annotation.PostConstruct;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.EventTopic;
import org.eclipse.e4.ui.workbench.IWorkbench;

public final class AgentDisconnectAddon {

    private static final String DISCONNECTION_TOPIC = "osgi/fx/console/disconnected";

    @PostConstruct
    public void listen(@Optional final IWorkbench workbench, @Optional @EventTopic(DISCONNECTION_TOPIC) final String diconnectionTime) {
        final boolean isOk = showDisconnectDialog();
        if (isOk) {
            workbench.close();
        }
    }

    private boolean showDisconnectDialog() {
        // TODO Auto-generated method stub
        return false;
    }

}
