package core.gen;

import core.Position;
import core.Room;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One spatial region of the chunk-partitioned world. A chunk owns the rooms
 * generated inside its own bounds and knows which other chunks border it;
 * the chunk graph used by the world generator is built entirely out of
 * these neighbor relationships.
 */
public final class Chunk {
    private final int id;
    private final int row;
    private final int col;
    private final int x;
    private final int y;
    private final int w;
    private final int h;

    private final List<Room> rooms = new ArrayList<>();
    private final List<Chunk> neighbors = new ArrayList<>();
    private Room anchorRoom;

    public Chunk(int id, int row, int col, int x, int y, int w, int h) {
        this.id = id;
        this.row = row;
        this.col = col;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public int id() {
        return id;
    }

    public int row() {
        return row;
    }

    public int col() {
        return col;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int width() {
        return w;
    }

    public int height() {
        return h;
    }

    public boolean contains(Position p) {
        return p.x >= x && p.x < x + w && p.y >= y && p.y < y + h;
    }

    /** The first room placed in this chunk. Every active chunk has one. */
    public Room anchorRoom() {
        return anchorRoom;
    }

    public List<Room> rooms() {
        return Collections.unmodifiableList(rooms);
    }

    public void addRoom(Room room) {
        if (rooms.isEmpty()) {
            anchorRoom = room;
        }
        rooms.add(room);
    }

    public List<Chunk> neighbors() {
        return Collections.unmodifiableList(neighbors);
    }

    public void addNeighbor(Chunk neighbor) {
        neighbors.add(neighbor);
    }

    @Override
    public String toString() {
        return "Chunk#" + id + "[r=" + row + ",c=" + col + "]";
    }
}
