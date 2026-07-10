package com.preguntasSimulator.preguntas.mappers;


import com.preguntasSimulator.preguntas.models.Preguntas;
import com.preguntasSimulator.preguntas.models.dtos.PreguntasDTO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PreguntasMapper {
    PreguntasMapper INSTANCIA= Mappers.getMapper(PreguntasMapper.class);
    PreguntasDTO preguntasToPreguntasDTO(Preguntas preguntas);
    Preguntas preguntasDTOToPreguntas(PreguntasDTO preguntasDTO);
}
