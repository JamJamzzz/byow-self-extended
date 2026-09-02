# BYOW Self-Extended

This repository contains my independently extended version of UC Berkeley CS 61B BYOW, with custom
chunk-based generation, graph connectivity, checkpoint/replay, pathfinding, and verification work built
on the course scaffold. The CS 61B-provided pieces are the tile engine (`tileengine/`) and course utility
libraries (`utils/`, `library-sp26/`); the chunk-based procedural generation, the Prim minimum-spanning-tree
connectivity algorithm, the checkpoint/replay system, BFS click-to-move, and the test/verification suite
are the custom work built on top of that scaffold.

All source lives under [`proj5/`](proj5/).

## Architecture

```
UI / Input (StdDraw)
        |
        v
      Main  --------------------------------------+
        |                                          |
        v                                          v
  GameEngine.step(char) <---- BfsPathfinder    Screens (rendering only)
        |
        v
      World
        |
        +---- Player (position/health/money/direction/invincibility)
        +---- enemies / traps / Interactable items (Coin, HealingItem)
        +---- Checkpoint (defensive snapshot)
        +---- DeterministicRng (checkpointable runtime RNG)

ChunkedWorldGenerator
        |
        +---- Chunk partitioning + per-chunk room placement
        +---- chunk adjacency graph (Manhattan anchor-room cost)
        +---- PrimMinimumSpanningTree (weighted Prim, PriorityQueue frontier)
        +---- seeded non-tree edges (loops)
        +---- corridor carving / wall rendering
        `---> GeneratedWorld (consumed once by World's constructor)

core.persistence
        +---- CommandHistory (input-log format)
        `---- SaveGameRepository (save.txt read/write)
```

`GameEngine.step(char)` is the single authoritative state-transition path: live keyboard input, BFS
click-to-move (converted to the same command characters via `GameEngine.directionCommand`), and replayed
save-file history all call it. There is no second, parallel implementation of movement/combat/interaction
logic anywhere else.

## Procedural Generation

The world is partitioned into a `CHUNK_ROWS x CHUNK_COLS` grid of spatial regions. Generating rooms
independently *inside* each chunk (rather than scattering them over the whole map and connecting them in
whatever order they happened to be created) gives generation an explicit spatial structure to reason
about, and keeps room placement a local, boundable problem: each chunk gets one anchor room via a fixed
number of attempts, plus optional extra rooms via bounded rejection sampling (never an unbounded retry
loop -- if the attempt budget is exhausted, the chunk simply keeps just its anchor room, a valid
outcome).

Chunks that are grid-adjacent become candidate edges in a graph, weighted by the Manhattan distance
between their anchor rooms' centers (a cheap, deterministic proxy for corridor length). A real Prim
minimum spanning tree -- a seeded start chunk, a `PriorityQueue` frontier, repeatedly absorbing the
cheapest edge that reaches an unvisited chunk -- connects every chunk with a low-total-cost backbone and
a mathematically guaranteed `N-1` edges for `N` chunks. A small seeded fraction of the *non*-tree edges
are then added back in, so the map has loops and alternate routes instead of being a strict tree.
Corridors are carved as a separate step from graph selection: first the algorithm decides *what* is
connected, then a deterministic L-shaped carve decides *how*.

## Determinism

Generation splits the world seed into independent per-stage streams (`WorldRng`: rooms / graph
tie-breaking / corridors), so changing how many random calls one stage makes can never shift what another
stage produces. Runtime randomness that must survive a checkpoint -- enemy and trap movement -- uses a
separate `DeterministicRng` (SplitMix64, explicit `long` state) rather than `java.util.Random`, because
`java.util.Random`'s internal state isn't part of its public API and can't be snapshotted without
reflection or serialization hacks. `DeterministicRng.snapshotState()`/`restoreState(long)` make that
state trivially checkpointable.

Same seed + same command sequence always produces the same observable state, live input and replayed
input share the same `GameEngine.step()` entry point, and restoring a checkpoint restores *every* piece
of logical execution state that can affect the future -- not just the visible grid, but the player's
direction/invincibility, the move-scheduling counter that decides when enemies/traps next act, and the
runtime RNG's exact progression. `CheckpointFutureEquivalenceTest` verifies this directly: it runs the
same command suffix twice -- once straight through, once after an unrelated detour and a restore -- and
requires both to land on the identical state.

## Algorithms

- **Weighted Prim MST** (`PrimMinimumSpanningTree`) over the chunk adjacency graph, edge cost = Manhattan
  distance between anchor-room centers.
- **BFS shortest-path** (`BfsPathfinder`) for click-to-move, decoupled from `World` (it takes a
  `Position -> boolean` walkability predicate), using `ArrayDeque` + parent-pointer reconstruction.
- **Kruskal + Union-Find** is used *only* as an independent test-side oracle
  (`PrimIsMinimumSpanningTreeTest`) to verify Prim's output is actually minimum-cost, not merely connected
  and acyclic. It is not part of the production generation path.

## Verification

Verification numbers below come only from `verification.VerificationRunner`, which runs the suites
through `JUnitCore` programmatically and writes `evidence/byow-generation-verification.json` and
`evidence/byow-replay-verification.json` from the actual `Result` (`getRunCount()`/`getFailureCount()`/
runtime), never a hand-typed count. Seed/trace counts are read from one shared
`verification.VerificationConfig` that the test classes and the runner both import, so the same number
can't drift between the code and the report.

As of the last run captured in those files:

- World-connectivity sweep: **1000 seeds**, every generated room reachable from spawn.
- Prim MST invariants (N-1 edges, full connectivity, acyclic, no duplicate edges): **500 seeds**.
- Independent Prim-vs-Kruskal minimum-cost cross-check: **1000 seeds**.
- Generation determinism (identical seed -> identical layout): **300 seeds**.
- Live vs. save/replay state equivalence: **40 traces x 250 commands**.
- Checkpoint future-equivalence (RNG + move-scheduling restoration): **50 seeds**.
- Full JUnit suite: see `evidence/*.json` for the exact run/failure counts and wall-clock time from the
  most recent run.

Reproduce locally from `proj5/`:

```bash
CP=$(ls ../library-sp26/*.jar | tr '\n' ';')
javac --release 17 -encoding UTF-8 -cp "$CP" -d out $(find src -name "*.java")
javac --release 17 -encoding UTF-8 -cp "${CP}out" -d out-test $(find test -name "*.java")
java -cp "${CP}out;out-test" verification.VerificationRunner
```

(Adjust the `library-sp26` path if your checkout layout differs; see `proj5/.idea/libraries/library_sp26.xml`.)

## Known limitations

- No GUI/StdDraw smoke test is automated (headless environment); correctness evidence for gameplay comes
  from the automated suites exercising `GameEngine` directly across thousands of steps.
- `Position`'s `x`/`y` fields are `public final` rather than fully encapsulated behind accessors -- an
  intentional, documented trade-off to limit the blast radius of an already-large refactor, not an
  oversight.
