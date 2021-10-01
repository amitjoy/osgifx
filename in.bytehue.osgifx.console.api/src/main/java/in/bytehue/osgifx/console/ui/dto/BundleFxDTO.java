package in.bytehue.osgifx.console.ui.dto;

import javafx.beans.property.SimpleStringProperty;

/**
 * Data Transfer Object for a Bundle
 */
public final class BundleFxDTO {

    /** The symbolic name */
    private SimpleStringProperty symbolicName;

    /** The version */
    private SimpleStringProperty version;

    /** The status */
    private SimpleStringProperty status;

    /**
     * Returns the symbolic name
     *
     * @return the symbolic name
     */
    public String getSymbolicName() {
        return symbolicNameProperty().get();
    }

    /**
     * Returns the symbolic name as JavaFx bean
     *
     * @return the symbolic name as JavaFx bean
     */
    public SimpleStringProperty symbolicNameProperty() {
        if (symbolicName == null) {
            symbolicName = new SimpleStringProperty(this, "symbolicName");
        }
        return symbolicName;
    }

    /**
     * Sets the symbolic name of the bundle
     *
     * @param symbolicName the symbolic name of the bundle
     */
    public void setSymbolicName(final String symbolicName) {
        symbolicNameProperty().set(symbolicName);
    }

    /**
     * Returns the version
     *
     * @return the version
     */
    public String getVersion() {
        return versionProperty().get();
    }

    /**
     * Returns the version as JavaFx bean
     *
     * @return the version as JavaFx bean
     */
    public SimpleStringProperty versionProperty() {
        if (version == null) {
            version = new SimpleStringProperty(this, "version");
        }
        return version;
    }

    /**
     * Sets the version of the bundle
     *
     * @param version the version of the bundle
     */
    public void setVersion(final String version) {
        versionProperty().set(version);
    }

    /**
     * Returns the status of the bundle
     *
     * @return the status of the bundle
     */
    public String getStatus() {
        return statusProperty().get();
    }

    /**
     * Returns the status of the bundle as JavaFx bean
     *
     * @return the status of the bundle as JavaFx bean
     */
    public SimpleStringProperty statusProperty() {
        if (status == null) {
            status = new SimpleStringProperty(this, "status");
        }
        return status;
    }

    /**
     * Sets the status of the bundle
     *
     * @param status the status of the bundle
     */
    public void setStatus(final String status) {
        statusProperty().set(status);
    }

}
