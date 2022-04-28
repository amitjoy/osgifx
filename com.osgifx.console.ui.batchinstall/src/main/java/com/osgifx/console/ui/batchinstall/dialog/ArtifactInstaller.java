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
package com.osgifx.console.ui.batchinstall.dialog;

import static com.osgifx.console.agent.dto.XResultDTO.ERROR;
import static org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME;
import static org.osgi.framework.Constants.BUNDLE_VERSION;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.List;
import java.util.Map;
import java.util.jar.JarInputStream;

import org.apache.felix.cm.json.Configurations;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.LoggerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.ui.batchinstall.dialog.BatchInstallDialog.ArtifactDTO;
import com.osgifx.console.util.fx.FxDialog;

@Component(service = ArtifactInstaller.class)
public class ArtifactInstaller {

	private static final int DEFAULT_START_LEVEL = 10;

	@Reference
	private LoggerFactory     factory;
	@Reference
	private Supervisor        supervisor;
	@Reference
	private ThreadSynchronize threadSync;
	private FluentLogger      logger;

	@Activate
	void activate() {
		logger = FluentLogger.of(factory.createLogger(getClass().getName()));
	}

	public String installArtifacts(final List<ArtifactDTO> artifacts) {
		final var agent = supervisor.getAgent();
		if (agent == null) {
			logger.atWarning().log("Agent is not connected");
			return null;
		}
		final var result = new StringBuilder();
		for (final ArtifactDTO artifact : artifacts) {
			if (artifact.isConfiguration()) {
				try {
					final var pids = readConfigFile(artifact.file());
					for (final ConfigDTO config : pids) {
						final var r = agent.createOrUpdateConfiguration(config.pid(), config.properties());
						if (r.result == ERROR) {
							result.append(config.pid);
							result.append(": ");
							result.append(r.response);
							result.append(System.lineSeparator());
						}
					}
				} catch (final Exception e) {
					threadSync.asyncExec(() -> FxDialog.showExceptionDialog(e, getClass().getClassLoader()));
					return null;
				}
			} else {
				final var jar = toValidJarFile(artifact.file());
				if (jar == null) {
					continue;
				}
				try {
					agent.installWithData(null, Files.toByteArray(artifact.file()), DEFAULT_START_LEVEL);
				} catch (final Exception e) {
					result.append(jar.bsn);
					result.append(": ");
					result.append(e.getMessage());
					result.append(System.lineSeparator());
				}
			}
		}
		return result.toString();
	}

	private JarDTO toValidJarFile(final File file) {
		try {
			final var bsn     = readAttributeFromManifest(file, BUNDLE_SYMBOLICNAME);
			final var version = readAttributeFromManifest(file, BUNDLE_VERSION);
			return new JarDTO(file, bsn, version);
		} catch (final Exception e) {
			logger.atError().withException(e).log("'%s' is not a valid bundle", file.getName());
		}
		return null;
	}

	private List<ConfigDTO> readConfigFile(final File file) throws Exception {
		final List<ConfigDTO> configs = Lists.newArrayList();
		try (var reader = new FileReader(file)) {

			final var configReader   = Configurations.buildReader().withIdentifier(file.getName()).build(reader);
			final var resource       = configReader.readConfigurationResource();
			final var configurations = resource.getConfigurations();

			configurations.forEach((k, v) -> configs.add(new ConfigDTO(k, v)));

			return configs;
		} catch (final Exception e) {
			logger.atError().withException(e).log("'%s' cannot be read", file.getName());
			throw e;
		}
	}

	public static record ConfigDTO(String pid, Map<String, Object> properties) {
	}

	public static record JarDTO(File file, String bsn, String version) {
	}

	private static String readAttributeFromManifest(final File jarResource, final String attribute) throws Exception {
		try (var is = new FileInputStream(jarResource); var jarStream = new JarInputStream(is);) {
			final var manifest = jarStream.getManifest();
			if (manifest == null) {
				throw new RuntimeException(jarResource + " is not a valid JAR");
			}
			final var value = manifest.getMainAttributes().getValue(attribute);
			if (value.contains(";")) {
				return value.split(";")[0];
			}
			return value;
		}
	}

}
