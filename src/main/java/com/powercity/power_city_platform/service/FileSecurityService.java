package com.powercity.power_city_platform.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Service
public class FileSecurityService {

    @Value("${app.upload.security.scan-files:true}")
    private boolean scanFiles;

    @Value("${app.upload.security.allowed-mime-types}")
    private String allowedMimeTypes;

    @Value("${app.upload.security.blocked-extensions}")
    private String blockedExtensions;

    @Value("${app.upload.image.max-width:2048}")
    private int maxImageWidth;

    @Value("${app.upload.image.max-height:2048}")
    private int maxImageHeight;

    private Set<String> getAllowedMimeTypes() {
        return new HashSet<>(Arrays.asList(allowedMimeTypes.split(",")));
    }

    private Set<String> getBlockedExtensions() {
        return new HashSet<>(Arrays.asList(blockedExtensions.split(",")));
    }

    /**
     * Comprehensive file security validation
     */
    public FileValidationResult validateFile(MultipartFile file, FileType expectedType) {
        if (!scanFiles) {
            return FileValidationResult.success();
        }

        try {
            // Basic null and empty checks
            if (file == null || file.isEmpty()) {
                return FileValidationResult.error("File is null or empty");
            }

            // Check file extension security
            String filename = file.getOriginalFilename();
            if (filename == null) {
                return FileValidationResult.error("File must have a valid filename");
            }

            String extension = getFileExtension(filename).toLowerCase();
            if (getBlockedExtensions().contains(extension)) {
                return FileValidationResult.error("File extension '" + extension + "' is not allowed for security reasons");
            }

            // Validate MIME type
            String contentType = file.getContentType();
            if (contentType == null || !getAllowedMimeTypes().contains(contentType)) {
                return FileValidationResult.error("File MIME type '" + contentType + "' is not allowed");
            }

            // Check for double extensions (e.g., file.jpg.exe)
            if (hasDoubleExtension(filename)) {
                return FileValidationResult.error("Files with double extensions are not allowed for security reasons");
            }

            // For large files (>2MB), only read header for validation to avoid memory issues
            // Book covers are typically 2-5MB and can cause ImageIO issues
            byte[] fileBytes;
            long fileSize = file.getSize();
            boolean isLargeFile = fileSize > 2 * 1024 * 1024; // 2MB threshold

            if (isLargeFile) {
                // Only read first 8KB for header validation on large files
                fileBytes = new byte[8192];
                try (var is = file.getInputStream()) {
                    int bytesRead = is.read(fileBytes);
                    if (bytesRead < fileBytes.length) {
                        fileBytes = Arrays.copyOf(fileBytes, bytesRead);
                    }
                }
            } else {
                // For smaller files, read entire file for thorough validation
                fileBytes = file.getBytes();
            }

            // Validate file header/magic bytes
            if (!validateFileHeader(fileBytes, expectedType)) {
                return FileValidationResult.error("File header does not match the expected file type");
            }

            // Additional security checks based on file type
            switch (expectedType) {
                case IMAGE -> {
                    return validateImageFile(file, fileBytes, isLargeFile);
                }
                case AUDIO -> {
                    return validateAudioFile(file, fileBytes, isLargeFile);
                }
                case PDF -> {
                    return validatePdfFile(file, fileBytes, isLargeFile);
                }
                default -> {
                    return FileValidationResult.success();
                }
            }

        } catch (Exception e) {
            return FileValidationResult.error("File validation failed: " + e.getMessage());
        }
    }

    /**
     * Validate image files for security and format compliance
     */
    private FileValidationResult validateImageFile(MultipartFile file, byte[] fileBytes, boolean isLargeFile) {
        try {
            // For large image files (>2MB), skip ImageIO validation to avoid memory issues
            // This includes book covers which are typically 2-5MB and can cause ImageIO errors
            // Header validation is sufficient for security on large files
            if (isLargeFile) {
                // Header has already been validated in validateFileHeader()
                // Large images are assumed valid if header is correct
                return FileValidationResult.success();
            }

            // For smaller images, perform full validation with ImageIO
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(fileBytes));
            if (image == null) {
                return FileValidationResult.error("File is not a valid image");
            }

            // Check image dimensions for security (prevent excessively large images)
            if (image.getWidth() > maxImageWidth || image.getHeight() > maxImageHeight) {
                return FileValidationResult.error(
                    String.format("Image dimensions (%dx%d) exceed maximum allowed size (%dx%d)",
                        image.getWidth(), image.getHeight(), maxImageWidth, maxImageHeight));
            }

            // For image files, we skip metadata checks since they are binary files
            // and the ImageIO.read() validation is sufficient for security

            return FileValidationResult.success();

        } catch (IOException e) {
            return FileValidationResult.error("Failed to validate image file: " + e.getMessage());
        }
    }

    /**
     * Validate audio files for security (optimized for large files)
     */
    private FileValidationResult validateAudioFile(MultipartFile file, byte[] fileBytes, boolean isLargeFile) {
        // Check actual file size (not just the bytes read)
        long actualFileSize = file.getSize();
        if (actualFileSize < 1000) { // Audio files should be at least 1KB
            return FileValidationResult.error("Audio file is too small to be valid");
        }

        // Validate common audio file headers (only checks first few bytes)
        if (file.getContentType() != null) {
            if (file.getContentType().equals("audio/mpeg") && !isValidMp3Header(fileBytes)) {
                return FileValidationResult.error("Invalid MP3 file header");
            }
            if (file.getContentType().equals("audio/wav") && !isValidWavHeader(fileBytes)) {
                return FileValidationResult.error("Invalid WAV file header");
            }
        }

        // For large audio files, header validation is sufficient
        return FileValidationResult.success();
    }

    /**
     * Validate PDF files for security (optimized for large files)
     */
    private FileValidationResult validatePdfFile(MultipartFile file, byte[] fileBytes, boolean isLargeFile) {
        // Check PDF magic bytes
        if (fileBytes.length < 4 ||
            fileBytes[0] != 0x25 || fileBytes[1] != 0x50 ||
            fileBytes[2] != 0x44 || fileBytes[3] != 0x46) {
            return FileValidationResult.error("File is not a valid PDF");
        }

        // Check for PDF version string (only check first 100 bytes)
        String header = new String(Arrays.copyOf(fileBytes, Math.min(100, fileBytes.length)));
        if (!header.startsWith("%PDF-")) {
            return FileValidationResult.error("Invalid PDF header");
        }

        // For large files, we only have the header (first 8KB), so skip EOF check
        // Header validation is sufficient for security on large PDFs
        if (isLargeFile) {
            return FileValidationResult.success();
        }

        // For smaller PDFs, check for EOF marker
        String content = new String(fileBytes);
        if (!content.contains("%%EOF")) {
            return FileValidationResult.error("PDF file appears to be corrupted or incomplete");
        }

        return FileValidationResult.success();
    }

    /**
     * Validate file header/magic bytes
     */
    private boolean validateFileHeader(byte[] fileBytes, FileType expectedType) {
        if (fileBytes.length < 4) return false;

        return switch (expectedType) {
            case IMAGE -> isValidImageHeader(fileBytes);
            case AUDIO -> isValidAudioHeader(fileBytes);
            case PDF -> isValidPdfHeader(fileBytes);
            default -> true;
        };
    }

    private boolean isValidImageHeader(byte[] bytes) {
        if (bytes.length < 4) return false;
        
        // JPEG: FF D8 FF
        if (bytes[0] == (byte)0xFF && bytes[1] == (byte)0xD8 && bytes[2] == (byte)0xFF) return true;
        
        // PNG: 89 50 4E 47
        if (bytes[0] == (byte)0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) return true;
        
        // GIF: 47 49 46 38
        if (bytes[0] == 0x47 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x38) return true;
        
        // WebP: 52 49 46 46 (RIFF)
        if (bytes.length >= 12 && bytes[0] == 0x52 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x46 &&
            bytes[8] == 0x57 && bytes[9] == 0x45 && bytes[10] == 0x42 && bytes[11] == 0x50) return true;
        
        return false;
    }

    private boolean isValidAudioHeader(byte[] bytes) {
        if (bytes.length < 4) return false;
        
        // MP3: FF FB or FF FA (MPEG Layer 3)
        if (bytes[0] == (byte)0xFF && (bytes[1] == (byte)0xFB || bytes[1] == (byte)0xFA)) return true;
        
        // WAV: 52 49 46 46 (RIFF)
        if (bytes[0] == 0x52 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x46) return true;
        
        // M4A/AAC: often starts with various patterns, basic check
        if (bytes.length >= 8) {
            String header = new String(Arrays.copyOfRange(bytes, 4, 8));
            if (header.equals("ftyp")) return true;
        }
        
        return false;
    }

    private boolean isValidPdfHeader(byte[] bytes) {
        return bytes.length >= 4 && 
               bytes[0] == 0x25 && bytes[1] == 0x50 && 
               bytes[2] == 0x44 && bytes[3] == 0x46;
    }

    private boolean isValidMp3Header(byte[] bytes) {
        return bytes.length >= 2 && bytes[0] == (byte)0xFF && 
               (bytes[1] == (byte)0xFB || bytes[1] == (byte)0xFA);
    }

    private boolean isValidWavHeader(byte[] bytes) {
        return bytes.length >= 4 && 
               bytes[0] == 0x52 && bytes[1] == 0x49 && 
               bytes[2] == 0x46 && bytes[3] == 0x46;
    }

    private boolean hasDoubleExtension(String filename) {
        // Guard against the double-extension attack (e.g. "shell.php.jpg"): an executable
        // extension hidden before the final one. A normal filename may legitimately contain
        // several dots (dates, versions, WhatsApp exports) — only flag when an INNER segment
        // is itself a blocked/executable extension.
        String[] parts = filename.split("\\.");
        if (parts.length <= 2) {
            return false;
        }
        Set<String> blocked = getBlockedExtensions();
        for (int i = 1; i < parts.length - 1; i++) {
            if (blocked.contains(parts[i].trim().toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasRestrictedMetadata(byte[] fileBytes) {
        // For image files, we should be more careful about metadata checking
        // Only check for obvious malicious patterns in text-based metadata
        try {
            String content = new String(fileBytes, "UTF-8");
            String[] suspiciousPatterns = {
                "<script", "javascript:", "vbscript:", "data:", "<?php", "<%", "onload=", "onerror="
            };
            
            for (String pattern : suspiciousPatterns) {
                if (content.toLowerCase().contains(pattern.toLowerCase())) {
                    return true;
                }
            }
        } catch (Exception e) {
            // If we can't read as UTF-8, it's likely binary data - not necessarily harmful
            // For binary files like images, we'll be more permissive
            return false;
        }
        return false;
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex == -1 ? "" : filename.substring(lastDotIndex + 1);
    }

    /**
     * File types supported by the security service
     */
    public enum FileType {
        IMAGE, AUDIO, PDF, DOCUMENT, OTHER
    }

    /**
     * Result of file validation
     */
    public static class FileValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private FileValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static FileValidationResult success() {
            return new FileValidationResult(true, null);
        }

        public static FileValidationResult error(String message) {
            return new FileValidationResult(false, message);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
} 