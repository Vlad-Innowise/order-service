package by.innowise.internship.orders.config;

import by.innowise.internship.security.dto.Role;
import by.innowise.internship.security.filter.JwtFilter;
import by.innowise.internship.security.filter.JwtFilterConfigurer;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final List<String> WHITELIST_PATHS = List.of(
            "/actuator/health", "/actuator/health/**"
    );

    @Bean
    public JwtFilterConfigurer whitelistConfigurer() {
        return filter -> filter.setWhitelistPaths(WHITELIST_PATHS);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtFilter filter) throws Exception {

        http.cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                                       session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(requests -> requests
                    .requestMatchers(HttpMethod.GET, "/api/v1/items", "/api/v1/items/**").authenticated()
                    .requestMatchers("/api/v1/items", "/api/v1/items/**").hasRole(Role.ADMIN.name())
                    .requestMatchers(WHITELIST_PATHS.toArray(String[]::new)).permitAll()
                    .anyRequest().authenticated()
            )

            .exceptionHandling(configurer ->
                                       configurer
                                               .authenticationEntryPoint((req, resp, authException) ->
                                                                                 resp.setStatus(
                                                                                         HttpServletResponse.SC_UNAUTHORIZED))
                                               .accessDeniedHandler((req, resp, authException) ->
                                                                            resp.setStatus(
                                                                                    HttpServletResponse.SC_FORBIDDEN)
                                               )
            )
            .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}
