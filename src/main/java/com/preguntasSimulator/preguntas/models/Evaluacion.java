package com.preguntasSimulator.preguntas.models;


import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "evaluaciones")
@Getter
@Setter
@NoArgsConstructor
public class Evaluacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long preguntaId;

    @Column(length = 2000)
    private String respuestaEsperada;

    @Column(length = 2000)
    private String transcripcion;

    private int porcentaje;

    @ElementCollection
    @CollectionTable(name = "evaluacion_faltantes", joinColumns = @JoinColumn(name = "evaluacion_id"))
    @Column(name = "palabra")
    private List<String> faltantes = new ArrayList<>();

    private int atencionPct;

    private LocalDateTime fechaCreacion;
}
