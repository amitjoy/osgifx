package in.bytehue.osgifx.console.ui.dto;

import javafx.beans.property.MapProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableMap;

public final class ConfigurationFxDTO {

    private final StringProperty              pid        = new SimpleStringProperty(this, "pid");
    private final StringProperty              location   = new SimpleStringProperty(this, "location");
    private final StringProperty              factoryPid = new SimpleStringProperty(this, "factoryPid");
    private final MapProperty<String, String> properties = new SimpleMapProperty<>(this, "properties");

    public StringProperty pidProperty() {
        return pid;
    }

    public String getPid() {
        return pidProperty().get();
    }

    public void setPid(final String pid) {
        pidProperty().set(pid);
    }

    public StringProperty locationProperty() {
        return location;
    }

    public String getLocation() {
        return locationProperty().get();
    }

    public void setLocation(final String location) {
        locationProperty().set(location);
    }

    public StringProperty factoryPidProperty() {
        return factoryPid;
    }

    public String getFactoryPid() {
        return factoryPidProperty().get();
    }

    public void setFactoryPid(final String factoryPid) {
        factoryPidProperty().set(factoryPid);
    }

    public MapProperty<String, String> propertiesProperty() {
        return properties;
    }

    public ObservableMap<String, String> getProperties() {
        return propertiesProperty().get();
    }

    public void setProperties(final ObservableMap<String, String> properties) {
        propertiesProperty().set(properties);
    }

}
