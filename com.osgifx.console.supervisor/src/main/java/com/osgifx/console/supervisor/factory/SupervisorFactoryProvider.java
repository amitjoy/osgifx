/*******************************************************************************
 * Copyright 2021-2026 Amit Kumar Mondal
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
package com.osgifx.console.supervisor.factory;

import static org.osgi.service.condition.Condition.CONDITION_ID;
import static org.osgi.service.condition.Condition.INSTANCE;

import java.util.Map;

import org.apache.aries.component.dsl.OSGi;
import org.apache.aries.component.dsl.OSGiResult;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.condition.Condition;

import com.google.common.collect.Maps;
import com.osgifx.console.supervisor.rpc.RpcSupervisor;
import com.osgifx.console.supervisor.snapshot.SnapshotSupervisor;

@Component
public final class SupervisorFactoryProvider implements SupervisorFactory {

    private final Map<SupervisorType, OSGiResult> registrations = Maps.newHashMap();

    @Activate
    private BundleContext context;

    @Override
    public void createSupervisor(final SupervisorType type) {
        final var conditionIdValue = switch (type) {
            case REMOTE_RPC -> RpcSupervisor.CONDITION_ID_VALUE;
            case SNAPSHOT -> SnapshotSupervisor.CONDITION_ID_VALUE;
        };
        registrations.computeIfAbsent(type,
                _ -> OSGi.register(Condition.class, INSTANCE, Map.of(CONDITION_ID, conditionIdValue)).run(context));
    }

    @Override
    public void removeSupervisor(final SupervisorType type) {
        registrations.computeIfPresent(type, (_, v) -> {
            v.close();
            return null;
        });
    }

}
