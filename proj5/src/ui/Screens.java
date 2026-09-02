package ui;

import core.Player;
import edu.princeton.cs.algs4.StdDraw;
import tileengine.TETile;

import java.awt.Color;
import java.awt.Font;

/**
 * All StdDraw drawing for menus, HUD, and end-of-run screens. Screens only
 * reads game state to decide what to draw -- it never mutates it.
 */
public final class Screens {
    private final int width;
    private final int height;
    private final int hudHeight;

    private String hudMessage = "";
    private int hudMessageTimer = 0;

    public Screens(int width, int height, int hudHeight) {
        this.width = width;
        this.height = height;
        this.hudHeight = hudHeight;
    }

    public void initializeCanvas() {
        StdDraw.setCanvasSize(width * 12, (height + hudHeight) * 12);
        StdDraw.setXscale(0, width);
        StdDraw.setYscale(0, height + hudHeight);
        StdDraw.clear();
        StdDraw.enableDoubleBuffering();
    }

    public void showHudMessage(String message) {
        hudMessage = message;
        hudMessageTimer = 80;
    }

    public void drawMainMenu() {
        double cx = width / 2.0;
        double cy = height / 2.0;

        StdDraw.clear(new Color(15, 15, 30));

        StdDraw.setPenColor(new Color(255, 215, 0));
        StdDraw.rectangle(cx, cy, width * 0.45, height * 0.40);
        StdDraw.rectangle(cx, cy, width * 0.44, height * 0.39);

        StdDraw.setPenColor(Color.WHITE);
        StdDraw.setFont(new Font("Monaco", Font.BOLD, 40));
        StdDraw.text(cx, cy + 12, "CS61B: BYOW");

        StdDraw.setFont(new Font("Monaco", Font.ITALIC, 18));
        StdDraw.text(cx, cy + 6, "Dungeon Adventure");

        StdDraw.setFont(new Font("Monaco", Font.PLAIN, 22));
        StdDraw.text(cx, cy + 1, "(N) New Game");
        StdDraw.text(cx, cy - 4, "(L) Load Game");
        StdDraw.text(cx, cy - 9, "(Q) Quit Game");

        StdDraw.line(cx - 15, cy + 3, cx + 15, cy + 3);
        StdDraw.show();
    }

    public void drawSeedPrompt(String seed) {
        double cx = width / 2.0;
        double cy = height / 2.0;

        Color bg = new Color(6, 16, 28);
        Color panel = new Color(18, 42, 62);
        Color ice = new Color(145, 205, 225);
        Color frost = new Color(190, 240, 255);
        Color gold = new Color(255, 220, 105);
        Color dim = new Color(70, 105, 145);

        StdDraw.clear(bg);

        StdDraw.setPenColor(panel);
        StdDraw.filledRectangle(cx, cy, width * 0.42, height * 0.30);

        StdDraw.setPenColor(ice);
        StdDraw.rectangle(cx, cy, width * 0.42, height * 0.30);

        StdDraw.setPenColor(dim);
        StdDraw.rectangle(cx, cy, width * 0.38, height * 0.26);

        StdDraw.setPenColor(frost);
        StdDraw.setFont(new Font("Monaco", Font.BOLD, 34));
        StdDraw.text(cx, cy + 9, "OPEN THE FROST VAULT");

        StdDraw.setFont(new Font("Monaco", Font.PLAIN, 16));
        StdDraw.text(cx, cy + 4, "Enter a seed, then press S");

        StdDraw.setPenColor(new Color(10, 25, 40));
        StdDraw.filledRectangle(cx, cy - 3, width * 0.25, 2.2);

        StdDraw.setPenColor(dim);
        StdDraw.rectangle(cx, cy - 3, width * 0.25, 2.2);

        StdDraw.setPenColor(gold);
        StdDraw.setFont(new Font("Monaco", Font.BOLD, 22));
        String displaySeed = seed.isEmpty() ? "Seed: _" : "Seed: " + seed + "_";
        StdDraw.text(cx, cy - 3, displaySeed);

        StdDraw.setPenColor(ice);
        StdDraw.setFont(new Font("Monaco", Font.PLAIN, 13));
        StdDraw.text(cx, cy - 10, "Backspace edits   S begins the run");

        StdDraw.show();
    }

    public void drawHud(TETile[][] tiles, Player player) {
        Color hudBg = new Color(20, 34, 50);
        Color ice = new Color(145, 205, 225);
        Color frost = new Color(190, 240, 255);
        Color gold = new Color(255, 220, 105);
        Color danger = new Color(255, 95, 135);
        Color dim = new Color(80, 95, 110);

        StdDraw.setPenColor(hudBg);
        StdDraw.filledRectangle(width / 2.0, height + hudHeight / 2.0, width / 2.0, hudHeight / 2.0);

        if (hudMessageTimer > 0) {
            StdDraw.setPenColor(new Color(190, 240, 255));
            StdDraw.setFont(new Font("Monaco", Font.BOLD, 13));
            StdDraw.text(width / 2.0, height + 0.35, hudMessage);
            hudMessageTimer--;
        }

        StdDraw.setPenColor(new Color(70, 105, 145));
        StdDraw.line(0, height, width, height);

        double y = height + hudHeight - 1;
        StdDraw.setFont(new Font("Monaco", Font.BOLD, 14));

        StdDraw.setPenColor(frost);
        StdDraw.textLeft(1, y, "HP");

        int hp = player.getHealth();
        int maxHp = player.getMaxHP();
        for (int i = 0; i < maxHp; i++) {
            StdDraw.setPenColor(i < hp ? danger : dim);
            StdDraw.textLeft(4 + i * 1.2, y, "♥");
        }

        StdDraw.setPenColor(gold);
        StdDraw.textLeft(15, y, "Coins " + player.getMoney());

        if (player.isInvincible()) {
            StdDraw.setPenColor(new Color(120, 255, 220));
            StdDraw.textLeft(28, y, "Mode AURORA");
        } else {
            StdDraw.setPenColor(ice);
            StdDraw.textLeft(28, y, "Mode MORTAL");
        }

        StdDraw.setPenColor(new Color(255, 240, 170));
        StdDraw.textLeft(45, y, "[E] Aura");

        int x = (int) StdDraw.mouseX();
        int yy = (int) StdDraw.mouseY();
        if (x >= 0 && x < width && yy >= 0 && yy < height) {
            StdDraw.setPenColor(frost);
            StdDraw.textRight(width - 1, y, "Tile " + tiles[x][yy].description());
        }
    }

    public char showVictory() {
        initializeCanvas();
        double cx = width / 2.0;
        double cy = height / 2.0;

        Color bg = new Color(6, 16, 28);
        Color ice = new Color(145, 205, 225);
        Color frost = new Color(190, 240, 255);
        Color gold = new Color(255, 220, 105);

        StdDraw.clear(bg);
        StdDraw.setPenColor(new Color(18, 42, 62));
        StdDraw.filledRectangle(cx, cy, width * 0.42, height * 0.34);
        StdDraw.setPenColor(ice);
        StdDraw.rectangle(cx, cy, width * 0.42, height * 0.34);
        StdDraw.setPenColor(new Color(70, 105, 145));
        StdDraw.rectangle(cx, cy, width * 0.39, height * 0.30);

        StdDraw.setPenColor(gold);
        StdDraw.setFont(new Font("Monaco", Font.BOLD, 64));
        StdDraw.text(cx, cy + 12, "★");

        StdDraw.setPenColor(frost);
        StdDraw.setFont(new Font("Monaco", Font.BOLD, 38));
        StdDraw.text(cx, cy + 5, "TREASURE CLAIMED");

        StdDraw.setPenColor(ice);
        StdDraw.setFont(new Font("Monaco", Font.ITALIC, 17));
        StdDraw.text(cx, cy, "The ice vault falls silent.");

        StdDraw.setPenColor(new Color(255, 235, 160));
        StdDraw.setFont(new Font("Monaco", Font.PLAIN, 16));
        StdDraw.text(cx, cy - 8, "R  checkpoint    M  menu    Q  quit");

        StdDraw.show();
        return waitForChoice();
    }

    public char showGameOver() {
        initializeCanvas();
        double cx = width / 2.0;
        double cy = height / 2.0;

        Color bg = new Color(6, 16, 28);
        Color ice = new Color(70, 105, 145);
        Color frost = new Color(145, 205, 225);
        Color danger = new Color(255, 95, 135);
        Color dim = new Color(18, 30, 45);

        StdDraw.clear(bg);
        StdDraw.setPenColor(dim);
        StdDraw.filledRectangle(cx, cy, width * 0.42, height * 0.34);
        StdDraw.setPenColor(ice);
        StdDraw.rectangle(cx, cy, width * 0.42, height * 0.34);
        StdDraw.setPenColor(new Color(255, 95, 135));
        StdDraw.rectangle(cx, cy, width * 0.39, height * 0.30);

        StdDraw.setPenColor(danger);
        StdDraw.setFont(new Font("Monaco", Font.BOLD, 60));
        StdDraw.text(cx, cy + 12, "✦");

        StdDraw.setPenColor(new Color(255, 150, 180));
        StdDraw.setFont(new Font("Monaco", Font.BOLD, 38));
        StdDraw.text(cx, cy + 5, "FROZEN OUT");

        StdDraw.setPenColor(frost);
        StdDraw.setFont(new Font("Monaco", Font.ITALIC, 17));
        StdDraw.text(cx, cy, "The cold takes hold.");

        StdDraw.setPenColor(new Color(190, 240, 255));
        StdDraw.setFont(new Font("Monaco", Font.PLAIN, 16));
        StdDraw.text(cx, cy - 8, "R  checkpoint    M  menu    Q  quit");

        StdDraw.show();
        return waitForChoice();
    }

    private char waitForChoice() {
        while (true) {
            if (StdDraw.hasNextKeyTyped()) {
                char key = Character.toLowerCase(StdDraw.nextKeyTyped());
                if (key == 'r' || key == 'm' || key == 'q') {
                    return key;
                }
            }
        }
    }
}
