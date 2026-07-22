package com.preguntasSimulator.preguntas.models.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;


public record EvaluacionResponseDTO(
        String transcripcion,
        int porcentaje,
        List<String> faltantes,
        @JsonProperty("atencion_pct") int atencionPct,
        @JsonProperty("analisis_frame") Map<String, Object> analisisFrame,
        String mensaje
) {
}
