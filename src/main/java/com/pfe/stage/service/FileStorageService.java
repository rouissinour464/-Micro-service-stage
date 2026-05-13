package com.pfe.stage.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    private final Path uploadLocation;

    public FileStorageService(
            @Value("${app.upload.dir:uploads/stage}") String uploadDir
    ) {
        this.uploadLocation = Paths.get(uploadDir)
                .toAbsolutePath()
                .normalize();

        try {
            Files.createDirectories(this.uploadLocation);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Impossible de créer le dossier upload",
                    e
            );
        }
    }

    /* ===================== STORE ===================== */

    public String store(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Fichier vide ou manquant");
        }

        String original = StringUtils.cleanPath(
                file.getOriginalFilename() != null
                        ? file.getOriginalFilename()
                        : ""
        );

        if (original.isBlank() || original.contains("..")) {
            throw new IllegalArgumentException("Nom de fichier invalide");
        }

        String extension = "";
        int dot = original.lastIndexOf('.');
        if (dot > 0) {
            extension = original.substring(dot);
        }

        String filename = UUID.randomUUID() + extension;

        try {
            Files.copy(
                    file.getInputStream(),
                    uploadLocation.resolve(filename),
                    StandardCopyOption.REPLACE_EXISTING
            );

            log.info("📁 Fichier uploadé : {}", filename);
            return filename;

        } catch (IOException e) {
            throw new RuntimeException("Erreur upload fichier", e);
        }
    }

    /* ===================== LOAD ===================== */

    public Path load(String filename) {

        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Nom de fichier vide");
        }

        Path file = uploadLocation.resolve(filename).normalize();

        if (!Files.exists(file)) {
            throw new IllegalArgumentException("Fichier introuvable : " + filename);
        }

        return file;
    }

    /* ===================== DELETE ===================== */

    public void delete(String filename) {

        if (filename == null || filename.isBlank()) {
            return;
        }

        try {
            Files.deleteIfExists(uploadLocation.resolve(filename).normalize());
            log.info("🗑️ Fichier supprimé : {}", filename);

        } catch (IOException e) {
            log.warn("Erreur suppression fichier : {}", filename, e);
        }
    }
}