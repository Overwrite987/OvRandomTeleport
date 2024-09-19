package ru.overwrite.rtp.utils;

/**
 * Ultra-fast pseudo random generator that is not synchronized!
 * Don't use anything from Random by inheritance, this will inherit
 * a volatile! Not my idea, copyied in parts some demo random
 * generator lessons.
 *
 * @author rschwietzke
 * <p>
 * Stolen from https://github.com/gunnarmorling/1brc/blob/main/src/main/java/org/rschwietzke/FastRandom.java
 * btw still not as fast as dsi fastutil FastRandom
 */
public class FastRandom {

    private long seed;

    private static final double DOUBLE_UNIT = 0x1.0p-53; // 1.0 / (1L << 53)

    public FastRandom() {
        this.seed = System.currentTimeMillis();
    }

    public FastRandom(long seed) {
        this.seed = seed;
    }

    protected int next(int nbits) {
        // N.B. Not thread-safe!
        long x = this.seed;
        x ^= (x << 21);
        x ^= (x >>> 35);
        x ^= (x << 4);
        this.seed = x;

        x &= ((1L << nbits) - 1);

        return (int) x;
    }

    /**
     * Borrowed from the JDK
     *
     * @param bound
     * @return
     */
    public int nextInt(int bound) {
        int r = next(31);
        int m = bound - 1;
        if ((bound & m) == 0) // i.e., bound is a power of 2
            r = (int) ((bound * (long) r) >> 31);
        else {
            for (int u = r; u - (r = u % bound) + m < 0; u = next(31))
                ;
        }
        return r;
    }

    /**
     * Borrowed from the JDK
     *
     * @return
     */
    public int nextInt() {
        return next(32);
    }

    /**
     * Borrowed from the JDK
     *
     * @return
     */
    public double nextDouble() {
        return (((long)(next(26)) << 27) + next(27)) * DOUBLE_UNIT;
    }

    /**
     * Borrowed from the JDK
     *
     * @return
     */
    public boolean nextBoolean() {
        return next(1) != 0;
    }
}
