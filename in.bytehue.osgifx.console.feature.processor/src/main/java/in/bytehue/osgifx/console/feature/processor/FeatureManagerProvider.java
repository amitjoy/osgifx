package in.bytehue.osgifx.console.feature.processor;

import java.io.File;
import java.util.Collection;

import org.osgi.service.component.annotations.Component;

import in.bytehue.osgifx.console.feature.FeatureDTO;
import in.bytehue.osgifx.console.feature.FeatureInstallationListener;
import in.bytehue.osgifx.console.feature.FeatureManager;
import in.bytehue.osgifx.console.feature.FeatureRemovalListener;

@Component
public final class FeatureManagerProvider implements FeatureManager {

    @Override
    public FeatureDTO install(final File featureJson, final FeatureInstallationListener listener) throws Exception {
        // TODO Auto-generated method stub
        return null;
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
