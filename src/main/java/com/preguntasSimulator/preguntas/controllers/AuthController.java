package com.preguntasSimulator.preguntas.controllers;

import com.preguntasSimulator.preguntas.models.Usuario;
import com.preguntasSimulator.preguntas.models.dtos.AuthResponseDTO;
import com.preguntasSimulator.preguntas.models.dtos.LoginRequestDTO;
import com.preguntasSimulator.preguntas.models.dtos.RegistroRequestDTO;
import com.preguntasSimulator.preguntas.repository.UsuarioRepository;
import com.preguntasSimulator.preguntas.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @PostMapping("/registro")
    public ResponseEntity<?> registro(@RequestBody RegistroRequestDTO datos) {
        if (datos.username() == null || datos.username().isBlank()
                || datos.password() == null || datos.password().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "username y password son obligatorios"));
        }

        if (usuarioRepository.existsByUsername(datos.username())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Ese username ya esta registrado"));
        }

        Usuario usuario = Usuario.builder()
                .username(datos.username())

                .password(passwordEncoder.encode(datos.password()))
                .nombres(datos.nombres())
                .apellidos(datos.apellidos())
                .carrera(datos.carrera())
                .build();

        usuarioRepository.save(usuario);


        String token = jwtService.generarToken(usuario.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponseDTO(token, usuario.getUsername()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO datos) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(datos.username(), datos.password()));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Username o password incorrectos"));
        }

        String token = jwtService.generarToken(datos.username());
        return ResponseEntity.ok(new AuthResponseDTO(token, datos.username()));
    }
}
