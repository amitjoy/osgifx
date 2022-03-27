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
package com.osgifx.console.ui.configurations.converter;

import java.util.List;

import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.LoggerFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.osgifx.console.agent.dto.ConfigValue;
import com.osgifx.console.agent.dto.XResultDTO;
import com.osgifx.console.supervisor.Supervisor;

@Component(service = ConfigurationManager.class)
public final class ConfigurationManager {

	@Reference
	private Supervisor supervisor;

	@Reference
	private LoggerFactory factory;
	private FluentLogger  logger;

	void activate() {
		logger = FluentLogger.of(factory.createLogger(getClass().getName()));
	}

	public boolean createOrUpdateConfiguration(final String pid, final List<ConfigValue> newProperties) {
		final var agent = supervisor.getAgent();
		if (agent == null) {
			logger.atWarning().log("Remote agent cannot be connected");
			return false;
		}

		final var result = agent.createOrUpdateConfiguration(pid, newProperties);
		return result.result == XResultDTO.SUCCESS;
	}

	public boolean createFactoryConfiguration(final String factoryPid, final List<ConfigValue> newProperties) {
		final var agent = supervisor.getAgent();
		if (agent == null) {
			logger.atWarning().log("Remote agent cannot be connected");
			return false;
		}
		final var result = agent.createFactoryConfiguration(factoryPid, newProperties);
		return result.result == XResultDTO.SUCCESS;
	}

}
