package com.preguntasSimulator.preguntas.repository;

import com.preguntasSimulator.preguntas.models.DeteccionRegistro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeteccionRegistroRepository extends JpaRepository<DeteccionRegistro, Long> {
}
