package core;

import edu.princeton.cs.algs4.StdDraw;
import tileengine.TERenderer;
import tileengine.TETile;

import java.awt.*;

public class Main {
    private static final int WIDTH = 80;
    private static final int HEIGHT = 80;

    public static void main(String[] args) {
        TERenderer ter = new TERenderer();
        ter.initialize(WIDTH, HEIGHT);

        long seed = 2148924051058622185L;


        World world = new World(
                WIDTH,
                HEIGHT,
                4,
                4,
                seed
        );


        TETile[][] tiles = world.getGrid();
        ter.drawTiles(tiles);
        StdDraw.show();
    }
}
