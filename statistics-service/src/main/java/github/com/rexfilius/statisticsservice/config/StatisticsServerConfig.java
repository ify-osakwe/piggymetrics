package github.com.rexfilius.statisticsservice.config;

import github.com.rexfilius.statisticsservice.service.security.StatisticsOpaqueTokenIntrospector;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableMethodSecurity
public class StatisticsServerConfig  {

    @Value("${app.security.user-info-uri}")
    private URI userInfoUri;

    @Bean
    OpaqueTokenIntrospector opaqueTokenIntrospector(RestClient.Builder restBuilder) {
        return new StatisticsOpaqueTokenIntrospector(restBuilder.build(), userInfoUri);
    }

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        OpaqueTokenIntrospector introspector
    ) throws Exception {
        http.authorizeHttpRequests(auth -> 
        auth.requestMatchers("/", "/demo").permitAll()
        .anyRequest().authenticated())
        .oauth2ResourceServer(rs -> 
        rs.opaqueToken(ot -> ot.introspector(introspector))) ;

        return http.build();
    }

    // ---------- OPTIONAL: Outbound OAuth2 (WebClient) + Feign integration ----------

    /**
     * A manager that obtains/refreshes access tokens for your registered OAuth2 clients.
     * Useful for WebClient and also used by Spring Cloud OpenFeign's OAuth2 support.
     */
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

    /**
     * If you previously injected an OAuth2RestTemplate, use WebClient instead.
     * Set a default client registration ID that uses the client_credentials flow.
     */
    @Bean
    WebClient oauth2WebClient(OAuth2AuthorizedClientManager manager) {
        var oauth2 = new ServletOAuth2AuthorizedClientExchangeFilterFunction(manager);
        oauth2.setDefaultClientRegistrationId("my-client"); // match your application.yml
        return WebClient.builder().apply(oauth2.oauth2Configuration()).build();
    }
}




// @Configuration
// @EnableMethodSecurity
// public class ResourceServerConfig extends ResourceServerConfigurerAdapter {
//     @Autowired
//     private ResourceServerProperties sso;

//     @Bean
//     public ResourceServerTokenServices tokenServices() {
//         return new CustomUserInfoTokenServices(sso.getUserInfoUri(), sso.getClientId());
//     }
// }
