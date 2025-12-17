package pl.co.common.util;

import java.security.SecureRandom;
import java.time.Instant;

/**
 * Simple ULID generator (26-char Crockford base32).
 */
public final class UlidGenerator {
    private static final char[] ENCODING = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private UlidGenerator() {
    }

    public static String nextUlid() {
        long time = Instant.now().toEpochMilli();
        byte[] randomness = new byte[10];
        RANDOM.nextBytes(randomness);
        char[] out = new char[26];
        // time 48 bits -> 10 chars
        for (int i = 9; i >= 0; i--) {
            out[i] = ENCODING[(int) (time & 31)];
            time >>>= 5;
        }
        // randomness 80 bits -> 16 chars
        int index = 10;
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : randomness) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                bitsLeft -= 5;
                out[index++] = ENCODING[(buffer >> bitsLeft) & 31];
            }
        }
        if (bitsLeft > 0) {
            out[index] = ENCODING[(buffer << (5 - bitsLeft)) & 31];
        }
        return new String(out);
    }
}
