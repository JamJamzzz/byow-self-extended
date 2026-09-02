package core.gen;

import java.util.Random;

/**
 * Splits one master seed into independent, stage-specific random streams.
 * Each stage of generation (room placement, chunk-graph tie-breaking,
 * corridor carving, entity placement) draws only from its own {@link Random}
 * instance, so adding or removing random calls in one stage never shifts
 * the sequence seen by another stage.
 */
public final class WorldRng {
    private final Random rooms;
    private final Random graph;
    private final Random corridors;
    private final Random entities;

    public WorldRng(long seed) {
        // Stage seeds are drawn in a fixed order from one master stream so the
        // whole split is reproducible from just the world seed.
        Random master = new Random(seed);
        this.rooms = new Random(master.nextLong());
        this.graph = new Random(master.nextLong());
        this.corridors = new Random(master.nextLong());
        this.entities = new Random(master.nextLong());
    }

    public Random rooms() {
        return rooms;
    }

    public Random graph() {
        return graph;
    }

    public Random corridors() {
        return corridors;
    }

    public Random entities() {
        return entities;
    }
}
