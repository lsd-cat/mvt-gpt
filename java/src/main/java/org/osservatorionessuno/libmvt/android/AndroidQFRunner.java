package org.osservatorionessuno.libmvt.android;

import org.osservatorionessuno.libmvt.android.artifacts.*;
import org.osservatorionessuno.libmvt.common.Artifact;
import org.osservatorionessuno.libmvt.common.Indicators;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Simple helper to run the available AndroidQF artifact parsers on a folder
 * containing extracted androidqf data.
 */
public class AndroidQFRunner {
    private final Path directory;
    private Indicators indicators;

    public AndroidQFRunner(Path directory) {
        this.directory = directory;
    }

    /** Assign indicators to use for IOC matching. */
    public void setIndicators(Indicators indicators) {
        this.indicators = indicators;
    }

    /** Run all known modules on the provided directory. */
    public Map<String, Artifact> runAll() throws Exception {
        Map<String, Artifact> map = new LinkedHashMap<>();
        for (String name : AVAILABLE_MODULES) {
            Artifact art = runModule(name);
            if (art != null) {
                map.put(name, art);
            }
        }
        return map;
    }

    /** Run a single module by name. */
    public Artifact runModule(String moduleName) throws Exception {
        return runModule(moduleName, this.directory);
    }

    /** Run a single module on a custom directory. */
    public Artifact runModule(String moduleName, Path dir) throws Exception {
        return switch (moduleName) {
            case "dumpsys_accessibility" -> runDumpsysSection(dir, new DumpsysAccessibility(),
                    "DUMP OF SERVICE accessibility:");
            case "dumpsys_activities" -> runDumpsysSection(dir, new DumpsysPackageActivities(),
                    "DUMP OF SERVICE package:");
            case "dumpsys_receivers" -> runDumpsysSection(dir, new DumpsysReceivers(),
                    "DUMP OF SERVICE package:");
            case "dumpsys_adb" -> runDumpsysSection(dir, new DumpsysAdb(),
                    "DUMP OF SERVICE adb:");
            case "dumpsys_appops" -> runDumpsysSection(dir, new DumpsysAppops(),
                    "DUMP OF SERVICE appops:");
            case "dumpsys_battery_daily" -> runDumpsysSection(dir, new DumpsysBatteryDaily(),
                    "DUMP OF SERVICE batterystats:");
            case "dumpsys_battery_history" -> runDumpsysSection(dir, new DumpsysBatteryHistory(),
                    "DUMP OF SERVICE batterystats:");
            case "dumpsys_dbinfo" -> runDumpsysSection(dir, new DumpsysDBInfo(),
                    "DUMP OF SERVICE dbinfo:");
            case "dumpsys_packages" -> runDumpsysSection(dir, new DumpsysPackages(),
                    "DUMP OF SERVICE package:");
            case "dumpsys_platform_compat" -> runDumpsysSection(dir, new DumpsysPlatformCompat(),
                    "DUMP OF SERVICE platform_compat:");
            case "processes" -> runSimpleFile(dir, "ps.txt", new Processes());
            case "getprop" -> runSimpleFile(dir, "getprop.txt", new GetProp());
            case "settings" -> runSettings(dir);
            default -> throw new IllegalArgumentException("Unknown module: " + moduleName);
        };
    }

    private Artifact finalizeArtifact(AndroidArtifact art) {
        if (indicators != null) {
            art.setIndicators(indicators);
            art.checkIndicators();
        }
        return art;
    }

    private Artifact runDumpsysSection(Path dir, AndroidArtifact art, String header) throws Exception {
        Path file = dir.resolve("dumpsys.txt");
        if (!Files.exists(file)) return null;
        String dumpsys = Files.readString(file);
        String section = extractSection(dumpsys, header);
        art.parse(section);
        return finalizeArtifact(art);
    }

    private Artifact runSimpleFile(Path dir, String name, AndroidArtifact art) throws Exception {
        Path file = dir.resolve(name);
        if (!Files.exists(file)) return null;
        String data = Files.readString(file);
        art.parse(data);
        return finalizeArtifact(art);
    }

    private Artifact runSettings(Path dir) throws Exception {
        List<Path> files;
        try (var stream = Files.list(dir)) {
            files = stream.filter(p -> p.getFileName().toString().startsWith("settings_")
                    && p.getFileName().toString().endsWith(".txt")).toList();
        }
        if (files.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        for (Path f : files) {
            sb.append(Files.readString(f)).append("\n");
        }
        Settings settings = new Settings();
        settings.parse(sb.toString());
        return finalizeArtifact(settings);
    }

    private static String extractSection(String dumpsys, String header) {
        List<String> lines = new ArrayList<>();
        boolean inSection = false;
        String delimiter = "-".repeat(78);
        for (String line : dumpsys.split("\n")) {
            if (line.trim().equals(header)) {
                inSection = true;
                continue;
            }
            if (!inSection) continue;
            if (line.trim().startsWith(delimiter)) break;
            lines.add(line);
        }
        return String.join("\n", lines);
    }

    /** List of all module names understood by the runner. */
    public static final List<String> AVAILABLE_MODULES = List.of(
            "dumpsys_accessibility",
            "dumpsys_activities",
            "dumpsys_receivers",
            "dumpsys_adb",
            "dumpsys_appops",
            "dumpsys_battery_daily",
            "dumpsys_battery_history",
            "dumpsys_dbinfo",
            "dumpsys_packages",
            "dumpsys_platform_compat",
            "processes",
            "getprop",
            "settings"
    );
}
