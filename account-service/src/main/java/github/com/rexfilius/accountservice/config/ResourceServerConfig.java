package github.com.rexfilius.accountservice.config;

import github.com.rexfilius.accountservice.service.security.CustomUserInfoOpaqueTokenIntrospector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;

//@ConfigurationProperties(prefix = "app.security")
//record SecurityProps(URI userInfoUri) {}

//@EnableConfigurationProperties(SecurityProps.class)
@Configuration
@EnableMethodSecurity
public class ResourceServerConfig {
    /**
     * Where your auth server exposes user info (what your old TokenServices used).
     */
    @Value("${app.security.user-info-uri}")
    private URI userInfoUri;

    /**
     * Introspector that calls your UserInfo endpoint with the bearer token.
     */
//    @Bean
//    OpaqueTokenIntrospector opaqueTokenIntrospector(RestClient.Builder restClientBuilder,
//                                                    SecurityProps props) {
//        return new CustomUserInfoOpaqueTokenIntrospector(restClientBuilder.build(), props.userInfoUri());
//    }
    @Bean
    OpaqueTokenIntrospector opaqueTokenIntrospector(RestClient.Builder restClientBuilder) {
        // Your new implementation that replaces CustomUserInfoTokenServices
        return new CustomUserInfoOpaqueTokenIntrospector(restClientBuilder.build(), userInfoUri);
    }

    /**
     * The HTTP security filter chain (replaces ResourceServerConfigurerAdapter).
     */
    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            OpaqueTokenIntrospector introspector
    ) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/demo").permitAll()
                        .anyRequest().authenticated()
                )
                // Resource Server with opaque tokens using our custom introspector
                .oauth2ResourceServer(rs -> rs.opaqueToken(ot -> ot.introspector(introspector)));

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

/*
@Configuration
@EnableResourceServer
public class ResourceServerConfig extends ResourceServerConfigurerAdapter {

    private final ResourceServerProperties sso;

    @Autowired
    public ResourceServerConfig(ResourceServerProperties sso) {
        this.sso = sso;
    }

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

    @Bean
    public ResourceServerTokenServices tokenServices() {
        return new CustomUserInfoTokenServices(sso.getUserInfoUri(), sso.getClientId());
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .antMatchers("/" , "/demo").permitAll()
                .anyRequest().authenticated();
    }
}
*/
