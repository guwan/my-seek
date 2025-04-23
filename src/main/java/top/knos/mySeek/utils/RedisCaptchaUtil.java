package top.knos.mySeek.utils;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.knos.captcha.PngCaptcha;
import top.knos.mySeek.security.pojo.CaptchaVo;

import java.io.IOException;

@Component
public class RedisCaptchaUtil {
    @Autowired
    RedisUtil redisUtil;
    private static final String LOGIN_CAPTCHA_KEY = "login_c_";

    public CaptchaVo getLoginCaptcha(String sessionId) throws IOException {
        return getCaptcha(LOGIN_CAPTCHA_KEY,sessionId);
    }
    public boolean verifyLoginCaptcha( String sessionId,String code) {
        return verifyCaptcha(LOGIN_CAPTCHA_KEY,sessionId,code);
    }
    public boolean verifyCaptcha(String prefix,String sessionId,String code) {
        if (code != null) {
            String captcha = redisUtil.getStr(prefix+sessionId);
            return StringUtils.equalsIgnoreCase(code.trim(), captcha);
        }
        return false;
    }
    public CaptchaVo getCaptcha(String prefix,String sessionId) throws IOException {
        if (sessionId == null) {
            sessionId = UUID.captchaChar(10);
        }
        PngCaptcha pngCaptcha = new PngCaptcha(130, 48, 4);

        String captcha = pngCaptcha.text();
        String img =pngCaptcha.toBase64();
        redisUtil.set(prefix+sessionId, captcha, 600);
        CaptchaVo captchaVo = new CaptchaVo();
        captchaVo.setCaptchaKey(sessionId);
        captchaVo.setImageData(img);
        return captchaVo;
    }
}
