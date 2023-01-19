/*******************************************************************************
 * Copyright 2021-2023 Amit Kumar Mondal
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
package com.osgifx.console.application.theme;

import static com.osgifx.console.constants.FxConstants.STANDARD_CSS;
import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;

import org.eclipse.fx.ui.services.theme.Stylesheet;
import org.eclipse.fx.ui.services.theme.Theme;
import org.eclipse.fx.ui.theme.AbstractTheme;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = Theme.class)
public final class DefaultTheme extends AbstractTheme {

    @Activate
    public DefaultTheme(final BundleContext context) {
        super("theme.default", "Default Theme", context.getBundle().getResource(STANDARD_CSS));
    }

    @Override
    @Reference(cardinality = MULTIPLE, policy = DYNAMIC)
    public synchronized void registerStylesheet(final Stylesheet stylesheet) {
        super.registerStylesheet(stylesheet);
    }

    @Override
    public synchronized void unregisterStylesheet(final Stylesheet stylesheet) {
        // required for bnd, otherwise, it will report that unbind method is missing
        super.unregisterStylesheet(stylesheet);
    }

}
