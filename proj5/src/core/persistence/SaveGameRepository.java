package core.persistence;

import utils.FileUtils;

import java.io.File;

/** Reads and writes the single save-file that stores a game's full input history. */
public final class SaveGameRepository {
    private final String path;

    public SaveGameRepository(String path) {
        this.path = path;
    }

    public void save(String history) {
        FileUtils.writeFile(path, history);
    }

    public String load() {
        if (!FileUtils.fileExists(path)) {
            return "";
        }
        return FileUtils.readFile(path).trim();
    }

    public void delete() {
        new File(path).delete();
    }
}
