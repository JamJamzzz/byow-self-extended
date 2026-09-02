package core.gen;

import org.junit.Test;
import tileengine.TETile;

import static org.junit.Assert.assertEquals;

/** Same seed must produce byte-for-byte identical generated layouts. */
public class DeterminismTest {
    private static final int SEED_COUNT = 300;
    private static final int WIDTH = 70;
    private static final int HEIGHT = 50;
    private static final int CHUNK_ROWS = 4;
    private static final int CHUNK_COLS = 4;

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
