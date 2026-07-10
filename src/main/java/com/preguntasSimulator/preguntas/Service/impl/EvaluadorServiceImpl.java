package com.preguntasSimulator.preguntas.Service.impl;

import com.preguntasSimulator.preguntas.Service.EvaluadorService;
import com.preguntasSimulator.preguntas.models.dtos.ResultadoComparacionDTO;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    // Equivalente a str.maketrans("", "", ".,;:!?¡¿\"'()[]") + translate()
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

    /**
     * Convierte un texto en un conjunto de palabras significativas.
     * Elimina puntuacion basica, pasa a minusculas y filtra stopwords.
     */
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
