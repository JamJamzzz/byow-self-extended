package core.gen;

import org.junit.Test;
import tileengine.TETile;
import verification.VerificationConfig;

import static org.junit.Assert.assertEquals;

/** Same seed must produce byte-for-byte identical generated layouts. */
public class DeterminismTest {
    private static final int SEED_COUNT = VerificationConfig.DETERMINISM_SEED_COUNT;
    private static final int WIDTH = VerificationConfig.WORLD_WIDTH;
    private static final int HEIGHT = VerificationConfig.WORLD_HEIGHT;
    private static final int CHUNK_ROWS = VerificationConfig.CHUNK_ROWS;
    private static final int CHUNK_COLS = VerificationConfig.CHUNK_COLS;

    @Test
    public void sameSeedProducesIdenticalGridAcrossManySeeds() {
        for (long seed = 0; seed < SEED_COUNT; seed++) {
            String first = canonicalHash(seed);
            String second = canonicalHash(seed);
            assertEquals("seed " + seed + ": two generations of the same seed diverged", first, second);
        }
    }

    @Test
    public void differentSeedsUsuallyProduceDifferentLayouts() {
        String a = canonicalHash(1L);
        String b = canonicalHash(2L);
        assertEquals(false, a.equals(b));
    }

    private String canonicalHash(long seed) {
        GeneratedWorld world = new ChunkedWorldGenerator(WIDTH, HEIGHT, CHUNK_ROWS, CHUNK_COLS, seed).generate();
        TETile[][] grid = world.grid();
        StringBuilder sb = new StringBuilder(grid.length * grid[0].length);
        for (TETile[] column : grid) {
            for (TETile tile : column) {
                sb.append(tile.character());
            }
        }
        return sb.toString();
    }
}
