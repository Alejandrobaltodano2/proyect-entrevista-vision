package com.preguntasSimulator.preguntas.Service.impl;

import com.preguntasSimulator.preguntas.Service.AudioTranscripcionService;
import com.preguntasSimulator.preguntas.models.Transcripcion;
import com.preguntasSimulator.preguntas.models.dtos.TranscripcionResponseDTO;
import com.preguntasSimulator.preguntas.repository.TranscripcionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Service

public class AudioTranscripcionServiceImpl implements AudioTranscripcionService {

    private static final Logger log = LoggerFactory.getLogger(AudioTranscripcionServiceImpl.class);

    private static final int TAMANIO_MINIMO_WAV = 44;

    private static final String GROQ_STT_URL = "https://api.groq.com/openai/v1/audio/transcriptions";

    @Value("${groq.api.key}")
    private String GROQ_API_KEY;

    private static final String GROQ_MODEL = "whisper-large-v3-turbo";

    private static final double PERCENTIL_REFERENCIA = 0.95;

    private static final double NIVEL_OBJETIVO = 0.60;

    private static final double GANANCIA_MAXIMA = 20.0;

    private static final double NIVEL_MINIMO_CON_SENAL = 0.01;

    private static final int REINTENTOS_RED = 2;

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    @Autowired
    private  RestTemplate restTemplate;
    @Autowired
    private  TranscripcionRepository transcripcionRepository;


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
            int canales = formato.getChannels();

            log.info("Formato de audio recibido: {} canal(es), {} bits, {} Hz, encoding={}",
                    canales, formato.getSampleSizeInBits(), sampleRate, formato.getEncoding());

            byte[] pcmData = audioInputStream.readAllBytes();
            byte[] audioParaEnviar;

            // --- Pre-procesamiento para mejorar reconocimiento con voz baja o ruido ---
            if (formato.getSampleSizeInBits() == 16
                    && formato.getEncoding() == AudioFormat.Encoding.PCM_SIGNED) {
                pcmData = filtroPasaAltos(pcmData, sampleRate);
                pcmData = normalizarGanancia(pcmData);

                audioParaEnviar = construirWav(pcmData, sampleRate, canales, 16);
            } else {

                log.warn("Formato de audio no es PCM_SIGNED de 16 bits (es {} de {} bits). " +
                        "Se omite el pre-procesamiento para no corromper el audio; " +
                        "revisar la grabación del cliente.", formato.getEncoding(), formato.getSampleSizeInBits());
                audioParaEnviar = wavBytes;
            }

            String textoReconocido = enviarAGroqConReintentos(audioParaEnviar, idiomaFinal);
            boolean exito = textoReconocido != null && !textoReconocido.isBlank();

            if (exito) {
                log.info("Transcripcion -> «{}»", textoReconocido);
            } else {
                log.warn("Groq: audio no reconocido (silencio, ruido o voz poco clara)");
            }

            return guardarYRetornar(exito ? textoReconocido : "", exito, idiomaFinal);

        } catch (UnsupportedAudioFileException e) {
            log.error("Formato de audio no soportado: {}", e.getMessage());
            return guardarYRetornar("", false, idiomaFinal);
        } catch (RestClientException e) {
            log.error("Groq error de red: {}", e.getMessage());
            return guardarYRetornar("", false, idiomaFinal);
        } catch (IOException e) {
            log.error("Error leyendo el audio WAV: {}", e.getMessage());
            return guardarYRetornar("", false, idiomaFinal);
        }
    }


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
            log.warn("Audio sin señal detectable (nivel {}%), se omite amplificación",
                    String.format("%.2f", referenciaNormalizada * 100));
            return pcmData;
        }

        double ganancia = NIVEL_OBJETIVO / referenciaNormalizada;
        ganancia = Math.min(ganancia, GANANCIA_MAXIMA);

        if (ganancia <= 1.05) {
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


    private double limitadorSuave(double muestra) {
        double umbral = 30000.0;
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


    private byte[] filtroPasaAltos(byte[] pcmData, int sampleRate) {
        if (pcmData.length < 4) {
            return pcmData;
        }

        double frecuenciaCorte = 80.0;
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

    private String enviarAGroqConReintentos(byte[] audioWav, String idioma) {
        RestClientException ultimaExcepcion = null;

        for (int intento = 1; intento <= REINTENTOS_RED; intento++) {
            try {
                return enviarAGroq(audioWav, idioma);
            } catch (RestClientException e) {
                ultimaExcepcion = e;
                log.warn("Intento {}/{} fallido al llamar a Groq: {}", intento, REINTENTOS_RED, e.getMessage());
            }
        }

        throw ultimaExcepcion;
    }


    private String enviarAGroq(byte[] audioWav, String idioma) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(GROQ_API_KEY);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(audioWav) {
            @Override
            public String getFilename() {
                return "audio.wav";
            }
        });
        body.add("model", GROQ_MODEL);
        body.add("language", idioma.split("-")[0]);
        body.add("response_format", "json");

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(GROQ_STT_URL, request, String.class);

        return parsearRespuestaGroq(response.getBody());
    }

    private String parsearRespuestaGroq(String cuerpo) {
        if (cuerpo == null || cuerpo.isBlank()) {
            return null;
        }
        try {
            JsonNode nodo = JSON_MAPPER.readTree(cuerpo);
            String texto = nodo.path("text").asText(null);
            return (texto != null && !texto.isBlank()) ? texto.trim() : null;
        } catch (Exception e) {
            log.warn("No se pudo parsear la respuesta de Groq: {}", e.getMessage());
            return null;
        }
    }


    private byte[] construirWav(byte[] pcmData, int sampleRate, int canales, int bitsPorMuestra) {
        int byteRate = sampleRate * canales * bitsPorMuestra / 8;
        int blockAlign = canales * bitsPorMuestra / 8;
        int dataSize = pcmData.length;

        ByteBuffer header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
        header.put("RIFF".getBytes(StandardCharsets.US_ASCII));
        header.putInt(36 + dataSize);
        header.put("WAVE".getBytes(StandardCharsets.US_ASCII));
        header.put("fmt ".getBytes(StandardCharsets.US_ASCII));
        header.putInt(16);
        header.putShort((short) 1);
        header.putShort((short) canales);
        header.putInt(sampleRate);
        header.putInt(byteRate);
        header.putShort((short) blockAlign);
        header.putShort((short) bitsPorMuestra);
        header.put("data".getBytes(StandardCharsets.US_ASCII));
        header.putInt(dataSize);

        byte[] wav = new byte[44 + dataSize];
        System.arraycopy(header.array(), 0, wav, 0, 44);
        System.arraycopy(pcmData, 0, wav, 44, dataSize);
        return wav;
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