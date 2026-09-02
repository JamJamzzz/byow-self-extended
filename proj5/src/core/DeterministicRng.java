package core;

/**
 * Deterministic, explicit-state pseudo-random generator (SplitMix64) used
 * anywhere runtime randomness must be checkpointable.
 *
 * <p>{@code java.util.Random}'s internal state is private and not exposed
 * through its public API, so a checkpoint cannot snapshot/restore it without
 * reflection or Java object serialization -- both fragile. This generator's
 * entire state is a single {@code long}, so {@link #snapshotState()} /
 * {@link #restoreState(long)} are exact and trivial: identical state plus
 * identical subsequent calls always produces identical outputs.
 */
public final class DeterministicRng {
    private long state;

    public DeterministicRng(long seed) {
        this.state = seed;
    }

    /** Next raw 64-bit output. Standard SplitMix64 step. */
    public long nextLong() {
        state += 0x9E3779B97F4A7C15L;
        long z = state;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** Uniform int in [0, bound). Small modulo bias is acceptable for gameplay AI choices. */
    public int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be positive: " + bound);
        }
        return (int) Long.remainderUnsigned(nextLong(), bound);
    }

    public boolean nextBoolean() {
        return (nextLong() & 1L) != 0L;
    }

    /** Uniform double in [0, 1), built from the top 53 bits, same technique java.util.Random uses. */
    public double nextDouble() {
        return (nextLong() >>> 11) * 0x1.0p-53;
    }

    /** The entire generator state -- trivially snapshot-able/restorable for checkpointing. */
    public long snapshotState() {
        return state;
    }

    public void restoreState(long snapshot) {
        this.state = snapshot;
    }
}
