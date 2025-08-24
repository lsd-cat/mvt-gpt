package org.osservatorionessuno.libmvt.android;

import org.junit.jupiter.api.Test;
import org.osservatorionessuno.libmvt.common.Artifact;
import org.osservatorionessuno.libmvt.common.Indicators;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AndroidQFRunnerTest {
    @Test
    public void testRunAllModules() throws Exception {
        Path dir = Paths.get("src", "test", "resources", "androidqf");
        Indicators ind = Indicators.loadFromDirectory(Paths.get("src","test","resources","iocs").toFile());
        AndroidQFRunner runner = new AndroidQFRunner(dir);
        runner.setIndicators(ind);
        Map<String, Artifact> res = runner.runAll();
        assertTrue(res.containsKey("processes"));
        Artifact proc = res.get("processes");
        assertEquals(15, proc.getResults().size());
        assertTrue(res.containsKey("getprop"));
        assertEquals(10, res.get("getprop").getResults().size());
    }

    @Test
    public void testRunSingleModule() throws Exception {
        Path dir = Paths.get("src", "test", "resources", "androidqf");
        AndroidQFRunner runner = new AndroidQFRunner(dir);
        Artifact art = runner.runModule("getprop");
        assertEquals(10, art.getResults().size());
    }
}
