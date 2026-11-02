package com.truongtho.be.config;

import com.truongtho.be.enums.ErrorCode;
import com.truongtho.be.exception.ErrorBody;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SecurityConfig {

    static  final String API = "/api/v1";

    final JwtAuthenticationFilter jwtFilter;
    final ObjectMapper objectMapper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> write(res, ErrorCode.UNAUTHORIZED))
                        .accessDeniedHandler((req, res, ex) -> write(res, ErrorCode.FORBIDDEN)))

                .authorizeHttpRequests(a -> a
                        .requestMatchers(API + "/auth/me").authenticated()
                        .requestMatchers(API + "/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, API + "/products/**", API + "/categories/**").permitAll()
                        .requestMatchers(HttpMethod.POST, API + "/reservations").permitAll()
                        .requestMatchers(HttpMethod.PATCH,API +  "/reservations/**").hasAnyRole("ADMIN")
                        .requestMatchers(API + "/admin/**").hasRole("ADMIN")
                        .requestMatchers(API + "/products/**", API + "/categories/**",
                                API + "/dining-tables/**", API + "/reservations/**",
                                API + "/uploads/**").hasAnyRole("ADMIN", "STAFF")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }


    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter f) {
        FilterRegistrationBean<JwtAuthenticationFilter> r = new FilterRegistrationBean<>(f);
        r.setEnabled(false);
        return r;
    }

    private void write(HttpServletResponse res, ErrorCode c) throws IOException {
        res.setStatus(c.getStatus().value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(res.getWriter(),
                new ErrorBody(new ErrorBody.Err(c.getCode(), c.getMessage(), null)));
    }



    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

}