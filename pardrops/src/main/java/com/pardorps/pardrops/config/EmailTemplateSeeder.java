package com.pardorps.pardrops.config;

import com.pardorps.pardrops.service.EmailTemplateService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Crea las plantillas de correo predeterminadas al arrancar.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
public class EmailTemplateSeeder implements CommandLineRunner {

    private final EmailTemplateService emailTemplateService;

    /**
     * @param emailTemplateService servicio de plantillas.
     */
    public EmailTemplateSeeder(EmailTemplateService emailTemplateService) {
        this.emailTemplateService = emailTemplateService;
    }

    @Override
    public void run(String... args) {
        emailTemplateService.seedDefaults();
    }
}
