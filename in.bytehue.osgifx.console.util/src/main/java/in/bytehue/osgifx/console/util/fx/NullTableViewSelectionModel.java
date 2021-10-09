package in.bytehue.osgifx.console.util.fx;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;

@SuppressWarnings("rawtypes")
public class NullTableViewSelectionModel<S> extends TableView.TableViewSelectionModel<S> {

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