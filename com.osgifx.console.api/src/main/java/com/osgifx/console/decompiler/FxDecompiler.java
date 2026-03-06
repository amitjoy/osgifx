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
