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

import java.util.Locale;
import java.util.SplittableRandom;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class GenerateCounter {

    private static final int THREAD_COUNT = 8, WORDS = 6, EXPECTED_DISTANCE = 68;

    private static final SplittableRandom splittableRandom = new SplittableRandom(0x93C467E37DB0C7A4L);

    private static int bestDistance = -1;

    private static final Object mutex = new Object();

    private static final CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT);

    private static class Int128 {
        final long hi, lo;

        public Int128(final long hi, final long lo) {
            this.hi = hi;
            this.lo = lo;
        }

        public String toString(final boolean verbose) {
            final String hexString = toString();
            if (!verbose) {
                return hexString;
            }
            final String upperString = hexString.toUpperCase(Locale.ENGLISH);
            final StringBuilder sb = new StringBuilder("{ ");
            int pos = 0;
            while (pos < hexString.length()) {
                if (pos > 0) {
                    sb.append(", ");
                }
                sb.append("(byte)0x");
                sb.append(upperString.subSequence(pos, pos += 2));
            }
            return sb.append(" }").toString();
        }

        @Override
        public String toString() {
            return longToHexStr(hi) + longToHexStr(lo);
        }
    }

    private static int distance(final Int128 a,final Int128 b) {
        return Long.bitCount(a.hi ^ b.hi) + Long.bitCount(a.lo ^ b.lo);
    }

    public static void main(String[] args) {
        final Thread[] threads = new Thread[THREAD_COUNT];
        for (int tid = 0; tid < THREAD_COUNT; ++tid) {
            threads[tid] = new Thread(GenerateCounter::threadMain);
            threads[tid].start();
        }
        for (final Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private static void threadMain() {
        final Int128[] values = new Int128[WORDS];
        final char[][] nibble = new char[WORDS][];

        final SplittableRandom random;
        synchronized (mutex) {
            random = splittableRandom.split();
        }

        for (;;) {
            for (int i = 0; i < WORDS; ++i) {
                generatorLoop:
                for (;;) {
                    final char[] thisNibbles; 
                    try {
                        values[i] = new Int128(random.nextLong(), random.nextLong());
                        thisNibbles = nibble[i] = values[i].toString().toCharArray();
                    } catch(NumberFormatException e) {
                        continue generatorLoop;
                    }
                    for (int k = 0; k < 31; ++k) {
                        if (thisNibbles[k] == thisNibbles[k + 1]) {
                            continue generatorLoop;
                        }
                    }
                    for (int k = 0; k < 30; k += 2) {
                        if ((thisNibbles[k] == thisNibbles[k + 2]) && (thisNibbles[k+1] == thisNibbles[k + 3])) {
                            continue generatorLoop;
                        }
                    }
                    for (int j = 0; j < i; ++j) {
                        final char[] otherNibbles = nibble[j];
                        for (int k = 0; k < 32; ++k) {
                            if (thisNibbles[k] == otherNibbles[k]) {
                                continue generatorLoop;
                            }
                        }
                    }
                    break; /*okay*/
                }
            }

            int minDistance = Integer.MAX_VALUE;
            for (int i = 0; i < WORDS; ++i) {
                for (int j = i + 1; j < WORDS; ++j) {
                    final int thisDistance = distance(values[i], values[j]);
                    if (thisDistance < minDistance) {
                        minDistance = thisDistance;
                    }
                }
            }

            synchronized (mutex) {
                if (minDistance >= bestDistance) {
                    bestDistance = minDistance;
                    System.out.printf("[%d]%n", bestDistance);
                    for (int i = 0; i < WORDS; ++i) {
                        final String word = new String(nibble[i]).toUpperCase(Locale.ENGLISH);
                        if (!values[i].toString().equalsIgnoreCase(word)) {
                            throw new AssertionError("Whoops!");
                        }
                        System.out.println(word);
                    }
                    System.out.println();
                    if (bestDistance >= EXPECTED_DISTANCE) {
                        for (int i = 0; i < WORDS; ++i) {
                            System.out.println(values[i].toString(true));
                        }
                        System.out.println();
                    }
                }
            }

            try {
                barrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                return;
            }

            synchronized (mutex) {
                if (bestDistance >= EXPECTED_DISTANCE) {
                    return;
                }
            }
        }
    }

    private static String longToHexStr(final long value) {
        final String str = Long.toHexString(value);
        switch (str.length()) {
        case 16:
            return str;
        case 15:
            return '0' + str;
        case 14:
            return "00" + str;
        default:
            throw new NumberFormatException();
        }
    }
}
