/*******************************************************************************
 * Copyright 2022 Amit Kumar Mondal
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
package com.osgifx.console.feature.exporter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

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
		try {
			final var id             = options.get("id");
			final var groupID        = options.get("groupID");
			final var name           = options.get("name");
			final var description    = options.get("description");
			final var license        = options.get("license");
			final var docURL         = options.get("docURL");
			final var scm            = options.get("scm");
			final var vendor         = options.get("vendor");
			final var categories     = options.get("categories");
			final var variables      = options.get("variables");
			final var configurations = options.get("configurations");
			final var output         = options.get("output");

			checkPrecondition(project, id, "Feature ID not set");
			checkPrecondition(project, groupID, "Feature group ID not set");
			checkPrecondition(project, output, "Feature output filename not set");

			final var resources = project.getRunbundles();
			final var feature   = new FeatureDTO();

			feature.id             = id;
			feature.name           = name;
			feature.description    = description;
			feature.license        = license;
			feature.docURL         = docURL;
			feature.scm            = scm;
			feature.vendor         = vendor;
			feature.categories     = parseCategories(categories);
			feature.variables      = parseVariables(variables);
			feature.configurations = parseConfigurations(configurations);
			feature.bundles        = prepareBundles(groupID, resources);

			final var outputZIP = new File(project.getTargetDir(), output);
			createZip(feature, outputZIP);

			if (project.isOk()) {
				final var result = new FileResource(outputZIP);
				return new SimpleEntry<>("feature", result);
			}
		} catch (final Exception e) {
			try {
				IO.write(Throwables.getStackTraceAsString(e).getBytes(), new File(project.getBase(), "exception.txt"));
			} catch (final IOException e1) {
				project.exception(e, "Cannot write exception text file");
			}
			project.exception(e, "Cannot create feature archive");
			throw e;
		}
		return null;
	}

	private static Map<String, Map<String, String>> parseConfigurations(final String configurations) {
		if (configurations == null) {
			return Collections.emptyMap();
		}
		final var configMap = new TypeToken<Map<String, Map<String, String>>>() {
		}.getType();
		return new Gson().fromJson(configurations, configMap);
	}

	private List<FeatureBundleDTO> prepareBundles(final String groupID, final Collection<Container> resources) throws Exception {
		final List<FeatureBundleDTO> bundles = new ArrayList<>();
		for (final Container container : resources) {
			final var file       = container.getFile();
			final var attributes = container.getAttributes();
			final var startLevel = attributes.get("startlevel");
			final var bundleID   = getBundleID(file);

			final var dto = new FeatureBundleDTO();
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

	private List<String> parseCategories(final String categories) throws IOException {
		if (categories == null) {
			return List.of();
		}
		return Splitter.on(PATTERN).splitToList(categories);
	}

	private BundleID getBundleID(final File jarFile) throws Exception {
		try (final var jar = new Jar(jarFile)) {
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

	private void createZip(final FeatureDTO feature, final File outputZIP) throws Exception {
		try (final var zipFile = new ZipFile(outputZIP)) {

			final var bundlesDir = Paths.get("bundles");
			IO.delete(bundlesDir);
			IO.mkdirs(bundlesDir);

			for (final FeatureBundleDTO bundle : feature.bundles) {
				final var bundleJar = Paths.get(bundle.file);
				Files.copy(bundleJar, bundlesDir.resolve(bundleJar.getFileName()));
				bundle.file = null; // we don't want to include the file location in JSON
			}
			zipFile.addFolder(bundlesDir.toFile());

			final var gson        = new GsonBuilder().setPrettyPrinting().create();
			final var json        = gson.toJson(feature);
			final var featureFile = new File("feature.json");

			IO.write(json.getBytes(), featureFile);
			zipFile.addFile(featureFile);
			IO.delete(featureFile);
		}
	}

}
