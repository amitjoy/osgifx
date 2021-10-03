package in.bytehue.osgifx.console.ui.dto;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public final class PropertyFxDTO {

    private final StringProperty name  = new SimpleStringProperty(this, "name");
    private final StringProperty value = new SimpleStringProperty(this, "value");
    private final StringProperty type  = new SimpleStringProperty(this, "type");

    public StringProperty nameProperty() {
        return name;
    }

    public String getName() {
        return nameProperty().get();
    }

    public void setName(final String name) {
        nameProperty().set(name);
    }

    public StringProperty valueProperty() {
        return value;
    }

    public String getValue() {
        return valueProperty().get();
    }

    public void setValue(final String value) {
        valueProperty().set(value);
    }

    public StringProperty typeProperty() {
        return type;
    }

    public String getType() {
        return typeProperty().get();
    }

    public void setType(final String type) {
        typeProperty().set(type);
    }

}
