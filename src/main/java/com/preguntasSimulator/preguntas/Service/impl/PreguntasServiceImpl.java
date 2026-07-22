package com.preguntasSimulator.preguntas.Service.impl;

import com.preguntasSimulator.preguntas.Service.PreguntasService;
import com.preguntasSimulator.preguntas.mappers.PreguntasMapper;
import com.preguntasSimulator.preguntas.models.Preguntas;
import com.preguntasSimulator.preguntas.models.dtos.PreguntasDTO;
import com.preguntasSimulator.preguntas.repository.PreguntasRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service

public class PreguntasServiceImpl implements PreguntasService {

    private static final int CANTIDAD_PREGUNTAS_MOSTRADAS = 5;

    @Autowired
    private PreguntasRepository preguntasRepository;

    @Override
    public void guardarPregunta(PreguntasDTO pregunta) {

        preguntasRepository.save(PreguntasMapper.INSTANCIA.preguntasDTOToPreguntas(pregunta));
    }

    @Override
    public void modificarPregunta(PreguntasDTO pregunta) {

        preguntasRepository.save(PreguntasMapper.INSTANCIA.preguntasDTOToPreguntas(pregunta));

    }

    @Override
    public List<Preguntas> listarPreguntas() {
        return preguntasRepository.obtenerAleatorias(CANTIDAD_PREGUNTAS_MOSTRADAS);
    }

    @Override
    public Preguntas obtenerPreguntaPorId(Integer id) {
        return preguntasRepository.findById(id).orElse(null);
    }

    @Override
    public void eliminarPregunta(Integer id) {
        if (preguntasRepository.existsById(id)) {
            preguntasRepository.deleteById(id);
        }
    }
}