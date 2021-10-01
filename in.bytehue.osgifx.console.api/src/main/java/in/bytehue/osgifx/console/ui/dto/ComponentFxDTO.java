package in.bytehue.osgifx.console.ui.dto;

import javafx.beans.property.SimpleStringProperty;

/**
 * Data Transfer Object for a DS Service Component
 */
public final class ComponentFxDTO {

    /** The name of the component */
    private SimpleStringProperty name;

    /** The status of the component */
    private SimpleStringProperty status;

    /**
     * Returns the name of the component
     *
     * @return the name of the component
     */
    public String getName() {
        return nameProperty().get();
    }

    /**
     * Returns the name of the component as JavaFX bean
     *
     * @return the name of the component as JavFX bean
     */
    public SimpleStringProperty nameProperty() {
        if (name == null) {
            name = new SimpleStringProperty(this, "name");
        }
        return name;
    }

    /**
     * Sets the name of the component
     *
     * @param name the name of the component
     */
    public void setName(final String name) {
        nameProperty().set(name);
    }

    /**
     * Returns the status of the component
     *
     * @return the status of the component
     */
    public String getStatus() {
        return statusProperty().get();
    }

    /**
     * Returns the status of the component as JavaFX bean
     *
     * @return the status of the component as JavFX bean
     */
    public SimpleStringProperty statusProperty() {
        if (status == null) {
            status = new SimpleStringProperty(this, "status");
        }
        return status;
    }

    /**
     * Sets the status of the component
     *
     * @param status the status of the component
     */
    public void setStatus(final String status) {
        statusProperty().set(status);
    }

}
