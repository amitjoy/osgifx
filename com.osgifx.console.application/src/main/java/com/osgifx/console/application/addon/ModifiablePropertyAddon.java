/*******************************************************************************
 * Copyright 2021-2023 Amit Kumar Mondal
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
package com.osgifx.console.application.addon;

import java.util.Map;
import java.util.Set;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.google.common.collect.Maps;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

public final class ModifiablePropertyAddon {

    private final Map<String, Object> modifiableProperties = Maps.newHashMap();

    @Log
    @Inject
    private FluentLogger logger;

    public ModifiablePropertyAddon() {
        modifiableProperties.put("is_connected", false);
        modifiableProperties.put("is_local_agent", false);
        modifiableProperties.put("is_snapshot_agent", false);
        modifiableProperties.put("connected.agent", null);
        modifiableProperties.put("selected.settings", null);
        modifiableProperties.put("subscribed_topics", Set.of());
        modifiableProperties.put("local.agent.host", "localhost");
        modifiableProperties.put("local.agent.port", "1729");
        modifiableProperties.put("local.agent.timeout", "200");
    }

    @PostConstruct
    public void init(final IEclipseContext eclipseContext) {
        modifiableProperties.forEach((k, v) -> {
            eclipseContext.declareModifiable(k);
            if (v != null) {
                eclipseContext.set(k, v);
            }
            logger.atInfo().log("'%s' property has been declared as modifiable", k);
        });
    }

}
