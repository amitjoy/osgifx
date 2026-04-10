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

import com.osgifx.console.agent.admin.RemoteServiceAdminManager;
import com.osgifx.console.agent.admin.XBundleAdmin;
import com.osgifx.console.agent.admin.XCdiAdmin;
import com.osgifx.console.agent.admin.XComponentAdmin;
import com.osgifx.console.agent.admin.XConditionAdmin;
import com.osgifx.console.agent.admin.XConfigurationAdmin;
import com.osgifx.console.agent.admin.XDmtAdmin;
import com.osgifx.console.agent.admin.XDtoAdmin;
import com.osgifx.console.agent.admin.XEventAdmin;
import com.osgifx.console.agent.admin.XHcAdmin;
import com.osgifx.console.agent.admin.XHeapDumpAdmin;
import com.osgifx.console.agent.admin.XHttpAdmin;
import com.osgifx.console.agent.admin.XJaxRsAdmin;
import com.osgifx.console.agent.admin.XJmxAdmin;
import com.osgifx.console.agent.admin.XLogReaderAdmin;
import com.osgifx.console.agent.admin.XLoggerAdmin;
import com.osgifx.console.agent.admin.XMetaTypeAdmin;
import com.osgifx.console.agent.admin.XPropertyAdmin;
import com.osgifx.console.agent.admin.XServiceAdmin;
import com.osgifx.console.agent.admin.XSnapshotAdmin;
import com.osgifx.console.agent.admin.XThreadAdmin;
import com.osgifx.console.agent.admin.XThreadDumpAdmin;
import com.osgifx.console.agent.admin.XUserAdmin;
import com.osgifx.console.agent.di.DI;
import com.osgifx.console.agent.provider.BundleStartTimeCalculator;
import com.osgifx.console.agent.provider.PackageWirings;
import com.osgifx.console.agent.rpc.codec.BinaryCodec;
import com.osgifx.console.agent.rpc.codec.Lz4Codec;
import com.osgifx.console.agent.rpc.codec.SnapshotDecoder;
import com.osgifx.console.agent.spi.extension.AgentExtension;

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
    private XConfigurationAdmin       xConfigurationAdmin;
    private XComponentAdmin           xComponentAdmin;
    private XConditionAdmin           xConditionAdmin;
    private XMetaTypeAdmin            xMetaTypeAdmin;
    private XPropertyAdmin            xPropertyAdmin;
    private XHcAdmin                  xHcAdmin;
    private XHttpAdmin                xHttpAdmin;
    private XCdiAdmin                 xCdiAdmin;
    private XJaxRsAdmin               xJaxRsAdmin;
    private XUserAdmin                xUserAdmin;
    private XLoggerAdmin              xLoggerAdmin;
    private XLogReaderAdmin           xLogReaderAdmin;
    private XJmxAdmin                 xJmxAdmin;
    private XThreadAdmin              xThreadAdmin;
    private RemoteServiceAdminManager remoteServiceAdminManager;

    public DIModule(final BundleContext context) {
        di           = new DI();
        this.context = context;

        di.bindInstance(DI.class, di);
        di.bindInstance(BundleContext.class, context);
        final BinaryCodec codec = new BinaryCodec(context);
        di.bindInstance(BinaryCodec.class, codec);
        di.bindInstance(SnapshotDecoder.class, new SnapshotDecoder(codec));
    }

    public void start() throws Exception {
        initServiceTrackers();

        final PackageWirings wirings = di.getInstance(PackageWirings.class);

        bundleStartTimeCalculator = new BundleStartTimeCalculator(context);
        di.bindInstance(BundleStartTimeCalculator.class, bundleStartTimeCalculator);

        // --- Eagerly instantiated admins (guarded by PackageWirings) ---

        final BinaryCodec     codec   = di.getInstance(BinaryCodec.class);
        final SnapshotDecoder decoder = di.getInstance(SnapshotDecoder.class);

        if (wirings.isScrWired()) {
            xComponentAdmin = new XComponentAdmin(context, codec, decoder, executor);
            di.bindInstance(XComponentAdmin.class, xComponentAdmin);
        }

        xBundleAdmin = new XBundleAdmin(context, bundleStartTimeCalculator, codec, decoder, executor);
        di.bindInstance(XBundleAdmin.class, xBundleAdmin);

        xServiceAdmin = new XServiceAdmin(context, codec, decoder, executor);
        di.bindInstance(XServiceAdmin.class, xServiceAdmin);

        if (wirings.isMetatypeWired() && wirings.isConfigAdminWired()) {
            xMetaTypeAdmin = new XMetaTypeAdmin(context, executor);
            di.bindInstance(XMetaTypeAdmin.class, xMetaTypeAdmin);
        }

        if (wirings.isConditionWired()) {
            xConditionAdmin = new XConditionAdmin(context, xComponentAdmin, codec, decoder, executor);
            di.bindInstance(XConditionAdmin.class, xConditionAdmin);
        }

        if (wirings.isConfigAdminWired()) {
            xConfigurationAdmin = new XConfigurationAdmin(context, xComponentAdmin, xMetaTypeAdmin, wirings, codec,
                                                          decoder, executor);
            di.bindInstance(XConfigurationAdmin.class, xConfigurationAdmin);
        }

        xPropertyAdmin = new XPropertyAdmin(context, codec, decoder, executor);
        di.bindInstance(XPropertyAdmin.class, xPropertyAdmin);

        if (wirings.isFelixHcWired()) {
            xHcAdmin = new XHcAdmin(context, codec, decoder, () -> felixHcExecutorTracker.getService(), executor);
            di.bindInstance(XHcAdmin.class, xHcAdmin);
        }

        if (wirings.isRemoteServiceAdminWired()) {
            remoteServiceAdminManager = new RemoteServiceAdminManager(context, codec, decoder, executor);
            di.bindInstance(RemoteServiceAdminManager.class, remoteServiceAdminManager);
        }

        if (wirings.isJaxRsWired()) {
            xJaxRsAdmin = new XJaxRsAdmin(context, () -> jaxrsServiceRuntimeTracker.getService(), codec, decoder,
                                          executor);
            di.bindInstance(XJaxRsAdmin.class, xJaxRsAdmin);
        }

        if (wirings.isHttpServiceRuntimeWired()) {
            xHttpAdmin = new XHttpAdmin(context, () -> httpServiceRuntimeTracker.getService(), codec, decoder,
                                        executor);
            di.bindInstance(XHttpAdmin.class, xHttpAdmin);
        }

        if (wirings.isCDIWired()) {
            xCdiAdmin = new XCdiAdmin(context, () -> cdiServiceRuntimeTracker.getService(), codec, decoder, executor);
            di.bindInstance(XCdiAdmin.class, xCdiAdmin);
        }

        if (wirings.isUserAdminWired()) {
            xUserAdmin = new XUserAdmin(context, () -> userAdminTracker.getService(), codec, decoder, executor);
            di.bindInstance(XUserAdmin.class, xUserAdmin);
        }

        if (wirings.isR7LoggerAdminWired()) {
            xLoggerAdmin = new XLoggerAdmin(() -> loggerAdminTracker.getService(), wirings, context, codec, decoder,
                                            executor);
            di.bindInstance(XLoggerAdmin.class, xLoggerAdmin);
        }

        if (wirings.isLogWired()) {
            xLogReaderAdmin = new XLogReaderAdmin();
            di.bindInstance(XLogReaderAdmin.class, xLogReaderAdmin);
        }

        if (wirings.isJmxWired()) {
            xJmxAdmin = new XJmxAdmin();
            di.bindInstance(XJmxAdmin.class, xJmxAdmin);

            di.bindInstance(XHeapDumpAdmin.class, new XHeapDumpAdmin(context));
        }

        xThreadAdmin = new XThreadAdmin(wirings, codec, decoder, executor);
        di.bindInstance(XThreadAdmin.class, xThreadAdmin);

        di.bindInstance(XThreadDumpAdmin.class, new XThreadDumpAdmin(context));

        di.bindProvider(XSnapshotAdmin.class, () -> new XSnapshotAdmin(context, di.getInstance(XDtoAdmin.class)));

        // initialize the trackers
        xBundleAdmin.init();
        xServiceAdmin.init();
        if (xComponentAdmin != null) {
            xComponentAdmin.init();
        }
        if (xConditionAdmin != null) {
            xConditionAdmin.init();
        }
        if (xMetaTypeAdmin != null) {
            xMetaTypeAdmin.init();
        }
        if (xConfigurationAdmin != null) {
            xConfigurationAdmin.init();
        }
        if (xHcAdmin != null) {
            xHcAdmin.init();
        }
        if (xJaxRsAdmin != null) {
            xJaxRsAdmin.init();
        }
        if (xHttpAdmin != null) {
            xHttpAdmin.init();
        }
        if (xCdiAdmin != null) {
            xCdiAdmin.init();
        }
        if (xUserAdmin != null) {
            xUserAdmin.init();
        }
        if (xLoggerAdmin != null) {
            xLoggerAdmin.init();
        }
        if (xLogReaderAdmin != null) {
            // XLogReaderAdmin currently has no init()
        }
        if (xJmxAdmin != null) {
            xJmxAdmin.init();
        }
        if (remoteServiceAdminManager != null) {
            remoteServiceAdminManager.init();
        }
        xThreadAdmin.init();
        xPropertyAdmin.init();

        // --- Lazily bound admins (guarded by PackageWirings) ---

        if (wirings.isDmtAdminWired()) {
            di.bindProvider(XDmtAdmin.class, () -> new XDmtAdmin(() -> dmtAdminTracker.getService()));
        }
        if (wirings.isScrWired()) {
            di.bindProvider(XDtoAdmin.class,
                    () -> new XDtoAdmin(context, () -> xComponentAdmin.getServiceComponentRuntime(),
                                        () -> jaxrsServiceRuntimeTracker.getService(),
                                        () -> httpServiceRuntimeTracker.getService(),
                                        () -> cdiServiceRuntimeTracker.getService(), wirings));
        }
        if (wirings.isEventAdminWired()) {
            di.bindProvider(XEventAdmin.class, () -> new XEventAdmin(() -> eventAdminTracker.getService()));
        }
        di.bindInstance(Set.class, gogoCommands);
        di.bindInstance(Map.class, agentExtensions);
    }

    public void stop() {
        if (xComponentAdmin != null) {
            xComponentAdmin.stop();
        }
        if (xBundleAdmin != null) {
            xBundleAdmin.stop();
        }
        if (xServiceAdmin != null) {
            xServiceAdmin.stop();
        }
        if (xConditionAdmin != null) {
            xConditionAdmin.stop();
        }
        if (xConfigurationAdmin != null) {
            xConfigurationAdmin.stop();
        }
        if (xMetaTypeAdmin != null) {
            xMetaTypeAdmin.stop();
        }
        if (xHcAdmin != null) {
            xHcAdmin.stop();
        }
        if (xJaxRsAdmin != null) {
            xJaxRsAdmin.stop();
        }
        if (xHttpAdmin != null) {
            xHttpAdmin.stop();
        }
        if (xCdiAdmin != null) {
            xCdiAdmin.stop();
        }
        if (xUserAdmin != null) {
            xUserAdmin.stop();
        }
        if (xLoggerAdmin != null) {
            xLoggerAdmin.stop();
        }
        if (xPropertyAdmin != null) {
            xPropertyAdmin.stop();
        }
        if (remoteServiceAdminManager != null) {
            remoteServiceAdminManager.stop();
        }
        if (xThreadAdmin != null) {
            xThreadAdmin.stop();
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
        BinaryCodec.clearCache();
        Lz4Codec.clear();
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
