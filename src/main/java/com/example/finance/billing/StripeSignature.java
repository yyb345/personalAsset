package com.example.finance.billing;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

final class StripeSignature {
    static boolean valid(String payload, String header, String secret) {
        if (header == null || secret.isBlank()) return false;
        try {
            String timestamp = null;
            for (String part : header.split(",")) if (part.startsWith("t=")) timestamp = part.substring(2);
            long seconds = Long.parseLong(timestamp);
            if (Math.abs(Instant.now().getEpochSecond() - seconds) > 300) return false;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = mac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8));
            for (String part : header.split(",")) {
                if (part.startsWith("v1=") && MessageDigest.isEqual(expected, HexFormat.of().parseHex(part.substring(3)))) return true;
            }
        } catch (Exception ignored) { return false; }
        return false;
    }
}
