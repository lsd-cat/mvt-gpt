package org.osservatorionessuno.libmvt.android.artifacts;

import org.junit.jupiter.api.Test;
import org.osservatorionessuno.libmvt.common.Indicators;
import org.osservatorionessuno.libmvt.common.IndicatorType;
import org.osservatorionessuno.libmvt.common.Detection;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DumpsysPackagesTest {
    private String readResource(String name) throws Exception {
        Path path = Path.of("src", "test", "resources", name);
        return Files.readString(path);
    }

    @Test
    public void testParsing() throws Exception {
        DumpsysPackages dpa = new DumpsysPackages();
        String data = readResource("android_data/dumpsys_packages.txt");
        dpa.parse(data);
        assertEquals(2, dpa.getResults().size());
        @SuppressWarnings("unchecked")
        Map<String, Object> first = (Map<String, Object>) dpa.getResults().get(0);
        assertEquals("com.samsung.android.provider.filterprovider", first.get("package_name"));
        assertEquals("5.0.07", first.get("version_name"));
    }

    @Test
    public void testIocCheck() throws Exception {
        DumpsysPackages dpa = new DumpsysPackages();
        String data = readResource("android_data/dumpsys_packages.txt");
        dpa.parse(data);
        Indicators indicators = Indicators.loadFromDirectory(Path.of("src", "test", "resources", "iocs").toFile());
        dpa.setIndicators(indicators);
        dpa.checkIndicators();
        assertTrue(dpa.getDetected().size() > 0);
    }

    @Test
    public void testRootPackageDetection() {
        String sample = String.join("\n",
                "Packages:",
                "  Package [com.topjohnwu.magisk] (abcd):",
                "    userId=0",
                "");
        DumpsysPackages dpa = new DumpsysPackages();
        dpa.parse(sample);
        dpa.checkIndicators();
        assertEquals(1, dpa.getDetected().size());
        Detection detected = dpa.getDetected().get(0);
        assertEquals(IndicatorType.PROCESS, detected.type());
        assertEquals("com.topjohnwu.magisk", detected.ioc());
    }
}
