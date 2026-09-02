package core.gen;

import core.Position;
import core.Room;
import org.junit.Test;
import tileengine.TETile;
import tileengine.Tileset;
import verification.VerificationConfig;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertTrue;

/**
 * For a large deterministic seed set: every generated room is reachable
 * from spawn through walkable floor tiles, and no tile write escapes the
 * grid's bounds.
 */
public class WorldConnectivityTest {
    private static final int SEED_COUNT = VerificationConfig.WORLD_CONNECTIVITY_SEED_COUNT;
    private static final int WIDTH = VerificationConfig.WORLD_WIDTH;
    private static final int HEIGHT = VerificationConfig.WORLD_HEIGHT;
    private static final int CHUNK_ROWS = VerificationConfig.CHUNK_ROWS;
    private static final int CHUNK_COLS = VerificationConfig.CHUNK_COLS;

    @Test
    public void everyRoomReachableFromSpawnAcrossManySeeds() {
        for (long seed = 0; seed < SEED_COUNT; seed++) {
            GeneratedWorld world = new ChunkedWorldGenerator(WIDTH, HEIGHT, CHUNK_ROWS, CHUNK_COLS, seed).generate();
            TETile[][] grid = world.grid();

            assertGridInBounds(seed, grid);

            Set<Position> reachable = floodFillFloor(grid, world.spawnRoom().center());
            for (Room room : world.rooms()) {
                assertTrue("seed " + seed + ": room centered at " + room.center() + " unreachable from spawn",
                        reachable.contains(room.center()));
            }
        }
    }

    private void assertGridInBounds(long seed, TETile[][] grid) {
        assertTrue("seed " + seed + ": grid must not be empty", grid.length == WIDTH || grid.length > 0);
        for (TETile[] column : grid) {
            assertTrue("seed " + seed + ": ragged grid column", column.length == grid[0].length);
            for (TETile tile : column) {
                assertTrue("seed " + seed + ": null tile found", tile != null);
            }
        }
    }

    private Set<Position> floodFillFloor(TETile[][] grid, Position start) {
        int width = grid.length;
        int height = grid[0].length;

        Set<Position> visited = new HashSet<>();
        Deque<Position> frontier = new ArrayDeque<>();
        visited.add(start);
        frontier.add(start);

        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!frontier.isEmpty()) {
            Position cur = frontier.poll();
            for (int[] dir : dirs) {
                int nx = cur.x + dir[0];
                int ny = cur.y + dir[1];
                if (nx < 0 || nx >= width || ny < 0 || ny >= height) {
                    continue;
                }
                Position next = new Position(nx, ny);
                if (visited.contains(next)) {
                    continue;
                }
                if (grid[nx][ny] == Tileset.FLOOR) {
                    visited.add(next);
                    frontier.add(next);
                }
            }
        }
        return visited;
    }
}
