package com.preguntasSimulator.preguntas.config;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    // Sin timeouts, una llamada externa lenta (p. ej. Google STT) puede
    // dejar un hilo de Tomcat bloqueado indefinidamente. En un servidor con
    // pocos hilos disponibles (plan gratuito de Render) esto puede agotar
    // el pool de hilos y tumbar el servicio con solo unas pocas peticiones
    // lentas simultaneas.
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(15);

    @Bean
    public RestTemplate restTemplate() {
        HttpClientSettings settings = HttpClientSettings.defaults()
                .withConnectTimeout(CONNECT_TIMEOUT)
                .withReadTimeout(READ_TIMEOUT);

        ClientHttpRequestFactory factory = ClientHttpRequestFactoryBuilder.detect().build(settings);

        return new RestTemplate(factory);
    }
}