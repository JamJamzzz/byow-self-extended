package core.gen;

import java.util.Set;

/**
 * A candidate connection between two neighboring chunks in the chunk graph.
 * The cost is the Manhattan distance between the chunks' anchor rooms, so
 * the minimum spanning tree favors geometrically short corridors. The
 * tie-break value is drawn once (deterministically, from the seeded graph
 * RNG) per edge so that equal-cost edges still resolve the same way on
 * every run of the same seed, regardless of priority-queue internals.
 */
public final class ChunkEdge implements Comparable<ChunkEdge> {
    private final Chunk a;
    private final Chunk b;
    private final long cost;
    private final long tieBreak;

    public ChunkEdge(Chunk a, Chunk b, long cost, long tieBreak) {
        this.a = a;
        this.b = b;
        this.cost = cost;
        this.tieBreak = tieBreak;
    }

    public Chunk a() {
        return a;
    }

    public Chunk b() {
        return b;
    }

    public long cost() {
        return cost;
    }

    /** Returns the endpoint of this edge that is not yet in the tree, or null if both are. */
    public Chunk other(Set<Chunk> visited) {
        if (!visited.contains(a)) {
            return a;
        }
        if (!visited.contains(b)) {
            return b;
        }
        return null;
    }

    public boolean connects(Chunk x, Chunk y) {
        return (a == x && b == y) || (a == y && b == x);
    }

    @Override
    public int compareTo(ChunkEdge other) {
        int byCost = Long.compare(this.cost, other.cost);
        if (byCost != 0) {
            return byCost;
        }
        return Long.compare(this.tieBreak, other.tieBreak);
    }

    @Override
    public String toString() {
        return a + "--" + b + " (cost=" + cost + ")";
    }
}
