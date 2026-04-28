package core;

import tileengine.TETile;

import java.util.List;

/** Create a snapshot of the last world **/
public class Checkpoint {
    TETile[][] grid;
    List<Position> enemies;
    List<Position> traps;
    Position playerPosition;
    int playerHealth;
    int playerMoney;
    TETile playerStandingOn;
}
