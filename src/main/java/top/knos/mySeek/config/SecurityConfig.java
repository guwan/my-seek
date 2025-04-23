package top.knos.mySeek.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import top.knos.mySeek.security.JwtTokenFilter;
import top.knos.mySeek.security.JwtTokenProvider;
import top.knos.mySeek.security.service.RbacAuthorityService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final RbacAuthorityService rbacAuthorityService;

    public SecurityConfig(JwtTokenProvider jwtTokenProvider, RbacAuthorityService rbacAuthorityService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.rbacAuthorityService = rbacAuthorityService;
    }

    public static final String[] PUBLIC = new String[]{
            "/api/dev/autoGenerateMenu",
            "/api/auth/**",
            "/api/open/**",
            "/api/call/event",
            "/api/fee/event",
            "/api/ai/**",
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // Disable CSRF (cross site request forgery)
        http.csrf(AbstractHttpConfigurer::disable);

        // No session will be created or used by spring security
        http.sessionManagement((sessionManagement) ->
                sessionManagement
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        http.authorizeHttpRequests(
                (authorizeHttpRequests) ->
                        authorizeHttpRequests
                                .requestMatchers("/static/**").permitAll()
                                .requestMatchers(PUBLIC).permitAll()
                                .requestMatchers("/api/**").authenticated()
                                .anyRequest().permitAll());


        // Apply JWT
        http.addFilterBefore(
                new JwtTokenFilter(jwtTokenProvider, rbacAuthorityService),
                UsernamePasswordAuthenticationFilter.class
        );
        http.logout(logout -> logout.logoutUrl("/auth/logout"));


        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
