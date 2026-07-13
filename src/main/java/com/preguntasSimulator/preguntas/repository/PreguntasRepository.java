package com.preguntasSimulator.preguntas.repository;

import com.preguntasSimulator.preguntas.models.Preguntas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PreguntasRepository extends JpaRepository<Preguntas, Integer> {

    // ORDER BY RANDOM() + LIMIT hace que Postgres devuelva solo las filas
    // pedidas, en vez de traer toda la tabla a memoria en la app para
    // barajarla y descartar el resto (como hacia antes con findAll()).
    @Query(value = "SELECT * FROM preguntas ORDER BY RANDOM() LIMIT :cantidad", nativeQuery = true)
    List<Preguntas> obtenerAleatorias(@Param("cantidad") int cantidad);
}