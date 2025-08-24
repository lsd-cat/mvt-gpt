package org.osservatorionessuno.libmvt.common;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

public class IndicatorsUpdatesTest {
    @Test
    public void testUpdateLocal() throws Exception {
        Path temp = Files.createTempDirectory("mvt");
        Path stix = Paths.get("src","test","resources","stix2","cytrox.stix2");
        String index = "indicators:\n" +
                "  - name: local\n" +
                "    type: download\n" +
                "    download_url: " + stix.toUri().toString() + "\n";
        Path indexFile = Files.createTempFile(temp, "index", ".yaml");
        Files.writeString(indexFile, index);

        IndicatorsUpdates updates = new IndicatorsUpdates(temp, indexFile.toUri().toString());
        updates.update();

        Path indicatorsDir = temp.resolve("indicators");
        String fileName = stix.toUri().toString().replaceFirst("^https?://", "").replaceAll("[\\/]", "_");
        assertTrue(Files.exists(indicatorsDir.resolve(fileName)));

        Indicators indicators = Indicators.loadFromDirectory(indicatorsDir.toFile());
        assertFalse(indicators.matchString("shortenurls.me", IndicatorType.DOMAIN).isEmpty());
    }
}
