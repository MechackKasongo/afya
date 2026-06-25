package com.afya.platform.user.service;

import com.afya.platform.user.dto.CredentialsLogPreviewResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class CredentialsLogService {

    public record CredentialLine(String loggedAt, String username, String fullName, String password) {
    }

    private static final int PREVIEW_MAX_CHARS = 12_000;
    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final Path logPath;

    public CredentialsLogService(@Value("${app.credentials-log.path:./data/credentials-log.txt}") String path) {
        this.logPath = Path.of(path).toAbsolutePath().normalize();
    }

    @PostConstruct
    void migrateLegacyLogFiles() {
        Path cwd = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        List<Path> legacyCandidates = List.of(
                cwd.resolve("data/credentials-log.txt"),
                cwd.resolve("user-service/data/credentials-log.txt"));
        for (Path legacy : legacyCandidates) {
            if (!Files.isRegularFile(legacy)) {
                continue;
            }
            if (legacy.equals(logPath)) {
                continue;
            }
            try {
                mergeLinesFrom(Files.readString(legacy, StandardCharsets.UTF_8));
            } catch (IOException ex) {
                throw new IllegalStateException("Impossible de migrer l'ancien journal des comptes : " + legacy, ex);
            }
        }
    }

    public void append(String username, String password, String fullName) {
        try {
            if (logPath.getParent() != null) {
                Files.createDirectories(logPath.getParent());
            }
            String line = FORMAT.format(Instant.now()) + " | " + username + " | " + fullName + " | " + password
                    + System.lineSeparator();
            Files.writeString(
                    logPath,
                    line,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException ex) {
            throw new IllegalStateException("Impossible d'écrire le journal des comptes", ex);
        }
    }

    public CredentialsLogPreviewResponse preview() {
        if (!Files.exists(logPath)) {
            return new CredentialsLogPreviewResponse("", true, false, 0, 0);
        }
        try {
            String content = Files.readString(logPath, StandardCharsets.UTF_8);
            long bytes = content.getBytes(StandardCharsets.UTF_8).length;
            int lines = content.isEmpty() ? 0 : content.split("\\R").length;
            boolean truncated = content.length() > PREVIEW_MAX_CHARS;
            String shown = truncated ? content.substring(0, PREVIEW_MAX_CHARS) + "\n… (tronqué)" : content;
            return new CredentialsLogPreviewResponse(shown, content.isBlank(), truncated, bytes, lines);
        } catch (IOException ex) {
            throw new IllegalStateException("Impossible de lire le journal des comptes", ex);
        }
    }

    public byte[] readAllBytes() {
        try {
            if (!Files.exists(logPath)) {
                return new byte[0];
            }
            return Files.readAllBytes(logPath);
        } catch (IOException ex) {
            throw new IllegalStateException("Impossible de lire le journal des comptes", ex);
        }
    }

    public void delete() {
        try {
            Files.deleteIfExists(logPath);
        } catch (IOException ex) {
            throw new IllegalStateException("Impossible de supprimer le journal des comptes", ex);
        }
    }

    /**
     * Dernière entrée du journal pour cet identifiant (mot de passe connu uniquement à la création).
     */
    public Optional<CredentialLine> findLatestForUsername(String username) {
        if (username == null || username.isBlank() || !Files.exists(logPath)) {
            return Optional.empty();
        }
        try {
            String content = Files.readString(logPath, StandardCharsets.UTF_8);
            CredentialLine latest = null;
            for (String line : content.split("\\R")) {
                if (line.isBlank()) {
                    continue;
                }
                String[] parts = line.split("\\|", 4);
                if (parts.length < 4) {
                    continue;
                }
                if (!parts[1].trim().equalsIgnoreCase(username.trim())) {
                    continue;
                }
                latest = new CredentialLine(
                        parts[0].trim(),
                        parts[1].trim(),
                        parts[2].trim(),
                        parts[3].trim());
            }
            return Optional.ofNullable(latest);
        } catch (IOException ex) {
            throw new IllegalStateException("Impossible de lire le journal des comptes", ex);
        }
    }

    private void mergeLinesFrom(String legacyContent) throws IOException {
        if (legacyContent.isBlank()) {
            return;
        }
        Set<String> existing = new LinkedHashSet<>();
        if (Files.exists(logPath)) {
            for (String line : Files.readString(logPath, StandardCharsets.UTF_8).split("\\R")) {
                if (!line.isBlank()) {
                    existing.add(line.trim());
                }
            }
        } else if (logPath.getParent() != null) {
            Files.createDirectories(logPath.getParent());
        }
        StringBuilder toAppend = new StringBuilder();
        for (String line : legacyContent.split("\\R")) {
            if (line.isBlank()) {
                continue;
            }
            String normalized = line.trim();
            if (existing.add(normalized)) {
                toAppend.append(normalized).append(System.lineSeparator());
            }
        }
        if (!toAppend.isEmpty()) {
            Files.writeString(
                    logPath,
                    toAppend.toString(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        }
    }
}
