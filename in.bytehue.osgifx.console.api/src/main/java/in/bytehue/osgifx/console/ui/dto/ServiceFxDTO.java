package in.bytehue.osgifx.console.ui.dto;

import javafx.beans.property.LongProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableMap;
import javafx.util.Pair;

public final class ServiceFxDTO {

    private final LongProperty                       id           = new SimpleLongProperty(this, "id");
    private final ObjectProperty<Pair<String, Long>> bundle       = new SimpleObjectProperty<>(this, "bundle");
    private final MapProperty<String, String>        properties   = new SimpleMapProperty<>(this, "properties");
    private final MapProperty<Long, String>          usingBundles = new SimpleMapProperty<>(this, "usingBundles");

    public LongProperty idProperty() {
        return id;
    }

    public long getId() {
        return idProperty().get();
    }

    public void setId(final long id) {
        idProperty().set(id);
    }

    public ObjectProperty<Pair<String, Long>> bundleProperty() {
        return bundle;
    }

    public Pair<String, Long> getBundle() {
        return bundleProperty().get();
    }

    public void setBundle(final Pair<String, Long> bundle) {
        bundleProperty().set(bundle);
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
