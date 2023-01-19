/*******************************************************************************
 * Copyright 2021-2023 Amit Kumar Mondal
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

import java.util.List;

import com.google.common.collect.Lists;

import eu.lestard.grid.Cell;
import eu.lestard.grid.GridModel;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public final class GameLogic {

    public enum States {
        EMPTY,
        X,
        O
    }

    private States                       currentTurn;
    private final GridModel<States>      gridModel;
    private final ObjectProperty<States> winner = new SimpleObjectProperty<>();

    public GameLogic(final GridModel<States> gridModel) {
        this.gridModel = gridModel;
    }

    public void start() {
        currentTurn = States.X;
        gridModel.getCells().forEach(cell -> cell.setOnClick(event -> makeTurn(cell.getColumn(), cell.getRow())));
    }

    private void makeTurn(final int column, final int row) {
        final var cell = gridModel.getCell(column, row);

        if (cell.getState() == States.EMPTY) {
            if (currentTurn == States.X) {
                currentTurn = States.O;
            } else {
                currentTurn = States.X;
            }
            cell.changeState(currentTurn);
            checkWin();
        }
    }

    private void checkWin() {
        final List<Cell<States>> diagonalOne = Lists.newArrayList();
        diagonalOne.add(gridModel.getCell(0, 0));
        diagonalOne.add(gridModel.getCell(1, 1));
        diagonalOne.add(gridModel.getCell(2, 2));

        if (checkCellList(diagonalOne)) {
            return;
        }

        final List<Cell<States>> diagonalTwo = Lists.newArrayList();
        diagonalTwo.add(gridModel.getCell(2, 0));
        diagonalTwo.add(gridModel.getCell(1, 1));
        diagonalTwo.add(gridModel.getCell(0, 2));

        if (checkCellList(diagonalTwo)) {
            return;
        }
        for (var number = 0; number < 3; number++) {
            final var row = gridModel.getCellsOfRow(number);
            if (checkCellList(row)) {
                return;
            }
            final var column = gridModel.getCellsOfColumn(number);
            if (checkCellList(column)) {
                return;
            }
        }
        if (gridModel.getCellsWithState(States.EMPTY).isEmpty()) {
            winner.set(States.EMPTY);
        }
    }

    private boolean checkCellList(final List<Cell<States>> cells) {
        if (threeOfTheSameKind(cells, States.O)) {
            winner.set(States.O);
            return true;
        }
        if (threeOfTheSameKind(cells, States.X)) {
            winner.set(States.X);
            return true;
        }
        return false;
    }

    private boolean threeOfTheSameKind(final List<Cell<States>> cells, final States state) {
        return cells.stream().filter(cell -> cell.getState().equals(state)).count() == 3;
    }

    public ReadOnlyObjectProperty<States> winnerProperty() {
        return winner;
    }
}