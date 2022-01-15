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
package com.osgifx.console.agent.provider;

import java.io.InputStream;

/**
 * An object that can retrieve an inputstream on a given SHA-1
 */
public interface ShaSource {
    /**
     * Retrieving the stream is fast so do not cache
     *
     * @return true if a fast retrieval can be done
     */
    boolean isFast();

    /**
     * Get an inputstream based on the given SHA-1
     *
     * @param sha the SHA-1
     * @return a stream or null if not found
     */
    InputStream get(String sha) throws Exception;
}
