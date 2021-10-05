package in.bytehue.osgifx.console.agent;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.service.cm.Configuration.ConfigurationAttribute;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.cm.ReadOnlyConfigurationException;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;

import aQute.remote.api.Agent;
import in.bytehue.osgifx.console.agent.dto.XBundleDTO;
import in.bytehue.osgifx.console.agent.dto.XComponentDTO;
import in.bytehue.osgifx.console.agent.dto.XConfigurationDTO;
import in.bytehue.osgifx.console.agent.dto.XEventDTO;
import in.bytehue.osgifx.console.agent.dto.XPropertyDTO;
import in.bytehue.osgifx.console.agent.dto.XServiceDTO;

/**
 * OSGi.fx console agent running on remote OSGi framework
 */
@ProviderType
public interface ConsoleAgent extends Agent {

    /**
     * Get the detailed information of all the installed bundles
     *
     * @return the detailed information of all the installed bundles
     */
    List<XBundleDTO> getAllBundles();

    /**
     * Get the detailed information of all the registered DS service components
     *
     * @return the detailed information of all the registered DS service components
     */
    List<XComponentDTO> getAllComponents();

    /**
     * Get the detailed information of all the configurations
     *
     * @return the detailed information of all the configurations
     */
    List<XConfigurationDTO> getAllConfigurations();

    /**
     * Get the detailed information of all events
     *
     * @return the detailed information of all events
     */
    List<XEventDTO> getAllEvents();

    /**
     * Get the detailed information of all the properties
     *
     * @return the detailed information of all the properties
     */
    List<XPropertyDTO> getAllProperties();

    /**
     * Get the detailed information of all services
     *
     * @return the detailed information of all services
     */
    List<XServiceDTO> getAllServices();

    /**
     * Returns all the component descriptions.
     *
     * <p>
     * Only component descriptions from active bundles are returned.
     *
     * @return The declared component descriptions of all the active {@code bundles}.
     *         An empty collection is returned if there are no component descriptions
     *         for the specified active bundles.
     */
    Collection<ComponentDescriptionDTO> getComponentDescriptionDTOs();

    /**
     * Returns the component configurations for the specified component
     * description.
     *
     * @param description The component description. Must not be {@code null}.
     * @return A collection containing a snapshot of the current component
     *         configurations for the specified component description. An empty
     *         collection is returned if there are none or if the provided
     *         component description does not belong to an active bundle.
     */
    Collection<ComponentConfigurationDTO> getComponentConfigurationDTOs(ComponentDescriptionDTO description);

    /**
     * Enables the specified component description.
     *
     * <p>
     * If the specified component description is currently enabled, this method
     * has no effect.
     *
     * <p>
     * This method must return after changing the enabled state of the specified
     * component description. Any actions that result from this, such as
     * activating or deactivating a component configuration, must occur
     * asynchronously to this method call.
     *
     * @param description The component description to enable. Must not be
     *            {@code null}.
     * @see #isComponentEnabled(ComponentDescriptionDTO)
     */
    void enableComponent(ComponentDescriptionDTO description);

    /**
     * Disables the specified component description.
     * <p>
     * If the specified component description is currently disabled, this method
     * has no effect.
     * <p>
     * This method must return after changing the enabled state of the specified
     * component description. Any actions that result from this, such as
     * activating or deactivating a component configuration, must occur
     * asynchronously to this method call.
     *
     * @param description The component description to disable. Must not be
     *            {@code null}.
     * @see #isComponentEnabled(ComponentDescriptionDTO)
     */
    void disableComponent(ComponentDescriptionDTO description);

    /**
     *
     * Returns an array of {@code ServiceReferenceDTO} objects. The returned array
     * of {@code ServiceReferenceDTO} objects contains services that were
     * registered under the specified class and match the specified filter
     * expression.
     *
     * <p>
     * The list is valid at the time of the call to this method. However since
     * the Framework is a very dynamic environment, services can be modified or
     * unregistered at any time.
     *
     * <p>
     * The specified {@code filter} expression is used to select the registered
     * services whose service properties contain keys and values which satisfy
     * the filter expression. See {@link Filter} for a description of the filter
     * syntax. If the specified {@code filter} is {@code null}, all registered
     * services are considered to match the filter. If the specified
     * {@code filter} expression cannot be parsed, an
     * {@link InvalidSyntaxException} will be thrown with a human readable
     * message where the filter became unparsable.
     *
     * @param filter The filter expression or {@code null} for all services
     * @return An array of {@code ServiceReferenceDTO} objects or {@code empty} array if
     *         no services are registered which satisfy the search.
     * @throws InvalidSyntaxException If the specified {@code filter} contains
     *             an invalid filter expression that cannot be parsed.
     * @throws IllegalStateException If this BundleContext is no longer valid.
     */
    Collection<ServiceReferenceDTO> getServiceReferences(String filter) throws Exception;

    /**
     * List the current {@code ConfigurationDTO} objects which match the filter.
     *
     * <p>
     * The syntax of the filter string is as defined in the {@link Filter}
     * class. The filter can test any configuration properties including the
     * following:
     * <ul>
     * <li>{@code service.pid} - the persistent identity</li>
     * <li>{@code service.factoryPid} - the factory PID, if applicable</li>
     * <li>{@code service.bundleLocation} - the bundle location</li>
     * </ul>
     * The filter can also be {@code null}, meaning that all
     * {@code ConfigurationDTO} objects should be returned.
     *
     * @param filter A filter string, or {@code null} to retrieve all
     *            {@code ConfigurationDTO} objects.
     * @return All matching {@code ConfigurationDTO} objects, or {@code empty}
     *         collection
     * @throws IOException if access to persistent storage fails
     * @throws InvalidSyntaxException if the filter string is invalid
     */
    Collection<XConfigurationDTO> listConfigurations(String filter) throws IOException, InvalidSyntaxException;

    /**
     * Delete the associated {@code Configuration} object.
     * <p>
     * Removes this configuration object from the persistent store. Notify
     * asynchronously the corresponding Managed Service or Managed Service
     * Factory. A {@link ManagedService} object is notified by a call to its
     * {@code updated} method with a {@code null} properties argument. A
     * {@link ManagedServiceFactory} object is notified by a call to its
     * {@code deleted} method.
     * <p>
     * Also notifies all Configuration Listeners with a
     * {@link ConfigurationEvent#CM_DELETED} event.
     *
     * @throws ReadOnlyConfigurationException If the configuration is
     *             {@link ConfigurationAttribute#READ_ONLY read only}.
     * @throws IOException If delete fails.
     * @throws IllegalStateException If this configuration has been deleted.
     */
    void deleteConfiguration(String pid) throws IOException;

    /**
     * Update the {@code Configuration} object associated with the specified
     * {@code if} with the current properties.
     *
     * @throws IOException if update cannot access the properties in persistent
     *             storage
     * @throws IllegalStateException If this configuration has been deleted.
     */
    void updateConfiguration(String pid, Map<String, Object> newProperties) throws IOException;

    /**
     * Returns the runtime information of the remote system
     *
     * @return the runtime information
     */
    Map<String, String> runtimeInfo();
}