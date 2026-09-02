package core.gen;

import core.Room;

/** Two rooms that should be joined by a carved corridor. */
public final class RoomConnection {
    private final Room a;
    private final Room b;

    public RoomConnection(Room a, Room b) {
        this.a = a;
        this.b = b;
    }

    public Room a() {
        return a;
    }

    public Room b() {
        return b;
    }
}
