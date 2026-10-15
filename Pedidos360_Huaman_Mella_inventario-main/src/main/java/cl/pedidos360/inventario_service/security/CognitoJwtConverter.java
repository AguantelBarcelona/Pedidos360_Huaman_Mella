package cl.pedidos360.inventario_service.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.*;

import java.util.*;

public class CognitoJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>(
            Optional.ofNullable(scopes.convert(jwt)).orElse(List.of()));

        List<String> grupos = jwt.getClaimAsStringList("cognito:groups");
        if (grupos != null) {
            grupos.forEach(g -> authorities.add(new SimpleGrantedAuthority("ROLE_" + g)));
        }

        String usuario = jwt.getClaimAsString("username");
        return new JwtAuthenticationToken(jwt, authorities, usuario);
    }
}