package core;

import tileengine.TETile;
import tileengine.Tileset;

import java.util.*;

public class World {
    /** Class Attributes **/
    private final int WIDTH;
    private final int HEIGHT;

    private final int CHUNK_ROWS;
    private final int CHUNK_COLS;

    private final int MIN_ROOM_W = 5;
    private final int MAX_ROOM_W = 12;
    private final int MIN_ROOM_H = 5;
    private final int MAX_ROOM_H = 12;

    private final int MIN_ROOM_NUM = 1;
    private final int MAX_ROOM_NUM = 1;

    private final Random random;

    private final long seed;

    /** Grid **/
    private TETile[][] grid;
    
    /** Key Data Structures **/
    private List<Chunk> chunks;
    private List<Edge> edges; //The hallway
    private List<Room> allRooms;
    private final int[][] wallDirections = new int[][]{{0, -1}, {0, 1}, {1, 0}, {-1, 0}, {-1,-1}, {-1,1}, {1,-1}, {1,1}};
    private final int[][] chunkDirections = new int[][]{{0, -1}, {0, 1}, {1, 0}, {-1, 0}};
    private final Player player;
    private List<Position> traps;

    /** Constructor **/
    public World(int width, int height, int chunkRows, int chunkCols, long seed) {
        //To prevent the user add oversize chunkCols and chunkRows, get the minChunkWidth and minChunkHeight
        int minChunkWidth = MIN_ROOM_W + 2;
        int minChunkHeight = MIN_ROOM_H + 2;

        //Set the minimum world width and height based on chunk
        int minWorldWidth = 2 * minChunkWidth;
        int minWorldHeight = 2 * minChunkHeight;

        this.WIDTH = Math.max(width, minWorldWidth);
        this.HEIGHT = Math.max(height, minWorldHeight);

        this.CHUNK_COLS = Math.max(2, Math.min(chunkCols, WIDTH / minChunkWidth));
        this.CHUNK_ROWS = Math.max(2, Math.min(chunkRows, HEIGHT / minChunkHeight));

        this.random = new Random(seed);

        player = new Player();
        this.seed = seed;

        initializeGrid();

        generateRooms();
        connectRooms();

        renderRooms();
        renderHallWays();

        addWalls();

        validate();
    }

    private void initializeGrid() {
        grid = new TETile[WIDTH][HEIGHT];
        //Set everything as NOTHING

        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                grid[x][y] = Tileset.NOTHING;
            }
        }
    }

    private void generateChunks() {
        chunks = new ArrayList<>();

        //Divide grid into chunks based on CHUNK_ROWS × CHUNK_COLS
        int chunkWidth = WIDTH / CHUNK_COLS;
        int chunkHeight = HEIGHT / CHUNK_ROWS;

        for (int r = 0; r < CHUNK_ROWS; r++) {
            for (int c = 0; c < CHUNK_COLS; c++) {
                Chunk chunk = new Chunk();
                chunk.x = c * chunkWidth;
                chunk.y = r * chunkHeight;
                //Dealing with the remainder area
                if (c == CHUNK_COLS - 1) {
                    chunk.w = WIDTH - chunk.x;
                } else {
                    chunk.w = chunkWidth;
                }

                if (r == CHUNK_ROWS - 1) {
                    chunk.h = HEIGHT - chunk.y;
                } else {
                    chunk.h = chunkHeight;
                }

                chunk.rooms = new ArrayList<>();
                chunks.add(chunk);
            }
        }

        assignNeighbors();
    }

    private void assignNeighbors() {
        //Find and assign the neighbors for every chunks
        for (int r = 0; r < CHUNK_ROWS; r++) {
            for (int c = 0; c < CHUNK_COLS; c++) {
                //Get the current cols and rows
                int idx = r * CHUNK_COLS + c;
                Chunk curr = chunks.get(idx);
                curr.siblings = new ArrayList<>();

                for (int[] direction : chunkDirections) {
                    int nextRow = r + direction[0];
                    int nextCol = c + direction[1];

                    if (nextRow >= 0 && nextRow < CHUNK_ROWS && nextCol >= 0 && nextCol < CHUNK_COLS) {
                        int nextIdx = nextRow * CHUNK_COLS + nextCol;
                        curr.siblings.add(chunks.get(nextIdx));
                    }
                }
            }
        }
    }

    private void generateRooms() {
        allRooms = new ArrayList<>();

        int targetRooms = random.nextInt(6) + 8;
        int attempts = 0;

        while (allRooms.size() < targetRooms && attempts < 200) {
            attempts++;

            int roomW = random.nextInt(MAX_ROOM_W - MIN_ROOM_W + 1) + MIN_ROOM_W;
            int roomH = random.nextInt(MAX_ROOM_H - MIN_ROOM_H + 1) + MIN_ROOM_H;

            int x = random.nextInt(WIDTH - roomW - 2) + 1;
            int y = random.nextInt(HEIGHT - roomH - 2) + 1;

            Room room = new Room();
            room.x = x;
            room.y = y;
            room.w = roomW;
            room.h = roomH;

            if (!overlapsExisting(room)) {
                allRooms.add(room);
            }
        }
    }

    private boolean overlapsExisting(Room newRoom) {
        for (Room existing : allRooms) {
            boolean xOverlap = newRoom.x < existing.x + existing.w + 1
                    && newRoom.x + newRoom.w + 1 > existing.x;
            boolean yOverlap = newRoom.y < existing.y + existing.h + 1
                    && newRoom.y + newRoom.h + 1 > existing.y;

            if (xOverlap && yOverlap) {
                return true;
            }
        }
        return false;
    }

    private void connectRooms() {
        edges = new ArrayList<>();

        if (allRooms.size() <= 1) {
            return;
        }

        for (int i = 0; i < allRooms.size() - 1; i++) {
            edges.add(new Edge(allRooms.get(i), allRooms.get(i + 1)));
        }

        int extras = random.nextInt(4) + 2;
        for (int i = 0; i < extras; i++) {
            Room a = allRooms.get(random.nextInt(allRooms.size()));
            Room b = allRooms.get(random.nextInt(allRooms.size()));
            if (a != b && !isDuplicateConnection(a, b)) {
                edges.add(new Edge(a, b));
            }
        }
    }

    //private void connectChunks() {
        //edges = new ArrayList<>();

        // Connected each rooms in every chunks
        //for (Chunk chunk : chunks) {
            //connectRoomsInChunk(chunk);
        //}

        //Connect the critical path with BFS
        //connectChunkMST();

        //for (Chunk chunk : chunks) {
            //List<Chunk> neighbors = chunk.siblings;
            //if (neighbors.isEmpty()) {
                //continue;
            //}

            //Shuffle the rooms to make sure they're connected pseudorandomly based on the seed
            //List<Chunk> shuffled = new ArrayList<>(neighbors);
            //Collections.shuffle(shuffled, random);

            //int maxConnections = Math.min(4, shuffled.size());

            //int connections = random.nextInt(maxConnections + 1); // from 0 to 3(max)

           // for (int i = 0; i < connections; i++) {
                //Chunk other = shuffled.get(i);

                //Room a = getRandomRoom(chunk);
                //Room b = getRandomRoom(other);

                //if (a != null && b != null && a != b && !isDuplicateConnection(a, b)) {
                    //edges.add(new Edge(a, b));
                //}
            //}
        //}
    //}

    //Using Prim's algorithm based on pseudorandomness
    private void connectChunkMST() {
        Set<Chunk> visited = new HashSet<>();
        List<Chunk> fringe = new ArrayList<>();

        Chunk start = chunks.get(random.nextInt(chunks.size()));
        visited.add(start);
        fringe.add(start);

        while (!fringe.isEmpty()) {
            Chunk cur = fringe.remove(random.nextInt(fringe.size()));

            List<Chunk> neighbors = new ArrayList<>(cur.siblings);
            Collections.shuffle(neighbors, random);

            for (Chunk neighbor : neighbors) {
                if (!visited.contains(neighbor)) {
                    Room a = getRandomRoom(cur);
                    Room b = getRandomRoom(neighbor);

                    if (a != null && b != null && !isDuplicateConnection(a, b)) {
                        edges.add(new Edge(a, b));
                    }

                    visited.add(neighbor);
                    fringe.add(neighbor);
                }
            }
        }
    }

    private Room getRandomRoom(Chunk chunk) {
        if (chunk.rooms == null || chunk.rooms.isEmpty()) {
            return null;
        }
        int idx = random.nextInt(chunk.rooms.size());
        return chunk.rooms.get(idx);
    }

    //Check if the edge already existed in edges
    private boolean isDuplicateConnection(Room a, Room b) {
        for (Edge e : edges) {
            if ((e.a == a && e.b == b) || (e.a == b && e.b == a)) {
                return true;
            }
        }
        return false;
    }

    private void renderRooms() {
        //Draw the rooms (floors) on the grid
        for (Room room : allRooms) {
            for (int x = room.x; x < room.x + room.w; x++) {
                for (int y = room.y; y < room.y + room.h; y++) {
                    grid[x][y] = Tileset.FLOOR;
                }
            }
        }
    }

    private void renderHallWays() {
        //Draw the hallways on the grid
        for (Edge e : edges) {
            int x1 = e.a.centerAtX();
            int y1 = e.a.centerAtY();
            int x2 = e.b.centerAtX();
            int y2 = e.b.centerAtY();

            //Pseudorandomly generate two hallways
            if (random.nextBoolean()) {
                for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
                    grid[x][y1] = Tileset.FLOOR;
                }

                for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                    grid[x2][y] = Tileset.FLOOR;
                }
            } else {
                for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                    grid[x1][y] = Tileset.FLOOR;
                }

                for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
                    grid[x][y2] = Tileset.FLOOR;
                }
            }
        }
    }

    private void addWalls() {
        for (int x = 0; x < WIDTH - 1; x++) {
            for (int y = 0; y < HEIGHT - 1; y++) {
                if (grid[x][y] == Tileset.NOTHING) {
                    //Use a helper method to detect whether there're floor besides it or not
                    if (hasFloorBeside(x, y)) {
                        grid[x][y] = Tileset.WALL;
                    }
                }
            }
        }
    }

    private boolean hasFloorBeside(int x, int y) {
        for (int[] direction : wallDirections) {
            int nextX = x + direction[0];
            int nextY = y + direction[1];

            //Prune the invalid location
            if (nextX >= 0 && nextX < WIDTH && nextY >= 0 && nextY < HEIGHT) {
                if (grid[nextX][nextY] == Tileset.FLOOR) {
                    return true;
                }
            }
        }
        return false;
    }

    private void validate() {
//        //Check the whole world is valid or not
//        //Check whether there are isolated area, rooms, or dead-ends
//        //If there exists, remove the error wall
//        for (int x = 1; x < WIDTH - 1; x++) {
//            for (int y = 1; y < HEIGHT - 1; y++) {
//                if (grid[x][y] == Tileset.WALL) {
//                    // F W F
//                    if (grid[x - 1][y] == Tileset.FLOOR &&
//                            grid[x + 1][y] == Tileset.FLOOR) {
//
//                        grid[x][y] = Tileset.FLOOR;
//                        continue;
//                    }
//
//                    //F
//                    //W
//                    //F
//                    if (grid[x][y - 1] == Tileset.FLOOR &&
//                            grid[x][y + 1] == Tileset.FLOOR) {
//
//                        grid[x][y] = Tileset.FLOOR;
//                    }
//                }
//            }
//        }
        //Fixing the bottom
//        for (int x = 0; x < WIDTH; x++) {
//            if (grid[x][1] == Tileset.FLOOR) {
//                grid[x][1] = Tileset.WALL;
//            }
//        }
//        //Fixing the left
//        for (int y = 0; y < HEIGHT; y++) {
//            if (grid[1][y] == Tileset.FLOOR) {
//                grid[1][y] = Tileset.WALL;
//            }
//        }
    }

    public void placePlayer() {
        Room spawnRoom = allRooms.get(0);

        int spawnX = spawnRoom.centerAtX();
        int spawnY = spawnRoom.centerAtY();
        player.setPosition(new Position(spawnX, spawnY));
        grid[spawnX][spawnY] = player.getAvator();
    }

    public void placeEnemy() {
        for (Room room : allRooms) {
            int enemyCount = random.nextInt(3); // 0,1,2

            for (int i = 0; i < enemyCount; i++) {
                placeEnemyInRooms(room);
            }
        }
    }

    private void placeEnemyInRooms(Room room) {
        int x = random.nextInt(room.w) + room.x;
        int y = random.nextInt(room.h) + room.y;

        if (grid[x][y] == Tileset.FLOOR){
            grid[x][y] = Tileset.ENEMY;
        }

    }

    public void placeTrap() {
        traps = new ArrayList<>();
        for (Room room : allRooms) {
            int enemyCount = random.nextInt(3); // 0,1,2

            for (int i = 0; i < enemyCount; i++) {
                placeTrapInRooms(room);
            }
        }
    }

    private void placeTrapInRooms(Room room) {
        int x = random.nextInt(room.w - 2) + room.x + 1;
        int y = random.nextInt(room.h - 2) + room.y + 1;

        if (grid[x][y] == Tileset.FLOOR){
            grid[x][y] = Tileset.TRAP;
        }
    }

    public void placeHealingItems() {
        for (Room room : allRooms) {
            if (random.nextBoolean()) {
                int x = random.nextInt(room.w - 2) + room.x + 1;
                int y = random.nextInt(room.h - 2) + room.y + 1;

                if (grid[x][y] == Tileset.FLOOR) {
                    grid[x][y] = Tileset.FLOWER;
                }
            }
        }
    }

    public void placeCoins() {
        for (Room room : allRooms) {
            int coinCount = random.nextInt(3) + 1;

            for (int i = 0; i < coinCount; i++) {
                int x = random.nextInt(room.w - 2) + room.x + 1;
                int y = random.nextInt(room.h - 2) + room.y + 1;

                if (grid[x][y] == Tileset.FLOOR) {
                    grid[x][y] = Tileset.UNLOCKED_DOOR;
                }
            }
        }
    }

    public int countCoins() {
        int count = 0;
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                if (grid[x][y] == Tileset.UNLOCKED_DOOR) {
                    count++;
                }
            }
        }
        return count;
    }

    private boolean inBounds(int x, int y) {
        return x >= 0 && x < grid.length &&
                y >= 0 && y < grid[0].length;
    }

    public TETile[][] getGrid() {
        return grid;
    }

    public Player getPlayer() {
        return player;
    }

    public long getSeed() {
        return seed;
    }
}
