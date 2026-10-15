package cl.pedidos360.inventario_service.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuer;

    @Value("${app.cognito.client-id}")
    private String clientId;

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuer).build();

        OAuth2TokenValidator<Jwt> porDefecto = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> completo = new DelegatingOAuth2TokenValidator<>(
                porDefecto, new CognitoAccessTokenValidator(clientId));

        decoder.setJwtValidator(completo);
        return decoder;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                        .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/inventario/producto/*/reservar").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/inventario/producto/*/liberar").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/inventario/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/inventario/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/inventario/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/inventario/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth -> oauth
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(new CognitoJwtConverter())))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                .accessDeniedHandler(new BearerTokenAccessDeniedHandler()));

        return http.build();
    }
}