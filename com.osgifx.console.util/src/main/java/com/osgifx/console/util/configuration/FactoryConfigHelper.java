/*******************************************************************************
 * Copyright 2021-2024 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.util.configuration;

import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.fx.core.ExceptionUtils;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Extends the {@link ConfigHelper} to support also factories.
 *
 * @param <T> the type of the configuration (annotation) interface
 */
public class FactoryConfigHelper<T> extends ConfigHelper<T> {

    final String factoryPid;

    /**
     * Create a configuration helper for a factory PID.
     *
     * @param type the configuration type
     * @param cm the config admin
     * @param factoryPid the factory pid
     */
    public FactoryConfigHelper(final Class<T> type, final ConfigurationAdmin cm, final String factoryPid) {
        super(type, cm);
        this.factoryPid = factoryPid;
    }

    /**
     * Create a new configuration for the given factory Pid
     *
     * @return this
     */
    public FactoryConfigHelper<T> create() {
        Configuration configuration;
        try {
            configuration = cm.createFactoryConfiguration(factoryPid, "?");
            pid           = configuration.getPid();
            configuration.update(FrameworkUtil.asDictionary(properties));
        } catch (final IOException e) {
            throw ExceptionUtils.wrap(e);
        }
        return this;
    }

    /**
     * Answer a list of instances (well the PIDs).
     *
     * @return a list of PIDs that are instances of this factory
     */
    public Set<String> getInstances() throws IOException, InvalidSyntaxException {
        final var listConfigurations = cm.listConfigurations("(service.factoryPid=" + factoryPid + ")");
        if (listConfigurations == null) {
            return Collections.emptySet();
        }

        return Stream.of(listConfigurations).map(Configuration::getPid).collect(toSet());
    }
}