package com.preguntasSimulator.preguntas.models.dtos;

import java.util.List;

public record ResumenEntrevistaRequestDTO(
        List<ItemEvaluacionDTO> resultados
) {
}