package com.powercity.power_city_platform.controller;

import com.powercity.power_city_platform.dto.request.contact.ContactFormRequest;
import com.powercity.power_city_platform.service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/contact")
@RequiredArgsConstructor
public class ContactController {

    private final EmailService emailService;

    @PostMapping
    public ResponseEntity<Map<String, String>> submitContactForm(
            @Valid @RequestBody ContactFormRequest request
    ) {
        emailService.sendContactFormEmail(request.name(), request.email(), request.subject(), request.message());
        return ResponseEntity.ok(Map.of("message", "Your message has been sent. We'll get back to you soon."));
    }
}
