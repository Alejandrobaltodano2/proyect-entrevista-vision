package com.preguntasSimulator.preguntas.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Table
@Builder
@Entity
public class Preguntas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private  String pregunta;

    private String respuesta;

    private Integer duracion_ms;

}
