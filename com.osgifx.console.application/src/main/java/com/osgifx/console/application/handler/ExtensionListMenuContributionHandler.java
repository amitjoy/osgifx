/*******************************************************************************
 * Copyright 2021-2024 Amit Kumar Mondal
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

import static org.eclipse.e4.ui.workbench.modeling.EPartService.PartState.ACTIVATE;

import java.util.List;

import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.e4.ui.di.AboutToShow;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.model.application.ui.menu.MDirectMenuItem;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuElement;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.framework.BundleContext;

public final class ExtensionListMenuContributionHandler {

    @Log
    @Inject
    private FluentLogger  logger;
    @Inject
    @OSGiBundle
    private BundleContext context;
    @Inject
    private EPartService  partService;
    @Inject
    private EModelService modelService;

    @AboutToShow
    public void aboutToShow(final List<MMenuElement> items, final MWindow window) {
        final var parts = partService.getParts();
        parts.stream().map(this::createViewMenu).forEach(items::add);
    }

    @Execute
    public void execute(final MDirectMenuItem menuItem) {
        final var partId = menuItem.getAccessibilityPhrase();
        logger.atInfo().log("Activating part '%s'", partId);
        partService.showPart(partId, ACTIVATE);
    }

    private MDirectMenuItem createViewMenu(final MPart part) {
        final var dynamicItem = modelService.createModelElement(MDirectMenuItem.class);
        final var bsn         = context.getBundle().getSymbolicName();

        dynamicItem.setLabel(part.getLabel());
        dynamicItem.setIconURI(part.getIconURI());
        dynamicItem.setAccessibilityPhrase(part.getElementId());
        dynamicItem.setContributorURI("platform:/plugin/" + bsn);
        dynamicItem.setContributionURI("bundleclass://" + bsn + "/" + getClass().getName());

        return dynamicItem;
    }

}
