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
package com.osgifx.console.ui.events;

import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;

import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;

import org.controlsfx.control.table.TableFilter;
import org.controlsfx.control.table.TableRowExpanderColumn;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.fx.core.di.LocalInstance;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.annotation.bundle.Requirement;
import org.osgi.framework.BundleContext;

import com.osgifx.console.agent.dto.XEventDTO;
import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.util.fx.DTOCellValueFactory;
import com.osgifx.console.util.fx.Fx;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;

@Requirement(effective = "active", namespace = SERVICE_NAMESPACE, filter = "(objectClass=com.osgifx.console.data.provider.DataProvider)")
public final class EventsFxController {

	@Log
	@Inject
	private FluentLogger         logger;
	@Inject
	@LocalInstance
	private FXMLLoader           loader;
	@FXML
	private TableView<XEventDTO> table;
	@Inject
	@OSGiBundle
	private BundleContext        context;
	@Inject
	@Named("is_connected")
	private boolean              isConnected;
	@Inject
	private DataProvider         dataProvider;

	private static final String EVENT_TOPIC = "com/osgifx/clear/events";

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
		final var controller     = (EventDetailsFxController) loader.getController();
		final var expanderColumn = new TableRowExpanderColumn<XEventDTO>(expandedEvent -> {
										controller.initControls(expandedEvent.getValue());
										return expandedNode;
									});

		final var receivedAtColumn = new TableColumn<XEventDTO, Date>("Received At");

		receivedAtColumn.setPrefWidth(290);
		receivedAtColumn.setCellValueFactory(new DTOCellValueFactory<>("received", Date.class));

		final var topicColumn = new TableColumn<XEventDTO, String>("Topic");

		topicColumn.setPrefWidth(650);
		topicColumn.setCellValueFactory(new DTOCellValueFactory<>("topic", String.class));

		table.getColumns().add(expanderColumn);
		table.getColumns().add(receivedAtColumn);
		table.getColumns().add(topicColumn);

		final var events = dataProvider.events();
		table.setItems(events);

		TableFilter.forTableView(table).apply();
		sortByReceivedAt(receivedAtColumn);
	}

	@Inject
	@Optional
	private synchronized void clearTableEvent(@UIEventTopic(EVENT_TOPIC) final String data) {
		table.setItems(FXCollections.emptyObservableList());
		final var events = dataProvider.events();
		events.clear();
		table.setItems(events);
		logger.atInfo().log("Cleared events table successfully");
	}

	private void sortByReceivedAt(final TableColumn<XEventDTO, Date> column) {
		column.setSortType(TableColumn.SortType.DESCENDING);
		table.getSortOrder().add(column);
		table.sort();
	}

}
