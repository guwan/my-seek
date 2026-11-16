package top.knos.mySeek.security;


import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import top.knos.mySeek.exception.TipsException;
import top.knos.mySeek.security.service.RbacAuthorityService;
import top.knos.mySeek.utils.JsonUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static top.knos.mySeek.config.SecurityConfig.PUBLIC;

// We should use OncePerRequestFilter since we are doing a database call, there is no point in doing this more than once
public class JwtTokenFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    private final RbacAuthorityService rbacAuthorityService;

    //"/api/auth/captcha/base64",

    public JwtTokenFilter(JwtTokenProvider jwtTokenProvider, RbacAuthorityService rbacAuthorityService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.rbacAuthorityService = rbacAuthorityService;

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
            if (token == null) {
                SecurityContextHolder.clearContext();
                writeError(response, HttpStatus.UNAUTHORIZED.value(), "未登录或登录已过期");
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
            writeError(response, HttpStatus.UNAUTHORIZED.value(), ex.getMessage());
            return;
        } catch (TipsException ex) {
            //this is very important, since it guarantees the user is not authenticated at all
            SecurityContextHolder.clearContext();
            writeError(response, ex.getCode(), ex.getMessage());
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 以统一 JSON 结构返回错误，保证前端能按 code 判断未登录等场景。
     */
    private void writeError(HttpServletResponse response, int code, String msg) throws IOException {
        response.setStatus(code);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(JsonUtils.objectToJson(
                top.knos.mySeek.common.Result.fail(code, msg == null ? "请求失败" : msg)));
    }

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    public boolean needTokenMatches(HttpServletRequest request) {
        // 判断请求是否匹配
        return Arrays.stream(PUBLIC)
                .noneMatch(matcher ->
                        antPathMatcher.match(matcher,request.getRequestURI())
                );
    }
}
