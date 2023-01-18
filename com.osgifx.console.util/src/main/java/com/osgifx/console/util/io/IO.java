/*******************************************************************************
 * Copyright 2021-2023 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.util.io;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class IO {

    private static final String            FILE_NAME_PREFIX            = "OSGi.fx";
    private static final String            FILE_NAME_SEPARATOR         = "_";
    private static final DateTimeFormatter FILE_NAME_DATE_TIME_PATTERN = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    private IO() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static String prepareFilenameFor(final String extension) {
        return FILE_NAME_PREFIX + FILE_NAME_SEPARATOR + LocalDateTime.now().format(FILE_NAME_DATE_TIME_PATTERN) + "."
                + extension;
    }

}
