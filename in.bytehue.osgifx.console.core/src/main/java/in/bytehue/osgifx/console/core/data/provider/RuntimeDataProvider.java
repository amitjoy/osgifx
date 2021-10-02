package in.bytehue.osgifx.console.core.data.provider;

import org.osgi.service.component.annotations.Component;

import in.bytehue.osgifx.console.ui.dto.BundleFxDTO;
import in.bytehue.osgifx.console.ui.dto.ComponentFxDTO;
import in.bytehue.osgifx.console.ui.dto.ConfigurationFxDTO;
import in.bytehue.osgifx.console.ui.dto.EventFxDTO;
import in.bytehue.osgifx.console.ui.dto.PropertyFxDTO;
import in.bytehue.osgifx.console.ui.dto.ServiceFxDTO;
import in.bytehue.osgifx.console.ui.service.DataProvider;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

@Component
public final class RuntimeDataProvider implements DataProvider {

    private final ObservableList<BundleFxDTO>        bundles        = FXCollections.observableArrayList();
    private final ObservableList<ServiceFxDTO>       services       = FXCollections.observableArrayList();
    private final ObservableList<ComponentFxDTO>     components     = FXCollections.observableArrayList();
    private final ObservableList<ConfigurationFxDTO> configurations = FXCollections.observableArrayList();
    private final ObservableList<EventFxDTO>         events         = FXCollections.observableArrayList();
    private final ObservableList<PropertyFxDTO>      properties     = FXCollections.observableArrayList();

    public RuntimeDataProvider() {
        for (int i = 1; i < 101; i++) {
            final BundleFxDTO dto1 = new BundleFxDTO();
            dto1.setStatus("ACTIVE");
            dto1.setSymbolicName("com.amitinside.1");
            dto1.setVersion("1.2.3");

            bundles.add(dto1);
        }
    }

    @Override
    public ObservableList<BundleFxDTO> bundles() {
        return bundles;
    }

    @Override
    public ObservableList<ServiceFxDTO> services() {
        return services;
    }

    @Override
    public ObservableList<ComponentFxDTO> components() {
        return components;
    }

    @Override
    public ObservableList<ConfigurationFxDTO> configurations() {
        return configurations;
    }

    @Override
    public ObservableList<EventFxDTO> events() {
        return events;
    }

    @Override
    public ObservableList<PropertyFxDTO> properties() {
        return properties;
    }

}
