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
package com.osgifx.console.application.handler;

import static org.osgi.service.condition.Condition.CONDITION_ID;
import static org.osgi.service.condition.Condition.INSTANCE;

import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.aries.component.dsl.OSGi;
import org.apache.aries.component.dsl.OSGiResult;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.e4.ui.di.AboutToShow;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.model.application.ui.menu.MDirectMenuItem;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuElement;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.framework.BundleContext;
import org.osgi.service.condition.Condition;

import com.osgifx.console.executor.Executor;
import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.util.fx.Fx;

public final class McpMenuContributionHandler {

    @Log
    @Inject
    private FluentLogger            logger;
    @Inject
    private Executor                executor;
    @Inject
    @Optional
    private Supervisor              supervisor;
    @Inject
    private ThreadSynchronize       threadSync;
    @Inject
    private EModelService           modelService;
    @Inject
    @Named("is_connected")
    private boolean                 isConnected;
    @Inject
    @OSGiBundle
    private BundleContext           bundleContext;
    private static OSGiResult       mcpResult;
    private final BooleanSupplier   isStartedMCP        = () -> Boolean.getBoolean("is_started_mcp");
    private final Consumer<Boolean> isStartedMcpUpdater = flag -> System.setProperty("is_started_mcp",
            String.valueOf(flag));

    @AboutToShow
    public void aboutToShow(final List<MMenuElement> items, final MWindow window) {
        prepareMenu(items, isStartedMCP.getAsBoolean());
    }

    @Execute
    public void execute(final MDirectMenuItem menuItem) {
        if (supervisor == null || (supervisor.getAgent() == null)) {
            logger.atInfo().log("Agent not connected");
            return;
        }
        final var flag = !isStartedMCP.getAsBoolean();
        if (flag) {
            // @formatter:off
            executor.runAsync(this::registerMcpCondition)
                    .thenRun(() -> threadSync.asyncExec(() -> {
                                            Fx.showSuccessNotification("Model Context Protocol", "MCP server has been started");
                                            })
                            )
                    .thenRun(() -> isStartedMcpUpdater.accept(true));
        } else {
            executor.runAsync(this::deregisterMcpCondition)
                    .thenRun(() -> threadSync.asyncExec(() -> {
                                            Fx.showSuccessNotification("Model Context Protocol", "MCP server has been stopped");
                                            })
                            )
                    .thenRun(() -> isStartedMcpUpdater.accept(false));
            // @formatter:on
        }
    }

    @CanExecute
    public boolean canExecute() {
        return isConnected;
    }

    private void prepareMenu(final List<MMenuElement> items, final boolean value) {
        final MDirectMenuItem eventActionMenu;
        if (value) {
            eventActionMenu = createMcpActionMenu(Type.STOP);
        } else {
            eventActionMenu = createMcpActionMenu(Type.START);
        }
        items.add(eventActionMenu);
    }

    private MDirectMenuItem createMcpActionMenu(final Type type) {
        String label;
        String icon;
        if (type == Type.STOP) {
            label = "Stop MCP Server";
            icon  = "stop.png";
        } else {
            label = "Start MCP Server";
            icon  = "start.png";
        }
        final var dynamicItem = modelService.createModelElement(MDirectMenuItem.class);
        final var bsn         = bundleContext.getBundle().getSymbolicName();

        dynamicItem.setLabel(label);
        dynamicItem.setIconURI("platform:/plugin/" + bsn + "/graphic/icons/" + icon);
        dynamicItem.setContributorURI("platform:/plugin/" + bsn);
        dynamicItem.setContributionURI("bundleclass://" + bsn + "/" + getClass().getName());

        return dynamicItem;
    }

    private void registerMcpCondition() {
        mcpResult = OSGi.register(Condition.class, INSTANCE, Map.of(CONDITION_ID, "osgi.fx.mcp")).run(bundleContext);
    }

    private void deregisterMcpCondition() {
        if (mcpResult != null) {
            mcpResult.close();
            mcpResult = null;
        }
    }

    private enum Type {
        START,
        STOP
    }

}
