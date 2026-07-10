package com.preguntasSimulator.preguntas.Service.impl;

import com.preguntasSimulator.preguntas.Service.VisionService;
import com.preguntasSimulator.preguntas.models.dtos.AnalisisFrameDTO;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;


@Service
@AllArgsConstructor
@NoArgsConstructor
public class VisionServiceImpl implements VisionService {

    private static final Logger log = LoggerFactory.getLogger(VisionServiceImpl.class);

    // Parametros de deteccion (identicos a los del Python original)
    private static final double FACE_SCALE = 1.1;
    private static final int FACE_NEIGHBORS = 5;
    private static final Size FACE_MIN_SIZE = new Size(60, 60);

    private static final double EYE_SCALE = 1.1;
    private static final int EYE_NEIGHBORS = 10;
    private static final Size EYE_MIN_SIZE = new Size(20, 20);

    private CascadeClassifier faceCascade;
    private CascadeClassifier eyeCascade;

    /**
     * Carga los modelos Haar Cascade una sola vez, al arrancar el bean.
     * CascadeClassifier de OpenCV solo acepta rutas de archivo reales,
     * por eso los recursos del classpath se copian primero a un archivo temporal.
     */
    @PostConstruct
    public void cargarModelos() {
        faceCascade = new CascadeClassifier(
                extraerRecursoATemporal("haarcascades/haarcascade_frontalface_default.xml"));
        eyeCascade = new CascadeClassifier(
                extraerRecursoATemporal("haarcascades/haarcascade_eye.xml"));

        if (faceCascade.empty() || eyeCascade.empty()) {
            log.error("No se pudieron cargar los clasificadores Haar Cascade");
        } else {
            log.info("Clasificadores Haar Cascade (rostro/ojos) cargados correctamente");
        }
    }

    @Override
    public AnalisisFrameDTO analizarFrame(Mat frameBgr) {
        Mat gray = preprocesar(frameBgr);

        Rect[] faces = detectarRostros(gray);
        if (faces.length == 0) {
            return new AnalisisFrameDTO(false, "no_face", 0, 0);
        }

        Rect rostroPrincipal = rostroMasGrande(faces);
        List<Rect> ojos = detectarOjos(gray, rostroPrincipal);

        boolean mirando = ojos.size() >= 2;
        String estado = mirando ? "ok" : "no_eyes";

        return new AnalisisFrameDTO(mirando, estado, faces.length, ojos.size());
    }

    /** Convierte a escala de grises y ecualiza el histograma. */
    private Mat preprocesar(Mat frameBgr) {
        Mat gray = new Mat();
        Imgproc.cvtColor(frameBgr, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.equalizeHist(gray, gray);
        return gray;
    }

    private Rect[] detectarRostros(Mat gray) {
        MatOfRect faces = new MatOfRect();
        faceCascade.detectMultiScale(
                gray, faces, FACE_SCALE, FACE_NEIGHBORS, 0, FACE_MIN_SIZE, new Size());
        return faces.toArray();
    }

    /** Devuelve el rostro (bounding box) con mayor area. */
    private Rect rostroMasGrande(Rect[] faces) {
        Rect mayor = faces[0];
        for (Rect r : faces) {
            if (r.area() > mayor.area()) {
                mayor = r;
            }
        }
        return mayor;
    }

    /**
     * Busca ojos dentro de la mitad superior del rostro.
     * Limitar la ROI a la mitad superior reduce falsos positivos
     * causados por nariz y boca.
     */
    private List<Rect> detectarOjos(Mat gray, Rect rostro) {
        int altoMitad = rostro.height / 2;
        Rect roiRect = new Rect(rostro.x, rostro.y, rostro.width, altoMitad);
        Mat roi = new Mat(gray, roiRect);

        MatOfRect ojosDetectados = new MatOfRect();
        eyeCascade.detectMultiScale(
                roi, ojosDetectados, EYE_SCALE, EYE_NEIGHBORS, 0, EYE_MIN_SIZE, new Size());

        // Filtro adicional: el ojo debe estar en el tercio superior de la ROI
        int tercioSuperior = rostro.height / 3;
        List<Rect> ojosFiltrados = new ArrayList<>();
        for (Rect ojo : ojosDetectados.toArray()) {
            if (ojo.y < tercioSuperior) {
                ojosFiltrados.add(ojo);
            }
        }
        return ojosFiltrados;
    }

    private String extraerRecursoATemporal(String rutaClasspath) {
        try (InputStream in = new ClassPathResource(rutaClasspath).getInputStream()) {
            Path temp = Files.createTempFile("cascade-", ".xml");
            temp.toFile().deleteOnExit();
            Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
            return temp.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo extraer el recurso: " + rutaClasspath, e);
        }
    }
}
