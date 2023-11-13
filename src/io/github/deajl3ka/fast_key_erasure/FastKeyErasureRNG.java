/*
 * FastKeyErasureRNG: Fast-key-erasure random-number generator for Java
 * Copyright (c) 2023 "dEajL3kA" <Cumpoing79@web.de>
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sub license, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions: The above copyright notice and this
 * permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT
 * OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.deajl3ka.fast_key_erasure;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.security.GeneralSecurityException;
import java.security.KeyException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

/**
 * Fast-key-erasure random-number generator for Java
 * <p>
 * A secure random number generator, based on AES-256 “CTR” mode, with high performance and forward secrecy; it is based on “Fast-key-erasure random-number generators” by D. J. Bernstein.
 * 
 * @author dEajL3kA {@literal <Cumpoing79@web.de>
 */
@SuppressWarnings("serial")
public class FastKeyErasureRNG extends Random {

    private static final int KEY_SIZE = 32, OUT_SIZE = 96, RESEED_INTERVAL = 257;

    /**
     * 128-bit (16 bytes) words to be used as “plaintext” counter values, generated reproducibly to maximize the pairwise hamming-distance, cf. {@link GenerateCounter}
     */
    private static final byte[] PLAINTEXT_K = new byte[] {
        (byte)0x43, (byte)0x1E, (byte)0x12, (byte)0x16, (byte)0xE0, (byte)0x50, (byte)0xB5, (byte)0xBC, (byte)0x90, (byte)0x6D, (byte)0x06, (byte)0xC3, (byte)0x7E, (byte)0xAE, (byte)0x8D, (byte)0xED,
        (byte)0xA4, (byte)0x39, (byte)0x2F, (byte)0x45, (byte)0x08, (byte)0x72, (byte)0x48, (byte)0x63, (byte)0xD2, (byte)0x57, (byte)0xE7, (byte)0x17, (byte)0x48, (byte)0x95, (byte)0xB8, (byte)0x41
    },
    PLAINTEXT_V = new byte[] {
        (byte)0x36, (byte)0x51, (byte)0x08, (byte)0xA3, (byte)0x19, (byte)0xE6, (byte)0x1F, (byte)0x18, (byte)0xAB, (byte)0x0D, (byte)0x31, (byte)0x8D, (byte)0x29, (byte)0xC9, (byte)0x35, (byte)0x3A,
        (byte)0x49, (byte)0x23, (byte)0x29, (byte)0x3A, (byte)0xC4, (byte)0xBD, (byte)0x4B, (byte)0x2A, (byte)0x20, (byte)0xFB, (byte)0x94, (byte)0xB2, (byte)0x1B, (byte)0x75, (byte)0x64, (byte)0xF2,
        (byte)0xFB, (byte)0xAC, (byte)0x80, (byte)0xD4, (byte)0x35, (byte)0x17, (byte)0x84, (byte)0xD8, (byte)0x4E, (byte)0x45, (byte)0x1B, (byte)0x7F, (byte)0x45, (byte)0xEB, (byte)0x52, (byte)0x64,
        (byte)0x3E, (byte)0xFA, (byte)0xCD, (byte)0x9E, (byte)0xBE, (byte)0x74, (byte)0xE0, (byte)0x25, (byte)0xFD, (byte)0x01, (byte)0x27, (byte)0xF1, (byte)0x06, (byte)0x14, (byte)0xE3, (byte)0x96,
        (byte)0x97, (byte)0xA4, (byte)0x51, (byte)0x23, (byte)0x6B, (byte)0xC6, (byte)0x7D, (byte)0x67, (byte)0xC4, (byte)0xF6, (byte)0x9A, (byte)0x50, (byte)0x87, (byte)0xEA, (byte)0x04, (byte)0x0F,
        (byte)0xD0, (byte)0x76, (byte)0xFE, (byte)0x69, (byte)0x30, (byte)0xB8, (byte)0x2A, (byte)0x95, (byte)0x18, (byte)0x74, (byte)0x0C, (byte)0xAF, (byte)0xE1, (byte)0x01, (byte)0x6B, (byte)0x58
    };

    private static final SecureRandom strongRandom;
    static {
        assert PLAINTEXT_K.length == KEY_SIZE : "Inconsistent plaintext size!";
        assert PLAINTEXT_V.length == OUT_SIZE : "Inconsistent plaintext size!";
        try {
            strongRandom = SecureRandom.getInstanceStrong();
        } catch (final GeneralSecurityException e) {
            throw new RuntimeException("Failed to create secure random number generator!", e);
        }
    }

    // ======================================================================
    // Constructor
    // ======================================================================

    private final Cipher cipher;

    private final KeyWrapper wrappedKey = new KeyWrapper();

    private final byte[] keyData = new byte[KEY_SIZE], outData = new byte[OUT_SIZE];

    private int reseedCounter = RESEED_INTERVAL, nextPos = OUT_SIZE;

    protected FastKeyErasureRNG() {
        super(0);
        try {
            cipher = Cipher.getInstance("AES/ECB/NoPadding");
            emplaceKey();
        } catch (final GeneralSecurityException e) {
            throw new RuntimeException("Failed to create the required AES cipher!", e);
        }
   }

    // ======================================================================
    // Key wrapper class
    // ======================================================================

    private class KeyWrapper implements SecretKey {
        @Override
        public String getAlgorithm() {
            return "AES";
        }

        @Override
        public String getFormat() {
            return "RAW";
        }

        @Override
        public byte[] getEncoded() {
            return keyData;
        }
    }

    // ======================================================================
    // Public methods
    // ======================================================================

    public byte[] nextBytes(final int length) {
        if (length < 0) {
            throw new IllegalArgumentException("Length must be a positive value!");
        }

        final byte[] output = new byte[length];
        if (length > 0) {
            nextBytes(output, 0, length);
        }

        return output;
    }

    public void nextBytes(final byte[] bytes, final int offset, final int length) {
        if (bytes == null) {
            throw new IllegalArgumentException("Output array must not be null!");
        }
        if ((offset < 0) || (length < 0) || (offset > bytes.length) || (bytes.length - offset < length)) {
            throw new IllegalArgumentException("Invalid offset and/or length!");
        }

        for (int copyCount, done = 0; done < length; done += copyCount) {
            ensureBufferAvailable();
            System.arraycopy(outData, nextPos, bytes, offset + done, copyCount = Math.min(OUT_SIZE - nextPos, length - done));
            Arrays.fill(outData, nextPos, nextPos += copyCount, (byte)0);
        }
    }

    @Override
    public void nextBytes(final byte[] bytes) {
        nextBytes(bytes, 0, (bytes != null) ? bytes.length : 0);
    }

    public UUID nextUuid() {
        return new UUID(nextLong(), nextLong());
    }

    @Override
    public void setSeed(final long seed) {
        if (seed != 0) {
            setSeed(longToByteArray(seed));
            nextPos = OUT_SIZE;
        }
    }

    public void reseed() {
        doReseed();
        nextPos = OUT_SIZE;
    }

    // ======================================================================
    // Protected methods
    // ======================================================================

    @Override
    protected int next(final int numBits) {
        final int numBytes = (numBits + 7) / 8;
        int value = 0;
        for (int i = 0; i < numBytes; ++i) {
            ensureBufferAvailable();
            value = (value << 8) + (outData[nextPos] & 0xFF);
            outData[nextPos++] = (byte)0; 
        }
        return value >>> ((numBytes * 8) - numBits);
    }

    // ======================================================================
    // Internal methods
    // ======================================================================

    private void ensureBufferAvailable() {
        if (nextPos >= OUT_SIZE) {
            nextBlock();
            nextPos = 0;
        }
    }

    protected void nextBlock() {
        if (++reseedCounter >= RESEED_INTERVAL) {
            doReseed();
        }

        try {
            cipher.update(PLAINTEXT_K, 0, KEY_SIZE, keyData);
            cipher.update(PLAINTEXT_V, 0, OUT_SIZE, outData);
            emplaceKey();
        } catch (final GeneralSecurityException e) {
            throw new RuntimeException("Failed to update CRNG state!", e);
        }
    }

    private final void doReseed() {
        setSeed(strongRandom.generateSeed(KEY_SIZE));
        reseedCounter = 0;
    }

    protected void setSeed(final byte[] seed) {
        assert (seed != null) && (seed.length > 0) && (seed.length <= KEY_SIZE);
        try {
            for (int i = 0; i < 2; ++i) {
                cipher.update(PLAINTEXT_K, 0, KEY_SIZE, keyData);
                xorBytes(keyData, seed);
                emplaceKey();
            }
        } catch (final GeneralSecurityException e) {
            throw new RuntimeException("Failed to re-seed the cipher!", e);
        } finally {
            Arrays.fill(seed, (byte)0);
        }
    }

    private void emplaceKey() throws KeyException {
        try {
            cipher.init(Cipher.ENCRYPT_MODE, wrappedKey);
        } finally {
            Arrays.fill(keyData, (byte)0);
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        throw new NotSerializableException();
    }

    private static void xorBytes(final byte[] target, final byte[] source) {
        assert (target != null) && (source != null) && (target.length >= source.length);
        for (int pos = 0; pos < source.length; ++pos) {
            target[pos] ^= source[pos];
        }
    }

    private static byte[] longToByteArray(long value) {
        final byte[] result = new byte[Long.BYTES];
        for (int pos = Long.BYTES - 1; pos >= 0; --pos, value >>= Byte.SIZE) {
            result[pos] = (byte) (value & 0xffL);
        }
        return result;
    }

    // ======================================================================
    // Factory methods
    // ======================================================================

    private static final ThreadLocal<FastKeyErasureRNG> INSTANCES = ThreadLocal.withInitial(FastKeyErasureRNG::new);

    public static FastKeyErasureRNG current() {
        return INSTANCES.get();
    }

    // ======================================================================
    // Version information
    // ======================================================================

    public static short[] getVersion() {
        try {
            final String version = FastKeyErasureRNG.class.getPackage().getImplementationVersion();
            if ((version != null) && (!version.isEmpty())) {
                final String[] versionParts = version.split("\\.");
                if (versionParts.length > 1) {
                    return new short[] { Short.parseShort(versionParts[0].trim(), 10), Short.parseShort(versionParts[1].trim(), 10) };
                }
            }
        } catch (Exception e) { }
        return new short[] { (short)0, (short)0 };
    }
}
