package top.knos.mySeek.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import top.knos.mySeek.security.domain.Authority;
import top.knos.mySeek.security.domain.User;
import top.knos.mySeek.security.service.impl.UserService;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JwtTokenProvider {

    /** HS256 要求密钥至少 256 bit */
    private static final int MIN_SECRET_BYTES = 32;

    private final UserService userService;
    private final Environment environment;

    @Value("${security.jwt.token.secret-key:}")
    private String secretKey;

    @Value("${security.jwt.token.expire-length:3600000}")
    private long validityInMilliseconds = 3600000; // 1h

    /** 派生的 HMAC 密钥 */
    private SecretKey signingKey;

    public JwtTokenProvider(UserService userService, Environment environment) {
        this.userService = userService;
        this.environment = environment;
    }

    @PostConstruct
    protected void init() {
        if (secretKey == null || secretKey.isBlank()) {
            // 生产环境必须显式配置密钥，避免多实例使用随机密钥导致 token 互相不认
            if (environment.acceptsProfiles(Profiles.of("prod"))) {
                throw new IllegalStateException("未配置 security.jwt.token.secret-key，请通过环境变量 JWT_SECRET_KEY 注入");
            }
            // 开发环境生成临时随机密钥，保证开箱即用（重启后历史 token 失效）
            byte[] randomKey = new byte[MIN_SECRET_BYTES];
            new SecureRandom().nextBytes(randomKey);
            this.signingKey = Keys.hmacShaKeyFor(randomKey);
            log.warn("未配置 security.jwt.token.secret-key，已启用临时随机密钥（仅限开发环境，重启后所有 token 失效）。"
                    + "生产环境请通过环境变量 JWT_SECRET_KEY 注入。");
            return;
        }
        this.signingKey = Keys.hmacShaKeyFor(deriveKeyBytes(secretKey));
    }

    /**
     * 密钥字节派生：优先按 Base64 解码，解码后长度不足则退化为原始 UTF-8 字节。
     */
    private byte[] deriveKeyBytes(String raw) {
        try {
            byte[] decoded = Base64.getDecoder().decode(raw);
            if (decoded.length >= MIN_SECRET_BYTES) {
                return decoded;
            }
        } catch (IllegalArgumentException ignored) {
            // 非 Base64 格式，按原始字符串处理
        }
        byte[] bytes = raw.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("security.jwt.token.secret-key 长度不足，至少需要 " + MIN_SECRET_BYTES + " 字节");
        }
        return bytes;
    }

    public String createToken(String username, Set<Authority> authorities) {

        List<SimpleGrantedAuthority> auth = authorities
                .stream()
                .map(authority -> new SimpleGrantedAuthority(authority.getAuthority()))
                .collect(Collectors.toList());

        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .subject(username)
                .claim("auth", auth)
                .issuedAt(now)
                .expiration(validity)
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    @Transactional
    public Authentication getAuthentication(String token) {
        UserDetails userDetails = userService.loadUserByUsername(getUsername(token));
        //延迟加载的数据，需要在这里获取
        User user = (User) userDetails;
        user.getTags().size();
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    public String getUsername(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    public String resolveToken(HttpServletRequest req) {
        String bearerToken = req.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtException("jwt令牌无效或过期");
        }
    }
}
