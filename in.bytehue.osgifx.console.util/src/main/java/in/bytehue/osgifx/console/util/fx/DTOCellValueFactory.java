package in.bytehue.osgifx.console.util.fx;

import java.lang.reflect.Field;

import org.osgi.util.converter.Converter;
import org.osgi.util.converter.Converters;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.util.Callback;

public final class DTOCellValueFactory<S, T> implements Callback<CellDataFeatures<S, T>, ObservableValue<T>> {

    private final Class<T>  clazz;
    private final String    property;
    private final Converter converter;

    public DTOCellValueFactory(final String property, final Class<T> clazz) {
        this.clazz     = clazz;
        this.property  = property;
        this.converter = Converters.standardConverter();
    }

    @Override
    public ObservableValue<T> call(final CellDataFeatures<S, T> celldata) {
        final S source = celldata.getValue();
        try {
            final Field field = source.getClass().getField(property);
            final T     value = converter.convert(field.get(source)).to(clazz);
            return new ReadOnlyObjectWrapper<>(value);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException("No such field '" + property + "' exists");
        }
    }

}
