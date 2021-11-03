package in.bytehue.osgifx.console.feature.processor;

import java.io.File;
import java.io.FileReader;
import java.util.Collection;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureService;

import in.bytehue.osgifx.console.feature.FeatureDTO;
import in.bytehue.osgifx.console.feature.FeatureInstallationListener;
import in.bytehue.osgifx.console.feature.FeatureManager;
import in.bytehue.osgifx.console.feature.FeatureRemovalListener;

@Component
public final class FeatureManagerProvider implements FeatureManager {

    @Reference
    private FeatureService featureService;

    @Override
    public FeatureDTO install(final File featureJson, final FeatureInstallationListener listener) throws Exception {
        final Feature feature = featureService.readFeature(new FileReader(featureJson));
        return FeatureHelper.toFeature(feature);
    }

    @Override
    public void remove(final String featureId, final FeatureRemovalListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public Collection<FeatureDTO> getInstalledFeatures() {
        return null;
    }

}
