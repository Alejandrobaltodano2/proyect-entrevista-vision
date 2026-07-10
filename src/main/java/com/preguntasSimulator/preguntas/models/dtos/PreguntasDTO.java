package com.preguntasSimulator.preguntas.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreguntasDTO {

    private  String pregunta;

    private String respuesta;

    private Integer duracion_ms;

}
