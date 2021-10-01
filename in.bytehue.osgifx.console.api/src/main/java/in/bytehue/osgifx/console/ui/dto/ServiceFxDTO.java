package in.bytehue.osgifx.console.ui.dto;

import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * Data Transfer Object for a Service
 */
public final class ServiceFxDTO {

    /** The service ID */
    private SimpleLongProperty id;

    /** The registered type(s) */
    private SimpleStringProperty type;

    /** The id of the bundle that registered the service */
    private SimpleLongProperty bundle;

    /** The ranking of the service */
    private SimpleLongProperty ranking;

    /**
     * Returns the ID of the service
     *
     * @return the ID of the service
     */
    public long getId() {
        return idProperty().get();
    }

    /**
     * Returns the service ID as JavaFX bean
     *
     * @return the service ID as JavaFX bean
     */
    public SimpleLongProperty idProperty() {
        if (id == null) {
            id = new SimpleLongProperty(this, "id");
        }
        return id;
    }

    /**
     * Sets the ID of the service
     *
     * @param id the ID of the service
     */
    public void setId(final long id) {
        idProperty().set(id);
    }

    /**
     * Returns the registered type(s) of the service
     *
     * @return the registered type(s) of the service
     */
    public String getType() {
        return typeProperty().get();
    }

    /**
     * Returns the registered type(s) as JavaFX bean
     *
     * @return the registered type(s) as JavaFX bean
     */
    public SimpleStringProperty typeProperty() {
        if (type == null) {
            type = new SimpleStringProperty(this, "type");
        }
        return type;
    }

    /**
     * Sets the registered type(s) of the service
     *
     * @param type the registered type(s) of the service
     */
    public void setType(final String type) {
        typeProperty().set(type);
    }

    /**
     * Returns the registering bundle ID
     *
     * @return the registering bundle ID
     */
    public long getBundle() {
        return bundleProperty().get();
    }

    /**
     * Returns the registering bundle ID as JavaFX bean
     *
     * @return the registering bundle as JavaFX bean
     */
    public SimpleLongProperty bundleProperty() {
        if (bundle == null) {
            bundle = new SimpleLongProperty(this, "bundle");
        }
        return bundle;
    }

    /**
     * Sets the ID of the service
     *
     * @param bundle the ID of the service
     */
    public void setBundle(final long bundle) {
        bundleProperty().set(bundle);
    }

    /**
     * Returns the service ranking
     *
     * @return the service ranking
     */
    public long getRanking() {
        return rankingProperty().get();
    }

    /**
     * Returns the service ranking as JavaFX bean
     *
     * @return the service ranking as JavaFX bean
     */
    public SimpleLongProperty rankingProperty() {
        if (ranking == null) {
            ranking = new SimpleLongProperty(this, "ranking");
        }
        return ranking;
    }

    /**
     * Sets the service ranking
     *
     * @param ranking the service ranking
     */
    public void setRanking(final long ranking) {
        rankingProperty().set(ranking);
    }

}
