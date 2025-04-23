package top.knos.mySeek.constant;

import lombok.Getter;

/**
 * TODO
 *
 * @Author 技术古玩
 * @Date 2021/4/27 15:15
 */
@Getter
public enum AuthEnum {
    ADMIN("admin"),
    USER("user");
    private final String name;

    AuthEnum(String name) {
        this.name = name;
    }

}
