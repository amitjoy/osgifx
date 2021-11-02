package in.bytehue.osgifx.console.feature;

import org.osgi.framework.dto.BundleDTO;

/**
 * Callback to track the progress of the feature installation
 */
public interface FeatureInstallationListener {

    /**
     * Callback to be executed when the feature removal
     *
     * @param feature the parsed feature
     */
    void onStart(FeatureDTO feature);

    /**
     * Callback to be executed when a bundle is installed
     *
     * @param bundle the installed bundle
     */
    void onBundleInstalled(BundleDTO bundle);

    /**
     * Callback to be executed when a bundle is started
     *
     * @param bundle the started bundle (Note that, this will never be called for fragments)
     */
    void onBundleStarted(BundleDTO bundle);

    /**
     * Callback to be executed when a configuration is installed
     *
     * @param pid the installed configuration's PID
     */
    void onConfigurationInstalled(String pid);

    /**
     * Callback to be executed when the feature installation is finished
     */
    void onComplete();

}
