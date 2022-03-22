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
package com.osgifx.console.smartgraph.containers;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

/**
 * This class provides zooming and panning for a JavaFX node.
 *
 * It shows the zoom level with a slider control and reacts to mouse scrolls and
 * mouse dragging.
 *
 * The content node is out forward in the z-index so it can react to mouse
 * events first. The node should consume any event not meant to propagate to
 * this pane.
 */
public class ContentZoomPane extends BorderPane {

	/*
	 * PAN AND ZOOM
	 */
	private final DoubleProperty scaleFactorProperty = new ReadOnlyDoubleWrapper(1);
	private final Node           content;

	private static final double MIN_SCALE    = 1;
	private static final double MAX_SCALE    = 5;
	private static final double SCROLL_DELTA = 0.25;

	public ContentZoomPane(final Node content) {
		if (content == null) {
			throw new IllegalArgumentException("Content cannot be null.");
		}

		this.content = content;

		final var center = content;
		content.toFront();

		setCenter(center);
		setRight(createSlider());

		enablePanAndZoom();
	}

	private Node createSlider() {

		final var slider = new Slider(MIN_SCALE, MAX_SCALE, MIN_SCALE);
		slider.setOrientation(Orientation.VERTICAL);
		slider.setShowTickMarks(true);
		slider.setShowTickLabels(true);
		slider.setMajorTickUnit(SCROLL_DELTA);
		slider.setMinorTickCount(1);
		slider.setBlockIncrement(0.125f);
		slider.setSnapToTicks(true);

		final var label = new Text("Zoom");

		final var paneSlider = new VBox(slider, label);

		paneSlider.setPadding(new Insets(10, 10, 10, 10));
		paneSlider.setSpacing(10);

		slider.valueProperty().bind(scaleFactorProperty());

		return paneSlider;
	}

	public void setContentPivot(final double x, final double y) {
		content.setTranslateX(content.getTranslateX() - x);
		content.setTranslateY(content.getTranslateY() - y);
	}

	public static double boundValue(final double value, final double min, final double max) {

		if (Double.compare(value, min) < 0) {
			return min;
		}

		if (Double.compare(value, max) > 0) {
			return max;
		}

		return value;
	}

	private void enablePanAndZoom() {

		setOnScroll((final ScrollEvent event) -> {

			final double direction = event.getDeltaY() >= 0 ? 1 : -1;

			final double currentScale  = scaleFactorProperty.getValue();
			var          computedScale = currentScale + direction * SCROLL_DELTA;

			computedScale = boundValue(computedScale, MIN_SCALE, MAX_SCALE);

			if (currentScale != computedScale) {

				content.setScaleX(computedScale);
				content.setScaleY(computedScale);

				if (computedScale == 1) {
					content.setTranslateX(-getTranslateX());
					content.setTranslateY(-getTranslateY());
				} else {
					scaleFactorProperty.setValue(computedScale);

					final var bounds = content.localToScene(content.getBoundsInLocal());
					final var f      = computedScale / currentScale - 1;
					final var dx     = event.getX() - (bounds.getWidth() / 2 + bounds.getMinX());
					final var dy     = event.getY() - (bounds.getHeight() / 2 + bounds.getMinY());

					setContentPivot(f * dx, f * dy);
				}

			}
			// do not propagate
			event.consume();

		});

		final var sceneDragContext = new DragContext();

		setOnMousePressed((final MouseEvent event) -> {

			if (event.isSecondaryButtonDown()) {
				getScene().setCursor(Cursor.MOVE);

				sceneDragContext.mouseAnchorX = event.getX();
				sceneDragContext.mouseAnchorY = event.getY();

				sceneDragContext.translateAnchorX = content.getTranslateX();
				sceneDragContext.translateAnchorY = content.getTranslateY();
			}

		});

		setOnMouseReleased((final MouseEvent event) -> {
			getScene().setCursor(Cursor.DEFAULT);
		});

		setOnMouseDragged((final MouseEvent event) -> {
			if (event.isSecondaryButtonDown()) {

				content.setTranslateX(sceneDragContext.translateAnchorX + event.getX() - sceneDragContext.mouseAnchorX);
				content.setTranslateY(sceneDragContext.translateAnchorY + event.getY() - sceneDragContext.mouseAnchorY);
			}
		});

	}

	public DoubleProperty scaleFactorProperty() {
		return scaleFactorProperty;
	}

	static class DragContext {

		double mouseAnchorX;
		double mouseAnchorY;

		double translateAnchorX;
		double translateAnchorY;

	}

}
