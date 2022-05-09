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
package com.osgifx.console.agent.handler;

import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;

import com.osgifx.console.agent.dto.XFrameworkEventDTO;
import com.osgifx.console.supervisor.Supervisor;

public final class OSGiFrameworkEventHandler implements FrameworkListener {

	private final Supervisor supervisor;

	public OSGiFrameworkEventHandler(final Supervisor supervisor) {
		this.supervisor = supervisor;
	}

	@Override
	public void frameworkEvent(final FrameworkEvent event) {
		final XFrameworkEventDTO dto = new XFrameworkEventDTO();

		dto.received        = System.currentTimeMillis();
		dto.eventType       = event.getType();
		dto.sourceBundleBsn = event.getBundle().getSymbolicName();
		dto.throwable       = OSGiLogListener.toExceptionString(event.getThrowable());

		supervisor.onFrameworkEvent(dto);
	}

}
