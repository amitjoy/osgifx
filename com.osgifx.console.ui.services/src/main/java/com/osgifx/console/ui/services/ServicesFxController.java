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
package com.osgifx.console.ui.services;

import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;
import static org.osgi.service.component.ComponentConstants.COMPONENT_ID;

import javax.inject.Inject;
import javax.inject.Named;

import org.controlsfx.control.table.TableFilter;
import org.controlsfx.control.table.TableRowExpanderColumn;
import org.controlsfx.control.table.TableRowExpanderColumn.TableRowDataFeatures;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.fx.core.di.LocalInstance;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.annotation.bundle.Requirement;
import org.osgi.framework.BundleContext;

import com.osgifx.console.agent.dto.XServiceDTO;
import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.util.fx.DTOCellValueFactory;
import com.osgifx.console.util.fx.Fx;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;

@Requirement(effective = "active", namespace = SERVICE_NAMESPACE, filter = "(objectClass=com.osgifx.console.data.provider.DataProvider)")
public final class ServicesFxController {

	@Log
	@Inject
	private FluentLogger                      logger;
	@Inject
	@LocalInstance
	private FXMLLoader                        loader;
	@FXML
	private TableView<XServiceDTO>            table;
	@Inject
	@OSGiBundle
	private BundleContext                     context;
	@Inject
	@Named("is_connected")
	private boolean                           isConnected;
	@Inject
	private DataProvider                      dataProvider;
	private TableRowDataFeatures<XServiceDTO> previouslyExpanded;

	@FXML
	public void initialize() {
		if (!isConnected) {
			Fx.addTablePlaceholderWhenDisconnected(table);
			return;
		}
		createControls();
		Fx.disableSelectionModel(table);
		logger.atDebug().log("FXML controller has been initialized");
	}

	private void createControls() {
		final var expandedNode   = (GridPane) Fx.loadFXML(loader, context, "/fxml/expander-column-content.fxml");
		final var controller     = (ServiceDetailsFxController) loader.getController();
		final var expanderColumn = new TableRowExpanderColumn<XServiceDTO>(current -> {
										if (previouslyExpanded != null && current.getValue() == previouslyExpanded.getValue()) {
											return expandedNode;
										}
										if (previouslyExpanded != null && previouslyExpanded.isExpanded()) {
											previouslyExpanded.toggleExpanded();
										}
										controller.initControls(current.getValue());
										previouslyExpanded = current;
										return expandedNode;
									});

		final var idColumn = new TableColumn<XServiceDTO, Integer>("ID");

		idColumn.setPrefWidth(100);
		idColumn.setCellValueFactory(new DTOCellValueFactory<>("id", Integer.class));

		final var objectClassColumn = new TableColumn<XServiceDTO, String>("Object Class");

		objectClassColumn.setPrefWidth(700);
		objectClassColumn.setCellValueFactory(new DTOCellValueFactory<>("types", String.class));
		Fx.addCellFactory(objectClassColumn, s -> s.properties.containsKey(COMPONENT_ID), Color.SLATEBLUE, Color.BLACK);

		final var registeringBundleColumn = new TableColumn<XServiceDTO, String>("Registering Bundle");

		registeringBundleColumn.setPrefWidth(400);
		registeringBundleColumn.setCellValueFactory(new DTOCellValueFactory<>("registeringBundle", String.class));

		table.getColumns().add(expanderColumn);
		table.getColumns().add(idColumn);
		table.getColumns().add(objectClassColumn);
		table.getColumns().add(registeringBundleColumn);

		final var services = dataProvider.services();
		table.setItems(services);
		Fx.sortBy(table, idColumn);

		TableFilter.forTableView(table).apply();
	}

}
