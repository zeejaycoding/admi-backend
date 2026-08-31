package com.powercity.power_city_platform.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ValidImageFileValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidImageFile {
    String message() default "Invalid image file. Must be JPG, JPEG, PNG, or GIF format and less than 10MB";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    long maxSizeInBytes() default 10 * 1024 * 1024; // 10MB default

    String[] allowedTypes() default { "jpg", "jpeg", "png", "gif" };
}