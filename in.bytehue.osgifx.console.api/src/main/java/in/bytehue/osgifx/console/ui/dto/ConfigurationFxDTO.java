package in.bytehue.osgifx.console.ui.dto;

import javafx.beans.property.SimpleStringProperty;

/**
 * Data Transfer Object of a Configuration
 */
public final class ConfigurationFxDTO {

    /** The name of the configuration */
    private SimpleStringProperty name;

    /** The bundle to which the configuration is bound */
    private SimpleStringProperty bundle;

    /**
     * Returns the name of the configuration
     *
     * @return the name of the configuration
     */
    public String getName() {
        return nameProperty().get();
    }

    /**
     * Returns the name of the configuration as JavaFX bean
     *
     * @return the name of the configuration as JavaFX bean
     */
    public SimpleStringProperty nameProperty() {
        if (name == null) {
            name = new SimpleStringProperty(this, "name");
        }
        return name;
    }

    /**
     * Sets the name of the configuration
     *
     * @param name the name of the configuration
     */
    public void setName(final String name) {
        nameProperty().set(name);
    }

    /**
     * Returns the bound bundle
     *
     * @return the name of the configuration
     */
    public String getBundle() {
        return bundleProperty().get();
    }

    /**
     * Returns the bound bundle as JavaFX bean
     *
     * @return the name of the configuration as JavaFX bean
     */
    public SimpleStringProperty bundleProperty() {
        if (bundle == null) {
            bundle = new SimpleStringProperty(this, "bundle");
        }
        return bundle;
    }

    /**
     * Sets the bound bundle
     *
     * @param bundle the bound bundle
     */
    public void setBundleProperty(final String bundle) {
        bundleProperty().set(bundle);
    }

}
