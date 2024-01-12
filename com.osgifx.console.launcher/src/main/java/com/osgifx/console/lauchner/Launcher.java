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
package com.osgifx.console.lauchner;

import static java.util.Collections.emptyMap;
import static org.osgi.framework.Constants.SERVICE_PID;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;

import java.util.Optional;

import org.eclipse.fx.core.ExceptionUtils;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.LoggerFactory;
import org.osgi.service.application.ApplicationDescriptor;
import org.osgi.service.application.ApplicationException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.osgifx.console.propertytypes.MainThread;

@Component
@MainThread
public final class Launcher implements Runnable {

    private static final String APPLICATION_ID = "com.osgifx.console.application.osgifx";

    @Reference(target = "(" + SERVICE_PID + "=" + APPLICATION_ID + ")", cardinality = OPTIONAL)
    private volatile ApplicationDescriptor applicationDescriptor;

    @Reference
    private LoggerFactory factory;
    private FluentLogger  logger;

    @Activate
    void activate() {
        logger = FluentLogger.of(factory.createLogger(getClass().getName()));
    }

    @Override
    public void run() {
        try {
            if (applicationDescriptor == null) {
                logger.atError().log("Application descriptor '%s' not found", APPLICATION_ID);
                return;
            }
            logger.atInfo().log("Application descriptor '%s' found", APPLICATION_ID);
            final var handle = applicationDescriptor.launch(emptyMap());
            handle.getExitValue(0);
        } catch (final ApplicationException e) {
            logger.atError().withException(e).log(Optional.ofNullable(e.getMessage()).orElse(""));
            throw ExceptionUtils.wrap(e);
        } catch (final InterruptedException e) {
            logger.atError().withException(e).log(Optional.ofNullable(e.getMessage()).orElse(""));
            Thread.currentThread().interrupt();
        }
    }

}
