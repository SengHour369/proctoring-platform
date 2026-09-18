package com.example.identityservice.auth.security;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/** RFC 6238 TOTP: 6-digit codes on a 30-second step, HMAC-SHA1. */
@Component
public class TotpService {

    private static final int STEP_SECONDS = 30;
    private static final int DIGITS = 6;

    /** True if {@code code} matches the seed at the current step, within {@code windowSteps} either side. */
    public boolean verify(String base32Seed, String code, int windowSteps) {
        long currentStep = System.currentTimeMillis() / 1000 / STEP_SECONDS;
        for (long step = currentStep - windowSteps; step <= currentStep + windowSteps; step++) {
            if (generate(base32Seed, step).equals(code)) {
                return true;
            }
        }
        return false;
    }

    private String generate(String base32Seed, long step) {
        try {
            byte[] key = base32Decode(base32Seed);
            byte[] data = new byte[8];
            long value = step;
            for (int i = 7; i >= 0; i--) {
                data[i] = (byte) (value & 0xFF);
                value >>= 8;
            }
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            int otp = binary % (int) Math.pow(10, DIGITS);
            return String.format("%0" + DIGITS + "d", otp);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate TOTP", e);
        }
    }

    /** Generates a random Base32 TOTP seed suitable for authenticator apps. */
    public String generateSeed(java.security.SecureRandom secureRandom) {
        byte[] bytes = new byte[20];
        secureRandom.nextBytes(bytes);
        return base32Encode(bytes);
    }

    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private static String base32Encode(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int bits = 0;
        int value = 0;
        for (byte b : data) {
            value = (value << 8) | (b & 0xFF);
            bits += 8;
            while (bits >= 5) {
                sb.append(BASE32_ALPHABET.charAt((value >>> (bits - 5)) & 0x1F));
                bits -= 5;
            }
        }
        if (bits > 0) {
            sb.append(BASE32_ALPHABET.charAt((value << (5 - bits)) & 0x1F));
        }
        return sb.toString();
    }

    private static byte[] base32Decode(String encoded) {
        String clean = encoded.trim().toUpperCase().replace("=", "");
        int bits = 0;
        int value = 0;
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        for (char c : clean.toCharArray()) {
            int idx = BASE32_ALPHABET.indexOf(c);
            if (idx < 0) {
                continue;
            }
            value = (value << 5) | idx;
            bits += 5;
            if (bits >= 8) {
                out.write((value >>> (bits - 8)) & 0xFF);
                bits -= 8;
            }
        }
        return out.toByteArray();
    }
}
