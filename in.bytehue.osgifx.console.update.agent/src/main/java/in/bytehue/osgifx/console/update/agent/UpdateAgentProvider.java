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
import java.util.zip.ZipInputStream;

import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.LoggerFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
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

import in.bytehue.osgifx.console.feature.FeatureDTO;
import in.bytehue.osgifx.console.feature.IdDTO;
import in.bytehue.osgifx.console.update.UpdateAgent;

@Component
public final class UpdateAgentProvider implements UpdateAgent {

    private static final String LOCATION_PREFIX     = "osgifx://";
    private static final String STARTLEVEL_KEY      = "start-order";
    private static final String CONFIG_KEY          = "features";
    private static final String BUNDLES_DIRECTORY   = "bundles";
    private static final String FEATURE_STORAGE_PID = "fx.features";
    private static final int    DEAFULT_START_LEVEL = 100;

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
        cachedDirectory = Files.createTempDirectory("fx.console_");
    }

    @Deactivate
    void deactivate() throws IOException {
        deleteFileOrFolder(cachedDirectory);
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
    public boolean remove(final String featureId) {
        logger.atInfo().log("Removing feature: %s", featureId);
        try {
            removeFeature(featureId);
            return true;
        } catch (final Exception e) {
            return false;
        }
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
            }
        } else {
            final Bundle           installedBundle = bundleContext.installBundle(LOCATION_PREFIX + bsn);
            final BundleStartLevel sl              = installedBundle.adapt(BundleStartLevel.class);
            sl.setStartLevel(startLevel);
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
        final File   bundleDir = new File(directory, BUNDLES_DIRECTORY);
        final File[] files     = bundleDir.listFiles();
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

    private Optional<Bundle> getExistingBundle(final FeatureBundle bundle) {
        return Stream.of(bundleContext.getBundles()).filter(b -> b.getSymbolicName().equals(bundle.getID().getArtifactId())).findAny();
    }

    private static String readAttributeFromManifest(final File jarResource, final String attribute) throws Exception {
        try (FileInputStream is = new FileInputStream(jarResource); JarInputStream jarStream = new JarInputStream(is);) {
            final Manifest manifest = jarStream.getManifest();
            if (manifest == null) {
                throw new RuntimeException(jarResource + " is not a valid JAR");
            }
            return manifest.getMainAttributes().getValue(attribute);
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

    private List<File> extractFeatures(final File archive) {
        unzip(archive, cachedDirectory.toFile());
        final File[] files = cachedDirectory.toFile().listFiles((FilenameFilter) (dir, name) -> name.endsWith(".json"));
        return Stream.of(files).collect(Collectors.toList());
    }

    public static void deleteFileOrFolder(final Path path) throws IOException {
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
                e.printStackTrace(); // replace with more robust error handling
                return FileVisitResult.TERMINATE;
            }

            @Override
            public FileVisitResult postVisitDirectory(final Path dir, final IOException e) throws IOException {
                if (e != null) {
                    return handleException(e);
                }
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void unzip(final File zipFilePath, final File destDir) {
        // create output directory if it doesn't exist
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        FileInputStream fis;
        // buffer for read and write data to file
        final byte[] buffer = new byte[1024];
        try {
            fis = new FileInputStream(zipFilePath);
            final ZipInputStream zis = new ZipInputStream(fis);
            ZipEntry             ze  = zis.getNextEntry();
            while (ze != null) {
                final String fileName = ze.getName();
                final File   newFile  = new File(destDir + File.separator + fileName);
                logger.atInfo().log("Unzipping to %s", newFile.getAbsolutePath());
                // create directories for sub directories in zip
                new File(newFile.getParent()).mkdirs();
                final FileOutputStream fos = new FileOutputStream(newFile);
                int                    len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
                // close this ZipEntry
                zis.closeEntry();
                ze = zis.getNextEntry();
            }
            // close last ZipEntry
            zis.closeEntry();
            zis.close();
            fis.close();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void storeFeature(final FeatureDTO dto) throws IOException {
        final Configuration              configuration = configAdmin.getConfiguration(FEATURE_STORAGE_PID, "?");
        final Dictionary<String, Object> properties    = configuration.getProperties();
        final Object                     features      = properties.get(CONFIG_KEY);
        final Gson                       gson          = new Gson();
        if (features == null) {
            final String json = gson.toJson(Lists.newArrayList(dto).toArray(new FeatureDTO[0]));

            final Map<String, Object> props = new HashMap<>();
            props.put("features", json);

            configuration.update(new Hashtable<>(props));
        } else {
            final String[]     fs     = (String[]) features;
            final FeatureDTO[] result = Stream.of(fs).map(e -> gson.fromJson(e, FeatureDTO.class)).toArray(FeatureDTO[]::new);

            final List<FeatureDTO> toBeStored = Lists.newArrayList(result);
            toBeStored.add(dto);

            final String              json  = gson.toJson(toBeStored);
            final Map<String, Object> props = new HashMap<>();

            props.put("features", json);
            configuration.update(new Hashtable<>(props));
        }
    }

    private void removeFeature(final String featureId) throws IOException {
        final Configuration              configuration = configAdmin.getConfiguration(FEATURE_STORAGE_PID, "?");
        final Dictionary<String, Object> properties    = configuration.getProperties();
        final Object                     features      = properties.get(CONFIG_KEY);
        final Gson                       gson          = new Gson();
        if (features == null) {
            return;
        }
        final String[]     fs     = (String[]) features;
        final FeatureDTO[] result = Stream.of(fs).map(e -> gson.fromJson(e, FeatureDTO.class)).toArray(FeatureDTO[]::new);

        final List<FeatureDTO> finalList = Lists.newArrayList(result);
        finalList.removeIf(f -> checkIdEquals(f.id, featureId));

        final String              json  = gson.toJson(finalList);
        final Map<String, Object> props = new HashMap<>();

        props.put("features", json);
        configuration.update(new Hashtable<>(props));
    }

    private boolean checkIdEquals(final IdDTO id, final String featureId) {
        final String idToCompare = id.groupId + ":" + id.artifactId + ":" + id.version;
        return idToCompare.equals(featureId);
    }

}
