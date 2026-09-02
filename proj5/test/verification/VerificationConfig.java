package verification;

/**
 * Single source of truth for the seed/trace counts and world dimensions
 * used by both the verification test classes and {@link VerificationRunner}.
 * A number quoted in an evidence JSON file always comes from here -- it is
 * never independently retyped in the runner.
 */
public final class VerificationConfig {
    private VerificationConfig() {
    }

    public static final int WORLD_WIDTH = 70;
    public static final int WORLD_HEIGHT = 50;
    public static final int CHUNK_ROWS = 4;
    public static final int CHUNK_COLS = 4;

    public static final int WORLD_CONNECTIVITY_SEED_COUNT = 1000;
    public static final int PRIM_INVARIANTS_SEED_COUNT = 500;
    public static final int PRIM_VS_KRUSKAL_SEED_COUNT = 1000;
    public static final int DETERMINISM_SEED_COUNT = 300;

    public static final int REPLAY_TRACE_COUNT = 40;
    public static final int REPLAY_TRACE_LENGTH = 250;

    public static final int RNG_FUTURE_EQUIVALENCE_SEED_COUNT = 50;
    public static final int RNG_FUTURE_EQUIVALENCE_PREFIX_LENGTH = 60;
    public static final int RNG_FUTURE_EQUIVALENCE_SUFFIX_LENGTH = 120;
}
