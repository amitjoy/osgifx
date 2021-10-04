package in.bytehue.osgifx.console.ui.dto;

import java.util.Map;

import javafx.beans.property.ListProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.util.Pair;

public final class ComponentFxDTO {

    private final LongProperty                       id                    = new SimpleLongProperty(this, "id");
    private final StringProperty                     name                  = new SimpleStringProperty(this, "name");
    private final StringProperty                     state                 = new SimpleStringProperty(this, "state");
    private final ObjectProperty<Pair<String, Long>> bundle                = new SimpleObjectProperty<>(this, "bundle");
    private final StringProperty                     factory               = new SimpleStringProperty(this, "factory");
    private final StringProperty                     scope                 = new SimpleStringProperty(this, "scope");
    private final StringProperty                     implementationClass   = new SimpleStringProperty(this, "implementationClass");
    private final StringProperty                     configurationPolicy   = new SimpleStringProperty(this, "configurationPolicy");
    private final ListProperty<String>               serviceInterfaces     = new SimpleListProperty<>(this, "serviceInterfaces");
    private final ListProperty<String>               configurationPid      = new SimpleListProperty<>(this, "configurationPid");
    private final MapProperty<String, String>        properties            = new SimpleMapProperty<>(this, "properties");
    private final ListProperty<Map<String, String>>  references            = new SimpleListProperty<>(this, "references");
    private final StringProperty                     failure               = new SimpleStringProperty(this, "failure");
    private final StringProperty                     activate              = new SimpleStringProperty(this, "activate");
    private final StringProperty                     deactivate            = new SimpleStringProperty(this, "deactivate");
    private final StringProperty                     modified              = new SimpleStringProperty(this, "modified");
    private final MapProperty<String, String>        satisfiedReferences   = new SimpleMapProperty<>(this, "satisfiedReferences");
    private final MapProperty<String, String>        unsatisfiedReferences = new SimpleMapProperty<>(this, "unsatisfiedReferences");

    public LongProperty idProperty() {
        return id;
    }

    public long getId() {
        return idProperty().get();
    }

    public void setId(final long id) {
        idProperty().set(id);
    }

    public StringProperty nameProperty() {
        return name;
    }

    public String getName() {
        return nameProperty().get();
    }

    public void setName(final String name) {
        nameProperty().set(name);
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

    public ObjectProperty<Pair<String, Long>> bundleProperty() {
        return bundle;
    }

    public Pair<String, Long> getBundle() {
        return bundleProperty().get();
    }

    public void setBundle(final Pair<String, Long> bundle) {
        bundleProperty().set(bundle);
    }

    public StringProperty factoryProperty() {
        return factory;
    }

    public String getFactory() {
        return factoryProperty().get();
    }

    public void setFactory(final String factory) {
        factoryProperty().set(factory);
    }

    public StringProperty scopeProperty() {
        return scope;
    }

    public String getScope() {
        return scopeProperty().get();
    }

    public void setScope(final String scope) {
        scopeProperty().set(scope);
    }

    public StringProperty implementationClassProperty() {
        return implementationClass;
    }

    public String getImplementationClass() {
        return implementationClassProperty().get();
    }

    public void setImplementationClass(final String implementationClass) {
        implementationClassProperty().set(implementationClass);
    }

    public StringProperty configurationPolicyProperty() {
        return configurationPolicy;
    }

    public String getConfigurationPolicy() {
        return configurationPolicyProperty().get();
    }

    public void setConfigurationPolicy(final String configurationPolicy) {
        configurationPolicyProperty().set(configurationPolicy);
    }

    public ListProperty<String> serviceInterfacesProperty() {
        return serviceInterfaces;
    }

    public ObservableList<String> getServiceInterfaces() {
        return serviceInterfacesProperty().get();
    }

    public void setServiceInterfaces(final ObservableList<String> serviceInterfaces) {
        serviceInterfacesProperty().set(serviceInterfaces);
    }

    public ListProperty<String> configurationPidProperty() {
        return configurationPid;
    }

    public ObservableList<String> getConfigurationPid() {
        return configurationPidProperty().get();
    }

    public void setConfigurationPid(final ObservableList<String> configurationPid) {
        configurationPidProperty().set(configurationPid);
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

    public ListProperty<Map<String, String>> referencesProperty() {
        return references;
    }

    public ObservableList<Map<String, String>> getReferences() {
        return referencesProperty().get();
    }

    public void setReferences(final ObservableList<Map<String, String>> references) {
        referencesProperty().set(references);
    }

    public StringProperty failureProperty() {
        return failure;
    }

    public String getFailure() {
        return failureProperty().get();
    }

    public void setFailure(final String failure) {
        failureProperty().set(failure);
    }

    public StringProperty activateProperty() {
        return activate;
    }

    public String getActivate() {
        return activateProperty().get();
    }

    public void setActivate(final String activate) {
        activateProperty().set(activate);
    }

    public StringProperty deactivateProperty() {
        return deactivate;
    }

    public String getDeactivate() {
        return deactivateProperty().get();
    }

    public void setDeactivate(final String deactivate) {
        deactivateProperty().set(deactivate);
    }

    public StringProperty modifiedProperty() {
        return modified;
    }

    public String getModified() {
        return modifiedProperty().get();
    }

    public void setModified(final String modified) {
        modifiedProperty().set(modified);
    }

    public MapProperty<String, String> satisfiedReferencesProperty() {
        return satisfiedReferences;
    }

    public ObservableMap<String, String> getSatisfiedReferences() {
        return satisfiedReferencesProperty().get();
    }

    public void setSatisfiedReferences(final ObservableMap<String, String> satisfiedReferences) {
        satisfiedReferencesProperty().set(satisfiedReferences);
    }

    public MapProperty<String, String> unsatisfiedReferencesProperty() {
        return unsatisfiedReferences;
    }

    public ObservableMap<String, String> getUnsatisfiedReferences() {
        return unsatisfiedReferencesProperty().get();
    }

    public void setUnsatisfiedReferences(final ObservableMap<String, String> unsatisfiedReferences) {
        unsatisfiedReferencesProperty().set(unsatisfiedReferences);
    }

}
