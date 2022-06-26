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
package com.osgifx.console.util.fx;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;

@SuppressWarnings("rawtypes")
public final class NullTableViewSelectionModel<S> extends TableView.TableViewSelectionModel<S> {

    public NullTableViewSelectionModel(final TableView<S> tableView) {
        super(tableView);
    }

    @Override
    public ObservableList<TablePosition> getSelectedCells() {
        return FXCollections.emptyObservableList();
    }

    @Override
    public void selectLeftCell() {

    }

    @Override
    public void selectRightCell() {

    }

    @Override
    public void selectAboveCell() {

    }

    @Override
    public void selectBelowCell() {

    }

    @Override
    public void clearSelection(final int i, final TableColumn tableColumn) {

    }

    @Override
    public void clearAndSelect(final int i, final TableColumn tableColumn) {

    }

    @Override
    public void select(final int i, final TableColumn tableColumn) {

    }

    @Override
    public boolean isSelected(final int i, final TableColumn tableColumn) {
        return false;
    }

    @Override
    public ObservableList<Integer> getSelectedIndices() {
        return FXCollections.emptyObservableList();
    }

    @SuppressWarnings("unchecked")
    @Override
    public ObservableList getSelectedItems() {
        return FXCollections.emptyObservableList();
    }

    @Override
    public void selectIndices(final int i, final int... ints) {

    }

    @Override
    public void selectAll() {

    }

    @Override
    public void clearAndSelect(final int i) {

    }

    @Override
    public void select(final int i) {

    }

    @Override
    public void select(final Object o) {

    }

    @Override
    public void clearSelection(final int i) {

    }

    @Override
    public void clearSelection() {

    }

    @Override
    public boolean isSelected(final int i) {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public void selectPrevious() {

    }

    @Override
    public void selectNext() {

    }

    @Override
    public void selectFirst() {

    }

    @Override
    public void selectLast() {

    }
}
