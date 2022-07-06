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
package com.osgifx.console.logging.forwarder;

import static java.lang.StackWalker.Option.RETAIN_CLASS_REFERENCE;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.bridge.SLF4JBridgeHandler;

/*
 * This class is instantiated by the SLF4J API.
 */
public final class Slf4jLoggerFactory implements ILoggerFactory {

    /**
     * Initialization part to make Java Util Logging to not log to the console but
     * instead bridge to SLF4J, see
     * {@link https://www.slf4j.org/api/org/slf4j/bridge/SLF4JBridgeHandler.html}.
     */
    static {
        // Remove potentially existing handlers attached to j.u.l root logger (e.g.
        // console logging)
        SLF4JBridgeHandler.removeHandlersForRootLogger(); // (since SLF4J 1.6.5)
        // Add SLF4JBridgeHandler to j.u.l's root logger
        SLF4JBridgeHandler.install();
    }

    private static final ILoggerFactory loggerFactory = new Slf4jLoggerFactory();

    @Override
    public Logger getLogger(final String name) {
        final var bundle = getCallerBundle();
        return StaticLoggerController.createLogger(bundle, name);
    }

    /**
     * Provide a dedicated loggerFactory instance for each bundle.
     *
     * @return loggerFactory depending on determined calling bundle
     */
    public static ILoggerFactory getSlf4jLoggerFactory() {
        return loggerFactory;
    }

    private Bundle getCallerBundle() {
        final var      thisBundle  = FrameworkUtil.getBundle(Slf4jLoggerFactory.class);
        final Class<?> callerClass = StackWalker.getInstance(RETAIN_CLASS_REFERENCE).getCallerClass();
        final var      bundle      = FrameworkUtil.getBundle(callerClass);
        if (bundle != null && !bundle.equals(thisBundle)) {
            return bundle;
        }
        return thisBundle;
    }

}
