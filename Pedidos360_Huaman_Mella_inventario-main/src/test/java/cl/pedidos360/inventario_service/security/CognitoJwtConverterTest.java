package cl.pedidos360.inventario_service.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CognitoJwtConverterTest {

    private final CognitoJwtConverter converter =
            new CognitoJwtConverter();

    @Test
    void debeConvertirGrupoAdminEnRoleAdmin() {

        Jwt jwt = crearJwt(
                List.of("ADMIN"),
                "usuario-admin"
        );

        AbstractAuthenticationToken autenticacion =
                converter.convert(jwt);

        assertNotNull(
                autenticacion
        );

        assertEquals(
                "usuario-admin",
                autenticacion.getName()
        );

        assertTrue(
                autenticacion
                        .getAuthorities()
                        .stream()
                        .anyMatch(
                                autoridad ->
                                        autoridad
                                                .getAuthority()
                                                .equals("ROLE_ADMIN")
                        )
        );
    }

    @Test
    void debeConvertirGrupoClienteEnRoleCliente() {

        Jwt jwt = crearJwt(
                List.of("CLIENTE"),
                "usuario-cliente"
        );

        AbstractAuthenticationToken autenticacion =
                converter.convert(jwt);

        assertNotNull(
                autenticacion
        );

        assertEquals(
                "usuario-cliente",
                autenticacion.getName()
        );

        assertTrue(
                autenticacion
                        .getAuthorities()
                        .stream()
                        .anyMatch(
                                autoridad ->
                                        autoridad
                                                .getAuthority()
                                                .equals("ROLE_CLIENTE")
                        )
        );
    }

    @Test
    void debeConservarScopesComoAuthorities() {

        Jwt jwt = Jwt.withTokenValue("token-prueba")
                .header(
                        "alg",
                        "RS256"
                )
                .subject(
                        "sub-prueba"
                )
                .issuedAt(
                        Instant.now()
                                .minusSeconds(30)
                )
                .expiresAt(
                        Instant.now()
                                .plusSeconds(300)
                )
                .claim(
                        "username",
                        "usuario-prueba"
                )
                .claim(
                        "scope",
                        "inventario/read inventario/write"
                )
                .claim(
                        "cognito:groups",
                        List.of("CLIENTE")
                )
                .build();

        AbstractAuthenticationToken autenticacion =
                converter.convert(jwt);

        assertNotNull(
                autenticacion
        );

        assertTrue(
                autenticacion
                        .getAuthorities()
                        .stream()
                        .anyMatch(
                                autoridad ->
                                        autoridad
                                                .getAuthority()
                                                .equals(
                                                        "SCOPE_inventario/read"
                                                )
                        )
        );

        assertTrue(
                autenticacion
                        .getAuthorities()
                        .stream()
                        .anyMatch(
                                autoridad ->
                                        autoridad
                                                .getAuthority()
                                                .equals(
                                                        "SCOPE_inventario/write"
                                                )
                        )
        );
    }

    private Jwt crearJwt(
            List<String> grupos,
            String username
    ) {
        Instant ahora = Instant.now();

        return Jwt.withTokenValue("token-prueba")
                .header(
                        "alg",
                        "RS256"
                )
                .subject(
                        "sub-prueba"
                )
                .issuedAt(
                        ahora.minusSeconds(30)
                )
                .expiresAt(
                        ahora.plusSeconds(300)
                )
                .claim(
                        "username",
                        username
                )
                .claim(
                        "cognito:groups",
                        grupos
                )
                .build();
    }
}