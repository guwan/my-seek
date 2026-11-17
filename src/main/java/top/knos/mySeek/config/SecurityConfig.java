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

    /**
     * 免鉴权路径。
     * <p>注意：/api/ai/** 与 /api/dev/** 均已改为需要登录，避免匿名用户占用模型资源、
     * 读取他人会话以及调用开发工具接口。</p>
     */
    public static final String[] PUBLIC = new String[]{
            "/api/auth/**",
            "/api/open/**",
            "/actuator/health",
            "/actuator/health/**",
    };

    private final JwtTokenProvider jwtTokenProvider;
    private final RbacAuthorityService rbacAuthorityService;

    public SecurityConfig(JwtTokenProvider jwtTokenProvider, RbacAuthorityService rbacAuthorityService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.rbacAuthorityService = rbacAuthorityService;
    }

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
                                .requestMatchers("/error").permitAll()
                                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                                .requestMatchers(PUBLIC).permitAll()
                                .requestMatchers("/actuator/**").authenticated()
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
