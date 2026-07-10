# Diagrama de funcionamiento del sistema

Este diagrama describe el flujo principal del proceso de evaluación de respuestas en el proyecto.

```mermaid
flowchart TD
    A[Usuario inicia la evaluación] --> B[Selecciona una pregunta]
    B --> C[Frontend envía audio, respuesta esperada y frames]
    C --> D[Controlador de evaluación]
    D --> E[Servicio de transcripción de audio]
    E --> F[Servicio evaluador]
    D --> G[Servicio de visión]
    G --> H[Detección de rostro y ojos]
    H --> I[Análisis de atención]
    F --> J[Comparación de respuesta]
    E --> J
    I --> K[Generación de resultado]
    J --> K
    K --> L[Persistencia de evaluación]
    L --> M[Respuesta al usuario]
    M --> N[Visualización de porcentaje, texto transcrito y atención]
```

## Descripción breve

1. El usuario inicia una evaluación desde la interfaz.
2. El sistema recibe audio, la respuesta esperada y datos visuales del frame.
3. Se transcribe el audio, se detecta la atención y se compara la respuesta.
4. El resultado se guarda y se devuelve al usuario.
