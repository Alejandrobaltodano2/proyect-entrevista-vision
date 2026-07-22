package com.preguntasSimulator.preguntas.util;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;


public final class ImagenUtils {

    private static final Logger log = LoggerFactory.getLogger(ImagenUtils.class);

    private ImagenUtils() {
    }


    public static Mat dataUrlToBgr(String dataUrl) {
        if (dataUrl == null || dataUrl.isBlank()) {
            return null;
        }

        MatOfByte buffer = null;
        try {
            int comaIndex = dataUrl.indexOf(',');
            String base64Data = comaIndex >= 0 ? dataUrl.substring(comaIndex + 1) : dataUrl;
            byte[] bytes = Base64.getDecoder().decode(base64Data);

            buffer = new MatOfByte(bytes);
            Mat bgr = Imgcodecs.imdecode(buffer, Imgcodecs.IMREAD_COLOR);
            if (bgr.empty()) {
                log.warn("dataUrlToBgr: la imagen no pudo decodificarse (Mat vacio)");
                bgr.release();
                return null;
            }
            return bgr;

        } catch (IllegalArgumentException e) {
            log.warn("dataUrlToBgr: data URL invalida - {}", e.getMessage());
            return null;
        } finally {
            if (buffer != null) {
                buffer.release();
            }
        }
    }
}