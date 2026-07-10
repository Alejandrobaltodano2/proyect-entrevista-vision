package com.preguntasSimulator.preguntas.models.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Espejo exacto del JSON que devolvia POST /procesar en Flask.
 * Se usan @JsonProperty para mantener las claves snake_case
 * (atencion_pct, analisis_frame) y no romper el contrato con el frontend.
 */
public record EvaluacionResponseDTO(
        String transcripcion,
        int porcentaje,
        List<String> faltantes,
        @JsonProperty("atencion_pct") int atencionPct,
        @JsonProperty("analisis_frame") Map<String, Object> analisisFrame,
        String mensaje
) {
}
