package core;

import tileengine.TETile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A defensively-copied snapshot of everything needed to resume play
 * identically: the tile grid, enemy/trap positions, the player's stats,
 * and any interactable items that had not yet been collected.
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

    Checkpoint(TETile[][] grid, List<Position> enemies, List<Position> traps,
               Map<Position, Interactable> interactables, Position playerPosition,
               int playerHealth, int playerMoney, TETile playerStandingOn) {
        this.grid = TETile.copyOf(grid);
        this.enemies = copyPositions(enemies);
        this.traps = copyPositions(traps);
        this.interactables = new HashMap<>(interactables);
        this.playerPosition = new Position(playerPosition.x, playerPosition.y);
        this.playerHealth = playerHealth;
        this.playerMoney = playerMoney;
        this.playerStandingOn = playerStandingOn;
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
}
