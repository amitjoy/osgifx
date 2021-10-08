package in.bytehue.osgifx.console.util.fx;

import java.lang.reflect.Field;
import java.util.function.Function;

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
    private Function<S, T>  nullValueReplacer;

    public DTOCellValueFactory(final String property, final Class<T> clazz) {
        this(property, clazz, null);
    }

    public DTOCellValueFactory(final String property, final Class<T> clazz, final Function<S, T> nullValueReplacer) {
        this.clazz             = clazz;
        this.property          = property;
        this.converter         = Converters.standardConverter();
        this.nullValueReplacer = nullValueReplacer;
    }

    @Override
    public ObservableValue<T> call(final CellDataFeatures<S, T> celldata) {
        final S source = celldata.getValue();
        T       value  = null;
        try {
            final Field field = source.getClass().getField(property);
            value = converter.convert(field.get(source)).to(clazz);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            // nothing to do as we have to check for the null value replacer
        }
        if (value == null && nullValueReplacer != null) {
            value = nullValueReplacer.apply(source);
        }
        return new ReadOnlyObjectWrapper<>(value);
    }

}
