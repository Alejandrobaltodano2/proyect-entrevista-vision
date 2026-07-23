package com.preguntasSimulator.preguntas.Service.impl;

import com.preguntasSimulator.preguntas.Service.EvaluadorService;
import com.preguntasSimulator.preguntas.models.dtos.ItemEvaluacionDTO;
import com.preguntasSimulator.preguntas.models.dtos.ResultadoComparacionDTO;
import com.preguntasSimulator.preguntas.models.dtos.SugerenciaDTO;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Service
public class EvaluadorServiceImpl implements EvaluadorService {

    private static final Logger log = LoggerFactory.getLogger(EvaluadorServiceImpl.class);

    // Palabras vacias en español que no aportan significado semantico
    private static final Set<String> STOPWORDS = Set.of(
            "el", "la", "los", "las", "un", "una", "unos", "unas",
            "de", "del", "en", "y", "a", "que", "es", "con", "por",
            "se", "su", "sus", "al", "lo", "le", "les", "no", "si",
            "me", "te", "nos", "más", "pero", "como", "para", "esto",
            "esta", "ese", "esa", "esos", "esas"
    );

    private static final int UMBRAL_RESPUESTA_BAJA = 60;


    private static final int UMBRAL_ATENCION_BAJA = 70;

    private static final Pattern PUNTUACION = Pattern.compile("[.,;:!?¡¿\"'()\\[\\]]");

    @Override
    public ResultadoComparacionDTO compararRespuestas(String esperada, String usuario) {
        if (esperada == null || esperada.isBlank()) {
            return new ResultadoComparacionDTO(100, List.of());
        }

        Set<String> clavesEsperadas = tokenizar(esperada);
        Set<String> clavesUsuario = tokenizar(usuario);

        log.debug("Usuario: {}", usuario);

        if (clavesEsperadas.isEmpty()) {
            return new ResultadoComparacionDTO(100, List.of());
        }

        Set<String> encontradas = new HashSet<>(clavesEsperadas);
        encontradas.retainAll(clavesUsuario);

        List<String> faltantes = clavesEsperadas.stream()
                .filter(palabra -> !clavesUsuario.contains(palabra))
                .sorted()
                .toList();

        int porcentaje = Math.round((float) encontradas.size() / clavesEsperadas.size() * 100);

        log.debug("Evaluación → esperadas={}  encontradas={}  faltantes={}  acierto={}%",
                clavesEsperadas, encontradas, faltantes, porcentaje);

        return new ResultadoComparacionDTO(porcentaje, faltantes);
    }

    @Override
    public List<SugerenciaDTO> generarSugerencias(List<ItemEvaluacionDTO> resultados) {
        List<SugerenciaDTO> sugerencias = new ArrayList<>();

        if (resultados == null || resultados.isEmpty()) {
            return sugerencias;
        }

        // --- 1. Preguntas con acierto bajo: sugerir revisar la respuesta ---
        for (int i = 0; i < resultados.size(); i++) {
            ItemEvaluacionDTO item = resultados.get(i);
            if (item.porcentaje() <= UMBRAL_RESPUESTA_BAJA) {
                String mensaje = String.format(
                        "Pregunta %d: tu respuesta tuvo un acierto bajo (%d%%). Revisa la respuesta esperada.",
                        i + 1, item.porcentaje());
                sugerencias.add(new SugerenciaDTO("respuesta", mensaje, item.respuestaEsperada()));
            }
        }

        // --- 2. Atencion a camara promedio de toda la entrevista ---
        double promedioAtencion = resultados.stream()
                .mapToInt(ItemEvaluacionDTO::atencionPct)
                .average()
                .orElse(0);

        if (promedioAtencion <= UMBRAL_ATENCION_BAJA) {
            String mensaje = String.format(
                    "Tu atención a la cámara fue de %.0f%%, por debajo del %d%% recomendado. " +
                            "Debes mirar más hacia la cámara mientras respondes.",
                    promedioAtencion, UMBRAL_ATENCION_BAJA);
            sugerencias.add(new SugerenciaDTO("atencion", mensaje, null));
        }

        // --- 3. Si no hubo nada que corregir, un mensaje positivo ---
        if (sugerencias.isEmpty()) {
            sugerencias.add(new SugerenciaDTO(
                    "positivo",
                    "¡Buen trabajo! No se detectaron áreas críticas de mejora en esta entrevista.",
                    null));
        }

        return sugerencias;
    }


    private Set<String> tokenizar(String texto) {
        if (texto == null || texto.isBlank()) {
            return Set.of();
        }

        String limpio = PUNTUACION.matcher(texto.toLowerCase()).replaceAll("");

        return Arrays.stream(limpio.trim().split("\\s+"))
                .filter(palabra -> !palabra.isBlank())
                .filter(palabra -> !STOPWORDS.contains(palabra) && palabra.length() > 1)
                .collect(Collectors.toSet());
    }
}
