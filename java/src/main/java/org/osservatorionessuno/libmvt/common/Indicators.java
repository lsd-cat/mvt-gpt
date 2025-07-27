package org.osservatorionessuno.libmvt.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.digitalstate.stix.bundle.BundleObject;
import io.digitalstate.stix.bundle.BundleableObject;
import io.digitalstate.stix.json.StixParsers;
import io.digitalstate.stix.sdo.objects.IndicatorSdo;
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
    private final Trie appIdTrie;
    private final Trie propertyTrie;

    private Indicators(Trie domainTrie, Trie urlTrie, Trie processTrie, Trie appIdTrie, Trie propertyTrie) {
        this.domainTrie = domainTrie;
        this.urlTrie = urlTrie;
        this.processTrie = processTrie;
        this.appIdTrie = appIdTrie;
        this.propertyTrie = propertyTrie;
    }

    public static Indicators loadFromDirectory(File dir) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Trie.TrieBuilder domains = Trie.builder().ignoreCase();
        Trie.TrieBuilder urls = Trie.builder().ignoreCase();
        Trie.TrieBuilder processes = Trie.builder().ignoreCase();
        Trie.TrieBuilder appIds = Trie.builder().ignoreCase();
        Trie.TrieBuilder properties = Trie.builder().ignoreCase();

        File[] files = dir.listFiles((d, name) -> name.endsWith(".json") || name.endsWith(".stix2"));
        if (files == null) return new Indicators(domains.build(), urls.build(), processes.build(), appIds.build(), properties.build());

        for (File f : files) {
            if (f.getName().endsWith(".stix2")) {
                String json = java.nio.file.Files.readString(f.toPath());
                try {
                    BundleObject bundle = StixParsers.parseBundle(json);
                    for (BundleableObject obj : bundle.getObjects()) {
                        if (obj instanceof IndicatorSdo ind) {
                            addPattern(domains, urls, processes, appIds, properties, ind.getPattern());
                        }
                    }
                } catch (Exception ex) {
                    // Fallback to simple parsing if library fails
                    JsonNode root = mapper.readTree(json);
                    JsonNode objects = root.get("objects");
                    if (objects != null && objects.isArray()) {
                        for (JsonNode node : objects) {
                            if ("indicator".equals(node.path("type").asText())) {
                                addPattern(domains, urls, processes, appIds, properties, node.path("pattern").asText());
                            }
                        }
                    }
                }
            } else {
                JsonNode root = mapper.readTree(f);
                JsonNode arr = root.get("indicators");
                if (arr == null) continue;
                for (JsonNode coll : arr) {
                    addField(domains, coll, "domain-name:value");
                    addField(domains, coll, "ipv4-addr:value");
                    addField(urls, coll, "url:value");
                    addField(processes, coll, "process:name");
                    addField(appIds, coll, "app:id");
                    addField(properties, coll, "android-property:name");
                }
            }
        }
        return new Indicators(domains.build(), urls.build(), processes.build(), appIds.build(), properties.build());
    }

    private static void addPattern(Trie.TrieBuilder domains, Trie.TrieBuilder urls,
                                   Trie.TrieBuilder processes, Trie.TrieBuilder appIds,
                                   Trie.TrieBuilder properties, String pattern) {
        if (pattern == null) return;
        String p = pattern.trim();
        if (p.startsWith("[") && p.endsWith("]")) {
            p = p.substring(1, p.length() - 1);
        }
        String[] kv = p.split("=", 2);
        if (kv.length != 2) return;
        String key = kv[0].trim();
        String value = kv[1].trim();
        if (value.startsWith("'") && value.endsWith("'")) {
            value = value.substring(1, value.length() - 1);
        }
        switch (key) {
            case "domain-name:value", "ipv4-addr:value" -> domains.addKeyword(value.toLowerCase());
            case "url:value" -> urls.addKeyword(value.toLowerCase());
            case "process:name" -> processes.addKeyword(value.toLowerCase());
            case "app:id" -> appIds.addKeyword(value.toLowerCase());
            case "android-property:name" -> properties.addKeyword(value.toLowerCase());
            default -> {
            }
        }
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
            case APP_ID -> appIdTrie;
            case PROPERTY -> propertyTrie;
        };
        List<Detection> detections = new ArrayList<>();
        for (Emit e : trie.parseText(s.toLowerCase())) {
            detections.add(new Detection(type, e.getKeyword(), s));
        }
        return detections;
    }
}
