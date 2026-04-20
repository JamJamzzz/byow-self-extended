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

    private final int MIN_ROOM_W = 4;
    private final int MAX_ROOM_W = 8;
    private final int MIN_ROOM_H = 2;
    private final int MAX_ROOM_H = 6;

    private final int MIN_ROOM_NUM = 2;
    private final int MAX_ROOM_NUM = 4;

    private final Random random;

    /** Grid **/
    private TETile[][] grid;
    
    /** Key Data Structures **/
    private List<Chunk> chunks;
    private List<Edge> edges; //The hallway
    private List<Room> allRooms;
    private final int[][] wallDirections = new int[][]{{0, -1}, {0, 1}, {1, 0}, {-1, 0}, {-1,-1}, {-1,1}, {1,-1}, {1,1}};
    private final int[][] chunkDirections = new int[][]{{0, -1}, {0, 1}, {1, 0}, {-1, 0}};
    
    /** Inner Class **/
    private class Room {
        int x;
        int y;
        int w;
        int h;

        int centerAtX() {
            return x + w / 2;
        }

        int centerAtY() {
            return y + h / 2;
        }
    }

    private class Chunk {
        int x, y, w, h;

        List<Room> rooms;
        List<Chunk> siblings;
    }
    
    private class Edge {
        Room a;
        Room b;
        
        Edge(Room a, Room b) {
            this.a = a;
            this.b = b;
        }
    }

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

        initializeGrid();

        generateChunks();
        generateRoomsInChunks();

        connectChunks();

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

    private void generateRoomsInChunks() {
        //Generate rooms for each chunks
        //Get the next num in the fixed series based on seed, which is the key of pseudorandom

        allRooms = new ArrayList<>();

        for (Chunk chunk : chunks) {
            int maxW = Math.min(MAX_ROOM_W, chunk.w - 2);
            int maxH = Math.min(MAX_ROOM_H, chunk.h - 2);
            //Just skip this chunk if the chunk is way too small
            if (maxW < MIN_ROOM_W || maxH < MIN_ROOM_H) {
                continue;
            }

            //Pseudorandomly generate the room num
            int roomNum = random.nextInt(MAX_ROOM_NUM - MIN_ROOM_NUM + 1) + MIN_ROOM_NUM;

            for (int i = 0; i < roomNum; i++) {
                //Generate the width and height of the room
                int roomW = random.nextInt(maxW -MIN_ROOM_W + 1) + MIN_ROOM_W;
                int roomH = random.nextInt(maxH - MIN_ROOM_H + 1) + MIN_ROOM_H;

                //The location of the room, make sure they didnt touch the edges
                int xRange = chunk.w - roomW - 2;
                int yRange = chunk.h - roomH - 2;

                if (xRange <= 0 || yRange <= 0) {
                    continue;
                }
                int x = chunk.x + random.nextInt(xRange) + 1;
                int y = chunk.y + random.nextInt(yRange) + 1;

                //Create a new room
                Room room = new Room();
                room.x = x;
                room.y = y;
                room.w = roomW;
                room.h = roomH;

                chunk.rooms.add(room);
                allRooms.add(room);
            }
        }
    }

    private void connectRoomsInChunk(Chunk chunk) {
        List<Room> rooms = chunk.rooms;

        if (rooms.size() <= 1) {
            return;
        }

        for (int i = 0; i < rooms.size() - 1; i++) {
            Room a = rooms.get(i);
            Room b = rooms.get(i + 1);

            edges.add(new Edge(a, b));
        }
    }


    private void connectChunks() {
        edges = new ArrayList<>();

        // Connected each rooms in every chunks
        for (Chunk chunk : chunks) {
            connectRoomsInChunk(chunk);
        }

        //Connect the critical path with BFS
        connectChunkMST();

        for (Chunk chunk : chunks) {
            List<Chunk> neighbors = chunk.siblings;
            if (neighbors.isEmpty()) {
                continue;
            }

            //Shuffle the rooms to make sure they're connected pseudorandomly based on the seed
            List<Chunk> shuffled = new ArrayList<>(neighbors);
            Collections.shuffle(shuffled, random);

            int maxConnections = Math.min(4, shuffled.size());

            int connections = random.nextInt(maxConnections + 1); // from 0 to 3(max)

            for (int i = 0; i < connections; i++) {
                Chunk other = shuffled.get(i);

                Room a = getRandomRoom(chunk);
                Room b = getRandomRoom(other);

                if (a != null && b != null && a != b && !isDuplicateConnection(a, b)) {
                    edges.add(new Edge(a, b));
                }
            }
        }
    }

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
        for (int x = 1; x < WIDTH - 1; x++) {
            for (int y = 1; y < HEIGHT - 1; y++) {
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
        for (int x = 0; x < WIDTH; x++) {
            if (grid[x][1] == Tileset.FLOOR) {
                grid[x][1] = Tileset.WALL;
            }
        }
        //Fixing the left
        for (int y = 0; y < HEIGHT; y++) {
            if (grid[1][y] == Tileset.FLOOR) {
                grid[1][y] = Tileset.WALL;
            }
        }
    }

    public TETile[][] getGrid() {
        return grid;
    }
}
