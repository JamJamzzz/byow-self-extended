package core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;

/** Shortest-path click-to-move search over walkable tiles. Decoupled from {@link World} so it can be unit tested directly. */
public final class BfsPathfinder {
    private BfsPathfinder() {
    }

    public static List<Position> findPath(World world, Position start, Position target) {
        return findPathAgainst(start, target, world::isWalkable);
    }

    /**
     * @return the ordered steps from start to target (exclusive of start), an
     *     empty list if start already equals target, or null if target is
     *     unwalkable or unreachable.
     */
    public static List<Position> findPathAgainst(Position start, Position target, Predicate<Position> isWalkable) {
        if (start.equals(target)) {
            return new ArrayList<>();
        }
        if (!isWalkable.test(target)) {
            return null;
        }

        Queue<Position> queue = new ArrayDeque<>();
        Map<Position, Position> parent = new HashMap<>();
        Set<Position> visited = new HashSet<>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            Position cur = queue.poll();
            if (cur.equals(target)) {
                break;
            }
            for (Position next : neighbors(cur)) {
                if (!visited.contains(next) && isWalkable.test(next)) {
                    visited.add(next);
                    parent.put(next, cur);
                    queue.add(next);
                }
            }
        }

        return reconstruct(parent, start, target);
    }

    private static List<Position> neighbors(Position p) {
        List<Position> result = new ArrayList<>(4);
        result.add(new Position(p.x + 1, p.y));
        result.add(new Position(p.x - 1, p.y));
        result.add(new Position(p.x, p.y + 1));
        result.add(new Position(p.x, p.y - 1));
        return result;
    }

    private static List<Position> reconstruct(Map<Position, Position> parent, Position start, Position target) {
        if (!parent.containsKey(target)) {
            return null;
        }
        List<Position> path = new ArrayList<>();
        Position cur = target;
        while (!cur.equals(start)) {
            path.add(cur);
            cur = parent.get(cur);
        }
        java.util.Collections.reverse(path);
        return path;
    }
}
