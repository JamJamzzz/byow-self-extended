package core;

import tileengine.TETile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A defensively-copied snapshot of every piece of logical execution state
 * that can affect future gameplay: not just the visible tile grid, but the
 * player's full state (position/health/money/standingOn/direction/
 * invincibility -- the avatar tile itself is derived, so it is not stored),
 * the movement-scheduling counter that decides when enemies/traps take
 * their next turn, and the exact runtime RNG state that will drive their
 * next moves. Restoring a checkpoint and replaying the same command suffix
 * must reproduce the same future, not just look the same immediately after
 * restore -- omitting any of these would let that suffix diverge.
 */
final class Checkpoint {
    private final TETile[][] grid;
    private final List<Position> enemies;
    private final List<Position> traps;
    private final Map<Position, Interactable> interactables;
    private final Position playerPosition;
    private final int playerHealth;
    private final int playerMoney;
    private final TETile playerStandingOn;
    private final Direction playerDirection;
    private final boolean playerInvincible;
    private final int moveCount;
    private final long rngState;

    Checkpoint(TETile[][] grid, List<Position> enemies, List<Position> traps,
               Map<Position, Interactable> interactables, Position playerPosition,
               int playerHealth, int playerMoney, TETile playerStandingOn,
               Direction playerDirection, boolean playerInvincible, int moveCount, long rngState) {
        this.grid = TETile.copyOf(grid);
        this.enemies = copyPositions(enemies);
        this.traps = copyPositions(traps);
        this.interactables = new HashMap<>(interactables);
        this.playerPosition = new Position(playerPosition.x, playerPosition.y);
        this.playerHealth = playerHealth;
        this.playerMoney = playerMoney;
        this.playerStandingOn = playerStandingOn;
        this.playerDirection = playerDirection;
        this.playerInvincible = playerInvincible;
        this.moveCount = moveCount;
        this.rngState = rngState;
    }

    private static List<Position> copyPositions(List<Position> positions) {
        List<Position> copy = new ArrayList<>();
        for (Position p : positions) {
            copy.add(new Position(p.x, p.y));
        }
        return copy;
    }

    TETile[][] grid() {
        return TETile.copyOf(grid);
    }

    List<Position> enemies() {
        return copyPositions(enemies);
    }

    List<Position> traps() {
        return copyPositions(traps);
    }

    Map<Position, Interactable> interactables() {
        return new HashMap<>(interactables);
    }

    Position playerPosition() {
        return new Position(playerPosition.x, playerPosition.y);
    }

    int playerHealth() {
        return playerHealth;
    }

    int playerMoney() {
        return playerMoney;
    }

    TETile playerStandingOn() {
        return playerStandingOn;
    }

    Direction playerDirection() {
        return playerDirection;
    }

    boolean playerInvincible() {
        return playerInvincible;
    }

    int moveCount() {
        return moveCount;
    }

    long rngState() {
        return rngState;
    }
}
