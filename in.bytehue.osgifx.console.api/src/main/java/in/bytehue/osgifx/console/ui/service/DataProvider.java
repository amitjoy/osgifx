package in.bytehue.osgifx.console.ui.service;

import org.osgi.annotation.versioning.ProviderType;

import in.bytehue.osgifx.console.agent.dto.XBundleDTO;
import in.bytehue.osgifx.console.agent.dto.XComponentDTO;
import in.bytehue.osgifx.console.agent.dto.XConfigurationDTO;
import in.bytehue.osgifx.console.agent.dto.XEventDTO;
import in.bytehue.osgifx.console.agent.dto.XPropertyDTO;
import in.bytehue.osgifx.console.agent.dto.XServiceDTO;
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
    ObservableList<XBundleDTO> bundles();

    /**
     * Returns the observable list of services
     *
     * @return the observable list of services
     */
    ObservableList<XServiceDTO> services();

    /**
     * Returns the observable list of components
     *
     * @return the observable list of components
     */
    ObservableList<XComponentDTO> components();

    /**
     * Returns the observable list of configurations
     *
     * @return the observable list of configurations
     */
    ObservableList<XConfigurationDTO> configurations();

    /**
     * Returns the observable list of events
     *
     * @return the observable list of events
     */
    ObservableList<XEventDTO> events();

    /**
     * Returns the observable list of properties
     *
     * @return the observable list of properties
     */
    ObservableList<XPropertyDTO> properties();

}
