package top.knos.mySeek.security.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import top.knos.mySeek.common.Result;
import top.knos.mySeek.exception.CustomHttpException;
import top.knos.mySeek.security.JwtTokenProvider;
import top.knos.mySeek.security.domain.User;
import top.knos.mySeek.security.repository.AuthorityRepository;
import top.knos.mySeek.security.service.impl.UserService;
import top.knos.mySeek.security.support.UserCheck;
import top.knos.mySeek.security.vo.req.LoginReq;
import top.knos.mySeek.utils.RedisCaptchaUtil;

/**
 * TODO
 *
 * @Author andy
 * @Date 2022/3/19 22:16
 */
@Service
@Slf4j
public class AuthService {

    private final UserService userService;
    private final AuthorityRepository authorityRepository;

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    private final UserCheck userCheck;
    private final RedisCaptchaUtil redisCaptchaUtil;

    public AuthService(UserService userService,
                       AuthorityRepository authorityRepository, AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider, UserCheck userCheck, RedisCaptchaUtil redisCaptchaUtil) {
        this.userService = userService;
        this.authorityRepository = authorityRepository;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userCheck = userCheck;
        this.redisCaptchaUtil = redisCaptchaUtil;
    }

    public String login(String username, String password) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
            // loadUserByUsername 已在事务内初始化 authorities，这里直接复用认证主体，避免二次查询触发懒加载
            User user = (User) authentication.getPrincipal();
            return jwtTokenProvider.createToken(user.getUsername(), user.getAuthorities());
        } catch (AuthenticationException e) {
            //log.debug("登录失败：{}", ExceptionUtils.getStackTrace(e));
            throw new CustomHttpException(e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    public Result<Object> login(LoginReq loginReq) {
        boolean verify = redisCaptchaUtil.verifyLoginCaptcha(loginReq.getCaptchaKey(), loginReq.getCaptcha());
        if (!verify) {
            return Result.fail(HttpStatus.BAD_REQUEST.value(), "验证码错误");
        }
        String token = login(loginReq.getUsername(), loginReq.getPassword());
        log.debug("登录成功，用户: {}", loginReq.getUsername());
        return Result.ok(token);
    }


}
