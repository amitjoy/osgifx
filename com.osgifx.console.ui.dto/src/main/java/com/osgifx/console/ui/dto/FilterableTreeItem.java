package com.osgifx.console.ui.dto;

import org.eclipse.fx.ui.controls.tree.TreeItemComparator;
import org.eclipse.fx.ui.controls.tree.TreeItemPredicate;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.cell.CheckBoxTreeCell;

/**
 * An extension of {@link TreeItem} with the possibility to filter its children. To enable filtering
 * it is necessary to set the {@link TreeItemPredicate}. If a predicate is set, then the tree item
 * will also use this predicate to filter its children (if they are of the type FilterableTreeItem).
 *
 * A tree item that has children will not be filtered. The predicate will only be evaluated, if the
 * tree item is a leaf. Since the predicate is also set for the child tree items, the tree item in question
 * can turn into a leaf if all its children are filtered.
 *
 * This class extends {@link CheckBoxTreeItem} so it can, but does not need to be, used in conjunction
 * with {@link CheckBoxTreeCell} cells.
 *
 * @param <T> The type of the {@link #getValue() value} property within {@link TreeItem}.
 */
public class FilterableTreeItem<T> extends CheckBoxTreeItem<T> {

    private final ObservableList<TreeItem<T>> sourceList   = FXCollections.observableArrayList();
    private final FilteredList<TreeItem<T>>   filteredList = new FilteredList<>(this.sourceList);

    private final ObjectProperty<TreeItemPredicate<T>> predicate = new SimpleObjectProperty<>();

    /**
     * Creates a new {@link TreeItem} with sorted children. To enable sorting it is
     * necessary to set the {@link TreeItemComparator}. If no comparator is set, then
     * the tree item will attempt so bind itself to the comparator of its parent.
     *
     * @param value the value of the {@link TreeItem}
     */
    public FilterableTreeItem(final T value) {
        super(value);
        this.filteredList.predicateProperty().bind(Bindings.createObjectBinding(() -> child -> {
            // Set the predicate of child items to force filtering
            if (child instanceof FilterableTreeItem) {
                final var filterableChild = (FilterableTreeItem<T>) child;
                filterableChild.setPredicate(this.predicate.get());
            }
            // If there is no predicate, keep this tree item
            // If there are children, keep this tree item
            if (this.predicate.get() == null || !child.getChildren().isEmpty()) {
                return true;
            }
            // Otherwise ask the TreeItemPredicate
            return this.predicate.get().test(this, child.getValue());
        }, this.predicate));
        Bindings.bindContent(getChildren(), getBackingList());
    }

    /**
     * @return the backing list
     */
    protected ObservableList<TreeItem<T>> getBackingList() {
        return this.filteredList;
    }

    /**
     * Returns the list of children that is backing the filtered list.
     *
     * @return underlying list of children
     */
    public ObservableList<TreeItem<T>> getInternalChildren() {
        return this.sourceList;
    }

    /**
     * @return the predicate property
     */
    public final ObjectProperty<TreeItemPredicate<T>> predicateProperty() {
        return this.predicate;
    }

    /**
     * @return the predicate
     */
    public final TreeItemPredicate<T> getPredicate() {
        return this.predicate.get();
    }

    /**
     * Set the predicate
     *
     * @param predicate the predicate
     */
    public final void setPredicate(final TreeItemPredicate<T> predicate) {
        this.predicate.set(predicate);
    }
}
