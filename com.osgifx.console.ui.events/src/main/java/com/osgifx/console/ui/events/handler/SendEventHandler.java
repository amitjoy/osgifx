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

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.google.common.base.Strings;
import com.osgifx.console.ui.events.converter.EventManager;
import com.osgifx.console.ui.events.dialog.EventDTO;
import com.osgifx.console.ui.events.dialog.SendEventDialog;
import com.osgifx.console.util.fx.Fx;
import com.osgifx.console.util.fx.FxDialog;

public final class SendEventHandler {

    @Log
    @Inject
    private FluentLogger    logger;
    @Inject
    private IEclipseContext context;
    @Inject
    @Named("is_connected")
    private boolean         isConnected;
    @Inject
    private EventManager    eventManager;

    @Execute
    public void execute() {
        final SendEventDialog dialog = new SendEventDialog();
        ContextInjectionFactory.inject(dialog, context);
        logger.atInfo().log("Injected send event dialog to eclipse context");
        dialog.init();

        final Optional<EventDTO> event = dialog.showAndWait();
        if (event.isPresent()) {
            try {
                final EventDTO            dto        = event.get();
                final String              topic      = dto.topic;
                final boolean             isSync     = dto.isSync;
                final Map<String, Object> properties = dto.properties;

                if (Strings.isNullOrEmpty(topic) || properties == null) {
                    return;
                }

                boolean result;
                if (isSync) {
                    result = eventManager.sendEvent(topic, properties);
                } else {
                    result = eventManager.postEvent(topic, properties);
                }
                if (result) {
                    Fx.showSuccessNotification("Send Event", "Event successfully sent successfully to " + topic);
                    logger.atInfo().log("Event successfully sent successfully to %s", topic);
                } else {
                    Fx.showErrorNotification("Send Event", "Event cannot be sent to " + topic);
                }
            } catch (final Exception e) {
                logger.atError().withException(e).log("Event cannot be sent");
                FxDialog.showExceptionDialog(e, getClass().getClassLoader());
            }
        }
    }

    @CanExecute
    public boolean canExecute() {
        return isConnected;
    }

}
