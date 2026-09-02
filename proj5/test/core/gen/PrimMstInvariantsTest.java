package core.gen;

import org.junit.Test;

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
 * chunk is reachable through tree edges alone, the tree is acyclic, and no
 * edge (tree or extra) is duplicated.
 */
public class PrimMstInvariantsTest {
    private static final int SEED_COUNT = 500;
    private static final int WIDTH = 70;
    private static final int HEIGHT = 50;
    private static final int CHUNK_ROWS = 4;
    private static final int CHUNK_COLS = 4;

    @Test
    public void mstAndGraphInvariantsHoldAcrossManySeeds() {
        for (long seed = 0; seed < SEED_COUNT; seed++) {
            GeneratedWorld world = new ChunkedWorldGenerator(WIDTH, HEIGHT, CHUNK_ROWS, CHUNK_COLS, seed).generate();

            int participating = (int) world.chunks().stream().filter(c -> c.anchorRoom() != null).count();
            assertEquals("seed " + seed + ": every chunk should have an anchor room", world.chunks().size(), participating);
            assertEquals("seed " + seed + ": MST edge count must be N-1", participating - 1, world.mstEdges().size());

            assertTrue("seed " + seed + ": MST must connect every chunk", allChunksConnected(world.chunks(), world.mstEdges()));
            assertTrue("seed " + seed + ": MST must be acyclic", isForest(world.chunks(), world.mstEdges()));

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

    /** A graph is a forest iff it has no cycle, which (given |E| = |V|-1 already checked) reduces to reachability. */
    private boolean isForest(List<Chunk> chunks, List<ChunkEdge> mstEdges) {
        return mstEdges.size() == chunks.size() - 1;
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
