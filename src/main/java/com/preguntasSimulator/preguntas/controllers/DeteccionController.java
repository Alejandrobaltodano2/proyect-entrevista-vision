package com.preguntasSimulator.preguntas.controllers;

import com.preguntasSimulator.preguntas.Service.VisionService;
import com.preguntasSimulator.preguntas.models.dtos.AnalisisFrameDTO;
import com.preguntasSimulator.preguntas.models.dtos.FrameRequestDTO;
import com.preguntasSimulator.preguntas.util.ImagenUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.opencv.core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@RestController
@RequestMapping("/api/deteccion")
public class DeteccionController {
    private static final Logger log = LoggerFactory.getLogger(DeteccionController.class);

    private final VisionService visionService;

    public DeteccionController(VisionService visionService) {
        this.visionService = visionService;
    }


    @PostMapping("/detect")
    public ResponseEntity<?> detect(@RequestBody(required = false) FrameRequestDTO body) {
        String frameUrl = (body != null) ? body.frame() : null;

        if (frameUrl == null || frameUrl.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("mirando", false, "estado", "no_frame"));
        }

        Mat frameBgr = ImagenUtils.dataUrlToBgr(frameUrl);
        if (frameBgr == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("mirando", false, "estado", "error"));
        }

        try {
            AnalisisFrameDTO resultado = visionService.analizarFrame(frameBgr);
            log.debug("POST /detect → {}", resultado);
            return ResponseEntity.ok(resultado);
        } finally {
            // Mat reserva memoria nativa (fuera del heap de la JVM) que el
            // Garbage Collector de Java no libera de forma oportuna.
            // Sin este release(), cada frame recibido deja memoria nativa
            // "colgada" y el proceso puede quedarse sin RAM en Render.
            frameBgr.release();
        }
    }
}