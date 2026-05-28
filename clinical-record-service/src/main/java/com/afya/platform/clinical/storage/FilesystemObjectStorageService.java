package com.afya.platform.clinical.storage;

import com.afya.platform.shared.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "filesystem", matchIfMissing = true)
public class FilesystemObjectStorageService implements ObjectStorageService {

    private final Path baseDir;

    public FilesystemObjectStorageService(
            @Value("${app.storage.filesystem.base-dir:./data/clinical-object-store}") String baseDir
    ) {
        this.baseDir = Path.of(baseDir).toAbsolutePath().normalize();
    }

    @Override
    public void put(String objectKey, InputStream data, long size, String contentType) {
        try {
            Path target = resolve(objectKey);
            Files.createDirectories(target.getParent());
            Files.copy(data, target);
        } catch (IOException e) {
            throw new IllegalStateException("Échec écriture fichier clinique : " + objectKey, e);
        }
    }

    @Override
    public InputStream get(String objectKey) {
        Path target = resolve(objectKey);
        if (!Files.isRegularFile(target)) {
            throw new NotFoundException("Fichier clinique introuvable : " + objectKey);
        }
        try {
            return Files.newInputStream(target);
        } catch (IOException e) {
            throw new IllegalStateException("Échec lecture fichier clinique : " + objectKey, e);
        }
    }

    @Override
    public boolean exists(String objectKey) {
        return Files.isRegularFile(resolve(objectKey));
    }

    private Path resolve(String objectKey) {
        Path resolved = baseDir.resolve(objectKey).normalize();
        if (!resolved.startsWith(baseDir)) {
            throw new IllegalArgumentException("Clé de stockage invalide");
        }
        return resolved;
    }
}
