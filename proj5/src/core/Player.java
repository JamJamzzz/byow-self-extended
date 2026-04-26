package core;

import tileengine.TETile;
import tileengine.Tileset;

import java.util.LinkedList;
import java.util.Queue;

/** Default Play **/
public class Player {
    /** Class Attributes **/
    private final int MAX_HP = 10;

    /** Player Attributes **/
    private int MaxHP;
    private int health;
    private TETile avator;

    public Player() {
        health = 3;
        MaxHP = 5;
        avator = Tileset.AVATAR;
    }

    public int getHealth() {
        return health;
    }

    public int getMaxHP() {
        return MaxHP;
    }

    //Ambitious Features:
    public void deductHealth() {
        ;
    }

    public void attack() {
        ;
    }

    public void death() {
        ;
    }
}
