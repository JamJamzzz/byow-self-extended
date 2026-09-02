package core;

import core.gen.ChunkedWorldGenerator;
import core.gen.GeneratedWorld;
import tileengine.TETile;
import tileengine.Tileset;
import utils.RandomUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Runtime game state: the rendered grid, the player, enemies/traps, item
 * interactables keyed by position, and checkpointing. World does not decide
 * *how* to generate the dungeon -- that is {@link ChunkedWorldGenerator}'s
 * job -- it only owns and mutates the state produced from it.
 */
public final class World {
    private final int width;
    private final int height;
    private final long seed;

    private final TETile[][] grid;
    private final List<Room> rooms;
    private final Room spawnRoom;
    private final Random entityRandom;

    private final Player player = new Player();
    private List<Position> enemies = new ArrayList<>();
    private List<Position> traps = new ArrayList<>();
    private final Map<Position, Interactable> interactables = new HashMap<>();
    private Checkpoint checkpoint;

    public World(int width, int height, int chunkRows, int chunkCols, long seed) {
        GeneratedWorld generated = new ChunkedWorldGenerator(width, height, chunkRows, chunkCols, seed).generate();
        this.grid = generated.grid();
        this.width = grid.length;
        this.height = grid[0].length;
        this.seed = seed;
        this.rooms = generated.rooms();
        this.spawnRoom = generated.spawnRoom();
        this.entityRandom = generated.entityRandom();
    }

    public void placePlayer() {
        int spawnX = spawnRoom.centerAtX();
        int spawnY = spawnRoom.centerAtY();
        player.setPosition(new Position(spawnX, spawnY));
        grid[spawnX][spawnY] = player.getAvator();
    }

    public void placeEnemy() {
        enemies = new ArrayList<>();
        for (Room room : rooms) {
            int enemyCount = Math.max(2, room.width() * room.height() / 45);
            for (int i = 0; i < enemyCount; i++) {
                placeEnemyInRoom(room);
            }
        }
    }

    private void placeEnemyInRoom(Room room) {
        int x = entityRandom.nextInt(room.width()) + room.x();
        int y = entityRandom.nextInt(room.height()) + room.y();
        if (grid[x][y] == Tileset.FLOOR) {
            grid[x][y] = Tileset.ENEMY;
            enemies.add(new Position(x, y));
        }
    }

    public void placeTrap() {
        traps = new ArrayList<>();
        for (Room room : rooms) {
            int trapCount = Math.max(1, room.width() * room.height() / 65);
            for (int i = 0; i < trapCount; i++) {
                placeTrapInRoom(room);
            }
        }
    }

    private void placeTrapInRoom(Room room) {
        if (room.width() <= 2 || room.height() <= 2) {
            return;
        }
        int x = entityRandom.nextInt(room.width() - 2) + room.x() + 1;
        int y = entityRandom.nextInt(room.height() - 2) + room.y() + 1;
        if (grid[x][y] == Tileset.FLOOR) {
            grid[x][y] = Tileset.TRAP;
            traps.add(new Position(x, y));
        }
    }

    public void placeHealingItems() {
        for (Room room : rooms) {
            if (room.width() <= 2 || room.height() <= 2) {
                continue;
            }
            if (RandomUtils.bernoulli(entityRandom, 0.5)) {
                int x = entityRandom.nextInt(room.width() - 2) + room.x() + 1;
                int y = entityRandom.nextInt(room.height() - 2) + room.y() + 1;
                if (grid[x][y] == Tileset.FLOOR) {
                    grid[x][y] = Tileset.HEAL;
                    interactables.put(new Position(x, y), new HealingItem(1));
                }
            }
        }
    }

    public void placeCoins() {
        for (Room room : rooms) {
            if (room.width() <= 2 || room.height() <= 2) {
                continue;
            }
            int coinCount = Math.max(1, room.width() * room.height() / 45);
            for (int i = 0; i < coinCount; i++) {
                int x = entityRandom.nextInt(room.width() - 2) + room.x() + 1;
                int y = entityRandom.nextInt(room.height() - 2) + room.y() + 1;
                if (grid[x][y] == Tileset.FLOOR) {
                    grid[x][y] = Tileset.UNLOCKED_DOOR;
                    interactables.put(new Position(x, y), new Coin(1));
                }
            }
        }
    }

    public int countCoins() {
        int count = 0;
        for (Interactable item : interactables.values()) {
            if (item instanceof Coin) {
                count++;
            }
        }
        return count;
    }

    public Interactable getInteractableAt(Position p) {
        return interactables.get(p);
    }

    public void removeInteractableAt(Position p) {
        interactables.remove(p);
        TETile tile = grid[p.x][p.y];
        if (tile == Tileset.HEAL || tile == Tileset.UNLOCKED_DOOR) {
            grid[p.x][p.y] = Tileset.FLOOR;
        }
    }

    public Room roomAt(Position p) {
        for (Room room : rooms) {
            if (room.contains(p)) {
                return room;
            }
        }
        return null;
    }

    public boolean inBounds(Position p) {
        return p.x >= 0 && p.x < width && p.y >= 0 && p.y < height;
    }

    public boolean isWalkable(Position p) {
        if (!inBounds(p)) {
            return false;
        }
        TETile tile = grid[p.x][p.y];
        return tile != Tileset.NOTHING && tile != Tileset.WALL;
    }

    public void moveTraps() {
        List<Position> nextTraps = new ArrayList<>();
        for (Position trap : traps) {
            if (trap.equals(player.getPosition())) {
                // Player is standing on this trap's tile (which now renders as
                // the avatar); leave it in place this turn instead of
                // clobbering the avatar's tile with FLOOR/TRAP.
                nextTraps.add(trap);
                continue;
            }

            Position next = randomTrapMove(trap);
            if (next.equals(player.getPosition())) {
                player.deductHealth(1);
                nextTraps.add(trap);
                continue;
            }

            grid[trap.x][trap.y] = Tileset.FLOOR;
            grid[next.x][next.y] = Tileset.TRAP;
            nextTraps.add(next);
        }
        traps = nextTraps;
    }

    private Position randomTrapMove(Position trap) {
        List<Position> options = new ArrayList<>();
        Position[] candidates = {
                new Position(trap.x + 1, trap.y),
                new Position(trap.x - 1, trap.y),
                new Position(trap.x, trap.y + 1),
                new Position(trap.x, trap.y - 1)
        };
        for (Position p : candidates) {
            if (inBounds(p) && grid[p.x][p.y] == Tileset.FLOOR) {
                options.add(p);
            }
        }
        if (options.isEmpty()) {
            return trap;
        }
        return options.get(entityRandom.nextInt(options.size()));
    }

    public void moveEnemies() {
        List<Position> nextEnemies = new ArrayList<>();
        for (Position enemy : enemies) {
            Room enemyRoom = roomAt(enemy);
            Room playerRoom = roomAt(player.getPosition());

            Position next = (enemyRoom != null && enemyRoom == playerRoom)
                    ? chasePlayer(enemy, enemyRoom)
                    : randomWalk(enemy, enemyRoom);

            if (next.equals(player.getPosition())) {
                player.deductHealth(1);
                nextEnemies.add(enemy);
                continue;
            }

            grid[enemy.x][enemy.y] = Tileset.FLOOR;
            grid[next.x][next.y] = Tileset.ENEMY;
            nextEnemies.add(next);
        }
        enemies = nextEnemies;
    }

    private Position randomWalk(Position enemy, Room room) {
        List<Position> options = enemyNeighbors(enemy, room);
        if (options.isEmpty()) {
            return enemy;
        }
        return options.get(entityRandom.nextInt(options.size()));
    }

    private Position chasePlayer(Position enemy, Room room) {
        List<Position> options = enemyNeighbors(enemy, room);
        Position best = enemy;
        int bestDist = manhattan(enemy, player.getPosition());
        for (Position option : options) {
            int d = manhattan(option, player.getPosition());
            if (d < bestDist) {
                best = option;
                bestDist = d;
            }
        }
        return best;
    }

    private int manhattan(Position a, Position b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    }

    private List<Position> enemyNeighbors(Position p, Room room) {
        List<Position> result = new ArrayList<>();
        Position[] candidates = {
                new Position(p.x + 1, p.y),
                new Position(p.x - 1, p.y),
                new Position(p.x, p.y + 1),
                new Position(p.x, p.y - 1)
        };
        for (Position c : candidates) {
            if (canEnemyMoveTo(c, room) || c.equals(player.getPosition())) {
                result.add(c);
            }
        }
        return result;
    }

    private boolean canEnemyMoveTo(Position p, Room room) {
        return room != null && room.contains(p) && grid[p.x][p.y] == Tileset.FLOOR;
    }

    public void removeEnemyAt(Position p) {
        enemies.remove(p);
    }

    public TETile[][] getGrid() {
        return grid;
    }

    public Player getPlayer() {
        return player;
    }

    public long getSeed() {
        return seed;
    }

    public List<Room> getRooms() {
        return rooms;
    }

    public void saveCheckpoint() {
        checkpoint = new Checkpoint(grid, enemies, traps, interactables, player.getPosition(),
                player.getHealth(), player.getMoney(), player.getStandingOn());
    }

    public void saveCheckpointIfLeavingRoom(Position from, Position to) {
        Room fromRoom = roomAt(from);
        Room toRoom = roomAt(to);
        if (fromRoom != null && fromRoom != toRoom) {
            saveCheckpoint();
        }
    }

    public boolean restoreCheckpoint() {
        if (checkpoint == null) {
            return false;
        }

        TETile[][] restoredGrid = checkpoint.grid();
        for (int x = 0; x < grid.length; x++) {
            System.arraycopy(restoredGrid[x], 0, grid[x], 0, grid[x].length);
        }

        enemies = checkpoint.enemies();
        traps = checkpoint.traps();
        interactables.clear();
        interactables.putAll(checkpoint.interactables());

        player.setPosition(checkpoint.playerPosition());
        player.setHealth(checkpoint.playerHealth());
        player.setMoney(checkpoint.playerMoney());
        player.setStandingOn(checkpoint.playerStandingOn());

        return true;
    }
}
