package com.preguntasSimulator.preguntas.models.dtos;

public record RegistroRequestDTO(
        String username,
        String password,
        String nombres,
        String apellidos,
        String carrera
) {
}
