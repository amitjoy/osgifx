/*******************************************************************************
 * Copyright 2021-2026 Amit Kumar Mondal
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
package com.osgifx.console.ui.extension.handler;

import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;

import java.io.FileInputStream;

import javax.inject.Inject;

import org.controlsfx.dialog.ProgressDialog;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.workbench.IWorkbench;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.annotation.bundle.Requirement;
import org.osgi.service.deploymentadmin.DeploymentAdmin;

import com.osgifx.console.executor.Executor;
import com.osgifx.console.util.fx.FxDialog;

import javafx.concurrent.Task;
import javafx.stage.FileChooser;

@Requirement(effective = "active", namespace = SERVICE_NAMESPACE, filter = "(objectClass=org.osgi.service.deploymentadmin.DeploymentAdmin)")
public final class ExtensionInstallHandler {

    @Log
    @Inject
    private FluentLogger      logger;
    @Inject
    private Executor          executor;
    @Inject
    private IWorkbench        workbench;
    @Inject
    private ThreadSynchronize threadSync;
    @Inject
    private DeploymentAdmin   deploymentAdmin;
    private ProgressDialog    progressDialog;

    @Execute
    public void execute() {
        final var bundleChooser = new FileChooser();
        bundleChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Deployment Packages (.dp)", "*.dp"));
        final var deploymentPackage = bundleChooser.showOpenDialog(null);

        if (deploymentPackage != null) {
            final Task<Void> task = new Task<>() {

                @Override
                protected Void call() throws Exception {
                    try (var is = new FileInputStream(deploymentPackage)) {
                        final var dp = deploymentAdmin.installDeploymentPackage(is);
                        logger.atInfo().log("Extension '%s' has been successfuly installed/updated", dp.getName());
                        return null;
                    } catch (final Exception e) {
                        logger.atError().withException(e).log("Cannot install extension '%s'",
                                deploymentPackage.getName());
                        threadSync.asyncExec(() -> {
                            progressDialog.close();
                            FxDialog.showExceptionDialog(e, getClass().getClassLoader());
                        });
                        throw e;
                    }
                }

                @Override
                protected void succeeded() {
                    threadSync.asyncExec(progressDialog::close);
                    FxDialog.showInfoDialog("Extension Installation",
                            "The application must be restarted, therefore, will be shut down right away",
                            getClass().getClassLoader(), btn -> workbench.restart());
                }
            };

            final var taskFuture = executor.runAsync(task);
            progressDialog = FxDialog.showProgressDialog("Extension Installation", task, getClass().getClassLoader(),
                    () -> {
                        final var isCancelled = deploymentAdmin.cancel();
                        if (isCancelled) {
                            taskFuture.cancel(true);
                        }
                    });
        }
    }

}
