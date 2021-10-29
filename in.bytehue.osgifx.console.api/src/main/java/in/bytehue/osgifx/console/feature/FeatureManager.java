package in.bytehue.osgifx.console.feature;

import java.net.URL;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface FeatureManager {

    void install(URL url) throws Exception;

    FeatureDTO getInstalledFeatures();

    void remove(String featureId);

}
