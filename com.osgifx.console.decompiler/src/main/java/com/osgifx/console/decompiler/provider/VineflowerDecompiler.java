package com.osgifx.console.decompiler.provider;

import static org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences.INDENT_STRING;
import static org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences.REMOVE_SYNTHETIC;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.jar.Manifest;

import org.jetbrains.java.decompiler.api.Decompiler;
import org.jetbrains.java.decompiler.main.extern.IContextSource;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;
import org.osgi.service.component.annotations.Component;

import com.osgifx.console.decompiler.FxDecompiler;

@Component
public final class VineflowerDecompiler implements FxDecompiler {

    @Override
    public String decompile(final byte[] classBytes, final String className) throws Exception {

        if (classBytes == null || classBytes.length == 0) {
            throw new IllegalArgumentException("Class bytes cannot be null or empty");
        }
        final var source = new InMemorySource(classBytes, className);
        final var saver  = new StringSaver();

        // @formatter:off
        final var decompiler = new Decompiler.Builder()
                                             .inputs(source)
                                             .output(saver)
                                             .option(INDENT_STRING, "    ")
                                             .option(REMOVE_SYNTHETIC, "1")
                                             .logger(new NoOpLogger())
                                             .build();
        // @formatter:on

        decompiler.decompile();
        return saver.getDecompiledSource();
    }

    // --- In-Memory Adapters ---
    private static class InMemorySource implements IContextSource {
        private final byte[] classBytes;
        private final String className;
        private final String classPath;

        public InMemorySource(final byte[] bytes, final String className) {
            this.classBytes = bytes;
            this.className  = className;
            this.classPath  = className.replace('.', '/') + ".class";
        }

        @Override
        public String getName() {
            return "InMemorySource";
        }

        @Override
        public Entries getEntries() {
            return new Entries(List.of(Entry.atBase(className.replace('.', '/'))), List.of(), List.of());
        }

        @Override
        public InputStream getInputStream(final Entry entry) throws IOException {
            return getInputStream(entry.path());
        }

        @Override
        public InputStream getInputStream(final String resource) throws IOException {
            if (resource.equals(classPath) || resource.equals(className.replace('.', '/') + ".class")) {
                return new ByteArrayInputStream(classBytes);
            }
            return null;
        }

        @Override
        public IOutputSink createOutputSink(final IResultSaver saver) {
            return new IOutputSink() {
                @Override
                public void begin() {
                }

                @Override
                public void acceptClass(final String qName,
                                        final String fileName,
                                        final String content,
                                        final int[] mapping) {
                    saver.saveClassFile("", qName, fileName, content, mapping);
                }

                @Override
                public void acceptDirectory(final String directory) {
                    saver.saveFolder(directory);
                }

                @Override
                public void acceptOther(final String path) {
                }

                @Override
                public void close() throws IOException {
                }
            };
        }
    }

    private static class StringSaver implements IResultSaver {
        private String source = "";

        @Override
        public void saveClassFile(final String path,
                                  final String qName,
                                  final String entry,
                                  final String content,
                                  final int[] map) {
            this.source = content;
        }

        public String getDecompiledSource() {
            return source;
        }

        // ... No-ops for other IResultSaver methods (saveFolder, copyFile, etc.) ...
        @Override
        public void saveFolder(final String path) {
        }

        @Override
        public void copyFile(final String src, final String path, final String entry) {
        }

        @Override
        public void createArchive(final String path, final String archiveName, final Manifest manifest) {
        }

        @Override
        public void saveDirEntry(final String path, final String archiveName, final String entryName) {
        }

        @Override
        public void copyEntry(final String src, final String path, final String archiveName, final String entry) {
        }

        @Override
        public void saveClassEntry(final String path,
                                   final String archiveName,
                                   final String qName,
                                   final String entry,
                                   final String content) {
            this.source = content;
        }

        @Override
        public void closeArchive(final String path, final String archiveName) {
        }
    }

    private static class NoOpLogger extends IFernflowerLogger {

        @Override
        public void writeMessage(final String message, final Severity severity) {
        }

        @Override
        public void writeMessage(final String message, final Severity severity, final Throwable t) {
        }
    }
}
