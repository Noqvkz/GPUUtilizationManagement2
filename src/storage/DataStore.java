package storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import model.AppState;

// Every saved model class must implement Serializable (directly or by inheritance).
public class DataStore {
    private final File file;

    public DataStore() {
        this("data/gpu-state.ser");
    }

    public DataStore(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("A save-file path is required.");
        }
        file = new File(fileName);
    }

    public String getFilePath() {
        return file.getAbsolutePath();
    }

    public AppState load() throws IOException {
        // A missing file is normal on the first run.
        if (!file.exists()) {
            return new AppState();
        }
        try (ObjectInputStream input = new ObjectInputStream(new FileInputStream(file))) {
            Object object = input.readObject();
            if (!(object instanceof AppState)) {
                throw new IOException("The file does not contain an AppState.");
            }
            AppState state = (AppState) object;
            state.validate();
            return state;
        } catch (ClassNotFoundException e) {
            throw new IOException("Could not load " + getFilePath()
                    + ": a saved Java class is unavailable.", e);
        } catch (IOException e) {
            throw new IOException("Could not load " + getFilePath()
                    + ": " + e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new IOException("Could not load " + getFilePath()
                    + ": invalid saved data (" + e.getMessage() + ").", e);
        }
        // The GUI must show the error and stop loading
    }

    public void save(AppState state) throws IOException {
        if (state == null) {
            throw new IOException("Cannot save a missing application state.");
        }
        try {
            state.validate();
            File parent = file.getAbsoluteFile().getParentFile();
            if (!parent.exists() && !parent.mkdirs()) {
                throw new IOException("Could not create the data folder.");
            }
            // Writing a temporary file first, so a serialization failure keeps the old save.
            File temporaryFile = new File(file.getAbsolutePath() + ".tmp");
            try (ObjectOutputStream output = new ObjectOutputStream(
                    new FileOutputStream(temporaryFile))) {
                output.writeObject(state);
            }
            Files.move(temporaryFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IOException("Could not save " + getFilePath()
                    + ": " + e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new IOException("Could not save " + getFilePath()
                    + ": " + e.getMessage(), e);
        }
    }
}
