package top.knos.mySeek.security.vo.res;

import lombok.Data;

@Data
public class UserConfig {
    private Long userId;
    private String name;
    private String depart;
    private String phone;
    private String dialPhone;
}
