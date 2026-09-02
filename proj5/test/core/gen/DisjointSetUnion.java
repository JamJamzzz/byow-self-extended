package core.gen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Standard Union-Find with path compression and union-by-rank, used only by
 * tests as an independent structural oracle (cycle detection, Kruskal's
 * MST). Deliberately not shared with any production code -- the point is to
 * cross-check the production graph algorithms with a second, unrelated
 * implementation.
 */
final class DisjointSetUnion {
    private final Map<Chunk, Chunk> parent = new HashMap<>();
    private final Map<Chunk, Integer> rank = new HashMap<>();

    DisjointSetUnion(List<Chunk> elements) {
        for (Chunk c : elements) {
            parent.put(c, c);
            rank.put(c, 0);
        }
    }

    Chunk find(Chunk c) {
        Chunk p = parent.get(c);
        if (p != c) {
            p = find(p);
            parent.put(c, p);
        }
        return p;
    }

    /** Unions the two components. @return false if they were already in the same component (i.e. this edge would close a cycle). */
    boolean union(Chunk a, Chunk b) {
        Chunk rootA = find(a);
        Chunk rootB = find(b);
        if (rootA == rootB) {
            return false;
        }
        int rankA = rank.get(rootA);
        int rankB = rank.get(rootB);
        if (rankA < rankB) {
            parent.put(rootA, rootB);
        } else if (rankA > rankB) {
            parent.put(rootB, rootA);
        } else {
            parent.put(rootB, rootA);
            rank.put(rootA, rankA + 1);
        }
        return true;
    }
}
