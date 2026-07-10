# Diagrama de casos de uso

```mermaid
flowchart LR
    U[Usuario] --> UC1[Consultar preguntas]
    U --> UC2[Responder una pregunta]
    U --> UC3[Enviar audio para transcripción]
    U --> UC4[Enviar frame para detección de atención]
    U --> UC5[Recibir evaluación]

    A[Administrador] --> UC6[Administrar preguntas]
    A --> UC7[Consultar historial de evaluaciones]

    subgraph Sistema
        UC1
        UC2
        UC3
        UC4
        UC5
        UC6
        UC7
    end

    UC2 --> UC3
    UC2 --> UC4
    UC3 --> UC5
    UC4 --> UC5
    UC6 --> UC1
    UC7 --> UC5
```

## Casos de uso principales

- Consultar preguntas disponibles.
- Responder una pregunta mediante audio.
- Transcribir el audio recibido.
- Analizar el frame para detectar atención.
- Evaluar la respuesta y devolver un resultado.
- Administrar preguntas y revisar evaluaciones previas.
