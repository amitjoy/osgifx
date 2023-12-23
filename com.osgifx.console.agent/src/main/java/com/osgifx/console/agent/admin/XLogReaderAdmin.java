/*******************************************************************************
 * Copyright 2021-2024 Amit Kumar Mondal
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
package com.osgifx.console.agent.admin;

import static java.util.Objects.requireNonNull;

import org.osgi.service.log.LogReaderService;

import com.osgifx.console.agent.handler.OSGiLogListener;

public final class XLogReaderAdmin {

    public void register(final Object service, final OSGiLogListener logListener) {
        requireNonNull(service);
        requireNonNull(logListener);

        if (service instanceof LogReaderService) {
            ((LogReaderService) service).addLogListener(logListener);
        }
    }

    public void unregister(final Object service, final OSGiLogListener logListener) {
        requireNonNull(service);
        requireNonNull(logListener);

        if (service instanceof LogReaderService) {
            ((LogReaderService) service).removeLogListener(logListener);
        }
    }

}
