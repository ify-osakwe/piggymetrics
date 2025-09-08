package github.com.rexfilius.notification.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Modernized security configuration for Notification Service (Java 21 + Spring Boot 3.5).
 *
 * Replaces deprecated Spring Security OAuth2 classes like:
 * - @EnableResourceServer / ResourceServerConfigurerAdapter
 * - OAuth2RestTemplate
 * - OAuth2FeignRequestInterceptor
 *
 * with Spring Security 6 equivalents:
 * - SecurityFilterChain + oauth2ResourceServer
 * - OAuth2AuthorizedClientManager for client_credentials
 * - RequestInterceptor that injects Bearer tokens via AuthorizedClientManager
 * - OAuth2-enabled WebClient
 */
/**
 * @author cdov
 */
@Configuration
public class NotificationServerConfig {

    // Registration id to use for outbound OAuth2 client_credentials
    // Ensure this exists under spring.security.oauth2.client.registration.<id>
    @Value("${app.security.feign-client-registration:my-client}")
    private String feignClientRegistrationId;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/demo").permitAll()
                .anyRequest().authenticated()
            )
            // Configure as opaque tokens; switch to .jwt() if using JWT resource server
            .oauth2ResourceServer(oauth2 -> oauth2.opaqueToken());

        return http.build();
    }

    @Bean
    OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository registrations,
            OAuth2AuthorizedClientService authorizedClientService) {

        var provider = OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .authorizationCode()
                .refreshToken()
                .build();

        var manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                registrations, authorizedClientService);
        manager.setAuthorizedClientProvider(provider);
        return manager;
    }

    @Bean
    WebClient oauth2WebClient(OAuth2AuthorizedClientManager manager) {
        var oauth2 = new ServletOAuth2AuthorizedClientExchangeFilterFunction(manager);
        oauth2.setDefaultClientRegistrationId(feignClientRegistrationId);
        return WebClient.builder().apply(oauth2.oauth2Configuration()).build();
    }

    /**
     * Feign interceptor that obtains a client_credentials token and sets Authorization header.
     */
    @Bean
    public RequestInterceptor oauth2FeignRequestInterceptor(OAuth2AuthorizedClientManager manager) {
        return requestTemplate -> {
            var authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId(feignClientRegistrationId)
                    .principal(new AnonymousAuthenticationToken(
                            "feign",
                            "feign",
                            AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")
                    ))
                    .build();

            OAuth2AuthorizedClient client = manager.authorize(authorizeRequest);
            if (client != null && client.getAccessToken() != null) {
                requestTemplate.header(HttpHeaders.AUTHORIZATION,
                        "Bearer " + client.getAccessToken().getTokenValue());
            }
        };
    }
}

/*
================================================================================
 Legacy (pre-Spring Boot 3) configuration kept for reference. DO NOT DELETE.
================================================================================

package github.com.rexfilius.notification.config;

import feign.RequestInterceptor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.security.oauth2.client.feign.OAuth2FeignRequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;

@Configuration
@EnableResourceServer
public class NotificationServerConfig extends ResourceServerConfigurerAdapter {
    @Bean
    @ConfigurationProperties(prefix = "security.oauth2.client")
    public ClientCredentialsResourceDetails clientCredentialsResourceDetails() {
        return new ClientCredentialsResourceDetails();
    }
    @Bean
    public RequestInterceptor oauth2FeignRequestInterceptor(){
        return new OAuth2FeignRequestInterceptor(new DefaultOAuth2ClientContext(), clientCredentialsResourceDetails());
    }

    @Bean
    public OAuth2RestTemplate clientCredentialsRestTemplate() {
        return new OAuth2RestTemplate(clientCredentialsResourceDetails());
    }
}

*/
