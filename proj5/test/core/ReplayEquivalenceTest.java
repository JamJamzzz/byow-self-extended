package core;

import core.persistence.CommandHistory;
import core.persistence.SaveGameRepository;
import org.junit.Test;
import tileengine.TETile;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * Builds deterministic command traces, runs them live through GameEngine,
 * then persists + replays the same trace through the exact same engine
 * entry point, and checks the two runs land in the same final state.
 */
public class ReplayEquivalenceTest {
    private static final int TRACE_COUNT = 40;
    private static final int TRACE_LENGTH = 250;
    private static final char[] ALPHABET = {'w', 'a', 's', 'd', 'j', 'e'};

    private static World buildWorld(long seed) {
        World world = new World(70, 50, 4, 4, seed);
        world.placePlayer();
        world.placeEnemy();
        world.placeTrap();
        world.placeHealingItems();
        world.placeCoins();
        world.saveCheckpoint();
        return world;
    }

    private static String trace(long traceSeed) {
        Random r = new Random(traceSeed);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < TRACE_LENGTH; i++) {
            sb.append(ALPHABET[r.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }

    @Test
    public void liveAndReplayedRunsReachTheSameFinalState() throws Exception {
        java.io.File tmp = java.io.File.createTempFile("byow-replay-test", ".txt");
        tmp.deleteOnExit();
        SaveGameRepository repo = new SaveGameRepository(tmp.getPath());

        for (long traceSeed = 0; traceSeed < TRACE_COUNT; traceSeed++) {
            long worldSeed = 1000 + traceSeed;
            String commands = trace(traceSeed);

            // Live run.
            World liveWorld = buildWorld(worldSeed);
            GameEngine liveEngine = new GameEngine(liveWorld);
            CommandHistory liveHistory = CommandHistory.startingWithSeed(worldSeed);
            for (char c : commands.toCharArray()) {
                liveHistory.append(c);
                StepResult result = liveEngine.step(c);
                if (result.isTerminal()) {
                    liveEngine.restoreFromCheckpoint();
                }
            }
            String liveState = canonicalState(liveWorld);

            // Persist, then replay the saved history through a fresh World/GameEngine pair,
            // exactly as Main's replay path does.
            repo.save(liveHistory.toString());
            String saved = repo.load();

            long replaySeed = CommandHistory.extractSeed(saved);
            assertEquals(worldSeed, replaySeed);

            World replayWorld = buildWorld(replaySeed);
            GameEngine replayEngine = new GameEngine(replayWorld);
            int start = CommandHistory.commandsStartIndex(saved);
            for (int i = start; i < saved.length(); i++) {
                StepResult result = replayEngine.step(saved.charAt(i));
                if (result.isTerminal()) {
                    replayEngine.restoreFromCheckpoint();
                }
            }
            String replayState = canonicalState(replayWorld);

            assertEquals("trace " + traceSeed + ": live vs. replayed final state diverged", liveState, replayState);
        }
    }

    private String canonicalState(World world) {
        Player p = world.getPlayer();
        TETile[][] grid = world.getGrid();
        StringBuilder sb = new StringBuilder();
        sb.append(p.getPosition()).append('|')
                .append(p.getHealth()).append('|')
                .append(p.getMoney()).append('|')
                .append(p.isInvincible()).append('|')
                .append(world.countCoins()).append('|');
        for (TETile[] column : grid) {
            for (TETile tile : column) {
                sb.append(tile.character());
            }
        }
        return sb.toString();
    }
}
