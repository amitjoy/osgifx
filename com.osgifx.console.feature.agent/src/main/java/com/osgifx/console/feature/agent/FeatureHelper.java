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
package com.osgifx.console.feature.agent;

import static java.util.stream.Collectors.toList;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureArtifact;
import org.osgi.service.feature.FeatureBundle;
import org.osgi.service.feature.FeatureConfiguration;
import org.osgi.service.feature.FeatureExtension;
import org.osgi.service.feature.ID;

import com.osgifx.console.feature.FeatureArtifactDTO;
import com.osgifx.console.feature.FeatureBundleDTO;
import com.osgifx.console.feature.FeatureConfigurationDTO;
import com.osgifx.console.feature.FeatureDTO;
import com.osgifx.console.feature.FeatureExtensionDTO;
import com.osgifx.console.feature.IdDTO;

public final class FeatureHelper {

    private FeatureHelper() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static IdDTO toIdDTO(final ID id) {
        final IdDTO idDTO = new IdDTO();

        idDTO.groupId    = id.getGroupId();
        idDTO.artifactId = id.getArtifactId();
        idDTO.version    = id.getVersion();
        idDTO.type       = id.getType().orElse(null);
        idDTO.classifier = id.getClassifier().orElse(null);

        return idDTO;
    }

    public static FeatureBundleDTO toFeatureBundleDTO(final FeatureBundle bundle) {
        final FeatureBundleDTO dto = new FeatureBundleDTO();

        dto.id       = toIdDTO(bundle.getID());
        dto.metadata = new HashMap<>(bundle.getMetadata());

        return dto;
    }

    public static FeatureConfigurationDTO toFeatureConfigDTO(final FeatureConfiguration config) {
        final FeatureConfigurationDTO dto = new FeatureConfigurationDTO();

        dto.pid        = config.getPid();
        dto.factoryPid = config.getFactoryPid().orElse(null);
        dto.values     = new HashMap<>(config.getValues());

        return dto;
    }

    public static FeatureArtifactDTO toFeatureArtifactDTO(final FeatureArtifact artifact) {
        final FeatureArtifactDTO dto = new FeatureArtifactDTO();

        dto.id       = toIdDTO(artifact.getID());
        dto.metadata = new HashMap<>(artifact.getMetadata());

        return dto;
    }

    public static FeatureExtensionDTO toFeatureExtensionDTO(final FeatureExtension extension) {
        final FeatureExtensionDTO dto = new FeatureExtensionDTO();

        dto.name      = extension.getName();
        dto.type      = extension.getType();
        dto.kind      = extension.getKind();
        dto.json      = extension.getJSON();
        dto.text      = extension.getText();
        dto.artifacts = extension.getArtifacts().stream().map(FeatureHelper::toFeatureArtifactDTO).collect(toList());

        return dto;
    }

    public static FeatureDTO toFeature(final Feature feature) {
        final FeatureDTO dto = new FeatureDTO();

        dto.id             = toIdDTO(feature.getID());
        dto.name           = feature.getName().orElse(null);
        dto.categories     = feature.getCategories();
        dto.description    = feature.getDescription().orElse(null);
        dto.docURL         = feature.getDocURL().orElse(null);
        dto.vendor         = feature.getVendor().orElse(null);
        dto.license        = feature.getLicense().orElse(null);
        dto.scm            = feature.getSCM().orElse(null);
        dto.isComplete     = feature.isComplete();
        dto.bundles        = feature.getBundles().stream().map(FeatureHelper::toFeatureBundleDTO).collect(toList());
        dto.configurations = feature.getConfigurations().entrySet().stream()
                .collect(Collectors.toMap(Entry::getKey, e -> toFeatureConfigDTO(e.getValue())));
        dto.extensions     = feature.getExtensions().entrySet().stream()
                .collect(Collectors.toMap(Entry::getKey, e -> toFeatureExtensionDTO(e.getValue())));

        return dto;

    }

}
