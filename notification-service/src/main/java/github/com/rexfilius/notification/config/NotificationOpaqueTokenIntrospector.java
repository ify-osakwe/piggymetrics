package github.com.rexfilius.notification.config;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

public class NotificationOpaqueTokenIntrospector implements OpaqueTokenIntrospector {
    private static final List<String> PRINCIPAL_KEYS =
            List.of("user","username","userid","user_id","login","id","name");

    private final RestClient restClient;
    private final URI userInfoUri;

    public NotificationOpaqueTokenIntrospector(String userInfoEndpointUrl) {
        this(RestClient.builder().build(), URI.create(userInfoEndpointUrl));
    }

    public NotificationOpaqueTokenIntrospector(RestClient restClient, URI userInfoUri) {
        this.restClient = Objects.requireNonNull(restClient, "restClient must not be null");
        this.userInfoUri = Objects.requireNonNull(userInfoUri, "userInfoUri must not be null");
    }

    @Override
    @SuppressWarnings("unchecked")
    public OAuth2AuthenticatedPrincipal introspect(String token) {
        Map<String, Object> attributes;
        try {
            attributes = this.restClient.get()
                    .uri(this.userInfoUri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(Map.class);
        } catch (Exception ex) {
            throw new OAuth2IntrospectionException("Could not fetch user details from userinfo endpoint", ex);
        }

        if (attributes == null || attributes.containsKey("error")) {
            throw new OAuth2IntrospectionException("UserInfo endpoint returned an error");
        }

        String name = extractPrincipal(attributes);
        Collection<GrantedAuthority> authorities = extractAuthorities(attributes);
        return new DefaultOAuth2AuthenticatedPrincipal(name, attributes, authorities);
    }

    private Collection<GrantedAuthority> extractAuthorities(Map<String, Object> map) {
        Object auths = map.get("authorities");
        if (auths instanceof Collection<?> coll) {
            return coll.stream()
                    .map(Object::toString)
                    .filter(StringUtils::hasText)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toSet());
        }

        Object roles = map.get("roles");
        if (roles instanceof Collection<?> roleColl) {
            return roleColl.stream()
                    .map(Object::toString)
                    .filter(StringUtils::hasText)
                    .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toSet());
        }

        // Fallback: convert OAuth2 scopes to authorities as SCOPE_*
        Set<String> scopes = new LinkedHashSet<>();
        Object scopeClaim = map.get("scope"); // may be space-delimited String or Collection
        Object scpClaim   = map.get("scp");   // some providers use "scp"

        if (scopeClaim instanceof String s && StringUtils.hasText(s)) {
            scopes.addAll(Arrays.asList(s.split("\\s+")));
        } else if (scopeClaim instanceof Collection<?> c) {
            c.forEach(v -> { if (v != null) scopes.add(v.toString()); });
        }
        if (scpClaim instanceof Collection<?> c) {
            c.forEach(v -> { if (v != null) scopes.add(v.toString()); });
        }

        return scopes.stream()
                .filter(StringUtils::hasText)
                .map(s -> "SCOPE_" + s)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }

    private String extractPrincipal(Map<String, Object> map) {
        for (String key : PRINCIPAL_KEYS) {
            Object v = map.get(key);
            if (v != null) return String.valueOf(v);
        }
        return "unknown";
    }
}
