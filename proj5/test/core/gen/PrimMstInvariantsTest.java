package core.gen;

import org.junit.Test;
import verification.VerificationConfig;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the chunk graph + Prim MST invariants across a deterministic
 * seed set: N participating chunks yield exactly N-1 tree edges, every
 * chunk is reachable through tree edges alone, the tree is genuinely
 * acyclic (checked with an independent Union-Find pass over the edge list,
 * not just inferred from the edge count), and no edge (tree or extra) is
 * duplicated. Independent verification that the tree is actually
 * *minimum*-cost lives separately in {@link PrimIsMinimumSpanningTreeTest}.
 */
public class PrimMstInvariantsTest {
    private static final int SEED_COUNT = VerificationConfig.PRIM_INVARIANTS_SEED_COUNT;
    private static final int WIDTH = VerificationConfig.WORLD_WIDTH;
    private static final int HEIGHT = VerificationConfig.WORLD_HEIGHT;
    private static final int CHUNK_ROWS = VerificationConfig.CHUNK_ROWS;
    private static final int CHUNK_COLS = VerificationConfig.CHUNK_COLS;

    @Test
    public void mstAndGraphInvariantsHoldAcrossManySeeds() {
        for (long seed = 0; seed < SEED_COUNT; seed++) {
            GeneratedWorld world = new ChunkedWorldGenerator(WIDTH, HEIGHT, CHUNK_ROWS, CHUNK_COLS, seed).generate();

            int participating = (int) world.chunks().stream().filter(c -> c.anchorRoom() != null).count();
            assertEquals("seed " + seed + ": every chunk should have an anchor room", world.chunks().size(), participating);
            assertEquals("seed " + seed + ": MST edge count must be N-1", participating - 1, world.mstEdges().size());

            assertTrue("seed " + seed + ": MST must connect every chunk", allChunksConnected(world.chunks(), world.mstEdges()));
            assertTrue("seed " + seed + ": MST edge set must not contain a cycle",
                    hasNoCycle(world.chunks(), world.mstEdges()));

            assertNoDuplicateEdges(seed, world.mstEdges(), world.extraEdges());
        }
    }

    private boolean allChunksConnected(List<Chunk> chunks, List<ChunkEdge> mstEdges) {
        if (chunks.isEmpty()) {
            return true;
        }
        Map<Chunk, List<Chunk>> adjacency = buildAdjacency(chunks, mstEdges);

        Set<Chunk> visited = new HashSet<>();
        Deque<Chunk> frontier = new ArrayDeque<>();
        Chunk start = chunks.get(0);
        visited.add(start);
        frontier.add(start);

        while (!frontier.isEmpty()) {
            Chunk cur = frontier.poll();
            for (Chunk next : adjacency.getOrDefault(cur, List.of())) {
                if (visited.add(next)) {
                    frontier.add(next);
                }
            }
        }
        return visited.size() == chunks.size();
    }

    /**
     * Independently confirms the MST edge set contains no cycle: union each
     * edge's two endpoints, and if any edge's endpoints are already in the
     * same component before that union, a cycle exists. This does not
     * assume anything about edge count -- it is a real structural check,
     * complementary to the separate N-1 edge-count assertion above.
     */
    private boolean hasNoCycle(List<Chunk> chunks, List<ChunkEdge> edges) {
        DisjointSetUnion dsu = new DisjointSetUnion(chunks);
        for (ChunkEdge edge : edges) {
            if (!dsu.union(edge.a(), edge.b())) {
                return false;
            }
        }
        return true;
    }

    private Map<Chunk, List<Chunk>> buildAdjacency(List<Chunk> chunks, List<ChunkEdge> edges) {
        Map<Chunk, List<Chunk>> adjacency = new HashMap<>();
        for (Chunk c : chunks) {
            adjacency.put(c, new java.util.ArrayList<>());
        }
        for (ChunkEdge edge : edges) {
            adjacency.get(edge.a()).add(edge.b());
            adjacency.get(edge.b()).add(edge.a());
        }
        return adjacency;
    }

    private void assertNoDuplicateEdges(long seed, List<ChunkEdge> mstEdges, List<ChunkEdge> extraEdges) {
        Set<String> seen = new HashSet<>();
        for (ChunkEdge edge : mstEdges) {
            String key = canonicalKey(edge);
            assertFalse("seed " + seed + ": duplicate MST edge " + edge, seen.contains(key));
            seen.add(key);
        }
        for (ChunkEdge edge : extraEdges) {
            String key = canonicalKey(edge);
            assertFalse("seed " + seed + ": extra edge duplicates an MST edge " + edge, seen.contains(key));
            seen.add(key);
        }
    }

    private String canonicalKey(ChunkEdge edge) {
        int idA = edge.a().id();
        int idB = edge.b().id();
        return Math.min(idA, idB) + "-" + Math.max(idA, idB);
    }
}
