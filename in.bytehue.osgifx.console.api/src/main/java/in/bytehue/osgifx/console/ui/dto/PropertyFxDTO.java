package in.bytehue.osgifx.console.ui.dto;

import javafx.beans.property.SimpleStringProperty;

/**
 * Data Transfer Object of a Property
 */
public final class PropertyFxDTO {

    /** The name of the property */
    private SimpleStringProperty name;

    /** The value of the property */
    private SimpleStringProperty value;

    /** The type of the property (framework or system) */
    private SimpleStringProperty type;

    /**
     * Returns the name of the property
     *
     * @return the name of the property
     */
    public String getName() {
        return nameProperty().get();
    }

    /**
     * Returns the name of the property as JavaFx bean
     *
     * @return the name of the property as JavaFx bean
     */
    public SimpleStringProperty nameProperty() {
        if (name == null) {
            name = new SimpleStringProperty(this, "name");
        }
        return name;
    }

    /**
     * Sets the name of the property
     *
     * @param name the name of the property
     */
    public void setName(final String name) {
        nameProperty().set(name);
    }

    /**
     * Returns the value of the property
     *
     * @return the value of the property
     */
    public String getValue() {
        return valueProperty().get();
    }

    /**
     * Returns the value of the property as JavaFx bean
     *
     * @return the value of the property as JavaFx bean
     */
    public SimpleStringProperty valueProperty() {
        if (value == null) {
            value = new SimpleStringProperty(this, "value");
        }
        return value;
    }

    /**
     * Sets the value of the property
     *
     * @param value the value of the property
     */
    public void setValue(final String value) {
        valueProperty().set(value);
    }

    /**
     * Returns the type of the property
     *
     * @return the type of the property
     */
    public String getType() {
        return typeProperty().get();
    }

    /**
     * Returns the type (framework or system) of the property as JavaFx bean
     *
     * @return the type (framework or system) of the property as JavaFx bean
     */
    public SimpleStringProperty typeProperty() {
        if (type == null) {
            type = new SimpleStringProperty(this, "type");
        }
        return type;
    }

    /**
     * Sets the type of the property
     *
     * @param type the name of the property
     */
    public void setType(final String type) {
        typeProperty().set(type);
    }

}
