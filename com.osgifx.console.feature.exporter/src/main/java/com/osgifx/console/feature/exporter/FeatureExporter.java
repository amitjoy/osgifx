package com.osgifx.console.feature.exporter;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import com.google.common.base.Splitter;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.osgi.FileResource;
import aQute.bnd.osgi.Jar;
import aQute.bnd.service.export.Exporter;
import aQute.lib.io.IO;
import net.lingala.zip4j.ZipFile;

@BndPlugin(name = "OSGi.fx Feature Exporter")
public class FeatureExporter implements Exporter {

    private static final Pattern PATTERN = Pattern.compile(",\\s*");
    private static final String  TYPE    = "osgi.fx.feature.exporter";

    @Override
    public String[] getTypes() {
        return new String[] { TYPE };
    }

    @Override
    public Entry<String, aQute.bnd.osgi.Resource> export(final String type, final Project project, final Map<String, String> options)
            throws Exception {

        final String id             = options.get("id");
        final String groupID        = options.get("groupID");
        final String name           = options.get("name");
        final String description    = options.get("description");
        final String license        = options.get("license");
        final String docURL         = options.get("docURL");
        final String scm            = options.get("scm");
        final String vendor         = options.get("vendor");
        final String categories     = options.get("categories");
        final String variables      = options.get("variables");
        final String configurations = options.get("configurations");
        final String output         = options.get("output");

        checkPrecondition(project, id, "Feature ID not set");
        checkPrecondition(project, groupID, "Feature group ID not set");
        checkPrecondition(project, output, "Feature output filename not set");

        final Collection<Container> resources = project.getRunbundles();
        final FeatureDTO            feature   = new FeatureDTO();

        feature.id             = id;
        feature.name           = name;
        feature.description    = description;
        feature.license        = license;
        feature.docURL         = docURL;
        feature.scm            = scm;
        feature.vendor         = vendor;
        feature.categories     = parseCategories(project, categories);
        feature.variables      = parseVariables(variables);
        feature.configurations = parseConfigurations(configurations);
        feature.bundles        = prepareBundles(project, groupID, resources);

        final File outputZIP = new File(project.getTargetDir(), output);
        createZip(project, feature, outputZIP);

        if (project.isOk()) {
            final FileResource result = new FileResource(outputZIP);
            return new SimpleEntry<>("feature", result);
        }
        return null;
    }

    private Map<String, Map<String, String>> parseConfigurations(final String configurations) {
        if (configurations == null) {
            return Collections.emptyMap();
        }
        final Map<String, Map<String, String>> configs    = new HashMap<>();
        final List<String>                     firstSplit = Splitter.on("],").splitToList(configurations);
        for (final String firstPart : firstSplit) {
            final List<String>        secondSplit = Splitter.on("=[").splitToList(firstPart);
            final Map<String, String> innerConfig = Splitter.on(PATTERN).withKeyValueSeparator('=').split(secondSplit.get(1));
            configs.put(secondSplit.get(0), innerConfig);
        }
        return configs;
    }

    private List<FeatureBundleDTO> prepareBundles(final Project project, final String groupID, final Collection<Container> resources)
            throws Exception {
        final List<FeatureBundleDTO> bundles = new ArrayList<>();
        for (final Container container : resources) {
            final File                file       = container.getFile();
            final Map<String, String> attributes = container.getAttributes();
            final String              startLevel = attributes.get("startlevel");
            final BundleID            bundleID   = getBundleID(file);

            final FeatureBundleDTO dto = new FeatureBundleDTO();
            dto.id         = groupID + ":" + bundleID.bsn + ":" + bundleID.version;
            dto.file       = file.getAbsolutePath();
            dto.startlevel = startLevel == null ? null : Integer.parseInt(startLevel);

            bundles.add(dto);
        }
        return bundles;
    }

    private Map<String, String> parseVariables(final String variables) {
        if (variables == null) {
            return Collections.emptyMap();
        }
        return Splitter.on(PATTERN).withKeyValueSeparator('=').split(variables);
    }

    private List<String> parseCategories(final Project project, final String categories) throws IOException {
        if (categories == null) {
            return Collections.emptyList();
        }
        return Splitter.on(PATTERN).splitToList(categories);
    }

    private BundleID getBundleID(final File jarFile) throws Exception {
        try (final Jar jar = new Jar(jarFile)) {
            return new BundleID(jar.getBsn(), jar.getVersion());
        }
    }

    private void checkPrecondition(final Project project, final String id, final String message) {
        if (id == null) {
            project.error(message);
            throw new RuntimeException(message);
        }
    }

    private static class BundleID {
        String bsn;
        String version;

        public BundleID(final String bsn, final String version) {
            this.bsn     = bsn;
            this.version = version;
        }
    }

    private void createZip(final Project project, final FeatureDTO feature, final File outputZIP) {
        try {
            final ZipFile zipFile = new ZipFile(outputZIP);
            try {
                final File bundlesDir = new File("bundles");
                IO.mkdirs(bundlesDir);
                for (final FeatureBundleDTO bundle : feature.bundles) {
                    final File bundleJar = new File(bundle.file);
                    Files.copy(bundleJar, new File(bundlesDir, bundleJar.getName()));
                    bundle.file = null; // we don't want to include in JSON
                }
                zipFile.addFolder(bundlesDir);

                final Gson   gson        = new GsonBuilder().setPrettyPrinting().create();
                final String json        = gson.toJson(feature);
                final File   featureFile = new File("feature.json");

                IO.write(json.getBytes(), featureFile);
                zipFile.addFile(featureFile);

                bundlesDir.delete();
                featureFile.delete();
            } finally {
                try {
                    zipFile.close();
                } catch (final IOException e) {
                    project.exception(e, "Cannot close feature archive");
                }
            }
        } catch (final Exception e) {
            project.exception(e, "Cannot create feature archive");
        }
    }

}