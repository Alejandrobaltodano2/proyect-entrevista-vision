package com.preguntasSimulator.preguntas.models.dtos;

import java.util.List;

/**
 * Equivale a la tupla (porcentaje_acierto: int, palabras_faltantes: list[str])
 * que devolvia comparar_respuestas() en Python.
 */
public record ResultadoComparacionDTO(int porcentaje, List<String> faltantes) {
}
