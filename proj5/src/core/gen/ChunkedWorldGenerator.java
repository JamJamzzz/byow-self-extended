package core.gen;

import core.Position;
import core.Room;
import tileengine.TETile;
import tileengine.Tileset;
import utils.RandomUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Deterministic chunk-based procedural world generator.
 *
 * <pre>
 * seed
 *   -&gt; partition world into chunks
 *   -&gt; generate one anchor room (+ optional extra rooms) per chunk
 *   -&gt; build a graph of neighboring chunks, weighted by inter-anchor distance
 *   -&gt; connect it with a Prim minimum spanning tree
 *   -&gt; add a small seeded set of non-tree edges for loops
 *   -&gt; carve corridors between the selected room pairs
 *   -&gt; render rooms/corridors/walls
 *   -&gt; validate connectivity
 * </pre>
 *
 * Every stage draws from its own {@link WorldRng} stream, so the layout for
 * a given seed never changes just because an unrelated stage's random-call
 * count changed.
 */
public final class ChunkedWorldGenerator {
    private static final int CHUNK_MARGIN = 1;
    private static final int MIN_ROOM_DIM = 4;
    private static final int ROOM_PLACEMENT_ATTEMPTS = 25;
    private static final double EXTRA_ROOM_PROBABILITY = 0.35;
    private static final double EXTRA_EDGE_PROBABILITY = 0.15;
    private static final int[][] CHUNK_DIRECTIONS = {{0, -1}, {0, 1}, {1, 0}, {-1, 0}};
    private static final int[][] WALL_DIRECTIONS = {
            {0, -1}, {0, 1}, {1, 0}, {-1, 0}, {-1, -1}, {-1, 1}, {1, -1}, {1, 1}
    };

    private final int width;
    private final int height;
    private final int chunkRows;
    private final int chunkCols;
    private final long seed;

    public ChunkedWorldGenerator(int width, int height, int chunkRows, int chunkCols, long seed) {
        int minChunkWidth = MIN_ROOM_DIM + 2 * CHUNK_MARGIN + 2;
        int minChunkHeight = MIN_ROOM_DIM + 2 * CHUNK_MARGIN + 2;

        this.width = Math.max(width, 2 * minChunkWidth);
        this.height = Math.max(height, 2 * minChunkHeight);
        this.chunkCols = Math.max(2, Math.min(chunkCols, this.width / minChunkWidth));
        this.chunkRows = Math.max(2, Math.min(chunkRows, this.height / minChunkHeight));
        this.seed = seed;
    }

    public GeneratedWorld generate() {
        WorldRng rng = new WorldRng(seed);

        TETile[][] grid = blankGrid();
        List<Chunk> chunks = partitionChunks();
        linkNeighbors(chunks);

        for (Chunk chunk : chunks) {
            placeRoomsInChunk(chunk, rng.rooms());
        }

        List<ChunkEdge> candidateEdges = PrimMinimumSpanningTree.candidateEdges(chunks, rng.graph());
        PrimMinimumSpanningTree.Result mst = PrimMinimumSpanningTree.build(chunks, candidateEdges, rng.graph());
        List<ChunkEdge> extraEdges = PrimMinimumSpanningTree.selectExtraEdges(
                candidateEdges, mst.mstEdges, EXTRA_EDGE_PROBABILITY, rng.graph());

        List<Room> allRooms = collectRooms(chunks);
        renderRooms(grid, allRooms);

        List<RoomConnection> corridors = buildCorridors(chunks, mst.mstEdges, extraEdges);
        carveCorridors(grid, corridors, rng.corridors());
        addWalls(grid);

        Room spawnRoom = chunks.get(0).anchorRoom();
        validateConnectivity(grid, spawnRoom, allRooms);

        return new GeneratedWorld(grid, allRooms, chunks, mst.mstEdges, extraEdges, spawnRoom, seed, rng.entitySeed());
    }

    private TETile[][] blankGrid() {
        TETile[][] grid = new TETile[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                grid[x][y] = Tileset.NOTHING;
            }
        }
        return grid;
    }

    private List<Chunk> partitionChunks() {
        List<Chunk> chunks = new ArrayList<>();
        int chunkWidth = width / chunkCols;
        int chunkHeight = height / chunkRows;

        int id = 0;
        for (int r = 0; r < chunkRows; r++) {
            for (int c = 0; c < chunkCols; c++) {
                int x = c * chunkWidth;
                int y = r * chunkHeight;
                int w = (c == chunkCols - 1) ? width - x : chunkWidth;
                int h = (r == chunkRows - 1) ? height - y : chunkHeight;
                chunks.add(new Chunk(id++, r, c, x, y, w, h));
            }
        }
        return chunks;
    }

    private void linkNeighbors(List<Chunk> chunks) {
        for (Chunk chunk : chunks) {
            for (int[] dir : CHUNK_DIRECTIONS) {
                int nr = chunk.row() + dir[0];
                int nc = chunk.col() + dir[1];
                if (nr >= 0 && nr < chunkRows && nc >= 0 && nc < chunkCols) {
                    chunk.addNeighbor(chunks.get(nr * chunkCols + nc));
                }
            }
        }
    }

    /**
     * Places the chunk's anchor room (always succeeds, since chunk bounds are
     * sized to guarantee room), then attempts a small number of optional
     * extra rooms via bounded rejection sampling. If an extra room's attempt
     * budget is exhausted, generation simply moves on without it -- the
     * chunk was already usable because of its anchor room.
     */
    private void placeRoomsInChunk(Chunk chunk, Random roomRng) {
        Room anchor = sampleRoom(chunk, roomRng);
        chunk.addRoom(anchor);

        if (RandomUtils.bernoulli(roomRng, EXTRA_ROOM_PROBABILITY)) {
            for (int attempt = 0; attempt < ROOM_PLACEMENT_ATTEMPTS; attempt++) {
                Room candidate = sampleRoom(chunk, roomRng);
                if (!overlapsAny(candidate, chunk.rooms())) {
                    chunk.addRoom(candidate);
                    break;
                }
            }
            // If every attempt overlapped, the chunk deterministically keeps
            // just its anchor room -- a valid, fully-connected outcome.
        }
    }

    private Room sampleRoom(Chunk chunk, Random roomRng) {
        int maxW = Math.max(MIN_ROOM_DIM, chunk.width() - 2 * CHUNK_MARGIN);
        int maxH = Math.max(MIN_ROOM_DIM, chunk.height() - 2 * CHUNK_MARGIN);
        int minW = Math.min(MIN_ROOM_DIM, maxW);
        int minH = Math.min(MIN_ROOM_DIM, maxH);

        int w = minW + (maxW > minW ? roomRng.nextInt(maxW - minW + 1) : 0);
        int h = minH + (maxH > minH ? roomRng.nextInt(maxH - minH + 1) : 0);

        int freeX = Math.max(1, chunk.width() - w - 2 * CHUNK_MARGIN);
        int freeY = Math.max(1, chunk.height() - h - 2 * CHUNK_MARGIN);
        int x = chunk.x() + CHUNK_MARGIN + roomRng.nextInt(freeX);
        int y = chunk.y() + CHUNK_MARGIN + roomRng.nextInt(freeY);

        return new Room(x, y, w, h);
    }

    private boolean overlapsAny(Room candidate, List<Room> existing) {
        for (Room room : existing) {
            if (candidate.overlapsWithMargin(room, 1)) {
                return true;
            }
        }
        return false;
    }

    private List<Room> collectRooms(List<Chunk> chunks) {
        List<Room> rooms = new ArrayList<>();
        for (Chunk chunk : chunks) {
            rooms.addAll(chunk.rooms());
        }
        return rooms;
    }

    private void renderRooms(TETile[][] grid, List<Room> rooms) {
        for (Room room : rooms) {
            for (int x = room.x(); x < room.x() + room.width(); x++) {
                for (int y = room.y(); y < room.y() + room.height(); y++) {
                    grid[x][y] = Tileset.FLOOR;
                }
            }
        }
    }

    /** Resolves chunk-graph edges to the anchor-room pairs to carve, plus any intra-chunk extra-room links. */
    private List<RoomConnection> buildCorridors(List<Chunk> chunks, List<ChunkEdge> mstEdges, List<ChunkEdge> extraEdges) {
        List<RoomConnection> connections = new ArrayList<>();
        for (ChunkEdge edge : mstEdges) {
            connections.add(new RoomConnection(edge.a().anchorRoom(), edge.b().anchorRoom()));
        }
        for (ChunkEdge edge : extraEdges) {
            connections.add(new RoomConnection(edge.a().anchorRoom(), edge.b().anchorRoom()));
        }
        for (Chunk chunk : chunks) {
            List<Room> rooms = chunk.rooms();
            for (int i = 1; i < rooms.size(); i++) {
                connections.add(new RoomConnection(chunk.anchorRoom(), rooms.get(i)));
            }
        }
        return connections;
    }

    private void carveCorridors(TETile[][] grid, List<RoomConnection> corridors, Random corridorRng) {
        for (RoomConnection link : corridors) {
            int x1 = link.a().centerAtX();
            int y1 = link.a().centerAtY();
            int x2 = link.b().centerAtX();
            int y2 = link.b().centerAtY();

            if (corridorRng.nextBoolean()) {
                carveHorizontal(grid, x1, x2, y1);
                carveVertical(grid, y1, y2, x2);
            } else {
                carveVertical(grid, y1, y2, x1);
                carveHorizontal(grid, x1, x2, y2);
            }
        }
    }

    private void carveHorizontal(TETile[][] grid, int xFrom, int xTo, int y) {
        for (int x = Math.min(xFrom, xTo); x <= Math.max(xFrom, xTo); x++) {
            grid[x][y] = Tileset.FLOOR;
        }
    }

    private void carveVertical(TETile[][] grid, int yFrom, int yTo, int x) {
        for (int y = Math.min(yFrom, yTo); y <= Math.max(yFrom, yTo); y++) {
            grid[x][y] = Tileset.FLOOR;
        }
    }

    private void addWalls(TETile[][] grid) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (grid[x][y] == Tileset.NOTHING && hasFloorBeside(grid, x, y)) {
                    grid[x][y] = Tileset.WALL;
                }
            }
        }
    }

    private boolean hasFloorBeside(TETile[][] grid, int x, int y) {
        for (int[] dir : WALL_DIRECTIONS) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            if (nx >= 0 && nx < width && ny >= 0 && ny < height && grid[nx][ny] == Tileset.FLOOR) {
                return true;
            }
        }
        return false;
    }

    /**
     * Defensive invariant check: every room center must be reachable from the
     * spawn room through floor tiles. The chunk graph is built so this can
     * never fail; a failure here means generation has a real bug.
     */
    private void validateConnectivity(TETile[][] grid, Room spawnRoom, List<Room> rooms) {
        Set<Position> reachable = reachableFloor(grid, spawnRoom.center());
        for (Room room : rooms) {
            if (!reachable.contains(room.center())) {
                throw new IllegalStateException("Generated room " + room.center() + " is unreachable from spawn");
            }
        }
    }

    private Set<Position> reachableFloor(TETile[][] grid, Position start) {
        Set<Position> visited = new HashSet<>();
        Deque<Position> frontier = new ArrayDeque<>();
        visited.add(start);
        frontier.add(start);

        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!frontier.isEmpty()) {
            Position cur = frontier.poll();
            for (int[] dir : dirs) {
                int nx = cur.x + dir[0];
                int ny = cur.y + dir[1];
                if (nx < 0 || nx >= width || ny < 0 || ny >= height) {
                    continue;
                }
                Position next = new Position(nx, ny);
                if (visited.contains(next)) {
                    continue;
                }
                TETile tile = grid[nx][ny];
                if (tile == Tileset.FLOOR) {
                    visited.add(next);
                    frontier.add(next);
                }
            }
        }
        return visited;
    }
}
