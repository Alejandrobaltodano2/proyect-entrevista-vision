package com.preguntasSimulator.preguntas.controllers;

import com.preguntasSimulator.preguntas.Service.PreguntasService;
import com.preguntasSimulator.preguntas.models.Preguntas;

import com.preguntasSimulator.preguntas.models.dtos.PreguntasDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/preguntas")
public class PreguntasController {

    @Autowired
    private PreguntasService preguntasService;

    @GetMapping
    public ResponseEntity<List<Preguntas>> listarPreguntas() {
        List<Preguntas> preguntas = preguntasService.listarPreguntas();
        return ResponseEntity.ok(preguntas);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Preguntas> obtenerPreguntaPorId(@PathVariable  Integer id) {
        Preguntas pregunta = preguntasService.obtenerPreguntaPorId(id);
        if (pregunta != null) {
            return ResponseEntity.ok(pregunta);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<Void> guardarPregunta(@RequestBody PreguntasDTO pregunta) {
        preguntasService.guardarPregunta(pregunta);
        return ResponseEntity.ok().build();
    }

    @PutMapping
    public ResponseEntity<Void> modificarPregunta(@RequestBody PreguntasDTO pregunta) {
        preguntasService.modificarPregunta(pregunta);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarPregunta(@PathVariable Integer id) {
        preguntasService.eliminarPregunta(id);
        return ResponseEntity.ok().build();
    }


}
