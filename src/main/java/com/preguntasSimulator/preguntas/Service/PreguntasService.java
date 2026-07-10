package com.preguntasSimulator.preguntas.Service;

import com.preguntasSimulator.preguntas.models.Preguntas;
import com.preguntasSimulator.preguntas.models.dtos.PreguntasDTO;

import java.util.List;

public interface PreguntasService {

    void guardarPregunta(PreguntasDTO pregunta);
    void modificarPregunta(PreguntasDTO pregunta);
    List<Preguntas> listarPreguntas();
    Preguntas obtenerPreguntaPorId(Integer id);
    void  eliminarPregunta(Integer id);
}
