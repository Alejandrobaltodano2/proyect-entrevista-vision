package com.preguntasSimulator.preguntas.repository;

import com.preguntasSimulator.preguntas.models.Preguntas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PreguntasRepository extends JpaRepository<Preguntas, Integer> {
}
