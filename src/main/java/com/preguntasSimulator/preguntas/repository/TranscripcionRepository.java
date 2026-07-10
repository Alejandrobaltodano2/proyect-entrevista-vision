package com.preguntasSimulator.preguntas.repository;

import com.preguntasSimulator.preguntas.models.Transcripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TranscripcionRepository extends JpaRepository<Transcripcion, Long> {
}
