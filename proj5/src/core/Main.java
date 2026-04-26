package core;

import edu.princeton.cs.algs4.In;
import edu.princeton.cs.algs4.Out;
import edu.princeton.cs.algs4.StdDraw;
import tileengine.TERenderer;
import tileengine.TETile;

import java.awt.*;
import java.io.File;

public class Main {
    /** World Parameters **/
    private static final int WIDTH = 80;
    private static final int HEIGHT = 60;
    private static final int CHUNK_ROWS = 4;
    private static final int CHUNK_COLS = 4;

    private static final int HUD_HEIGHT = 2;

    /** World Record **/
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
                    seedStr.append(ch);
                    drawSeed(seedStr.toString());
                } else if (ch == 's' || ch == 'S') {
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

        //Initializing the history with seed
        inputHistory = new StringBuilder();
        inputHistory.append("n").append(seed).append("s");

        //Initializing the render
        TERenderer ter = new TERenderer();
        ter.initialize(WIDTH,HEIGHT + HUD_HEIGHT);

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

        ter.initialize(WIDTH,HEIGHT + HUD_HEIGHT);

        World world = new World(
                WIDTH,
                HEIGHT,
                CHUNK_ROWS,
                CHUNK_COLS,
                seed
        );

        boolean startReply = false;

        for (char c : history.toCharArray()) {
            //Skip the seed part
            if (c == 's') {
                startReply = true;
                continue;
            }

            if (startReply) {
                applyMovement(world.getPlayer(), world, c);
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

    //Implement for task2
    /**
     *
     * @param player add a spawn player function in World class,
     *               should be called in the constructor
     * @param world
     * @param move only deal with wasd/WASD, make sure that the player wouldn't be stuck in the wall
     * Updating the player location by draw the avatar in the tiles
     */
    private static void applyMovement(Player player, World world, char move) {

    }

    private static void startGameFromReload(World world, TERenderer ter, String history) {
        inputHistory = new StringBuilder(history);
        gameLoop(world, ter);
    }

    private static void gameLoop(World world, TERenderer ter) {
        Player player = world.getPlayer();
        TETile[][] tiles = world.getGrid();

        char key;
        boolean waitForNextQ = false;

        while (true) {
            //Game loop start here
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
}
