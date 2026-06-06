package com.ec.mokshitha_collections.service.admin;

import com.ec.mokshitha_collections.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Saves uploaded images to a local filesystem root and returns the public URL
 * the browser can fetch them from. Filename is a fresh UUID so no caller-
 * supplied path component is ever trusted (defends against path traversal).
 *
 * Configure via:
 *   app.uploads.root         (filesystem dir, default ./uploads)
 *   app.uploads.url-prefix   (URL prefix exposed by WebMvcConfig, default /uploads)
 */
@Service
@Slf4j
public class ImageStorageService {

    private static final Set<String> ALLOWED_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private static final long MAX_BYTES = 5L * 1024 * 1024; // 5MB

    @Value("${app.uploads.root:./uploads}")
    private String uploadsRoot;

    @Value("${app.uploads.url-prefix:/uploads}")
    private String urlPrefix;

    private Path rootPath;

    @PostConstruct
    void init() throws IOException {
        rootPath = Path.of(uploadsRoot).toAbsolutePath().normalize();
        Files.createDirectories(rootPath);
        log.info("Image storage root: {}", rootPath);
    }

    /**
     * Stores a file under <root>/<subdir>/<uuid>.<ext> and returns the
     * public URL <urlPrefix>/<subdir>/<uuid>.<ext>.
     */
    public String store(MultipartFile file, String subdir) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException("File exceeds 5MB limit");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("Only JPEG, PNG, WebP and GIF images are allowed");
        }

        String safeSubdir = sanitizeSubdir(subdir);
        Path targetDir = rootPath.resolve(safeSubdir).normalize();
        // Defence-in-depth — guarantee we stayed under rootPath.
        if (!targetDir.startsWith(rootPath)) {
            throw new BadRequestException("Invalid upload path");
        }

        try {
            Files.createDirectories(targetDir);
            String ext = extensionFor(contentType);
            String filename = UUID.randomUUID() + ext;
            Path target = targetDir.resolve(filename);
            try (var in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return urlPrefix + "/" + safeSubdir + "/" + filename;
        } catch (IOException e) {
            log.error("Failed to store upload", e);
            throw new BadRequestException("Failed to store image");
        }
    }

    /** Best-effort delete — used when a record's image is being replaced or deleted. */
    public void delete(String publicUrl) {
        if (publicUrl == null || !publicUrl.startsWith(urlPrefix + "/")) return;
        String relative = publicUrl.substring(urlPrefix.length() + 1);
        Path candidate = rootPath.resolve(relative).normalize();
        if (!candidate.startsWith(rootPath)) return;
        try {
            Files.deleteIfExists(candidate);
        } catch (IOException e) {
            log.warn("Could not delete {}: {}", candidate, e.getMessage());
        }
    }

    private static String sanitizeSubdir(String subdir) {
        if (subdir == null || subdir.isBlank()) return "misc";
        // Allow only alnum / dash / underscore / forward slash. No "..", no backslashes.
        String cleaned = subdir.replaceAll("[^a-zA-Z0-9_\\-/]", "");
        if (cleaned.contains("..") || cleaned.startsWith("/")) {
            throw new BadRequestException("Invalid upload subdirectory");
        }
        return cleaned;
    }

    private static String extensionFor(String contentType) {
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg" -> ".jpg";
            case "image/png"  -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif"  -> ".gif";
            default           -> "";
        };
    }
}
