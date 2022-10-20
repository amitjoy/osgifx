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
package com.osgifx.console.agent.di;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Stream;

import org.osgi.dto.DTO;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import com.osgifx.console.agent.admin.XComponentAdmin;
import com.osgifx.console.agent.admin.XConfigurationAdmin;
import com.osgifx.console.agent.admin.XDmtAdmin;
import com.osgifx.console.agent.admin.XEventAdmin;
import com.osgifx.console.agent.admin.XHcAdmin;
import com.osgifx.console.agent.admin.XHttpAdmin;
import com.osgifx.console.agent.admin.XLoggerAdmin;
import com.osgifx.console.agent.admin.XMetaTypeAdmin;
import com.osgifx.console.agent.admin.XUserAdmin;
import com.osgifx.console.agent.extension.AgentExtension;
import com.osgifx.console.agent.provider.PackageWirings;

import eu.lestard.easydi.EasyDI;

@SuppressWarnings("rawtypes")
public final class Module {

    private final EasyDI        di;
    private final BundleContext context;

    private ServiceTracker<Object, Object>                 scrTracker;
    private ServiceTracker<Object, Object>                 metatypeTracker;
    private ServiceTracker<Object, Object>                 dmtAdminTracker;
    private ServiceTracker<Object, Object>                 userAdminTracker;
    private ServiceTracker<Object, Object>                 eventAdminTracker;
    private ServiceTracker<Object, Object>                 loggerAdminTracker;
    private ServiceTracker<Object, Object>                 configAdminTracker;
    private ServiceTracker<Object, Object>                 gogoCommandsTracker;
    private ServiceTracker<Object, Object>                 felixHcExecutorTracker;
    private ServiceTracker<Object, Object>                 httpServiceRuntimeTracker;
    private ServiceTracker<AgentExtension, AgentExtension> agentExtensionTracker;

    private final Set<String>                           gogoCommands    = new CopyOnWriteArraySet<>();
    private final Map<String, AgentExtension<DTO, DTO>> agentExtensions = new ConcurrentHashMap<>();

    public Module(final BundleContext context) {
        di           = new EasyDI();
        this.context = context;

        di.bindInstance(EasyDI.class, di);
        di.bindInstance(BundleContext.class, context);
    }

    public void start() throws Exception {
        initServiceTrackers();

        di.bindProvider(XComponentAdmin.class, () -> new XComponentAdmin(scrTracker.getService()));
        di.bindProvider(XConfigurationAdmin.class,
                () -> new XConfigurationAdmin(context, configAdminTracker.getService(), metatypeTracker.getService()));
        di.bindProvider(XDmtAdmin.class, () -> new XDmtAdmin(dmtAdminTracker.getService()));
        di.bindProvider(XEventAdmin.class, () -> new XEventAdmin(eventAdminTracker.getService()));
        di.bindProvider(XHcAdmin.class, () -> new XHcAdmin(context, felixHcExecutorTracker.getService()));
        di.bindProvider(XHttpAdmin.class, () -> new XHttpAdmin(httpServiceRuntimeTracker.getService()));
        di.bindProvider(XMetaTypeAdmin.class,
                () -> new XMetaTypeAdmin(context, configAdminTracker.getService(), metatypeTracker.getService()));
        di.bindProvider(XUserAdmin.class, () -> new XUserAdmin(userAdminTracker.getService()));
        di.bindProvider(XLoggerAdmin.class,
                () -> new XLoggerAdmin(loggerAdminTracker.getService(), di.getInstance(PackageWirings.class), context));
        di.bindInstance(Set.class, gogoCommands);
        di.bindInstance(Map.class, agentExtensions);
    }

    public void stop() {
        scrTracker.close();
        metatypeTracker.close();
        dmtAdminTracker.close();
        userAdminTracker.close();
        loggerAdminTracker.close();
        eventAdminTracker.close();
        configAdminTracker.close();
        gogoCommandsTracker.close();
        agentExtensionTracker.close();
        felixHcExecutorTracker.close();
        httpServiceRuntimeTracker.close();
    }

    public EasyDI di() {
        return di;
    }

    public <T> void bindInstance(final Class<T> classType, final T instance) {
        di.bindInstance(classType, instance);
    }

    private void initServiceTrackers() throws InvalidSyntaxException {
        final Filter gogoCommandFilter = context.createFilter("(osgi.command.scope=*)");

        metatypeTracker           = new ServiceTracker<>(context, "org.osgi.service.metatype.MetaTypeService", null);
        dmtAdminTracker           = new ServiceTracker<>(context, "org.osgi.service.dmt.DmtAdmin", null);
        userAdminTracker          = new ServiceTracker<>(context, "org.osgi.service.useradmin.UserAdmin", null);
        loggerAdminTracker        = new ServiceTracker<>(context, "org.osgi.service.log.admin.LoggerAdmin", null);
        eventAdminTracker         = new ServiceTracker<>(context, "org.osgi.service.event.EventAdmin", null);
        configAdminTracker        = new ServiceTracker<>(context, "org.osgi.service.cm.ConfigurationAdmin", null);
        felixHcExecutorTracker    = new ServiceTracker<>(context, "org.apache.felix.hc.api.execution.HealthCheckExecutor", null);
        scrTracker                = new ServiceTracker<>(context, "org.osgi.service.component.runtime.ServiceComponentRuntime", null);
        httpServiceRuntimeTracker = new ServiceTracker<>(context, "org.osgi.service.http.runtime.HttpServiceRuntime", null);
        agentExtensionTracker     = new ServiceTracker<AgentExtension, AgentExtension>(context, AgentExtension.class, null) {

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
        gogoCommandsTracker       = new ServiceTracker<Object, Object>(context, gogoCommandFilter, null) {
                                      @Override
                                      public Object addingService(final ServiceReference<Object> reference) {
                                          final String   scope     = String.valueOf(reference.getProperty("osgi.command.scope"));
                                          final String[] functions = adapt(reference.getProperty("osgi.command.function"));
                                          addCommand(scope, functions);
                                          return super.addingService(reference);
                                      }

                                      @Override
                                      public void removedService(final ServiceReference<Object> reference, final Object service) {
                                          final String   scope     = String.valueOf(reference.getProperty("osgi.command.scope"));
                                          final String[] functions = adapt(reference.getProperty("osgi.command.function"));
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

        scrTracker.open();
        metatypeTracker.open();
        dmtAdminTracker.open();
        userAdminTracker.open();
        loggerAdminTracker.open();
        eventAdminTracker.open();
        configAdminTracker.open();
        gogoCommandsTracker.open();
        agentExtensionTracker.open();
        felixHcExecutorTracker.open();
        httpServiceRuntimeTracker.open();
    }

}
