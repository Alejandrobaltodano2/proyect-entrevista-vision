package com.preguntasSimulator.preguntas.Service;

import com.preguntasSimulator.preguntas.models.dtos.AnalisisFrameDTO;
import org.opencv.core.Mat;

public interface VisionService {

    AnalisisFrameDTO analizarFrame(Mat frameBgr);
}
