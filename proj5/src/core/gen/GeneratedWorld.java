package core.gen;

import core.Room;
import tileengine.TETile;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Immutable output of {@link ChunkedWorldGenerator}: a rendered grid plus the structure behind it. */
public final class GeneratedWorld {
    private final TETile[][] grid;
    private final List<Room> rooms;
    private final List<Chunk> chunks;
    private final List<ChunkEdge> mstEdges;
    private final List<ChunkEdge> extraEdges;
    private final Room spawnRoom;
    private final long seed;
    private final Random entityRandom;

    GeneratedWorld(TETile[][] grid, List<Room> rooms, List<Chunk> chunks, List<ChunkEdge> mstEdges,
                   List<ChunkEdge> extraEdges, Room spawnRoom, long seed, Random entityRandom) {
        this.grid = grid;
        this.rooms = Collections.unmodifiableList(rooms);
        this.chunks = Collections.unmodifiableList(chunks);
        this.mstEdges = Collections.unmodifiableList(mstEdges);
        this.extraEdges = Collections.unmodifiableList(extraEdges);
        this.spawnRoom = spawnRoom;
        this.seed = seed;
        this.entityRandom = entityRandom;
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

    /** The entity-placement RNG stream, already advanced past every generation-stage draw. */
    public Random entityRandom() {
        return entityRandom;
    }
}
