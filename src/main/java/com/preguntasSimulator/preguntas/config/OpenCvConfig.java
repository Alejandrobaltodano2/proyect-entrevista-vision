package com.preguntasSimulator.preguntas.config;

import jakarta.annotation.PostConstruct;
import nu.pattern.OpenCV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;


@Configuration
public class OpenCvConfig {

    private static final Logger log = LoggerFactory.getLogger(OpenCvConfig.class);

    @PostConstruct
    public void cargarLibreriaNativa() {
        OpenCV.loadLocally();
        log.info("Libreria nativa de OpenCV cargada correctamente");
    }
}