package com.preguntasSimulator.preguntas.models.dtos;

public record LoginRequestDTO(
        String username,
        String password
) {
}