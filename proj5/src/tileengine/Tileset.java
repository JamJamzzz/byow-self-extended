package tileengine;

import java.awt.Color;

/**
 * Contains constant tile objects, to avoid having to remake the same tiles in different parts of
 * the code.
 *
 * You are free to (and encouraged to) create and add your own tiles to this file. This file will
 * be turned in with the rest of your code.
 *
 * Ex:
 *      world[x][y] = Tileset.FLOOR;
 *
 * The style checker may crash when you try to style check this file due to use of unicode
 * characters. This is OK.
 */

public class Tileset {
    public static final TETile AVATAR = new TETile('@', new Color(125, 190, 215), new Color(6, 16, 28), "mortal", 0);
    public static final TETile AURORA = new TETile('♛', new Color(255, 215, 95), new Color(6, 16, 28), "aurora", 0);
    public static final TETile WALL = new TETile('#', new Color(70, 105, 145), new Color(12, 24, 40),
            "wall", 1);
    public static final TETile FLOOR = new TETile('·', new Color(125, 190, 215), new Color(6, 16, 28), "floor", 2);
    public static final TETile NOTHING = new TETile('·', new Color(18, 38, 58), new Color(3, 8, 16), "void", 3);
    public static final TETile GRASS = new TETile('"', new Color(190, 240, 255), new Color(6, 16, 28), "grass", 4);
    public static final TETile WATER = new TETile('≈', Color.blue, Color.black, "water", 5);
    public static final TETile HEAL = new TETile('♥', new Color(100, 255, 185), new Color(6, 16, 28), "healing item", 6);
    public static final TETile LOCKED_DOOR = new TETile('█', Color.orange, Color.black,
            "locked door", 7);
    public static final TETile UNLOCKED_DOOR = new TETile('◉', new Color(245, 190, 60), new Color(6, 16, 28),
            "unlocked door", 8);
    public static final TETile SAND = new TETile('▒', Color.yellow, Color.black, "sand", 9);
    public static final TETile MOUNTAIN = new TETile('▲', Color.gray, Color.black, "mountain", 10);
    public static final TETile TREE = new TETile('♠', Color.green, Color.black, "tree", 11);

    public static final TETile CELL = new TETile('█', Color.white, Color.black, "cell", 12);
    public static final TETile ENEMY = new TETile('♆', new Color(205, 95, 255), new Color(6, 16, 28), "enemy", 13);
    public static final TETile TRAP = new TETile('✿', new Color(255, 95, 150), new Color(6, 16, 28), "trap", 14);
    public static final TETile ATTACK_UP = new TETile('^', new Color(255, 240, 170), new Color(6, 16, 28), "attack up", 15);
    public static final TETile ATTACK_DOWN = new TETile('v', new Color(255, 240, 170), new Color(6, 16, 28), "attack down", 16);
    public static final TETile ATTACK_LEFT = new TETile('(', new Color(255, 240, 170), new Color(6, 16, 28), "attack left", 17);
    public static final TETile ATTACK_RIGHT = new TETile(')', new Color(255, 240, 170), new Color(6, 16, 28), "attack right", 18);
}


