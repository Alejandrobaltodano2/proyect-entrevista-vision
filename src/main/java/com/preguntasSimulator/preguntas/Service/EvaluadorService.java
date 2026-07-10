package com.preguntasSimulator.preguntas.Service;

import com.preguntasSimulator.preguntas.models.dtos.ResultadoComparacionDTO;

public interface EvaluadorService {
    ResultadoComparacionDTO compararRespuestas(String esperada, String usuario);

}
