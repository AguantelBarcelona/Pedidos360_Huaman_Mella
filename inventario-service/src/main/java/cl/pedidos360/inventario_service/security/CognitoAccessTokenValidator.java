package cl.pedidos360.inventario_service.security;

import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.Jwt;

public class CognitoAccessTokenValidator implements OAuth2TokenValidator<Jwt> {

    private final String clientId;

    public CognitoAccessTokenValidator(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String tokenUse = jwt.getClaimAsString("token_use");
        if (!"access".equals(tokenUse)) {
            return fallo("Se esperaba un access token, llego token_use=" + tokenUse);
        }
        if (!clientId.equals(jwt.getClaimAsString("client_id"))) {
            return fallo("El client_id del token no corresponde a esta aplicacion");
        }
        return OAuth2TokenValidatorResult.success();
    }

    private OAuth2TokenValidatorResult fallo(String descripcion) {
        return OAuth2TokenValidatorResult.failure(
                new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, descripcion, null));
    }
}