package org.osservatorionessuno.libmvt.android.artifacts;

import org.junit.jupiter.api.Test;
import org.osservatorionessuno.libmvt.common.Indicators;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DumpsysPlatformCompatTest {
    private String readResource(String name) throws Exception {
        Path path = Path.of("src", "test", "resources", name);
        return Files.readString(path);
    }

    @Test
    public void testParsing() throws Exception {
        DumpsysPlatformCompat pc = new DumpsysPlatformCompat();
        String data = readResource("android_data/dumpsys_platform_compat.txt");
        pc.parse(data);
        assertEquals(2, pc.getResults().size());
        @SuppressWarnings("unchecked")
        Map<String, String> first = (Map<String, String>) pc.getResults().get(0);
        assertEquals("org.torproject.torbrowser", first.get("package_name"));
    }

    @Test
    public void testIocCheck() throws Exception {
        DumpsysPlatformCompat pc = new DumpsysPlatformCompat();
        String data = readResource("android_data/dumpsys_platform_compat.txt");
        pc.parse(data);
        Indicators ind = Indicators.loadFromDirectory(Path.of("src", "test", "resources", "iocs").toFile());
        pc.setIndicators(ind);
        pc.checkIndicators();
        assertEquals(0, pc.getDetected().size());
    }
}
