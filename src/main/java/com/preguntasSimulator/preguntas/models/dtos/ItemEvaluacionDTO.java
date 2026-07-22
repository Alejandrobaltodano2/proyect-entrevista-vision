package com.preguntasSimulator.preguntas.models.dtos;

public record ItemEvaluacionDTO(
        String pregunta,
        String respuestaEsperada,
        String transcripcion,
        int porcentaje,
        int atencionPct
) {
}