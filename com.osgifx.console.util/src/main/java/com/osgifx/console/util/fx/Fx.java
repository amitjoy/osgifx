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
package com.osgifx.console.util.fx;

import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.controlsfx.control.Notifications;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;
import org.osgi.framework.BundleContext;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.util.Duration;

public final class Fx {

	private Fx() {
		throw new IllegalAccessError("Cannot be instantiated");
	}

	public static Node loadFXML(final FXMLLoader loader, final BundleContext bundleContext, final String resourceName) {
		final var fxml = bundleContext.getBundle().getResource(resourceName);
		loader.setLocation(fxml);
		try {
			return loader.load();
		} catch (final Exception e) {
			return null;
		}
	}

	public static <S, T> void sortBy(final TableView<S> table, final TableColumn<S, T> column) {
		column.setSortType(TableColumn.SortType.ASCENDING);
		table.getSortOrder().add(column);
		table.sort();
	}

	public static void autoResizeColumns(final TableView<?> table) {
		// Set the right policy
		table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
		table.getColumns().stream().forEach(column -> {
			// Minimal width = column header
			var t   = new Text(column.getText());
			var max = t.getLayoutBounds().getWidth();
			for (var i = 0; i < table.getItems().size(); i++) {
				// cell must not be empty
				if (column.getCellData(i) != null) {
					t = new Text(column.getCellData(i).toString());
					final var calcwidth = t.getLayoutBounds().getWidth();
					// remember new max-width
					if (calcwidth > max) {
						max = calcwidth;
					}
				}
			}
			// set the new max-widht with some extra space
			column.setPrefWidth(max + 10.0d);
		});
	}

	public static void showSuccessNotification(final String title, final String text) {
		final var success = new Image(Fx.class.getClassLoader().getResource("/success.png").toExternalForm());
		// @formatter:off
        final var notification = //
                Notifications.create()
                             .title(title)
                             .graphic(new ImageView(success))
                             .text(text)
                             .hideAfter(Duration.seconds(3))
                             .position(Pos.CENTER);
        // @formatter:on
		notification.show();
	}

	public static void showErrorNotification(final String title, final String text) {
		final var success = new Image(Fx.class.getClassLoader().getResource("/error.png").toExternalForm());
		// @formatter:off
        final var notification = //
                Notifications.create()
                             .title(title)
                             .graphic(new ImageView(success))
                             .text(text)
                             .hideAfter(Duration.seconds(3))
                             .position(Pos.CENTER);
        // @formatter:on
		notification.show();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void disableSelectionModel(final TableView... tableViews) {
		Stream.of(tableViews).forEach(t -> t.setSelectionModel(new NullTableViewSelectionModel<>(t)));
	}

	public static <S> void addContextMenuToCopyContent(final TableView<S> table) {
		table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		final var item = new MenuItem("Copy");
		item.setOnAction(event -> {

			final Set<Integer> rows = new TreeSet<>();
			for (final TablePosition<?, ?> tablePosition : table.getSelectionModel().getSelectedCells()) {
				rows.add(tablePosition.getRow());
			}
			final var strb     = new StringBuilder();
			var       firstRow = true;
			for (final Integer row : rows) {
				if (!firstRow) {
					strb.append('\n');
				}
				firstRow = false;
				var firstCol = true;
				for (final TableColumn<?, ?> column : table.getColumns()) {
					if (!firstCol) {
						strb.append('\t');
					}
					firstCol = false;
					final Object cellData = column.getCellData(row);
					strb.append(cellData == null ? "" : cellData.toString());
				}
			}
			final var content = new ClipboardContent();

			content.putString(strb.toString());
			Clipboard.getSystemClipboard().setContent(content);
		});
		final var menu = new ContextMenu();
		menu.getItems().add(item);
		table.setContextMenu(menu);
	}

	public static <S> void addContextMenuToCopyContent(final ListView<S> list) {
		list.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		final var item = new MenuItem("Copy");
		item.setOnAction(event -> {
			final var strb = new StringBuilder();
			for (final int index : list.getSelectionModel().getSelectedIndices()) {
				final var content = list.getItems().get(index);
				strb.append(content == null ? "" : content.toString());
			}
			final var content = new ClipboardContent();

			content.putString(strb.toString());
			Clipboard.getSystemClipboard().setContent(content);
		});
		final var menu = new ContextMenu();
		menu.getItems().add(item);
		list.setContextMenu(menu);
	}

	public static <S, T> void addCellFactory(final TableColumn<S, T> column, final Predicate<S> predicate, final Color match,
	        final Color noMatch) {
		column.setCellFactory(c -> new TableCell<>() {
			@Override
			protected void updateItem(final T item, final boolean empty) {
				super.updateItem(item, empty);
				if (item == null || empty) {
					setText(null);
					setStyle("");
				} else {
					setText(item.toString());
					final var source = getTableView().getItems().get(getIndex());
					if (predicate.test(source)) {
						setTextFill(match);
					} else {
						setTextFill(noMatch);
					}
				}
			}
		});
	}

	public static void addTablePlaceholderWhenDisconnected(final TableView<?> table) {
		final var placeholder = new Label();

		placeholder.setText("Agent not connected");
		placeholder.setGraphic(new Glyph("FontAwesome", FontAwesome.Glyph.POWER_OFF));

		table.setPlaceholder(placeholder);
	}
}
