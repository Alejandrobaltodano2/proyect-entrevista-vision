package com.preguntasSimulator.preguntas.Service;

import com.preguntasSimulator.preguntas.models.dtos.ItemEvaluacionDTO;
import com.preguntasSimulator.preguntas.models.dtos.ResultadoComparacionDTO;
import com.preguntasSimulator.preguntas.models.dtos.SugerenciaDTO;

import java.util.List;

public interface EvaluadorService {
    ResultadoComparacionDTO compararRespuestas(String esperada, String usuario);
    List<SugerenciaDTO> generarSugerencias(List<ItemEvaluacionDTO> resultados);

}
