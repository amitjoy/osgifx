package in.bytehue.osgifx.console.ui.dto;

import org.osgi.service.component.runtime.dto.ReferenceDTO;

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

    private final ObjectProperty<Pair<String, Long>> bundle              = new SimpleObjectProperty<>(this, "bundle");
    private final StringProperty                     implementationClass = new SimpleStringProperty(this, "implementationClass");
    private final StringProperty                     defaultState        = new SimpleStringProperty(this, "defaultState");
    private final StringProperty                     activation          = new SimpleStringProperty(this, "activation");
    private final StringProperty                     configurationPolicy = new SimpleStringProperty(this, "configurationPolicy");
    private final LongProperty                       serviceId           = new SimpleLongProperty(this, "serviceId");
    private final StringProperty                     serviceType         = new SimpleStringProperty(this, "serviceType");
    private final MapProperty<Long, String>          services            = new SimpleMapProperty<>(this, "services");
    private final ListProperty<String>               pid                 = new SimpleListProperty<>(this, "pid");
    private final MapProperty<String, String>        properties          = new SimpleMapProperty<>(this, "properties");
    private final ListProperty<ReferenceDTO>         references          = new SimpleListProperty<>(this, "references");
    private final StringProperty                     activate            = new SimpleStringProperty(this, "activate");
    private final StringProperty                     deactivate          = new SimpleStringProperty(this, "deactivate");
    private final StringProperty                     modified            = new SimpleStringProperty(this, "modified");

    public ObjectProperty<Pair<String, Long>> bundleProperty() {
        return bundle;
    }

    public Pair<String, Long> getBundle() {
        return bundleProperty().get();
    }

    public void setBundle(final Pair<String, Long> bundle) {
        bundleProperty().set(bundle);
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

    public StringProperty defaultStateProperty() {
        return defaultState;
    }

    public String getDefaultState() {
        return defaultStateProperty().get();
    }

    public void setDefaultState(final String defaultState) {
        defaultStateProperty().set(defaultState);
    }

    public StringProperty activationProperty() {
        return activation;
    }

    public String getActivation() {
        return activationProperty().get();
    }

    public void setActivation(final String activation) {
        activationProperty().set(activation);
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

    public LongProperty serviceIdProperty() {
        return serviceId;
    }

    public long getServiceId() {
        return serviceIdProperty().get();
    }

    public void setServiceId(final long serviceId) {
        serviceIdProperty().set(serviceId);
    }

    public StringProperty serviceTypeProperty() {
        return serviceType;
    }

    public String getServiceType() {
        return serviceTypeProperty().get();
    }

    public void setServiceType(final String serviceType) {
        serviceTypeProperty().set(serviceType);
    }

    public MapProperty<Long, String> servicesProperty() {
        return services;
    }

    public ObservableMap<Long, String> getServices() {
        return servicesProperty().get();
    }

    public void setServices(final ObservableMap<Long, String> services) {
        servicesProperty().set(services);
    }

    public ListProperty<String> pidProperty() {
        return pid;
    }

    public ObservableList<String> getPid() {
        return pidProperty().get();
    }

    public void setPid(final ObservableList<String> pid) {
        pidProperty().set(pid);
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

    public ListProperty<ReferenceDTO> referencesProperty() {
        return references;
    }

    public ObservableList<ReferenceDTO> getReferences() {
        return referencesProperty().get();
    }

    public void setReferences(final ObservableList<ReferenceDTO> references) {
        referencesProperty().set(references);
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

}
