package in.bytehue.osgifx.console.application.handler;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.extensions.Preference;
import org.osgi.service.prefs.BackingStoreException;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import in.bytehue.osgifx.console.application.dialog.ConnectionSettingDTO;
import in.bytehue.osgifx.console.application.preference.ConnectionsProvider;

public final class ConnectionPreferenceHandler {

    @Inject
    @Preference(nodePath = "osgi.fx.connections")
    private IEclipsePreferences preferences;

    @Inject
    private ConnectionsProvider connectionsProvider;

    @PostConstruct
    public void init() {
        connectionsProvider.addConnections(getStoredValues());
    }

    @Execute
    public void execute(@Named("host") final String host, @Named("port") final String port, @Named("timeout") final String timeout)
            throws BackingStoreException {
        final Gson                       gson        = new Gson();
        final List<ConnectionSettingDTO> connections = getStoredValues();
        final ConnectionSettingDTO       dto         = new ConnectionSettingDTO(host, Integer.parseInt(port), Integer.parseInt(timeout));

        connections.add(dto);
        preferences.put("settings", gson.toJson(connections));
        preferences.flush();

        connectionsProvider.addConnection(dto);
    }

    private List<ConnectionSettingDTO> getStoredValues() {
        final Gson                 gson        = new Gson();
        final String               storedValue = preferences.get("settings", "");
        List<ConnectionSettingDTO> connections = gson.fromJson(storedValue, new TypeToken<List<ConnectionSettingDTO>>() {
                                               }.getType());
        if (connections == null) {
            connections = new ArrayList<>();
        }
        return connections;
    }

}
