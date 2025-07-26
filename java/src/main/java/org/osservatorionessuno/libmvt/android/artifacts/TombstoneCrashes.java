package org.osservatorionessuno.libmvt.android.artifacts;

import org.osservatorionessuno.libmvt.common.IndicatorType;
import com.android.server.os.TombstoneProtos;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.*;

/** Parser for Android tombstone crash files (text format only). */
public class TombstoneCrashes extends AndroidArtifact {
    @Override
    public void parse(String input) {
        results.clear();
        if (input == null) return;
        Map<String, Object> rec = new HashMap<>();
        for (String line : input.split("\n")) {
            line = line.trim();
            if (line.startsWith("Timestamp:")) {
                String ts = line.substring(10).trim();
                ts = ts.replaceFirst("[+-][0-9]{4}$", "");
                if (ts.contains(".")) {
                    int dot = ts.indexOf('.');
                    String frac = ts.substring(dot + 1);
                    if (frac.length() > 6) frac = frac.substring(0, 6);
                    ts = ts.substring(0, dot) + "." + frac;
                }
                rec.put("timestamp", ts);
            } else if (line.startsWith("Cmdline:")) {
                String cmd = line.substring(8).trim();
                rec.put("command_line", List.of(cmd));
            } else if (line.startsWith("uid:")) {
                try { rec.put("uid", Integer.parseInt(line.substring(4).trim())); } catch (NumberFormatException ignored) {}
            } else if (line.startsWith("pid:")) {
                // pid: 25541, tid: 21307, name: mtk.ape.decoder  >>> /vendor/bin/hw/android.hardware.media.c2@1.2-mediatek <<<
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    String pid = parts[0].split(":" )[1].trim();
                    String tid = parts[1].split(":" )[1].trim();
                    String rest = parts[2].trim();
                    if (rest.startsWith("name:")) {
                        rest = rest.substring(5).trim();
                        String[] nameParts = rest.split(">>>");
                        String procName = nameParts[0].trim();
                        rec.put("process_name", procName);
                    }
                    try { rec.put("pid", Integer.parseInt(pid)); } catch (NumberFormatException ignored) {}
                    try { rec.put("tid", Integer.parseInt(tid)); } catch (NumberFormatException ignored) {}
                }
            }
        }
        if (!rec.isEmpty()) results.add(rec);
    }

    /** Parse Android tombstone protobuf data. */
    public void parseProtobuf(byte[] data) {
        results.clear();
        if (data == null) return;
        Map<String, Object> rec = new HashMap<>();
        try {
            TombstoneProtos.Tombstone pb = TombstoneProtos.Tombstone.parseFrom(data);
            String ts = pb.getTimestamp();
            ts = ts.replaceFirst("[+-][0-9]{4}$", "");
            if (ts.contains(".")) {
                int dot = ts.indexOf('.');
                String frac = ts.substring(dot + 1);
                if (frac.length() > 6) frac = frac.substring(0, 6);
                ts = ts.substring(0, dot) + "." + frac;
            }
            rec.put("timestamp", ts);
            if (!pb.getCommandLineList().isEmpty()) {
                rec.put("command_line", new ArrayList<>(pb.getCommandLineList()));
            }
            rec.put("pid", pb.getPid());
            rec.put("tid", pb.getTid());
            rec.put("uid", pb.getUid());
            if (pb.getThreadsMap().containsKey(pb.getTid())) {
                String name = pb.getThreadsMap().get(pb.getTid()).getName();
                if (!name.isEmpty()) rec.put("process_name", name);
            }
        } catch (InvalidProtocolBufferException ignored) {
        }
        if (!rec.isEmpty()) results.add(rec);
    }

    @Override
    public void checkIndicators() {
        if (indicators == null) return;
        for (Object obj : results) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) obj;
            String proc = (String) map.get("process_name");
            if (proc != null) detected.addAll(indicators.matchString(proc, IndicatorType.PROCESS));
            Object cmdLineObj = map.get("command_line");
            if (cmdLineObj instanceof List<?> list && !list.isEmpty()) {
                String cmd = list.get(0).toString();
                int slash = cmd.lastIndexOf('/');
                String name = slash >=0 ? cmd.substring(slash+1) : cmd;
                detected.addAll(indicators.matchString(name, IndicatorType.PROCESS));
            }
        }
    }
}
