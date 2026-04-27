package core;

import tileengine.TETile;
import tileengine.Tileset;

import java.util.LinkedList;
import java.util.Queue;

/** Default Player **/
public class Player {
    /** Class Attributes **/
    private final int MAX_HP = 10;

    /** Player Attributes **/
    private int money;
    private int MaxHP;
    private int health;
    private TETile avator;

    /** Player Location **/
    private Position position;

    /** Player Direction **/
    private Direction direction;
    TETile standingOn;

    public Player() {
        health = 3;
        MaxHP = 5;
        avator = Tileset.AVATAR;
        direction = Direction.UP;
        standingOn = Tileset.FLOOR;
    }

    public int getHealth() {
        return health;
    }

    public int getMaxHP() {
        return MaxHP;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public TETile getAvator() {
        return avator;
    }

    //Ambitious Features:
    public void deductHealth(int damage) {
        health -= damage;
    }

    public Position attack() {
        int x = position.x;
        int y = position.y;

        switch (direction) {
            case UP: return new Position(x, y + 1);
            case DOWN: return new Position(x, y - 1);
            case LEFT: return new Position(x - 1, y);
            case RIGHT: return new Position(x + 1, y);
        }
        return null;
    }

    public TETile getAttackTile() {
        switch (direction) {
            case UP: return Tileset.ATTACK_UP;
            case DOWN: return Tileset.ATTACK_DOWN;
            case LEFT: return Tileset.ATTACK_LEFT;
            case RIGHT: return Tileset.ATTACK_RIGHT;
        }
        return Tileset.CELL; // fallback
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public int getMoney() {
        return money;
    }

    public void addMoney(int amount) {
        money += amount;
    }

    public TETile getStandingOn() {
        return standingOn;
    }

    public void setStandingOn(TETile standing) {
        standingOn = standing;
    }

    public void death() {
        ;
    }
}
