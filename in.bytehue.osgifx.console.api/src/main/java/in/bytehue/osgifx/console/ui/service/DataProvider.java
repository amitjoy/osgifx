package in.bytehue.osgifx.console.ui.service;

import org.osgi.annotation.versioning.ProviderType;

import in.bytehue.osgifx.console.ui.dto.BundleFxDTO;
import in.bytehue.osgifx.console.ui.dto.ComponentFxDTO;
import in.bytehue.osgifx.console.ui.dto.ConfigurationFxDTO;
import in.bytehue.osgifx.console.ui.dto.EventFxDTO;
import in.bytehue.osgifx.console.ui.dto.PropertyFxDTO;
import in.bytehue.osgifx.console.ui.dto.ServiceFxDTO;
import javafx.collections.ObservableList;

/**
 * This service is responsible for providing the utility methods to retrieve data from the runtime
 * so that they can be shown on the UI
 */
@ProviderType
public interface DataProvider {

    /**
     * Returns the observable list of bundles
     *
     * @return the observable list of bundles
     */
    ObservableList<BundleFxDTO> bundles();

    /**
     * Returns the observable list of services
     *
     * @return the observable list of services
     */
    ObservableList<ServiceFxDTO> services();

    /**
     * Returns the observable list of components
     *
     * @return the observable list of components
     */
    ObservableList<ComponentFxDTO> components();

    /**
     * Returns the observable list of configurations
     *
     * @return the observable list of configurations
     */
    ObservableList<ConfigurationFxDTO> configurations();

    /**
     * Returns the observable list of events
     *
     * @return the observable list of events
     */
    ObservableList<EventFxDTO> events();

    /**
     * Returns the observable list of properties
     *
     * @return the observable list of properties
     */
    ObservableList<PropertyFxDTO> properties();

}
