package core;

import edu.princeton.cs.algs4.StdDraw;
import tileengine.TERenderer;
import tileengine.TETile;

import java.awt.*;

public class Main {
    private static final int WIDTH = 60;
    private static final int HEIGHT = 40;

    public static void main(String[] args) {
        TERenderer ter = new TERenderer();
        ter.initialize(WIDTH, HEIGHT);

        long seed = 1637079600780352519L;


        World world = new World(
                WIDTH,
                HEIGHT,
                4,
                5,
                seed
        );


        TETile[][] tiles = world.getGrid();
        ter.drawTiles(tiles);
        StdDraw.show();
    }
}
