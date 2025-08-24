package org.osservatorionessuno.libmvt.android.artifacts;

import org.junit.jupiter.api.Test;
import org.osservatorionessuno.libmvt.common.Indicators;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

public class DumpsysBatteryDailyTest {
    private String readResource(String name) throws Exception {
        Path path = Paths.get("src", "test", "resources", name);
        return Files.readString(path);
    }

    @Test
    public void testParsing() throws Exception {
        DumpsysBatteryDaily bd = new DumpsysBatteryDaily();
        String data = readResource("android_data/dumpsys_battery.txt");
        bd.parse(data);
        assertEquals(3, bd.getResults().size());
    }

    @Test
    public void testIocCheck() throws Exception {
        DumpsysBatteryDaily bd = new DumpsysBatteryDaily();
        String data = readResource("android_data/dumpsys_battery.txt");
        bd.parse(data);
        Indicators ind = Indicators.loadFromDirectory(Paths.get("src", "test", "resources", "iocs").toFile());
        bd.setIndicators(ind);
        bd.checkIndicators();
        assertEquals(1, bd.getDetected().size());
    }
}
