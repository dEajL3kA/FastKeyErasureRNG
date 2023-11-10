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
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

public abstract class AbstractUnitTest {

    protected static final String EMPTY_STRING = new String();

    private static final String ZERO_STRING = "00000000000000000000000000000000";

    @BeforeEach
    public void printHeader(final TestInfo testInfo) {
        final String displayName = testInfo.getDisplayName();
        final String separator = repeat('~', displayName.length() + 10);
        System.out.println(separator);
        System.out.println("~~~~ " + displayName + " ~~~~");
        System.out.println(separator);
    }

    // ======================================================================
    // I/O operations
    // ======================================================================

    protected static OutputStream nullOutputStream() {
        return new OutputStream() {
            @Override
            public void write(int b) throws IOException { }
            @Override
            public void write(byte b[], int off, int len) throws IOException { }
        };
    }

    // ======================================================================
    // MutableLong class
    // ======================================================================

    protected static class MutableLong {
        protected long value;

        public MutableLong(final long value) {
            this.value = value;
        }

        public static MutableLong zero() {
            return new MutableLong(0L);
        }

        public long asLong() {
            return value;
        }

        public void set(final long value) {
            this.value = value;
        }

        public void increment() {
            ++value;
        }

        public static <K> void increment(final Map<K, MutableLong> map, final K key) {
            final MutableLong instance = Objects.requireNonNull(map).computeIfAbsent(Objects.requireNonNull(key), k -> zero());
            instance.increment();
        }
    }

    // ======================================================================
    // String formatting
    // ======================================================================

    protected static String toHexString(final int value, final int digits) {
        return zeroPadding(Integer.toHexString(value), digits);
    }

    protected static String toHexString(final long value, final int digits) {
        return zeroPadding(Long.toHexString(value), digits);
    }

    protected static String toDecString(final int value, final int digits) {
        return zeroPadding(Integer.toString(value, 10), digits);
    }

    protected static String toDecString(final long value, final int digits) {
        return zeroPadding(Long.toString(value, 10), digits);
    }

    protected static String repeat(final String str, final int count) {
        Objects.requireNonNull(str, "Sting must not be nul!");
        if ((count > 1) && (!str.isEmpty())) {
            final StringBuilder sb = new StringBuilder(Math.multiplyExact(str.length(), count));
            for (int i = 0; i < count; ++i) {
                sb.append(str);
            }
            return sb.toString();
        }
        return (count < 1) ? EMPTY_STRING : str;
    }

    protected static String repeat(final char c, final int count) {
        if (count > 0) {
            final StringBuilder sb = new StringBuilder(count);
            for (int i = 0; i < count; ++i) {
                sb.append(c);
            }
            return sb.toString();
        }
        return EMPTY_STRING;
    }

    protected static String zeroPadding(final String str, final int digits) {
        Objects.requireNonNull(str, "Sting must not be null!");
        final int length;
        if ((length = str.length()) < digits) {
            final int paddingChars = digits - length;
            try {
                return ZERO_STRING.substring(0, paddingChars) + str;
            } catch(IndexOutOfBoundsException e) {
                return repeat('0', paddingChars) + str;
            }
        }
        return str;
    }

    // ======================================================================
    // Tuples support
    // ======================================================================

    protected static class Tuple {
        private Tuple() {
            throw new UnsupportedOperationException();
        }

        public static int[] create() {
            return new int[2];
        }

        public static int getFirst(final int[] val) {
            return val[0];
        }

        public static int getSecond(final int[] val) {
            return val[1];
        }
    }

    protected static IntStream nCopies(final int count, final int value) {
        return IntStream.generate(() -> value).limit(count);
    }

    // ======================================================================
    // Hashing
    // ======================================================================

    protected static int radixHash(final int[] values, final int radix) {
        if (radix < 2) {
            throw new IllegalArgumentException("Invalid radix!");
        }
        int hash = 0;
        for (final int value : Objects.requireNonNull(values, "Values must not be null!")) {
            hash *= radix;
            hash += value % radix;
        }
        return hash;
    }

    protected static int factorial(final int number) {
        if (number < 1) {
            throw new IllegalArgumentException("Parameter must be postive!");
        }
        int fact = 1;
        for (int factor = 2; factor <= number; factor++) {
            fact = Math.multiplyExact(fact, factor);
        }
        return fact;
    }

    // ======================================================================
    // Reflection
    // ======================================================================

    protected static <T> T getField(final Class<FastKeyErasureRNG> clazz, final Class<T> resultType, final String name) {
        try {
            final Field field = clazz.getDeclaredField(name);
            boolean retryFlag = false;
            if (!Modifier.isStatic(field.getModifiers())) {
                throw new IllegalArgumentException("Not a static field!");
            }
            for (;;) {
                try {
                    return resultType.cast(field.get(null));
                } catch (IllegalAccessException e) {
                    if (retryFlag) {
                        return null;
                    }
                    field.setAccessible(true);
                    retryFlag = true;
                }
            }
        } catch (ClassCastException | ReflectiveOperationException | SecurityException e) {
            return null;
        }
    }
}
