package in.bytehue.osgifx.console.feature;

import org.osgi.framework.dto.BundleDTO;

/**
 * Callback to track the progress of the feature removal
 */
public interface FeatureRemovalListener {

    /**
     * Callback to be executed when the feature installation is started just after parsing the JSON
     *
     * @param feature the parsed feature
     */
    void onStart(FeatureDTO feature);

    /**
     * Callback to be executed when a bundle is removed
     *
     * @param bundle the removed bundle
     */
    void onBundleRemoved(BundleDTO bundle);

    /**
     * Callback to be executed when a configuration is removed
     *
     * @param pid the removed configuration's PID
     */
    void onConfigurationRemoved(String pid);

    /**
     * Callback to be executed when the feature removal is finished
     */
    void onComplete();

}
