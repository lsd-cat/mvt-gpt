package org.osservatorionessuno.libmvt.android.artifacts;

import org.junit.jupiter.api.Test;
import org.osservatorionessuno.libmvt.common.Indicators;
import org.osservatorionessuno.libmvt.common.IndicatorType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class GetPropTest {
    private String readResource(String name) throws Exception {
        Path path = Path.of("src", "test", "resources", name);
        return Files.readString(path);
    }

    @Test
    public void testParsing() throws Exception {
        GetProp gp = new GetProp();
        String data = readResource("android_data/getprop.txt");
        gp.parse(data);
        assertEquals(13, gp.getResults().size());
        @SuppressWarnings("unchecked")
        Map<String, String> first = (Map<String, String>) gp.getResults().get(0);
        assertEquals("af.fast_track_multiplier", first.get("name"));
        assertEquals("1", first.get("value"));
    }

    @Test
    public void testIocCheck() throws Exception {
        GetProp gp = new GetProp();
        String data = readResource("android_data/getprop.txt");
        gp.parse(data);
        Indicators indicators = Indicators.loadFromDirectory(Path.of("src", "test", "resources", "iocs").toFile());
        gp.setIndicators(indicators);
        gp.checkIndicators();
        // For this test dataset there should be zero detections
        assertEquals(0, gp.getDetected().size());
    }
}
