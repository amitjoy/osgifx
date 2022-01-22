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
package com.osgifx.console.application.addon;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

public final class ModifiablePropertyAddon {

    private static final String PROPERTY = "is_connected";

    @Log
    @Inject
    private FluentLogger logger;

    @PostConstruct
    public void init(final IEclipseContext eclipseContext) {
        eclipseContext.declareModifiable(PROPERTY);
        eclipseContext.set(PROPERTY, false);
        logger.atInfo().log("'%s' property has been declared as modifiable");
    }

}
