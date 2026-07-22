package com.preguntasSimulator.preguntas.controllers;

import com.preguntasSimulator.preguntas.Service.AudioTranscripcionService;
import com.preguntasSimulator.preguntas.Service.EvaluadorService;
import com.preguntasSimulator.preguntas.Service.VisionService;
import com.preguntasSimulator.preguntas.models.Evaluacion;
import com.preguntasSimulator.preguntas.models.dtos.*;
import com.preguntasSimulator.preguntas.repository.EvaluacionRepository;
import com.preguntasSimulator.preguntas.util.ImagenUtils;
import org.opencv.core.Mat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/evaluacion")
public class EvaluacionController{


    private static final Logger log = LoggerFactory.getLogger(EvaluacionController.class);

    @Autowired
    private   AudioTranscripcionService audioTranscripcionService;
    @Autowired
    private  EvaluadorService evaluadorService;
    @Autowired
    private  EvaluacionRepository evaluacionRepository;

    @Autowired
    private VisionService visionService;

    @PostMapping(value = "/transcribir", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> transcribir(
            @RequestParam(value = "audio", required = false) MultipartFile audio,
            @RequestParam(value = "respuesta_esperada", defaultValue = "") String respuestaEsperada,
            @RequestParam(value = "pregunta_id", required = false) Long preguntaId,
            @RequestParam(value = "frames_total", defaultValue = "0") String framesTotal,
            @RequestParam(value = "frames_mirando", defaultValue = "0") String framesMirando,
            @RequestParam(value = "frame", defaultValue = "") String frame) {

        if (audio == null || audio.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No se recibió archivo de audio"));
        }

        byte[] wavBytes;
        try {
            wavBytes = audio.getBytes();
        } catch (IOException e) {
            log.error("Error leyendo archivo de audio: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "No se pudo leer el archivo de audio"));
        }

        TranscripcionResponseDTO transcripcionResultado = transcribirAudio(wavBytes);
        String transcripcion = transcripcionResultado.texto();
        boolean reconocido = transcripcionResultado.exito();

        ResultadoComparacionDTO comparacion = reconocido
                ? evaluadorService.compararRespuestas(respuestaEsperada, transcripcion)
                : new ResultadoComparacionDTO(0, List.of());

        Map<String, Object> frameInfo = analizarFrameOpcional(frame);

        int atencionPct = calcularAtencion(framesTotal, framesMirando);

        guardarEvaluacion(preguntaId, respuestaEsperada, transcripcion, comparacion, atencionPct);

        EvaluacionResponseDTO respuesta = new EvaluacionResponseDTO(
                transcripcion,
                comparacion.porcentaje(),
                comparacion.faltantes(),
                atencionPct,
                frameInfo,
                construirMensaje(reconocido, comparacion.porcentaje(), atencionPct)
        );

        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/resumen")
    public ResponseEntity<?> resumen(@RequestBody ResumenEntrevistaRequestDTO body,
                                     Authentication authentication) {
        List<ItemEvaluacionDTO> resultados =
                (body != null && body.resultados() != null) ? body.resultados() : List.of();

        if (resultados.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No hay resultados que resumir"));
        }

        int promedioPorcentaje = Math.round((float) resultados.stream()
                .mapToInt(ItemEvaluacionDTO::porcentaje)
                .average().orElse(0));

        int promedioAtencion = Math.round((float) resultados.stream()
                .mapToInt(ItemEvaluacionDTO::atencionPct)
                .average().orElse(0));

        List<SugerenciaDTO> sugerencias = evaluadorService.generarSugerencias(resultados);

        log.info("Resumen de entrevista generado para usuario={} ({} preguntas, {} sugerencias)",
                authentication.getName(), resultados.size(), sugerencias.size());

        return ResponseEntity.ok(new ResumenEntrevistaResponseDTO(promedioPorcentaje, promedioAtencion, sugerencias));
    }


    private TranscripcionResponseDTO transcribirAudio(byte[] wavBytes) {
        try {
            return audioTranscripcionService.transcribirWav(wavBytes, null);
        } catch (Exception e) {
            log.error("Error procesando audio: {}", e.getMessage());
            return new TranscripcionResponseDTO("", false);
        }
    }

    private Map<String, Object> analizarFrameOpcional(String frameUrl) {
        if (frameUrl == null || frameUrl.isBlank()) {
            return Map.of();
        }
        Mat bgr = ImagenUtils.dataUrlToBgr(frameUrl);
        if (bgr == null) {
            return Map.of();
        }
        try {
            return visionService.analizarFrame(bgr).toMap();
        } finally {

            bgr.release();
        }
    }

    private int calcularAtencion(String framesTotal, String framesMirando) {
        try {
            int total = Integer.parseInt(framesTotal);
            int mirando = Integer.parseInt(framesMirando);
            return total > 0 ? Math.round((float) mirando / total * 100) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String construirMensaje(boolean reconocido, int porcentaje, int atencion) {
        if (!reconocido) {
            return "No se reconoció audio — habla más cerca del micrófono.";
        }
        if (atencion < 40) {
            return "Acierto: %d%% ✔ — pero baja atención a la cámara (%d%%).".formatted(porcentaje, atencion);
        }
        return "Procesado correctamente ✔ — acierto: %d%%  |  atención: %d%%.".formatted(porcentaje, atencion);
    }

    private void guardarEvaluacion(Long preguntaId, String respuestaEsperada, String transcripcion,
                                   ResultadoComparacionDTO comparacion, int atencionPct) {
        Evaluacion evaluacion = new Evaluacion();
        evaluacion.setPreguntaId(preguntaId);
        evaluacion.setRespuestaEsperada(respuestaEsperada);
        evaluacion.setTranscripcion(transcripcion);
        evaluacion.setPorcentaje(comparacion.porcentaje());
        evaluacion.setFaltantes(comparacion.faltantes());
        evaluacion.setAtencionPct(atencionPct);
        evaluacion.setFechaCreacion(LocalDateTime.now());
        evaluacionRepository.save(evaluacion);
    }
}
