package core;

import org.junit.Test;
import tileengine.TETile;
import tileengine.Tileset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Checkpoint save/restore must be a real, defensively-copied snapshot. */
public class CheckpointTest {

    private World buildWorld(long seed) {
        World world = new World(70, 50, 4, 4, seed);
        world.placePlayer();
        world.placeEnemy();
        world.placeTrap();
        world.placeHealingItems();
        world.placeCoins();
        return world;
    }

    @Test
    public void restoringUndoesHealthAndPositionChanges() {
        World world = buildWorld(42);
        world.saveCheckpoint();

        Player player = world.getPlayer();
        Position before = player.getPosition();
        player.setHealth(1);
        player.setPosition(new Position(before.x + 1, before.y));

        assertTrue(world.restoreCheckpoint());
        assertEquals(before, player.getPosition());
        assertEquals(3, player.getHealth());
    }

    @Test
    public void restoringUndoesDirectionMoneyAndInvincibility() {
        World world = buildWorld(11);
        Player player = world.getPlayer();

        Direction directionBefore = player.getDirection();
        boolean invincibleBefore = player.isInvincible();
        TETile avatarBefore = player.getAvatar();

        world.saveCheckpoint();

        Direction mutatedDirection = directionBefore == Direction.LEFT ? Direction.RIGHT : Direction.LEFT;
        player.setDirection(mutatedDirection);
        player.toggleInvincible();
        player.addMoney(5);

        assertTrue("test setup should actually change direction", player.getDirection() != directionBefore);
        assertTrue("test setup should actually flip invincibility", invincibleBefore != player.isInvincible());

        assertTrue(world.restoreCheckpoint());

        assertEquals("direction must be restored exactly", directionBefore, player.getDirection());
        assertEquals("invincibility must be restored exactly", invincibleBefore, player.isInvincible());
        assertEquals("money must be restored exactly", 0, player.getMoney());
        assertEquals("avatar tile must be re-derived from the restored invincibility flag, not stored separately",
                avatarBefore, player.getAvatar());
    }

    @Test
    public void restoringBringsBackACollectedCoin() {
        World world = buildWorld(7);

        // Find a coin tile and its position.
        Position coinPos = null;
        var grid = world.getGrid();
        for (int x = 0; x < grid.length && coinPos == null; x++) {
            for (int y = 0; y < grid[0].length; y++) {
                if (grid[x][y] == Tileset.UNLOCKED_DOOR) {
                    coinPos = new Position(x, y);
                    break;
                }
            }
        }
        assertNotNull("test seed should place at least one coin", coinPos);

        world.saveCheckpoint();
        assertNotNull(world.getInteractableAt(coinPos));

        // Simulate collecting it.
        world.getInteractableAt(coinPos).interact(world.getPlayer());
        world.removeInteractableAt(coinPos);
        assertEquals(Tileset.FLOOR, world.getGrid()[coinPos.x][coinPos.y]);

        assertTrue(world.restoreCheckpoint());
        assertEquals(Tileset.UNLOCKED_DOOR, world.getGrid()[coinPos.x][coinPos.y]);
        assertNotNull("interactable map must be restored too, not just the tile",
                world.getInteractableAt(coinPos));
    }

    @Test
    public void mutatingTheGridAfterCheckpointDoesNotAffectTheSnapshot() {
        World world = buildWorld(99);
        world.saveCheckpoint();

        var grid = world.getGrid();
        grid[5][5] = Tileset.LOCKED_DOOR;

        assertTrue(world.restoreCheckpoint());
        assertFalse(Tileset.LOCKED_DOOR.equals(world.getGrid()[5][5]));
    }
}
