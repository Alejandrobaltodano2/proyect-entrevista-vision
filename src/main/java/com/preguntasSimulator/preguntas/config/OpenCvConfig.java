package com.preguntasSimulator.preguntas.config;

import jakarta.annotation.PostConstruct;
import nu.pattern.OpenCV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * Carga la libreria nativa de OpenCV correcta para el SO/arquitectura actual
 * (Windows/Linux/Mac, x86_64/arm64) una sola vez al iniciar la aplicacion.
 * Equivale a "import cv2" en Python, donde la carga del binario nativo
 * ocurre de forma transparente.
 */
@Configuration
public class OpenCvConfig {

    private static final Logger log = LoggerFactory.getLogger(OpenCvConfig.class);

    @PostConstruct
    public void cargarLibreriaNativa() {
        OpenCV.loadLocally();
        log.info("Libreria nativa de OpenCV cargada correctamente");
    }
}