package core;

import tileengine.TETile;
import tileengine.Tileset;

/**
 * The single authoritative state-transition path for the game. Live
 * keyboard input, replayed history, and BFS click-to-move all end up
 * calling {@link #step(char)} with the same command characters, so there
 * is exactly one place that knows how movement, combat, and interactions
 * mutate the world.
 */
public final class GameEngine {
    private static final int ENEMY_MOVE_INTERVAL = 2;
    private static final int TRAP_MOVE_INTERVAL = 5;

    private final World world;
    private final Player player;
    private int moveCount = 0;

    public GameEngine(World world) {
        this.world = world;
        this.player = world.getPlayer();
    }

    /** Converts a BFS path step into the same command char keyboard input would produce. */
    public static char directionCommand(Position from, Position to) {
        if (to.x == from.x + 1 && to.y == from.y) {
            return 'd';
        }
        if (to.x == from.x - 1 && to.y == from.y) {
            return 'a';
        }
        if (to.x == from.x && to.y == from.y + 1) {
            return 'w';
        }
        if (to.x == from.x && to.y == from.y - 1) {
            return 's';
        }
        throw new IllegalArgumentException("Non-adjacent step from " + from + " to " + to);
    }

    public StepResult step(char rawKey) {
        char key = Character.toLowerCase(rawKey);
        switch (key) {
            case 'w': return move(Direction.UP);
            case 'a': return move(Direction.LEFT);
            case 's': return move(Direction.DOWN);
            case 'd': return move(Direction.RIGHT);
            case 'j': return attack();
            case 'e': return toggleInvincible();
            default: return StepResult.idle();
        }
    }

    /** Restores the last checkpoint and resets the move counter, mirroring what a fresh run/replay start looks like. */
    public boolean restoreFromCheckpoint() {
        boolean restored = world.restoreCheckpoint();
        if (restored) {
            moveCount = 0;
        }
        return restored;
    }

    private StepResult move(Direction dir) {
        Position pos = player.getPosition();
        Position next = new Position(pos.x + dir.dx, pos.y + dir.dy);
        player.setDirection(dir);

        if (!world.isWalkable(next)) {
            return StepResult.idle();
        }

        world.saveCheckpointIfLeavingRoom(pos, next);

        TETile[][] grid = world.getGrid();
        TETile nextTile = grid[next.x][next.y];
        TETile restoreOldTile = player.getStandingOn();

        String hudMessage = null;
        TETile standingOnAtNext = Tileset.FLOOR;

        if (nextTile == Tileset.ENEMY) {
            player.deductHealth(1);
            world.removeEnemyAt(next);
        } else if (nextTile == Tileset.TRAP) {
            player.deductHealth(1);
            standingOnAtNext = Tileset.TRAP;
        } else {
            Interactable item = world.getInteractableAt(next);
            if (item != null) {
                item.interact(player);
                world.removeInteractableAt(next);
                hudMessage = item.hudMessage();
            }
        }

        grid[pos.x][pos.y] = restoreOldTile;
        grid[next.x][next.y] = player.getAvator();
        player.setPosition(next);
        player.setStandingOn(standingOnAtNext);

        moveCount++;
        if (moveCount % ENEMY_MOVE_INTERVAL == 0) {
            world.moveEnemies();
        }
        if (moveCount % TRAP_MOVE_INTERVAL == 0) {
            world.moveTraps();
        }

        return terminalAwareResult(true, hudMessage, null);
    }

    private StepResult attack() {
        Position target = player.attack();
        if (target == null || !world.inBounds(target)) {
            return StepResult.idle();
        }

        TETile[][] grid = world.getGrid();
        TETile original = grid[target.x][target.y];
        if (original != Tileset.FLOOR && original != Tileset.ENEMY && original != Tileset.TRAP) {
            return StepResult.idle();
        }

        String message = null;
        if (original == Tileset.ENEMY) {
            world.removeEnemyAt(target);
            grid[target.x][target.y] = Tileset.FLOOR;
            message = "Smashed an enemy";
        } else if (original == Tileset.TRAP) {
            player.deductHealth(1);
            message = "Ouch";
        }

        return terminalAwareResult(false, message, target);
    }

    private StepResult toggleInvincible() {
        player.toggleInvincible();
        Position p = player.getPosition();
        world.getGrid()[p.x][p.y] = player.getAvator();
        String message = player.isInvincible() ? "Aurora awakened" : "Aurora faded";
        return StepResult.of(false, message, false, false, null);
    }

    private StepResult terminalAwareResult(boolean moved, String hudMessage, Position attackedPosition) {
        boolean died = player.getHealth() <= 0;
        boolean victorious = !died && world.countCoins() == 0;
        return StepResult.of(moved, hudMessage, died, victorious, attackedPosition);
    }
}
