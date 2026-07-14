package com.preguntasSimulator.preguntas.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Se ejecuta una vez por peticion: lee el header "Authorization: Bearer ...",
 * valida el JWT y, si es valido, marca al usuario como autenticado para el
 * resto del pipeline de Spring Security. De aqui en adelante, cualquier
 * controller puede saber quien hizo la peticion con
 * SecurityContextHolder.getContext().getAuthentication().getName().
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        String username;

        try {
            username = jwtService.extraerUsername(token);
        } catch (Exception e) {
            // Token malformado/expirado/con firma invalida: seguimos sin
            // autenticar: el endpoint protegido respondera 401 mas adelante.
            filterChain.doFilter(request, response);
            return;
        }

        boolean nadieAutenticadoAun = SecurityContextHolder.getContext().getAuthentication() == null;

        if (username != null && nadieAutenticadoAun) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (jwtService.esTokenValido(token, userDetails.getUsername())) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
