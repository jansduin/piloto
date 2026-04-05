package com.piloto.cdi.gateway.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * Configuración dinámica para cargar variables desde el archivo .env
 * a las propiedades del sistema de Spring/JVM.
 */
@Configuration
public class DotenvConfig {
    private static final Logger logger = LoggerFactory.getLogger(DotenvConfig.class);

    @PostConstruct
    public void init() {
        try {
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing()
                    .load();

            dotenv.entries().forEach(entry -> {
                if (System.getProperty(entry.getKey()) == null) {
                    System.setProperty(entry.getKey(), entry.getValue());
                }
            });
            logger.info("✅ Variables de entorno desde .env cargadas con éxito.");
        } catch (Exception e) {
            logger.warn("⚠️ No se pudo cargar el archivo .env: {}", e.getMessage());
        }
    }
}
