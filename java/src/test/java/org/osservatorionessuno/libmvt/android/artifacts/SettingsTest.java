package org.osservatorionessuno.libmvt.android.artifacts;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SettingsTest {
    private String readResource(String name) throws Exception {
        Path path = Path.of("src", "test", "resources", name);
        return Files.readString(path);
    }

    @Test
    public void testParsingAndCheck() throws Exception {
        Settings s = new Settings();
        String data = readResource("androidqf/settings_random.txt");
        s.parse(data);
        assertEquals(1, s.getResults().size());
        @SuppressWarnings("unchecked")
        Map<String, String> vals = (Map<String, String>) s.getResults().get(0);
        assertEquals("0", vals.get("samsung_errorlog_agree"));
        s.checkIndicators();
        assertEquals(1, s.getDetected().size());
    }
}
