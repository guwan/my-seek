package top.knos.mySeek.security;


import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import top.knos.mySeek.config.SecurityConfig;
import top.knos.mySeek.exception.TipsException;
import top.knos.mySeek.security.service.RbacAuthorityService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// We should use OncePerRequestFilter since we are doing a database call, there is no point in doing this more than once
public class JwtTokenFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final List<AntPathRequestMatcher> antPathRequestMatchers;

    private final RbacAuthorityService rbacAuthorityService;

    //"/api/auth/captcha/base64",

    public JwtTokenFilter(JwtTokenProvider jwtTokenProvider, RbacAuthorityService rbacAuthorityService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.rbacAuthorityService = rbacAuthorityService;

        // 定义需要排除的URL
        antPathRequestMatchers = new ArrayList<>();
        for (String excludedUrl : SecurityConfig.PUBLIC) {
            antPathRequestMatchers.add(new AntPathRequestMatcher(excludedUrl));
        }
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        //不需要token的链接就不校验
        if (!needTokenMatches(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = jwtTokenProvider.resolveToken(request);
        try {
            if(token == null){
                SecurityContextHolder.clearContext();
                response.sendError(HttpStatus.UNAUTHORIZED.value(), "未登录");
                return;
            }
            if (jwtTokenProvider.validateToken(token)) {
                Authentication auth = jwtTokenProvider.getAuthentication(token);
                rbacAuthorityService.checkPermission(request, auth);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (JwtException ex) {
            //this is very important, since it guarantees the user is not authenticated at all
            SecurityContextHolder.clearContext();
            response.sendError(HttpStatus.UNAUTHORIZED.value(), ex.getMessage());
            return;
        } catch (TipsException ex) {
            //this is very important, since it guarantees the user is not authenticated at all
            SecurityContextHolder.clearContext();
            response.sendError(ex.getCode(), ex.getMessage());
            return;
        }

        filterChain.doFilter(request, response);
    }

    public boolean needTokenMatches(HttpServletRequest request) {
        // 判断请求是否匹配
        return antPathRequestMatchers.stream().noneMatch(matcher -> matcher.matches(request));
    }
}
