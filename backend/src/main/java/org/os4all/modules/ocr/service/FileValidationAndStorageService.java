package org.os4all.modules.ocr.service;

import org.os4all.core.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class FileValidationAndStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileValidationAndStorageService.class);

    private final Path uploadDirectory;
    private final long maxSizeBytes;

    // Magic Bytes signatures
    private static final byte[] PDF_MAGIC = new byte[]{0x25, 0x50, 0x44, 0x46}; // %PDF
    private static final byte[] PNG_MAGIC = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}; // PNG signature
    private static final byte[] JPEG_MAGIC = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}; // JPEG SOI

    public record StoredFileMetadata(
            String originalFilename,
            String storedPath,
            String mimeType,
            long fileSize,
            String sha256Hex
    ) {}

    public FileValidationAndStorageService(
            @Value("${app.storage.upload-dir:${java.io.tmpdir}/os4all-uploads}") String uploadDir,
            @Value("${app.storage.max-file-size-bytes:10485760}") long maxSizeBytes
    ) {
        this.uploadDirectory = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.maxSizeBytes = maxSizeBytes;
        try {
            Files.createDirectories(this.uploadDirectory);
            log.info("Initialized report upload directory at: {}", this.uploadDirectory);
        } catch (IOException e) {
            log.error("Failed to create upload directory at: {}", this.uploadDirectory, e);
            throw new IllegalStateException("Could not create report upload directory: " + this.uploadDirectory, e);
        }
    }

    /**
     * Validates file size, non-emptiness, and magic bytes. Then computes SHA-256 and saves file to isolated directory.
     */
    public StoredFileMetadata validateAndStore(MultipartFile file, UUID userId) {
        if (file == null || file.isEmpty()) {
            throw new ApiException("Uploaded file must not be empty");
        }

        if (file.getSize() > maxSizeBytes) {
            throw new ApiException("File size exceeds maximum allowed limit of " + (maxSizeBytes / (1024 * 1024)) + " MB");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "unnamed_report";
        }

        // Validate Magic Bytes to prevent malicious or spoofed file uploads
        String detectedMimeType;
        byte[] header = new byte[8];
        try (InputStream is = file.getInputStream()) {
            int read = is.read(header);
            if (read < 3) {
                throw new ApiException("File is too small or corrupted to verify format");
            }
            detectedMimeType = detectMimeType(header, read);
        } catch (IOException e) {
            throw new ApiException("Failed to read uploaded file header: " + e.getMessage());
        }

        if (detectedMimeType == null) {
            throw new ApiException("Unsupported file format. Only PDF, PNG, and JPG/JPEG files are accepted.");
        }

        // Compute SHA-256 hash & store safely
        String sha256Hex;
        Path targetPath;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] fileBytes = file.getBytes();
            byte[] hashBytes = digest.digest(fileBytes);
            sha256Hex = HexFormat.of().formatHex(hashBytes);

            // User-isolated directory structure
            Path userDir = this.uploadDirectory.resolve(userId.toString());
            Files.createDirectories(userDir);

            String extension = getExtensionForMimeType(detectedMimeType);
            String safeFileName = UUID.randomUUID() + extension;
            targetPath = userDir.resolve(safeFileName);

            Files.write(targetPath, fileBytes);
            log.info("Stored uploaded file for user {} at {}", userId, targetPath);
        } catch (NoSuchAlgorithmException | IOException e) {
            log.error("Failed to store file: {}", e.getMessage(), e);
            throw new ApiException("Failed to store uploaded report securely: " + e.getMessage());
        }

        return new StoredFileMetadata(
                originalFilename,
                targetPath.toString(),
                detectedMimeType,
                file.getSize(),
                sha256Hex
        );
    }

    private String detectMimeType(byte[] header, int read) {
        // PDF check (%PDF)
        if (read >= 4 &&
                header[0] == PDF_MAGIC[0] &&
                header[1] == PDF_MAGIC[1] &&
                header[2] == PDF_MAGIC[2] &&
                header[3] == PDF_MAGIC[3]) {
            return "application/pdf";
        }

        // PNG check
        if (read >= 8 &&
                header[0] == PNG_MAGIC[0] &&
                header[1] == PNG_MAGIC[1] &&
                header[2] == PNG_MAGIC[2] &&
                header[3] == PNG_MAGIC[3] &&
                header[4] == PNG_MAGIC[4] &&
                header[5] == PNG_MAGIC[5] &&
                header[6] == PNG_MAGIC[6] &&
                header[7] == PNG_MAGIC[7]) {
            return "image/png";
        }

        // JPEG check (FF D8 FF)
        if (read >= 3 &&
                header[0] == JPEG_MAGIC[0] &&
                header[1] == JPEG_MAGIC[1] &&
                header[2] == JPEG_MAGIC[2]) {
            return "image/jpeg";
        }

        return null;
    }

    private String getExtensionForMimeType(String mimeType) {
        return switch (mimeType) {
            case "application/pdf" -> ".pdf";
            case "image/png" -> ".png";
            case "image/jpeg" -> ".jpg";
            default -> ".bin";
        };
    }
}
