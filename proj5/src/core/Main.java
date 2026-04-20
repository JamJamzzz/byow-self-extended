package core;

import edu.princeton.cs.algs4.StdDraw;
import tileengine.TERenderer;
import tileengine.TETile;

import java.awt.*;

public class Main {
    private static final int WIDTH = 80;
    private static final int HEIGHT = 40;

    public static void main(String[] args) {
        TERenderer ter = new TERenderer();
        ter.initialize(WIDTH, HEIGHT);

        long seed = 2873123;


        World world = new World(
                WIDTH,
                HEIGHT,
                5,
                6,
                seed
        );


        TETile[][] tiles = world.getGrid();
        ter.drawTiles(tiles);
        StdDraw.show();
    }
}
