package in.bytehue.osgifx.console.smartgraph.graphview;

/**
 * A node to which a {@link SmartLabel} can be attached.
 */
public interface SmartLabelledNode {

    /**
     * Own and bind the <code>label</code> position to the desired position.
     *
     * @param label text label node
     */
    void attachLabel(SmartLabel label);

    /**
     * Returns the attached text label, if any.
     *
     * @return the text label reference or null if no label is attached
     */
    SmartLabel getAttachedLabel();

}
