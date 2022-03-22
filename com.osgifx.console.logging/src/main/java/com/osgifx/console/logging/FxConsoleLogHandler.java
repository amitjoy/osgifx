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
package com.osgifx.console.logging;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

@Component(service = Handler.class)
public final class FxConsoleLogHandler extends Handler {

	private static final String LOG_FILE_NAME = "log.txt";

	private final FileHandler        fileHandler;
	private final CustomLogFormatter formatter;

	@Activate
	public FxConsoleLogHandler(final BundleContext context) throws SecurityException, IOException, URISyntaxException {
		var area = context.getProperty("osgi.instance.area.default");

		// remove the prefix
		final var prefix = "file:";
		area = area.substring(area.indexOf(prefix) + prefix.length());

		final var path         = new URI(area).getPath();
		final var parent       = new File(path);
		final var logDirectory = new File(parent, "log");

		// create missing directories
		logDirectory.mkdirs();

		formatter   = new CustomLogFormatter();
		fileHandler = new FileHandler(logDirectory.getPath() + "/" + LOG_FILE_NAME, true);
		fileHandler.setFormatter(formatter);
	}

	@Override
	public void publish(final LogRecord logRecord) {
		fileHandler.publish(logRecord);
	}

	@Override
	public void flush() {
		fileHandler.flush();
	}

	@Override
	public void close() throws SecurityException {
		fileHandler.close();
	}

}
