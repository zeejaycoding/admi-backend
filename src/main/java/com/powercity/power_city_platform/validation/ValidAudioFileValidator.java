package com.powercity.power_city_platform.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ValidAudioFileValidator implements ConstraintValidator<ValidAudioFile, MultipartFile> {

    private long maxSizeInBytes;
    private Set<String> allowedTypes;

    @Override
    public void initialize(ValidAudioFile constraintAnnotation) {
        this.maxSizeInBytes = constraintAnnotation.maxSizeInBytes();
        this.allowedTypes = new HashSet<>(Arrays.asList(constraintAnnotation.allowedTypes()));
    }

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        if (file == null || file.isEmpty()) {
            return true; // Let @NotNull handle null/empty files if required
        }

        // Check file size
        if (file.getSize() > maxSizeInBytes) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "File size exceeds maximum allowed size of " + (maxSizeInBytes / 1024 / 1024) + "MB")
                    .addConstraintViolation();
            return false;
        }

        // Check file type
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("File must have a valid filename")
                    .addConstraintViolation();
            return false;
        }

        String fileExtension = getFileExtension(originalFilename).toLowerCase();
        if (!allowedTypes.contains(fileExtension)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "File type not allowed. Allowed types: " + allowedTypes.toString())
                    .addConstraintViolation();
            return false;
        }

        // Check MIME type for audio files
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("audio/")) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("File must be an audio file")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex == -1 ? "" : filename.substring(lastDotIndex + 1);
    }
} 