package com.preguntasSimulator.preguntas.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
@Entity
@Table(name = "transcripciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Transcripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private String texto;

    private boolean exito;

    private String idioma;

    private LocalDateTime fechaCreacion;
}
