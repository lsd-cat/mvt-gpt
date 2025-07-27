package org.osservatorionessuno.libmvt.android.artifacts;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TombstoneCrashesTest {
    private byte[] readBytes(String name) throws Exception {
        Path path = Path.of("src", "test", "resources", name);
        return Files.readAllBytes(path);
    }

    @Test
    public void testParsing() throws Exception {
        TombstoneCrashes tc = new TombstoneCrashes();
        String data = Files.readString(Path.of("src", "test", "resources", "android_data/tombstone_process.txt"));
        tc.parse(data);
        assertEquals(1, tc.getResults().size());
        @SuppressWarnings("unchecked")
        Map<String, Object> rec = (Map<String, Object>) tc.getResults().get(0);
        assertEquals("mtk.ape.decoder", rec.get("process_name"));
        assertEquals(25541, rec.get("pid"));
        assertEquals(1046, rec.get("uid"));
        assertEquals("/vendor/bin/hw/android.hardware.media.c2@1.2-mediatek", ((java.util.List<?>)rec.get("command_line")).get(0));
        assertEquals("2023-04-12 12:32:40.518290", rec.get("timestamp"));
    }

    @Test
    public void testParseProtobuf() throws Exception {
        TombstoneCrashes tc = new TombstoneCrashes();
        byte[] data = readBytes("android_data/tombstone_process.pb");
        tc.parseProtobuf(data);
        assertEquals(1, tc.getResults().size());
        @SuppressWarnings("unchecked")
        Map<String, Object> rec = (Map<String, Object>) tc.getResults().get(0);
        assertEquals("mtk.ape.decoder", rec.get("process_name"));
        assertEquals(25541, rec.get("pid"));
        assertEquals(1046, rec.get("uid"));
        assertEquals("/vendor/bin/hw/android.hardware.media.c2@1.2-mediatek",
                ((java.util.List<?>) rec.get("command_line")).get(0));
        assertEquals("2023-04-12 12:32:40.518290", rec.get("timestamp"));
    }

    @Test
    @Disabled("Not implemented yet")
    public void testParseKernel() throws Exception {
        // Skipped: kernel tombstone parsing is not yet implemented
    }
}
