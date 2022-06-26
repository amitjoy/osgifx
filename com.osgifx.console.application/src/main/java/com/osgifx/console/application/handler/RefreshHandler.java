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
package com.osgifx.console.application.handler;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.osgifx.console.data.provider.DataProvider;

public final class RefreshHandler {

    @Log
    @Inject
    private FluentLogger logger;
    @Inject
    private DataProvider dataProvider;
    @Inject
    @Optional
    @Named("is_connected")
    private boolean      isConnected;

    @Execute
    public void execute() {
        logger.atInfo().log("Refreshing data views");
        dataProvider.retrieveInfo(null, true);
        logger.atInfo().log("Refreshed data views successfully");
    }

    @CanExecute
    public boolean canExecute() {
        return isConnected;
    }

}
