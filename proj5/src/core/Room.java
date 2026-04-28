package core;

public class Room {
    int x;
    int y;
    int w;
    int h;

    int centerAtX() {
        return x + w / 2;
    }

    int centerAtY() {
        return y + h / 2;
    }

    boolean contains(Position p) {
        return p.x >= x && p.x < x + w && p.y >= y && p.y < y + h;
    }
}