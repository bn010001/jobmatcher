package com.jobmatcher.api.service.storage;

import com.jobmatcher.api.config.StorageProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;

@Service
public class LocalStorageService {

    private final Path rootDir;

    public LocalStorageService(StorageProperties props) {
        this.rootDir = Paths.get(props.getLocal().getRootDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootDir);
        } catch (IOException e) {
            throw new RuntimeException("Impossibile creare root storage dir: " + this.rootDir, e);
        }
    }

    public String save(byte[] bytes, String storedFilename) {
        try {
            Path target = rootDir.resolve(storedFilename).normalize();
            if (!target.startsWith(rootDir)) throw new RuntimeException("Path traversal detected");
            Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
            return storedFilename; // path relativo (filename)
        } catch (IOException e) {
            throw new RuntimeException("Errore salvataggio file", e);
        }
    }

    public Resource loadAsResource(String storedFilename) {
        try {
            Path file = rootDir.resolve(storedFilename).normalize();
            if (!file.startsWith(rootDir)) throw new RuntimeException("Path traversal detected");
            Resource res = new UrlResource(file.toUri());
            if (!res.exists()) throw new RuntimeException("File non trovato: " + storedFilename);
            return res;
        } catch (MalformedURLException e) {
            throw new RuntimeException("Errore caricamento file", e);
        }
    }

    public void delete(String storedFilename) {
        try {
            Path p = rootDir.resolve(storedFilename).normalize();
            Files.deleteIfExists(p);
        } catch (IOException e) {
            throw new IllegalStateException("Impossibile cancellare file: " + storedFilename, e);
        }
    }

    public Path resolvePath(String storedFilename) {
        Path file = rootDir.resolve(storedFilename).normalize();
        if (!file.startsWith(rootDir)) throw new RuntimeException("Path traversal detected");
        return file;
    }


}
