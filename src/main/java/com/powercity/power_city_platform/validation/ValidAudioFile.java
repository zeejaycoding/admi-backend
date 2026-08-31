package com.powercity.power_city_platform.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ValidAudioFileValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidAudioFile {
    String message() default "Invalid audio file. Must be MP3, WAV, M4A, or AAC format and less than 100MB";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    long maxSizeInBytes() default 100 * 1024 * 1024; // 100MB default for audio files

    String[] allowedTypes() default { "mp3", "wav", "m4a", "aac", "ogg", "flac" };
} 