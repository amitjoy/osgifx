/*******************************************************************************
 * Copyright 2022 Amit Kumar Mondal
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
package com.osgifx.console.ui.events.handler;

import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.di.AboutToShow;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.model.application.ui.menu.MDirectMenuItem;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuElement;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.eclipse.fx.core.preferences.Preference;
import org.eclipse.fx.core.preferences.Value;
import org.osgi.annotation.bundle.Requirement;
import org.osgi.service.prefs.BackingStoreException;

import com.osgifx.console.supervisor.EventListener;
import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.util.fx.Fx;

@Requirement(effective = "active", namespace = SERVICE_NAMESPACE, filter = "(objectClass=com.osgifx.console.supervisor.EventListener)")
public final class EventReceiveMenuContributionHandler {

    @Log
    @Inject
    private FluentLogger   logger;
    @Inject
    private Supervisor     supervisor;
    @Inject
    private EventListener  eventListener;
    @Inject
    private EModelService  modelService;
    @Inject
    @Preference(nodePath = "osgi.fx.event", key = "action")
    private Value<Boolean> action;
    @Inject
    @Named("is_connected")
    private boolean        isConnected;

    @PostConstruct
    public void init() {
        final boolean currentState = action.getValue();
        if (currentState) {
            supervisor.addOSGiEventListener(eventListener);
            logger.atInfo().throttleByCount(10).log("OSGi event listener has been added");
        } else {
            supervisor.removeOSGiEventListener(eventListener);
            logger.atInfo().throttleByCount(10).log("OSGi event listener has been removed");
        }
    }

    @AboutToShow
    public void aboutToShow(final List<MMenuElement> items, final MWindow window) {
        prepareMenu(items, action.getValue());
    }

    @Execute
    public void execute(final MDirectMenuItem menuItem) throws BackingStoreException {
        final boolean accessibilityPhrase = Boolean.parseBoolean(menuItem.getAccessibilityPhrase());
        action.publish(accessibilityPhrase);

        if (accessibilityPhrase) {
            supervisor.addOSGiEventListener(eventListener);
            Fx.showSuccessNotification("Event Notification", "Events will now be received");
            logger.atInfo().log("OSGi events will now be received");
        } else {
            supervisor.removeOSGiEventListener(eventListener);
            Fx.showSuccessNotification("Event Notification", "Events will not be received anymore");
            logger.atInfo().log("OSGi events will not be received anymore");
        }
    }

    @CanExecute
    public boolean canExecute() {
        return isConnected;
    }

    private void prepareMenu(final List<MMenuElement> items, final boolean value) {
        final MDirectMenuItem eventActionMenu;
        if (value) {
            eventActionMenu = createEventActionMenu(Type.STOP);
        } else {
            eventActionMenu = createEventActionMenu(Type.START);
        }
        items.add(eventActionMenu);
    }

    private MDirectMenuItem createEventActionMenu(final Type type) {
        String label;
        String icon;
        String accessibilityPhrase;
        if (type == Type.STOP) {
            label               = "Stop Receiving Events";
            icon                = "stop.png";
            accessibilityPhrase = "false";
        } else {
            label               = "Start Receiving Events";
            icon                = "start.png";
            accessibilityPhrase = "true";
        }
        final MDirectMenuItem dynamicItem = modelService.createModelElement(MDirectMenuItem.class);

        dynamicItem.setLabel(label);
        dynamicItem.setIconURI("platform:/plugin/com.osgifx.console.ui.events/graphic/icons/" + icon);
        dynamicItem.setAccessibilityPhrase(accessibilityPhrase);
        dynamicItem.setContributorURI("platform:/plugin/com.osgifx.console.ui.events");
        dynamicItem.setContributionURI(
                "bundleclass://com.osgifx.console.ui.events/com.osgifx.console.ui.events.handler.EventReceiveMenuContributionHandler");

        return dynamicItem;
    }

    private enum Type {
        START,
        STOP
    }

}
