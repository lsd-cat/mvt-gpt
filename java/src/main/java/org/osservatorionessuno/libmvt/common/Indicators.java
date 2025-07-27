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
    private final List<String> processList;
    private final List<String> appIdList;
    private final List<String> propertyList;

    private Indicators(Trie domainTrie, Trie urlTrie, Trie processTrie, Trie appIdTrie, Trie propertyTrie,
                       List<String> processList, List<String> appIdList, List<String> propertyList) {
        this.domainTrie = domainTrie;
        this.urlTrie = urlTrie;
        this.processTrie = processTrie;
        this.appIdTrie = appIdTrie;
        this.propertyTrie = propertyTrie;
        this.processList = processList;
        this.appIdList = appIdList;
        this.propertyList = propertyList;
    }

    public static Indicators loadFromDirectory(File dir) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Trie.TrieBuilder domains = Trie.builder().ignoreCase();
        Trie.TrieBuilder urls = Trie.builder().ignoreCase();
        Trie.TrieBuilder processes = Trie.builder().ignoreCase();
        Trie.TrieBuilder appIds = Trie.builder().ignoreCase();
        Trie.TrieBuilder properties = Trie.builder().ignoreCase();
        List<String> procList = new ArrayList<>();
        List<String> appIdList = new ArrayList<>();
        List<String> propList = new ArrayList<>();

        File[] files = dir.listFiles((d, name) -> name.endsWith(".json") || name.endsWith(".stix2"));
        if (files == null) return new Indicators(domains.build(), urls.build(), processes.build(), appIds.build(), properties.build(), procList, appIdList, propList);

        for (File f : files) {
            if (f.getName().endsWith(".stix2")) {
                String json = java.nio.file.Files.readString(f.toPath());
                try {
                    BundleObject bundle = StixParsers.parseBundle(json);
                    for (BundleableObject obj : bundle.getObjects()) {
                        if (obj instanceof IndicatorSdo ind) {
                            addPattern(domains, urls, processes, appIds, properties,
                                       procList, appIdList, propList, ind.getPattern());
                        }
                    }
                } catch (Exception ex) {
                    // Fallback to simple parsing if library fails
                    JsonNode root = mapper.readTree(json);
                    JsonNode objects = root.get("objects");
                    if (objects != null && objects.isArray()) {
                        for (JsonNode node : objects) {
                            if ("indicator".equals(node.path("type").asText())) {
                                addPattern(domains, urls, processes, appIds, properties,
                                           procList, appIdList, propList,
                                           node.path("pattern").asText());
                            }
                        }
                    }
                }
            } else {
                JsonNode root = mapper.readTree(f);
                JsonNode arr = root.get("indicators");
                if (arr == null) continue;
                for (JsonNode coll : arr) {
                    addField(domains, coll, "domain-name:value", null);
                    addField(domains, coll, "ipv4-addr:value", null);
                    addField(urls, coll, "url:value", null);
                    addField(processes, coll, "process:name", procList);
                    addField(appIds, coll, "app:id", appIdList);
                    addField(properties, coll, "android-property:name", propList);
                }
            }
        }
        return new Indicators(domains.build(), urls.build(), processes.build(), appIds.build(), properties.build(),
                procList, appIdList, propList);
    }

    private static void addPattern(Trie.TrieBuilder domains, Trie.TrieBuilder urls,
                                   Trie.TrieBuilder processes, Trie.TrieBuilder appIds,
                                   Trie.TrieBuilder properties,
                                   List<String> procList, List<String> appIdList, List<String> propList,
                                   String pattern) {
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
            case "process:name" -> { processes.addKeyword(value.toLowerCase()); procList.add(value.toLowerCase()); }
            case "app:id" -> { appIds.addKeyword(value.toLowerCase()); appIdList.add(value.toLowerCase()); }
            case "android-property:name" -> { properties.addKeyword(value.toLowerCase()); propList.add(value.toLowerCase()); }
            default -> {
            }
        }
    }

    private static void addField(Trie.TrieBuilder builder, JsonNode coll, String key, List<String> store) {
        if (coll == null) return;
        JsonNode node = coll.get(key);
        if (node == null || node.isNull()) return;
        for (JsonNode value : iterable(node)) {
            String s = value.asText();
            if (s != null && !s.isBlank()) {
                builder.addKeyword(s.toLowerCase());
                if (store != null) store.add(s.toLowerCase());
            }
        }
    }

    private static Iterable<JsonNode> iterable(JsonNode node) {
        if (node.isArray()) return node::elements;
        return Collections.singletonList(node);
    }

    public List<Detection> matchString(String s, IndicatorType type) {
        if (s == null) return List.of();
        String lower = s.toLowerCase();
        List<Detection> detections = new ArrayList<>();
        switch (type) {
            case DOMAIN -> {
                for (Emit e : domainTrie.parseText(lower)) {
                    detections.add(new Detection(type, e.getKeyword(), s));
                }
            }
            case URL -> {
                for (Emit e : urlTrie.parseText(lower)) {
                    detections.add(new Detection(type, e.getKeyword(), s));
                }
            }
            case PROCESS -> {
                for (String kw : processList) {
                    if (kw.equals(lower) || (lower.length() == 16 && kw.startsWith(lower))) {
                        detections.add(new Detection(type, kw, s));
                    }
                }
            }
            case APP_ID -> {
                for (String kw : appIdList) {
                    if (kw.equals(lower)) {
                        detections.add(new Detection(type, kw, s));
                    }
                }
            }
            case PROPERTY -> {
                for (String kw : propertyList) {
                    if (kw.equals(lower)) {
                        detections.add(new Detection(type, kw, s));
                    }
                }
            }
        }
        return detections;
    }
}
