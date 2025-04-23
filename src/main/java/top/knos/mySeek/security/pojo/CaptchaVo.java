package top.knos.mySeek.security.pojo;

import lombok.Data;

@Data
public class CaptchaVo {
    private String imageData;
    private String captchaKey;
}
