package org.osservatorionessuno.libmvt.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public class IndicatorsUpdates {
    private static final String DEFAULT_GITHUB_RAW = "https://raw.githubusercontent.com/%s/%s/%s/%s";
    public static final Path MVT_DATA_FOLDER = Paths.get(System.getProperty("user.home"), ".mvt");

    private final HttpClient client = HttpClient.newHttpClient();
    private final Path latestUpdatePath;
    private final Path latestCheckPath;
    private final Path indicatorsFolder;

    private final String indexUrl;
    private final String githubRawUrl;

    public IndicatorsUpdates() {
        this(null, null);
    }

    public IndicatorsUpdates(Path dataFolder, String indexUrl) {
        Path base = dataFolder == null ? MVT_DATA_FOLDER : dataFolder;
        this.indexUrl = indexUrl;
        this.githubRawUrl = DEFAULT_GITHUB_RAW;
        this.latestUpdatePath = base.resolve("latest_indicators_update");
        this.latestCheckPath = base.resolve("latest_indicators_check");
        this.indicatorsFolder = base.resolve("indicators");
        if (!Files.exists(base)) {
            try {
                Files.createDirectories(base);
            } catch (IOException ignored) {
            }
        }
        if (!Files.exists(indicatorsFolder)) {
            try {
                Files.createDirectories(indicatorsFolder);
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Return the folder where indicators are stored.
     */
    public Path getIndicatorsFolder() {
        return indicatorsFolder;
    }

    private Map<String, Object> getRemoteIndex() throws IOException, InterruptedException {
        String url = indexUrl != null ? indexUrl : String.format(githubRawUrl, "mvt-project", "mvt-indicators", "main", "indicators.yaml");
        if (url.startsWith("file://")) {
            Path p = Paths.get(URI.create(url));
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            @SuppressWarnings("unchecked")
            Map<String,Object> res = (Map<String,Object>) mapper.readValue(p.toFile(), Map.class);
            return res;
        }
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) return null;
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        @SuppressWarnings("unchecked")
        Map<String,Object> map = (Map<String,Object>) mapper.readValue(res.body(), Map.class);
        return map;
    }

    private String downloadRemoteIoc(String url) throws IOException, InterruptedException {
        Path indicatorsDir = indicatorsFolder;
        if (!Files.exists(indicatorsDir)) Files.createDirectories(indicatorsDir);
        String fileName = url.replaceFirst("^https?://", "").replaceAll("[\\/]", "_");
        Path dest = indicatorsDir.resolve(fileName);
        if (url.startsWith("file://")) {
            Path p = Paths.get(URI.create(url));
            Files.copy(p, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return dest.toString();
        }
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .build();
        HttpResponse<Path> res = client.send(req, HttpResponse.BodyHandlers.ofFile(dest));
        if (res.statusCode() != 200) return null;
        return dest.toString();
    }

    private long getLatestCheck() {
        try {
            return Long.parseLong(Files.readString(latestCheckPath));
        } catch (IOException | NumberFormatException e) {
            return 0;
        }
    }

    private void setLatestCheck() {
        try { Files.writeString(latestCheckPath, Long.toString(Instant.now().getEpochSecond())); } catch (IOException ignored) {}
    }

    private long getLatestUpdate() {
        try {
            return Long.parseLong(Files.readString(latestUpdatePath));
        } catch (IOException | NumberFormatException e) {
            return 0;
        }
    }

    private void setLatestUpdate() {
        try { Files.writeString(latestUpdatePath, Long.toString(Instant.now().getEpochSecond())); } catch (IOException ignored) {}
    }

    /**
     * Download a single IOC file from a URL into the indicators folder.
     * @param url the remote or local URL
     * @return the path to the downloaded file or {@code null} on failure
     */
    public Path download(String url) throws IOException, InterruptedException {
        String dl = downloadRemoteIoc(url);
        return dl != null ? Paths.get(dl) : null;
    }

    public void update() throws IOException, InterruptedException {
        setLatestCheck();
        Map<String, Object> index = getRemoteIndex();
        if (index == null) return;
        Object indicators = index.get("indicators");
        if (!(indicators instanceof Iterable<?> inds)) return;
        for (Object obj : inds) {
            if (!(obj instanceof Map)) continue;
            @SuppressWarnings("unchecked")
            Map<String,Object> map = (Map<String,Object>) obj;
            String type = (String) map.getOrDefault("type", "");
            String url;
            if ("github".equals(type)) {
                @SuppressWarnings("unchecked")
                Map<String,Object> gh = (Map<String,Object>) map.get("github");
                String owner = (String) gh.getOrDefault("owner", "");
                String repo = (String) gh.getOrDefault("repo", "");
                String branch = (String) gh.getOrDefault("branch", "main");
                String path = (String) gh.getOrDefault("path", "");
                url = String.format(githubRawUrl, owner, repo, branch, path);
            } else {
                url = (String) map.get("download_url");
            }
            if (url == null || url.isBlank()) continue;
            downloadRemoteIoc(url);
        }
        setLatestUpdate();
    }
}
