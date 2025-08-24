package org.osservatorionessuno.libmvt.android.artifacts;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

public class AndroidArtifactTest {
    private String readResource(String name) throws Exception {
        Path path = Paths.get("src", "test", "resources", name);
        return Files.readString(path);
    }

    @Test
    public void testExtractDumpsysSection() throws Exception {
        String dumpsys = readResource("androidqf/dumpsys.txt");
        String section = AndroidArtifact.extractDumpsysSection(dumpsys, "DUMP OF SERVICE package:");
        assertNotNull(section);
        assertEquals(3907, section.length());
    }
}
