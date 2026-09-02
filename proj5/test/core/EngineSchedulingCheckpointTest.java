package core;

import org.junit.Test;
import verification.VerificationConfig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Isolates the movement-scheduling counter (which decides when enemies/traps
 * take their next turn) from every other piece of checkpoint state. Before
 * this fix, {@code GameEngine.restoreFromCheckpoint()} unconditionally reset
 * this counter to 0 instead of restoring the value it had when the
 * checkpoint was taken, which would desynchronize enemy/trap turn timing
 * from that point on even though the grid/player looked correct
 * immediately after restore.
 */
public class EngineSchedulingCheckpointTest {

    @Test
    public void restoringAtANonZeroMoveCountRestoresThatExactPhaseNotZero() {
        World world = new World(VerificationConfig.WORLD_WIDTH, VerificationConfig.WORLD_HEIGHT,
                VerificationConfig.CHUNK_ROWS, VerificationConfig.CHUNK_COLS, 4242);
        world.placePlayer();
        world.placeEnemy();
        world.placeTrap();

        world.incrementMoveCount();
        world.incrementMoveCount();
        world.incrementMoveCount();
        assertEquals(3, world.getMoveCount());

        world.saveCheckpoint();

        // Advance further so the "reset to 0" bug and the "restore exact value"
        // fix would disagree on what comes back.
        world.incrementMoveCount();
        world.incrementMoveCount();
        assertEquals(5, world.getMoveCount());

        assertTrue(world.restoreCheckpoint());
        assertEquals("restoring must bring back the exact move-scheduling phase (3), not reset it to 0",
                3, world.getMoveCount());
    }

    @Test
    public void gameEngineRestoreDelegatesToWorldsCheckpointedMoveCount() {
        World world = new World(VerificationConfig.WORLD_WIDTH, VerificationConfig.WORLD_HEIGHT,
                VerificationConfig.CHUNK_ROWS, VerificationConfig.CHUNK_COLS, 777);
        world.placePlayer();
        world.placeEnemy();
        world.placeTrap();

        GameEngine engine = new GameEngine(world);

        world.incrementMoveCount();
        world.incrementMoveCount();
        world.incrementMoveCount();
        world.incrementMoveCount();
        world.incrementMoveCount(); // moveCount == 5
        world.saveCheckpoint();

        world.incrementMoveCount(); // diverge to 6 after the checkpoint
        assertTrue(engine.restoreFromCheckpoint());

        assertEquals("GameEngine.restoreFromCheckpoint() must not independently reset scheduling state",
                5, world.getMoveCount());
    }
}
