package core;

import org.junit.Test;
import tileengine.TETile;
import verification.VerificationConfig;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * The strong checkpoint invariant this project targets: if the game is
 * checkpointed at state S, then executing command suffix C from S must
 * evolve identically no matter how many times / in what order that
 * happens. This test would fail under the pre-fix semantics, where
 * {@code World}'s runtime RNG (entity movement) was never snapshotted or
 * restored -- so a restored run's future silently diverged from the
 * original even though the grid looked identical immediately after
 * restore.
 *
 * <p>Methodology: run a prefix, checkpoint, then compare two ways of
 * reaching "checkpoint + suffix": (1) applying the suffix directly, and
 * (2) advancing the runtime RNG and move counter with a detour -- calling
 * {@code World#moveEnemies}/{@code #moveTraps}/{@code #incrementMoveCount}
 * directly, deliberately bypassing {@code GameEngine} -- restoring the
 * checkpoint, and *then* applying the same suffix. Both must land on the
 * same canonical state.
 *
 * <p>The detour intentionally does not go through {@code GameEngine.step},
 * because a real move that crosses a room boundary auto-saves a *new*
 * checkpoint ({@code World#saveCheckpointIfLeavingRoom}) -- a legitimate,
 * separate feature that would otherwise move "the checkpoint" out from
 * under this test and make it test the wrong thing. Calling the mutation
 * methods directly isolates exactly what this test is about: whether
 * restoring rolls back every piece of state that can affect the future,
 * not whether auto-checkpointing-on-room-exit also fired correctly.
 */
public class CheckpointFutureEquivalenceTest {
    private static final int SEED_COUNT = VerificationConfig.RNG_FUTURE_EQUIVALENCE_SEED_COUNT;
    private static final int PREFIX_LENGTH = VerificationConfig.RNG_FUTURE_EQUIVALENCE_PREFIX_LENGTH;
    private static final int SUFFIX_LENGTH = VerificationConfig.RNG_FUTURE_EQUIVALENCE_SUFFIX_LENGTH;
    private static final char[] ALPHABET = {'w', 'a', 's', 'd', 'j', 'e'};

    @Test
    public void identicalSuffixAfterRestoreReachesTheIdenticalFutureState() {
        for (long seed = 0; seed < SEED_COUNT; seed++) {
            long worldSeed = 9000 + seed;
            String prefix = randomTrace(seed * 3 + 1, PREFIX_LENGTH);
            String suffix = randomTrace(seed * 3 + 2, SUFFIX_LENGTH);

            // Reference: prefix, checkpoint, suffix -- straight through.
            World reference = buildWorld(worldSeed);
            GameEngine referenceEngine = new GameEngine(reference);
            applyEachStep(referenceEngine, prefix);
            reference.saveCheckpoint();
            String checkpointState = canonicalState(reference);
            applyEachStep(referenceEngine, suffix);
            String expectedFutureState = canonicalState(reference);

            // Subject: prefix, checkpoint, a detour that advances the runtime
            // RNG and move counter without touching GameEngine, restore, then
            // the *same* suffix.
            World subject = buildWorld(worldSeed);
            GameEngine subjectEngine = new GameEngine(subject);
            applyEachStep(subjectEngine, prefix);
            subject.saveCheckpoint();
            assertEquals("seed " + seed + ": a freshly-taken checkpoint must match the state it was taken from",
                    checkpointState, canonicalState(subject));

            runRngAndSchedulingDetour(subject, seed);
            subject.restoreCheckpoint();
            assertEquals("seed " + seed + ": restoring must reproduce the exact checkpointed state "
                            + "(including runtime RNG progression and move-scheduling phase), "
                            + "not just something that looks similar",
                    checkpointState, canonicalState(subject));

            applyEachStep(subjectEngine, suffix);
            String actualFutureState = canonicalState(subject);

            assertEquals("seed " + seed + ": the same command suffix from a restored checkpoint must reach "
                            + "the exact same future state (grid, player stats/direction/invincibility, "
                            + "move-scheduling phase, and remaining enemy/trap/item layout all included) "
                            + "as running that suffix the first time",
                    expectedFutureState, actualFutureState);
        }
    }

    /** Directly advances entity RNG draws and the move counter, bypassing GameEngine/room-transition checkpointing. */
    private static void runRngAndSchedulingDetour(World world, long seed) {
        Random r = new Random(seed * 3 + 3);
        int rounds = 5 + r.nextInt(6);
        for (int i = 0; i < rounds; i++) {
            world.moveEnemies();
            world.moveTraps();
            world.incrementMoveCount();
        }
    }

    private static World buildWorld(long seed) {
        World world = new World(VerificationConfig.WORLD_WIDTH, VerificationConfig.WORLD_HEIGHT,
                VerificationConfig.CHUNK_ROWS, VerificationConfig.CHUNK_COLS, seed);
        world.placePlayer();
        world.placeEnemy();
        world.placeTrap();
        world.placeHealingItems();
        world.placeCoins();
        return world;
    }

    private static void applyEachStep(GameEngine engine, String commands) {
        // Deliberately does not auto-restore on death/victory here: this test
        // is about checkpoint save/restore mechanics in isolation, not the
        // game loop's terminal-state policy (covered by ReplayEquivalenceTest).
        for (char c : commands.toCharArray()) {
            engine.step(c);
        }
    }

    private static String randomTrace(long traceSeed, int length) {
        Random r = new Random(traceSeed);
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET[r.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }

    private static String canonicalState(World world) {
        Player p = world.getPlayer();
        TETile[][] grid = world.getGrid();
        StringBuilder sb = new StringBuilder();
        sb.append(p.getPosition()).append('|')
                .append(p.getHealth()).append('|')
                .append(p.getMoney()).append('|')
                .append(p.getDirection()).append('|')
                .append(p.isInvincible()).append('|')
                .append(p.getStandingOn().description()).append('|')
                .append(world.getMoveCount()).append('|')
                .append(world.countCoins()).append('|');
        for (TETile[] column : grid) {
            for (TETile tile : column) {
                sb.append(tile.character());
            }
        }
        return sb.toString();
    }
}
