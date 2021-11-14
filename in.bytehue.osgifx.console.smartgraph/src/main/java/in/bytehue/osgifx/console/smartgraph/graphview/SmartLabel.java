package in.bytehue.osgifx.console.smartgraph.graphview;

import javafx.scene.text.Text;

/**
 * A label contains text and can be attached to any {@link SmartLabelledNode}.
 * <br>
 * This class extends from {@link Text} and is allowed any corresponding
 * css formatting.
 */
public class SmartLabel extends Text implements SmartStylableNode {

    private final SmartStyleProxy styleProxy;

    public SmartLabel() {
        this(0, 0, "");
    }

    public SmartLabel(final String text) {
        this(0, 0, text);
    }

    public SmartLabel(final double x, final double y, final String text) {
        super(x, y, text);
        styleProxy = new SmartStyleProxy(this);
    }

    @Override
    public void setStyleClass(final String cssClass) {
        styleProxy.setStyleClass(cssClass);
    }

    @Override
    public void addStyleClass(final String cssClass) {
        styleProxy.addStyleClass(cssClass);
    }

    @Override
    public boolean removeStyleClass(final String cssClass) {
        return styleProxy.removeStyleClass(cssClass);
    }

}
