package core;

import edu.princeton.cs.algs4.StdDraw;
import tileengine.TERenderer;
import tileengine.TETile;

import java.awt.*;

public class Main {
    private static final int WIDTH = 80;
    private static final int HEIGHT = 60;

    public static void main(String[] args) {
        TERenderer ter = new TERenderer();
        ter.initialize(WIDTH, HEIGHT);

        long seed = 7520326094332350746L;


        World world = new World(
                WIDTH,
                HEIGHT,
                5,
                7,
                seed
        );


        TETile[][] tiles = world.getGrid();
        ter.drawTiles(tiles);
        StdDraw.show();
    }
}
