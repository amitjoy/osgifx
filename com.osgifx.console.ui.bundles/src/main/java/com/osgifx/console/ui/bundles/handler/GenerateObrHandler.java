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
package com.osgifx.console.ui.bundles.handler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.resource.Resource;

import com.osgifx.console.agent.dto.XBundleDTO;
import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.ui.bundles.obr.bnd.ResourceBuilder;
import com.osgifx.console.ui.bundles.obr.bnd.XMLResourceGenerator;
import com.osgifx.console.util.fx.Fx;
import com.osgifx.console.util.fx.FxDialog;

import javafx.stage.DirectoryChooser;

public final class GenerateObrHandler {

    @Log
    @Inject
    private FluentLogger logger;
    @Inject
    private DataProvider dataProvider;
    @Inject
    @Named("is_connected")
    private boolean      isConnected;
    @Inject
    @Optional
    @Named("connected.agent")
    private String       connectedAgent;

    @Execute
    public void execute() {
        final var directoryChooser = new DirectoryChooser();
        final var location         = directoryChooser.showDialog(null);
        if (location == null) {
            return;
        }
        exportOBR(location);
    }

    @CanExecute
    public boolean canExecute() {
        return isConnected;
    }

    private void exportOBR(final File location) {
        final var resources = dataProvider.bundles().stream().map(this::toResource).toList();
        if (resources.isEmpty()) {
            logger.atInfo().log("Resources are empty");
            return;
        }
        final var xmlResourceGenerator = new XMLResourceGenerator();
        final var name                 = connectedAgent.replace(":", "_") + "_" + LocalDateTime.now() + ".xml";
        final var outputFile           = new File(location, name);
        try (final var buffer = new ByteArrayOutputStream();
                OutputStream fileStream = new FileOutputStream(outputFile)) {
            xmlResourceGenerator.resources(resources);
            xmlResourceGenerator.save(fileStream);
            Fx.showSuccessNotification("OBR Generation", "Successfully generated");
            logger.atInfo().log("OBR XML has been successfully generated - '%s'", outputFile);
        } catch (final Exception e) {
            FxDialog.showExceptionDialog(e, getClass().getClassLoader());
            logger.atError().withException(e).log("OBR XML cannot be generated");
        }
    }

    private Resource toResource(final XBundleDTO bundle) {
        try {
            final var builder = new ResourceBuilder();
            builder.addCapabilities(bundle.bundleRevision.capabilities);
            builder.addRequirements(bundle.bundleRevision.requirements);
            return builder.build();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

    }

}
