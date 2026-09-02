package core.persistence;

/**
 * The recorded input history for one run: {@code n<seed>s} (or {@code l<seed>s}
 * for a resumed run) followed by every subsequent command character.
 * Replaying a history means feeding everything after the {@code s} through
 * {@link core.GameEngine#step(char)} -- the exact same entry point live
 * keyboard input uses.
 */
public final class CommandHistory {
    private final StringBuilder buffer;

    private CommandHistory(String initial) {
        this.buffer = new StringBuilder(initial);
    }

    public static CommandHistory startingWithSeed(long seed) {
        return new CommandHistory("n" + seed + "s");
    }

    public static CommandHistory fromSaved(String raw) {
        return new CommandHistory(raw);
    }

    public void append(char command) {
        buffer.append(command);
    }

    /** Truncates the history to end right after the given index (inclusive), used when a checkpoint is restored. */
    public void truncateAfter(int index) {
        buffer.setLength(index + 1);
    }

    public static long extractSeed(String history) {
        StringBuilder sb = new StringBuilder();
        for (char c : history.toCharArray()) {
            if (Character.isDigit(c)) {
                sb.append(c);
            } else if (c == 's') {
                break;
            }
        }
        return Long.parseLong(sb.toString());
    }

    public static int commandsStartIndex(String history) {
        return history.indexOf('s') + 1;
    }

    @Override
    public String toString() {
        return buffer.toString();
    }
}
