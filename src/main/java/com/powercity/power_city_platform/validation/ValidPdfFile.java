package com.powercity.power_city_platform.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ValidPdfFileValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPdfFile {
    String message() default "Invalid PDF file. Must be PDF format and less than 50MB";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    long maxSizeInBytes() default 50 * 1024 * 1024; // 50MB default for PDF files

    String[] allowedTypes() default { "pdf" };
} 