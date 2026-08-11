package com.osgifx.console.bnd.dp.packager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import aQute.bnd.build.Project;

@ExtendWith(MockitoExtension.class)
public class DeploymentPackageExporterTest {

    @TempDir
    File tempDir;

    @Mock
    Project project;

    private DeploymentPackageExporter exporter;
    private File                      targetDir;
    private File                      baseDir;

    @BeforeEach
    public void setUp() {
        exporter = new DeploymentPackageExporter();
        targetDir = new File(tempDir, "target");
        baseDir = new File(tempDir, "base");
        
        targetDir.mkdirs();
        baseDir.mkdirs();
    }

    private Map<String, String> defaultOptions() {
        Map<String, String> options = new HashMap<>();
        options.put("symbolicName", "com.test.dp");
        options.put("version", "1.0.0");
        options.put("output", "test.dp");
        return options;
    }

    @Test
    public void exportCreatesValidJarFile() throws Exception {
        when(project.getTargetDir()).thenReturn(targetDir);
        when(project.getBase()).thenReturn(baseDir);
        when(project.getRunbundles()).thenReturn(Collections.emptyList());
        when(project.isOk()).thenReturn(true);

        exporter.export("osgi.dp.exporter", project, defaultOptions());

        File outputJar = new File(targetDir, "test.dp");
        assertTrue(outputJar.exists(), "Output JAR should be created");

        try (JarFile jar = new JarFile(outputJar)) {
            assertNotNull(jar.getManifest(), "Manifest should be present");
        }
    }

    @Test
    public void manifestContainsSymbolicName() throws Exception {
        when(project.getTargetDir()).thenReturn(targetDir);
        when(project.getBase()).thenReturn(baseDir);
        when(project.getRunbundles()).thenReturn(Collections.emptyList());
        when(project.isOk()).thenReturn(true);

        exporter.export("osgi.dp.exporter", project, defaultOptions());

        File outputJar = new File(targetDir, "test.dp");
        try (JarFile jar = new JarFile(outputJar)) {
            Attributes attrs = jar.getManifest().getMainAttributes();
            assertEquals("com.test.dp", attrs.getValue("DeploymentPackage-SymbolicName"));
        }
    }

    @Test
    public void manifestContainsVersion() throws Exception {
        when(project.getTargetDir()).thenReturn(targetDir);
        when(project.getBase()).thenReturn(baseDir);
        when(project.getRunbundles()).thenReturn(Collections.emptyList());
        when(project.isOk()).thenReturn(true);

        exporter.export("osgi.dp.exporter", project, defaultOptions());

        File outputJar = new File(targetDir, "test.dp");
        try (JarFile jar = new JarFile(outputJar)) {
            Attributes attrs = jar.getManifest().getMainAttributes();
            assertEquals("1.0.0", attrs.getValue("DeploymentPackage-Version"));
        }
    }

    @Test
    public void exportWithNoBundlesProducesEmptyJar() throws Exception {
        when(project.getTargetDir()).thenReturn(targetDir);
        when(project.getBase()).thenReturn(baseDir);
        when(project.getRunbundles()).thenReturn(Collections.emptyList());
        when(project.isOk()).thenReturn(true);

        exporter.export("osgi.dp.exporter", project, defaultOptions());

        File outputJar = new File(targetDir, "test.dp");
        try (JarFile jar = new JarFile(outputJar)) {
            // META-INF/ and META-INF/MANIFEST.MF are the only entries
            assertTrue(jar.size() <= 2, "Jar should contain only manifest entries");
        }
    }

    @Test
    public void exportWithMissingBundleFileThrows() throws Exception {
        when(project.getRunbundles()).thenReturn(Collections.emptyList());
        
        // Return null for missing resource
        when(project.getFile("missing.txt")).thenReturn(null);

        Map<String, String> options = defaultOptions();
        options.put("resources", "missing.txt");

        assertThrows(FileNotFoundException.class, () -> {
            exporter.export("osgi.dp.exporter", project, options);
        });
    }
}
