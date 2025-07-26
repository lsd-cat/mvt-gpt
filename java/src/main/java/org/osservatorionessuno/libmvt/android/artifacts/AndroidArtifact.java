package org.osservatorionessuno.libmvt.android.artifacts;

import org.osservatorionessuno.libmvt.common.Artifact;

/**
 * Base class for Android-related artifact parsers.
 * Still pure Java, only operates on Strings.
 */
public abstract class AndroidArtifact extends Artifact {

    /**
     * Extract a section from a dumpsys string by a separator, stopping at a line starting with '---'.
     */
    protected static String extractDumpsysSection(String dumpsys, String separator) {
        StringBuilder sb = new StringBuilder();
        boolean inSection = false;
        for (String line : dumpsys.split("\n")) {
            if (line.trim().equals(separator)) {
                inSection = true;
                continue;
            }
            if (!inSection) continue;
            if (line.trim().startsWith("---")) break;
            sb.append(line).append('\n');
        }
        return sb.toString();
    }
}
