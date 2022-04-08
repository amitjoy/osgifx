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
package com.osgifx.console.application;

import static org.osgi.framework.Constants.BUNDLE_ACTIVATOR;

import org.eclipse.e4.core.di.InjectorFactory;
import org.osgi.annotation.bundle.Header;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.osgifx.console.application.ui.ConsoleMaskerPaneProvider;
import com.osgifx.console.application.ui.ConsoleStatusBarProvider;
import com.osgifx.console.ui.ConsoleMaskerPane;
import com.osgifx.console.ui.ConsoleStatusBar;

@Header(name = BUNDLE_ACTIVATOR, value = "${@class}")
public final class FxStarter implements BundleActivator {

	@Override
	public void start(final BundleContext context) throws Exception {
		InjectorFactory.getDefault().addBinding(ConsoleStatusBar.class).implementedBy(ConsoleStatusBarProvider.class);
		InjectorFactory.getDefault().addBinding(ConsoleMaskerPane.class).implementedBy(ConsoleMaskerPaneProvider.class);
	}

	@Override
	public void stop(final BundleContext context) throws Exception {
		// nothing to implement
	}

}
