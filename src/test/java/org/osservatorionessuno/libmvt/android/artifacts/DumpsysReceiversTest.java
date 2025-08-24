package org.osservatorionessuno.libmvt.android.artifacts;

import org.junit.jupiter.api.Test;
import org.osservatorionessuno.libmvt.common.Indicators;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DumpsysReceiversTest {
    private String readResource(String name) throws Exception {
        Path path = Paths.get("src", "test", "resources", name);
        return Files.readString(path);
    }

    @Test
    public void testParsing() throws Exception {
        DumpsysReceivers dr = new DumpsysReceivers();
        String data = readResource("android_data/dumpsys_packages.txt");
        dr.parse(data);
        assertEquals(4, dr.getResults().size());
        @SuppressWarnings("unchecked")
        Map<String, String> first = (Map<String, String>) dr.getResults().get(0);
        assertEquals("com.android.storagemanager.automatic.SHOW_NOTIFICATION", first.get("intent"));
        assertEquals("com.android.storagemanager", first.get("package_name"));
    }

    @Test
    public void testIocCheck() throws Exception {
        DumpsysReceivers dr = new DumpsysReceivers();
        String data = readResource("android_data/dumpsys_packages.txt");
        dr.parse(data);
        Indicators ind = Indicators.loadFromDirectory(Paths.get("src", "test", "resources", "iocs").toFile());
        dr.setIndicators(ind);
        dr.checkIndicators();
        assertEquals(0, dr.getDetected().size());
    }
}
