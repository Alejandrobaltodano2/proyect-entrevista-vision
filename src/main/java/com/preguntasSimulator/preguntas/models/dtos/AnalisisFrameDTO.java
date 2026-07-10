package com.preguntasSimulator.preguntas.models.dtos;

import java.util.Map;

public record AnalisisFrameDTO(boolean mirando, String estado, int caras, int ojos) {

    /** Conveniencia para embeber el resultado dentro de otro DTO (ej. EvaluacionResponseDTO). */
    public Map<String, Object> toMap() {
        return Map.of(
                "mirando", mirando,
                "estado", estado,
                "caras", caras,
                "ojos", ojos
        );
    }
}
