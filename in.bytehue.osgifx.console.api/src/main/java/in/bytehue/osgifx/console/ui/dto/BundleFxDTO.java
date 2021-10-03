package in.bytehue.osgifx.console.ui.dto;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableMap;

public final class BundleFxDTO {

    private final LongProperty                id                 = new SimpleLongProperty(this, "id");
    private final StringProperty              state              = new SimpleStringProperty(this, "state");
    private final StringProperty              symbolicName       = new SimpleStringProperty(this, "symbolicName");
    private final StringProperty              version            = new SimpleStringProperty(this, "version");
    private final StringProperty              location           = new SimpleStringProperty(this, "location");
    private final BooleanProperty             isFragment         = new SimpleBooleanProperty(this, "isFragment");
    private final LongProperty                lastModified       = new SimpleLongProperty(this, "lastModified");
    private final StringProperty              documentation      = new SimpleStringProperty(this, "documentation");
    private final StringProperty              vendor             = new SimpleStringProperty(this, "vendor");
    private final StringProperty              description        = new SimpleStringProperty(this, "description");
    private final IntegerProperty             startLevel         = new SimpleIntegerProperty(this, "startLevel");
    private final MapProperty<String, String> exportedPackages   = new SimpleMapProperty<>(this, "exportedPackages");
    private final MapProperty<String, String> importedPackages   = new SimpleMapProperty<>(this, "importedPackages");
    private final MapProperty<Long, String>   importingBundles   = new SimpleMapProperty<>(this, "importingBundles");
    private final MapProperty<Long, String>   registeredServices = new SimpleMapProperty<>(this, "registeredServices");
    private final MapProperty<String, String> manifestHeaders    = new SimpleMapProperty<>(this, "manifestHeaders");
    private final MapProperty<Long, String>   usedServices       = new SimpleMapProperty<>(this, "usedServices");
    private final MapProperty<Long, String>   hostBundles        = new SimpleMapProperty<>(this, "hostBundles");
    private final MapProperty<Long, String>   fragmentsAttached  = new SimpleMapProperty<>(this, "fragmentsAttached");

    public LongProperty idProperty() {
        return id;
    }

    public long getId() {
        return idProperty().get();
    }

    public void setId(final long id) {
        idProperty().set(id);
    }

    public StringProperty stateProperty() {
        return state;
    }

    public String getState() {
        return stateProperty().get();
    }

    public void setState(final String state) {
        stateProperty().set(state);
    }

    public StringProperty symbolicNameProperty() {
        return symbolicName;
    }

    public String getSymbolicName() {
        return symbolicNameProperty().get();
    }

    public void setSymbolicName(final String symbolicName) {
        symbolicNameProperty().set(symbolicName);
    }

    public StringProperty versionProperty() {
        return version;
    }

    public String getVersion() {
        return versionProperty().get();
    }

    public void setVersion(final String version) {
        versionProperty().set(version);
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

    public BooleanProperty isFragmentProperty() {
        return isFragment;
    }

    public boolean isIsFragment() {
        return isFragmentProperty().get();
    }

    public void setIsFragment(final boolean isFragment) {
        isFragmentProperty().set(isFragment);
    }

    public LongProperty lastModifiedProperty() {
        return lastModified;
    }

    public long getLastModified() {
        return lastModifiedProperty().get();
    }

    public void setLastModified(final long lastModified) {
        lastModifiedProperty().set(lastModified);
    }

    public StringProperty documentationProperty() {
        return documentation;
    }

    public String getDocumentation() {
        return documentationProperty().get();
    }

    public void setDocumentation(final String documentation) {
        documentationProperty().set(documentation);
    }

    public StringProperty vendorProperty() {
        return vendor;
    }

    public String getVendor() {
        return vendorProperty().get();
    }

    public void setVendor(final String vendor) {
        vendorProperty().set(vendor);
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    public String getDescription() {
        return descriptionProperty().get();
    }

    public void setDescription(final String description) {
        descriptionProperty().set(description);
    }

    public IntegerProperty startLevelProperty() {
        return startLevel;
    }

    public int getStartLevel() {
        return startLevelProperty().get();
    }

    public void setStartLevel(final int startLevel) {
        startLevelProperty().set(startLevel);
    }

    public MapProperty<String, String> exportedPackagesProperty() {
        return exportedPackages;
    }

    public ObservableMap<String, String> getExportedPackages() {
        return exportedPackagesProperty().get();
    }

    public void setExportedPackages(final ObservableMap<String, String> exportedPackages) {
        exportedPackagesProperty().set(exportedPackages);
    }

    public MapProperty<String, String> importedPackagesProperty() {
        return importedPackages;
    }

    public ObservableMap<String, String> getImportedPackages() {
        return importedPackagesProperty().get();
    }

    public void setImportedPackages(final ObservableMap<String, String> importedPackages) {
        importedPackagesProperty().set(importedPackages);
    }

    public MapProperty<Long, String> importingBundlesProperty() {
        return importingBundles;
    }

    public ObservableMap<Long, String> getImportingBundles() {
        return importingBundlesProperty().get();
    }

    public void setImportingBundles(final ObservableMap<Long, String> importingBundles) {
        importingBundlesProperty().set(importingBundles);
    }

    public MapProperty<Long, String> registeredServicesProperty() {
        return registeredServices;
    }

    public ObservableMap<Long, String> getRegisteredServices() {
        return registeredServicesProperty().get();
    }

    public void setRegisteredServices(final ObservableMap<Long, String> registeredServices) {
        registeredServicesProperty().set(registeredServices);
    }

    public MapProperty<String, String> manifestHeadersProperty() {
        return manifestHeaders;
    }

    public ObservableMap<String, String> getManifestHeaders() {
        return manifestHeadersProperty().get();
    }

    public void setManifestHeaders(final ObservableMap<String, String> manifestHeaders) {
        manifestHeadersProperty().set(manifestHeaders);
    }

    public MapProperty<Long, String> usedServicesProperty() {
        return usedServices;
    }

    public ObservableMap<Long, String> getUsedServices() {
        return usedServicesProperty().get();
    }

    public void setUsedServices(final ObservableMap<Long, String> usedServices) {
        usedServicesProperty().set(usedServices);
    }

    public MapProperty<Long, String> hostBundlesProperty() {
        return hostBundles;
    }

    public ObservableMap<Long, String> getHostBundles() {
        return hostBundlesProperty().get();
    }

    public void setHostBundles(final ObservableMap<Long, String> hostBundles) {
        hostBundlesProperty().set(hostBundles);
    }

    public MapProperty<Long, String> fragmentsAttachedProperty() {
        return fragmentsAttached;
    }

    public ObservableMap<Long, String> getFragmentsAttached() {
        return fragmentsAttachedProperty().get();
    }

    public void setFragmentsAttached(final ObservableMap<Long, String> fragmentsAttached) {
        fragmentsAttachedProperty().set(fragmentsAttached);
    }

}
