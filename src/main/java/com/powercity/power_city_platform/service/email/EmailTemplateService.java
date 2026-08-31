package com.powercity.power_city_platform.service.email;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Service
public class EmailTemplateService {

    @Autowired
    private TemplateEngine templateEngine;

    public String processTemplate(String templateName, Map<String, Object> variables) {
        Context context = new Context();

        if (variables != null) {
            variables.forEach(context::setVariable);
        }

        return templateEngine.process("email/" + templateName, context);
    }

    public boolean templateExists(String templateName) {
        try {
            templateEngine.process("email/" + templateName, new Context());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}