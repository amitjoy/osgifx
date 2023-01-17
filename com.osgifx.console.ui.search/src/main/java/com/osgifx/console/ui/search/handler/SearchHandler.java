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
package com.osgifx.console.ui.search.handler;

import static com.osgifx.console.event.topics.TableFilterUpdateTopics.UPDATE_BUNDLE_FILTER_EVENT_TOPIC;
import static com.osgifx.console.event.topics.TableFilterUpdateTopics.UPDATE_COMPONENT_FILTER_EVENT_TOPIC;
import static com.osgifx.console.event.topics.TableFilterUpdateTopics.UPDATE_CONFIGURATION_FILTER_EVENT_TOPIC;
import static com.osgifx.console.event.topics.TableFilterUpdateTopics.UPDATE_PACKAGE_FILTER_EVENT_TOPIC;
import static com.osgifx.console.event.topics.TableFilterUpdateTopics.UPDATE_SERVICE_FILTER_EVENT_TOPIC;
import static org.eclipse.e4.ui.workbench.modeling.EPartService.PartState.ACTIVATE;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.osgifx.console.ui.search.dialog.SearchDialog;

public final class SearchHandler {

    @Log
    @Inject
    private FluentLogger    logger;
    @Inject
    private IEclipseContext context;
    @Inject
    @Optional
    @Named("is_connected")
    private boolean         isConnected;
    @Inject
    private IEventBroker    eventBroker;
    @Inject
    @Optional
    private EPartService    partService;

    @Execute
    public void execute() {
        final var dialog = new SearchDialog();
        ContextInjectionFactory.inject(dialog, context);
        logger.atInfo().log("Injected search dialog to eclipse context");
        dialog.init();

        final var search = dialog.showAndWait();
        if (search.isPresent()) {
            final var filterDTO = search.get();
            switch (filterDTO.component()) {
                case BUNDLES:
                    partService.showPart("com.osgifx.console.application.tab.bundles", ACTIVATE);
                    eventBroker.post(UPDATE_BUNDLE_FILTER_EVENT_TOPIC, filterDTO.filter());
                    break;
                case COMPONENTS:
                    partService.showPart("com.osgifx.console.application.tab.components", ACTIVATE);
                    eventBroker.post(UPDATE_COMPONENT_FILTER_EVENT_TOPIC, filterDTO.filter());
                    break;
                case CONFIGURATIONS:
                    partService.showPart("com.osgifx.console.application.tab.configurations", ACTIVATE);
                    eventBroker.post(UPDATE_CONFIGURATION_FILTER_EVENT_TOPIC, filterDTO.filter());
                    break;
                case SERVICES:
                    partService.showPart("com.osgifx.console.application.tab.services", ACTIVATE);
                    eventBroker.post(UPDATE_SERVICE_FILTER_EVENT_TOPIC, filterDTO.filter());
                    break;
                case PACKAGES:
                    partService.showPart("com.osgifx.console.application.tab.packages", ACTIVATE);
                    eventBroker.post(UPDATE_PACKAGE_FILTER_EVENT_TOPIC, filterDTO.filter());
                    break;
                default:
                    break;
            }
        }
    }

    @CanExecute
    public boolean canExecute() {
        return isConnected;
    }

}
