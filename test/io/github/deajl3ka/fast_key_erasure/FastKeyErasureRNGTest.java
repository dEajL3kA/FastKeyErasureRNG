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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.github.fzakaria.ascii85.Ascii85;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FastKeyErasureRNGTest extends AbstractUnitTest {

    private final Logger logger = Logger.getLogger("FastKeyErasureRNGTest");

    private final SortedMap<String, MutableLong> callStats =  Collections.synchronizedSortedMap(new TreeMap<String, MutableLong>(String.CASE_INSENSITIVE_ORDER));

    // ======================================================================
    // Constructor
    // ======================================================================

    public FastKeyErasureRNGTest() {
        logger.addHandler(new Handler() {
            @Override
            public void publish(final LogRecord record) {
                if (record != null) {
                    final String methodName = record.getMessage();
                    if ((methodName != null) && (!methodName.isEmpty())) {
                        MutableLong.increment(callStats, methodName);
                    }
                }
            }
            @Override public void flush() { }
            @Override public void close() throws SecurityException { }
        });
        logger.setUseParentHandlers(false);
    }

    // ======================================================================
    // Utility methods
    // ======================================================================

    @BeforeEach
    private void initialize() {
        final AtomicBoolean assertionsEnabled = new AtomicBoolean(false);
        assert assertionsEnabled.compareAndSet(false, true);
        assertTrue(assertionsEnabled.get(), "Assertions must be enabled!");
    }

    private long getStats(final String methodName) {
        final MutableLong counter = callStats.get(methodName);
        return (counter != null) ? counter.asLong() : 0L;
    }

    @AfterEach
    private void printStats() {
        for (final Entry<String, MutableLong> entry : callStats.entrySet()) {
            System.out.printf("Method \"%s\" was called %d times.%n", entry.getKey(), entry.getValue().asLong());
        }
    }

    // ======================================================================
    // Instrumentation
    // ======================================================================

    @SuppressWarnings("serial")
    private static FastKeyErasureRNG createInstance(final Logger logger) {
        return new FastKeyErasureRNG() {
            private final Logger log = Objects.requireNonNull(logger, "Logger must not be null!");

            @Override
            protected final int next(final int numBits) {
                if (log.isLoggable(Level.INFO)) {
                    log.log(Level.INFO, "next", numBits);
                }
                return super.next(numBits);
            }

            @Override
            protected final void nextBlock() {
                if (log.isLoggable(Level.INFO)) {
                    log.log(Level.INFO, "nextBlock");
                }
                super.nextBlock();
            }

            @Override
            protected final void setSeed(final byte[] seed) {
                if (log.isLoggable(Level.INFO)) {
                    log.log(Level.INFO, "setSeed", Base64.getEncoder().encodeToString(seed));
                }
                super.setSeed(seed);
            }
        };
    }

    // ======================================================================
    // Test methods
    // ======================================================================

    @Test
    @Order(1)
    public void testNextBytes() {
        // Create instance
        final FastKeyErasureRNG instance = createInstance(logger);

        // Initialize length array
        final List<Integer> LENGTHS = Collections.unmodifiableList(Arrays.asList(64, 73, 97));

        // Accumulate total number of bytes
        long totalBytes = 0L;

        for (final int length : LENGTHS) {
            // Initialize set
            final HashSet<String> hashSet = new HashSet<String>();

            for (int i = 0; i < 4999999; ++i) {
                // Generate array
                final byte[] array = instance.nextBytes(length);
                totalBytes += array.length;
                assertEquals(length, array.length);

                // Convert to ASCII
                final String ascii = Ascii85.encode(array);
                System.out.println(ascii);

                // Add to the set
                assertTrue(hashSet.add(ascii));
            }
        }

        // Print stats
        final long expectedBlocks = (totalBytes + 63) / 64;
        System.out.printf("Total bytes generated: %d (total blocks generated: %d)%n", totalBytes, expectedBlocks);

        // Verify stats
        assertEquals(0L, getStats("next"));
        assertEquals(expectedBlocks, getStats("nextBlock"));
        assertEquals((expectedBlocks + 256) / 257, getStats("setSeed"));
    }

    @Test
    @Order(2)
    public void testNextBytesInplace() {
        // Create instance
        final FastKeyErasureRNG instance = createInstance(logger);

        // Initialize length array
        final List<Integer> LENGTHS = Collections.unmodifiableList(Arrays.asList(64, 73, 97));

        // Accumulate total number of bytes
        long totalBytes = 0L;

        for (final int length : LENGTHS) {
            // Initialize set
            final HashSet<String> hashSet = new HashSet<String>();

            // Allocate array
            final byte[] array = new byte[length];

            for (int i = 0; i < 4999999; ++i) {
                // Generate array
                instance.nextBytes(array);
                totalBytes += array.length;

                // Convert to ASCII
                final String ascii = Ascii85.encode(array);
                System.out.println(ascii);

                // Add to the set
                assertTrue(hashSet.add(ascii));
            }
        }

        // Print stats
        final long expectedBlocks = (totalBytes + 63) / 64;
        System.out.printf("Total bytes generated: %d (total blocks generated: %d)%n", totalBytes, expectedBlocks);

        // Verify stats
        assertEquals(0L, getStats("next"));
        assertEquals(expectedBlocks, getStats("nextBlock"));
        assertEquals((expectedBlocks + 256) / 257, getStats("setSeed"));
    }

    @Test
    @Order(3)
    public void testNextBytesRange() {
        // Create instance
        final FastKeyErasureRNG instance = createInstance(logger);

        // Local random generator
        final ThreadLocalRandom localRandom = ThreadLocalRandom.current();

        // Allocate array
        final byte[] array = new byte[97];

        // Initialize set
        final HashSet<String> hashSet = new HashSet<String>();

        // Accumulate total number of bytes
        long totalBytes = 0L;

        for (int i = 0; i < 4999999; ++i) {
            final int offset = localRandom.nextInt(11, 32);
            final int length = localRandom.nextInt(13, 60);

            // Generate array
            generatorLoop:
            for (;;) {
                Arrays.fill(array, (byte)0);
                instance.nextBytes(array, offset, length);
                totalBytes += length;
                for (int check = offset; check < offset + length; ++check) {
                    if (array[check] == 0) {
                        continue generatorLoop;
                    }
                }
                break;
            }

            // Convert to ASCII
            System.out.println(Ascii85.encode(array));

            // Verify
            for (int check = 0; check < offset; ++check) {
                assertEquals((byte)0, array[check]);
            }
            for (int check = offset; check < offset + length; ++check) {
                assertNotEquals((byte)0, array[check]);
            }
            for (int check = offset + length; check < 97; ++check) {
                assertEquals((byte)0, array[check]);
            }

            // Add to the set
            final String innerSection = Ascii85.encode(Arrays.copyOfRange(array, offset, offset + length));
            assertTrue(hashSet.add(innerSection));
        }

        // Print stats
        final long expectedBlocks = (totalBytes + 63) / 64;
        System.out.printf("Total bytes generated: %d (total blocks generated: %d)%n", totalBytes, expectedBlocks);

        // Verify stats
        assertEquals(0L, getStats("next"));
        assertEquals(expectedBlocks, getStats("nextBlock"));
        assertEquals((expectedBlocks + 256) / 257, getStats("setSeed"));
    }

    @Test
    @Order(4)
    public void testNextBoolean() {
        // Create instance
        final FastKeyErasureRNG instance = createInstance(logger);

        // Initialize set
        final long[] stats = new long[2];

        // Accumulate total number of bytes
        long totalBytes = 0L;

        for (int i = 0; i < 4999999; ++i) {
            // Generate value
            final boolean boolValue = instance.nextBoolean();
            ++totalBytes;
            System.out.println(Boolean.toString(boolValue));

            // Update stats
            ++stats[boolValue ? 1 : 0];
        }

        // Sort
        Arrays.sort(stats);

        // Compute ratio
        final double ratio = stats[0] / (double)stats[1];
        System.out.printf("%010d / %010d [%.5f]%n", stats[0], stats[1], ratio);
        assertTrue(ratio >= 0.99);

        // Print stats
        final long expectedBlocks = (totalBytes + 63) / 64;
        System.out.printf("Total bytes generated: %d (total blocks generated: %d)%n", totalBytes, expectedBlocks);
        
        // Verify stats
        assertEquals(totalBytes, getStats("next"));
        assertEquals(expectedBlocks, getStats("nextBlock"));
        assertEquals((expectedBlocks + 256) / 257, getStats("setSeed"));
    }

    @Test
    @Order(5)
    public void testNextInt() {
        // Create instance
        final FastKeyErasureRNG instance = createInstance(logger);

        // Initialize the maximum 
        final int[] finalSetSize = new int[997];

        // Accumulate total number of bytes
        long totalBytes = 0L;

        for (int i = 0; i < finalSetSize.length; ++i) {
            // Initialize set
            final HashSet<Integer> hashSet = new HashSet<Integer>();

            for (;;) {
                // Generate value
                final int intValue = instance.nextInt();
                totalBytes += Integer.BYTES;
                System.out.println(toHexString(intValue, 8));

                // Add to the set
                if (!hashSet.add(intValue)) {
                    break;
                }
            }

            // Store size
            finalSetSize[i] = hashSet.size();
        }

        // Sort results
        Arrays.sort(finalSetSize);

        // Verify median size
        final int quarter = finalSetSize.length / 4;
        final double average = Arrays.stream(finalSetSize).sorted().skip(quarter).limit(finalSetSize.length - (2 * quarter)).average().getAsDouble();
        System.out.printf("Average set size: %.2f%n", average);
        assertTrue(average >= 65536.0);
        assertTrue(average < 131072.0);

        // Print stats
        final long expectedBlocks = (totalBytes + 63) / 64;
        System.out.printf("Total bytes generated: %d (total blocks generated: %d)%n", totalBytes, expectedBlocks);

        // Verify stats
        assertEquals(totalBytes / 4, getStats("next"));
        assertEquals(expectedBlocks, getStats("nextBlock"));
        assertEquals((expectedBlocks + 256) / 257, getStats("setSeed"));
    }

    @Test
    @Order(6)
    public void testNextLong() {
        // Create instance
        final FastKeyErasureRNG instance = createInstance(logger);

        // Initialize set
        final HashSet<Long> hashSet = new HashSet<Long>();

        // Accumulate total number of bytes
        long totalBytes = 0L;

        for (int i = 0; i < 4999999; ++i) {
            // Generate value
            final long longValue = instance.nextLong();
            totalBytes += Long.BYTES;
            System.out.println(toHexString(longValue, 16));

            // Add to the set
            assertTrue(hashSet.add(longValue));
        }

        // Print stats
        final long expectedBlocks = (totalBytes + 63) / 64;
        System.out.printf("Total bytes generated: %d (total blocks generated: %d)%n", totalBytes, expectedBlocks);

        // Verify stats
        assertEquals(totalBytes / 4, getStats("next"));
        assertEquals(expectedBlocks, getStats("nextBlock"));
        assertEquals((expectedBlocks + 256) / 257, getStats("setSeed"));
    }

    @Test
    @Order(7)
    public void testNextDouble() {
        // Create instance
        final FastKeyErasureRNG instance = createInstance(logger);

        // Initialize set
        final HashSet<Double> hashSet = new HashSet<Double>();

        // Accumulate total number of bytes
        long totalBytes = 0L;

        for (int i = 0; i < 4999999; ++i) {
            // Generate value
            final double dblValue = instance.nextDouble();
            totalBytes += Double.BYTES;
            System.out.printf("%.10f%n", dblValue);

            // Add to the set
            assertTrue(hashSet.add(dblValue));
        }

        // Print stats
        final long expectedBlocks = (totalBytes + 63) / 64;
        System.out.printf("Total bytes generated: %d (total blocks generated: %d)%n", totalBytes, expectedBlocks);

        // Verify stats
        assertEquals(totalBytes / 4, getStats("next"));
        assertEquals(expectedBlocks, getStats("nextBlock"));
        assertEquals((expectedBlocks + 256) / 257, getStats("setSeed"));
    }

    @Test
    @Order(8)
    public void testNextUuid() {
        // Create instance
        final FastKeyErasureRNG instance = createInstance(logger);

        // Initialize set
        final HashSet<UUID> hashSet = new HashSet<UUID>();

        // Accumulate total number of bytes
        long totalBytes = 0L;

        for (int i = 0; i < 99999989; ++i) {
            // Generate value
            final UUID uuid = instance.nextUuid();
            totalBytes += Long.BYTES + Long.BYTES;
            System.out.println(uuid);

            // Add to the set
            assertTrue(hashSet.add(uuid));
        }

        // Print stats
        final long expectedBlocks = (totalBytes + 63) / 64;
        System.out.printf("Total bytes generated: %d (total blocks generated: %d)%n", totalBytes, expectedBlocks);

        // Verify stats
        assertEquals(totalBytes / 4, getStats("next"));
        assertEquals(expectedBlocks, getStats("nextBlock"));
        assertEquals((expectedBlocks + 256) / 257, getStats("setSeed"));
    }

    @Test
    @Order(9)
    public void testByteDistribution() {
        // Create instance
        final FastKeyErasureRNG instance = createInstance(logger);

        // Initialize arrays
        final byte[] array = new byte[64];
        final long[][] byteStats = new long[64][256]; 

        // Accumulate total number of bytes
        long totalBytes = 0L;

        for (int i = 0; i < 7499969; ++i) {
            // Generate array
            instance.nextBytes(array);
            System.out.println(Ascii85.encode(array));
            totalBytes += array.length;

            // Add to the set
            for (int j = 0; j < 64; ++j) {
                ++byteStats[j][array[j] & 0xFF];
            }
        }

        // Compute ration of most/less frequent values
        for (int j = 0; j < 64; ++j) {
            final long minFrequency = Arrays.stream(byteStats[j]).min().getAsLong();
            final long maxFrequency = Arrays.stream(byteStats[j]).max().getAsLong();
            final double ratio = minFrequency / (double)maxFrequency;
            System.out.printf("#%02d -> %010d / %010d [%.5f]%n", j, minFrequency, maxFrequency, ratio);
            assertTrue(ratio >= 0.95);
        }

        // Print stats
        final long expectedBlocks = (totalBytes + 63) / 64;
        System.out.printf("Total bytes generated: %d (total blocks generated: %d)%n", totalBytes, expectedBlocks);

        // Verify stats
        assertEquals(0L, getStats("next"));
        assertEquals(expectedBlocks, getStats("nextBlock"));
        assertEquals((expectedBlocks + 256) / 257, getStats("setSeed"));
    }

    @Test
    @Order(10)
    public void testBitDistribution() {
        // Create instance
        final FastKeyErasureRNG instance = createInstance(logger);

        // Initialize arrays
        final byte[] array = new byte[64];
        final long[][] bitStats = new long[64][2];

        // Accumulate total number of bytes
        long totalBytes = 0L;

        for (int i = 0; i < 7499969; ++i) {
            // Generate array
            instance.nextBytes(array);
            System.out.println(Ascii85.encode(array));
            totalBytes += array.length;

            // Update stats
            for (int j = 0; j < 64; ++j) {
                for (int k = 0; k < Byte.SIZE; ++k) {
                    ++bitStats[j][(array[j] & (0x1 << k)) >>> k];
                }
            }
        }

        // Compute ration of most/less frequent values
        for (int j = 0; j < 64; ++j) {
            final long minFrequency = Arrays.stream(bitStats[j]).min().getAsLong();
            final long maxFrequency = Arrays.stream(bitStats[j]).max().getAsLong();
            final double ratio = minFrequency / (double)maxFrequency;
            System.out.printf("#%02d -> %010d / %010d [%.5f]%n", j, minFrequency, maxFrequency, ratio);
            assertTrue(ratio >= 0.99);
        }

        // Print stats
        final long expectedBlocks = (totalBytes + 63) / 64;
        System.out.printf("Total bytes generated: %d (total blocks generated: %d)%n", totalBytes, expectedBlocks);

        // Verify stats
        assertEquals(0L, getStats("next"));
        assertEquals(expectedBlocks, getStats("nextBlock"));
        assertEquals((expectedBlocks + 256) / 257, getStats("setSeed"));
    }

    @Test
    @Order(11)
    public void testShortDistribution() {
        // Create instance
        final FastKeyErasureRNG instance = createInstance(logger);

        // Initialize arrays
        final ByteBuffer buffer = ByteBuffer.allocate(64);
        final byte[] array = buffer.array();
        final long[] shortStats = new long[65536];

        // Accumulate total number of bytes
        long totalBytes = 0L;

        for (int i = 0; i < 49999991; ++i) {
            // Generate array
            instance.nextBytes(array);
            System.out.println(Ascii85.encode(array));
            totalBytes += array.length;

            // Update stats
            for (int j = 0; j < 63; ++j) {
                ++shortStats[buffer.getShort(j) & 0xFFFF];
            }
        }

        // Compute ration of most/less frequent values
        final long minFrequency = Arrays.stream(shortStats).min().getAsLong();
        final long maxFrequency = Arrays.stream(shortStats).max().getAsLong();
        final double ratio = minFrequency / (double)maxFrequency;
        System.out.printf("%010d / %010d [%.5f]%n", minFrequency, maxFrequency, ratio);
        assertTrue(ratio >= 0.95);

        // Print stats
        final long expectedBlocks = (totalBytes + 63) / 64;
        System.out.printf("Total bytes generated: %d (total blocks generated: %d)%n", totalBytes, expectedBlocks);

        // Verify stats
        assertEquals(0L, getStats("next"));
        assertEquals(expectedBlocks, getStats("nextBlock"));
        assertEquals((expectedBlocks + 256) / 257, getStats("setSeed"));
    }

    @Test
    @Order(12)
    public void testTuplesDistribution() {
        // Create instance
        final FastKeyErasureRNG instance = createInstance(logger);

        // Initialize iterations array
        final List<Integer> ITERATIONS = Collections.unmodifiableList(Arrays.asList(4999999, 7499969, 99999989));

        // Accumulate total number of bytes
        long totalBytes = 0L;

        for (int length = 3; length < 6; ++length) {
            // Initialize arrays
            final List<int[]> list = Stream.generate(Tuple::create).limit(length).collect(Collectors.toCollection(ArrayList<int[]>::new));
            final long[] tupleStats = new long[radixHash(nCopies(length, length - 1).toArray(), length) + 1];

            // Get number of iterations
            final int iterations = ITERATIONS.get(length - 3);

            for (int i = 0; i < iterations; ++i) {
                // Update list
                for (int j = 0; j < length; ++j) {
                    final int[] currentElement = list.get(j);
                    currentElement[0] = j;
                    currentElement[1] = instance.nextInt();
                    totalBytes += Integer.BYTES;
                }

                // Sort by value
                list.sort((a, b) -> Integer.compare(a[1], b[1]));

                // Retrieve indices and values
                final int[] indices = list.stream().mapToInt(Tuple::getFirst).toArray();
                final int[] values = list.stream().mapToInt(Tuple::getSecond).toArray();

                // Update stats
                final int key = radixHash(indices, length);
                ++tupleStats[key];

                // Print details 
                System.out.println(toHexString(key, 3) + " <-- " + Arrays.toString(indices) + " <-- " + Arrays.toString(values));
            }

            // Print result
            for (int key = 0; key < tupleStats.length; ++key) {
                if (tupleStats[key] != 0L) {
                    System.out.printf("%03X -> %8d%n", key, tupleStats[key]);
                }
            }

            // Assert completeness
            final long[] nonZeroEntries = Arrays.stream(tupleStats).filter(val -> (val != 0L)).toArray();
            assertEquals(factorial(length), nonZeroEntries.length);

            // Compute ration of most/less frequent values
            final long minFrequency = Arrays.stream(nonZeroEntries).min().getAsLong();
            final long maxFrequency = Arrays.stream(nonZeroEntries).max().getAsLong();
            final double ratio = minFrequency / (double)maxFrequency;
            System.out.printf("%010d / %010d [%.5f]%n", minFrequency, maxFrequency, ratio);
            assertTrue(ratio >= 0.99);
        }

        // Print stats
        final long expectedBlocks = (totalBytes + 63) / 64;
        System.out.printf("Total bytes generated: %d (total blocks generated: %d)%n", totalBytes, expectedBlocks);

        // Verify stats
        assertEquals(totalBytes / 4, getStats("next"));
        assertEquals(expectedBlocks, getStats("nextBlock"));
        assertEquals((expectedBlocks + 256) / 257, getStats("setSeed"));
    }

    @Test
    @Order(13)
    public void testSetSeed() throws IOException {
        // Create instance
        final FastKeyErasureRNG instance = createInstance(logger);

        // Initialize set
        final HashSet<Long> hashSet = new HashSet<Long>();

        // Accumulate total number of bytes
        long totalBytes = 0L, expectedBlocks = 0L;

        final ThreadLocalRandom localRandom = ThreadLocalRandom.current();
        for (int i = 0; i < 4999999; ++i) {
            // Add seed value
            instance.setSeed(localRandom.nextLong());
            ++expectedBlocks;
            
            // Generate value
            final long longValue = instance.nextLong();
            totalBytes += Long.BYTES;
            System.out.println(toHexString(longValue, 16));

            // Add to the set
            assertTrue(hashSet.add(longValue));
        }

        // Print stats
        System.out.printf("Total bytes generated: %d (total blocks generated: %d)%n", totalBytes, expectedBlocks);

        // Verify stats
        assertEquals(totalBytes / 4, getStats("next"));
        assertEquals(expectedBlocks, getStats("nextBlock"));
        assertEquals(expectedBlocks + ((expectedBlocks + 256) / 257), getStats("setSeed"));
    }

    @Test
    @Order(14)
    public void testSerialization() throws IOException {
        // Create instance
        final FastKeyErasureRNG instance = createInstance(logger);

        // Try to serialize
        assertThrows(NotSerializableException.class, () -> {
            try (final ObjectOutputStream objectOutputStream = new ObjectOutputStream(nullOutputStream())) {
                try {
                    objectOutputStream.writeObject(instance);
                } catch (final Throwable e) {
                    System.out.println("Exception: " + e.toString());
                    throw e;
                }
            }
        });
    }
}
