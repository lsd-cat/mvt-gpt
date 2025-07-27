package org.osservatorionessuno.libmvt.android.parsers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.osservatorionessuno.libmvt.common.Utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.KeySpec;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.zip.InflaterInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

/** Utilities to parse Android backup (.ab) files and extract SMS messages. */
@SuppressWarnings("deprecation")
public final class BackupParser {
    private BackupParser() {}

    public static class AndroidBackupParsingException extends Exception {
        public AndroidBackupParsingException(String msg) { super(msg); }
        public AndroidBackupParsingException(String msg, Throwable t) { super(msg, t); }
    }

    public static class InvalidBackupPassword extends AndroidBackupParsingException {
        public InvalidBackupPassword() { super("Invalid backup password"); }
    }

    private static byte[] readLine(ByteArrayInputStream in) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\n') break;
            bos.write(b);
        }
        return bos.toByteArray();
    }

    /** Parse an android backup file and return the raw TAR data. */
    public static byte[] parseBackupFile(byte[] data, String password) throws AndroidBackupParsingException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        String magic = new String(readLine(in), StandardCharsets.UTF_8);
        if (!"ANDROID BACKUP".equals(magic)) {
            throw new AndroidBackupParsingException("Invalid file header");
        }
        int version = Integer.parseInt(new String(readLine(in), StandardCharsets.UTF_8));
        boolean compressed = "1".equals(new String(readLine(in), StandardCharsets.UTF_8));
        String encryption = new String(readLine(in), StandardCharsets.UTF_8);
        byte[] rest = in.readAllBytes();
        if (!"none".equals(encryption)) {
            rest = decryptBackupData(rest, password, encryption, version);
        }
        if (compressed) {
            try (InflaterInputStream inf = new InflaterInputStream(new ByteArrayInputStream(rest))) {
                rest = inf.readAllBytes();
            } catch (IOException ex) {
                throw new AndroidBackupParsingException("Impossible to decompress the backup file", ex);
            }
        }
        return rest;
    }

    private static byte[] decryptBackupData(byte[] enc, String password, String algo, int version)
            throws AndroidBackupParsingException {
        if (!"AES-256".equals(algo)) {
            throw new AndroidBackupParsingException("Encryption algorithm not implemented");
        }
        if (password == null) throw new InvalidBackupPassword();
        ByteArrayInputStream in = new ByteArrayInputStream(enc);
        byte[] userSalt = new String(readLine(in), StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8);
        byte[] checksumSalt = new String(readLine(in), StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8);
        int rounds = Integer.parseInt(new String(readLine(in), StandardCharsets.UTF_8));
        byte[] userIv = new String(readLine(in), StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8);
        byte[] masterKeyBlob = new String(readLine(in), StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = in.readAllBytes();
        userSalt = hexToBytes(new String(userSalt, StandardCharsets.UTF_8));
        checksumSalt = hexToBytes(new String(checksumSalt, StandardCharsets.UTF_8));
        userIv = hexToBytes(new String(userIv, StandardCharsets.UTF_8));
        masterKeyBlob = hexToBytes(new String(masterKeyBlob, StandardCharsets.UTF_8));
        byte[][] mk = decryptMasterKey(password, userSalt, userIv, rounds, masterKeyBlob, version, checksumSalt);
        byte[] masterKey = mk[0];
        byte[] masterIv = mk[1];
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(masterKey, "AES"), new IvParameterSpec(masterIv));
            byte[] decrypted = cipher.doFinal(encrypted);
            return decrypted;
        } catch (GeneralSecurityException ex) {
            throw new AndroidBackupParsingException("Failed to decrypt", ex);
        }
    }

    private static byte[][] decryptMasterKey(String password, byte[] userSalt, byte[] userIv,
                                             int rounds, byte[] masterBlob, int version, byte[] checksumSalt)
            throws AndroidBackupParsingException {
        try {
            SecretKeyFactory kf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            KeySpec spec = new PBEKeySpec(password.toCharArray(), userSalt, rounds, 256);
            byte[] key = kf.generateSecret(spec).getEncoded();
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(userIv));
            byte[] decrypted = cipher.doFinal(masterBlob);
            ByteArrayInputStream in = new ByteArrayInputStream(decrypted);
            int ivLen = in.read();
            byte[] masterIv = in.readNBytes(ivLen);
            int keyLen = in.read();
            byte[] masterKey = in.readNBytes(keyLen);
            int checksumLen = in.read();
            byte[] checksum = in.readNBytes(checksumLen);
            byte[] hmacMk = version > 1 ? toUtf8Bytes(masterKey) : masterKey;
            spec = new PBEKeySpec(new String(hmacMk, StandardCharsets.UTF_8).toCharArray(), checksumSalt, rounds, 256);
            byte[] calcChecksum = kf.generateSecret(spec).getEncoded();
            if (!Arrays.equals(calcChecksum, checksum)) throw new InvalidBackupPassword();
            return new byte[][]{masterKey, masterIv};
        } catch (GeneralSecurityException | IOException ex) {
            throw new AndroidBackupParsingException("Failed to decrypt", ex);
        }
    }

    private static byte[] toUtf8Bytes(byte[] input) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        for (byte b : input) {
            int ub = b & 0xff;
            if (ub < 0x80) {
                bos.write(ub);
            } else {
                bos.write(0xef | (ub >> 12));
                bos.write(0xbc | ((ub >> 6) & 0x3f));
                bos.write(0x80 | (ub & 0x3f));
            }
        }
        return bos.toByteArray();
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }

    /** Parse SMS/MMS backup files from a TAR archive. */
    public static List<Map<String, Object>> parseTarForSms(byte[] tarData) throws IOException {
        List<Map<String, Object>> res = new ArrayList<>();
        try (TarArchiveInputStream tin = new TarArchiveInputStream(new ByteArrayInputStream(tarData))) {
            TarArchiveEntry entry;
            while ((entry = tin.getNextTarEntry()) != null) {
                String name = entry.getName();
                if (name.startsWith("apps/com.android.providers.telephony/d_f/") &&
                        (name.endsWith("_sms_backup") || name.endsWith("_mms_backup"))) {
                    byte[] file = tin.readNBytes((int) entry.getSize());
                    res.addAll(parseSmsFile(file));
                }
            }
        }
        return res;
    }

    /** Parse an individual SMS or MMS backup file. */
    public static List<Map<String, Object>> parseSmsFile(byte[] data) throws IOException {
        try (InflaterInputStream inf = new InflaterInputStream(new ByteArrayInputStream(data))) {
            byte[] jsonBytes = inf.readAllBytes();
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> records = mapper.readValue(jsonBytes, new TypeReference<>(){});
            List<Map<String, Object>> res = new ArrayList<>();
            Pattern urlRx = Pattern.compile("https?://\\S+", Pattern.CASE_INSENSITIVE);
            for (Map<String, Object> r : records) {
                if (r.containsKey("mms_body")) {
                    r.put("body", r.remove("mms_body"));
                }
                Object bodyObj = r.get("body");
                if (bodyObj instanceof String s) {
                    Matcher m = urlRx.matcher(s);
                    List<String> links = new ArrayList<>();
                    while (m.find()) links.add(m.group());
                    if (!links.isEmpty() || s.trim().isEmpty()) r.put("links", links);
                }
                long date = Long.parseLong(r.getOrDefault("date", "0").toString());
                r.put("isodate", Utils.toIso(date));
                long sent = Long.parseLong(r.getOrDefault("date_sent", "0").toString());
                r.put("direction", sent > 0 ? "sent" : "received");
                res.add(r);
            }
            return res;
        }
    }
}
