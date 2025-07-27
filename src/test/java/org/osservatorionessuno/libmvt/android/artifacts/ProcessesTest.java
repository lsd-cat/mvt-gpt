package org.osservatorionessuno.libmvt.android.artifacts;

import org.junit.jupiter.api.Test;
import org.osservatorionessuno.libmvt.common.Indicators;
import org.osservatorionessuno.libmvt.common.IndicatorType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ProcessesTest {
    private String readResource(String name) throws Exception {
        Path path = Path.of("src", "test", "resources", name);
        return Files.readString(path);
    }

    @Test
    public void testParsing() throws Exception {
        Processes p = new Processes();
        String data = readResource("android_data/ps.txt");
        p.parse(data);
        assertEquals(17, p.getResults().size());
        @SuppressWarnings("unchecked")
        Map<String, Object> first = (Map<String, Object>) p.getResults().get(0);
        assertEquals("init", first.get("proc_name"));
    }

    @Test
    public void testIocCheck() throws Exception {
        Processes p = new Processes();
        String data = readResource("android_data/ps.txt");
        p.parse(data);
        Indicators indicators = Indicators.loadFromDirectory(Path.of("src", "test", "resources", "iocs").toFile());
        p.setIndicators(indicators);
        p.checkIndicators();
        assertTrue(p.getDetected().size() > 0);
    }

    @Test
    public void testTruncatedProcessMatch() throws Exception {
        Processes p = new Processes();
        String data = "USER PID PPID VSZ RSS WCHAN ADDR S NAME\n" +
                "root 50 2 0 0 0 0 S com.bad.actor.ma\n";
        p.parse(data);
        Indicators indicators = Indicators.loadFromDirectory(Path.of("src", "test", "resources", "iocs").toFile());
        p.setIndicators(indicators);
        p.checkIndicators();
        assertFalse(p.getDetected().isEmpty());
    }
}
