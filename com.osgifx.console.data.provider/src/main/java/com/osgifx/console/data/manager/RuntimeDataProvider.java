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

import static com.osgifx.console.event.topics.BundleActionEventTopics.BUNDLE_ACTION_EVENT_TOPICS;
import static com.osgifx.console.event.topics.BundleActionEventTopics.BUNDLE_ACTION_EVENT_TOPIC_PREFIX;
import static com.osgifx.console.event.topics.ComponentActionEventTopics.COMPONENT_ACTION_EVENT_TOPICS;
import static com.osgifx.console.event.topics.ComponentActionEventTopics.COMPONENT_ACTION_EVENT_TOPIC_PREFIX;
import static com.osgifx.console.event.topics.ConfigurationActionEventTopics.CONFIGURATION_ACTION_EVENT_TOPICS;
import static com.osgifx.console.event.topics.ConfigurationActionEventTopics.CONFIGURATION_ACTION_EVENT_TOPIC_PREFIX;
import static com.osgifx.console.supervisor.Supervisor.AGENT_CONNECTED_EVENT_TOPIC;
import static com.osgifx.console.util.fx.ConsoleFxHelper.makeNullSafe;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.LoggerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.osgifx.console.agent.dto.XBundleDTO;
import com.osgifx.console.agent.dto.XComponentDTO;
import com.osgifx.console.agent.dto.XConfigurationDTO;
import com.osgifx.console.agent.dto.XEventDTO;
import com.osgifx.console.agent.dto.XHttpContextInfoDTO;
import com.osgifx.console.agent.dto.XLogEntryDTO;
import com.osgifx.console.agent.dto.XPackageDTO;
import com.osgifx.console.agent.dto.XPropertyDTO;
import com.osgifx.console.agent.dto.XServiceDTO;
import com.osgifx.console.agent.dto.XThreadDTO;
import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.data.provider.PackageDTO;
import com.osgifx.console.supervisor.EventListener;
import com.osgifx.console.supervisor.LogEntryListener;
import com.osgifx.console.supervisor.Supervisor;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

@Component
@EventTopics({ AGENT_CONNECTED_EVENT_TOPIC, BUNDLE_ACTION_EVENT_TOPICS, COMPONENT_ACTION_EVENT_TOPICS, CONFIGURATION_ACTION_EVENT_TOPICS })
public final class RuntimeDataProvider implements DataProvider, EventListener, LogEntryListener, EventHandler {

	@Reference
	private LoggerFactory     factory;
	@Reference
	private Supervisor        supervisor;
	@Reference
	private ThreadSynchronize threadSync;
	private FluentLogger      logger;

	private final ObservableList<XBundleDTO>        bundles        = FXCollections
	        .synchronizedObservableList(FXCollections.observableArrayList());
	private final ObservableList<PackageDTO>        packages       = FXCollections
	        .synchronizedObservableList(FXCollections.observableArrayList());
	private final ObservableList<XServiceDTO>       services       = FXCollections
	        .synchronizedObservableList(FXCollections.observableArrayList());
	private final ObservableList<XComponentDTO>     components     = FXCollections
	        .synchronizedObservableList(FXCollections.observableArrayList());
	private final ObservableList<XConfigurationDTO> configurations = FXCollections
	        .synchronizedObservableList(FXCollections.observableArrayList());
	private final ObservableList<XPropertyDTO>      properties     = FXCollections
	        .synchronizedObservableList(FXCollections.observableArrayList());
	private final ObservableList<XThreadDTO>        threads        = FXCollections
	        .synchronizedObservableList(FXCollections.observableArrayList());
	private final ObservableList<XBundleDTO>        leaks          = FXCollections
	        .synchronizedObservableList(FXCollections.observableArrayList());
	private final ObservableList<XLogEntryDTO>      logs           = FXCollections
	        .synchronizedObservableList(FXCollections.observableArrayList());
	private final ObservableList<XEventDTO>         events         = FXCollections
	        .synchronizedObservableList(FXCollections.observableArrayList());

	@Activate
	void activate() {
		logger = FluentLogger.of(factory.createLogger(getClass().getName()));
	}

	@Override
	public void retrieveInfo(final boolean isAsync) {
		if (isAsync) {
			final var bundlesFuture        = CompletableFuture.runAsync(this::retrieveBundles);
			final var packagesFuture       = CompletableFuture.runAsync(this::retrievePackages);
			final var servicesFuture       = CompletableFuture.runAsync(this::retrieveServices);
			final var componentsFuture     = CompletableFuture.runAsync(this::retrieveComponents);
			final var configurationsFuture = CompletableFuture.runAsync(this::retrieveConfigurations);
			final var propertiesFuture     = CompletableFuture.runAsync(this::retrieveProperties);
			final var threadsFuture        = CompletableFuture.runAsync(this::retrieveThreads);
			final var leaksFuture          = CompletableFuture.runAsync(this::retrieveLeaks);

			// @formatter:off
			CompletableFuture.allOf(
									bundlesFuture,
									packagesFuture,
									servicesFuture,
									componentsFuture,
									configurationsFuture,
									propertiesFuture,
									threadsFuture,
									leaksFuture)
							.thenRunAsync(() -> logger.atInfo().log("All runtime informations have been retrieved successfully (async)"));
			// @formatter:on
		} else {
			retrieveBundles();
			retrievePackages();
			retrieveServices();
			retrieveComponents();
			retrieveConfigurations();
			retrieveProperties();
			retrieveThreads();
			retrieveLeaks();

			logger.atInfo().log("All runtime informations have been retrieved successfully (sync)");
		}
	}

	@Override
	public void handleEvent(final Event event) {
		final var topic = event.getTopic();
		if (AGENT_CONNECTED_EVENT_TOPIC.equals(topic)) {
			// on connection, we don't need to use UI thread
			retrieveInfo(true);
		} else if (topic.startsWith(BUNDLE_ACTION_EVENT_TOPIC_PREFIX)) {
			// synchronously update the bundles UI and the rest can be done asynchronously
			threadSync.syncExec(this::retrieveBundles);
			threadSync.asyncExec(this::retrievePackages);
			threadSync.asyncExec(this::retrieveServices);
			threadSync.asyncExec(this::retrieveComponents);
			threadSync.asyncExec(this::retrieveConfigurations);
			threadSync.asyncExec(this::retrieveProperties);
			threadSync.asyncExec(this::retrieveThreads);
			threadSync.asyncExec(this::retrieveLeaks);
		} else if (topic.startsWith(COMPONENT_ACTION_EVENT_TOPIC_PREFIX)) {
			// synchronously update the component UI and the rest can be done asynchronously
			threadSync.syncExec(this::retrieveComponents);
			threadSync.asyncExec(this::retrieveConfigurations);
			threadSync.asyncExec(this::retrieveProperties);
			threadSync.asyncExec(this::retrieveThreads);
		} else if (topic.startsWith(CONFIGURATION_ACTION_EVENT_TOPIC_PREFIX)) {
			// synchronously update the configurations UI and the rest asynchronously
			threadSync.asyncExec(this::retrieveServices);
			threadSync.asyncExec(this::retrieveComponents);
			threadSync.syncExec(this::retrieveConfigurations);
			threadSync.asyncExec(this::retrieveProperties);
			threadSync.asyncExec(this::retrieveThreads);
			threadSync.asyncExec(this::retrieveLeaks);
		}
	}

	@Override
	public synchronized ObservableList<XBundleDTO> bundles() {
		return bundles;
	}

	@Override
	public synchronized ObservableList<PackageDTO> packages() {
		return packages;
	}

	@Override
	public synchronized ObservableList<XServiceDTO> services() {
		return services;
	}

	@Override
	public synchronized ObservableList<XComponentDTO> components() {
		return components;
	}

	@Override
	public synchronized ObservableList<XConfigurationDTO> configurations() {
		return configurations;
	}

	@Override
	public synchronized ObservableList<XEventDTO> events() {
		return events;
	}

	@Override
	public synchronized ObservableList<XLogEntryDTO> logs() {
		return logs;
	}

	@Override
	public synchronized void onEvent(final XEventDTO event) {
		events.add(event);
	}

	@Override
	public synchronized void logged(final XLogEntryDTO logEntry) {
		logs.add(logEntry);
	}

	@Override
	public synchronized ObservableList<XPropertyDTO> properties() {
		return properties;
	}

	@Override
	public synchronized ObservableList<XThreadDTO> threads() {
		return threads;
	}

	@Override
	public ObservableList<XBundleDTO> leaks() {
		return leaks;
	}

	@Override
	public XHttpContextInfoDTO httpContext() {
		final var agent = supervisor.getAgent();
		if (agent == null) {
			logger.atWarning().log("Agent is not connected");
			return null;
		}
		return agent.getHttpContextInfo();
	}

	private synchronized void retrieveBundles() {
		final var agent = supervisor.getAgent();
		if (agent == null) {
			logger.atWarning().log("Agent is not connected");
			return;
		}
		bundles.clear();
		bundles.addAll(makeNullSafe(agent.getAllBundles()));
	}

	private synchronized void retrievePackages() {
		final var agent = supervisor.getAgent();
		if (agent == null) {
			logger.atWarning().log("Agent is not connected");
			return;
		}
		packages.clear();
		packages.addAll(preparePackages(agent.getAllBundles()));
	}

	private synchronized void retrieveServices() {
		final var agent = supervisor.getAgent();
		if (agent == null) {
			logger.atWarning().log("Agent is not connected");
			return;
		}
		services.clear();
		services.addAll(makeNullSafe(agent.getAllServices()));
	}

	private synchronized void retrieveComponents() {
		final var agent = supervisor.getAgent();
		if (agent == null) {
			logger.atWarning().log("Agent is not connected");
			return;
		}
		components.clear();
		components.addAll(makeNullSafe(agent.getAllComponents()));
	}

	private synchronized void retrieveConfigurations() {
		final var agent = supervisor.getAgent();
		if (agent == null) {
			logger.atWarning().log("Agent is not connected");
			return;
		}
		configurations.clear();
		configurations.addAll(makeNullSafe(agent.getAllConfigurations()));
	}

	private synchronized void retrieveProperties() {
		final var agent = supervisor.getAgent();
		if (agent == null) {
			logger.atWarning().log("Agent is not connected");
			return;
		}
		properties.clear();
		properties.addAll(makeNullSafe(agent.getAllProperties()));
	}

	private synchronized void retrieveThreads() {
		final var agent = supervisor.getAgent();
		if (agent == null) {
			logger.atWarning().log("Agent is not connected");
			return;
		}
		threads.clear();
		threads.addAll(makeNullSafe(agent.getAllThreads()));
	}

	private synchronized void retrieveLeaks() {
		final var agent = supervisor.getAgent();
		if (agent == null) {
			logger.atWarning().log("Agent is not connected");
			return;
		}
		leaks.clear();
		leaks.addAll(makeNullSafe(agent.getClassloaderLeaks()));
	}

	private synchronized ObservableList<PackageDTO> preparePackages(final List<XBundleDTO> bundles) {
		final List<PackageDTO>        packages      = Lists.newArrayList();
		final Map<String, PackageDTO> finalPackages = Maps.newHashMap();   // key: package name, value: PackageDTO

		for (final XBundleDTO bundle : bundles) {
			final var exportedPackages = toPackageDTOs(bundle.exportedPackages);
			final var importedPackages = toPackageDTOs(bundle.importedPackages);

			exportedPackages.forEach(p -> p.exporters.add(bundle));
			importedPackages.forEach(p -> p.importers.add(bundle));

			packages.addAll(exportedPackages);
			packages.addAll(importedPackages);
		}
		for (final PackageDTO pkg : packages) {
			final var key = pkg.name + ":" + pkg.version;
			if (!finalPackages.containsKey(key)) {
				finalPackages.put(key, pkg);
			} else {
				final var packageDTO = finalPackages.get(key);

				packageDTO.exporters.addAll(pkg.exporters);
				packageDTO.importers.addAll(pkg.importers);
			}
		}
		for (final PackageDTO pkg : finalPackages.values()) {
			if (pkg.exporters.size() > 1) {
				pkg.isDuplicateExport = true;
			}
		}
		return FXCollections.observableArrayList(finalPackages.values());
	}

	private List<PackageDTO> toPackageDTOs(final List<XPackageDTO> exportedPackages) {
		return exportedPackages.stream().map(this::toPackageDTO).toList();
	}

	private PackageDTO toPackageDTO(final XPackageDTO xpkg) {
		final var pkg = new PackageDTO();

		pkg.name    = xpkg.name;
		pkg.version = xpkg.version;

		return pkg;
	}

}
