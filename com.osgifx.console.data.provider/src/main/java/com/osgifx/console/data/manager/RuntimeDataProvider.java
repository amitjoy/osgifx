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

import com.google.common.collect.Maps;
import com.osgifx.console.agent.dto.XBundleDTO;
import com.osgifx.console.agent.dto.XComponentDTO;
import com.osgifx.console.agent.dto.XConfigurationDTO;
import com.osgifx.console.agent.dto.XEventDTO;
import com.osgifx.console.agent.dto.XHttpComponentDTO;
import com.osgifx.console.agent.dto.XLogEntryDTO;
import com.osgifx.console.agent.dto.XMemoryInfoDTO;
import com.osgifx.console.agent.dto.XPropertyDTO;
import com.osgifx.console.agent.dto.XServiceDTO;
import com.osgifx.console.agent.dto.XThreadDTO;
import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.data.provider.PackageDTO;
import com.osgifx.console.data.supplier.RuntimeInfoSupplier;
import com.osgifx.console.supervisor.Supervisor;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

@Component
@SuppressWarnings("unchecked")
public final class RuntimeDataProvider implements DataProvider {

	private final FluentLogger                     logger;
	private final Supervisor                       supervisor;
	private final Map<String, RuntimeInfoSupplier> infoSuppliers;

	@Activate
	public RuntimeDataProvider(@Reference final LoggerFactory factory, @Reference final Supervisor supervisor) {
		this.supervisor = supervisor;
		infoSuppliers   = Maps.newHashMap();
		logger          = FluentLogger.of(factory.createLogger(getClass().getName()));
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
	public ObservableList<XBundleDTO> bundles() {
		return (ObservableList<XBundleDTO>) getData(BUNDLES_ID);
	}

	@Override
	public ObservableList<PackageDTO> packages() {
		return (ObservableList<PackageDTO>) getData(PACKAGES_ID);
	}

	@Override
	public ObservableList<XServiceDTO> services() {
		return (ObservableList<XServiceDTO>) getData(SERVICES_ID);
	}

	@Override
	public ObservableList<XComponentDTO> components() {
		return (ObservableList<XComponentDTO>) getData(COMPONENTS_ID);
	}

	@Override
	public ObservableList<XConfigurationDTO> configurations() {
		return (ObservableList<XConfigurationDTO>) getData(CONFIGURATIONS_ID);
	}

	@Override
	public ObservableList<XEventDTO> events() {
		return (ObservableList<XEventDTO>) getData(EVENTS_ID);
	}

	@Override
	public ObservableList<XLogEntryDTO> logs() {
		return (ObservableList<XLogEntryDTO>) getData(LOGS_ID);
	}

	@Override
	public ObservableList<XPropertyDTO> properties() {
		return (ObservableList<XPropertyDTO>) getData(PROPERTIES_ID);
	}

	@Override
	public ObservableList<XThreadDTO> threads() {
		return (ObservableList<XThreadDTO>) getData(THREADS_ID);
	}

	@Override
	public ObservableList<XBundleDTO> leaks() {
		return (ObservableList<XBundleDTO>) getData(LEAKS_ID);
	}

	@Override
	public ObservableList<XHttpComponentDTO> httpComponents() {
		return (ObservableList<XHttpComponentDTO>) getData(HTTP_ID);
	}

	@Override
	public XMemoryInfoDTO memory() {
		final var agent = supervisor.getAgent();
		if (agent == null) {
			logger.atWarning().log("Agent is not connected");
			return null;
		}
		return agent.getMemoryInfo();
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
