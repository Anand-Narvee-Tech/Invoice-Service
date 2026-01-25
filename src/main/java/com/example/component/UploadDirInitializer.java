package com.example.component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class UploadDirInitializer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @PostConstruct
    public void createUploadDir() {
        Path path = Paths.get(uploadDir);

        try {
            Files.createDirectories(path);

            if (!Files.isWritable(path)) {
                throw new IllegalStateException(
                    "Upload directory exists but is not writable: " + uploadDir
                );
            }

        } catch (Exception e) {
            throw new IllegalStateException(
                "Upload directory initialization failed: " + uploadDir +
                ". Fix filesystem permissions or set UPLOAD_DIR environment variable.",
                e
            );
        }
    }
}
