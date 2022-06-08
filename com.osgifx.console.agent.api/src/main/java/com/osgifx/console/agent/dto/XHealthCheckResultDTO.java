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
package com.osgifx.console.agent.dto;

import java.util.List;

import org.osgi.dto.DTO;

public class XHealthCheckResultDTO extends DTO {

	public static class ResultDTO extends DTO {
		public String status;
		public String message;
		public String logLevel;
		public String exception;
	}

	public String          healthCheckName;
	public List<String>    healthCheckTags;
	public List<ResultDTO> results;
	public long            elapsedTime;
	public long            finishedAt;
	public boolean         isTimedOut;

}
