package org.osservatorionessuno.libmvt.android.artifacts;

import org.junit.jupiter.api.Test;
import org.osservatorionessuno.libmvt.common.Indicators;
import org.osservatorionessuno.libmvt.common.IndicatorType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DumpsysAccessibilityTest {

    private String readResource(String name) throws Exception {
        Path path = Path.of("src", "test", "resources", name);
        return Files.readString(path);
    }

    @Test
    public void testParsing() throws Exception {
        DumpsysAccessibility da = new DumpsysAccessibility();
        String data = readResource("android_data/dumpsys_accessibility.txt");
        da.parse(data);
        assertEquals(4, da.getResults().size());
        @SuppressWarnings("unchecked")
        Map<String, String> first = (Map<String, String>) da.getResults().get(0);
        assertEquals("com.android.settings", first.get("package_name"));
        assertEquals("com.android.settings/com.samsung.android.settings.development.gpuwatch.GPUWatchInterceptor", first.get("service"));
    }

    @Test
    public void testParsingV14Format() throws Exception {
        DumpsysAccessibility da = new DumpsysAccessibility();
        String data = readResource("android_data/dumpsys_accessibility_v14_or_later.txt");
        da.parse(data);
        assertEquals(1, da.getResults().size());
        @SuppressWarnings("unchecked")
        Map<String, String> first = (Map<String, String>) da.getResults().get(0);
        assertEquals("com.malware.accessibility", first.get("package_name"));
        assertEquals("com.malware.service.malwareservice", first.get("service"));
    }

    @Test
    public void testIocCheck() throws Exception {
        DumpsysAccessibility da = new DumpsysAccessibility();
        String data = readResource("android_data/dumpsys_accessibility.txt");
        da.parse(data);
        Indicators indicators = Indicators.loadFromDirectory(Path.of("src", "test", "resources", "iocs").toFile());
        da.setIndicators(indicators);
        da.checkIndicators();
        assertEquals(1, da.getDetected().size());
        assertEquals(IndicatorType.PROCESS, da.getDetected().get(0).type());
    }
}
