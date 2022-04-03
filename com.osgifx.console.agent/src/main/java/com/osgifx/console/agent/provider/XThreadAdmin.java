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
package com.osgifx.console.agent.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.osgifx.console.agent.dto.XThreadDTO;

public class XThreadAdmin {

	private XThreadAdmin() {
		throw new IllegalAccessError("Cannot be instantiated");
	}

	public static List<XThreadDTO> get() {
		final Map<Thread, StackTraceElement[]> threads    = Thread.getAllStackTraces();
		final List<Thread>                     threadList = new ArrayList<>(threads.keySet());
		return threadList.stream().map(XThreadAdmin::toDTO).collect(Collectors.toList());
	}

	private static XThreadDTO toDTO(final Thread thread) {
		final XThreadDTO dto = new XThreadDTO();

		dto.name          = thread.getName();
		dto.id            = thread.getId();
		dto.priority      = thread.getPriority();
		dto.state         = thread.getState().name();
		dto.isInterrupted = thread.isInterrupted();
		dto.isAlive       = thread.isAlive();
		dto.isDaemon      = thread.isDaemon();

		return dto;
	}

}
