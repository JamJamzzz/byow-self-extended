package core;

/** An immutable axis-aligned rectangular room on the world grid. */
public final class Room {
    private final int x;
    private final int y;
    private final int w;
    private final int h;

    public Room(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
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

    public int centerAtX() {
        return x + w / 2;
    }

    public int centerAtY() {
        return y + h / 2;
    }

    public Position center() {
        return new Position(centerAtX(), centerAtY());
    }

    public boolean contains(Position p) {
        return p.x >= x && p.x < x + w && p.y >= y && p.y < y + h;
    }

    /** True if this room's footprint (padded by one tile for walls) would overlap other. */
    public boolean overlapsWithMargin(Room other, int margin) {
        boolean xOverlap = x < other.x + other.w + margin && x + w + margin > other.x;
        boolean yOverlap = y < other.y + other.h + margin && y + h + margin > other.y;
        return xOverlap && yOverlap;
    }
}
