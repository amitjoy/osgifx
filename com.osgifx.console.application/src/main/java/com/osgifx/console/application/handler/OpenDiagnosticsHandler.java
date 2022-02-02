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

public final class OpenDiagnosticsHandler {

    @Log
    @Inject
    private FluentLogger  logger;
    @Inject
    @OSGiBundle
    private BundleContext context;

    @Execute
    public void execute() {
        String area = context.getProperty("osgi.instance.area.default");
        // remove the prefix
        final String prefix = "file:";
        area = area.substring(area.indexOf(prefix) + prefix.length());
        try {
            Desktop.getDesktop().open(new File(area, "./log/log.txt"));
        } catch (final IOException e) {
            logger.atError().withException(e).log("Cannot open diagnostics file");
        }
    }

}
