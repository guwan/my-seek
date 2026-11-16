package top.knos.mySeek.security.service;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import top.knos.mySeek.utils.RedisUtil;

/**
 * 注册用字符验证码服务：验证码写入 Redis，校验时一次性消费。
 */
@Service
public class GrecaptchaService {

    private final RedisUtil redisUtil;

    @Value("${captcha.key:captcha_}")
    private String captchaKey;

    @Value("${captcha.timeout:60}")
    private Long timeout;

    public GrecaptchaService(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    /**
     * 生成验证码并存入 Redis。
     *
     * @param token 业务标识（如用户名）
     * @return 验证码明文（仅测试/调试使用，生产不应返回给前端）
     */
    public String generate(String token) {
        String captcha = RandomStringUtils.secure().nextAlphanumeric(4);
        redisUtil.set(captchaKey + token, captcha, timeout);
        return captcha;
    }

    /**
     * 校验验证码，校验通过后立即失效（防止重放）。
     */
    public Boolean validate(String token, String validateCaptcha) {
        if (validateCaptcha == null || validateCaptcha.isBlank() || token == null) {
            return false;
        }
        String key = captchaKey + token;
        String captcha = redisUtil.getStr(key);
        if (captcha == null) {
            return false;
        }
        // 一次性消费
        redisUtil.del(key);
        return StringUtils.equalsIgnoreCase(validateCaptcha.trim(), captcha);
    }
}
