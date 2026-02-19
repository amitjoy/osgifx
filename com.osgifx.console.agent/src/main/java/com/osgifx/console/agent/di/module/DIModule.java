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
package com.osgifx.console.agent.di.module;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Stream;

import org.osgi.dto.DTO;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import com.osgifx.console.agent.admin.XBundleAdmin;
import com.osgifx.console.agent.admin.XComponentAdmin;
import com.osgifx.console.agent.admin.XConfigurationAdmin;
import com.osgifx.console.agent.admin.XDmtAdmin;
import com.osgifx.console.agent.admin.XDtoAdmin;
import com.osgifx.console.agent.admin.XEventAdmin;
import com.osgifx.console.agent.admin.XHcAdmin;
import com.osgifx.console.agent.admin.XHttpAdmin;
import com.osgifx.console.agent.admin.XLoggerAdmin;
import com.osgifx.console.agent.admin.XMetaTypeAdmin;
import com.osgifx.console.agent.admin.XServiceAdmin;
import com.osgifx.console.agent.admin.XUserAdmin;
import com.osgifx.console.agent.di.DI;
import com.osgifx.console.agent.extension.AgentExtension;
import com.osgifx.console.agent.provider.BundleStartTimeCalculator;
import com.osgifx.console.agent.provider.PackageWirings;

@SuppressWarnings("rawtypes")
public final class DIModule {

    private static final String GOGO_COMMAND_SCOPE    = "osgi.command.scope";
    private static final String GOGO_COMMAND_FUNCTION = "osgi.command.function";

    private final DI            di;
    private final BundleContext context;

    private ServiceTracker<Object, Object>                 dmtAdminTracker;
    private ServiceTracker<Object, Object>                 userAdminTracker;
    private ServiceTracker<Object, Object>                 eventAdminTracker;
    private ServiceTracker<Object, Object>                 loggerAdminTracker;
    private ServiceTracker<Object, Object>                 gogoCommandsTracker;
    private ServiceTracker<Object, Object>                 felixHcExecutorTracker;
    private ServiceTracker<Object, Object>                 cdiServiceRuntimeTracker;
    private ServiceTracker<Object, Object>                 httpServiceRuntimeTracker;
    private ServiceTracker<Object, Object>                 jaxrsServiceRuntimeTracker;
    private ServiceTracker<AgentExtension, AgentExtension> agentExtensionTracker;

    private final Set<String>                           gogoCommands    = new CopyOnWriteArraySet<>();
    private final Map<String, AgentExtension<DTO, DTO>> agentExtensions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService              executor        = Executors
            .newScheduledThreadPool(Runtime.getRuntime().availableProcessors());

    private BundleStartTimeCalculator bundleStartTimeCalculator;
    private XBundleAdmin              xBundleAdmin;
    private XServiceAdmin             xServiceAdmin;
    private Object                    xConfigurationAdmin;
    private Object                    xComponentAdmin;
    private Object                    xMetaTypeAdmin;

    public DIModule(final BundleContext context) {
        di           = new DI();
        this.context = context;

        di.bindInstance(DI.class, di);
        di.bindInstance(BundleContext.class, context);
    }

    public void start() throws Exception {
        initServiceTrackers();

        final PackageWirings wirings = di.getInstance(PackageWirings.class);

        bundleStartTimeCalculator = new BundleStartTimeCalculator(context);
        di.bindInstance(BundleStartTimeCalculator.class, bundleStartTimeCalculator);

        // --- Eagerly instantiated admins (guarded by PackageWirings) ---

        if (wirings.isScrWired()) {
            xComponentAdmin = new XComponentAdmin(context, executor);
            di.bindInstance(XComponentAdmin.class, (XComponentAdmin) xComponentAdmin);
        }

        xBundleAdmin = new XBundleAdmin(context, bundleStartTimeCalculator, executor);
        di.bindInstance(XBundleAdmin.class, xBundleAdmin);

        xServiceAdmin = new XServiceAdmin(context, executor);
        di.bindInstance(XServiceAdmin.class, xServiceAdmin);

        if (wirings.isMetatypeWired() && wirings.isConfigAdminWired()) {
            xMetaTypeAdmin = new XMetaTypeAdmin(context, executor);
            di.bindInstance(XMetaTypeAdmin.class, (XMetaTypeAdmin) xMetaTypeAdmin);
        }

        if (wirings.isConfigAdminWired()) {
            xConfigurationAdmin = new XConfigurationAdmin(context, (XComponentAdmin) xComponentAdmin,
                                                          (XMetaTypeAdmin) xMetaTypeAdmin, executor);
            di.bindInstance(XConfigurationAdmin.class, (XConfigurationAdmin) xConfigurationAdmin);
        }

        // initialize the trackers
        xBundleAdmin.init();
        xServiceAdmin.init();
        if (xComponentAdmin != null) {
            ((XComponentAdmin) xComponentAdmin).init();
        }
        if (xConfigurationAdmin != null) {
            ((XConfigurationAdmin) xConfigurationAdmin).init();
        }
        if (xMetaTypeAdmin != null) {
            ((XMetaTypeAdmin) xMetaTypeAdmin).init();
        }

        // --- Lazily bound admins (guarded by PackageWirings) ---

        if (wirings.isDmtAdminWired()) {
            di.bindProvider(XDmtAdmin.class, () -> new XDmtAdmin(() -> dmtAdminTracker.getService()));
        }
        if (wirings.isScrWired()) {
            di.bindProvider(XDtoAdmin.class,
                    () -> new XDtoAdmin(context, () -> ((XComponentAdmin) xComponentAdmin).getServiceComponentRuntime(),
                                        () -> jaxrsServiceRuntimeTracker.getService(),
                                        () -> httpServiceRuntimeTracker.getService(),
                                        () -> cdiServiceRuntimeTracker.getService(), wirings));
        }
        if (wirings.isEventAdminWired()) {
            di.bindProvider(XEventAdmin.class, () -> new XEventAdmin(() -> eventAdminTracker.getService()));
        }
        if (wirings.isFelixHcWired()) {
            di.bindProvider(XHcAdmin.class, () -> new XHcAdmin(context, () -> felixHcExecutorTracker.getService()));
        }
        if (wirings.isHttpServiceRuntimeWired()) {
            di.bindProvider(XHttpAdmin.class, () -> new XHttpAdmin(() -> httpServiceRuntimeTracker.getService()));
        }
        if (wirings.isUserAdminWired()) {
            di.bindProvider(XUserAdmin.class, () -> new XUserAdmin(() -> userAdminTracker.getService()));
        }
        if (wirings.isR7LoggerAdminWired()) {
            di.bindProvider(XLoggerAdmin.class,
                    () -> new XLoggerAdmin(() -> loggerAdminTracker.getService(), wirings, context));
        }
        di.bindInstance(Set.class, gogoCommands);
        di.bindInstance(Map.class, agentExtensions);
    }

    public void stop() {
        if (xComponentAdmin != null) {
            ((XComponentAdmin) xComponentAdmin).stop();
        }
        if (xBundleAdmin != null) {
            xBundleAdmin.stop();
        }
        if (xServiceAdmin != null) {
            xServiceAdmin.stop();
        }
        if (xConfigurationAdmin != null) {
            ((XConfigurationAdmin) xConfigurationAdmin).stop();
        }
        if (xMetaTypeAdmin != null) {
            ((XMetaTypeAdmin) xMetaTypeAdmin).stop();
        }
        dmtAdminTracker.close();
        userAdminTracker.close();
        loggerAdminTracker.close();
        eventAdminTracker.close();
        gogoCommandsTracker.close();
        agentExtensionTracker.close();
        felixHcExecutorTracker.close();
        cdiServiceRuntimeTracker.close();
        httpServiceRuntimeTracker.close();
        jaxrsServiceRuntimeTracker.close();
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    public DI di() {
        return di;
    }

    public <T> void bindInstance(final Class<T> classType, final T instance) {
        di.bindInstance(classType, instance);
    }

    private void initServiceTrackers() throws InvalidSyntaxException {
        final Filter gogoCommandFilter = context.createFilter("(osgi.command.scope=*)");

        dmtAdminTracker            = new ServiceTracker<>(context, "org.osgi.service.dmt.DmtAdmin", null);
        userAdminTracker           = new ServiceTracker<>(context, "org.osgi.service.useradmin.UserAdmin", null);
        loggerAdminTracker         = new ServiceTracker<>(context, "org.osgi.service.log.admin.LoggerAdmin", null);
        eventAdminTracker          = new ServiceTracker<>(context, "org.osgi.service.event.EventAdmin", null);
        felixHcExecutorTracker     = new ServiceTracker<>(context,
                                                          "org.apache.felix.hc.api.execution.HealthCheckExecutor",
                                                          null);
        cdiServiceRuntimeTracker   = new ServiceTracker<>(context, "org.osgi.service.cdi.runtime.CDIComponentRuntime",
                                                          null);
        httpServiceRuntimeTracker  = new ServiceTracker<>(context, "org.osgi.service.http.runtime.HttpServiceRuntime",
                                                          null);
        jaxrsServiceRuntimeTracker = new ServiceTracker<>(context, "org.osgi.service.jaxrs.runtime.JaxrsServiceRuntime",
                                                          null);
        agentExtensionTracker      = new ServiceTracker<AgentExtension, AgentExtension>(context, AgentExtension.class,
                                                                                        null) {
                                       @Override
                                       @SuppressWarnings("unchecked")
                                       public AgentExtension addingService(final ServiceReference<AgentExtension> reference) {
                                           final Object name = reference.getProperty(AgentExtension.PROPERTY_KEY);
                                           if (name == null) {
                                               return null;
                                           }
                                           final AgentExtension tracked = super.addingService(reference);
                                           agentExtensions.put(name.toString(), tracked);
                                           return tracked;
                                       }

                                       @Override
                                       public void modifiedService(final ServiceReference<AgentExtension> reference,
                                                                   final AgentExtension service) {
                                           removedService(reference, service);
                                           addingService(reference);
                                       }

                                       @Override
                                       public void removedService(final ServiceReference<AgentExtension> reference,
                                                                  final AgentExtension service) {
                                           final Object name = reference.getProperty(AgentExtension.PROPERTY_KEY);
                                           if (name == null) {
                                               return;
                                           }
                                           agentExtensions.remove(name);
                                       }
                                   };
        gogoCommandsTracker        = new ServiceTracker<Object, Object>(context, gogoCommandFilter, null) {

                                       @Override
                                       public Object addingService(final ServiceReference<Object> reference) {
                                           final String   scope     = String
                                                   .valueOf(reference.getProperty(GOGO_COMMAND_SCOPE));
                                           final String[] functions = adapt(
                                                   reference.getProperty(GOGO_COMMAND_FUNCTION));
                                           addCommand(scope, functions);
                                           return super.addingService(reference);
                                       }

                                       @Override
                                       public void removedService(final ServiceReference<Object> reference,
                                                                  final Object service) {
                                           final String   scope     = String
                                                   .valueOf(reference.getProperty(GOGO_COMMAND_SCOPE));
                                           final String[] functions = adapt(
                                                   reference.getProperty(GOGO_COMMAND_FUNCTION));
                                           removeCommand(scope, functions);
                                       }

                                       private String[] adapt(final Object value) {
                                           if (value instanceof String[]) {
                                               return (String[]) value;
                                           }
                                           return new String[] { value.toString() };
                                       }

                                       private void addCommand(final String scope, final String... commands) {
                                           Stream.of(commands).forEach(cmd -> gogoCommands.add(scope + ":" + cmd));
                                       }

                                       private void removeCommand(final String scope, final String... commands) {
                                           Stream.of(commands).forEach(cmd -> gogoCommands.remove(scope + ":" + cmd));
                                       }
                                   };

        dmtAdminTracker.open();
        userAdminTracker.open();
        loggerAdminTracker.open();
        eventAdminTracker.open();
        gogoCommandsTracker.open();
        agentExtensionTracker.open();
        felixHcExecutorTracker.open();
        cdiServiceRuntimeTracker.open();
        httpServiceRuntimeTracker.open();
        jaxrsServiceRuntimeTracker.open();
    }

}
