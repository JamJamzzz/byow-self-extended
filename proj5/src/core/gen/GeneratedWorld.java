package core.gen;

import core.Room;
import tileengine.TETile;

import java.util.Collections;
import java.util.List;

/**
 * The output of one {@link ChunkedWorldGenerator#generate()} call: a
 * rendered grid plus the chunk/graph structure behind it.
 *
 * <p>This is a generation <em>result</em>, not a deeply-immutable value: the
 * room/chunk/edge lists are wrapped unmodifiable, but {@link #grid()}
 * intentionally returns the live tile array by reference, because {@code
 * World} owns that array afterward and mutates it directly as gameplay
 * happens (movement, combat, checkpoint restore). Defensively copying a
 * ~70x50 grid on every access would only hide that ownership handoff, not
 * make it real -- so the accessor is documented instead of pretending
 * otherwise.
 */
public final class GeneratedWorld {
    private final TETile[][] grid;
    private final List<Room> rooms;
    private final List<Chunk> chunks;
    private final List<ChunkEdge> mstEdges;
    private final List<ChunkEdge> extraEdges;
    private final Room spawnRoom;
    private final long seed;
    private final long entitySeed;

    GeneratedWorld(TETile[][] grid, List<Room> rooms, List<Chunk> chunks, List<ChunkEdge> mstEdges,
                   List<ChunkEdge> extraEdges, Room spawnRoom, long seed, long entitySeed) {
        this.grid = grid;
        this.rooms = Collections.unmodifiableList(rooms);
        this.chunks = Collections.unmodifiableList(chunks);
        this.mstEdges = Collections.unmodifiableList(mstEdges);
        this.extraEdges = Collections.unmodifiableList(extraEdges);
        this.spawnRoom = spawnRoom;
        this.seed = seed;
        this.entitySeed = entitySeed;
    }

    public TETile[][] grid() {
        return grid;
    }

    public List<Room> rooms() {
        return rooms;
    }

    public List<Chunk> chunks() {
        return chunks;
    }

    public List<ChunkEdge> mstEdges() {
        return mstEdges;
    }

    public List<ChunkEdge> extraEdges() {
        return extraEdges;
    }

    public Room spawnRoom() {
        return spawnRoom;
    }

    public long seed() {
        return seed;
    }

    /** Seed for the runtime entity RNG (placement + ongoing enemy/trap movement), derived from the world seed. */
    public long entitySeed() {
        return entitySeed;
    }
}
