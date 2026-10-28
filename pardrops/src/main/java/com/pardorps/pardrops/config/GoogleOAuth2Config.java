package com.pardorps.pardrops.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;

/**
 * Registro del cliente OAuth2 de Google.
 *
 * <p>El registro solo se crea cuando existe la variable de entorno
 * {@code GOOGLE_CLIENT_ID}. De este modo el proyecto arranca sin credenciales
 * (desarrollo, pruebas y despliegues que aún no usan Google) y el flujo se
 * habilita en cuanto se configuran las credenciales.</p>
 */
@Configuration
public class GoogleOAuth2Config {

    /**
     * Crea el registro de Google a partir de las variables de entorno.
     *
     * @param clientId     identificador de cliente de Google Cloud.
     * @param clientSecret secreto de cliente de Google Cloud.
     * @return repositorio con el registro de Google.
     */
    @Bean
    @ConditionalOnExpression("'${GOOGLE_CLIENT_ID:}'.length() > 0")
    public ClientRegistrationRepository clientRegistrationRepository(
            @Value("${GOOGLE_CLIENT_ID:}") String clientId,
            @Value("${GOOGLE_CLIENT_SECRET:}") String clientSecret
    ) {
        ClientRegistration google = ClientRegistration.withRegistrationId("google")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
                .userNameAttributeName(IdTokenClaimNames.SUB)
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .clientName("Google")
                .build();

        return new InMemoryClientRegistrationRepository(google);
    }
}
