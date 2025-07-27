package org.osservatorionessuno.libmvt.android.artifacts;

import org.junit.jupiter.api.Test;
import org.osservatorionessuno.libmvt.common.Indicators;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DumpsysAppopsTest {
    private String readResource(String name) throws Exception {
        Path path = Path.of("src", "test", "resources", name);
        return Files.readString(path);
    }

    @Test
    public void testParsing() throws Exception {
        DumpsysAppops da = new DumpsysAppops();
        String data = readResource("android_data/dumpsys_appops.txt");
        da.parse(data);
        assertEquals(13, da.getResults().size());
        @SuppressWarnings("unchecked")
        Map<String, Object> first = (Map<String, Object>) da.getResults().get(0);
        assertEquals("com.android.phone", first.get("package_name"));
        assertEquals("0", first.get("uid"));
        @SuppressWarnings("unchecked")
        List<?> perms = (List<?>) first.get("permissions");
        assertEquals(1, perms.size());
        @SuppressWarnings("unchecked")
        Map<String, Object> seventh = (Map<String, Object>) da.getResults().get(6);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> plist = (List<Map<String, Object>>) seventh.get("permissions");
        @SuppressWarnings("unchecked")
        Map<String, Object> secondPerm = plist.get(1);
        @SuppressWarnings("unchecked")
        List<?> entries = (List<?>) secondPerm.get("entries");
        assertEquals(1, entries.size());
        @SuppressWarnings("unchecked")
        Map<String, Object> eleventh = (Map<String, Object>) da.getResults().get(11);
        assertEquals(4, ((List<?>) eleventh.get("permissions")).size());
    }

    @Test
    public void testIocCheck() throws Exception {
        DumpsysAppops da = new DumpsysAppops();
        String data = readResource("android_data/dumpsys_appops.txt");
        da.parse(data);
        Indicators indicators = Indicators.loadFromDirectory(Path.of("src", "test", "resources", "iocs").toFile());
        da.setIndicators(indicators);
        da.checkIndicators();
        assertTrue(da.getDetected().size() > 0);
    }
}
