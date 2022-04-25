/*******************************************************************************
 * Copyright 2021-2022 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.data.manager;

import static com.osgifx.console.data.supplier.BundlesInfoSupplier.BUNDLES_ID;
import static com.osgifx.console.data.supplier.ComponentsInfoSupplier.COMPONENTS_ID;
import static com.osgifx.console.data.supplier.ConfigurationsInfoSupplier.CONFIGURATIONS_ID;
import static com.osgifx.console.data.supplier.EventsInfoSupplier.EVENTS_ID;
import static com.osgifx.console.data.supplier.HttpComponentsInfoSupplier.HTTP_ID;
import static com.osgifx.console.data.supplier.LeaksInfoSupplier.LEAKS_ID;
import static com.osgifx.console.data.supplier.LogsInfoSupplier.LOGS_ID;
import static com.osgifx.console.data.supplier.PackagesInfoSupplier.PACKAGES_ID;
import static com.osgifx.console.data.supplier.PropertiesInfoSupplier.PROPERTIES_ID;
import static com.osgifx.console.data.supplier.RuntimeInfoSupplier.PROPERTY_ID;
import static com.osgifx.console.data.supplier.ServicesInfoSupplier.SERVICES_ID;
import static com.osgifx.console.data.supplier.ThreadsInfoSupplier.THREADS_ID;
import static com.osgifx.console.event.topics.BundleActionEventTopics.BUNDLE_ACTION_EVENT_TOPICS;
import static com.osgifx.console.event.topics.BundleActionEventTopics.BUNDLE_ACTION_EVENT_TOPIC_PREFIX;
import static com.osgifx.console.event.topics.ComponentActionEventTopics.COMPONENT_ACTION_EVENT_TOPICS;
import static com.osgifx.console.event.topics.ComponentActionEventTopics.COMPONENT_ACTION_EVENT_TOPIC_PREFIX;
import static com.osgifx.console.event.topics.ConfigurationActionEventTopics.CONFIGURATION_ACTION_EVENT_TOPICS;
import static com.osgifx.console.event.topics.ConfigurationActionEventTopics.CONFIGURATION_ACTION_EVENT_TOPIC_PREFIX;
import static com.osgifx.console.supervisor.Supervisor.AGENT_CONNECTED_EVENT_TOPIC;
import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.LoggerFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;

import com.google.common.collect.Maps;
import com.osgifx.console.agent.dto.XBundleDTO;
import com.osgifx.console.agent.dto.XComponentDTO;
import com.osgifx.console.agent.dto.XConfigurationDTO;
import com.osgifx.console.agent.dto.XEventDTO;
import com.osgifx.console.agent.dto.XHttpComponentDTO;
import com.osgifx.console.agent.dto.XLogEntryDTO;
import com.osgifx.console.agent.dto.XPropertyDTO;
import com.osgifx.console.agent.dto.XServiceDTO;
import com.osgifx.console.agent.dto.XThreadDTO;
import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.data.provider.PackageDTO;
import com.osgifx.console.data.supplier.RuntimeInfoSupplier;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

@Component
@EventTopics({ AGENT_CONNECTED_EVENT_TOPIC, BUNDLE_ACTION_EVENT_TOPICS, COMPONENT_ACTION_EVENT_TOPICS, CONFIGURATION_ACTION_EVENT_TOPICS })
@SuppressWarnings("unchecked")
public final class RuntimeDataProvider implements DataProvider, EventHandler {

	private final FluentLogger                     logger;
	private final Map<String, RuntimeInfoSupplier> infoSuppliers;

	@Activate
	public RuntimeDataProvider(@Reference final LoggerFactory factory) {
		infoSuppliers = Maps.newHashMap();
		logger        = FluentLogger.of(factory.createLogger(getClass().getName()));
	}

	@Override
	public void retrieveInfo(final String id, final boolean isAsync) {
		if (id == null) {
			if (isAsync) {
				// @formatter:off
				final Collection<CompletableFuture<Void>> futures =
						infoSuppliers.values()
						             .stream()
						             .map(s -> CompletableFuture.runAsync(s::retrieve))
						             .toList();
				CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
				                 .thenRunAsync(() -> logger.atInfo().log("All runtime informations have been retrieved successfully (async)"));
				// @formatter:on
			} else {
				infoSuppliers.values().stream().forEach(RuntimeInfoSupplier::retrieve);
				logger.atInfo().log("All runtime informations have been retrieved successfully (sync)");
			}
		} else if (isAsync) {
			CompletableFuture.runAsync(() -> retrieve(id));
			logger.atInfo().log("Runtime information of '%s' has been retrieved successfully (async)", id);
		} else {
			retrieve(id);
			logger.atInfo().log("Runtime information of '%s' has been retrieved successfully (sync)", id);
		}
	}

	@Override
	public void handleEvent(final Event event) {
		final var topic = event.getTopic();
		if (AGENT_CONNECTED_EVENT_TOPIC.equals(topic)) {
			// on connection, retrieve all informations just for the purpose of caching
			retrieveInfo(null, true);
		} else if (topic.startsWith(BUNDLE_ACTION_EVENT_TOPIC_PREFIX)) {
			// only retrieve those informations from the remote runtime that can be impacted
			// by bundle actions
			retrieveInfo(BUNDLES_ID, true);
			retrieveInfo(PACKAGES_ID, true);
			retrieveInfo(SERVICES_ID, true);
			retrieveInfo(COMPONENTS_ID, true);
			retrieveInfo(CONFIGURATIONS_ID, true);
			retrieveInfo(PROPERTIES_ID, true);
			retrieveInfo(THREADS_ID, true);
			retrieveInfo(LEAKS_ID, true);
			retrieveInfo(HTTP_ID, true);
		} else if (topic.startsWith(COMPONENT_ACTION_EVENT_TOPIC_PREFIX) || topic.startsWith(CONFIGURATION_ACTION_EVENT_TOPIC_PREFIX)) {
			// only retrieve those informations from the remote runtime that can be impacted
			// by component and configuration actions
			retrieveInfo(SERVICES_ID, true);
			retrieveInfo(COMPONENTS_ID, true);
			retrieveInfo(CONFIGURATIONS_ID, true);
			retrieveInfo(PROPERTIES_ID, true);
			retrieveInfo(THREADS_ID, true);
			retrieveInfo(HTTP_ID, true);
		}
	}

	@Override
	public synchronized ObservableList<XBundleDTO> bundles() {
		return (ObservableList<XBundleDTO>) getData(BUNDLES_ID);
	}

	@Override
	public synchronized ObservableList<PackageDTO> packages() {
		return (ObservableList<PackageDTO>) getData(PACKAGES_ID);
	}

	@Override
	public synchronized ObservableList<XServiceDTO> services() {
		return (ObservableList<XServiceDTO>) getData(SERVICES_ID);
	}

	@Override
	public synchronized ObservableList<XComponentDTO> components() {
		return (ObservableList<XComponentDTO>) getData(COMPONENTS_ID);
	}

	@Override
	public synchronized ObservableList<XConfigurationDTO> configurations() {
		return (ObservableList<XConfigurationDTO>) getData(CONFIGURATIONS_ID);
	}

	@Override
	public synchronized ObservableList<XEventDTO> events() {
		return (ObservableList<XEventDTO>) getData(EVENTS_ID);
	}

	@Override
	public synchronized ObservableList<XLogEntryDTO> logs() {
		return (ObservableList<XLogEntryDTO>) getData(LOGS_ID);
	}

	@Override
	public synchronized ObservableList<XPropertyDTO> properties() {
		return (ObservableList<XPropertyDTO>) getData(PROPERTIES_ID);
	}

	@Override
	public synchronized ObservableList<XThreadDTO> threads() {
		return (ObservableList<XThreadDTO>) getData(THREADS_ID);
	}

	@Override
	public synchronized ObservableList<XBundleDTO> leaks() {
		return (ObservableList<XBundleDTO>) getData(LEAKS_ID);
	}

	@Override
	public synchronized ObservableList<XHttpComponentDTO> httpComponents() {
		return (ObservableList<XHttpComponentDTO>) getData(HTTP_ID);
	}

	@Reference(cardinality = MULTIPLE, policy = DYNAMIC)
	void bindInfoSupplier(final RuntimeInfoSupplier supplier, final ServiceReference<RuntimeInfoSupplier> reference) {
		final var id = (String) reference.getProperty(PROPERTY_ID);
		infoSuppliers.put(id, supplier);
	}

	void unbindInfoSupplier(final RuntimeInfoSupplier supplier, final ServiceReference<RuntimeInfoSupplier> reference) {
		final var id = (String) reference.getProperty(PROPERTY_ID);
		infoSuppliers.remove(id);
	}

	private ObservableList<?> getData(final String id) {
		return Optional.ofNullable(infoSuppliers.get(id)).map(RuntimeInfoSupplier::supply).orElse(FXCollections.observableArrayList());
	}

	private void retrieve(final String id) {
		Optional.ofNullable(infoSuppliers.get(id)).ifPresent(RuntimeInfoSupplier::retrieve);
	}

}
