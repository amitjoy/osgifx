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
package com.osgifx.console.agent.provider;

import static org.osgi.framework.namespace.PackageNamespace.PACKAGE_NAMESPACE;

import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public final class PackageWirings {

    public enum Type {
        SCR("SCR"),
        CM("Config Admin"),
        DMT("DMT Admin"),
        USER_ADMIN("User Admin"),
        METATYPE("Metatype"),
        EVENT_ADMIN("Event Admin"),
        LOG("Log"),
        R7_LOGGER("R7 Logger"),
        HTTP("HTTP"),
        JMX("JMX"),
        HC("Felix Healthcheck");

        public String comprehensibleName;

        Type(final String comprehensibleName) {
            this.comprehensibleName = comprehensibleName;
        }
    }

    private final BundleContext context;

    @Inject
    public PackageWirings(final BundleContext context) {
        this.context = context;
    }

    public boolean isWired(final String packageName) {
        final BundleWiring wiring = context.getBundle().adapt(BundleWiring.class);
        for (final BundleWire wire : wiring.getRequiredWires(PACKAGE_NAMESPACE)) {
            final String       pkgName        = (String) wire.getCapability().getAttributes().get(PACKAGE_NAMESPACE);
            final BundleWiring providerWiring = wire.getProviderWiring();
            if (pkgName.startsWith(packageName) && providerWiring != null) {
                return true;
            }
        }
        return false;
    }

    public boolean isScrWired() {
        return isWired("org.osgi.service.component.runtime");
    }

    public boolean isConfigAdminWired() {
        return isWired("org.osgi.service.cm");
    }

    public boolean isDmtAdminWired() {
        return isWired("org.osgi.service.dmt");
    }

    public boolean isUserAdminWired() {
        return isWired("org.osgi.service.useradmin");
    }

    public boolean isMetatypeWired() {
        return isWired("org.osgi.service.metatype");
    }

    public boolean isEventAdminWired() {
        return isWired("org.osgi.service.event");
    }

    public boolean isLogWired() {
        return isWired("org.osgi.service.log");
    }

    public boolean isR7LoggerAdminWired() {
        return isWired("org.osgi.service.log.admin");
    }

    public boolean isHttpServiceRuntimeWired() {
        return isWired("org.osgi.service.http.runtime");
    }

    public boolean isJmxWired() {
        return isWired("javax.management");
    }

    public boolean isFelixHcWired() {
        return isWired("org.apache.felix.hc.api");
    }

}
