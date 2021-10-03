package in.bytehue.osgifx.console.ui.dto;

import javafx.beans.property.LongProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableMap;

public final class EventFxDTO {

    private final LongProperty                received   = new SimpleLongProperty(this, "received");
    private final StringProperty              topic      = new SimpleStringProperty(this, "topic");
    private final MapProperty<String, String> properties = new SimpleMapProperty<>(this, "properties");

    public LongProperty receivedProperty() {
        return received;
    }

    public long getReceived() {
        return receivedProperty().get();
    }

    public void setReceived(final long received) {
        receivedProperty().set(received);
    }

    public StringProperty topicProperty() {
        return topic;
    }

    public String getTopic() {
        return topicProperty().get();
    }

    public void setTopic(final String topic) {
        topicProperty().set(topic);
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
