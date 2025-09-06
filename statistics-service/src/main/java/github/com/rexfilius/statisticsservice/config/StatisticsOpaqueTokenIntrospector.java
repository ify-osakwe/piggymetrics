package github.com.rexfilius.statisticsservice.config;

import org.springframework.http.HttpHeaders;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Spring Security 6/Boot 3 replacement for the legacy ResourceServerTokenServices approach.
 *
 * Fetches claims from a UserInfo endpoint using the bearer token and builds an OAuth2 principal.
 * If your Authorization Server supports introspection and you need client_id, consider using
 * NimbusOpaqueTokenIntrospector (introspection endpoint) instead, or ensure userinfo returns it.
 */
public class StatisticsOpaqueTokenIntrospector implements OpaqueTokenIntrospector {

    private static final List<String> PRINCIPAL_KEYS =
            List.of("user","username","userid","user_id","login","id","name");

    private final RestClient restClient;
    private final URI userInfoUri;

    public StatisticsOpaqueTokenIntrospector(String userInfoEndpointUrl) {
        this(RestClient.builder().build(), URI.create(userInfoEndpointUrl));
    }

    public StatisticsOpaqueTokenIntrospector(RestClient restClient, URI userInfoUri) {
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



/**

public class CustomUserInfoTokenServices implements ResourceServerTokenServices {

	protected final Log logger = LogFactory.getLog(getClass());

	private static final String[] PRINCIPAL_KEYS = new String[] { "user", "username",
			"userid", "user_id", "login", "id", "name" };

	private final String userInfoEndpointUrl;

	private final String clientId;

	private OAuth2RestOperations restTemplate;

	private String tokenType = DefaultOAuth2AccessToken.BEARER_TYPE;

	private AuthoritiesExtractor authoritiesExtractor = new FixedAuthoritiesExtractor();

	public CustomUserInfoTokenServices(String userInfoEndpointUrl, String clientId) {
		this.userInfoEndpointUrl = userInfoEndpointUrl;
		this.clientId = clientId;
	}

	public void setTokenType(String tokenType) {
		this.tokenType = tokenType;
	}

	public void setRestTemplate(OAuth2RestOperations restTemplate) {
		this.restTemplate = restTemplate;
	}

	public void setAuthoritiesExtractor(AuthoritiesExtractor authoritiesExtractor) {
		this.authoritiesExtractor = authoritiesExtractor;
	}

	@Override
	public OAuth2Authentication loadAuthentication(String accessToken)
			throws AuthenticationException, InvalidTokenException {
		Map<String, Object> map = getMap(this.userInfoEndpointUrl, accessToken);
		if (map.containsKey("error")) {
			this.logger.debug("userinfo returned error: " + map.get("error"));
			throw new InvalidTokenException(accessToken);
		}
		return extractAuthentication(map);
	}

	private OAuth2Authentication extractAuthentication(Map<String, Object> map) {
		Object principal = getPrincipal(map);
		OAuth2Request request = getRequest(map);
		List<GrantedAuthority> authorities = this.authoritiesExtractor
				.extractAuthorities(map);
		UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
				principal, "N/A", authorities);
		token.setDetails(map);
		return new OAuth2Authentication(request, token);
	}

	private Object getPrincipal(Map<String, Object> map) {
		for (String key : PRINCIPAL_KEYS) {
			if (map.containsKey(key)) {
				return map.get(key);
			}
		}
		return "unknown";
	}

	@SuppressWarnings({ "unchecked" })
	private OAuth2Request getRequest(Map<String, Object> map) {
		Map<String, Object> request = (Map<String, Object>) map.get("oauth2Request");

		String clientId = (String) request.get("clientId");
		Set<String> scope = new LinkedHashSet<>(request.containsKey("scope") ?
				(Collection<String>) request.get("scope") : Collections.<String>emptySet());

		return new OAuth2Request(null, clientId, null, true, new HashSet<>(scope),
				null, null, null, null);
	}

	@Override
	public OAuth2AccessToken readAccessToken(String accessToken) {
		throw new UnsupportedOperationException("Not supported: read access token");
	}

	@SuppressWarnings({ "unchecked" })
	private Map<String, Object> getMap(String path, String accessToken) {
		this.logger.debug("Getting user info from: " + path);
		try {
			OAuth2RestOperations restTemplate = this.restTemplate;
			if (restTemplate == null) {
				BaseOAuth2ProtectedResourceDetails resource = new BaseOAuth2ProtectedResourceDetails();
				resource.setClientId(this.clientId);
				restTemplate = new OAuth2RestTemplate(resource);
			}
			OAuth2AccessToken existingToken = restTemplate.getOAuth2ClientContext()
					.getAccessToken();
			if (existingToken == null || !accessToken.equals(existingToken.getValue())) {
				DefaultOAuth2AccessToken token = new DefaultOAuth2AccessToken(
						accessToken);
				token.setTokenType(this.tokenType);
				restTemplate.getOAuth2ClientContext().setAccessToken(token);
			}
			return restTemplate.getForEntity(path, Map.class).getBody();
		}
		catch (Exception ex) {
			this.logger.info("Could not fetch user details: " + ex.getClass() + ", "
					+ ex.getMessage());
			return Collections.<String, Object>singletonMap("error",
					"Could not fetch user details");
		}
	}
}
 */