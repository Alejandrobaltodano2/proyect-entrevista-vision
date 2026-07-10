package com.preguntasSimulator.preguntas.models;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Historial de detecciones de POST /api/deteccion/detect.
 * No existia en el Python original, se agrega para trazabilidad
 * (igual criterio que con Transcripcion y Evaluacion).
 */
@Entity
@Table(name = "detecciones")
@Getter
@Setter
@NoArgsConstructor
public class DeteccionRegistro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private boolean mirando;

    private String estado;

    private int caras;

    private int ojos;

    private LocalDateTime fechaCreacion;
}
