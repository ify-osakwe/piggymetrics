package github.com.rexfilius.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration using Spring Security 6 style (Boot 3.5).
 *
 * Previous WebSecurityConfigurerAdapter-based config is kept below as comments.
 */
@Configuration
public class WebSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        // Uses the auto-configured AuthenticationManager built from 
        // UserDetailsService and PasswordEncoder beans
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}


/*
    // Legacy config (pre-Spring Security 6)
    // @Configuration
    // public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    //
    //     private final MongoUserDetailsService userDetailsService;
    //
    //     public WebSecurityConfig(MongoUserDetailsService userDetailsService) {
    //         this.userDetailsService = userDetailsService;
    //     }
    //
    //     @Override
    //     protected void configure(HttpSecurity http) throws Exception {
    //         http
    //                 .authorizeRequests().anyRequest().authenticated()
    //                 .and()
    //                 .csrf().disable();
    //     }
    //
    //     @Override
    //     protected void configure(AuthenticationManagerBuilder auth) throws Exception {
    //         auth.userDetailsService(userDetailsService)
    //                 .passwordEncoder(passwordEncoder());
    //     }
    //
    //     @Bean
    //     @Override
    //     public AuthenticationManager authenticationManagerBean() throws Exception {
    //         return super.authenticationManagerBean();
    //     }
    //
    //     @Bean
    //     public PasswordEncoder passwordEncoder() {
    //         return new BCryptPasswordEncoder();
    //     }
    // }
    */