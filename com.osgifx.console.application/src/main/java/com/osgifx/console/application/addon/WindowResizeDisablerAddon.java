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
package com.osgifx.console.application.addon;

import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.EventTopic;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.fx.ui.workbench.renderers.base.widget.WWindow;
import org.osgi.service.event.Event;

import javafx.stage.Stage;

public final class WindowResizeDisablerAddon {

    private static final String MAIN_WINDOW_ID = "com.osgifx.console.window.main";

    @Inject
    private MApplication  application;
    @Inject
    private EModelService modelService;

    @Inject
    @Optional
    public void onActivate(@EventTopic(UIEvents.UILifeCycle.ACTIVATE) final Event event) {
        try {
            final var window = (MWindow) modelService.find(MAIN_WINDOW_ID, application);
            final var stage  = (Stage) ((WWindow<?>) window.getWidget()).getWidget();
            stage.setResizable(false);
        } catch (final Exception e) {
            // ignore
        }
    }

}
