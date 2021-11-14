package in.bytehue.osgifx.console.smartgraph.graphview;

import javafx.scene.shape.Shape;

/**
 * This class acts as a proxy for styling of nodes.
 *
 * It essentially groups all the logic, avoiding code duplicate.
 *
 * Classes that have this behavior can delegate the method calls to an instance
 * of this class.
 */
public class SmartStyleProxy implements SmartStylableNode {

    private final Shape client;

    public SmartStyleProxy(final Shape client) {
        this.client = client;
    }

    @Override
    public void setStyle(final String css) {
        client.setStyle(css);
    }

    @Override
    public void setStyleClass(final String cssClass) {
        client.getStyleClass().clear();
        client.setStyle(null);
        client.getStyleClass().add(cssClass);
    }

    @Override
    public void addStyleClass(final String cssClass) {
        client.getStyleClass().add(cssClass);
    }

    @Override
    public boolean removeStyleClass(final String cssClass) {
        return client.getStyleClass().remove(cssClass);
    }

}
