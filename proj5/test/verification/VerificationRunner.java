package verification;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates the evidence JSON artifacts in {@code evidence/} directly from
 * an actual {@link JUnitCore} run -- it never hand-types a pass/fail count,
 * a seed count, or a runtime. Seed/trace counts come from
 * {@link VerificationConfig}, the same constants the test classes
 * themselves use, so the number quoted in the JSON is provably the number
 * that was actually exercised.
 *
 * <p>Run as: {@code java -cp <classpath> verification.VerificationRunner}
 * from the project root (so the relative {@code evidence/} path and the
 * {@code git} subprocess resolve correctly). Exits 0 only if every suite in
 * every group passed; otherwise exits 1 and the written JSON says so.
 */
public final class VerificationRunner {

    private static final Class<?>[] GENERATION_SUITES = {
            core.gen.PrimMstInvariantsTest.class,
            core.gen.PrimIsMinimumSpanningTreeTest.class,
            core.gen.DeterminismTest.class,
            core.gen.WorldConnectivityTest.class,
    };

    private static final Class<?>[] REPLAY_STATE_SUITES = {
            core.ReplayEquivalenceTest.class,
            core.BfsPathfinderTest.class,
            core.PositionTest.class,
            core.CheckpointTest.class,
            core.EngineSchedulingCheckpointTest.class,
            core.CheckpointFutureEquivalenceTest.class,
    };

    public static void main(String[] args) throws IOException {
        Path evidenceDir = Path.of("evidence");
        Files.createDirectories(evidenceDir);

        // Captured exactly once, before either evidence file is written: writing
        // the first artifact mutates the working tree, which would otherwise make
        // gitDirty() report a false "dirty" for whichever group runs second. Both
        // artifacts must describe the same tested source state.
        String testedCommit = gitCommit();
        boolean testedWorktreeDirty = gitDirty();

        boolean generationOk = runGroup("generation", GENERATION_SUITES,
                evidenceDir.resolve("byow-generation-verification.json"), testedCommit, testedWorktreeDirty);
        boolean replayOk = runGroup("replay_state", REPLAY_STATE_SUITES,
                evidenceDir.resolve("byow-replay-verification.json"), testedCommit, testedWorktreeDirty);

        if (!generationOk || !replayOk) {
            System.err.println("VerificationRunner: at least one suite group failed; see evidence JSON for details.");
            System.exit(1);
        }
    }

    private static boolean runGroup(String suiteGroup, Class<?>[] classes, Path outputPath,
                                     String testedCommit, boolean testedWorktreeDirty) throws IOException {
        JUnitCore core = new JUnitCore();
        long startNanos = System.nanoTime();
        Result result = core.run(classes);
        long runtimeMs = (System.nanoTime() - startNanos) / 1_000_000;

        JsonObject json = new JsonObject();
        json.addProperty("schema_version", 1);
        json.addProperty("generated_at", Instant.now().toString());
        json.addProperty("tested_commit", testedCommit);
        json.addProperty("tested_worktree_dirty", testedWorktreeDirty);
        json.addProperty("suite_group", suiteGroup);
        json.addProperty("command_or_runner", "verification.VerificationRunner -> org.junit.runner.JUnitCore.run("
                + classNames(classes) + ")");

        JsonObject junit = new JsonObject();
        junit.addProperty("run_count", result.getRunCount());
        junit.addProperty("failure_count", result.getFailureCount());
        junit.addProperty("ignored_count", result.getIgnoreCount());
        junit.addProperty("runtime_ms", runtimeMs);
        json.add("junit", junit);

        json.add("verification", suiteScope(suiteGroup));
        json.addProperty("verification_status", result.wasSuccessful() ? "verified" : "failed");

        if (!result.wasSuccessful()) {
            JsonArray failures = new JsonArray();
            for (Failure f : result.getFailures()) {
                JsonObject fj = new JsonObject();
                fj.addProperty("test", f.getTestHeader());
                fj.addProperty("message", String.valueOf(f.getMessage()));
                failures.add(fj);
            }
            json.add("failures", failures);
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8))) {
            writer.println(gson.toJson(json));
        }

        System.out.println(suiteGroup + ": " + result.getRunCount() + " run, "
                + result.getFailureCount() + " failed, " + runtimeMs + " ms -> " + outputPath);

        return result.wasSuccessful();
    }

    private static JsonArray suiteScope(String suiteGroup) {
        JsonArray suites = new JsonArray();
        if (suiteGroup.equals("generation")) {
            suites.add(suite("core.gen.PrimMstInvariantsTest", VerificationConfig.PRIM_INVARIANTS_SEED_COUNT,
                    "N participating chunks -> N-1 MST edges; all chunks reachable via MST edges; "
                            + "MST edge set contains no cycle (independent Union-Find check); "
                            + "no duplicate edges between MST and extra edges"));
            suites.add(suite("core.gen.PrimIsMinimumSpanningTreeTest", VerificationConfig.PRIM_VS_KRUSKAL_SEED_COUNT,
                    "sum(Prim MST edge costs) == sum(independent Kruskal+DSU MST edge costs) over the same "
                            + "candidate chunk-graph edges"));
            suites.add(suite("core.gen.DeterminismTest", VerificationConfig.DETERMINISM_SEED_COUNT,
                    "same seed produces a byte-identical canonical tile-character hash across two independent "
                            + "generate() calls"));
            suites.add(suite("core.gen.WorldConnectivityTest", VerificationConfig.WORLD_CONNECTIVITY_SEED_COUNT,
                    "every generated room center is reachable from spawn through floor tiles; no null/ragged "
                            + "grid tiles (no out-of-bounds writes)"));
        } else {
            JsonObject replay = new JsonObject();
            replay.addProperty("name", "core.ReplayEquivalenceTest");
            replay.addProperty("trace_count", VerificationConfig.REPLAY_TRACE_COUNT);
            replay.addProperty("trace_length", VerificationConfig.REPLAY_TRACE_LENGTH);
            replay.addProperty("invariant", "live GameEngine.step() run vs. save-then-replay through the same "
                    + "GameEngine.step() entry point reach the same canonical final state");
            suites.add(replay);

            suites.add(suite("core.CheckpointFutureEquivalenceTest", VerificationConfig.RNG_FUTURE_EQUIVALENCE_SEED_COUNT,
                    "prefix, checkpoint, RNG/move-count-advancing detour, restore, suffix reaches the identical "
                            + "canonical state as prefix, checkpoint, suffix run directly -- detects incomplete "
                            + "runtime-RNG or move-scheduling checkpoint restoration"));

            JsonObject scheduling = new JsonObject();
            scheduling.addProperty("name", "core.EngineSchedulingCheckpointTest");
            scheduling.addProperty("invariant", "restoring a checkpoint restores the exact move-scheduling "
                    + "counter it was taken at, not zero");
            suites.add(scheduling);

            JsonObject checkpoint = new JsonObject();
            checkpoint.addProperty("name", "core.CheckpointTest");
            checkpoint.addProperty("invariant", "defensive copy of the grid; collected-item interactable state "
                    + "and grid tile both restored; direction/invincibility/avatar-derivation restored exactly");
            suites.add(checkpoint);

            JsonObject bfs = new JsonObject();
            bfs.addProperty("name", "core.BfsPathfinderTest");
            bfs.addProperty("invariant", "start==target, unreachable/blocked target, shortest-path correctness, "
                    + "no repeated tiles, deterministic reconstruction");
            suites.add(bfs);

            JsonObject position = new JsonObject();
            position.addProperty("name", "core.PositionTest");
            position.addProperty("invariant", "equals/hashCode contract");
            suites.add(position);
        }
        return suites;
    }

    private static JsonObject suite(String name, int seedCount, String invariant) {
        JsonObject o = new JsonObject();
        o.addProperty("name", name);
        o.addProperty("seed_count", seedCount);
        o.addProperty("invariant", invariant);
        return o;
    }

    private static String classNames(Class<?>[] classes) {
        List<String> names = new ArrayList<>();
        for (Class<?> c : classes) {
            names.add(c.getName());
        }
        return String.join(", ", names);
    }

    /** The commit this verification run is testing. Call once, before any evidence file is written. */
    private static String gitCommit() {
        String sha = runGit("rev-parse", "HEAD");
        return sha != null ? sha.trim() : "unknown";
    }

    /** Whether the working tree had uncommitted changes at the moment this run started. */
    private static boolean gitDirty() {
        String status = runGit("status", "--porcelain");
        return status != null && !status.trim().isEmpty();
    }

    private static String runGit(String... args) {
        try {
            List<String> command = new ArrayList<>();
            command.add("git");
            for (String a : args) {
                command.add(a);
            }
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exit = process.waitFor();
            return exit == 0 ? output : null;
        } catch (IOException | InterruptedException e) {
            return null;
        }
    }
}
