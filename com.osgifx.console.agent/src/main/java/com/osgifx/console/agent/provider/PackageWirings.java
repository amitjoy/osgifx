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
package com.osgifx.console.agent.provider;

import static org.osgi.framework.namespace.PackageNamespace.PACKAGE_NAMESPACE;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * Lazily caches package-wiring checks so that repeated RPC calls do not
 * re-walk the entire wiring list on every invocation. Wiring results are
 * stable between bundle refresh/update cycles, so caching is safe.
 */
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
        JAX_RS("JAX-RS"),
        CDI("CDI"),
        JMX("JMX"),
        HC("Felix Healthcheck"),
        CONDITION("Condition"),
        RSA("Remote Service Admin"),
        GOGO("Gogo");

        public String comprehensibleName;

        Type(final String comprehensibleName) {
            this.comprehensibleName = comprehensibleName;
        }
    }

    private final BundleContext        context;
    private final Map<String, Boolean> cache = new ConcurrentHashMap<>();

    @Inject
    public PackageWirings(final BundleContext context) {
        this.context = context;
    }

    public boolean isWired(final String packageName) {
        final Boolean cached = cache.get(packageName);
        if (cached != null) {
            return cached;
        }
        final boolean result = checkWiring(packageName);
        cache.put(packageName, result);
        return result;
    }

    private boolean checkWiring(final String packageName) {
        final BundleWiring wiring = context.getBundle().adapt(BundleWiring.class);
        for (final BundleWire wire : wiring.getRequiredWires(PACKAGE_NAMESPACE)) {
            final String       pkgName        = (String) wire.getCapability().getAttributes().get(PACKAGE_NAMESPACE);
            final BundleWiring providerWiring = wire.getProviderWiring();
            if (pkgName.equals(packageName) && providerWiring != null) {
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

    public boolean isJaxRsWired() {
        return isWired("org.osgi.service.jaxrs.runtime");
    }

    public boolean isCDIWired() {
        return isWired("org.osgi.service.cdi.runtime");
    }

    public boolean isMqttWired() {
        return isWired("org.osgi.service.messaging");
    }

    public boolean isJmxWired() {
        return isWired("javax.management") || isWired("com.sun.management");
    }

    public boolean isFelixHcWired() {
        return isWired("org.apache.felix.hc.api");
    }

    public boolean isConditionWired() {
        return isWired("org.osgi.service.condition");
    }

    public boolean isRemoteServiceAdminWired() {
        return isWired("org.osgi.service.remoteserviceadmin");
    }

    public boolean isGogoWired() {
        return isWired("org.apache.felix.gogo.runtime");
    }

    /**
     * Dispatches to the cache-backed individual {@code isXxxWired()} methods.
     * Keeps the existing lazy-caching behaviour for all internal callers.
     */
    public boolean isWiredByType(final Type type) {
        switch (type) {
            case SCR:
                return isScrWired();
            case CM:
                return isConfigAdminWired();
            case DMT:
                return isDmtAdminWired();
            case USER_ADMIN:
                return isUserAdminWired();
            case METATYPE:
                return isMetatypeWired();
            case EVENT_ADMIN:
                return isEventAdminWired();
            case LOG:
                return isLogWired();
            case R7_LOGGER:
                return isR7LoggerAdminWired();
            case HTTP:
                return isHttpServiceRuntimeWired();
            case JAX_RS:
                return isJaxRsWired();
            case CDI:
                return isCDIWired();
            case JMX:
                return isJmxWired();
            case HC:
                return isFelixHcWired();
            case CONDITION:
                return isConditionWired();
            case RSA:
                return isRemoteServiceAdminWired();
            case GOGO:
                return isGogoWired();
            default:
                return false;
        }
    }

    /**
     * Bypasses the lazy cache and walks the live framework wires directly.
     * <p>
     * Used exclusively by {@code AgentServer.getRuntimeCapabilities()} so that a
     * fresh snapshot is returned after a bundle install/uninstall <em>without</em>
     * invalidating the shared cache for all other callers.
     * </p>
     */
    public boolean isWiredFresh(final Type type) {
        final String pkg = packageNameFor(type);
        return pkg != null && checkWiring(pkg);
    }

    private String packageNameFor(final Type type) {
        switch (type) {
            case SCR:
                return "org.osgi.service.component.runtime";
            case CM:
                return "org.osgi.service.cm";
            case DMT:
                return "org.osgi.service.dmt";
            case USER_ADMIN:
                return "org.osgi.service.useradmin";
            case METATYPE:
                return "org.osgi.service.metatype";
            case EVENT_ADMIN:
                return "org.osgi.service.event";
            case LOG:
                return "org.osgi.service.log";
            case R7_LOGGER:
                return "org.osgi.service.log.admin";
            case HTTP:
                return "org.osgi.service.http.runtime";
            case JAX_RS:
                return "org.osgi.service.jaxrs.runtime";
            case CDI:
                return "org.osgi.service.cdi.runtime";
            case JMX:
                return "javax.management";
            case HC:
                return "org.apache.felix.hc.api";
            case CONDITION:
                return "org.osgi.service.condition";
            case RSA:
                return "org.osgi.service.remoteserviceadmin";
            case GOGO:
                return "org.apache.felix.gogo.runtime";
            default:
                return null;
        }
    }

}
