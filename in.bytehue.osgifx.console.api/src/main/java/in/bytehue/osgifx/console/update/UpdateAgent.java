package in.bytehue.osgifx.console.update;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;

import in.bytehue.osgifx.console.feature.FeatureDTO;

/**
 * The {@link UpdateAgent} service is the application access point to update
 * functionalities. {@link UpdateAgent} service allows introspection of all
 * currently available or installed features in runtime.
 *
 * <p>
 * Access to this service requires the
 * {@code ServicePermission[UpdateAgent, GET]} permission. It is intended
 * that only administrative bundles should be granted this permission to limit
 * access to the potentially intrusive methods provided by this service.
 *
 * @noimplement This interface is not intended to be implemented by consumers.
 * @noextend This interface is not intended to be extended by consumers.
 *
 * @ThreadSafe
 */
@ProviderType
public interface UpdateAgent {

    /**
     * Checks for updates in all available software repositories and if updates are available,
     * synchronously updates them during application start.
     *
     * @return the list of features that have been updated (cannot be {@code null})
     */
    Collection<FeatureDTO> update();

    /**
     * Reads all the features from the specified archive URL.
     *
     * @param archiveURL the archive URL to read the features from
     * @return the map of features in the archive URL. The key is the name of the feature file. (cannot be {@code null})
     */
    Map<String, FeatureDTO> readFeatures(File archiveURL);

    /**
     * Checks if the specified feature is already installed, if yes, the feature will be updated.
     * Otherwise, the feature will be installed.
     *
     * @param task the download task
     * @return the {@link FeatureDTO} comprising all informations regarding the feature (cannot be {@code null})
     * @throws Exception if the feature can neither be installed nor updated
     */
    FeatureDTO updateOrInstall(File featureJson) throws Exception;

    /**
     * Returns all currently installed features.
     *
     * @return the list of all installed features (cannot be {@code null})
     */
    Collection<FeatureDTO> getInstalledFeatures();

    /**
     * Removes the specified feature.
     *
     * @param featureId the feature ID to be removed
     * @return the {@link FeatureDTO} comprising all informations regarding the feature (cannot be {@code null})
     */
    FeatureDTO remove(String featureId);

    /**
     * Returns the list of features that can be updated.
     *
     * @return the collection of features to be updated (cannot be {@code null})
     */
    Collection<FeatureDTO> checkForUpdates();

}
