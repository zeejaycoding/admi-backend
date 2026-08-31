package com.powercity.power_city_platform.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SwaggerInitializerController {

    @GetMapping(value = "/swagger-ui/swagger-initializer.js", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> swaggerInitializer() {
        String content = """
                window.onload = function() {
                  //<editor-fold desc="Changeable Configuration Block">

                  // the following lines will be replaced by docker/configurator, when it runs in a docker-container
                  window.ui = SwaggerUIBundle({
                    url: "/v3/api-docs",
                    dom_id: '#swagger-ui',
                    deepLinking: true,
                    presets: [
                      SwaggerUIBundle.presets.apis,
                      SwaggerUIStandalonePreset
                    ],
                    plugins: [
                      SwaggerUIBundle.plugins.DownloadUrl
                    ],
                    layout: "StandaloneLayout"
                  });

                  //</editor-fold>
                };
                """;
        return ResponseEntity.ok()
                .header("Content-Type", "application/javascript")
                .body(content);
    }
}