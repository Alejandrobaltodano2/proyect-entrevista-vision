package com.preguntasSimulator.preguntas.repository;

import com.preguntasSimulator.preguntas.models.Evaluacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvaluacionRepository extends JpaRepository<Evaluacion, Long> {

    List<Evaluacion> findByPreguntaId(Long preguntaId);
}
