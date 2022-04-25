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
package com.osgifx.console.ui.events.handler;

import static com.osgifx.console.event.topics.CommonEventTopics.EVENT_RECEIVE_STARTED_EVENT_TOPIC;
import static com.osgifx.console.event.topics.CommonEventTopics.EVENT_RECEIVE_STOPPED_EVENT_TOPIC;
import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.di.AboutToShow;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.model.application.ui.menu.MDirectMenuItem;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuElement;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.fx.core.di.Service;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.annotation.bundle.Requirement;
import org.osgi.framework.BundleContext;

import com.osgifx.console.supervisor.EventListener;
import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.util.fx.Fx;

@Requirement(effective = "active", namespace = SERVICE_NAMESPACE, filter = "(objectClass=com.osgifx.console.supervisor.EventListener)")
public final class EventReceiveMenuContributionHandler {

	public static final String PROPERTY_KEY_EVENT_DISPLAY = "osgi.fx.event";

	@Log
	@Inject
	private FluentLogger  logger;
	@Inject
	@OSGiBundle
	private BundleContext context;
	@Inject
	private Supervisor    supervisor;
	@Inject
	private IEventBroker  eventBroker;
	@Inject
	@Service(filterExpression = "(supplier.id=events)")
	private EventListener eventListener;
	@Inject
	private EModelService modelService;
	@Inject
	@Named("is_connected")
	private boolean       isConnected;

	@PostConstruct
	public void init() {
		final var currentState = Boolean.getBoolean(PROPERTY_KEY_EVENT_DISPLAY);
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
		final var value = Boolean.getBoolean(PROPERTY_KEY_EVENT_DISPLAY);
		prepareMenu(items, value);
	}

	@Execute
	public void execute(final MDirectMenuItem menuItem) {
		final var accessibilityPhrase = Boolean.parseBoolean(menuItem.getAccessibilityPhrase());

		if (accessibilityPhrase) {
			eventBroker.post(EVENT_RECEIVE_STARTED_EVENT_TOPIC, String.valueOf(accessibilityPhrase));
		} else {
			eventBroker.post(EVENT_RECEIVE_STOPPED_EVENT_TOPIC, String.valueOf(accessibilityPhrase));
		}

		System.setProperty(PROPERTY_KEY_EVENT_DISPLAY, String.valueOf(accessibilityPhrase));

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
		final var dynamicItem = modelService.createModelElement(MDirectMenuItem.class);
		final var bsn         = context.getBundle().getSymbolicName();

		dynamicItem.setLabel(label);
		dynamicItem.setIconURI("platform:/plugin/" + bsn + "/graphic/icons/" + icon);
		dynamicItem.setAccessibilityPhrase(accessibilityPhrase);
		dynamicItem.setContributorURI("platform:/plugin/" + bsn);
		dynamicItem.setContributionURI("bundleclass://" + bsn + "/" + getClass().getName());

		return dynamicItem;
	}

	private enum Type {
		START, STOP
	}

}
