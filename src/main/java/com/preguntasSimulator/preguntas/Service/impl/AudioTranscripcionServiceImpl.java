package com.preguntasSimulator.preguntas.Service.impl;

import com.preguntasSimulator.preguntas.Service.AudioTranscripcionService;
import com.preguntasSimulator.preguntas.models.Transcripcion;
import com.preguntasSimulator.preguntas.models.dtos.TranscripcionResponseDTO;
import com.preguntasSimulator.preguntas.repository.TranscripcionRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class AudioTranscripcionServiceImpl implements AudioTranscripcionService {

    private static final Logger log = LoggerFactory.getLogger(AudioTranscripcionServiceImpl.class);

    private static final int TAMANIO_MINIMO_WAV = 44;

    private static final String GOOGLE_STT_URL = "https://www.google.com/speech-api/v2/recognize";
    private static final String GOOGLE_API_KEY = "AIzaSyBOti4mM-6x9WDnZIjIeyEU21OpBXqWBgw";

    // Cuántas alternativas le pedimos a Google (nos quedamos con la de mayor confianza)
    private static final int MAX_ALTERNATIVAS = 5;

    // Percentil que usamos como referencia del "nivel típico de voz" (0-1).
    // Usamos el percentil en vez del pico absoluto porque un solo clic o golpe
    // en el micrófono puede disparar el pico sin que la voz real sea fuerte,
    // haciendo que el AGC no amplifique lo suficiente.
    private static final double PERCENTIL_REFERENCIA = 0.95;

    // Nivel objetivo para ese percentil, como fracción del máximo de 16 bits
    private static final double NIVEL_OBJETIVO = 0.60;

    // Ganancia máxima permitida. Se sube bastante porque hablar bajo/susurrado
    // puede estar 20-30 dB por debajo del nivel objetivo.
    private static final double GANANCIA_MAXIMA = 20.0;

    // Por debajo de este nivel consideramos que es ruido de fondo o silencio,
    // no voz, y no tiene sentido (ni es seguro) amplificarlo
    private static final double NIVEL_MINIMO_CON_SENAL = 0.01;

    private static final int REINTENTOS_RED = 2;

    private final RestTemplate restTemplate;
    private final TranscripcionRepository transcripcionRepository;


    private static final String idiomaPorDefecto = "es-ES";


    @Override
    public TranscripcionResponseDTO transcribirWav(byte[] wavBytes, String idioma) {
        String idiomaFinal = (idioma != null && !idioma.isBlank()) ? idioma : idiomaPorDefecto;

        if (wavBytes == null || wavBytes.length < TAMANIO_MINIMO_WAV) {
            log.warn("transcribirWav: audio vacio o demasiado corto");
            return guardarYRetornar("", false, idiomaFinal);
        }

        try {
            AudioInputStream audioInputStream =
                    AudioSystem.getAudioInputStream(new ByteArrayInputStream(wavBytes));
            AudioFormat formato = audioInputStream.getFormat();
            int sampleRate = (int) formato.getSampleRate();

            log.info("Formato de audio recibido: {} canal(es), {} bits, {} Hz, encoding={}",
                    formato.getChannels(), formato.getSampleSizeInBits(), sampleRate, formato.getEncoding());

            byte[] pcmData = audioInputStream.readAllBytes();

            // --- Pre-procesamiento para mejorar reconocimiento con voz baja o ruido ---
            if (formato.getSampleSizeInBits() == 16
                    && formato.getEncoding() == AudioFormat.Encoding.PCM_SIGNED) {
                pcmData = filtroPasaAltos(pcmData, sampleRate);
                pcmData = normalizarGanancia(pcmData);
            } else {
                // Si el audio no es PCM de 16 bits con signo, el procesamiento de arriba
                // interpretaría mal los bytes y corrompería el audio en vez de mejorarlo.
                // Esto es una señal fuerte de que hay que revisar cómo se está grabando
                // el WAV del lado del cliente.
                log.warn("Formato de audio no es PCM_SIGNED de 16 bits (es {} de {} bits). " +
                        "Se omite el pre-procesamiento para no corromper el audio; " +
                        "revisar la grabación del cliente.", formato.getEncoding(), formato.getSampleSizeInBits());
            }

            String textoReconocido = enviarAGoogleSTTConReintentos(pcmData, sampleRate, idiomaFinal);
            boolean exito = textoReconocido != null && !textoReconocido.isBlank();

            if (exito) {
                log.info("Transcripcion -> «{}»", textoReconocido);
            } else {
                log.warn("Google STT: audio no reconocido (silencio, ruido o voz poco clara)");
            }

            return guardarYRetornar(exito ? textoReconocido : "", exito, idiomaFinal);

        } catch (UnsupportedAudioFileException e) {
            log.error("Formato de audio no soportado: {}", e.getMessage());
            return guardarYRetornar("", false, idiomaFinal);
        } catch (RestClientException e) {
            log.error("Google STT error de red: {}", e.getMessage());
            return guardarYRetornar("", false, idiomaFinal);
        } catch (IOException e) {
            log.error("Error leyendo el audio WAV: {}", e.getMessage());
            return guardarYRetornar("", false, idiomaFinal);
        }
    }

    /**
     * Amplifica automáticamente el audio cuando la voz llegó muy baja (AGC).
     * En vez de basarse en el pico absoluto (frágil: un solo clic o golpe al
     * micrófono infla el pico y hace que el AGC "crea" que el audio ya es
     * fuerte), usamos el percentil 95 de amplitud como referencia del nivel
     * real de la voz. Así, transitorios aislados no bloquean la amplificación.
     *
     * Las muestras que aún así superen el rango de 16 bits tras aplicar la
     * ganancia se comprimen con un limitador suave (tanh) en vez de recortarse
     * a lo bruto, para evitar distorsión audible.
     */
    private byte[] normalizarGanancia(byte[] pcmData) {
        if (pcmData.length < 2) {
            return pcmData;
        }

        ByteBuffer buffer = ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN);
        int numSamples = pcmData.length / 2;
        short[] samples = new short[numSamples];
        int[] amplitudesAbs = new int[numSamples];

        for (int i = 0; i < numSamples; i++) {
            short muestra = buffer.getShort();
            samples[i] = muestra;
            amplitudesAbs[i] = Math.abs((int) muestra);
        }

        int[] copiaOrdenada = amplitudesAbs.clone();
        java.util.Arrays.sort(copiaOrdenada);
        int indicePercentil = (int) Math.min(
                copiaOrdenada.length - 1,
                Math.floor(copiaOrdenada.length * PERCENTIL_REFERENCIA));
        int referencia = copiaOrdenada[indicePercentil];

        double referenciaNormalizada = referencia / 32767.0;

        if (referenciaNormalizada < NIVEL_MINIMO_CON_SENAL) {
            // No hay señal de voz real (silencio o ruido de piso), no amplificamos
            log.warn("Audio sin señal detectable (nivel {}%), se omite amplificación",
                    String.format("%.2f", referenciaNormalizada * 100));
            return pcmData;
        }

        double ganancia = NIVEL_OBJETIVO / referenciaNormalizada;
        ganancia = Math.min(ganancia, GANANCIA_MAXIMA);

        if (ganancia <= 1.05) {
            // El audio ya está en un nivel adecuado, no vale la pena tocarlo
            return pcmData;
        }

        log.info("Voz baja detectada (nivel de referencia {}%). Aplicando ganancia x{}",
                Math.round(referenciaNormalizada * 100), String.format("%.2f", ganancia));

        ByteBuffer salida = ByteBuffer.allocate(pcmData.length).order(ByteOrder.LITTLE_ENDIAN);
        for (short muestra : samples) {
            double amplificada = muestra * ganancia;
            salida.putShort((short) limitadorSuave(amplificada));
        }

        return salida.array();
    }

    /**
     * Limitador suave tipo "soft clipping" usando tanh. En vez de recortar en
     * seco las muestras que exceden el rango de 16 bits (lo que suena como
     * distorsión digital dura), las comprime progresivamente cerca del límite,
     * preservando mejor la inteligibilidad de la voz cuando amplificamos fuerte.
     */
    private double limitadorSuave(double muestra) {
        double umbral = 30000.0; // por debajo de esto no tocamos nada
        double maximo = Short.MAX_VALUE;

        double abs = Math.abs(muestra);
        if (abs <= umbral) {
            return muestra;
        }

        double signo = Math.signum(muestra);
        double exceso = abs - umbral;
        double rango = maximo - umbral;
        double comprimido = umbral + rango * Math.tanh(exceso / rango);

        return signo * Math.min(comprimido, maximo);
    }

    /**
     * Filtro pasa-altos IIR de primer orden. Elimina ruido de baja frecuencia
     * (zumbido eléctrico, aire acondicionado, retumbe de micrófono, viento)
     * que ensucia la señal y confunde al reconocedor, sin afectar el rango de
     * frecuencias donde vive la voz humana (~85 Hz en adelante).
     */
    private byte[] filtroPasaAltos(byte[] pcmData, int sampleRate) {
        if (pcmData.length < 4) {
            return pcmData;
        }

        double frecuenciaCorte = 80.0; // Hz, por debajo de la voz humana típica
        double rc = 1.0 / (2 * Math.PI * frecuenciaCorte);
        double dt = 1.0 / sampleRate;
        double alpha = rc / (rc + dt);

        ByteBuffer entrada = ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN);
        int numSamples = pcmData.length / 2;
        ByteBuffer salida = ByteBuffer.allocate(pcmData.length).order(ByteOrder.LITTLE_ENDIAN);

        double muestraAnteriorEntrada = 0;
        double muestraAnteriorSalida = 0;

        for (int i = 0; i < numSamples; i++) {
            double actual = entrada.getShort();
            double filtrada = alpha * (muestraAnteriorSalida + actual - muestraAnteriorEntrada);

            muestraAnteriorEntrada = actual;
            muestraAnteriorSalida = filtrada;

            int redondeada = (int) Math.round(filtrada);
            if (redondeada > Short.MAX_VALUE) redondeada = Short.MAX_VALUE;
            if (redondeada < Short.MIN_VALUE) redondeada = Short.MIN_VALUE;
            salida.putShort((short) redondeada);
        }

        return salida.array();
    }

    /**
     * Igual que enviarAGoogleSTT pero con reintentos ante fallos de red
     * transitorios (útil cuando la conexión es inestable).
     */
    private String enviarAGoogleSTTConReintentos(byte[] pcmData, int sampleRate, String idioma) {
        RestClientException ultimaExcepcion = null;

        for (int intento = 1; intento <= REINTENTOS_RED; intento++) {
            try {
                return enviarAGoogleSTT(pcmData, sampleRate, idioma);
            } catch (RestClientException e) {
                ultimaExcepcion = e;
                log.warn("Intento {}/{} fallido al llamar a Google STT: {}", intento, REINTENTOS_RED, e.getMessage());
            }
        }

        throw ultimaExcepcion;
    }

    /**
     * Envia el audio PCM crudo a la API de Google, igual que hace
     * recognizer.recognize_google(audio_data, language=idioma) en Python.
     */
    private String enviarAGoogleSTT(byte[] pcmData, int sampleRate, String idioma) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "audio/l16; rate=" + sampleRate);

        HttpEntity<byte[]> request = new HttpEntity<>(pcmData, headers);

        String url = UriComponentsBuilder.fromUriString(GOOGLE_STT_URL)
                .queryParam("client", "chromium")
                .queryParam("lang", idioma)
                .queryParam("key", GOOGLE_API_KEY)
                .queryParam("maxAlternatives", MAX_ALTERNATIVAS)
                .queryParam("pFilter", 0) // no filtrar/censurar palabras, mejora precisión en frases informales
                .toUriString();

        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        return parsearRespuesta(response.getBody());
    }

    /**
     * La API de Google devuelve varias lineas NDJSON; la ultima linea con
     * resultados "final" trae las alternativas de transcripcion. Ahora, en vez
     * de quedarnos siempre con la primera alternativa, elegimos la de mayor
     * "confidence" quando ese campo viene informado (mejora la precisión en
     * condiciones de ruido, donde la primera opción no siempre es la mejor).
     */
    private String parsearRespuesta(String cuerpo) {
        if (cuerpo == null || cuerpo.isBlank()) {
            return null;
        }

        ObjectMapper mapper = new ObjectMapper();
        String mejorTranscripcion = null;
        double mejorConfianza = -1.0;

        for (String linea : cuerpo.split("\n")) {
            if (linea.isBlank()) {
                continue;
            }
            try {
                JsonNode nodo = mapper.readTree(linea);
                JsonNode resultado = nodo.path("result");
                if (resultado.isArray() && resultado.size() > 0) {
                    JsonNode alternativas = resultado.get(0).path("alternative");
                    if (alternativas.isArray() && alternativas.size() > 0) {
                        for (JsonNode alternativa : alternativas) {
                            String transcript = alternativa.path("transcript").asText(null);
                            if (transcript == null || transcript.isBlank()) {
                                continue;
                            }
                            double confianza = alternativa.path("confidence").asDouble(-1.0);

                            if (mejorTranscripcion == null) {
                                // Primera transcripción válida que encontramos: la usamos
                                // como base aunque no tenga "confidence".
                                mejorTranscripcion = transcript;
                                mejorConfianza = confianza;
                            } else if (confianza > mejorConfianza) {
                                mejorTranscripcion = transcript;
                                mejorConfianza = confianza;
                            }
                        }
                    }
                }
            } catch (Exception ignorado) {
                // Linea no es JSON valido (Google a veces manda lineas vacias intermedias)
            }
        }

        return mejorTranscripcion;
    }

    private TranscripcionResponseDTO guardarYRetornar(String texto, boolean exito, String idioma) {
        Transcripcion entidad = new Transcripcion();
        entidad.setTexto(texto);
        entidad.setExito(exito);
        entidad.setIdioma(idioma);
        entidad.setFechaCreacion(LocalDateTime.now());
        transcripcionRepository.save(entidad);

        return new TranscripcionResponseDTO(texto, exito);
    }
}