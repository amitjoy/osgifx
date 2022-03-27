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
package com.osgifx.console.smartgraph.containers;

import com.osgifx.console.smartgraph.graphview.SmartGraphPanel;

import javafx.scene.control.CheckBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

public class SmartGraphDemoContainer extends BorderPane {

	public SmartGraphDemoContainer(@SuppressWarnings("rawtypes") final SmartGraphPanel graphView) {

		setCenter(new ContentZoomPane(graphView));

		// create bottom pane with controls
		final var bottom = new HBox(10);

		final var automatic = new CheckBox("Automatic layout");
		automatic.selectedProperty().bindBidirectional(graphView.automaticLayoutProperty());

		bottom.getChildren().add(automatic);

		setBottom(bottom);
	}

}
