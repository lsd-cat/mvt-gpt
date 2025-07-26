package re.mvt.android.artifacts;

import re.mvt.common.Artifact;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DumpsysAccessibility extends AndroidArtifact {

    @Override
    public void parse(String input) {
        List<String> lines = Arrays.asList(input.split("\n"));
        Pattern legacyPattern = Pattern.compile("\\s*(\\d+) : (.+)");
        Pattern v14Pattern = Pattern.compile("\\{\\{(.+?)}}", Pattern.DOTALL);

        boolean legacy = false;
        boolean v14 = false;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.trim().startsWith("installed services:")) {
                legacy = true;
                for (int j = i + 1; j < lines.size(); j++) {
                    Matcher m = legacyPattern.matcher(lines.get(j));
                    if (m.find()) {
                        String fullService = m.group(2).trim();
                        String packageName = fullService.split("/")[0];
                        Map<String, String> result = new HashMap<>();
                        result.put("package_name", packageName);
                        result.put("service", fullService);
                        results.add(result);
                    } else if (lines.get(j).trim().startsWith("}")) {
                        break;
                    }
                }
            } else if (line.trim().startsWith("Enabled services:")) {
                v14 = true;
                for (int j = i; j < lines.size(); j++) {
                    Matcher m = v14Pattern.matcher(lines.get(j));
                    if (m.find()) {
                        String fullService = m.group(1).trim();
                        String packageName = fullService.split("/")[0];
                        Map<String, String> result = new HashMap<>();
                        result.put("package_name", packageName);
                        result.put("service", fullService);
                        results.add(result);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void checkIndicators() {
        if (indicators == null) return;
        for (Object obj : results) {
            @SuppressWarnings("unchecked")
            Map<String, String> record = (Map<String, String>) obj;
            String context = record.get("service");
            detected.addAll(indicators.matchString(context, re.mvt.common.IndicatorType.PROCESS));
        }
    }
}