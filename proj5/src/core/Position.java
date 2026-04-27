package core;

public class Position {
    int x;
    int y;

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof Position)) {
            return false;
        }

        Position posi = (Position) other;
        return this.x == posi.x && this.y == posi.y;
    }

    @Override
    public int hashCode() {
        return 31 * x + y;
    }
}
