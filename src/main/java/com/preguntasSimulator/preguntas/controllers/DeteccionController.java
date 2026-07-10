package com.preguntasSimulator.preguntas.controllers;

import com.preguntasSimulator.preguntas.Service.VisionService;
import com.preguntasSimulator.preguntas.models.DeteccionRegistro;
import com.preguntasSimulator.preguntas.models.dtos.AnalisisFrameDTO;
import com.preguntasSimulator.preguntas.models.dtos.FrameRequestDTO;
import com.preguntasSimulator.preguntas.repository.DeteccionRegistroRepository;
import com.preguntasSimulator.preguntas.util.ImagenUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.opencv.core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/deteccion")
public class DeteccionController {
    private static final Logger log = LoggerFactory.getLogger(DeteccionController.class);

    private final VisionService visionService;
    private final DeteccionRegistroRepository deteccionRegistroRepository;

    public DeteccionController(VisionService visionService,
                               DeteccionRegistroRepository deteccionRegistroRepository) {
        this.visionService = visionService;
        this.deteccionRegistroRepository = deteccionRegistroRepository;
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

        AnalisisFrameDTO resultado = visionService.analizarFrame(frameBgr);
        log.debug("POST /detect → {}", resultado);

        guardarDeteccion(resultado);

        return ResponseEntity.ok(resultado);
    }

    private void guardarDeteccion(AnalisisFrameDTO resultado) {
        DeteccionRegistro registro = new DeteccionRegistro();
        registro.setMirando(resultado.mirando());
        registro.setEstado(resultado.estado());
        registro.setCaras(resultado.caras());
        registro.setOjos(resultado.ojos());
        registro.setFechaCreacion(LocalDateTime.now());
        deteccionRegistroRepository.save(registro);
    }
}
