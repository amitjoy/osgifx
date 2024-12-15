/*******************************************************************************
 * Copyright 2021-2025 Amit Kumar Mondal
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

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.framework.BundleContext;

import com.osgifx.console.util.fx.FxDialog;

public final class OpenDiagnosticsHandler {

    private static final String LOG_FILE_LOCATION_PROPERTY = "org.apache.sling.commons.log.file";

    @Log
    @Inject
    private FluentLogger  logger;
    @Inject
    @OSGiBundle
    private BundleContext context;

    @Execute
    public void execute() {
        try {
            final var logFileDirectory = new File(context.getProperty(LOG_FILE_LOCATION_PROPERTY)).getParentFile();
            Desktop.getDesktop().open(logFileDirectory);
            logger.atInfo().log("Diagnostics directory has been opened");
        } catch (final IOException e) {
            FxDialog.showExceptionDialog(e, getClass().getClassLoader());
            logger.atError().withException(e).log("Cannot open diagnostics directory");
        }
    }

}
