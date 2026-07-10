package com.preguntasSimulator.preguntas.Service;

import com.preguntasSimulator.preguntas.models.dtos.TranscripcionResponseDTO;

public interface AudioTranscripcionService {
    TranscripcionResponseDTO transcribirWav(byte[] wavBytes, String idioma);

}
