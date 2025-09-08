package github.com.rexfilius.auth.config;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.UUID;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class OAuth2AuthorizationConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain authServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        return http.formLogin(Customizer.withDefaults()).build();
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository(Environment env, PasswordEncoder passwordEncoder) {
        // Public browser client: migrate from deprecated password grant to authorization_code + PKCE
        RegisteredClient browserClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("browser")
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE) // public client
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri(env.getProperty("BROWSER_REDIRECT_URI", "http://localhost:8080/login/oauth2/code/browser"))
                .scope("ui")
                .clientSettings(ClientSettings.builder().requireProofKey(true).requireAuthorizationConsent(false).build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(1))
                        .refreshTokenTimeToLive(Duration.ofDays(30))
                        .build())
                .build();

        // Service-to-service clients: client_credentials (no refresh tokens per spec)
        RegisteredClient accountService = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("account-service")
                .clientSecret(passwordEncoder.encode(env.getProperty("ACCOUNT_SERVICE_PASSWORD", "change-me")))
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("server")
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(2))
                        .reuseRefreshTokens(false)
                        .build())
                .build();

        RegisteredClient statisticsService = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("statistics-service")
                .clientSecret(passwordEncoder.encode(env.getProperty("STATISTICS_SERVICE_PASSWORD", "change-me")))
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("server")
                .tokenSettings(TokenSettings.builder().accessTokenTimeToLive(Duration.ofHours(2)).build())
                .build();

        RegisteredClient notificationService = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("notification-service")
                .clientSecret(passwordEncoder.encode(env.getProperty("NOTIFICATION_SERVICE_PASSWORD", "change-me")))
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("server")
                .tokenSettings(TokenSettings.builder().accessTokenTimeToLive(Duration.ofHours(2)).build())
                .build();

        return new InMemoryRegisteredClientRepository(browserClient, accountService, statisticsService, notificationService);
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder().build();
    }

    private static KeyPair generateRsaKey() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
