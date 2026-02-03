/*******************************************************************************
 * Copyright 2021-2026 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.bnd.dp.packager;

import static com.osgifx.console.bnd.dp.packager.DeploymentPackageHeaders.DEPLOYMENT_PACKAGE_CONTACT_ADDRESS;
import static com.osgifx.console.bnd.dp.packager.DeploymentPackageHeaders.DEPLOYMENT_PACKAGE_COPYRIGHT;
import static com.osgifx.console.bnd.dp.packager.DeploymentPackageHeaders.DEPLOYMENT_PACKAGE_CUSTOMIZER;
import static com.osgifx.console.bnd.dp.packager.DeploymentPackageHeaders.DEPLOYMENT_PACKAGE_DESCRIPTION;
import static com.osgifx.console.bnd.dp.packager.DeploymentPackageHeaders.DEPLOYMENT_PACKAGE_DOC_URL;
import static com.osgifx.console.bnd.dp.packager.DeploymentPackageHeaders.DEPLOYMENT_PACKAGE_FIX_PACK;
import static com.osgifx.console.bnd.dp.packager.DeploymentPackageHeaders.DEPLOYMENT_PACKAGE_ICON;
import static com.osgifx.console.bnd.dp.packager.DeploymentPackageHeaders.DEPLOYMENT_PACKAGE_LICENSE;
import static com.osgifx.console.bnd.dp.packager.DeploymentPackageHeaders.DEPLOYMENT_PACKAGE_NAME;
import static com.osgifx.console.bnd.dp.packager.DeploymentPackageHeaders.DEPLOYMENT_PACKAGE_REQUIRED_STORAGE;
import static com.osgifx.console.bnd.dp.packager.DeploymentPackageHeaders.DEPLOYMENT_PACKAGE_RESOURCE_BUNDLE_SYMBOLIC_NAME;
import static com.osgifx.console.bnd.dp.packager.DeploymentPackageHeaders.DEPLOYMENT_PACKAGE_RESOURCE_BUNDLE_VERSION;
import static com.osgifx.console.bnd.dp.packager.DeploymentPackageHeaders.DEPLOYMENT_PACKAGE_RESOURCE_PROCESSOR_PID;
import static com.osgifx.console.bnd.dp.packager.DeploymentPackageHeaders.DEPLOYMENT_PACKAGE_RESOURCE_SHA_DIGEST;
import static com.osgifx.console.bnd.dp.packager.DeploymentPackageHeaders.DEPLOYMENT_PACKAGE_SYMBOLIC_NAME;
import static com.osgifx.console.bnd.dp.packager.DeploymentPackageHeaders.DEPLOYMENT_PACKAGE_VENDOR;
import static com.osgifx.console.bnd.dp.packager.DeploymentPackageHeaders.DEPLOYMENT_PACKAGE_VERSION;
import static java.util.Objects.requireNonNull;
import static java.util.jar.Attributes.Name.MANIFEST_VERSION;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.hash.Hashing;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.osgi.FileResource;
import aQute.bnd.osgi.Jar;
import aQute.bnd.service.export.Exporter;
import aQute.lib.io.IO;

@BndPlugin(name = "OSGi Deployment Package Exporter")
public class DeploymentPackageExporter implements Exporter {

    private static final String  TYPE    = "osgi.dp.exporter";
    private static final Pattern PATTERN = Pattern.compile(",\\s*");

    private Project project;

    @Override
    public String[] getTypes() {
        return new String[] { TYPE };
    }

    @Override
    public Entry<String, aQute.bnd.osgi.Resource> export(final String type,
                                                         final Project project,
                                                         final Map<String, String> options) throws Exception {
        this.project = project;
        try {
            final var symbolicName       = options.get("symbolicName");
            final var version            = options.get("version");
            final var fixpack            = options.get("fixpack");
            final var name               = options.get("name");
            final var copyright          = options.get("copyright");
            final var contact            = options.get("contact");
            final var description        = options.get("scm");
            final var docURL             = options.get("docURL");
            final var icon               = options.get("icon");
            final var vendor             = options.get("vendor");
            final var license            = options.get("license");
            final var customizer         = options.get("customizer");
            final var requiredStorage    = options.get("requiredStorage");
            final var output             = options.get("output");
            final var resources          = options.get("resources");
            final var resourceProcessors = options.get("resourceProcessors");

            checkPrecondition(project, symbolicName, "Deployment package symbolic name not set");
            checkPrecondition(project, version, "Deployment package version not set");
            checkPrecondition(project, output, "Deployment package output filename not set");

            final var bundles           = project.getRunbundles();
            final var deploymentPackage = new DeploymentPackageDTO();

            deploymentPackage.symbolicName    = symbolicName;
            deploymentPackage.version         = version;
            deploymentPackage.fixPack         = fixpack;
            deploymentPackage.name            = name;
            deploymentPackage.copyright       = copyright;
            deploymentPackage.contactAddress  = contact;
            deploymentPackage.description     = description;
            deploymentPackage.docURL          = docURL;
            deploymentPackage.icon            = icon;
            deploymentPackage.vendor          = vendor;
            deploymentPackage.license         = license;
            deploymentPackage.isCustomizer    = customizer;
            deploymentPackage.requiredStorage = requiredStorage;
            deploymentPackage.entries         = prepareEntries(bundles, resourceProcessors, resources);

            final var outputJar = new File(project.getTargetDir(), output);
            createJar(deploymentPackage, outputJar);

            if (project.isOk()) {
                final var result = new FileResource(outputJar);
                return new SimpleEntry<>("deployment-package", result);
            }
        } catch (final Exception e) {
            logException(e);
            project.exception(e, "Cannot create deployment package");
            throw e;
        }
        return null;
    }

    private void logException(final Exception e) {
        try {
            IO.write(Throwables.getStackTraceAsString(e).getBytes(), new File(project.getBase(), "exception.txt"));
        } catch (final IOException e1) {
            project.exception(e, "Cannot write exception text file");
        }
    }

    private Collection<DeploymentPackageEntryDTO> prepareEntries(final Collection<Container> bundles,
                                                                 final String resourceProcessors,
                                                                 final String resources) throws Exception {
        final List<DeploymentPackageEntryDTO> entries = Lists.newArrayList();

        final var resBundles            = prepareBundles(bundles);
        final var resResources          = prepareResources(resources);
        final var resResourceProcessors = prepareResourceProcessors(resourceProcessors);

        entries.addAll(resBundles);
        entries.addAll(resResources);
        entries.addAll(resResourceProcessors);

        return entries;
    }

    private List<DeploymentPackageBundleDTO> prepareBundles(final Collection<Container> resources) throws Exception {
        final List<DeploymentPackageBundleDTO> bundles = new ArrayList<>();
        for (final Container container : resources) {
            final var file = container.getFile();
            try (final var jar = new Jar(file)) {
                final var dto = new DeploymentPackageBundleDTO();

                dto.name         = "bundles/" + FilenameUtils.getName(file.getAbsolutePath());
                dto.symbolicName = jar.getBsn();
                dto.version      = jar.getVersion();
                dto.sha          = sha1(file);
                dto.file         = file;

                bundles.add(dto);
            }
        }
        return bundles;
    }

    private List<DeploymentPackageEntryDTO> prepareResources(final String resources) throws Exception {
        if (resources == null) {
            return List.of();
        }
        final List<DeploymentPackageEntryDTO> result = Lists.newArrayList();
        final var                             res    = Splitter.on(PATTERN).split(resources);
        for (final String resource : res) {
            final var file = findFile(resource);
            if (file == null) {
                final var message = "Resource '" + resource + "' does not exist in the project";
                project.error(message);
                throw new FileNotFoundException(message);
            }
            final var dto = new DeploymentPackageEntryDTO();

            dto.name = resource;
            dto.sha  = sha1(file);
            dto.file = file;

            result.add(dto);
        }
        return result;
    }

    private List<DeploymentPackageResourceProcessorDTO> prepareResourceProcessors(final String resourceProcessors) throws Exception {
        if (resourceProcessors == null) {
            return List.of();
        }
        final List<DeploymentPackageResourceProcessorDTO> res = Lists.newArrayList();
        final var                                         map = Splitter.on(PATTERN).withKeyValueSeparator('=')
                .split(resourceProcessors);

        for (final Entry<String, String> entry : map.entrySet()) {
            final var name = entry.getKey();
            final var pid  = entry.getValue();
            final var file = findFile(name);

            final var dto = new DeploymentPackageResourceProcessorDTO();
            dto.name = name;
            if (file.exists()) {
                // calculate SHA only if the resource exists
                dto.sha = sha1(file);
            }
            dto.file                 = file;
            dto.resourceProcessorPID = pid;

            res.add(dto);
        }
        return res;
    }

    @SuppressWarnings("deprecation")
    private String sha1(final File file) throws IOException {
        return com.google.common.io.Files.asByteSource(file).hash(Hashing.sha1()).toString();
    }

    private File findFile(final String name) {
        return project.getFile(name);
    }

    private void checkPrecondition(final Project project, final String id, final String message) {
        if (id == null) {
            project.error(message);
            requireNonNull(id, message);
        }
    }

    private void createJar(final DeploymentPackageDTO deploymentPackage, final File outputJar) throws Exception {
        final var entries        = deploymentPackage.entries;
        final var directoryToZip = new File(project.getBase(), "dp");
        IO.mkdirs(directoryToZip);

        for (final DeploymentPackageEntryDTO entry : entries) {
            final var subDirectory = findDirectoryInEntryName(entry);
            if (subDirectory != null) {
                final var subDirectoryFile = new File(directoryToZip.getAbsolutePath(), subDirectory.getName());
                FileUtils.copyToDirectory(entry.file, subDirectoryFile);
            }
        }
        final var metaInfDirectory = new File(directoryToZip, "META-INF");
        final var manifestFile     = prepareManifest(deploymentPackage);

        FileUtils.moveFileToDirectory(manifestFile, metaInfDirectory, true);
        jarPkg(directoryToZip.toPath(), outputJar.toPath());
        FileUtils.forceDelete(directoryToZip);
    }

    private void jarPkg(final Path sourceFolder, final Path targetJar) throws Exception {
        try (final var jos = new JarOutputStream(new FileOutputStream(targetJar.toFile()))) {
            Files.walkFileTree(sourceFolder, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                    final var entry = sourceFolder.relativize(file).toString();
                    jos.putNextEntry(new JarEntry(entry));
                    Files.copy(file, jos);
                    jos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    private File prepareManifest(final DeploymentPackageDTO deploymentPackage) throws Exception {
        final var manifest = new Manifest();
        final var attrs    = manifest.getMainAttributes();
        attrs.put(MANIFEST_VERSION, "1.0.0");

        initMainManifestAttributes(deploymentPackage, manifest);
        initNameManifestAttributes(deploymentPackage, manifest);

        final var manifestFile = IO.getFile("MANIFEST.MF");
        try (final OutputStream outputStream = new FileOutputStream(manifestFile)) {
            manifest.write(outputStream);
        }
        return manifestFile;
    }

    private File findDirectoryInEntryName(final DeploymentPackageEntryDTO entry) {
        final var lastPathSeparatorIndex = entry.name.lastIndexOf("/");
        if (lastPathSeparatorIndex == -1) {
            return null;
        }
        final var directory = entry.name.substring(0, lastPathSeparatorIndex);
        return IO.getFile(directory);
    }

    private void initMainManifestAttributes(final DeploymentPackageDTO dp, final Manifest manifest) throws Exception {
        addToMainManifest(manifest, DEPLOYMENT_PACKAGE_SYMBOLIC_NAME, dp.symbolicName);
        addToMainManifest(manifest, DEPLOYMENT_PACKAGE_VERSION, dp.version);
        addToMainManifest(manifest, DEPLOYMENT_PACKAGE_FIX_PACK, dp.fixPack);
        addToMainManifest(manifest, DEPLOYMENT_PACKAGE_NAME, dp.name);
        addToMainManifest(manifest, DEPLOYMENT_PACKAGE_COPYRIGHT, dp.copyright);
        addToMainManifest(manifest, DEPLOYMENT_PACKAGE_CONTACT_ADDRESS, dp.contactAddress);
        addToMainManifest(manifest, DEPLOYMENT_PACKAGE_DESCRIPTION, dp.description);
        addToMainManifest(manifest, DEPLOYMENT_PACKAGE_DOC_URL, dp.docURL);
        addToMainManifest(manifest, DEPLOYMENT_PACKAGE_ICON, dp.icon);
        addToMainManifest(manifest, DEPLOYMENT_PACKAGE_VENDOR, dp.vendor);
        addToMainManifest(manifest, DEPLOYMENT_PACKAGE_LICENSE, dp.license);
        addToMainManifest(manifest, DEPLOYMENT_PACKAGE_REQUIRED_STORAGE, dp.requiredStorage);
        addToMainManifest(manifest, DEPLOYMENT_PACKAGE_CUSTOMIZER, dp.isCustomizer);
    }

    private void initNameManifestAttributes(final DeploymentPackageDTO dp, final Manifest manifest) throws Exception {
        for (final DeploymentPackageEntryDTO e : dp.entries) {
            final var attrs = new Attributes();
            String    name  = null;
            if (e instanceof final DeploymentPackageBundleDTO dpb) {
                addToNameManifest(attrs, DEPLOYMENT_PACKAGE_RESOURCE_BUNDLE_SYMBOLIC_NAME, dpb.symbolicName);
                addToNameManifest(attrs, DEPLOYMENT_PACKAGE_RESOURCE_BUNDLE_VERSION, dpb.version);
                addToNameManifest(attrs, DEPLOYMENT_PACKAGE_RESOURCE_SHA_DIGEST, dpb.sha);
                name = dpb.name;
            } else if (e instanceof final DeploymentPackageResourceProcessorDTO dprp) {
                addToNameManifest(attrs, DEPLOYMENT_PACKAGE_RESOURCE_SHA_DIGEST, dprp.sha);
                addToNameManifest(attrs, DEPLOYMENT_PACKAGE_RESOURCE_PROCESSOR_PID, dprp.resourceProcessorPID);
                name = dprp.name;
            } else {
                addToNameManifest(attrs, DEPLOYMENT_PACKAGE_RESOURCE_SHA_DIGEST, e.sha);
                name = e.name;
            }
            manifest.getEntries().put(name, attrs);
        }
    }

    private void addToMainManifest(final Manifest manifest, final String header, final String value) {
        final var mainAttributes = manifest.getMainAttributes();
        if (value != null) {
            mainAttributes.putValue(new Attributes.Name(header).toString(), value);
        }
    }

    private void addToNameManifest(final Attributes attributes, final String header, final String value) {
        if (value != null) {
            attributes.putValue(header, value);
        }
    }

}