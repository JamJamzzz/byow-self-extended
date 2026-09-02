package core;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Unit tests against a small synthetic grid, independent of World/generation. */
public class BfsPathfinderTest {

    // A 5x5 open room with a single wall column at x=2 except a gap at y=2:
    //   . . # . .
    //   . . # . .
    //   . . . . .   <- gap at (2,2)
    //   . . # . .
    //   . . # . .
    private boolean walkable(Position p) {
        if (p.x < 0 || p.x >= 5 || p.y < 0 || p.y >= 5) {
            return false;
        }
        if (p.x == 2 && p.y != 2) {
            return false;
        }
        return true;
    }

    @Test
    public void startEqualsTargetReturnsEmptyPath() {
        List<Position> path = BfsPathfinder.findPathAgainst(new Position(0, 0), new Position(0, 0), this::walkable);
        assertEquals(0, path.size());
    }

    @Test
    public void unreachableTargetReturnsNull() {
        // (2,0) is a wall tile in this layout -> unwalkable target.
        List<Position> path = BfsPathfinder.findPathAgainst(new Position(0, 0), new Position(2, 0), this::walkable);
        assertNull(path);
    }

    @Test
    public void blockedIsolatedTargetReturnsNull() {
        // Everything outside the grid is unwalkable.
        List<Position> path = BfsPathfinder.findPathAgainst(new Position(0, 0), new Position(9, 9), this::walkable);
        assertNull(path);
    }

    @Test
    public void findsShortestPathThroughGap() {
        List<Position> path = BfsPathfinder.findPathAgainst(new Position(0, 2), new Position(4, 2), this::walkable);
        assertEquals(4, path.size());
        assertEquals(new Position(4, 2), path.get(path.size() - 1));

        Position prev = new Position(0, 2);
        for (Position step : path) {
            assertTrue(manhattan(prev, step) == 1);
            prev = step;
        }
    }

    @Test
    public void pathHasNoRepeatedTiles() {
        List<Position> path = BfsPathfinder.findPathAgainst(new Position(0, 0), new Position(1, 4), this::walkable);
        Set<Position> seen = new HashSet<>(path);
        assertEquals(path.size(), seen.size());
    }

    @Test
    public void deterministicAcrossRepeatedCalls() {
        List<Position> first = BfsPathfinder.findPathAgainst(new Position(0, 0), new Position(4, 4), this::walkable);
        List<Position> second = BfsPathfinder.findPathAgainst(new Position(0, 0), new Position(4, 4), this::walkable);
        assertEquals(first, second);
    }

    private int manhattan(Position a, Position b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    }
}
