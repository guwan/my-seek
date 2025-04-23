package top.knos.mySeek.security.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.knos.mySeek.security.pojo.CaptchaVo;
import top.knos.mySeek.utils.RedisCaptchaUtil;

import java.io.IOException;

@Service
public class CaptchaService {
    @Autowired
    private RedisCaptchaUtil redisCaptchaUtil;
    public CaptchaVo getCaptcha(String sessionId) throws IOException {
        return redisCaptchaUtil.getLoginCaptcha(sessionId);
    }
}
