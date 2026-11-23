package ar.org.cmm.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Login (T36). Sesión por cookie HttpOnly; el tiempo de vencimiento se configura en
 * {@code server.servlet.session.timeout} (application.yml). Protección CSRF con cookie XSRF-TOKEN
 * que el frontend devuelve en el encabezado X-XSRF-TOKEN.
 */
@Configuration
@EnableWebSecurity
public class SeguridadConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain cadena(HttpSecurity http, RespuestasSeguridad respuestas) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers("/h2-console/**"))
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
                .headers(h -> h.frameOptions(f -> f.sameOrigin())) // la consola H2 (solo demo) usa frames
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/", "/index.html", "/css/**", "/js/**").permitAll()
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/h2-console/**").hasRole("IT")
                        .anyRequest().authenticated())
                .formLogin(f -> f
                        .loginPage("/") // sin pantalla de login generada por Spring: la pantalla es del frontend
                        .loginProcessingUrl("/api/auth/login")
                        .usernameParameter("usuario")
                        .passwordParameter("clave")
                        .successHandler((req, res, auth) -> res.setStatus(HttpStatus.NO_CONTENT.value()))
                        .failureHandler((req, res, ex) -> respuestas.escribir(res, 401, "Ingreso rechazado",
                                "Usuario o contraseña incorrectos.")))
                .logout(l -> l
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((req, res, auth) -> res.setStatus(HttpStatus.NO_CONTENT.value()))
                        .deleteCookies("JSESSIONID"))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler((req, res, ex) -> {
                            if (ex instanceof CsrfException) {
                                respuestas.escribir(res, 403, "Sesión de seguridad vencida",
                                        "La página quedó desactualizada. Recárguela (F5) e intente de nuevo.");
                            } else {
                                respuestas.escribir(res, 403, "Sin permiso",
                                        "Su usuario no tiene permiso para esta operación.");
                            }
                        }));
        return http.build();
    }

    /** Fuerza a generar la cookie XSRF-TOKEN en cada respuesta (el token de Spring Security es diferido). */
    static final class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (token != null) {
                token.getToken();
            }
            chain.doFilter(request, response);
        }
    }
}
