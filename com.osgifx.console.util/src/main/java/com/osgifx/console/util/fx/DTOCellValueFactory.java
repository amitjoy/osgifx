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
        final var source = celldata.getValue();
        T         value  = null;
        try {
            final var field = source.getClass().getField(property);
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
