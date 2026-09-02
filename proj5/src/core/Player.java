package core;

import tileengine.TETile;
import tileengine.Tileset;

/** The player-controlled avatar: health, money, facing direction, and invincibility mode. */
public class Player {
    private static final int STARTING_HEALTH = 3;
    private static final int STARTING_MAX_HP = 5;

    private int money;
    private int maxHp;
    private int health;

    private Position position;
    private Direction direction;
    private TETile standingOn;

    private boolean invincible;

    public Player() {
        health = STARTING_HEALTH;
        maxHp = STARTING_MAX_HP;
        direction = Direction.UP;
        standingOn = Tileset.FLOOR;
        invincible = false;
    }

    public int getHealth() {
        return health;
    }

    public int getMaxHP() {
        return maxHp;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    /** The avatar tile is purely derived from invincibility -- there is no separate stored render mode to drift out of sync. */
    public TETile getAvatar() {
        return invincible ? Tileset.AURORA : Tileset.AVATAR;
    }

    public void deductHealth(int damage) {
        if (invincible) {
            return;
        }
        health -= damage;
        health = Math.min(health, maxHp);
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
        return Tileset.CELL;
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

    public void setMoney(int amount) {
        money = amount;
    }

    public TETile getStandingOn() {
        return standingOn;
    }

    public void setStandingOn(TETile standing) {
        standingOn = standing;
    }

    public void setInvincible(boolean invincible) {
        this.invincible = invincible;
    }

    public boolean isInvincible() {
        return invincible;
    }

    public void toggleInvincible() {
        invincible = !invincible;
    }
}
