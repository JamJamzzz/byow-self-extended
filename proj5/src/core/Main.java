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
    private static int playerMoveCount = 0;
    private static final int ENEMY_MOVE_INTERVAL = 2;
    private static boolean canMove = false;

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
        StdDraw.clear(new Color(15, 15, 30));

        StdDraw.setPenColor(new Color(255, 215, 0));
        StdDraw.rectangle(400, 300, 360, 260);
        StdDraw.rectangle(400, 300, 355, 255);

        StdDraw.setPenColor(Color.WHITE);
        StdDraw.setFont(new Font("Monaco", Font.BOLD, 48));
        StdDraw.text(400, 470, "CS61B: BYOW");

        StdDraw.setFont(new Font("Monaco", Font.ITALIC, 18));
        StdDraw.text(400, 420, "Dungeon Adventure");

        StdDraw.setFont(new Font("Monaco", Font.PLAIN, 24));
        StdDraw.text(400, 320, "(N) New Game");
        StdDraw.text(400, 270, "(L) Load Game");
        StdDraw.text(400, 220, "(Q) Quit Game");

        StdDraw.line(250, 380, 550, 380);

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
        world.placeEnemy();
        world.placeTrap();
        world.placeHealingItems();
        world.placeHealingItems();
        world.placeCoins();

        //Initializing the history with seed
        inputHistory = new StringBuilder();
        inputHistory.append("n").append(seed).append("s");

        world.saveCheckpoint();

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
        world.placeEnemy();
        world.placeTrap();
        world.placeHealingItems();
        world.placeHealingItems();
        world.placeCoins();
        world.saveCheckpoint();

        int start = history.indexOf('s') + 1;

        Player player = world.getPlayer();
        TETile[][] tiles = world.getGrid();

        for (int i = start; i < history.length(); i++) {
            char key = history.charAt(i);

            if (key == 'e') {
                player.toggleInvincible();
                continue;
            }

            applyMovement(player, world, key);
            if (playerMoveCount % ENEMY_MOVE_INTERVAL == 0 && canMove) {
                world.moveEnemies();
            }
            if (playerMoveCount % 5 == 0 && canMove) {
                world.moveTraps();
            }

            if (player.getHealth() <= 0) {
                world.restoreCheckpoint();
                playerMoveCount = 0;
                canMove = false;
            }

            if (world.countCoins() == 0) {
                world.restoreCheckpoint();
                playerMoveCount = 0;
                canMove = false;
            }

            //Attack
            if (key == 'j') {
                Position attackPosi = player.attack();

                if (attackPosi != null) {
                    int ax = attackPosi.x;
                    int ay = attackPosi.y;

                    if (inBounds(ax, ay, tiles)) {

                        TETile original = tiles[ax][ay];

                        if (isAttackable(original)) {

                            tiles[ax][ay] = player.getAttackTile();
                            ter.drawTiles(tiles);
                            StdDraw.show();

                            StdDraw.pause(80);
                            tiles[ax][ay] = original;


                            if (original == Tileset.ENEMY) {
                                tiles[ax][ay] = Tileset.FLOOR;
                                world.removeEnemyAt(attackPosi);
                            }

                            if (original == Tileset.TRAP) {
                                player.deductHealth(1);
                            }

                            if (player.getHealth() <= 0) {
                                world.restoreCheckpoint();
                                playerMoveCount = 0;
                                canMove = false;
                            }
                        }
                    }
                }
            }

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

    private static void applyMovement(Player player, World world, char move) {
        Position pos = player.getPosition();
        int newX = pos.x;
        int newY = pos.y;

        char lower = Character.toLowerCase(move);
        if (lower == 'w') {
            newY += 1;
            player.setDirection(Direction.UP);
            playerMoveCount++;
            canMove = true;
        } else if (lower == 'a') {
            newX -= 1;
            player.setDirection(Direction.LEFT);
            playerMoveCount++;
            canMove = true;
        } else if (lower == 's') {
            newY -= 1;
            player.setDirection(Direction.DOWN);
            playerMoveCount++;
            canMove = true;
        } else if (lower == 'd') {
            newX += 1;
            player.setDirection(Direction.RIGHT);
            playerMoveCount++;
            canMove = true;
        } else {
            canMove = false;
            return;
        }

        Position next = new Position(newX, newY);

        if (!isWalkable(world, next)) {
            canMove = false;
            return;
        }
        //Check if the player is leaving the room or not
        world.saveCheckpointIfLeavingRoom(pos, next);

        TETile[][] grid = world.getGrid();

        Position old = player.getPosition();

        TETile nextTile = grid[newX][newY];

        if (nextTile == Tileset.ENEMY) {
            player.deductHealth(1);
            world.removeEnemyAt(next);
            nextTile = Tileset.FLOOR;
        } else if (nextTile == Tileset.TRAP) {
            player.deductHealth(1);
        } else if (nextTile == Tileset.FLOWER) {
            new HealingItem(1).interact(player);
            nextTile = Tileset.FLOOR;
        } else if (nextTile == Tileset.UNLOCKED_DOOR) {
            new Coin(1).interact(player);
            nextTile = Tileset.FLOOR;
        }

        //Interaction
        if (nextTile == Tileset.ENEMY) {
            player.deductHealth(1);
            player.setStandingOn(Tileset.FLOOR);
            world.removeEnemyAt(new Position(newX, newY));
        } else if (nextTile == Tileset.TRAP) {
            player.deductHealth(1);
            player.setStandingOn(Tileset.TRAP);
        } else if (nextTile == Tileset.FLOWER) {
            HealingItem heal = new HealingItem(1);
            heal.interact(player);
            player.setStandingOn(Tileset.FLOOR);
        } else if (nextTile == Tileset.UNLOCKED_DOOR) {
            Coin coin = new Coin(1);
            coin.interact(player);
            player.setStandingOn(Tileset.FLOOR);
        }
        grid[old.x][old.y] = player.getStandingOn();
        grid[next.x][next.y] = player.getAvator();
        player.setPosition(next);
        player.setStandingOn(Tileset.FLOOR);
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

                if (key == 'e') {
                    player.toggleInvincible();
                }

                //Task 2 here:
                applyMovement(player, world, key);
                if (playerMoveCount % ENEMY_MOVE_INTERVAL == 0 && canMove) {
                    world.moveEnemies();
                }
                if (playerMoveCount % 5 == 0 && canMove) {
                    world.moveTraps();
                }

                if (key == ':') {
                    waitForNextQ = true;
                } else if (waitForNextQ && (key == 'q' || key == 'Q')) {
                    saveGame(inputHistory.toString());
                    System.exit(0);
                } else {
                    waitForNextQ = false;
                }

                //Attack
                if (key == 'j') {
                    Position attackPosi = player.attack();

                    if (attackPosi != null) {
                        int ax = attackPosi.x;
                        int ay = attackPosi.y;

                        if (inBounds(ax, ay, tiles)) {

                            TETile original = tiles[ax][ay];

                            if (isAttackable(original)) {

                                tiles[ax][ay] = player.getAttackTile();
                                ter.drawTiles(tiles);
                                StdDraw.show();

                                StdDraw.pause(80);
                                tiles[ax][ay] = original;


                                if (original == Tileset.ENEMY) {
                                    tiles[ax][ay] = Tileset.FLOOR;
                                    world.removeEnemyAt(attackPosi);
                                }

                                if (original == Tileset.TRAP) {
                                    player.deductHealth(1);
                                }
                            }
                        }
                    }
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
                    path = null;
                    target = null;
                } else {
                    if (target == null) {
                        List<Position> newPath = findPathBFS(world, world.getPlayer().getPosition(), mouse);
                        if (newPath != null && !newPath.isEmpty()) {
                            path = newPath;
                            target = mouse;
                        }
                    } else if (mouse.equals(target)) {
                        ter.drawTiles(world.getGrid());
                        StdDraw.show();

                        followPath(player, world, path, ter);
                        path = null;
                        target = null;
                    } else {
                        List<Position> newPath =
                                findPathBFS(world, player.getPosition(), mouse);

                        if (newPath != null && !newPath.isEmpty()) {
                            path = newPath;
                            target = mouse;
                        }
                    }
                }
            }
            mousePressed = mousePressedCur;

            //Draw the world
            ter.drawTiles(tiles);

            //Draw HUD
            drawHUD(tiles, player);

            //Draw the overlay path
            drawPathOverlay(path);

            if (player.getHealth() <= 0) {
                char choice = showGameOver();
                if (choice == 'r') {
                    if (world.restoreCheckpoint()) {
                        //Initiliazing the grid
                        ter.initialize(WIDTH, HEIGHT + HUD_HEIGHT);
                        path = null;
                        target = null;
                        playerMoveCount = 0;
                        saveGame(inputHistory.toString());
                        continue;
                    }
                }

                if (choice == 'm') {
                    File file = new File("save.txt");
                    file.delete();
                    showMenu();
                    return;
                }

                if (choice == 'q') {
                    saveGame(inputHistory.toString());
                    System.exit(0);
                }
            }

            if (world.countCoins() == 0) {
                char choice = showVictory();
                if (choice == 'r') {
                    if (world.restoreCheckpoint()) {
                        ter.initialize(WIDTH, HEIGHT + HUD_HEIGHT);
                        path = null;
                        target = null;
                        playerMoveCount = 0;
                        canMove = false;
                        saveGame(inputHistory.toString());
                        continue;
                    }
                }

                if (choice == 'm') {
                    File file = new File("save.txt");
                    file.delete();
                    showMenu();
                    return;
                }

                if (choice == 'q') {
                    saveGame(inputHistory.toString());
                    System.exit(0);
                }
            }


            StdDraw.show();
        }
    }

    private static boolean inBounds(int x, int y, TETile[][] grid) {
        return x >= 0 && x < grid.length &&
                y >= 0 && y < grid[0].length;
    }

    private static boolean isAttackable(TETile tile) {
        return tile == Tileset.FLOOR ||
                tile == Tileset.ENEMY ||
                tile == Tileset.TRAP;
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

        //status of invincible mode
        if (player.isInvincible()) {
            StdDraw.setPenColor(Color.RED);
        } else {
            StdDraw.setPenColor(Color.WHITE);
        }
        StdDraw.textLeft(WIDTH - 35, HEIGHT + HUD_HEIGHT - 1, "(e)nable invincible mode");


        // Display coin
        StdDraw.setPenColor(Color.YELLOW);
        StdDraw.textLeft(WIDTH - 20, HEIGHT + HUD_HEIGHT - 1, "Coins: " + player.getMoney());

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
        canMove = true;

        for (Position nextStep : path) {
            //Clear the old position
            Position oldStep = player.getPosition();

            char move = getMove(oldStep, nextStep);
            inputHistory.append(move);

            world.saveCheckpointIfLeavingRoom(oldStep, nextStep);
            grid[oldStep.x][oldStep.y] = player.getStandingOn();
            TETile nextTile = grid[nextStep.x][nextStep.y];
            player.setStandingOn(nextTile);
            if (nextTile == Tileset.ENEMY) {
                player.deductHealth(1);
                player.setStandingOn(Tileset.FLOOR);
            }

            if (nextTile == Tileset.TRAP) {
                player.deductHealth(1);
            }

            if (nextTile == Tileset.FLOWER) {
                HealingItem heal = new HealingItem(1);
                heal.interact(player);
                player.setStandingOn(Tileset.FLOOR);
            }

            if (nextTile == Tileset.UNLOCKED_DOOR) {
                Coin coin = new Coin(1);
                coin.interact(player);
                player.setStandingOn(Tileset.FLOOR);
            }

            //Place the new position
            grid[nextStep.x][nextStep.y] = player.getAvator();
            //Update the current position
            player.setPosition(nextStep);
            playerMoveCount++;
            if (playerMoveCount % ENEMY_MOVE_INTERVAL == 0 && canMove) {
                world.moveEnemies();
            }

            if (playerMoveCount % 5 == 0 && canMove) {
                world.moveTraps();
            }

            ter.drawTiles(grid);
            StdDraw.show();
        }
        player.setStandingOn(Tileset.FLOOR);
        canMove = false;
    }

    private static char getMove(Position from, Position to) {
        if (to.x == from.x + 1 && to.y == from.y) return 'd';
        if (to.x == from.x - 1 && to.y == from.y) return 'a';
        if (to.x == from.x && to.y == from.y + 1) return 'w';
        if (to.x == from.x && to.y == from.y - 1) return 's';

        throw new IllegalArgumentException("Invalid move");
    }
    private static void drawPathOverlay(List<Position> path) {
        if (path == null) return;

        for (Position p : path) {
            Tileset.GRASS.draw(p.x, p.y);
        }
    }

    private static char showVictory() {
        StdDraw.setCanvasSize(800, 600);
        StdDraw.setXscale(0, 800);
        StdDraw.setYscale(0, 600);

        StdDraw.clear(new Color(10, 25, 10));

        StdDraw.setPenColor(new Color(255, 215, 0));
        StdDraw.rectangle(400, 300, 350, 250);
        StdDraw.rectangle(400, 300, 340, 240);

        StdDraw.setPenColor(new Color(255, 215, 0));
        StdDraw.setFont(new Font("Monaco", Font.BOLD, 80));
        StdDraw.text(400, 430, "★");

        StdDraw.text(400, 260, "Treasure secured!");

        StdDraw.setPenColor(new Color(255, 215, 0));
        StdDraw.setFont(new Font("Monaco", Font.BOLD, 50));
        StdDraw.text(400, 340, "YOU WIN!");

        StdDraw.setPenColor(Color.WHITE);
        StdDraw.setFont(new Font("Monaco", Font.ITALIC, 18));
        StdDraw.text(400, 280, "All coins collected. Legend!");

        StdDraw.setPenColor(Color.WHITE);
        StdDraw.setFont(new Font("Monaco", Font.PLAIN, 20));
        StdDraw.text(400, 220, "Press R to restart, M for menu, Q to quit");

        StdDraw.show();

        while (true) {
            if (StdDraw.hasNextKeyTyped()) {
                char key = Character.toLowerCase(StdDraw.nextKeyTyped());
                if (key == 'r' || key == 'm' || key == 'q') {
                    return key;
                }
            }
        }
    }

    private static char showGameOver() {
        StdDraw.setCanvasSize(800, 600);
        StdDraw.setXscale(0, 800);
        StdDraw.setYscale(0, 600);

        StdDraw.clear(new Color(15, 0, 0));
        StdDraw.text(400, 180, "Press R to retry");

        StdDraw.setPenColor(Color.RED);
        StdDraw.rectangle(400, 300, 350, 250);

        StdDraw.setPenColor(Color.RED);
        StdDraw.setFont(new Font("Monaco", Font.BOLD, 80));
        StdDraw.text(400, 430, "☠");

        StdDraw.setPenColor(Color.RED);
        StdDraw.setFont(new Font("Monaco", Font.BOLD, 50));
        StdDraw.text(400, 340, "YOU DIED");

        StdDraw.setPenColor(new Color(200, 200, 200));
        StdDraw.setFont(new Font("Monaco", Font.ITALIC, 18));
        StdDraw.text(400, 280, "The dungeon claims another soul...");

        StdDraw.setPenColor(Color.WHITE);
        StdDraw.setFont(new Font("Monaco", Font.PLAIN, 20));
        StdDraw.text(400, 220, "Press R to restart, M for menu, Q to quit");

        StdDraw.show();

        while (true) {
            if (StdDraw.hasNextKeyTyped()) {
                char key = Character.toLowerCase(StdDraw.nextKeyTyped());

                if (key == 'r' || key == 'm' || key == 'q') {
                    return key;
                }
            }
        }
    }

//    private static void drawPath(World world, List<Position> path) {
//        if (path == null) {
//            return;
//        }
//
//        TETile[][] grid = world.getGrid();
//
//        for (Position p : path) {
//            if (!grid[p.x][p.y].equals(Tileset.AVATAR)) {
//                grid[p.x][p.y] = Tileset.GRASS;
//            }
//        }
//    }
//
//    private static void clearPath(World world, List<Position> path) {
//        if (path == null) {
//            return;
//        }
//
//        TETile[][] grid = world.getGrid();
//
//        for (Position p : path) {
//            if (grid[p.x][p.y].equals(Tileset.GRASS)) {
//                grid[p.x][p.y] = Tileset.FLOOR;
//            }
//        }
//    }
}
