# Plan Scrum de 10 sprints

Este plan propone una ruta de desarrollo incremental para el proyecto usando metodología Scrum.

## Metodología

- Duración estimada: 10 sprints de 2 semanas cada uno.
- Enfoque: entregas incrementales, validación continua y mejora del producto.

## Sprint 1: Fundamentos del proyecto
- Objetivo: dejar la base técnica estable.
- Historias de usuario:
  - Como desarrollador, quiero tener la estructura base del backend Spring Boot.
  - Como equipo, quiero documentar la arquitectura inicial.
- Entregables:
  - Proyecto configurado.
  - Documentación inicial.

## Sprint 2: Gestión de preguntas
- Objetivo: implementar el módulo de administración de preguntas.
- Historias de usuario:
  - Como administrador, quiero crear, editar y eliminar preguntas.
  - Como usuario, quiero consultar preguntas disponibles.
- Entregables:
  - CRUD de preguntas.
  - Endpoints principales funcionales.

## Sprint 3: Detección de atención visual
- Objetivo: integrar la detección de rostros y ojos desde frames.
- Historias de usuario:
  - Como sistema, quiero analizar frames para detectar si la persona está mirando a la cámara.
- Entregables:
  - Servicio de visión.
  - Endpoint de detección.

## Sprint 4: Transcripción de audio
- Objetivo: convertir audio en texto de forma confiable.
- Historias de usuario:
  - Como usuario, quiero enviar un audio y obtener una transcripción.
- Entregables:
  - Servicio de transcripción.
  - Validación de archivos de audio.

## Sprint 5: Evaluación automática de respuestas
- Objetivo: comparar la respuesta esperada con la transcripción generada.
- Historias de usuario:
  - Como usuario, quiero recibir un porcentaje de acierto.
- Entregables:
  - Motor de evaluación.
  - Respuesta con métricas básicas.

## Sprint 6: Integración end-to-end
- Objetivo: unir todos los módulos en un flujo completo.
- Historias de usuario:
  - Como usuario, quiero enviar audio y recibir una evaluación completa.
- Entregables:
  - Flujo completo de evaluación.
  - Validación de datos entre módulos.

## Sprint 7: Persistencia y trazabilidad
- Objetivo: guardar evaluaciones y detecciones para auditoría.
- Historias de usuario:
  - Como administrador, quiero consultar resultados previos.
- Entregables:
  - Repositorios y registros persistidos.
  - Historial de evaluaciones.

## Sprint 8: Experiencia de usuario y usabilidad
- Objetivo: mejorar la interacción del usuario con la plataforma.
- Historias de usuario:
  - Como usuario, quiero entender claramente los resultados obtenidos.
- Entregables:
  - Mensajes de feedback.
  - Mejoras en la respuesta del sistema.

## Sprint 9: Calidad, pruebas y seguridad
- Objetivo: asegurar estabilidad y confianza en la solución.
- Historias de usuario:
  - Como equipo, quiero validar que el sistema funciona sin errores críticos.
- Entregables:
  - Pruebas unitarias y de integración.
  - Ajustes de seguridad y manejo de errores.

## Sprint 10: Despliegue y madurez del producto
- Objetivo: preparar el producto para uso real.
- Historias de usuario:
  - Como producto, quiero poder desplegarse y operar en entorno real.
- Entregables:
  - Configuración de despliegue.
  - Documentación de operación y mantenimiento.
