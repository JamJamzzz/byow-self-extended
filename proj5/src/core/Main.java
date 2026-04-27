package core;

import edu.princeton.cs.algs4.In;
import edu.princeton.cs.algs4.Out;
import edu.princeton.cs.algs4.StdDraw;
import tileengine.TERenderer;
import tileengine.TETile;
import tileengine.Tileset;

import java.awt.*;
import java.util.*;
import java.io.File;
import java.util.List;

public class Main {
    /**
     * World Parameters
     **/
    private static final int WIDTH = 80;
    private static final int HEIGHT = 60;
    private static final int CHUNK_ROWS = 4;
    private static final int CHUNK_COLS = 4;

    private static final int HUD_HEIGHT = 2;

    /**
     * World Record
     **/
    private static StringBuilder inputHistory;

    public static void main(String[] args) {
        showMenu();
    }

    private static void showMenu() {
        initialize();
        draw();

        while (true) {

            if (StdDraw.hasNextKeyTyped()) {
                char key = Character.toLowerCase(StdDraw.nextKeyTyped());

                if (key == 'n') {
                    IO.print("Start New Game");
                    long seed = getSeed();
                    startGame(seed);
                    break;
                } else if (key == 'l') {
                    IO.print("Load Game");
                    String history = loadGame();
                    if (history == null || history.isEmpty()) {
                        draw();
                        continue;
                    }
                    replayGame(history);
                    break;
                } else if (key == 'q') {
                    IO.print("Quit Game");
                    System.exit(0);
                }
            }
        }
    }

    private static void initialize() {
        StdDraw.setCanvasSize(800, 600);
        StdDraw.setXscale(0, 800);
        StdDraw.setYscale(0, 600);
        StdDraw.clear();
        //Enable the buffer to carry the double
        StdDraw.enableDoubleBuffering();
    }

    private static void draw() {
        StdDraw.clear(Color.black);

        StdDraw.setPenColor(StdDraw.WHITE);

        StdDraw.setFont(new Font("Monaco", Font.BOLD, 40));
        StdDraw.text(400, 450, "CS61B: BYOW");

        StdDraw.setFont(new Font("Monaco", Font.PLAIN, 20));
        StdDraw.text(400, 300, "(N) New Game");
        StdDraw.setFont(new Font("Monaco", Font.PLAIN, 20));
        StdDraw.text(400, 250, "(L) Load Game");
        StdDraw.setFont(new Font("Monaco", Font.PLAIN, 20));
        StdDraw.text(400, 200, "(Q) Quit Game");

        StdDraw.show();
    }

    private static long getSeed() {
        StringBuilder seedStr = new StringBuilder();

        drawSeed(seedStr.toString());
        while (true) {
            if (StdDraw.hasNextKeyTyped()) {
                char ch = StdDraw.nextKeyTyped();

                if (Character.isDigit(ch)) {
                    if (seedStr.length() < 18) {
                        seedStr.append(ch);
                    }
                    drawSeed(seedStr.toString());
                } else if (ch == 's' || ch == 'S') {
                    if (seedStr.length() == 0) {
                        continue;
                    }
                    break;
                }
            }
        }

        return Long.parseLong(seedStr.toString());
    }

    private static void drawSeed(String seed) {
        StdDraw.clear(Color.black);

        StdDraw.setPenColor(Color.WHITE);
        StdDraw.setFont(new Font("Monaco", Font.BOLD, 40));
        StdDraw.text(400, 450, "CS61B:  BYOW");

        StdDraw.setFont(new Font("Monaco", Font.PLAIN, 20));
        StdDraw.text(400, 350, "Enter seed followed by S");

        StdDraw.setPenColor(Color.YELLOW);
        StdDraw.setFont(new Font("Monaco", Font.BOLD, 25));
        StdDraw.text(400, 250, seed);

        StdDraw.show();
    }

    private static void startGame(long seed) {
        //Generate the World
        World world = new World(
                WIDTH,
                HEIGHT,
                CHUNK_ROWS,
                CHUNK_COLS,
                seed
        );

        world.placePlayer();

        //Initializing the history with seed
        inputHistory = new StringBuilder();
        inputHistory.append("n").append(seed).append("s");

        //Initializing the render
        TERenderer ter = new TERenderer();
        ter.initialize(WIDTH, HEIGHT + HUD_HEIGHT);

        gameLoop(world, ter);
    }

    private static void saveGame(String history) {
        Out out = new Out("save.txt");
        out.println(history);
        out.close();
    }

    private static String loadGame() {
        String fileName = "save.txt";
        File file = new File(fileName);

        if (file.exists()) {
            In in = new In(file);
            if (in.hasNextLine()) {
                return in.readLine();
            }
        }

        //No save history
        return "";
    }

    private static void replayGame(String history) {
        long seed = extractSeedFromHistory(history);

        TERenderer ter = new TERenderer();

        ter.initialize(WIDTH, HEIGHT + HUD_HEIGHT);

        World world = new World(
                WIDTH,
                HEIGHT,
                CHUNK_ROWS,
                CHUNK_COLS,
                seed
        );

        world.placePlayer();

        int start = history.indexOf('s') + 1;

        for (int i = start; i < history.length(); i++) {
            char c = history.charAt(i);

            applyMovement(world.getPlayer(), world, c);
        }

        startGameFromReload(world, ter, history);
    }

    private static long extractSeedFromHistory(String history) {
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

    //Implement for task2

    /**
     *
     * @param player add a spawn player function in World class,
     *               should be called in the constructor
     * @param world
     * @param move   only deal with wasd/WASD, make sure that the player wouldn't be stuck in the wall
     *               Updating the player location by draw the avatar in the tiles
     */
    private static void applyMovement(Player player, World world, char move) {
        Position pos = player.getPosition();
        int newX = pos.x;
        int newY = pos.y;

        char lower = Character.toLowerCase(move);
        if (lower == 'w') {
            newY += 1;
        } else if (lower == 'a') {
            newX -= 1;
        } else if (lower == 's') {
            newY -= 1;
        } else if (lower == 'd') {
            newX += 1;
        } else {
            return;
        }

        Position next = new Position(newX, newY);

        if (!isWalkable(world, next)) {
            return;
        }

        world.getGrid()[pos.x][pos.y] = Tileset.FLOOR;
        world.getGrid()[newX][newY] = player.getAvator();
        player.setPosition(next);
    }

    private static boolean isWalkable(World world, Position next) {
        int x = next.x;
        int y = next.y;

        if (x < 0 || x >= world.getGrid().length || y < 0 || y >= world.getGrid()[0].length) {
            return false;
        }

        if (world.getGrid()[x][y] == Tileset.NOTHING || world.getGrid()[x][y] == Tileset.WALL) {
            return false;
        }

        return true;
    }

    private static void startGameFromReload(World world, TERenderer ter, String history) {
        inputHistory = new StringBuilder(history);
        gameLoop(world, ter);
    }

    private static void gameLoop(World world, TERenderer ter) {
        Player player = world.getPlayer();
        TETile[][] tiles = world.getGrid();

        //Key Board
        char key;
        boolean waitForNextQ = false;

        //Mouse Click
        List<Position> path = null;
        boolean mousePressed = false;
        Position target = null;

        while (true) {
            //WASD
            if (StdDraw.hasNextKeyTyped()) {
                key = Character.toLowerCase(StdDraw.nextKeyTyped());

                inputHistory.append(key);

                //Task 2 here:
                applyMovement(player, world, key);

                if (key == ':') {
                    waitForNextQ = true;
                } else if (waitForNextQ && (key == 'q' || key == 'Q')) {
                    saveGame(inputHistory.toString());
                    System.exit(0);
                } else {
                    waitForNextQ = false;
                }
            }

            //Mouse Click
            boolean mousePressedCur = StdDraw.isMousePressed();
            //Only trigger once
            if (mousePressedCur && !mousePressed) {
                int x = (int) StdDraw.mouseX();
                int y = (int) StdDraw.mouseY();

                Position mouse = new Position(x, y);

                if (!isWalkable(world, mouse)){
                    clearPath(world, path);
                    path = null;
                    target = null;
                } else {
                    if (target == null) {
                        List<Position> newPath = findPathBFS(world, world.getPlayer().getPosition(), mouse);
                        if (newPath != null && !newPath.isEmpty()) {
                            clearPath(world, path);
                            path = newPath;
                            target = mouse;

                            drawPath(world, path);
                        }
                    } else if (mouse.equals(target)) {
                        ter.drawTiles(world.getGrid());
                        StdDraw.show();

                        followPath(player, world, path, ter);
                        clearPath(world, path);
                        path = null;
                        target = null;
                    } else {
                        List<Position> newPath =
                                findPathBFS(world, player.getPosition(), mouse);

                        if (newPath != null && !newPath.isEmpty()) {
                            clearPath(world, path);
                            path = newPath;
                            target = mouse;

                            drawPath(world, path);
                        }
                    }
                }
            }
            mousePressed = mousePressedCur;

            //Draw the world
            ter.drawTiles(tiles);

            //Draw HUD
            drawHUD(tiles, player);

            StdDraw.show();
        }
    }

    private static void drawHUD(TETile[][] tiles, Player player) {
        //Gray Background
        StdDraw.setPenColor(Color.GRAY);
        StdDraw.filledRectangle(
                WIDTH / 2.0,
                HEIGHT + HUD_HEIGHT / 2.0,
                WIDTH / 2.0,
                HUD_HEIGHT / 2.0
        );

        //Display the status of player
        drawPlayerStatus(player);

        //Get the location of the cursor
        int x = (int) StdDraw.mouseX();
        int y = (int) StdDraw.mouseY();

        if (x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT) {
            String desc = tiles[x][y].description();

            StdDraw.setPenColor(Color.WHITE);
            StdDraw.textLeft(WIDTH - 8, HEIGHT + HUD_HEIGHT - 1, desc);
        }
    }

    private static void drawPlayerStatus(Player player) {
        drawHearts(player);
        //Ambitious Features:
    }

    private static void drawHearts(Player player) {
        int hp = player.getHealth();
        int maxHp = player.getMaxHP();

        double startX = 2;
        double y = HEIGHT + HUD_HEIGHT - 1;

        for (int i = 0; i < maxHp; i++) {
            if (i < hp) {
                StdDraw.setPenColor(Color.RED);
            } else {
                StdDraw.setPenColor(Color.DARK_GRAY);
            }

            StdDraw.textLeft(startX + i * 1.2, y, "❤");
        }
    }

    private static List<Position> findPathBFS (World world, Position start, Position end) {
        Queue<Position> queue = new LinkedList<>();
        Map<Position, Position> parent = new HashMap<>();
        Set<Position> visited = new HashSet<>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            Position cur = queue.poll();

            if (cur.equals(end)) {
                break;
            }

            for (Position next : getNeighbors(cur)) {
                if (!visited.contains(next) && isWalkable(world, next)) {
                    queue.add(next);
                    visited.add(next);
                    parent.put(next, cur);
                }
            }
        }

        return findShortestPath(parent, start, end);
    }

    private static List<Position> getNeighbors(Position current) {
        List<Position> result = new ArrayList<>();

        result.add(new Position(current.x + 1, current.y));
        result.add(new Position(current.x - 1, current.y));
        result.add(new Position(current.x, current.y + 1));
        result.add(new Position(current.x, current.y - 1));

        return result;
    }

    private static List<Position> findShortestPath(Map<Position, Position> parent, Position start, Position end) {
        List<Position> path = new ArrayList<>();

        if (!parent.containsKey(end)) {
            return null;
        }

        Position cur = end;

        while (!cur.equals(start)) {
            path.add(cur);
            cur = parent.get(cur);
        }

        Collections.reverse(path);

        return path;
    }

    private static void followPath(Player player, World world, List<Position> path, TERenderer ter) {
        if(path == null) {
            return;
        }

        TETile[][] grid = world.getGrid();

        for (Position nextStep : path) {
            //Clear the old position
            Position oldStep = player.getPosition();

            char move = getMove(oldStep, nextStep);
            inputHistory.append(move);

            grid[oldStep.x][oldStep.y] = Tileset.FLOOR;

            //Update the current position
            player.setPosition(nextStep);

            //Place the new position
            grid[nextStep.x][nextStep.y] = player.getAvator();

            ter.drawTiles(grid);
            StdDraw.show();
        }
    }

    private static char getMove(Position from, Position to) {
        if (to.x == from.x + 1 && to.y == from.y) return 'd';
        if (to.x == from.x - 1 && to.y == from.y) return 'a';
        if (to.x == from.x && to.y == from.y + 1) return 'w';
        if (to.x == from.x && to.y == from.y - 1) return 's';

        throw new IllegalArgumentException("Invalid move");
    }

    private static void drawPath(World world, List<Position> path) {
        if (path == null) {
            return;
        }

        TETile[][] grid = world.getGrid();

        for (Position p : path) {
            if (!grid[p.x][p.y].equals(Tileset.AVATAR)) {
                grid[p.x][p.y] = Tileset.GRASS;
            }
        }
    }

    private static void clearPath(World world, List<Position> path) {
        if (path == null) {
            return;
        }

        TETile[][] grid = world.getGrid();

        for (Position p : path) {
            if (grid[p.x][p.y].equals(Tileset.GRASS)) {
                grid[p.x][p.y] = Tileset.FLOOR;
            }
        }
    }
}
