package org.osservatorionessuno.libmvt.android.artifacts;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class FileTimestampsTest {
    private String readResource(String name) throws Exception {
        Path path = Path.of("src", "test", "resources", name);
        return Files.readString(path);
    }

    @Test
    public void testSerialize() throws Exception {
        String json = readResource("androidqf/files.json");
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> records = mapper.readValue(json, List.class);
        Map<String, Object> first = records.get(0);
        FileTimestamps ft = new FileTimestamps();
        List<Map<String, Object>> events = ft.serialize(first);
        assertEquals(2, events.size());
        assertEquals("MA--", events.get(0).get("event"));
    }
}
