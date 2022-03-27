/*******************************************************************************
 * Copyright 2021-2022 Amit Kumar Mondal
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
package com.osgifx.console.feature;

import java.util.List;
import java.util.Map;

import org.osgi.dto.DTO;

public class FeatureDTO extends DTO {

	public String                               archiveURL;
	public IdDTO                                id;
	public String                               name;
	public List<String>                         categories;
	public String                               description;
	public String                               docURL;
	public String                               vendor;
	public String                               license;
	public String                               scm;
	public boolean                              isComplete;
	public List<FeatureBundleDTO>               bundles;
	public Map<String, FeatureConfigurationDTO> configurations;
	public Map<String, FeatureExtensionDTO>     extensions;

}
