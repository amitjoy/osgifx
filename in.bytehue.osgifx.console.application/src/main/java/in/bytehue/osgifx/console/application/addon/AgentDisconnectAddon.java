package in.bytehue.osgifx.console.application.addon;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.EventTopic;

import javafx.application.Platform;

public final class AgentDisconnectAddon {

    private static final String DISCONNECTION_TOPIC = "osgi/fx/console/disconnected";

    public void listen(@Optional @EventTopic(DISCONNECTION_TOPIC) final String diconnectionTime) {
        final boolean isOk = showDisconnectDialog();
        if (isOk) {
            Platform.exit();
        }
    }

    private boolean showDisconnectDialog() {
        // TODO Auto-generated method stub
        return false;
    }

}
