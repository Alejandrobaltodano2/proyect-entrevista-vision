package com.preguntasSimulator.preguntas.models.dtos;

import java.util.List;


public record ResultadoComparacionDTO(int porcentaje, List<String> faltantes) {
}
