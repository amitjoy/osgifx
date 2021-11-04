package in.bytehue.osgifx.console.update.agent;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.LoggerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.feature.FeatureService;

import in.bytehue.osgifx.console.feature.FeatureDTO;
import in.bytehue.osgifx.console.update.UpdateAgent;

@Component
public final class UpdateAgentProvider implements UpdateAgent {

    @Reference
    private LoggerFactory  factory;
    @Reference
    private FeatureService featureService;
    private FluentLogger   logger;

    @Activate
    void activate() {
        logger = FluentLogger.of(factory.createLogger(getClass().getName()));
    }

    @Override
    public Collection<FeatureDTO> update() {
        logger.atInfo().log("Updating features if updates are available");
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, FeatureDTO> readFeatures(final File archiveURL) {
        logger.atInfo().log("Reading archive: %s", archiveURL);
        return Collections.emptyMap();
    }

    @Override
    public FeatureDTO updateOrInstall(final File featureJson) throws Exception {
        logger.atInfo().log("Updating or installing feature: %s", featureJson);
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<FeatureDTO> getInstalledFeatures() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public FeatureDTO remove(final String featureId) {
        logger.atInfo().log("Removing feature: %s", featureId);
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<FeatureDTO> checkForUpdates() {
        logger.atInfo().log("Checking for updates");
        // TODO Auto-generated method stub
        return null;
    }

}
