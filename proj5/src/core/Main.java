package core;

import edu.princeton.cs.algs4.StdDraw;
import net.sf.saxon.expr.Component;
import tileengine.TERenderer;
import tileengine.TETile;

import java.awt.*;

public class Main {
    /** World Parameters **/
    private static final int WIDTH = 80;
    private static final int HEIGHT = 60;
    private static final int CHUNK_ROWS = 4;
    private static final int CHUNK_COLS = 4;

    private static final int HUD_HEIGHT = 2;

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
        TERenderer ter = new TERenderer();

        ter.initialize(WIDTH,HEIGHT + HUD_HEIGHT);

        World world = new World(
                WIDTH,
                HEIGHT,
                CHUNK_ROWS,
                CHUNK_COLS,
                seed
        );

        Player player = new Player();

        TETile[][] tiles = world.getGrid();

        while (true) {
            ter.drawTiles(tiles);

            drawHUD(tiles, player);

            //Implement task2 interactivity

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
