package in.bytehue.osgifx.console.update.agent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.LoggerFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureBundle;
import org.osgi.service.feature.FeatureConfiguration;
import org.osgi.service.feature.FeatureService;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;

import in.bytehue.osgifx.console.feature.FeatureBundleDTO;
import in.bytehue.osgifx.console.feature.FeatureConfigurationDTO;
import in.bytehue.osgifx.console.feature.FeatureDTO;
import in.bytehue.osgifx.console.feature.IdDTO;
import in.bytehue.osgifx.console.update.UpdateAgent;

@Component
public final class UpdateAgentProvider implements UpdateAgent {

    private static final String LOCATION_PREFIX       = "osgifx:";
    private static final String TEMP_DIRECTORY_PREFIX = "osgifx.console_";
    private static final String STARTLEVEL_KEY        = "start-order";
    private static final String CONFIG_KEY            = "features";
    private static final String BUNDLES_DIRECTORY     = "bundles";
    private static final String FEATURE_STORAGE_PID   = "osgifx.features";
    private static final int    DEAFULT_START_LEVEL   = 100;

    @Reference
    private LoggerFactory      factory;
    @Reference
    private FeatureService     featureService;
    @Reference
    private ConfigurationAdmin configAdmin;
    @Activate
    private BundleContext      bundleContext;
    private FluentLogger       logger;
    private Path               cachedDirectory;

    @Activate
    void activate() throws IOException {
        logger          = FluentLogger.of(factory.createLogger(getClass().getName()));
        cachedDirectory = Files.createTempDirectory(TEMP_DIRECTORY_PREFIX);
    }

    @Deactivate
    void deactivate() throws IOException {
        cleanDirectory(cachedDirectory);
        final File   file = cachedDirectory.toFile();
        final String path = file.getAbsolutePath();
        if (file.delete()) {
            logger.atInfo().log("Removed '%s' directory", path);
        }
    }

    @Override
    public Collection<FeatureDTO> update() {
        logger.atInfo().log("Updating features if updates are available");
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<File, FeatureDTO> readFeatures(final File archive) throws Exception {
        logger.atInfo().log("Reading archive: %s", archive);

        final List<File>            features = extractFeatures(archive);
        final Map<File, FeatureDTO> result   = Maps.newHashMap();

        for (final File file : features) {
            final Feature    feature = featureService.readFeature(new FileReader(file));
            final FeatureDTO dto     = FeatureHelper.toFeature(feature);
            result.put(file, dto);
        }
        return result;
    }

    @Override
    public Map<File, FeatureDTO> readFeatures(final URL archiveURL) throws Exception {
        logger.atInfo().log("Reading archive: %s", archiveURL);

        final File                  file     = downloadArchive(archiveURL);
        final Map<File, FeatureDTO> features = readFeatures(file);

        for (final FeatureDTO feature : features.values()) {
            feature.archiveURL = archiveURL.toString();
        }
        return features;
    }

    @Override
    public FeatureDTO updateOrInstall(final File featureJson, final String archiveURL) throws Exception {
        logger.atInfo().log("Updating or installing feature: %s", featureJson);
        final Feature feature = featureService.readFeature(new FileReader(featureJson));

        // validate all required constraints before processing the installation request
        logger.atInfo().log("Validating all bundles before processing");
        for (final FeatureBundle bundle : feature.getBundles()) {
            validateBundle(bundle, featureJson);
        }
        // install or update the bundles
        logger.atInfo().log("Installing or updating bundles");
        for (final FeatureBundle bundle : feature.getBundles()) {
            installOrUpdateBundle(bundle, featureJson);
        }
        // update configurations
        logger.atInfo().log("Updating configurations");
        for (final Entry<String, FeatureConfiguration> configuration : feature.getConfigurations().entrySet()) {
            final FeatureConfiguration config = configuration.getValue();
            updateConfiguration(config);
        }
        final FeatureDTO dto = FeatureHelper.toFeature(feature);
        dto.archiveURL = archiveURL;
        storeFeature(dto);
        return dto;
    }

    @Override
    public Collection<FeatureDTO> getInstalledFeatures() {
        try {
            final Configuration              configuration = configAdmin.getConfiguration(FEATURE_STORAGE_PID, "?");
            final Dictionary<String, Object> properties    = configuration.getProperties();
            final Object                     features      = properties.get(CONFIG_KEY);

            final Gson gson = new Gson();
            if (features == null) {
                return Collections.emptyList();
            }
            final String[]     fs     = (String[]) features;
            final FeatureDTO[] result = Stream.of(fs).map(e -> gson.fromJson(e, FeatureDTO.class)).toArray(FeatureDTO[]::new);

            return ImmutableList.copyOf(result);
        } catch (final IOException e) {
            // should not happen as location check has been disabled
            return Collections.emptyList();
        }
    }

    @Override
    public FeatureDTO remove(final String featureId) throws Exception {
        logger.atInfo().log("Removing feature: %s", featureId);
        final Optional<FeatureDTO> featureToBeRemoved = removeFeature(featureId);
        if (featureToBeRemoved.isPresent()) {
            final FeatureDTO feature = featureToBeRemoved.get();
            // uninstall all associated bundles
            for (final FeatureBundleDTO bundle : feature.bundles) {
                uninstallBundle(bundle);
            }
            // remove associated configurations
            for (final FeatureConfigurationDTO config : feature.configurations.values()) {
                removeConfiguration(config);
            }
            return feature;
        }
        return null;
    }

    @Override
    public Collection<FeatureDTO> checkForUpdates() {
        logger.atInfo().log("Checking for updates");
        // TODO Auto-generated method stub
        return null;
    }

    private void installOrUpdateBundle(final FeatureBundle bundle, final File featureJson) throws Exception {
        final Optional<Bundle> existingBundle       = getExistingBundle(bundle);
        final String           bsn                  = bundle.getID().getArtifactId();
        final String           version              = bundle.getID().getVersion();
        final String           configuredStartLevel = bundle.getMetadata().getOrDefault(STARTLEVEL_KEY, DEAFULT_START_LEVEL).toString();
        final int              startLevel           = Integer.parseInt(configuredStartLevel);
        final Optional<File>   bundleFile           = findBundleInBundlesDirectory(featureJson.getParentFile(), bsn, version);

        if (existingBundle.isPresent()) {
            try (InputStream is = new FileInputStream(bundleFile.get())) {
                existingBundle.get().update(is);
                final BundleStartLevel sl = existingBundle.get().adapt(BundleStartLevel.class);
                sl.setStartLevel(startLevel);
            }
        } else {
            try (InputStream is = new FileInputStream(bundleFile.get())) {
                final Bundle           installedBundle = bundleContext.installBundle(LOCATION_PREFIX + bsn, is);
                final BundleStartLevel sl              = installedBundle.adapt(BundleStartLevel.class);
                sl.setStartLevel(startLevel);
                installedBundle.start();
            }
        }
    }

    private void uninstallBundle(final FeatureBundleDTO bundle) throws BundleException {
        final Optional<Bundle> existingBundle = getExistingBundle(bundle);
        if (existingBundle.isPresent()) {
            existingBundle.get().uninstall();
        }
    }

    private void validateBundle(final FeatureBundle bundle, final File featureJson) throws Exception {
        final String         bsn            = bundle.getID().getArtifactId();
        final String         version        = bundle.getID().getVersion();
        final Optional<File> bundleFile     = findBundleInBundlesDirectory(featureJson.getParentFile(), bsn, version);
        final boolean        isSystemBundle = checkIfSystemBundle(bundle);
        if (isSystemBundle) {
            throw new RuntimeException("Cannot use bundle with bsn '" + bsn + "' as it cannot be updated");
        }
        if (!bundleFile.isPresent()) {
            throw new RuntimeException("Bundle with BSN '" + bsn + "' is not found in 'bundles' directory");
        }
    }

    private boolean checkIfSystemBundle(final FeatureBundle bundle) {
        return Stream.of(bundleContext.getBundles()).anyMatch(b -> b.getLocation().startsWith("reference://"));
    }

    private Optional<File> findBundleInBundlesDirectory(final File directory, final String bsn, final String version) throws Exception {
        final File bundleDir = new File(directory, BUNDLES_DIRECTORY);
        if (!bundleDir.exists()) {
            throw new RuntimeException(
                    "Feature associated bundles are missing. Make sure they are kept in the 'bundles' folder inside the archive.");
        }
        final File[] files = bundleDir.listFiles();
        for (final File f : files) {
            if (matchBundle(f, bsn, version)) {
                return Optional.of(f);
            }
        }
        return Optional.empty();
    }

    private boolean matchBundle(final File file, final String bsn, final String version) throws Exception {
        final String symbolicName  = readAttributeFromManifest(file, "Bundle-SymbolicName");
        final String bundleVersion = readAttributeFromManifest(file, "Bundle-Version");
        return symbolicName.equals(bsn) && bundleVersion.equals(version);
    }

    private void updateConfiguration(final FeatureConfiguration configuration) throws Exception {
        final Optional<String> factoryPid = configuration.getFactoryPid();
        final String           pid        = configuration.getPid();
        if (factoryPid.isPresent()) {
            final Configuration factoryConfiguration = configAdmin.createFactoryConfiguration(factoryPid.get(), "?");
            factoryConfiguration.updateIfDifferent(new Hashtable<>(configuration.getValues()));
        } else {
            final Configuration config = configAdmin.getConfiguration(pid, "?");
            config.updateIfDifferent(new Hashtable<>(configuration.getValues()));
        }
    }

    private void removeConfiguration(final FeatureConfigurationDTO configuration) throws Exception {
        final String factoryPid = configuration.factoryPid;
        final String pid        = configuration.pid;
        if (factoryPid != null) {
            final Configuration[] factoryConfigurations = configAdmin.listConfigurations("(service.factoryPid=" + factoryPid + ")");
            for (final Configuration config : factoryConfigurations) {
                config.delete();
            }
        } else {
            final Configuration config = configAdmin.getConfiguration(pid, "?");
            config.delete();
        }
    }

    private Optional<Bundle> getExistingBundle(final FeatureBundleDTO bundle) {
        return Stream.of(bundleContext.getBundles()).filter(b -> b.getSymbolicName().equals(bundle.id.artifactId)).findAny();
    }

    private Optional<Bundle> getExistingBundle(final FeatureBundle bundle) {
        return Stream.of(bundleContext.getBundles()).filter(b -> b.getSymbolicName().equals(bundle.getID().getArtifactId())).findAny();
    }

    private static String readAttributeFromManifest(final File jarResource, final String attribute) throws Exception {
        try (FileInputStream is = new FileInputStream(jarResource); JarInputStream jarStream = new JarInputStream(is);) {
            final Manifest manifest = jarStream.getManifest();
            if (manifest == null) {
                throw new RuntimeException(jarResource + " is not a valid JAR");
            }
            final String value = manifest.getMainAttributes().getValue(attribute);
            if (value.contains(";")) {
                return value.split(";")[0];
            }
            return value;
        }
    }

    private File downloadArchive(final URL url) throws IOException {
        logger.atInfo().log("Downloading archive from URL '%s'", url);

        final File outputPath = new File(cachedDirectory.toFile(), "archive.zip");

        try (ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
                FileOutputStream fileOutputStream = new FileOutputStream(outputPath)) {
            final FileChannel fileChannel = fileOutputStream.getChannel();
            fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        } catch (final MalformedURLException e) {
            logger.atError().withException(e).log("Invalid URL - '%s'", url);
        } catch (final IOException e) {
            logger.atError().withException(e).log("Download failed from '%s' to '%s'", url, outputPath);
        }
        logger.atInfo().log("Downloaded archive from URL '%s'", url);
        return outputPath;
    }

    private List<File> extractFeatures(final File archive) throws IOException {
        unzip(archive);
        final File[] files = cachedDirectory.toFile().listFiles((FilenameFilter) (dir, name) -> name.endsWith(".json"));
        return Stream.of(files).collect(Collectors.toList());
    }

    public void cleanDirectory(final Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(final Path file, final IOException e) {
                return handleException(e);
            }

            private FileVisitResult handleException(final IOException e) {
                logger.atError().withException(e).log("Exception occurred while walking down the file tree");
                return FileVisitResult.TERMINATE;
            }

            @Override
            public FileVisitResult postVisitDirectory(final Path dir, final IOException e) throws IOException {
                if (e != null) {
                    return handleException(e);
                }
                if (path != dir) {
                    Files.delete(dir);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void unzip(final File zipFilePath) throws IOException {
        cleanDirectory(cachedDirectory);
        try (final ZipFile zipFile = new ZipFile(zipFilePath)) {
            final Enumeration<?> enu = zipFile.entries();
            while (enu.hasMoreElements()) {
                final ZipEntry zipEntry = (ZipEntry) enu.nextElement();

                final String name           = zipEntry.getName();
                final long   size           = zipEntry.getSize();
                final long   compressedSize = zipEntry.getCompressedSize();
                logger.atInfo().log("[Extracting Archive] Name: %s | Size: %s | Compressed Size: %s ", name, size, compressedSize);

                final File file = new File(cachedDirectory.toFile(), name);
                if (name.endsWith("/")) {
                    file.mkdirs();
                    continue;
                }

                final File parent = file.getParentFile();
                if (parent != null) {
                    parent.mkdirs();
                }

                try (InputStream is = zipFile.getInputStream(zipEntry); FileOutputStream fos = new FileOutputStream(file)) {
                    final byte[] bytes = new byte[1024];
                    int          length;
                    while ((length = is.read(bytes)) >= 0) {
                        fos.write(bytes, 0, length);
                    }
                }
            }
        }
    }

    private void storeFeature(final FeatureDTO dto) throws IOException {
        final Configuration        configuration = configAdmin.getConfiguration(FEATURE_STORAGE_PID, "?");
        Dictionary<String, Object> properties    = configuration.getProperties();
        if (properties == null) {
            properties = new Hashtable<>();
        }
        final Object features = properties.get(CONFIG_KEY);
        final Gson   gson     = new Gson();
        if (features == null) {
            final String json = gson.toJson(Lists.newArrayList(dto).toArray(new FeatureDTO[0]));

            final Map<String, Object> props = new HashMap<>();
            props.put("features", json);

            configuration.update(new Hashtable<>(props));
        } else {
            final FeatureDTO[] result = gson.fromJson(features.toString(), FeatureDTO[].class);

            final List<FeatureDTO> toBeStored = Lists.newArrayList(result);
            toBeStored.removeIf(f -> checkIdEquals(f.id, dto.id));
            toBeStored.add(dto);

            final String              json  = gson.toJson(toBeStored);
            final Map<String, Object> props = new HashMap<>();

            props.put("features", json);
            configuration.update(new Hashtable<>(props));
        }
    }

    private Optional<FeatureDTO> removeFeature(final String featureId) throws IOException {
        final Configuration              configuration = configAdmin.getConfiguration(FEATURE_STORAGE_PID, "?");
        final Dictionary<String, Object> properties    = configuration.getProperties();
        final Object                     features      = properties.get(CONFIG_KEY);
        final Gson                       gson          = new Gson();
        if (features == null) {
            return Optional.empty();
        }
        final String[]     fs     = (String[]) features;
        final FeatureDTO[] result = Stream.of(fs).map(e -> gson.fromJson(e, FeatureDTO.class)).toArray(FeatureDTO[]::new);

        final List<FeatureDTO>     finalList           = Lists.newArrayList(result);
        final Optional<FeatureDTO> featuredToBeRemoved = finalList.stream().filter(f -> checkIdEquals(f.id, featureId)).findAny();
        finalList.removeIf(f -> checkIdEquals(f.id, featureId));

        final String              json  = gson.toJson(finalList);
        final Map<String, Object> props = new HashMap<>();

        props.put("features", json);
        configuration.update(new Hashtable<>(props));
        return featuredToBeRemoved;
    }

    private boolean checkIdEquals(final IdDTO id1, final IdDTO id2) {
        final String idToCompare = id2.groupId + ":" + id2.artifactId + ":" + id2.version;
        return checkIdEquals(id1, idToCompare);
    }

    private boolean checkIdEquals(final IdDTO id, final String featureId) {
        final String idToCompare = id.groupId + ":" + id.artifactId + ":" + id.version;
        return idToCompare.equals(featureId);
    }

}
