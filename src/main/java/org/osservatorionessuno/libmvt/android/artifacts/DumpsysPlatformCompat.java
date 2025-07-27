package org.osservatorionessuno.libmvt.android.artifacts;

import org.osservatorionessuno.libmvt.common.IndicatorType;

import java.util.*;

/** Parser for dumpsys platform_compat output. */
public class DumpsysPlatformCompat extends AndroidArtifact {
    @Override
    public void parse(String input) {
        results.clear();
        if (input == null) return;
        for (String line : input.split("\n")) {
            line = line.trim();
            if (!line.startsWith("ChangeId(168419799; name=DOWNSCALED")) continue;
            int idx = line.indexOf("rawOverrides={");
            if (idx < 0) continue;
            String overrides = line.substring(idx + 14);
            int end = overrides.indexOf("};");
            if (end >= 0) overrides = overrides.substring(0, end);
            for (String entry : overrides.split(",")) {
                String pkg = entry.split("=")[0].trim();
                Map<String, String> rec = new HashMap<>();
                rec.put("package_name", pkg);
                results.add(rec);
            }
        }
    }

    @Override
    public void checkIndicators() {
        if (indicators == null) return;
        for (Object obj : results) {
            @SuppressWarnings("unchecked")
            Map<String, String> map = (Map<String, String>) obj;
            String pkg = map.get("package_name");
            detected.addAll(indicators.matchString(pkg, IndicatorType.APP_ID));
        }
    }
}
