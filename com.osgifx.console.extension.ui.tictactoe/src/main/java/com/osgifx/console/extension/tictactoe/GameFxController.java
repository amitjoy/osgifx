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
package com.osgifx.console.extension.tictactoe;

import javax.inject.Inject;

import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.osgifx.console.extension.tictactoe.GameLogic.States;

import eu.lestard.grid.GridModel;
import eu.lestard.grid.GridView;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;

public final class GameFxController {

    @Log
    @Inject
    private FluentLogger logger;
    @FXML
    private AnchorPane   rootPane;

    @FXML
    public void initialize() {
        createControls();
        logger.atDebug().log("FXML controller has been initialized");
    }

    private void createControls() {
        final var gridModel = new GridModel<States>();

        gridModel.setDefaultState(States.EMPTY);
        gridModel.setNumberOfColumns(3);
        gridModel.setNumberOfRows(3);

        final var gridView = new GridView<States>();
        gridView.setGridModel(gridModel);
        gridView.setNodeFactory(cell -> States.EMPTY == cell.getState() ? null : new Label(cell.getState().name()));

        final var gameLogic = new GameLogic(gridModel);
        gameLogic.start();

        final var stackPane = new StackPane();

        final var winLabel = new Label("test");
        winLabel.setId("winLabel");

        winLabel.visibleProperty().bind(gameLogic.winnerProperty().isNotNull());
        winLabel.textProperty()
                .bind(Bindings.when(gameLogic.winnerProperty().isEqualTo(States.EMPTY).or(gameLogic.winnerProperty().isNull())).then("Tie")
                        .otherwise(Bindings.concat("Winner: ", gameLogic.winnerProperty())));

        stackPane.getChildren().add(gridView);
        stackPane.getChildren().add(winLabel);

        rootPane.getChildren().add(stackPane);
        AnchorPane.setBottomAnchor(stackPane, 0.0);
        AnchorPane.setTopAnchor(stackPane, 0.0);
        AnchorPane.setLeftAnchor(stackPane, 0.0);
        AnchorPane.setRightAnchor(stackPane, 0.0);
    }

}
