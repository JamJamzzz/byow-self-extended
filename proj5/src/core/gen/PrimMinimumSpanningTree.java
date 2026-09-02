package core.gen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;

/**
 * Builds the chunk-adjacency graph and connects it with a real Prim
 * minimum spanning tree: start from one seeded chunk, repeatedly grow the
 * tree by taking the cheapest edge leaving it, and stop once every
 * participating chunk has been absorbed. A frontier {@link PriorityQueue}
 * plays the role of the classic "fringe" set.
 */
public final class PrimMinimumSpanningTree {

    private PrimMinimumSpanningTree() {
    }

    /** All valid candidate edges between geometrically-adjacent chunks that each have an anchor room. */
    public static List<ChunkEdge> candidateEdges(List<Chunk> chunks, Random graphRng) {
        List<ChunkEdge> edges = new ArrayList<>();
        // Chunks are iterated in stable id order, and each unordered pair is
        // visited exactly once, so the tie-break draws below are reproducible
        // for a given seed no matter how the chunk list was built.
        for (Chunk chunk : chunks) {
            if (chunk.anchorRoom() == null) {
                continue;
            }
            for (Chunk neighbor : chunk.neighbors()) {
                if (neighbor.id() <= chunk.id() || neighbor.anchorRoom() == null) {
                    continue;
                }
                long cost = manhattan(chunk, neighbor);
                edges.add(new ChunkEdge(chunk, neighbor, cost, graphRng.nextLong()));
            }
        }
        return edges;
    }

    private static long manhattan(Chunk a, Chunk b) {
        return Math.abs((long) a.anchorRoom().centerAtX() - b.anchorRoom().centerAtX())
                + Math.abs((long) a.anchorRoom().centerAtY() - b.anchorRoom().centerAtY());
    }

    /** Result of running Prim's algorithm: the selected tree edges and the chunks it reached. */
    public static final class Result {
        public final List<ChunkEdge> mstEdges;
        public final Set<Chunk> connected;

        Result(List<ChunkEdge> mstEdges, Set<Chunk> connected) {
            this.mstEdges = mstEdges;
            this.connected = connected;
        }
    }

    public static Result build(List<Chunk> participatingChunks, List<ChunkEdge> candidateEdges, Random graphRng) {
        Map<Chunk, List<ChunkEdge>> adjacency = new HashMap<>();
        for (Chunk c : participatingChunks) {
            adjacency.put(c, new ArrayList<>());
        }
        for (ChunkEdge edge : candidateEdges) {
            adjacency.get(edge.a()).add(edge);
            adjacency.get(edge.b()).add(edge);
        }

        List<ChunkEdge> mstEdges = new ArrayList<>();
        Set<Chunk> visited = new HashSet<>();

        if (participatingChunks.isEmpty()) {
            return new Result(mstEdges, visited);
        }

        Chunk start = participatingChunks.get(graphRng.nextInt(participatingChunks.size()));
        visited.add(start);

        PriorityQueue<ChunkEdge> frontier = new PriorityQueue<>();
        frontier.addAll(adjacency.get(start));

        while (!frontier.isEmpty() && visited.size() < participatingChunks.size()) {
            ChunkEdge edge = frontier.poll();
            Chunk newChunk = edge.other(visited);
            if (newChunk == null) {
                // Both endpoints already in the tree: a stale frontier edge, skip it.
                continue;
            }
            mstEdges.add(edge);
            visited.add(newChunk);
            frontier.addAll(adjacency.get(newChunk));
        }

        return new Result(mstEdges, visited);
    }

    /**
     * Candidate edges left out of the MST, each included independently with
     * probability {@code extraEdgeProbability} (drawn from the same seeded
     * graph RNG, continuing deterministically right after the MST draws).
     */
    public static List<ChunkEdge> selectExtraEdges(List<ChunkEdge> candidateEdges, List<ChunkEdge> mstEdges,
                                                     double extraEdgeProbability, Random graphRng) {
        Set<ChunkEdge> tree = new HashSet<>(mstEdges);
        List<ChunkEdge> extras = new ArrayList<>();
        for (ChunkEdge edge : candidateEdges) {
            if (tree.contains(edge)) {
                continue;
            }
            if (graphRng.nextDouble() < extraEdgeProbability) {
                extras.add(edge);
            }
        }
        return extras;
    }
}
