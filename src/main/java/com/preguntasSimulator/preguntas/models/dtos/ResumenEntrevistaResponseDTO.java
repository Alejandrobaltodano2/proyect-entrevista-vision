package com.preguntasSimulator.preguntas.models.dtos;

import java.util.List;

public record ResumenEntrevistaResponseDTO(
        int promedioPorcentaje,
        int promedioAtencion,
        List<SugerenciaDTO> sugerencias
) {
}