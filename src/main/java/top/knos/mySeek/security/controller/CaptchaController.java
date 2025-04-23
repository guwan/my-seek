package top.knos.mySeek.security.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.knos.mySeek.security.pojo.CaptchaVo;
import top.knos.mySeek.security.service.CaptchaService;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class CaptchaController {

    @Autowired
    private CaptchaService captchaService;
    /**
     * 获取验证码。
     */
    @GetMapping("/captcha/base64")
    public CaptchaVo captcha(@RequestParam(required=false) String sessionId) throws Exception {
        return captchaService.getCaptcha(sessionId);
    }
    /**
     * 获取空白验证码，用于占位
     */
    /*@GetMapping("/captcha/base64/blank")
    public CaptchaVo blank() throws Exception {
        return captchaService.getBlank();
    }*/
}
