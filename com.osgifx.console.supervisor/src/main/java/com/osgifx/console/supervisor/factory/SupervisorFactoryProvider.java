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
package com.osgifx.console.supervisor.factory;

import static com.osgifx.console.supervisor.factory.SupervisorFactory.SupervisorType.SNAPSHOT;
import static com.osgifx.console.supervisor.factory.SupervisorFactory.SupervisorType.SOCKET_RPC;
import static org.osgi.service.condition.Condition.CONDITION_ID;
import static org.osgi.service.condition.Condition.INSTANCE;

import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.condition.Condition;

import com.google.common.collect.Maps;
import com.osgifx.console.supervisor.rpc.LauncherSupervisor;
import com.osgifx.console.supervisor.snapshot.SnapshotSupervisor;

@Component
public final class SupervisorFactoryProvider implements SupervisorFactory {

    @Activate
    private BundleContext context;

    private final Map<SupervisorType, ServiceRegistration<Condition>> registrations = Maps.newHashMap();

    @Override
    public void createSupervisor(final SupervisorType type) {
        String conditionIdValue = null;

        if (type == SOCKET_RPC) {
            conditionIdValue = LauncherSupervisor.CONDITION_ID_VALUE;
        } else if (type == SNAPSHOT) {
            conditionIdValue = SnapshotSupervisor.CONDITION_ID_VALUE;
        }

        final var conditionValue = conditionIdValue; // required for lambda
        registrations.computeIfAbsent(type, key -> context.registerService(Condition.class, INSTANCE,
                new Hashtable<>(Map.of(CONDITION_ID, conditionValue))));
    }

    @Override
    public void removeSupervisor(final SupervisorType type) {
        if (registrations.containsKey(type)) {
            final var registration = registrations.remove(type);
            registration.unregister();
        }
    }

}
