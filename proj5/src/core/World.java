package core;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class World {
    /** Class Attributes **/
    private final int WIDTH;
    private final int HEIGHT;

    private final int CHUNK_ROWS;
    private final int CHUNK_COLS;

    private final int MIN_ROOM_W = 4;
    private final int MAX_ROOM_W = 8;
    private final int MIN_ROOM_H = 4;
    private final int MAX_ROOM_H = 8;

    private final Random random;

    /** Grid **/
    private int[][] grid;
    
    /** Key Data Structures **/
    private List<Chunk> chunks;
    private List<Edge> edges; //The hallway
    
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
        this.WIDTH = width;
        this.HEIGHT = height;
        this.CHUNK_ROWS = chunkRows;
        this.CHUNK_COLS = chunkCols;
        this.random = new Random(seed);

        initializeGrid();

        generateChunks();
        generateRoomsInChunks();

        connectChunks();

        renderRooms();
        renderHallWays();

        addWalls();
    }

    private void initializeGrid() {
        grid = new int[WIDTH][HEIGHT];

        //Set everything as NOTHING
    }

    private void generateChunks() {
        chunks = new ArrayList<>();

        //Divide grid into chunks based on CHUNK_ROWS × CHUNK_COLS
    }

    private void assignNeighbors() {
        //Find and assign the neighbors for every chunks
    }

    private void generateRoomsInChunks() {
        //Generate rooms for each chunks
    }

    private void connectChunks() {
        edges = new ArrayList<>();

        // Connected each chunks
    }

    private void renderRooms() {
        //Draw the rooms (floors) on the grid
    }

    private void renderHallWays() {
        //Draw the hallways on the grid
    }

    private void addWalls() {
    }

    private boolean isValid() {
        //Check the whole world is valid or not
        //Check whether there are isolated area, rooms, or dead-ends
        return true;
    }

    public int[][] getGrid() {
        return grid;
    }
}
