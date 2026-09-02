package core.gen;

import org.junit.Test;
import verification.VerificationConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * Independently verifies that the production {@link PrimMinimumSpanningTree}
 * actually produces a *minimum*-cost spanning tree, not merely a connected
 * acyclic one (that weaker property is covered by {@link PrimMstInvariantsTest}).
 *
 * <p>This test does not call, extend, or otherwise reuse the production
 * Prim implementation's edge-selection logic. It reconstructs the same
 * candidate edge set (chunk graph + Manhattan anchor-room costs are pure
 * functions of the already-generated chunk layout, so re-deriving them here
 * is not "peeking" at Prim's answer) and independently runs Kruskal's
 * algorithm over a from-scratch {@link DisjointSetUnion}. Two different MST
 * algorithms over the same graph are guaranteed by MST theory to produce
 * the same *total* weight even when they pick different edges among ties,
 * so the assertion compares summed edge cost, not edge-by-edge identity.
 */
public class PrimIsMinimumSpanningTreeTest {
    private static final int SEED_COUNT = VerificationConfig.PRIM_VS_KRUSKAL_SEED_COUNT;
    private static final int WIDTH = VerificationConfig.WORLD_WIDTH;
    private static final int HEIGHT = VerificationConfig.WORLD_HEIGHT;
    private static final int CHUNK_ROWS = VerificationConfig.CHUNK_ROWS;
    private static final int CHUNK_COLS = VerificationConfig.CHUNK_COLS;

    @Test
    public void primMstCostMatchesIndependentKruskalMstCost() {
        for (long seed = 0; seed < SEED_COUNT; seed++) {
            GeneratedWorld world = new ChunkedWorldGenerator(WIDTH, HEIGHT, CHUNK_ROWS, CHUNK_COLS, seed).generate();

            // Same chunk layout (anchor rooms are already fixed at this point),
            // so this reconstructs the identical candidate-edge set Prim saw.
            // The tie-break seed only affects ordering among equal-cost edges,
            // never total MST weight, so any seed is valid here.
            List<ChunkEdge> candidates = PrimMinimumSpanningTree.candidateEdges(world.chunks(), new Random(seed));

            long primCost = totalCost(world.mstEdges());
            KruskalResult kruskal = kruskalMst(world.chunks(), candidates);

            assertEquals("seed " + seed + ": Kruskal oracle must also span all chunks",
                    world.chunks().size() - 1, kruskal.edgeCount);
            assertEquals("seed " + seed + ": Prim MST total cost must equal independent Kruskal MST total cost",
                    kruskal.totalCost, primCost);
        }
    }

    private long totalCost(List<ChunkEdge> edges) {
        long sum = 0;
        for (ChunkEdge edge : edges) {
            sum += edge.cost();
        }
        return sum;
    }

    private static final class KruskalResult {
        final long totalCost;
        final int edgeCount;

        KruskalResult(long totalCost, int edgeCount) {
            this.totalCost = totalCost;
            this.edgeCount = edgeCount;
        }
    }

    /** Textbook Kruskal: sort edges by cost, greedily add any edge that joins two different components. */
    private KruskalResult kruskalMst(List<Chunk> chunks, List<ChunkEdge> candidates) {
        List<ChunkEdge> sorted = new ArrayList<>(candidates);
        sorted.sort(Comparator.comparingLong(ChunkEdge::cost));

        DisjointSetUnion dsu = new DisjointSetUnion(chunks);
        long totalCost = 0;
        int edgeCount = 0;

        for (ChunkEdge edge : sorted) {
            if (dsu.union(edge.a(), edge.b())) {
                totalCost += edge.cost();
                edgeCount++;
            }
        }
        return new KruskalResult(totalCost, edgeCount);
    }
}
