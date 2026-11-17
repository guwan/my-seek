package top.knos.mySeek.config;

import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import top.knos.mySeek.security.JwtTokenFilter;
import top.knos.mySeek.security.JwtTokenProvider;
import top.knos.mySeek.security.service.RbacAuthorityService;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 免鉴权路径。
     * <p>SecurityConfig 与 JwtTokenFilter 共用这一份配置，避免 Authorization 已放行、
     * 但 JWT 过滤器仍然要求 token 导致 401 的不一致问题。</p>
     * <p>注意：/api/ai/** 与 /api/dev/** 均已改为需要登录，避免匿名用户占用模型资源、
     * 读取他人会话以及调用开发工具接口。</p>
     */
    public static final String[] PUBLIC = new String[]{
            "/api/auth/**",
            "/api/open/**",
            "/actuator/health",
            "/actuator/health/**",
            "/error",
            "/static/**",
            "/auth/logout",
    };

    private final JwtTokenProvider jwtTokenProvider;
    private final RbacAuthorityService rbacAuthorityService;

    @Value("${app.cors.allowed-origins:http://localhost:*,http://127.0.0.1:*}")
    private String allowedOrigins;

    public SecurityConfig(JwtTokenProvider jwtTokenProvider, RbacAuthorityService rbacAuthorityService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.rbacAuthorityService = rbacAuthorityService;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 用 origin 模式而非精确匹配，覆盖 Vite 自动顺延的 5174/5175 等本地端口
        configuration.setAllowedOriginPatterns(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList());
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {

        // Disable CSRF (cross site request forgery)
        http.csrf(AbstractHttpConfigurer::disable);

        // No session will be created or used by spring security
        http.sessionManagement((sessionManagement) ->
                sessionManagement
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        http.cors(cors -> cors.configurationSource(corsConfigurationSource));

        http.authorizeHttpRequests(
                (authorizeHttpRequests) ->
                        authorizeHttpRequests
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
