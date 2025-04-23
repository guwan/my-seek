package top.knos.mySeek.security.service;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GrecaptchaService {
    /*@Autowired
    private RedisUtil<String> redisUtil;*/
    @Value("${captcha.key}")
    private String captchaKey;
    @Value("${captcha.timeout}")
    private Long timeout;

    public String generate(String token){
        String captcha = RandomStringUtils.randomAlphanumeric(4);
        //redisUtil.set(captchaKey+token,captcha,timeout);
        return captcha;
    }


    public Boolean validate(String token,String validateCaptcha) {
        //String captcha = redisUtil.get(captchaKey+token);
        String captcha ="";
        if(StringUtils.equals(captcha,validateCaptcha)){
            return true;
        }
        return false;
    }
}
