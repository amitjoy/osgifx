package in.bytehue.osgifx.console.application.preference;

import java.util.List;

import org.osgi.service.component.annotations.Component;

import in.bytehue.osgifx.console.application.dialog.ConnectionSettingDTO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

@Component(service = ConnectionsProvider.class)
public final class ConnectionsProvider {

    private final ObservableList<ConnectionSettingDTO> connections = FXCollections.observableArrayList();

    public synchronized void addConnection(final ConnectionSettingDTO connection) {
        connections.add(connection);
    }

    public synchronized void removeConnection(final ConnectionSettingDTO connection) {
        connections.remove(connection);
    }

    public synchronized void addConnections(final List<ConnectionSettingDTO> connections) {
        this.connections.addAll(connections);
    }

    public synchronized ObservableList<ConnectionSettingDTO> getConnections() {
        return connections;
    }

}
