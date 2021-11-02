package in.bytehue.osgifx.console.feature;

import java.io.File;
import java.util.Collection;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Service API to process and manage features
 *
 * @see OSGi Features Specification
 */
@ProviderType
public interface FeatureManager {

    /**
     * Installs the specified feature JSON
     *
     * @param featureJson the feature JSON
     * @param listener the listener to be executed ({@code null} if not required)
     * @throws Exception if the installation fails
     */
    FeatureDTO install(File featureJson, FeatureInstallationListener listener) throws Exception;

    /**
     * Returns all currently installed features
     *
     * @return the collection of all features
     */
    Collection<FeatureDTO> getInstalledFeatures();

    /**
     * Removes the specified feature
     *
     * @param featureId the feature ID to be removed
     * @param listener the listener to be executed ({@code null} if not required)
     * @throws Exception if the removal fails
     */
    void remove(String featureId, FeatureRemovalListener listener);

}
