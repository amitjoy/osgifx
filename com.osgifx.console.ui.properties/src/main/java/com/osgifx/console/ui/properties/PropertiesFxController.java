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
package com.osgifx.console.ui.properties;

import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;

import javax.inject.Inject;
import javax.inject.Named;

import org.controlsfx.control.table.TableFilter;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.annotation.bundle.Requirement;

import com.osgifx.console.agent.dto.XPropertyDTO;
import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.util.fx.DTOCellValueFactory;
import com.osgifx.console.util.fx.Fx;

import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

@Requirement(effective = "active", namespace = SERVICE_NAMESPACE, filter = "(objectClass=com.osgifx.console.data.provider.DataProvider)")
public final class PropertiesFxController {

	@Log
	@Inject
	private FluentLogger                      logger;
	@FXML
	private TableView<XPropertyDTO>           propertyTable;
	@FXML
	private TableColumn<XPropertyDTO, String> propertyName;
	@FXML
	private TableColumn<XPropertyDTO, String> propertyValue;
	@FXML
	private TableColumn<XPropertyDTO, String> propertyType;
	@Inject
	@Named("is_connected")
	private boolean                           isConnected;
	@Inject
	private DataProvider                      dataProvider;

	@FXML
	public void initialize() {
		if (!isConnected) {
			Fx.addTablePlaceholderWhenDisconnected(propertyTable);
			return;
		}
		propertyName.setCellValueFactory(new DTOCellValueFactory<>("name", String.class));
		propertyValue.setCellValueFactory(new DTOCellValueFactory<>("value", String.class));
		propertyType.setCellValueFactory(new DTOCellValueFactory<>("type", String.class));

		propertyTable.setItems(dataProvider.properties());
		Fx.sortBy(propertyTable, propertyName);

		TableFilter.forTableView(propertyTable).apply();
		Fx.addContextMenuToCopyContent(propertyTable);

		logger.atDebug().log("FXML controller has been initialized");
	}

}
