package in.bytehue.osgifx.console.ui.dto;

import javafx.beans.property.SimpleStringProperty;

/**
 * Data Transfer Object of an Event
 */
public final class EventFxDTO {

    /** The received timestamp */
    private SimpleStringProperty received;

    /** The topic of the event */
    private SimpleStringProperty topic;

    /**
     * Returns the received timestamp
     *
     * @return the received timestamp
     */
    public String getReceived() {
        return receivedProperty().get();
    }

    /**
     * Returns the received timestamp as JavaFx bean
     *
     * @return the received timestamp as JavaFx bean
     */
    public SimpleStringProperty receivedProperty() {
        if (received == null) {
            received = new SimpleStringProperty(this, "received");
        }
        return received;
    }

    /**
     * Sets the received timestamp
     *
     * @param received the received timestamp
     */
    public void setReceived(final String received) {
        receivedProperty().set(received);
    }

    /**
     * Returns the received topic
     *
     * @return the received topic
     */
    public String getTopic() {
        return topicProperty().get();
    }

    /**
     * Returns the received topic as JavaFx bean
     *
     * @return the received topic as JavaFx bean
     */
    public SimpleStringProperty topicProperty() {
        if (topic == null) {
            topic = new SimpleStringProperty(this, "topic");
        }
        return topic;
    }

    /**
     * Sets the received topic
     *
     * @param received the received topic
     */
    public void setTopic(final String topic) {
        topicProperty().set(topic);
    }

}
