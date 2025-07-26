package org.osservatorionessuno.libmvt.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Indicators {
    private final Trie domainTrie;
    private final Trie urlTrie;
    private final Trie processTrie;

    private Indicators(Trie domainTrie, Trie urlTrie, Trie processTrie) {
        this.domainTrie = domainTrie;
        this.urlTrie = urlTrie;
        this.processTrie = processTrie;
    }

    public static Indicators loadFromDirectory(File dir) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Trie.TrieBuilder domains = Trie.builder().ignoreCase();
        Trie.TrieBuilder urls = Trie.builder().ignoreCase();
        Trie.TrieBuilder processes = Trie.builder().ignoreCase();

        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) return new Indicators(domains.build(), urls.build(), processes.build());

        for (File f : files) {
            JsonNode root = mapper.readTree(f);
            JsonNode arr = root.get("indicators");
            if (arr == null) continue;
            for (JsonNode coll : arr) {
                addField(domains, coll, "domain-name:value");
                addField(domains, coll, "ipv4-addr:value");
                addField(urls, coll, "url:value");
                addField(processes, coll, "process:name");
            }
        }
        return new Indicators(domains.build(), urls.build(), processes.build());
    }

    private static void addField(Trie.TrieBuilder builder, JsonNode coll, String key) {
        if (coll == null) return;
        JsonNode node = coll.get(key);
        if (node == null || node.isNull()) return;
        for (JsonNode value : iterable(node)) {
            String s = value.asText();
            if (s != null && !s.isBlank()) {
                builder.addKeyword(s.toLowerCase());
            }
        }
    }

    private static Iterable<JsonNode> iterable(JsonNode node) {
        if (node.isArray()) return node::elements;
        return Collections.singletonList(node);
    }

    public List<Detection> matchString(String s, IndicatorType type) {
        if (s == null) return List.of();
        Trie trie = switch (type) {
            case DOMAIN -> domainTrie;
            case URL -> urlTrie;
            case PROCESS -> processTrie;
        };
        List<Detection> detections = new ArrayList<>();
        for (Emit e : trie.parseText(s.toLowerCase())) {
            detections.add(new Detection(type, e.getKeyword(), s));
        }
        return detections;
    }
}
