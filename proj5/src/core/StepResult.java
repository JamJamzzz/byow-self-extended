package core;

/** Outcome of one {@link GameEngine#step(char)} call. */
public final class StepResult {
    private final boolean moved;
    private final String hudMessage;
    private final boolean died;
    private final boolean victorious;
    private final Position attackedPosition;

    private StepResult(boolean moved, String hudMessage, boolean died, boolean victorious, Position attackedPosition) {
        this.moved = moved;
        this.hudMessage = hudMessage;
        this.died = died;
        this.victorious = victorious;
        this.attackedPosition = attackedPosition;
    }

    static StepResult idle() {
        return new StepResult(false, null, false, false, null);
    }

    static StepResult of(boolean moved, String hudMessage, boolean died, boolean victorious, Position attackedPosition) {
        return new StepResult(moved, hudMessage, died, victorious, attackedPosition);
    }

    public boolean moved() {
        return moved;
    }

    public String hudMessage() {
        return hudMessage;
    }

    public boolean died() {
        return died;
    }

    public boolean victorious() {
        return victorious;
    }

    public boolean isTerminal() {
        return died || victorious;
    }

    /** Non-null only on an attack step: the tile that was struck, for flash rendering. */
    public Position attackedPosition() {
        return attackedPosition;
    }
}
