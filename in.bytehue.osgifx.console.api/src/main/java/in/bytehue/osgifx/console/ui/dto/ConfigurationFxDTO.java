package in.bytehue.osgifx.console.ui.dto;

import javafx.beans.property.MapProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableMap;

public final class ConfigurationFxDTO {

    private final StringProperty              pid          = new SimpleStringProperty(this, "pid");
    private final StringProperty              factoryPid   = new SimpleStringProperty(this, "factoryPid");
    private final MapProperty<String, String> properties   = new SimpleMapProperty<>(this, "properties");
    private final MapProperty<Long, String>   usingBundles = new SimpleMapProperty<>(this, "usingBundles");

    public StringProperty pidProperty() {
        return pid;
    }

    public String getPid() {
        return pidProperty().get();
    }

    public void setPid(final String pid) {
        pidProperty().set(pid);
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

    public MapProperty<Long, String> usingBundlesProperty() {
        return usingBundles;
    }

    public ObservableMap<Long, String> getUsingBundles() {
        return usingBundlesProperty().get();
    }

    public void setUsingBundles(final ObservableMap<Long, String> usingBundles) {
        usingBundlesProperty().set(usingBundles);
    }

}
