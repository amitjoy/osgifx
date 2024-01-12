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
package com.osgifx.console.supervisor.factory;

import org.osgi.annotation.versioning.ProviderType;

import com.osgifx.console.supervisor.Supervisor;

/**
 * The {@link SupervisorFactory} service is the application access point to the
 * {@link Supervisor} functionality. It is used to create/remove {@link Supervisor}
 * instances pretty easily.
 *
 * <p>
 * Access to this service requires the
 * {@code ServicePermission[SupervisorFactory, GET]} permission. It is intended
 * that only administrative bundles should be granted this permission to limit
 * access to the potentially intrusive methods provided by this service.
 * </p>
 *
 * @noimplement This interface is not intended to be implemented by consumers.
 * @noextend This interface is not intended to be extended by consumers.
 *
 * @ThreadSafe
 */
@ProviderType
public interface SupervisorFactory {

    public enum SupervisorType {
        REMOTE_RPC,
        SNAPSHOT
    }

    /**
     * Creates an instance of the {@link Supervisor} of the specified {@code type}
     *
     * <p>
     * <b>Note that</b>, if the {@link Supervisor} of the specified type is already created,
     * the method will simply ignore creating a new instance of the specified {@link Supervisor}.
     *
     * @param type the type of the {@link Supervisor}
     */
    void createSupervisor(SupervisorType type);

    /**
     * Removes the already created {@link Supervisor} of the specified {@code type}
     *
     * <p>
     * <b>Note that</b>, if the {@link Supervisor} of the specified type is already removed,
     * the method will simply ignore removing any instance of the specified {@link Supervisor}.
     *
     * @param type the type of the {@link Supervisor}
     */
    void removeSupervisor(SupervisorType type);

}
