/*******************************************************************************
 * Copyright 2021-2026 Amit Kumar Mondal
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
package com.osgifx.console.decompiler;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface FxDecompiler {
    /**
     * Decompiles the raw bytecode of a Java class into Java source code.
     *
     * @param classBytes the raw bytecode of the .class file
     * @param className the fully qualified class name (e.g., "com.example.MyClass")
     * @return the decompiled Java source code string
     * @throws Exception if the decompilation process fails
     */
    String decompile(byte[] classBytes, String className) throws Exception;
}
