package top.knos.mySeek.security.vo.req;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * TODO
 *
 * @Author 技术古玩
 * @Date 2021/5/1 20:50
 */
@JsonIgnoreProperties(ignoreUnknown =  true )
@Data
public class LoginReq {
    private Boolean autoLogin;
    private String password;
    private String type;
    private String username;
    private String captcha;
    private String captchaKey;
}
