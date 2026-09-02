package core;

import core.persistence.CommandHistory;
import core.persistence.SaveGameRepository;
import edu.princeton.cs.algs4.StdDraw;
import tileengine.TERenderer;
import tileengine.TETile;
import ui.Screens;

import java.util.List;

/**
 * Application entry point. Main only wires together World, GameEngine,
 * Screens (rendering) and the save repository, and drives the input loop --
 * all actual game-state logic lives in {@link GameEngine}.
 */
public final class Main {
    private static final int WIDTH = 70;
    private static final int HEIGHT = 50;
    private static final int CHUNK_ROWS = 4;
    private static final int CHUNK_COLS = 4;
    private static final int HUD_HEIGHT = 3;
    private static final String SAVE_FILE = "save.txt";

    private static final SaveGameRepository saves = new SaveGameRepository(SAVE_FILE);
    private static final Screens screens = new Screens(WIDTH, HEIGHT, HUD_HEIGHT);

    public static void main(String[] args) {
        showMenu();
    }

    private static void showMenu() {
        screens.initializeCanvas();
        screens.drawMainMenu();

        while (true) {
            if (StdDraw.hasNextKeyTyped()) {
                char key = Character.toLowerCase(StdDraw.nextKeyTyped());

                if (key == 'n') {
                    long seed = readSeed();
                    startNewGame(seed);
                    return;
                } else if (key == 'l') {
                    String history = saves.load();
                    if (history.isEmpty()) {
                        screens.drawMainMenu();
                        continue;
                    }
                    replayAndContinue(history);
                    return;
                } else if (key == 'q') {
                    System.exit(0);
                }
            }
        }
    }

    private static long readSeed() {
        StringBuilder seedStr = new StringBuilder();
        screens.drawSeedPrompt(seedStr.toString());

        while (true) {
            if (StdDraw.hasNextKeyTyped()) {
                char ch = StdDraw.nextKeyTyped();

                if (Character.isDigit(ch)) {
                    if (seedStr.length() < 20) {
                        seedStr.append(ch);
                    }
                    screens.drawSeedPrompt(seedStr.toString());
                } else if ((ch == '\b' || ch == 127) && seedStr.length() > 0) {
                    seedStr.deleteCharAt(seedStr.length() - 1);
                    screens.drawSeedPrompt(seedStr.toString());
                } else if ((ch == 's' || ch == 'S') && seedStr.length() > 0) {
                    break;
                }
            }
        }
        return Long.parseLong(seedStr.toString());
    }

    private static World buildWorld(long seed) {
        World world = new World(WIDTH, HEIGHT, CHUNK_ROWS, CHUNK_COLS, seed);
        world.placePlayer();
        world.placeEnemy();
        world.placeTrap();
        world.placeHealingItems();
        world.placeCoins();
        world.saveCheckpoint();
        return world;
    }

    private static void startNewGame(long seed) {
        World world = buildWorld(seed);
        GameEngine engine = new GameEngine(world);
        CommandHistory history = CommandHistory.startingWithSeed(seed);

        TERenderer ter = new TERenderer();
        ter.initialize(WIDTH, HEIGHT + HUD_HEIGHT);

        runGameLoop(world, engine, ter, history);
    }

    private static void replayAndContinue(String rawHistory) {
        long seed = CommandHistory.extractSeed(rawHistory);
        World world = buildWorld(seed);
        GameEngine engine = new GameEngine(world);

        TERenderer ter = new TERenderer();
        ter.initialize(WIDTH, HEIGHT + HUD_HEIGHT);

        int start = CommandHistory.commandsStartIndex(rawHistory);
        for (int i = start; i < rawHistory.length(); i++) {
            char key = rawHistory.charAt(i);
            StepResult result = engine.step(key);
            flashAttackIfAny(world, ter, result);

            if (result.isTerminal()) {
                engine.restoreFromCheckpoint();
            }
        }

        runGameLoop(world, engine, ter, CommandHistory.fromSaved(rawHistory));
    }

    private static void runGameLoop(World world, GameEngine engine, TERenderer ter, CommandHistory history) {
        Player player = world.getPlayer();
        TETile[][] tiles = world.getGrid();

        List<Position> path = null;
        Position pathTarget = null;
        boolean mousePressed = false;

        while (true) {
            if (StdDraw.hasNextKeyTyped()) {
                char key = Character.toLowerCase(StdDraw.nextKeyTyped());
                history.append(key);

                StepResult result = engine.step(key);
                flashAttackIfAny(world, ter, result);
                if (result.hudMessage() != null) {
                    screens.showHudMessage(result.hudMessage());
                }
            }

            boolean mousePressedNow = StdDraw.isMousePressed();
            if (mousePressedNow && !mousePressed) {
                int x = (int) StdDraw.mouseX();
                int y = (int) StdDraw.mouseY();
                Position mouse = new Position(x, y);

                if (!world.isWalkable(mouse)) {
                    path = null;
                    pathTarget = null;
                } else if (pathTarget != null && mouse.equals(pathTarget)) {
                    ter.drawTiles(world.getGrid());
                    StdDraw.show();
                    followPath(world, engine, path, ter, history);
                    path = null;
                    pathTarget = null;
                } else {
                    List<Position> newPath = BfsPathfinder.findPath(world, player.getPosition(), mouse);
                    if (newPath != null && !newPath.isEmpty()) {
                        path = newPath;
                        pathTarget = mouse;
                    }
                }
            }
            mousePressed = mousePressedNow;

            ter.drawTiles(tiles);
            screens.drawHud(tiles, player);
            drawPathOverlay(path);

            if (player.getHealth() <= 0) {
                if (!handleTerminalScreen(world, engine, ter, history, screens.showGameOver())) {
                    return;
                }
                path = null;
                pathTarget = null;
                continue;
            }

            if (world.countCoins() == 0) {
                if (!handleTerminalScreen(world, engine, ter, history, screens.showVictory())) {
                    return;
                }
                path = null;
                pathTarget = null;
                continue;
            }

            StdDraw.show();
        }
    }

    /** @return true to keep playing in this loop, false if control should return to the caller. */
    private static boolean handleTerminalScreen(World world, GameEngine engine, TERenderer ter,
                                                 CommandHistory history, char choice) {
        if (choice == 'r') {
            if (engine.restoreFromCheckpoint()) {
                ter.initialize(WIDTH, HEIGHT + HUD_HEIGHT);
                saves.save(history.toString());
                screens.showHudMessage("Returned to checkpoint");
                return true;
            }
            return true;
        }
        if (choice == 'm') {
            saves.delete();
            showMenu();
            return false;
        }
        saves.save(history.toString());
        System.exit(0);
        return false;
    }

    private static void followPath(World world, GameEngine engine, List<Position> path, TERenderer ter,
                                    CommandHistory history) {
        if (path == null) {
            return;
        }
        Player player = world.getPlayer();

        for (Position nextStep : path) {
            Position from = player.getPosition();
            char command = GameEngine.directionCommand(from, nextStep);
            history.append(command);

            StepResult result = engine.step(command);
            if (result.hudMessage() != null) {
                screens.showHudMessage(result.hudMessage());
            }

            ter.drawTiles(world.getGrid());
            screens.drawHud(world.getGrid(), player);
            StdDraw.show();

            if (result.isTerminal()) {
                return;
            }
        }
    }

    private static void flashAttackIfAny(World world, TERenderer ter, StepResult result) {
        Position target = result.attackedPosition();
        if (target == null) {
            return;
        }
        TETile[][] tiles = world.getGrid();
        TETile original = tiles[target.x][target.y];

        tiles[target.x][target.y] = world.getPlayer().getAttackTile();
        ter.drawTiles(tiles);
        StdDraw.show();
        StdDraw.pause(80);
        tiles[target.x][target.y] = original;
    }

    private static void drawPathOverlay(List<Position> path) {
        if (path == null) {
            return;
        }
        for (Position p : path) {
            tileengine.Tileset.GRASS.draw(p.x, p.y);
        }
    }
}
