package cl.pedidos360.inventario_service.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CognitoAccessTokenValidatorTest {

    private static final String CLIENT_ID =
            "cliente-aplicacion-test";

    private final CognitoAccessTokenValidator validator =
            new CognitoAccessTokenValidator(CLIENT_ID);

    @Test
    void debeAceptarAccessTokenConClientIdCorrecto() {

        Jwt jwt = crearJwt(
                "access",
                CLIENT_ID
        );

        OAuth2TokenValidatorResult resultado =
                validator.validate(jwt);

        assertFalse(
                resultado.hasErrors()
        );
    }

    @Test
    void debeRechazarTokenQueNoSeaAccessToken() {

        Jwt jwt = crearJwt(
                "id",
                CLIENT_ID
        );

        OAuth2TokenValidatorResult resultado =
                validator.validate(jwt);

        assertTrue(
                resultado.hasErrors()
        );

        assertTrue(
                resultado.getErrors()
                        .iterator()
                        .next()
                        .getDescription()
                        .contains("access token")
        );
    }

    @Test
    void debeRechazarClientIdIncorrecto() {

        Jwt jwt = crearJwt(
                "access",
                "otro-client-id"
        );

        OAuth2TokenValidatorResult resultado =
                validator.validate(jwt);

        assertTrue(
                resultado.hasErrors()
        );

        assertTrue(
                resultado.getErrors()
                        .iterator()
                        .next()
                        .getDescription()
                        .contains("client_id")
        );
    }

    private Jwt crearJwt(
            String tokenUse,
            String clientId
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
                        "token_use",
                        tokenUse
                )
                .claim(
                        "client_id",
                        clientId
                )
                .build();
    }
}